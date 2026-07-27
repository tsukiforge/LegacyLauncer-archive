package net.legacylauncher.managers;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.util.EHttpClient;
import net.legacylauncher.util.async.AsyncThread;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpHeaders;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Checks for new versions of the launcher by querying GitHub Releases API.
 * Provides update notifications with changelog and download URL.
 */
@Slf4j
public class UpdateChecker {

    /** GitHub API URL to check for the latest release */
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/tsukiforge/LegacyLauncer-archive/releases/latest";

    /** GitHub API URL to list recent releases (fallback if latest fails) */
    private static final String GITHUB_API_RELEASES = "https://api.github.com/repos/tsukiforge/LegacyLauncer-archive/releases?per_page=5";

    /** Custom update check URL from boot config (if configured) */
    private final String customUpdateUrl;

    /** User-Agent string for GitHub API requests */
    private static final String USER_AGENT = "LegacyLauncher/" + LegacyLauncher.getVersion();

    @Getter
    private volatile LauncherUpdate latestUpdate;

    @Getter
    private volatile boolean checkComplete;

    @Getter
    private volatile boolean updateAvailable;

    public UpdateChecker(String customUpdateUrl) {
        this.customUpdateUrl = customUpdateUrl;
    }

    public UpdateChecker() {
        this(null);
    }

    /**
     * Starts an asynchronous check for launcher updates.
     * Results are available via {@link #isUpdateAvailable()} and {@link #getLatestUpdate()}.
     */
    public CompletableFuture<Optional<LauncherUpdate>> checkForUpdates() {
        return AsyncThread.completableTimeout(15, TimeUnit.SECONDS, () -> {
            try {
                if (customUpdateUrl != null && !customUpdateUrl.isEmpty()) {
                    return checkCustomUrl();
                }
                return checkGitHubReleases();
            } catch (Exception e) {
                log.warn("Failed to check for launcher updates", e);
                return Optional.empty();
            }
        }).whenComplete((result, error) -> {
            checkComplete = true;
            if (error != null) {
                log.warn("Update check failed: {}", error.toString());
                return;
            }
            result.ifPresent(update -> {
                latestUpdate = update;
                updateAvailable = true;
                log.info("New launcher update available: {} ({})", update.version, update.tagName);
            });
        });
    }

    private Optional<LauncherUpdate> checkGitHubReleases() {
        // Try the "latest" endpoint first
        try {
            String json = EHttpClient.toString(
                    Request.get(GITHUB_API_LATEST)
                            .addHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                            .addHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            );
            LauncherUpdate update = parseReleaseJson(json);
            if (update != null && isNewer(update.version)) {
                return Optional.of(update);
            }
        } catch (Exception e) {
            log.debug("GitHub latest release API failed, trying releases list", e);
        }

        // Fallback: try the releases list endpoint
        try {
            String json = EHttpClient.toString(
                    Request.get(GITHUB_API_RELEASES)
                            .addHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                            .addHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            );
            JsonArray releases = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                LauncherUpdate update = parseReleaseJson(release);
                if (update != null && isNewer(update.version)) {
                    return Optional.of(update);
                }
            }
        } catch (Exception e) {
            log.debug("GitHub releases list API failed", e);
        }

        return Optional.empty();
    }

    private Optional<LauncherUpdate> checkCustomUrl() {
        try {
            String json = EHttpClient.toString(
                    Request.get(customUpdateUrl)
                            .addHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                            .addHeader(HttpHeaders.ACCEPT, "application/json")
            );
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            String tagName = getString(obj, "tag_name");
            String versionStr = tagName != null ? tagName.replaceAll("^v", "") : getString(obj, "version");
            String body = getString(obj, "body");
            String htmlUrl = getString(obj, "html_url");
            String downloadUrl = null;
            String assetName = null;

            if (obj.has("assets")) {
                JsonArray assets = obj.getAsJsonArray("assets");
                if (assets.size() > 0) {
                    JsonObject firstAsset = assets.get(0).getAsJsonObject();
                    downloadUrl = getString(firstAsset, "browser_download_url");
                    assetName = getString(firstAsset, "name");
                }
            }

            if (versionStr != null) {
                try {
                    Version version = Version.parse(versionStr);
                    if (isNewer(version)) {
                        LauncherUpdate update = new LauncherUpdate();
                        update.version = version;
                        update.tagName = tagName != null ? tagName : "v" + versionStr;
                        update.changelog = body;
                        update.downloadUrl = downloadUrl;
                        update.assetName = assetName;
                        update.htmlUrl = htmlUrl;
                        return Optional.of(update);
                    }
                } catch (RuntimeException e) {
                    log.warn("Could not parse version: {}", versionStr, e);
                }
            }
        } catch (Exception e) {
            log.warn("Custom update URL check failed", e);
        }
        return Optional.empty();
    }

    private LauncherUpdate parseReleaseJson(String json) {
        JsonObject release = JsonParser.parseString(json).getAsJsonObject();
        return parseReleaseJson(release);
    }

    private LauncherUpdate parseReleaseJson(JsonObject release) {
        String tagName = getString(release, "tag_name");
        if (tagName == null) {
            return null;
        }

        String versionStr = tagName.replaceAll("^v", "");
        Version version;
        try {
            version = Version.parse(versionStr);
        } catch (RuntimeException e) {
            log.warn("Could not parse version from tag: {}", tagName, e);
            return null;
        }

        String body = getString(release, "body");
        String htmlUrl = getString(release, "html_url");
        boolean prerelease = getBoolean(release, "prerelease");

        LauncherUpdate update = new LauncherUpdate();
        update.version = version;
        update.tagName = tagName;
        update.changelog = body;
        update.htmlUrl = htmlUrl;
        update.prerelease = prerelease;

        if (release.has("assets")) {
            JsonArray assets = release.getAsJsonArray("assets");
            for (JsonElement assetElement : assets) {
                JsonObject asset = assetElement.getAsJsonObject();
                String name = getString(asset, "name");
                if (name != null && name.endsWith(".jar")) {
                    update.downloadUrl = getString(asset, "browser_download_url");
                    update.assetName = name;
                    break;
                }
            }
        }

        return update;
    }

    private boolean isNewer(Version candidate) {
        Version current = LegacyLauncher.getVersion();
        return candidate != null && candidate.isHigherThan(current);
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }

    private static boolean getBoolean(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() && el.getAsBoolean();
    }

    /**
     * Data holder for a launcher update.
     */
    @Getter
    public static class LauncherUpdate {
        private Version version;
        private String tagName;
        private String changelog;
        private String downloadUrl;
        private String assetName;
        private String htmlUrl;
        private boolean prerelease;

        /**
         * Opens the GitHub releases page in the default browser.
         */
        public void openReleasePage() {
            if (htmlUrl != null) {
                net.legacylauncher.util.OS.openLink(htmlUrl);
            }
        }

        @Override
        public String toString() {
            return "LauncherUpdate{" +
                    "version=" + version +
                    ", tagName='" + tagName + '\'' +
                    '}';
        }
    }
}
