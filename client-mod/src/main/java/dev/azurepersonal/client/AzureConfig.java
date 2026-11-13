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

    // General & Launcher UI
    public boolean useClientHome = true;
    public String hudPreset = "custom";
    public int theme = 0; // 0=Azure, 1=Ember, 2=Violet, 3=Rose, 4=Mint, 5=Frost, 6=Chroma
    public int menuOpacity = 94;
    public int hudOpacity = 72;
    public boolean chromaHud = false;

    // Watermark HUD
    public boolean showWatermark = true;
    public int watermarkStyle = 0; // 0=Modern Pill, 1=Minimal Text, 2=Chroma Wave
    public boolean watermarkShowFps = true;
    public boolean watermarkShowPing = true;
    public boolean watermarkShowVersion = true;
    public boolean watermarkShowUser = false;
    public int watermarkX = 10;
    public int watermarkY = 10;
    public float watermarkScale = 1.0f;
    public int watermarkColor = 0xff49d8ce;

    // Keystrokes (Keycaps)
    public boolean showKeycaps = true;
    public boolean showMouseButtons = true;
    public boolean showSpacebar = true;
    public boolean showCpsOnKeys = true;
    public int keycapsX = -1;
    public int keycapsY = -1;
    public float keycapsScale = 1.0f;
    public int keycapsColor = 0xff36d1dc;

    // Dual CPS Counter
    public boolean showCps = true;
    public boolean showRightCps = true;
    public int cpsX = -1;
    public int cpsY = -1;
    public float cpsScale = 1.0f;
    public int cpsColor = 0xff49d8ce;

    // Ping / Latency
    public boolean showPing = true;
    public int pingX = -1;
    public int pingY = -1;
    public float pingScale = 1.0f;
    public int pingColor = 0xff8ab4ff;

    // FPS Counter
    public boolean showFps = true;
    public int fpsX = 10;
    public int fpsY = 28;
    public float fpsScale = 1.0f;
    public int fpsColor = 0xfff5f7ff;

    // Coordinates & Compass
    public boolean showCoordinates = true;
    public boolean showBiome = true;
    public boolean showNetherCoords = false;
    public int coordsX = 10;
    public int coordsY = 46;
    public float coordsScale = 1.0f;
    public int coordsColor = 0xfff5f7ff;

    public boolean showDirection = true;
    public int directionX = 10;
    public int directionY = 64;
    public float directionScale = 1.0f;
    public int directionColor = 0xfff5f7ff;

    // Equipment & Status
    public boolean showArmor = true;
    public boolean showArmorDurability = true;
    public int armorX = 10;
    public int armorY = -1;
    public float armorScale = 1.0f;

    public boolean showPotions = true;
    public int potionsX = -1;
    public int potionsY = 42;
    public float potionsScale = 1.0f;

    // PvP & Utilities
    public boolean showSpeedometer = false;
    public int speedX = 10;
    public int speedY = 82;
    public float speedScale = 1.0f;
    public int speedColor = 0xffffca78;

    public boolean showCombo = true;
    public int comboX = -1;
    public int comboY = -1;
    public float comboScale = 1.0f;
    public int comboColor = 0xffff7f9f;

    public boolean showReach = true;
    public int reachX = -1;
    public int reachY = -1;
    public float reachScale = 1.0f;
    public int reachColor = 0xffc6a0ff;

    public boolean showClock = false;
    public boolean clock24h = true;
    public int clockX = 10;
    public int clockY = 100;
    public float clockScale = 1.0f;
    public int clockColor = 0xff80e0a0;

    public boolean showMemory = false;
    public int memoryX = 10;
    public int memoryY = 118;
    public float memoryScale = 1.0f;
    public int memoryColor = 0xffaab8cb;

    // Movement & Keys
    public boolean toggleSprint;
    public boolean toggleSneak;
    public boolean autoSprint;
    public boolean showToggleSprintBadge = true;

    // Visuals & Crosshair
    public boolean customCrosshair = true;
    public int crosshairStyle = 0; // 0=Cross, 1=Dot, 2=Circle, 3=Arrow, 4=Box
    public int crosshairSize = 7;
    public int crosshairGap = 4;
    public int crosshairThickness = 2;
    public int crosshairColor = 0xfff5f7ff;
    public boolean crosshairHitColor = true;
    public int crosshairHitColorValue = 0xffff4757;
    public boolean crosshairOutline = true;
    public boolean crosshairCenterDot;
    public int crosshairOpacity = 100;

    // World & Render
    public boolean fullBright;
    public int fullBrightLevel = 200;
    public boolean blockOutline = true;
    public int blockOutlineColor = 0xff36d1dc;
    public boolean showBlockOverlay = true;
    public int blockOverlayColor = 0xfff5f7ff;
    public int blockOverlayX = 10;
    public int blockOverlayY = 136;
    public float blockOverlayScale = 1.0f;

    public int zoomLevel = 4;
    public boolean lowFire = true;
    public boolean noHurtCam = true;

    // Cosmetics
    public boolean azureCrownEquipped = true;
    public int crownColor = 0xff49d8ce;

    public static AzureConfig load() {
        try {
            if (Files.isRegularFile(CONFIG_FILE)) {
                JsonObject merged = GSON.toJsonTree(new AzureConfig()).getAsJsonObject();
                JsonObject saved = JsonParser.parseString(Files.readString(CONFIG_FILE)).getAsJsonObject();
                saved.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
                AzureConfig loaded = GSON.fromJson(merged, AzureConfig.class);
                if (loaded != null) {
                    loaded.crosshairStyle = Math.max(0, Math.min(4, loaded.crosshairStyle));
                    loaded.crosshairSize = Math.max(2, Math.min(20, loaded.crosshairSize));
                    loaded.crosshairGap = Math.max(0, Math.min(16, loaded.crosshairGap));
                    loaded.crosshairThickness = Math.max(1, Math.min(8, loaded.crosshairThickness));
                    loaded.crosshairOpacity = Math.max(20, Math.min(100, loaded.crosshairOpacity));
                    loaded.theme = Math.max(0, Math.min(6, loaded.theme));
                    loaded.fullBrightLevel = Math.max(100, Math.min(500, loaded.fullBrightLevel));
                    loaded.zoomLevel = Math.max(2, Math.min(12, loaded.zoomLevel));
                    loaded.menuOpacity = Math.max(35, Math.min(100, loaded.menuOpacity));
                    loaded.hudOpacity = Math.max(20, Math.min(100, loaded.hudOpacity));
                    if (!isHudPreset(loaded.hudPreset)) loaded.hudPreset = "custom";
                    loaded.fpsScale = clampScale(loaded.fpsScale);
                    loaded.keycapsScale = clampScale(loaded.keycapsScale);
                    loaded.blockOverlayScale = clampScale(loaded.blockOverlayScale);
                    loaded.watermarkScale = clampScale(loaded.watermarkScale);
                    loaded.cpsScale = clampScale(loaded.cpsScale);
                    loaded.pingScale = clampScale(loaded.pingScale);
                    loaded.armorScale = clampScale(loaded.armorScale);
                    loaded.potionsScale = clampScale(loaded.potionsScale);
                    loaded.coordsScale = clampScale(loaded.coordsScale);
                    loaded.directionScale = clampScale(loaded.directionScale);
                    loaded.speedScale = clampScale(loaded.speedScale);
                    loaded.comboScale = clampScale(loaded.comboScale);
                    loaded.reachScale = clampScale(loaded.reachScale);
                    loaded.clockScale = clampScale(loaded.clockScale);
                    loaded.memoryScale = clampScale(loaded.memoryScale);
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
            case "pvp", "bedwars", "smp", "minimal", "custom" -> true;
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

    public static float clampScale(float scale) {
        if (!Float.isFinite(scale)) return 1.0f;
        return Math.max(0.6f, Math.min(2.5f, scale));
    }

    public int accentColor() {
        if (theme == 6) {
            return AzureClient.getChromaColor(0);
        }
        return switch (theme) {
            case 1 -> 0xffff8a5b; // Ember
            case 2 -> 0xffa78bfa; // Violet
            case 3 -> 0xfff472b6; // Rose
            case 4 -> 0xff80e0a0; // Mint
            case 5 -> 0xffe6edf7; // Frost
            default -> 0xff49d8ce; // Azure
        };
    }

    public void applyHudPreset(String preset) {
        switch (preset.toLowerCase(Locale.ROOT)) {
            case "pvp" -> {
                showWatermark = true;
                watermarkStyle = 0;
                watermarkShowFps = true;
                watermarkShowPing = true;
                showFps = true;
                showKeycaps = true;
                showMouseButtons = true;
                showSpacebar = true;
                showCpsOnKeys = true;
                showCps = true;
                showRightCps = true;
                showPing = true;
                showArmor = true;
                showArmorDurability = true;
                showCombo = true;
                showReach = true;
                showCoordinates = false;
                showDirection = false;
                showPotions = true;
                showSpeedometer = false;
                showClock = false;
                showMemory = false;
                lowFire = true;
                noHurtCam = true;
                toggleSprint = true;
                fullBright = true;
                customCrosshair = true;
                crosshairStyle = 1; // Dot
                crosshairCenterDot = true;
            }
            case "bedwars" -> {
                showWatermark = true;
                watermarkStyle = 0;
                showFps = true;
                showKeycaps = true;
                showMouseButtons = true;
                showCps = true;
                showRightCps = true;
                showPing = true;
                showArmor = true;
                showCoordinates = true;
                showDirection = true;
                showCombo = true;
                showReach = true;
                lowFire = true;
                noHurtCam = true;
                toggleSprint = true;
                fullBright = true;
            }
            case "smp" -> {
                showWatermark = true;
                watermarkStyle = 1;
                showFps = true;
                showKeycaps = false;
                showCps = false;
                showPing = true;
                showArmor = true;
                showArmorDurability = true;
                showCoordinates = true;
                showBiome = true;
                showNetherCoords = true;
                showDirection = true;
                showPotions = true;
                showClock = true;
                showBlockOverlay = true;
                fullBright = true;
            }
            case "minimal" -> {
                showWatermark = true;
                watermarkStyle = 1;
                showFps = true;
                showCps = true;
                showPing = true;
                showKeycaps = false;
                showArmor = false;
                showCoordinates = false;
                showDirection = false;
                showPotions = false;
                showBlockOverlay = false;
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
