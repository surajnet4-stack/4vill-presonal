package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class AzureHud {
    private static final int KEY_IDLE = 0xb8222935;

    private AzureHud() {
    }

    public static void render(DrawContext context, AzureConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) return;

        if (config.showFps) {
            int x = clampX(config.fpsX, client.getWindow().getScaledWidth(), 86, config.fpsScale);
            int y = clampY(config.fpsY, client.getWindow().getScaledHeight(), 16, config.fpsScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.fpsScale, config.fpsScale, 1);
            drawChip(context, client, client.getCurrentFps() + " FPS", config.fpsColor);
            context.getMatrices().pop();
        }
        if (config.showCps) {
            String cps = AzureClient.clicksPerSecond() + " CPS";
            int labelWidth = client.textRenderer.getWidth(cps);
            context.getMatrices().push();
            context.getMatrices().translate(client.getWindow().getScaledWidth() - labelWidth - 32, 10, 0);
            drawChip(context, client, cps, config.keycapsColor);
            context.getMatrices().pop();
        }
        if (config.showBlockOverlay && client.crosshairTarget instanceof BlockHitResult hit &&
            hit.getType() == HitResult.Type.BLOCK) {
            renderBlockOverlay(context, config, hit.getBlockPos());
        }
        if (config.showKeycaps) {
            renderKeycaps(context, client, config);
        }
        int utilityY = 28;
        if (config.showCoordinates) {
            BlockPos pos = player.getBlockPos();
            String coordinates = String.format("XYZ  %d  %d  %d", pos.getX(), pos.getY(), pos.getZ());
            drawUtilityChip(context, client, coordinates, 10, utilityY, config.blockOverlayColor);
            utilityY += 21;
        }
        if (config.showDirection) {
            String direction = "FACING  " + player.getHorizontalFacing().asString().toUpperCase();
            drawUtilityChip(context, client, direction, 10, utilityY, config.fpsColor);
            utilityY += 21;
        }
        if (config.showArmor) {
            renderArmor(context, client, player, utilityY);
        }
        if (AzureClient.isFreeLookActive()) {
            int labelWidth = client.textRenderer.getWidth("FREE LOOK");
            context.getMatrices().push();
            context.getMatrices().translate(client.getWindow().getScaledWidth() - labelWidth - 28, 31, 0);
            drawChip(context, client, "FREE LOOK", 0xff49d8ce);
            context.getMatrices().pop();
        }
    }

    public static void renderCrosshair(DrawContext context, AzureConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        int size = config.crosshairSize;
        int thickness = config.crosshairThickness;
        int gap = config.crosshairGap;
        int color = config.crosshairColor;
        if (config.crosshairHitColor && client.crosshairTarget instanceof EntityHitResult) {
            color = config.crosshairHitColorValue;
        }
        color = withOpacity(color, config.crosshairOpacity);
        if (config.crosshairOutline) {
            drawCrosshairShape(context, centerX, centerY, size + 2, thickness + 2, gap, 0xb0000000,
                config.crosshairStyle);
        }
        drawCrosshairShape(context, centerX, centerY, size, thickness, gap, color, config.crosshairStyle);
        if (config.crosshairCenterDot) {
            context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
        }
    }

    private static void drawCrosshairShape(DrawContext context, int centerX, int centerY, int size,
                                           int thickness, int gap, int color, int style) {
        switch (style) {
            case 1 -> {
                int diameter = size;
                for (int y = -diameter; y <= diameter; y++) {
                    int span = (int) Math.sqrt(diameter * diameter - y * y);
                    context.fill(centerX - span, centerY + y, centerX + span + 1, centerY + y + 1, color);
                }
            }
            case 2 -> {
                int outer = size + gap;
                int inner = Math.max(1, outer - thickness);
                for (int y = -outer; y <= outer; y++) {
                    int outerSpan = (int) Math.sqrt(outer * outer - y * y);
                    if (Math.abs(y) < inner) {
                        int innerSpan = (int) Math.sqrt(inner * inner - y * y);
                        context.fill(centerX - outerSpan, centerY + y, centerX - innerSpan, centerY + y + 1, color);
                        context.fill(centerX + innerSpan + 1, centerY + y, centerX + outerSpan + 1, centerY + y + 1, color);
                    } else {
                        context.fill(centerX - outerSpan, centerY + y, centerX + outerSpan + 1, centerY + y + 1, color);
                    }
                }
            }
            default -> {
                fillCentered(context, centerX, centerY - gap - size, thickness, size, color);
                fillCentered(context, centerX, centerY + gap + 1, thickness, size, color);
                fillCentered(context, centerX - gap - size, centerY, size, thickness, color);
                fillCentered(context, centerX + gap + 1, centerY, size, thickness, color);
            }
        }
    }

    private static void fillCentered(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x - width / 2, y - height / 2, x + (width + 1) / 2, y + (height + 1) / 2, color);
    }

    private static int clampX(int x, int screenWidth, int contentWidth, float scale) {
        return Math.max(2, Math.min(x, screenWidth - (int) (contentWidth * scale) - 2));
    }

    private static int clampY(int y, int screenHeight, int contentHeight, float scale) {
        return Math.max(2, Math.min(y, screenHeight - (int) (contentHeight * scale) - 2));
    }

    private static void drawChip(DrawContext context, MinecraftClient client, String label, int color) {
        int width = client.textRenderer.getWidth(label);
        int opacity = AzureClient.config().hudOpacity;
        context.fill(-5, -3, width + 17, 14, withOpacity(0xff10151e, opacity));
        context.fill(-5, -3, -2, 14, color);
        context.fill(2, 2, 9, 9, color);
        context.fill(4, 4, 7, 7, withOpacity(0xff10151e, opacity));
        context.drawTextWithShadow(client.textRenderer, label, 13, 0, color);
    }

    private static void drawUtilityChip(DrawContext context, MinecraftClient client,
                                        String label, int x, int y, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        drawChip(context, client, label, color);
        context.getMatrices().pop();
    }

    private static void renderArmor(DrawContext context, MinecraftClient client, PlayerEntity player, int y) {
        int x = 10;
        for (net.minecraft.item.ItemStack stack : player.getArmorItems()) {
            if (stack.isEmpty()) continue;
            String durability = stack.isDamageable()
                ? Integer.toString(Math.max(0, (stack.getMaxDamage() - stack.getDamage()) * 100 / stack.getMaxDamage())) + "%"
                : "READY";
            drawUtilityChip(context, client, stack.getName().getString() + "  " + durability,
                x, y, stack.isDamageable() && stack.getDamage() * 100 / stack.getMaxDamage() > 75
                    ? 0xffff7f9f : 0xff9dd8c8);
            y += 21;
        }
    }

    private static void renderBlockOverlay(DrawContext context, AzureConfig config, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        String name = client.world.getBlockState(pos).getBlock().getName().getString();
        String details = name + "  " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        int x = clampX(config.blockOverlayX, client.getWindow().getScaledWidth(),
            client.textRenderer.getWidth(details) + 10, config.blockOverlayScale);
        int y = clampY(config.blockOverlayY, client.getWindow().getScaledHeight(), 16, config.blockOverlayScale);
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.blockOverlayScale, config.blockOverlayScale, 1);
        drawChip(context, client, details, config.blockOverlayColor);
        context.getMatrices().pop();
    }

    private static void renderKeycaps(DrawContext context, MinecraftClient client, AzureConfig config) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int box = 18;
        int gap = 3;
        int left = config.keycapsX < 0 ? width / 2 - (box * 3 + gap * 2) / 2 : config.keycapsX;
        int top = config.keycapsY < 0 ? height - 75 : config.keycapsY;
        left = clampX(left, width, box * 3 + gap * 2, config.keycapsScale);
        top = clampY(top, height, 78, config.keycapsScale);
        context.getMatrices().push();
        context.getMatrices().translate(left, top, 0);
        context.getMatrices().scale(config.keycapsScale, config.keycapsScale, 1);
        drawKey(context, client, "W", client.options.forwardKey.isPressed(), box + gap, 0, config.keycapsColor);
        drawKey(context, client, "A", client.options.leftKey.isPressed(), 0, box + gap, config.keycapsColor);
        drawKey(context, client, "S", client.options.backKey.isPressed(), box + gap, box + gap, config.keycapsColor);
        drawKey(context, client, "D", client.options.rightKey.isPressed(), (box + gap) * 2, box + gap, config.keycapsColor);
        drawKey(context, client, "JUMP", client.options.jumpKey.isPressed(), 0, (box + gap) * 2, box * 3 + gap * 2, config.keycapsColor);
        context.getMatrices().pop();
    }

    private static void drawKey(DrawContext context, MinecraftClient client, String label, boolean pressed, int x, int y, int color) {
        drawKey(context, client, label, pressed, x, y, 18, color);
    }

    private static void drawKey(DrawContext context, MinecraftClient client, String label, boolean pressed, int x, int y, int width, int color) {
        int height = 18;
        context.fill(x, y, x + width, y + height,
            pressed ? withOpacity(color, AzureClient.config().hudOpacity)
                : withOpacity(KEY_IDLE, AzureClient.config().hudOpacity));
        context.drawCenteredTextWithShadow(client.textRenderer, label, x + width / 2, y + 5, pressed ? 0xff10151d : color);
    }

    private static int withOpacity(int color, int opacityPercent) {
        int existingAlpha = color >>> 24;
        int alpha = existingAlpha * Math.max(0, Math.min(100, opacityPercent)) / 100;
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
