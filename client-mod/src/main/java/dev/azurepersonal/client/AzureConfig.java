package dev.azurepersonal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AzureConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("azure-client.json");

    public boolean useClientHome = true;
    public boolean showFps = true;
    public boolean showKeycaps = true;
    public boolean showBlockOverlay = true;
    public boolean showArmor = true;
    public boolean showCoordinates = true;
    public boolean showDirection = true;
    public boolean showCps = true;
    public boolean customCrosshair = true;
    public boolean toggleSprint;
    public boolean autoSprint;
    public boolean blockOutline = true;
    public boolean fullBright;
    public boolean crosshairHitColor = true;
    public boolean crosshairOutline = true;
    public boolean crosshairCenterDot;
    public String hudPreset = "custom";
    public int crosshairStyle = 0;
    public int crosshairSize = 7;
    public int crosshairGap = 4;
    public int crosshairThickness = 2;
    public int crosshairColor = 0xfff5f7ff;
    public int crosshairHitColorValue = 0xff49d8ce;
    public int crosshairOpacity = 100;
    public int blockOutlineColor = 0xff36d1dc;
    public int theme = 0;
    public int fullBrightLevel = 200;
    public int zoomLevel = 4;
    public int menuOpacity = 94;
    public int hudOpacity = 72;
    public int fpsColor = 0xfff5f7ff;
    public int keycapsColor = 0xff36d1dc;
    public int blockOverlayColor = 0xfff5f7ff;
    public int fpsX = 10;
    public int fpsY = 10;
    public int keycapsX = -1;
    public int keycapsY = -1;
    public int blockOverlayX = 10;
    public int blockOverlayY = 30;
    public float fpsScale = 1.0f;
    public float keycapsScale = 1.0f;
    public float blockOverlayScale = 1.0f;

    public static AzureConfig load() {
        try {
            if (Files.isRegularFile(CONFIG_FILE)) {
                JsonObject merged = GSON.toJsonTree(new AzureConfig()).getAsJsonObject();
                JsonObject saved = JsonParser.parseString(Files.readString(CONFIG_FILE)).getAsJsonObject();
                saved.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
                AzureConfig loaded = GSON.fromJson(merged, AzureConfig.class);
                if (loaded != null) {
                    loaded.crosshairStyle = Math.max(0, Math.min(2, loaded.crosshairStyle));
                    loaded.crosshairSize = Math.max(3, Math.min(16, loaded.crosshairSize));
                    loaded.crosshairGap = Math.max(1, Math.min(12, loaded.crosshairGap));
                    loaded.crosshairThickness = Math.max(1, Math.min(6, loaded.crosshairThickness));
                    loaded.crosshairOpacity = Math.max(20, Math.min(100, loaded.crosshairOpacity));
                    loaded.theme = Math.max(0, Math.min(5, loaded.theme));
                    loaded.fullBrightLevel = Math.max(100, Math.min(300, loaded.fullBrightLevel));
                    loaded.zoomLevel = Math.max(2, Math.min(10, loaded.zoomLevel));
                    loaded.menuOpacity = Math.max(35, Math.min(100, loaded.menuOpacity));
                    loaded.hudOpacity = Math.max(20, Math.min(100, loaded.hudOpacity));
                    if (!isHudPreset(loaded.hudPreset)) loaded.hudPreset = "custom";
                    loaded.fpsScale = clampScale(loaded.fpsScale);
                    loaded.keycapsScale = clampScale(loaded.keycapsScale);
                    loaded.blockOverlayScale = clampScale(loaded.blockOverlayScale);
                    return loaded;
                }
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[Azure Personal] Could not read client settings: " + exception.getMessage());
        }
        return new AzureConfig();
    }

    private static boolean isHudPreset(String preset) {
        return preset != null && switch (preset.toLowerCase(Locale.ROOT)) {
            case "pvp", "smp", "minimal", "custom" -> true;
            default -> false;
        };
    }

    public static String instanceLabel() {
        Path gameDirectory = FabricLoader.getInstance().getGameDir().getFileName();
        if (gameDirectory == null) return "CURRENT INSTANCE";
        String directoryName = gameDirectory.toString();
        String shortName = directoryName.length() > 8 ? directoryName.substring(0, 8) : directoryName;
        return "INSTANCE  ·  " + shortName.toUpperCase(Locale.ROOT);
    }

    private static float clampScale(float scale) {
        if (!Float.isFinite(scale)) return 1.0f;
        return Math.max(0.75f, Math.min(2.0f, scale));
    }

    public int accentColor() {
        return switch (theme) {
            case 1 -> 0xffff8a5b;
            case 2 -> 0xffa78bfa;
            case 3 -> 0xfff472b6;
            case 4 -> 0xff80e0a0;
            case 5 -> 0xffe6edf7;
            default -> 0xff49d8ce;
        };
    }

    public void applyHudPreset(String preset) {
        switch (preset) {
            case "pvp" -> {
                showFps = true;
                showKeycaps = true;
                showBlockOverlay = true;
                showArmor = false;
                showCoordinates = false;
                showDirection = false;
                showCps = true;
            }
            case "smp" -> {
                showFps = true;
                showKeycaps = true;
                showBlockOverlay = true;
                showArmor = true;
                showCoordinates = true;
                showDirection = true;
                showCps = true;
            }
            case "minimal" -> {
                showFps = true;
                showKeycaps = false;
                showBlockOverlay = false;
                showArmor = false;
                showCoordinates = false;
                showDirection = false;
                showCps = true;
            }
            default -> preset = "custom";
        }
        hudPreset = preset;
        save();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(this));
        } catch (IOException exception) {
            System.err.println("[Azure Personal] Could not save client settings: " + exception.getMessage());
        }
    }
}
