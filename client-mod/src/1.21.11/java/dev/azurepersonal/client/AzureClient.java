package dev.azurepersonal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public final class AzureClient implements ClientModInitializer {
    private static final AzureConfig CONFIG = AzureConfig.load();
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(
            Identifier.of("azureclient", "controls")
        );
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.azureclient.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                closeOrOpenMenu(client);
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> AzureHud.render(context, CONFIG));
    }

    private static void closeOrOpenMenu(MinecraftClient client) {
        if (client.currentScreen instanceof AzureMenuScreen) {
            client.setScreen(null);
        } else if (client.currentScreen == null) {
            client.setScreen(new AzureMenuScreen());
        }
    }

    public static AzureConfig config() {
        return CONFIG;
    }

    public static void openControls(MinecraftClient client, AzureMenuScreen parent) {
        client.setScreen(new net.minecraft.client.gui.screen.option.ControlsOptionsScreen(
            parent,
            client.options
        ));
    }

    public static int getChromaColor(long offsetMs) {
        float hue = (float) ((System.currentTimeMillis() + offsetMs) % 4000L) / 4000.0f;
        int rgb = Color.HSBtoRGB(hue, 0.82f, 1.0f);
        return rgb | 0xff000000;
    }
}
