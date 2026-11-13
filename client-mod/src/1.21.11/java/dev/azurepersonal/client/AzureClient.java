package dev.azurepersonal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AzureClient implements ClientModInitializer {
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
                client.setScreen(client.currentScreen instanceof AzureMenuScreen
                    ? null
                    : new AzureMenuScreen());
            }
        });
    }

    public static void openControls(MinecraftClient client, AzureMenuScreen parent) {
        client.setScreen(new net.minecraft.client.gui.screen.option.ControlsOptionsScreen(
            parent,
            client.options
        ));
    }
}
