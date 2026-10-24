package dev.azurepersonal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.Deque;

public final class AzureClient implements ClientModInitializer {
    private static final AzureConfig CONFIG = AzureConfig.load();
    private static KeyBinding menuKey;
    private static KeyBinding freeLookKey;
    private static KeyBinding recipeKey;
    private static KeyBinding toggleSprintKey;
    private static KeyBinding zoomKey;
    private static boolean recipeBookRequested;
    private static boolean sprintToggled;
    private static boolean freeLookActive;
    private static float freeLookYaw;
    private static float freeLookPitch;
    private static float zoomProgress;
    private static final Deque<Long> ATTACK_TIMES = new ArrayDeque<>();
    private static final int[] COLOR_PALETTE = {
        0xfff5f7ff, 0xff49d8ce, 0xff8ab4ff, 0xffffca78,
        0xffff7f9f, 0xffc6a0ff, 0xff80e0a0
    };

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.azureclient"
        ));
        freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.freelook",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.azureclient"
        ));
        recipeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.recipe",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.azureclient"
        ));
        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.sprint",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.azureclient"
        ));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.zoom",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.azureclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (CONFIG.useClientHome && client.currentScreen instanceof TitleScreen) {
                client.setScreen(new AzureHomeScreen());
            }
            while (menuKey.wasPressed()) {
                client.setScreen(client.currentScreen instanceof AzureMenuScreen ? null : new AzureMenuScreen());
            }
            while (freeLookKey.wasPressed()) {
                toggleFreeLook(client);
            }
            while (recipeKey.wasPressed()) {
                if (client.currentScreen == null) openRecipeBook(client);
            }
            while (toggleSprintKey.wasPressed()) {
                sprintToggled = !sprintToggled;
                CONFIG.toggleSprint = sprintToggled;
                CONFIG.save();
            }
            if (client.player == null || client.world == null) {
                freeLookActive = false;
            } else if (client.currentScreen == null) {
                if (sprintToggled) {
                    client.player.setSprinting(true);
                } else if (CONFIG.autoSprint) {
                    client.player.setSprinting(client.options.forwardKey.isPressed() &&
                        !client.options.sneakKey.isPressed());
                }
            }
            float zoomTarget = client.player != null && client.currentScreen == null && zoomKey.isPressed()
                ? 1.0f : 0.0f;
            zoomProgress += (zoomTarget - zoomProgress) * 0.35f;
            if (Math.abs(zoomTarget - zoomProgress) < 0.005f) zoomProgress = zoomTarget;
            AzureInventorySorter.tick(client);
        });
        sprintToggled = CONFIG.toggleSprint;
        HudRenderCallback.EVENT.register((context, tickDelta) -> AzureHud.render(context, CONFIG));
    }

    public static AzureConfig config() {
        return CONFIG;
    }

    public static double zoomFactor() {
        return 1.0 + (CONFIG.zoomLevel - 1.0) * zoomProgress;
    }

    public static void setClientHomeEnabled(boolean enabled) {
        CONFIG.useClientHome = enabled;
        CONFIG.save();
        MinecraftClient client = MinecraftClient.getInstance();
        if (enabled) {
            client.setScreen(new AzureHomeScreen());
        } else {
            client.setScreen(new TitleScreen());
        }
    }

    public static boolean consumeRecipeBookRequest() {
        boolean requested = recipeBookRequested;
        recipeBookRequested = false;
        return requested;
    }

    public static void openRecipeBook(MinecraftClient client) {
        if (client.player != null) {
            recipeBookRequested = true;
            client.setScreen(new InventoryScreen(client.player));
        }
    }

    public static boolean isSprintToggled() {
        return sprintToggled;
    }

    public static void recordAttack() {
        long now = System.currentTimeMillis();
        ATTACK_TIMES.addLast(now);
        while (!ATTACK_TIMES.isEmpty() && now - ATTACK_TIMES.peekFirst() > 1000) {
            ATTACK_TIMES.removeFirst();
        }
    }

    public static int clicksPerSecond() {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!ATTACK_TIMES.isEmpty() && ATTACK_TIMES.peekFirst() < cutoff) {
            ATTACK_TIMES.removeFirst();
        }
        return ATTACK_TIMES.size();
    }

    public static void setSprintToggled(boolean enabled) {
        sprintToggled = enabled;
        CONFIG.toggleSprint = enabled;
        if (!enabled && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.setSprinting(false);
        }
        CONFIG.save();
    }

    public static void setAutoSprint(boolean enabled) {
        CONFIG.autoSprint = enabled;
        CONFIG.save();
    }

    public static boolean isFreeLookActive() {
        return freeLookActive;
    }

    public static float freeLookYaw() {
        return freeLookYaw;
    }

    public static float freeLookPitch() {
        return freeLookPitch;
    }

    public static void setFreeLookRotation(float yaw, float pitch) {
        freeLookYaw = yaw;
        freeLookPitch = pitch;
    }

    public static int nextColor(int current) {
        for (int index = 0; index < COLOR_PALETTE.length; index++) {
            if (COLOR_PALETTE[index] == current) return COLOR_PALETTE[(index + 1) % COLOR_PALETTE.length];
        }
        return COLOR_PALETTE[0];
    }

    public static String colorName(int color) {
        return switch (color) {
            case 0xff49d8ce -> "AQUA";
            case 0xff8ab4ff -> "BLUE";
            case 0xffffca78 -> "GOLD";
            case 0xffff7f9f -> "ROSE";
            case 0xffc6a0ff -> "VIOLET";
            case 0xff80e0a0 -> "MINT";
            default -> "WHITE";
        };
    }

    public static float clampScale(float scale) {
        return Math.max(0.75f, Math.min(2.0f, scale));
    }

    public static void toggleFreeLook(MinecraftClient client) {
        if (client.player == null) {
            freeLookActive = false;
            return;
        }
        freeLookActive = !freeLookActive;
        if (freeLookActive) {
            freeLookYaw = client.player.getYaw();
            freeLookPitch = client.player.getPitch();
        }
    }
}
