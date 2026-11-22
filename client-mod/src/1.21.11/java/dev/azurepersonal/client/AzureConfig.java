package dev.azurepersonal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AzureConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("azure-client.json");

    public int theme = 0;
    public int menuOpacity = 94;
    public int hudOpacity = 72;
    public boolean chromaHud = false;

    // HUD modules
    public boolean showWatermark = true;
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showCoordinates = true;
    public boolean showDirection = true;
    public boolean showServerInfo = true;
    public boolean showHealth = true;
    public boolean showHunger = true;
    public boolean showWorldTime = false;
    public boolean showDayCounter = false;
    public boolean showMemory = false;
    public String hudPreset = "custom";

    public static AzureConfig load() {
        try {
            if (Files.isRegularFile(CONFIG_FILE)) {
                JsonObject merged = GSON.toJsonTree(new AzureConfig()).getAsJsonObject();
                JsonObject saved = JsonParser.parseString(Files.readString(CONFIG_FILE)).getAsJsonObject();
                saved.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
                AzureConfig loaded = GSON.fromJson(merged, AzureConfig.class);
                if (loaded != null) {
                    loaded.theme = Math.max(0, Math.min(6, loaded.theme));
                    loaded.menuOpacity = Math.max(35, Math.min(100, loaded.menuOpacity));
                    loaded.hudOpacity = Math.max(20, Math.min(100, loaded.hudOpacity));
                    return loaded;
                }
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[Azure Personal] Could not read client settings: " + exception.getMessage());
        }
        return new AzureConfig();
    }

    public int accentColor() {
        if (theme == 6) {
            return AzureClient.getChromaColor(0);
        }
        return switch (theme) {
            case 1 -> 0xffff8a5b;
            case 2 -> 0xffa78bfa;
            case 3 -> 0xfff472b6;
            case 4 -> 0xff80e0a0;
            case 5 -> 0xffe6edf7;
            default -> 0xff49d8ce;
        };
    }

    public void applyPreset(String preset) {
        switch (preset.toLowerCase(java.util.Locale.ROOT)) {
            case "pvp" -> {
                showWatermark = true;
                showFps = true;
                showPing = true;
                showCoordinates = true;
                showDirection = false;
                showServerInfo = true;
                showHealth = true;
                showHunger = true;
                showWorldTime = false;
                showDayCounter = false;
                showMemory = false;
            }
            case "bedwars" -> {
                showWatermark = true;
                showFps = true;
                showPing = true;
                showCoordinates = true;
                showDirection = true;
                showServerInfo = true;
                showHealth = true;
                showHunger = false;
                showWorldTime = false;
                showDayCounter = false;
                showMemory = false;
            }
            case "smp" -> {
                showWatermark = true;
                showFps = true;
                showPing = false;
                showCoordinates = true;
                showDirection = true;
                showServerInfo = false;
                showHealth = false;
                showHunger = false;
                showWorldTime = true;
                showDayCounter = true;
                showMemory = false;
            }
            case "minimal" -> {
                showWatermark = true;
                showFps = true;
                showPing = false;
                showCoordinates = false;
                showDirection = false;
                showServerInfo = false;
                showHealth = false;
                showHunger = false;
                showWorldTime = false;
                showDayCounter = false;
                showMemory = false;
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
