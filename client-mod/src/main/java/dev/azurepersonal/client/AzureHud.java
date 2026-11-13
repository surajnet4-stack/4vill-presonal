package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public final class AzureHud {
    private static final SimpleDateFormat TIME_FORMAT_24 = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat TIME_FORMAT_12 = new SimpleDateFormat("hh:mm:ss a", Locale.ROOT);

    private AzureHud() {
    }

    public static void render(DrawContext context, AzureConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int accent = config.chromaHud ? AzureClient.getChromaColor(0) : config.accentColor();

        // 1. Watermark
        if (config.showWatermark) {
            renderWatermark(context, client, config, accent, screenWidth, screenHeight);
        }

        // 2. FPS
        if (config.showFps) {
            int x = clampX(config.fpsX, screenWidth, 80, config.fpsScale);
            int y = clampY(config.fpsY, screenHeight, 20, config.fpsScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.fpsScale, config.fpsScale, 1);
            int color = config.chromaHud ? AzureClient.getChromaColor(100) : config.fpsColor;
            drawChip(context, client, client.getCurrentFps() + " FPS", color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 3. Ping / Latency
        if (config.showPing) {
            int ping = getPing(client, player);
            String label = ping >= 0 ? ping + " ms" : "0 ms";
            int pingColor = ping < 60 ? 0xff49d8ce : (ping < 120 ? 0xffffca78 : 0xffff4757);
            if (config.chromaHud) pingColor = AzureClient.getChromaColor(200);
            int x = config.pingX < 0 ? screenWidth - 76 : clampX(config.pingX, screenWidth, 68, config.pingScale);
            int y = config.pingY < 0 ? 10 : clampY(config.pingY, screenHeight, 20, config.pingScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.pingScale, config.pingScale, 1);
            drawChip(context, client, "PING " + label, pingColor, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 4. CPS (Dual Left & Right)
        if (config.showCps) {
            int leftCps = AzureClient.clicksPerSecond();
            int rightCps = AzureClient.rightClicksPerSecond();
            String cpsLabel = config.showRightCps ? (leftCps + " | " + rightCps + " CPS") : (leftCps + " CPS");
            int color = config.chromaHud ? AzureClient.getChromaColor(300) : config.cpsColor;
            int labelWidth = client.textRenderer.getWidth(cpsLabel);
            int x = config.cpsX < 0 ? screenWidth - labelWidth - 28 : clampX(config.cpsX, screenWidth, labelWidth + 20, config.cpsScale);
            int y = config.cpsY < 0 ? 32 : clampY(config.cpsY, screenHeight, 20, config.cpsScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.cpsScale, config.cpsScale, 1);
            drawChip(context, client, cpsLabel, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 5. Keystrokes (WASD + LMB/RMB + Space)
        if (config.showKeycaps) {
            renderKeycaps(context, client, config, accent, screenWidth, screenHeight);
        }

        // 6. Coordinates & Biome
        if (config.showCoordinates) {
            renderCoordinates(context, client, player, config, screenWidth, screenHeight);
        }

        // 7. Direction / Facing Compass
        if (config.showDirection) {
            renderDirection(context, client, player, config, screenWidth, screenHeight);
        }

        // 8. Armor & Equipment Status
        if (config.showArmor) {
            renderArmor(context, client, player, config, screenWidth, screenHeight);
        }

        // 9. Potion Effects Status
        if (config.showPotions) {
            renderPotions(context, client, player, config, screenWidth, screenHeight);
        }

        // 10. Speedometer
        if (config.showSpeedometer) {
            String speedText = String.format(Locale.ROOT, "SPEED %.1f m/s", AzureClient.speedBlocksPerSec());
            int color = config.chromaHud ? AzureClient.getChromaColor(400) : config.speedColor;
            int x = clampX(config.speedX, screenWidth, 90, config.speedScale);
            int y = clampY(config.speedY, screenHeight, 20, config.speedScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.speedScale, config.speedScale, 1);
            drawChip(context, client, speedText, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 11. Combo Counter
        if (config.showCombo && AzureClient.comboCount() > 0) {
            String comboText = "COMBO " + AzureClient.comboCount();
            int color = config.chromaHud ? AzureClient.getChromaColor(500) : config.comboColor;
            int labelWidth = client.textRenderer.getWidth(comboText);
            int x = config.comboX < 0 ? (screenWidth - labelWidth) / 2 : clampX(config.comboX, screenWidth, labelWidth + 20, config.comboScale);
            int y = config.comboY < 0 ? screenHeight / 2 + 18 : clampY(config.comboY, screenHeight, 20, config.comboScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.comboScale, config.comboScale, 1);
            drawChip(context, client, comboText, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 12. Reach Display (shows for 2.5s after hitting)
        if (config.showReach && System.currentTimeMillis() - AzureClient.lastReachTime() < 2500) {
            String reachText = String.format(Locale.ROOT, "REACH %.2fm", AzureClient.reachDistance());
            int color = config.chromaHud ? AzureClient.getChromaColor(600) : config.reachColor;
            int labelWidth = client.textRenderer.getWidth(reachText);
            int x = config.reachX < 0 ? (screenWidth - labelWidth) / 2 : clampX(config.reachX, screenWidth, labelWidth + 20, config.reachScale);
            int y = config.reachY < 0 ? screenHeight / 2 + 38 : clampY(config.reachY, screenHeight, 20, config.reachScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.reachScale, config.reachScale, 1);
            drawChip(context, client, reachText, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 13. Clock
        if (config.showClock) {
            String timeText = (config.clock24h ? TIME_FORMAT_24 : TIME_FORMAT_12).format(new Date());
            int color = config.chromaHud ? AzureClient.getChromaColor(700) : config.clockColor;
            int x = clampX(config.clockX, screenWidth, 80, config.clockScale);
            int y = clampY(config.clockY, screenHeight, 20, config.clockScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.clockScale, config.clockScale, 1);
            drawChip(context, client, timeText, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 14. Memory / RAM Usage
        if (config.showMemory) {
            long total = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long free = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long used = total - free;
            String memoryText = "RAM " + used + "MB / " + total + "MB";
            int color = config.chromaHud ? AzureClient.getChromaColor(800) : config.memoryColor;
            int x = clampX(config.memoryX, screenWidth, 110, config.memoryScale);
            int y = clampY(config.memoryY, screenHeight, 20, config.memoryScale);
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(config.memoryScale, config.memoryScale, 1);
            drawChip(context, client, memoryText, color, config.hudOpacity);
            context.getMatrices().pop();
        }

        // 15. Block Overlay
        if (config.showBlockOverlay && client.crosshairTarget instanceof BlockHitResult hit &&
            hit.getType() == HitResult.Type.BLOCK) {
            renderBlockOverlay(context, client, config, hit.getBlockPos(), screenWidth, screenHeight);
        }

        // 16. Toggle Sprint & FreeLook Badges
        int badgeY = screenHeight - 24;
        if (config.showToggleSprintBadge && AzureClient.isSprintToggled()) {
            drawChip(context, client, "[Sprinting (Toggled)]", accent, config.hudOpacity, 10, badgeY);
            badgeY -= 20;
        }
        if (AzureClient.isSneakToggled()) {
            drawChip(context, client, "[Sneaking (Toggled)]", 0xffffca78, config.hudOpacity, 10, badgeY);
            badgeY -= 20;
        }
        if (AzureClient.isFreeLookActive()) {
            drawChip(context, client, "FREE LOOK (V)", 0xff49d8ce, config.hudOpacity, screenWidth - 100, 54);
        }
    }

    // --- Watermark Renderer ---
    private static void renderWatermark(DrawContext context, MinecraftClient client, AzureConfig config,
                                        int accent, int screenWidth, int screenHeight) {
        StringBuilder sb = new StringBuilder("AZURE");
        if (config.watermarkShowVersion) sb.append("  |  1.20.x");
        if (config.watermarkShowFps) sb.append("  |  ").append(client.getCurrentFps()).append(" FPS");
        if (config.watermarkShowPing) {
            int ping = getPing(client, client.player);
            if (ping >= 0) sb.append("  |  ").append(ping).append("ms");
        }
        if (config.watermarkShowUser && client.player != null) {
            sb.append("  |  ").append(client.player.getName().getString());
        }
        String watermarkText = sb.toString();

        int x = clampX(config.watermarkX, screenWidth, 120, config.watermarkScale);
        int y = clampY(config.watermarkY, screenHeight, 24, config.watermarkScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.watermarkScale, config.watermarkScale, 1);

        int textWidth = client.textRenderer.getWidth(watermarkText);
        int cardWidth = textWidth + 24;
        int cardHeight = 18;

        if (config.watermarkStyle == 0) { // Modern Pill
            int bg = withOpacity(0xff0d131d, config.hudOpacity);
            context.fill(0, 0, cardWidth, cardHeight, bg);
            context.fill(0, 0, 3, cardHeight, accent);
            drawBorder(context, 0, 0, cardWidth, cardHeight, 0x442c3d52);
            context.drawTextWithShadow(client.textRenderer, watermarkText, 9, 5, 0xfff0f4fa);
        } else if (config.watermarkStyle == 1) { // Minimal Text
            context.drawTextWithShadow(client.textRenderer, watermarkText, 0, 4, accent);
        } else { // Chroma Wave Pill
            int chroma = AzureClient.getChromaColor(0);
            context.fill(0, 0, cardWidth, cardHeight, withOpacity(0xff0b1017, config.hudOpacity));
            context.fill(0, 0, cardWidth, 2, chroma);
            drawBorder(context, 0, 0, cardWidth, cardHeight, withOpacity(chroma, 60));
            context.drawTextWithShadow(client.textRenderer, watermarkText, 8, 5, chroma);
        }

        context.getMatrices().pop();
    }

    // --- Keystrokes Renderer ---
    private static void renderKeycaps(DrawContext context, MinecraftClient client, AzureConfig config,
                                      int accent, int screenWidth, int screenHeight) {
        int keyWidth = 20;
        int keyHeight = 20;
        int gap = 3;
        int startX = config.keycapsX < 0 ? screenWidth / 2 - 34 : clampX(config.keycapsX, screenWidth, 70, config.keycapsScale);
        int startY = config.keycapsY < 0 ? screenHeight - 95 : clampY(config.keycapsY, screenHeight, 90, config.keycapsScale);

        context.getMatrices().push();
        context.getMatrices().translate(startX, startY, 0);
        context.getMatrices().scale(config.keycapsScale, config.keycapsScale, 1);

        // Row 1: W
        drawKey(context, client, "W", keyWidth + gap, 0, keyWidth, keyHeight,
            client.options.forwardKey.isPressed(), accent, config.hudOpacity);

        // Row 2: A, S, D
        int row2Y = keyHeight + gap;
        drawKey(context, client, "A", 0, row2Y, keyWidth, keyHeight,
            client.options.leftKey.isPressed(), accent, config.hudOpacity);
        drawKey(context, client, "S", keyWidth + gap, row2Y, keyWidth, keyHeight,
            client.options.backKey.isPressed(), accent, config.hudOpacity);
        drawKey(context, client, "D", (keyWidth + gap) * 2, row2Y, keyWidth, keyHeight,
            client.options.rightKey.isPressed(), accent, config.hudOpacity);

        int nextY = row2Y + keyHeight + gap;

        // Row 3: LMB & RMB (Mouse Buttons)
        if (config.showMouseButtons) {
            int mouseWidth = (keyWidth * 3 + gap * 2 - gap) / 2;
            int mouseHeight = 20;
            boolean lmbPressed = client.options.attackKey.isPressed();
            boolean rmbPressed = client.options.useKey.isPressed();
            String lmbText = config.showCpsOnKeys ? (AzureClient.clicksPerSecond() + " CPS") : "LMB";
            String rmbText = config.showCpsOnKeys ? (AzureClient.rightClicksPerSecond() + " CPS") : "RMB";

            drawKey(context, client, lmbText, 0, nextY, mouseWidth, mouseHeight, lmbPressed, accent, config.hudOpacity);
            drawKey(context, client, rmbText, mouseWidth + gap, nextY, mouseWidth, mouseHeight, rmbPressed, accent, config.hudOpacity);
            nextY += mouseHeight + gap;
        }

        // Row 4: Spacebar
        if (config.showSpacebar) {
            int spaceWidth = keyWidth * 3 + gap * 2;
            int spaceHeight = 12;
            boolean spacePressed = client.options.jumpKey.isPressed();
            drawKey(context, client, "──────", 0, nextY, spaceWidth, spaceHeight, spacePressed, accent, config.hudOpacity);
        }

        context.getMatrices().pop();
    }

    private static void drawKey(DrawContext context, MinecraftClient client, String label, int x, int y,
                                int width, int height, boolean pressed, int accent, int opacity) {
        int bg = pressed ? accent : withOpacity(0xff121824, opacity);
        int textColor = pressed ? 0xff0d131d : 0xffedf2fa;
        context.fill(x, y, x + width, y + height, bg);
        drawBorder(context, x, y, width, height, pressed ? 0xffffffff : 0x5535455c);
        context.drawCenteredTextWithShadow(client.textRenderer, label, x + width / 2, y + (height - 8) / 2, textColor);
    }

    // --- Coordinates & Biome Renderer ---
    private static void renderCoordinates(DrawContext context, MinecraftClient client, PlayerEntity player,
                                          AzureConfig config, int screenWidth, int screenHeight) {
        BlockPos pos = player.getBlockPos();
        String coords = String.format(Locale.ROOT, "XYZ  %d  %d  %d", pos.getX(), pos.getY(), pos.getZ());
        int color = config.chromaHud ? AzureClient.getChromaColor(150) : config.coordsColor;

        int x = clampX(config.coordsX, screenWidth, 140, config.coordsScale);
        int y = clampY(config.coordsY, screenHeight, 20, config.coordsScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.coordsScale, config.coordsScale, 1);

        drawChip(context, client, coords, color, config.hudOpacity);

        int subY = 22;
        if (config.showBiome && client.world != null) {
            String biome = client.world.getBiome(pos).getKey()
                .map(k -> k.getValue().getPath().replace('_', ' ').toUpperCase(Locale.ROOT))
                .orElse("UNKNOWN");
            drawChip(context, client, "BIOME  " + biome, 0xffaab8cb, config.hudOpacity, 0, subY);
            subY += 20;
        }

        if (config.showNetherCoords && client.world != null) {
            boolean isNether = client.world.getRegistryKey().getValue().getPath().contains("nether");
            int netherX = isNether ? pos.getX() * 8 : pos.getX() / 8;
            int netherZ = isNether ? pos.getZ() * 8 : pos.getZ() / 8;
            String netherText = (isNether ? "OVERWORLD " : "NETHER ") + netherX + ", " + netherZ;
            drawChip(context, client, netherText, 0xffffca78, config.hudOpacity, 0, subY);
        }

        context.getMatrices().pop();
    }

    // --- Direction / Facing Compass Renderer ---
    private static void renderDirection(DrawContext context, MinecraftClient client, PlayerEntity player,
                                        AzureConfig config, int screenWidth, int screenHeight) {
        String facing = player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT);
        float yaw = (player.getYaw() % 360 + 360) % 360;
        String dirText = String.format(Locale.ROOT, "FACING  %s (%.0f°)", facing, yaw);
        int color = config.chromaHud ? AzureClient.getChromaColor(250) : config.directionColor;

        int x = clampX(config.directionX, screenWidth, 130, config.directionScale);
        int y = clampY(config.directionY, screenHeight, 20, config.directionScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.directionScale, config.directionScale, 1);
        drawChip(context, client, dirText, color, config.hudOpacity);
        context.getMatrices().pop();
    }

    // --- Armor & Equipment Status Renderer ---
    private static void renderArmor(DrawContext context, MinecraftClient client, PlayerEntity player,
                                    AzureConfig config, int screenWidth, int screenHeight) {
        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
        };

        int x = config.armorX < 0 ? 10 : clampX(config.armorX, screenWidth, 120, config.armorScale);
        int y = config.armorY < 0 ? screenHeight - 145 : clampY(config.armorY, screenHeight, 130, config.armorScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.armorScale, config.armorScale, 1);

        int rowY = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            context.fill(0, rowY, 78, rowY + 18, withOpacity(0xff101622, config.hudOpacity));
            context.drawItem(stack, 2, rowY + 1);

            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0 && config.showArmorDurability) {
                int damage = stack.getDamage();
                int remaining = maxDamage - damage;
                int percent = Math.max(0, Math.min(100, 100 * remaining / maxDamage));
                int durabilityColor = percent > 50 ? 0xff49d8ce : (percent > 20 ? 0xffffca78 : 0xffff4757);
                context.drawTextWithShadow(client.textRenderer, percent + "%", 22, rowY + 5, durabilityColor);
            } else {
                context.drawTextWithShadow(client.textRenderer, stack.getCount() > 1 ? ("x" + stack.getCount()) : "OK",
                    22, rowY + 5, 0xffaab8cb);
            }

            rowY += 20;
        }

        context.getMatrices().pop();
    }

    // --- Potion Status Renderer ---
    private static void renderPotions(DrawContext context, MinecraftClient client, PlayerEntity player,
                                      AzureConfig config, int screenWidth, int screenHeight) {
        Collection<StatusEffectInstance> effects = player.getStatusEffects();
        if (effects.isEmpty()) return;

        int x = config.potionsX < 0 ? screenWidth - 110 : clampX(config.potionsX, screenWidth, 105, config.potionsScale);
        int y = clampY(config.potionsY, screenHeight, effects.size() * 22, config.potionsScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.potionsScale, config.potionsScale, 1);

        int rowY = 0;
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().getName().getString();
            int durationTicks = effect.getDuration();
            String duration = String.format(Locale.ROOT, "%02d:%02d", (durationTicks / 20) / 60, (durationTicks / 20) % 60);

            int width = Math.max(90, client.textRenderer.getWidth(name + " " + duration) + 14);
            context.fill(0, rowY, width, rowY + 18, withOpacity(0xff101622, config.hudOpacity));
            context.fill(0, rowY, 3, rowY + 18, config.accentColor());
            context.drawTextWithShadow(client.textRenderer, name, 7, rowY + 2, 0xfff0f4fa);
            context.drawTextWithShadow(client.textRenderer, duration, 7, rowY + 10, 0xffaab8cb);

            rowY += 21;
        }

        context.getMatrices().pop();
    }

    // --- Block Overlay Renderer ---
    private static void renderBlockOverlay(DrawContext context, MinecraftClient client, AzureConfig config,
                                           BlockPos pos, int screenWidth, int screenHeight) {
        String name = client.world.getBlockState(pos).getBlock().getName().getString();
        String blockInfo = name + "  " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

        int x = clampX(config.blockOverlayX, screenWidth, 140, config.blockOverlayScale);
        int y = clampY(config.blockOverlayY, screenHeight, 20, config.blockOverlayScale);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.blockOverlayScale, config.blockOverlayScale, 1);
        drawChip(context, client, blockInfo, config.blockOverlayColor, config.hudOpacity);
        context.getMatrices().pop();
    }

    // --- Crosshair Renderer ---
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
            drawCrosshairShape(context, centerX, centerY, size + 1, thickness + 2, gap, 0xb0000000, config.crosshairStyle);
        }
        drawCrosshairShape(context, centerX, centerY, size, thickness, gap, color, config.crosshairStyle);

        if (config.crosshairCenterDot) {
            context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
        }
    }

    private static void drawCrosshairShape(DrawContext context, int centerX, int centerY, int size,
                                           int thickness, int gap, int color, int style) {
        switch (style) {
            case 1 -> { // Dot
                int r = Math.max(1, thickness);
                context.fill(centerX - r, centerY - r, centerX + r + 1, centerY + r + 1, color);
            }
            case 2 -> { // Circle / Ring
                int r = size;
                for (int y = -r; y <= r; y++) {
                    int span = (int) Math.sqrt(r * r - y * y);
                    context.fill(centerX - span, centerY + y, centerX + span + 1, centerY + y + 1, color);
                }
            }
            case 3 -> { // Arrow / Chevron
                for (int i = 0; i < size; i++) {
                    context.fill(centerX - i, centerY + gap + i, centerX - i + thickness, centerY + gap + i + 1, color);
                    context.fill(centerX + i - thickness + 1, centerY + gap + i, centerX + i + 1, centerY + gap + i + 1, color);
                }
            }
            case 4 -> { // Box / Square
                context.fill(centerX - size, centerY - size, centerX + size + 1, centerY - size + thickness, color);
                context.fill(centerX - size, centerY + size - thickness + 1, centerX + size + 1, centerY + size + 1, color);
                context.fill(centerX - size, centerY - size, centerX - size + thickness, centerY + size + 1, color);
                context.fill(centerX + size - thickness + 1, centerY - size, centerX + size + 1, centerY + size + 1, color);
            }
            default -> { // Cross (Style 0)
                int half = thickness / 2;
                context.fill(centerX - half, centerY - gap - size, centerX + half + 1, centerY - gap, color);
                context.fill(centerX - half, centerY + gap + 1, centerX + half + 1, centerY + gap + size + 1, color);
                context.fill(centerX - gap - size, centerY - half, centerX - gap, centerY + half + 1, color);
                context.fill(centerX + gap + 1, centerY - half, centerX + gap + size + 1, centerY + half + 1, color);
            }
        }
    }

    // --- Helper Utilities ---
    public static void drawChip(DrawContext context, MinecraftClient client, String text, int color, int opacity) {
        drawChip(context, client, text, color, opacity, 0, 0);
    }

    public static void drawChip(DrawContext context, MinecraftClient client, String text, int color, int opacity, int x, int y) {
        int textWidth = client.textRenderer.getWidth(text);
        int cardWidth = textWidth + 14;
        int cardHeight = 16;
        context.fill(x, y, x + cardWidth, y + cardHeight, withOpacity(0xff101622, opacity));
        context.fill(x, y, x + 2, y + cardHeight, color);
        drawBorder(context, x, y, cardWidth, cardHeight, 0x442c3d52);
        context.drawTextWithShadow(client.textRenderer, text, x + 7, y + 4, color);
    }

    public static int getPing(MinecraftClient client, PlayerEntity player) {
        if (client.getNetworkHandler() != null && player != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return -1;
    }

    public static int withOpacity(int color, int opacityPercent) {
        int alpha = Math.max(0, Math.min(255, opacityPercent * 255 / 100));
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static int clampX(int x, int screenWidth, int elementWidth, float scale) {
        return Math.max(4, Math.min(screenWidth - (int) (elementWidth * scale) - 4, x));
    }

    private static int clampY(int y, int screenHeight, int elementHeight, float scale) {
        return Math.max(4, Math.min(screenHeight - (int) (elementHeight * scale) - 4, y));
    }
}
