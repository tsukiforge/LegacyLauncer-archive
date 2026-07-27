package net.legacylauncher.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.configuration.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for auto-optimizing Minecraft for low-end PCs.
 * Handles downloading optimization mods from Modrinth API and configuring JVM arguments.
 */
@Slf4j
public class OptimizationService {
    private static final String MODS_DIR = "mods";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 60000;
    private static final String MODRINTH_API = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "LegacyLauncher/1.169 (Auto-Optimize; github.com/turikhay/LegacyLauncher)";

    private final LegacyLauncher launcher;

    public OptimizationService(LegacyLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * Check if auto-optimize is enabled in settings.
     */
    public boolean isEnabled() {
        return launcher.getSettings().getBoolean("optimize.enabled");
    }

    /**
     * Get the optimized JVM arguments for low-end PCs.
     */
    public String getOptimizedJvmArgs() {
        String setting = launcher.getSettings().get("optimize.jvm.args");
        if (setting == null || setting.isEmpty() || "auto".equals(setting)) {
            return buildAutoJvmArgs();
        }
        return setting;
    }

    /**
     * Build auto-configured JVM arguments for low-end PCs.
     */
    private String buildAutoJvmArgs() {
        List<String> args = new ArrayList<>();

        // Memory: Use minimal heap to save RAM
        args.add("-Xmx1G");
        args.add("-Xms512M");

        // Use G1GC for better memory management
        args.add("-XX:+UseG1GC");
        args.add("-XX:G1HeapRegionSize=4M");
        args.add("-XX:+UseStringDeduplication");
        args.add("-XX:+DisableExplicitGC");

        // Optimize for low memory
        args.add("-XX:MaxGCPauseMillis=200");
        args.add("-XX:ParallelGCThreads=2");
        args.add("-XX:ConcGCThreads=1");

        // Reduce startup time
        args.add("-Djava.awt.headless=true");

        return String.join(" ", args);
    }

    /**
     * Check if the launcher should close on game start.
     */
    public boolean shouldCloseOnStart() {
        return launcher.getSettings().getBoolean("optimize.close.on.start");
    }

    /**
     * Download optimization mods for the specified Minecraft version.
     * Uses Modrinth API v2 to fetch download URLs.
     */
    public CompletableFuture<Void> downloadOptimizationMods(String mcVersion) {
        Configuration settings = launcher.getSettings();
        List<CompletableFuture<Void>> downloads = new ArrayList<>();

        // Normalize MC version (e.g., "1.20.1" -> "1.20.1")
        String normalizedVersion = normalizeMcVersion(mcVersion);

        if (settings.getBoolean("optimize.mods.sodium")) {
            downloads.add(downloadFromModrinth("sodium", "Sodium", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.lithium")) {
            downloads.add(downloadFromModrinth("lithium", "Lithium", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.ferritecore")) {
            downloads.add(downloadFromModrinth("ferrite-core", "FerriteCore", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.optifine")) {
            downloads.add(downloadOptiFine(normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.dynamicfps")) {
            downloads.add(downloadFromModrinth("dynamic-fps", "Dynamic FPS", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.starlight")) {
            downloads.add(downloadFromModrinth("starlight", "Starlight", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.krypton")) {
            downloads.add(downloadFromModrinth("krypton", "Krypton", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.entityculling")) {
            downloads.add(downloadFromModrinth("entityculling", "EntityCulling", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.iris")) {
            downloads.add(downloadFromModrinth("iris", "Iris Shaders", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.immediatelyfast")) {
            downloads.add(downloadFromModrinth("immediatelyfast", "ImmediatelyFast", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.modernfix")) {
            downloads.add(downloadFromModrinth("modernfix", "ModernFix", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.memoryleakfix")) {
            downloads.add(downloadFromModrinth("memoryleakfix", "MemoryLeakFix", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.lazydfu")) {
            downloads.add(downloadFromModrinth("lazydfu", "LazyDFU", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.noisium")) {
            downloads.add(downloadFromModrinth("noisium", "Noisium", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.smoothboot")) {
            downloads.add(downloadFromModrinth("smoothboot-fabric", "Smooth Boot", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.c2me")) {
            downloads.add(downloadFromModrinth("c2me", "C2ME (Chunk Management)", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.alternatecurrent")) {
            downloads.add(downloadFromModrinth("alternate-current", "Alternate Current", normalizedVersion));
        }

        if (settings.getBoolean("optimize.mods.vmp")) {
            downloads.add(downloadFromModrinth("vmp", "Very Many Players", normalizedVersion));
        }

        return CompletableFuture.allOf(downloads.toArray(new CompletableFuture[0]));
    }

    /**
     * Normalize Minecraft version for API queries.
     */
    private String normalizeMcVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.20.1";
        }
        // Remove snapshot appendix (e.g., "1.20.1-SNAPSHOT" -> "1.20.1")
        int dashIndex = version.indexOf('-');
        if (dashIndex > 0) {
            version = version.substring(0, dashIndex);
        }
        return version;
    }

    /**
     * Download a mod from Modrinth by slug.
     * Queries the Modrinth APIv2 for versions matching the MC version.
     */
    private CompletableFuture<Void> downloadFromModrinth(String slug, String displayName, String mcVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                File mcDir = new File(launcher.getSettings().get("minecraft.gamedir"));
                File modsDir = new File(mcDir, MODS_DIR);
                if (!modsDir.exists()) {
                    modsDir.mkdirs();
                }

                log.info("Looking up {} (slug: {}) for MC {}", displayName, slug, mcVersion);

                // Step 1: Query Modrinth API for versions (URL-encode the JSON arrays)
                String loadersParam = URLEncoder.encode("[\"fabric\"]", "UTF-8");
                String versionsParam = URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8");
                String apiUrl = MODRINTH_API + "/project/" + slug + "/version"
                        + "?loaders=" + loadersParam
                        + "&game_versions=" + versionsParam;

                String jsonResponse = httpGetString(apiUrl);
                if (jsonResponse == null || jsonResponse.isEmpty()) {
                    log.warn("Empty response from Modrinth for {} version {}", displayName, mcVersion);
                    return;
                }

                // Step 2: Parse JSON array to find first version with a primary file
                JsonArray versions = JsonParser.parseString(jsonResponse).getAsJsonArray();
                if (versions.size() == 0) {
                    log.warn("No {} versions found for MC {}", displayName, mcVersion);
                    return;
                }

                String downloadUrl = null;
                String fileName = null;

                for (JsonElement versionElement : versions) {
                    JsonObject versionObj = versionElement.getAsJsonObject();
                    JsonArray files = versionObj.getAsJsonArray("files");

                    if (files != null && files.size() > 0) {
                        // Prefer primary file, otherwise use first file
                        for (JsonElement fileElement : files) {
                            JsonObject fileObj = fileElement.getAsJsonObject();
                            boolean isPrimary = fileObj.has("primary") && fileObj.get("primary").getAsBoolean();
                            if (isPrimary || downloadUrl == null) {
                                downloadUrl = fileObj.get("url").getAsString();
                                fileName = fileObj.get("filename").getAsString();
                                if (isPrimary) break;
                            }
                        }
                    }
                    if (downloadUrl != null) break;
                }

                if (downloadUrl == null || fileName == null) {
                    log.warn("Could not find downloadable file for {} (MC {})", displayName, mcVersion);
                    return;
                }

                // Step 3: Download to mods folder with proper filename
                File targetFile = new File(modsDir, fileName);

                if (targetFile.exists()) {
                    log.debug("Mod already exists: {} ({} bytes)", fileName, targetFile.length());
                    return;
                }

                log.info("Downloading {} v{} from Modrinth...", displayName, mcVersion);
                log.debug("Download URL: {}", downloadUrl);

                downloadFile(downloadUrl, targetFile);

                log.info("Successfully downloaded {} -> {}", displayName, targetFile.getName());

            } catch (Exception e) {
                log.error("Failed to download mod {} (slug: {}): {}", displayName, slug, e.toString());
                log.debug("Detailed error", e);
            }
        });
    }

    /**
     * Download OptiFine from GitHub releases.
     * Uses the official OptiFine GitHub releases API.
     */
    private CompletableFuture<Void> downloadOptiFine(String mcVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                File mcDir = new File(launcher.getSettings().get("minecraft.gamedir"));
                File modsDir = new File(mcDir, MODS_DIR);
                if (!modsDir.exists()) {
                    modsDir.mkdirs();
                }

                log.info("Looking up OptiFine for MC {}", mcVersion);

                // Use GitHub releases API to find OptiFine versions
                String apiUrl = "https://api.github.com/repos/sp614x/optifine/releases";
                String jsonResponse = httpGetString(apiUrl);

                if (jsonResponse == null || jsonResponse.isEmpty()) {
                    log.warn("Could not fetch OptiFine releases from GitHub");
                    return;
                }

                JsonArray releases = JsonParser.parseString(jsonResponse).getAsJsonArray();

                // Find a release that matches the requested MC version
                String downloadUrl = null;
                String fileName = null;

                for (JsonElement releaseElement : releases) {
                    JsonObject release = releaseElement.getAsJsonObject();
                    String tagName = release.get("tag_name").getAsString();
                    boolean isPrerelease = release.has("prerelease") && release.get("prerelease").getAsBoolean();

                    if (isPrerelease) continue;

                    // Tag format is usually like "HD_U_H6" or similar, not MC version specific
                    // Check assets for MC version hint in filenames
                    JsonArray assets = release.getAsJsonArray("assets");
                    if (assets == null) continue;

                    for (JsonElement assetElement : assets) {
                        JsonObject asset = assetElement.getAsJsonObject();
                        String assetName = asset.get("name").getAsString();

                        // Match by MC version (e.g., "OptiFine_1.20.1_HD_U_H6.jar")
                        if (assetName.contains(mcVersion)) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            fileName = assetName;
                            break;
                        }
                    }

                    if (downloadUrl != null) break;
                }

                if (downloadUrl == null || fileName == null) {
                    // Fallback: try direct download from optifine.net
                    log.warn("OptiFine not found on GitHub for MC {}, trying alternative source", mcVersion);
                    log.warn("Please download OptiFine manually from: https://optifine.net/downloads");
                    return;
                }

                File targetFile = new File(modsDir, fileName);
                if (targetFile.exists()) {
                    log.debug("OptiFine already exists: {}", fileName);
                    return;
                }

                log.info("Downloading OptiFine for MC {}...", mcVersion);
                downloadFile(downloadUrl, targetFile);
                log.info("Successfully downloaded OptiFine -> {}", fileName);

            } catch (Exception e) {
                log.error("Failed to download OptiFine: {}", e.toString());
                log.debug("Detailed error", e);
            }
        });
    }

    /**
     * Perform an HTTP GET request and return the response body as a String.
     */
    private static String httpGetString(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            log.warn("HTTP {} for URL: {}", responseCode, urlStr);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[8192];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
        }
        return sb.toString();
    }

    /**
     * Download a file from URL and save to disk.
     */
    private static void downloadFile(String fileUrl, File destination) throws Exception {
        URL url = URI.create(fileUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Download failed with HTTP code: " + responseCode);
        }

        long contentLength = conn.getContentLengthLong();
        log.debug("Downloading {} ({} bytes)...", destination.getName(), contentLength);

        Path parent = destination.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }

        log.info("Downloaded: {} -> {} ({} bytes)",
                destination.getName(), destination.getAbsolutePath(), destination.length());
    }
}
