package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class AzureHud {
    private AzureHud() {
    }

    public static void render(DrawContext context, AzureConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null || client.options.hudHidden) return;

        int accent = config.chromaHud ? AzureClient.getChromaColor(0) : config.accentColor();
        int y = 6;

        if (config.showWatermark) {
            int ping = ping(client, player);
            StringBuilder text = new StringBuilder("AZURE  |  1.21.x");
            if (config.showFps) text.append("  |  ").append(client.getCurrentFps()).append(" FPS");
            if (config.showPing && ping >= 0) text.append("  |  ").append(ping).append("ms");
            int width = client.textRenderer.getWidth(text.toString()) + 16;
            chip(context, client, text.toString(), accent, config.hudOpacity, 6, y, width);
            y += 20;
        }
        if (config.showFps && !config.showWatermark) {
            chip(context, client, client.getCurrentFps() + " FPS", 0xfff5f7ff, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showPing) {
            int ping = ping(client, player);
            int color = ping < 60 ? 0xff49d8ce : (ping < 120 ? 0xffffca78 : 0xffff4757);
            chip(context, client, "PING " + Math.max(0, ping) + " ms", color, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showCoordinates) {
            BlockPos pos = player.getBlockPos();
            chip(context, client,
                "XYZ  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ(),
                0xfff5f7ff, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showDirection) {
            chip(context, client,
                "FACING  " + player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT),
                0xffaab8cb, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showServerInfo) {
            String server = client.getCurrentServerEntry() != null
                ? client.getCurrentServerEntry().address
                : "SINGLEPLAYER";
            if (server.length() > 30) server = server.substring(0, 29) + "…";
            chip(context, client, "SERVER  " + server, 0xff8ab4ff, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showHealth) {
            float absorption = player.getAbsorptionAmount();
            String label = String.format(Locale.ROOT, "HEALTH  %.1f%s",
                player.getHealth(), absorption > 0 ? "  +" + (int) absorption : "");
            chip(context, client, label, player.getHealth() <= 6 ? 0xffff4757 : 0xffff7f9f,
                config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showHunger) {
            chip(context, client, String.format(Locale.ROOT, "HUNGER  %d  |  SAT %.1f",
                player.getHungerManager().getFoodLevel(), player.getHungerManager().getSaturationLevel()),
                0xffffca78, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showWorldTime) {
            long time = client.world.getTimeOfDay() % 24000L;
            int hour = (int) ((time / 1000L + 6L) % 24L);
            int minute = (int) ((time % 1000L) * 60L / 1000L);
            chip(context, client, String.format(Locale.ROOT, "TIME  %02d:%02d", hour, minute),
                0xff80e0a0, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showDayCounter) {
            long day = client.world.getTimeOfDay() / 24000L + 1L;
            chip(context, client, "DAY  " + day, 0xffc6a0ff, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showMemory) {
            long total = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long free = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            chip(context, client, "RAM  " + (total - free) + "MB / " + total + "MB",
                0xffaab8cb, config.hudOpacity, 6, y, -1);
        }
    }

    private static int ping(MinecraftClient client, PlayerEntity player) {
        if (client.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }

    private static void chip(DrawContext context, MinecraftClient client, String text,
                             int color, int opacity, int x, int y, int forcedWidth) {
        int width = forcedWidth > 0 ? forcedWidth : client.textRenderer.getWidth(text) + 16;
        int height = 17;
        context.fill(x, y, x + width, y + height, opacity(0xff0b1018, opacity));
        context.fill(x, y, x + 2, y + height, color);
        border(context, x, y, width, height, 0x5534455c);
        context.drawTextWithShadow(client.textRenderer, text, x + 7, y + 5, color);
    }

    public static int opacity(int color, int percent) {
        int alpha = Math.max(0, Math.min(255, percent * 255 / 100));
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
