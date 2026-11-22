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
import java.util.ArrayDeque;
import java.util.Deque;

public final class AzureClient implements ClientModInitializer {
    private static final AzureConfig CONFIG = AzureConfig.load();
    private static KeyBinding menuKey;

    private static final Deque<Long> ATTACK_TIMES = new ArrayDeque<>();
    private static final Deque<Long> USE_TIMES = new ArrayDeque<>();
    private static boolean lastAttackDown;
    private static boolean lastUseDown;
    private static double currentSpeed;
    private static double prevPlayerX;
    private static double prevPlayerZ;
    private static boolean positionKnown;

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

            trackClicks(client);
            trackMovement(client);
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

    private static void trackClicks(MinecraftClient client) {
        boolean inGame = client.currentScreen == null;
        boolean attackDown = inGame && client.options.attackKey.isPressed();
        boolean useDown = inGame && client.options.useKey.isPressed();
        if (attackDown && !lastAttackDown) recordClick(ATTACK_TIMES);
        if (useDown && !lastUseDown) recordClick(USE_TIMES);
        lastAttackDown = attackDown;
        lastUseDown = useDown;
    }

    private static void recordClick(Deque<Long> times) {
        long now = System.currentTimeMillis();
        times.addLast(now);
        while (!times.isEmpty() && now - times.peekFirst() > 1000L) {
            times.removeFirst();
        }
    }

    private static int clicks(Deque<Long> times) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!times.isEmpty() && times.peekFirst() < cutoff) {
            times.removeFirst();
        }
        return times.size();
    }

    public static int clicksPerSecond() {
        return clicks(ATTACK_TIMES);
    }

    public static int rightClicksPerSecond() {
        return clicks(USE_TIMES);
    }

    private static void trackMovement(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            positionKnown = false;
            currentSpeed = 0.0;
            return;
        }
        double x = client.player.getX();
        double z = client.player.getZ();
        if (positionKnown) {
            double dx = x - prevPlayerX;
            double dz = z - prevPlayerZ;
            currentSpeed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        }
        prevPlayerX = x;
        prevPlayerZ = z;
        positionKnown = true;
    }

    public static double speedBlocksPerSec() {
        return currentSpeed;
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
