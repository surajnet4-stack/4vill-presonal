package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.time.LocalTime;
import java.util.Collection;
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
        if (config.showCps) {
            chip(context, client, "CPS  " + AzureClient.clicksPerSecond() + "  |  "
                + AzureClient.rightClicksPerSecond(), 0xff8ab4ff, config.hudOpacity, 6, y, -1);
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
        if (config.showArmor) {
            chip(context, client, armorLine(player), 0xffaab8cb, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showPotions) {
            Collection<StatusEffectInstance> effects = player.getStatusEffects();
            int shown = 0;
            for (StatusEffectInstance effect : effects) {
                if (shown >= 3) break;
                String name = effect.getEffectType().value().getName().getString();
                int seconds = Math.max(0, effect.getDuration() / 20);
                String label = "BUFF  " + name + (effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "")
                    + "  " + String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                chip(context, client, label, 0xffc6a0ff, config.hudOpacity, 6, y, -1);
                y += 20;
                shown++;
            }
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
        if (config.showSpeed) {
            chip(context, client, String.format(Locale.ROOT, "SPEED  %.1f b/s",
                AzureClient.speedBlocksPerSec()), 0xff80e0a0, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showClock) {
            LocalTime now = LocalTime.now();
            chip(context, client, String.format(Locale.ROOT, "CLOCK  %02d:%02d",
                now.getHour(), now.getMinute()), 0xffe6edf7, config.hudOpacity, 6, y, -1);
            y += 20;
        }
        if (config.showBlockInfo) {
            String block = blockLine(client);
            if (block != null) {
                chip(context, client, block, 0xffffca78, config.hudOpacity, 6, y, -1);
                y += 20;
            }
        }
        if (config.showMemory) {
            long total = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long free = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            chip(context, client, "RAM  " + (total - free) + "MB / " + total + "MB",
                0xffaab8cb, config.hudOpacity, 6, y, -1);
        }
        if (config.showKeystrokes) {
            renderKeystrokes(context, client, config, accent,
                context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
    }

    private static String armorLine(PlayerEntity player) {
        StringBuilder text = new StringBuilder("ARMOR  ");
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"H", "C", "L", "B"};
        for (int index = 0; index < slots.length; index++) {
            ItemStack stack = player.getEquippedStack(slots[index]);
            text.append(labels[index]).append(' ');
            if (stack.isEmpty()) {
                text.append("--");
            } else if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                int percent = Math.round((1.0f - (float) stack.getDamage() / stack.getMaxDamage()) * 100.0f);
                text.append(percent).append('%');
            } else {
                text.append("100%");
            }
            if (index < slots.length - 1) text.append("  ");
        }
        return text.toString();
    }

    private static String blockLine(MinecraftClient client) {
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult hit)) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        String name = client.world.getBlockState(pos).getBlock().getName().getString();
        return "BLOCK  " + name + "  " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void renderKeystrokes(DrawContext context, MinecraftClient client, AzureConfig config,
                                         int accent, int screenWidth, int screenHeight) {
        int keyWidth = 20;
        int keyHeight = 20;
        int gap = 3;
        int startX = 6;
        int startY = screenHeight - 95;

        key(context, client, "W", startX + keyWidth + gap, startY, keyWidth, keyHeight,
            client.options.forwardKey.isPressed(), accent, config.hudOpacity);
        int row2 = startY + keyHeight + gap;
        key(context, client, "A", startX, row2, keyWidth, keyHeight,
            client.options.leftKey.isPressed(), accent, config.hudOpacity);
        key(context, client, "S", startX + keyWidth + gap, row2, keyWidth, keyHeight,
            client.options.backKey.isPressed(), accent, config.hudOpacity);
        key(context, client, "D", startX + (keyWidth + gap) * 2, row2, keyWidth, keyHeight,
            client.options.rightKey.isPressed(), accent, config.hudOpacity);

        int row3 = row2 + keyHeight + gap;
        int mouseWidth = (keyWidth * 3 + gap * 2 - gap) / 2;
        key(context, client, "LMB", startX, row3, mouseWidth, keyHeight,
            client.options.attackKey.isPressed(), accent, config.hudOpacity);
        key(context, client, "RMB", startX + mouseWidth + gap, row3, mouseWidth, keyHeight,
            client.options.useKey.isPressed(), accent, config.hudOpacity);

        int row4 = row3 + keyHeight + gap;
        key(context, client, "SPACE", startX, row4, keyWidth * 3 + gap * 2, 12,
            client.options.jumpKey.isPressed(), accent, config.hudOpacity);
    }

    private static void key(DrawContext context, MinecraftClient client, String label, int x, int y,
                            int width, int height, boolean pressed, int accent, int opacity) {
        context.fill(x, y, x + width, y + height, pressed ? accent : opacity(0xff121824, opacity));
        border(context, x, y, width, height, pressed ? 0xffffffff : 0x5535455c);
        context.drawCenteredTextWithShadow(client.textRenderer, label, x + width / 2,
            y + (height - 8) / 2, pressed ? 0xff0d131d : 0xffedf2fa);
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
