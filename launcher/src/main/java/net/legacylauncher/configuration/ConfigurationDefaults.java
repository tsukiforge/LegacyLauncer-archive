package net.legacylauncher.configuration;

import net.legacylauncher.managers.GPUManager;
import net.legacylauncher.managers.JavaManagerConfig;
import net.legacylauncher.ui.FlatLaf;
import net.legacylauncher.util.Direction;
import net.legacylauncher.util.IntegerArray;
import net.legacylauncher.util.MinecraftUtil;
import net.legacylauncher.util.OS;
import net.legacylauncher.util.shared.JavaVersion;
import net.minecraft.launcher.versions.ReleaseType;

import java.lang.ref.WeakReference;
import java.util.*;

public final class ConfigurationDefaults {
    private static WeakReference<ConfigurationDefaults> ref;

    public static ConfigurationDefaults getInstance() {
        ConfigurationDefaults instance;

        if (ref == null || (instance = ref.get()) == null) {
            instance = new ConfigurationDefaults();
            ref = new WeakReference<>(instance);
        }

        return instance;
    }

    private static final int VERSION = 3;
    private final HashMap<String, Object> d = new HashMap<>();

    private ConfigurationDefaults() {
        d.put("settings.version", VERSION);

        d.put("minecraft.gamedir", MinecraftUtil.getDefaultWorkingDirectory().getAbsolutePath());
        d.put("minecraft.gamedir.separate", Configuration.SeparateDirs.NONE.name().toLowerCase(Locale.ROOT));

        d.put("minecraft.size", new IntegerArray(925, 530));
        d.put("minecraft.fullscreen", false);

        for (ReleaseType type : ReleaseType.getDefault()) {
            d.put("minecraft.versions." + type.name().toLowerCase(java.util.Locale.ROOT), true);
        }
        d.put("minecraft.versions.sub." + ReleaseType.SubType.REMOTE.name().toLowerCase(java.util.Locale.ROOT), true);
        d.put("minecraft.versions.sub." + ReleaseType.SubType.OLD_RELEASE.name().toLowerCase(java.util.Locale.ROOT), true);
        d.put("minecraft.versions.only-installed", false);

        d.put("minecraft.jre.type", JavaManagerConfig.Recommended.TYPE);

        d.put("minecraft.javaargs", null);
        d.put("minecraft.args", null);
        d.put("minecraft.improvedargs", true);
        d.put("minecraft.gpu", GPUManager.GPU.DISCRETE.getName());
        if (OS.LINUX.isCurrent()) {
            d.put("minecraft.gamemode", true);
        }

        d.put("minecraft.xmx", "auto");

        d.put("minecraft.servers.promoted", true);
        d.put("minecraft.servers.promoted.ingame", true);

        d.put("minecraft.onlaunch", Configuration.ActionOnLaunch.HIDE);

        d.put("minecraft.crash", true);
        d.put("minecraft.mods.removeUndesirable", true);

        d.put("gui.font", 12);
        d.put("gui.size", new IntegerArray(1000, 600));
//        d.put("gui.systemlookandfeel", false);

        d.putAll(FlatLaf.getDefaults());

        d.put("gui.background", null);

        d.put("gui.logger", Configuration.LoggerType.getDefault());
        d.put("gui.logger.width", 720);
        d.put("gui.logger.height", 500);
        d.put("gui.logger.x", 30);
        d.put("gui.logger.y", 30);

        d.put("gui.notices.enabled", true);
        d.put("notice.promoted", true);
        d.put("notice.enabled", true);

        d.put("gui.direction.loginform", Direction.CENTER);

        d.put("client", UUID.randomUUID());

        d.put("connection.ssl", true);

        if (OS.WINDOWS.isCurrent()) {
            d.put("windows.dxdiag", true);
            d.put("windows.gpuperf", true);
        }

        d.put("bootstrap.switchToBeta", false);

        d.put("experiments.enabled", "none");

        d.put("minecraft.deletePatchy", true);

        // Anime Theme & Background settings
        d.put("gui.background.mode", "anime"); // anime, video, transparent
        d.put("gui.background.anime.url", "https://images.unsplash.com/photo-1567095761054-7a02e69e5c43?auto=format&fit=crop&w=1920&q=80");

        // Auto-Optimize Low-End PC settings
        d.put("optimize.enabled", false);
        d.put("optimize.close.on.start", false);
        d.put("optimize.jvm.args", "auto");
        d.put("optimize.mods.sodium", true);
        d.put("optimize.mods.lithium", true);
        d.put("optimize.mods.ferritecore", true);
        d.put("optimize.mods.optifine", false);
        d.put("optimize.mods.dynamicfps", true);
        d.put("optimize.mods.starlight", true);
        d.put("optimize.mods.krypton", true);
        d.put("optimize.mods.entityculling", true);
        d.put("optimize.mods.iris", true);
        d.put("optimize.mods.immediatelyfast", true);
        d.put("optimize.mods.modernfix", true);
        d.put("optimize.mods.memoryleakfix", true);
        d.put("optimize.mods.lazydfu", true);
        d.put("optimize.mods.noisium", true);
        d.put("optimize.mods.smoothboot", true);
        d.put("optimize.mods.c2me", false);
        d.put("optimize.mods.alternatecurrent", true);
        d.put("optimize.mods.vmp", false);
    }

    public static int getVersion() {
        return 3;
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(d);
    }

    public Object get(String key) {
        return d.get(key);
    }
}
