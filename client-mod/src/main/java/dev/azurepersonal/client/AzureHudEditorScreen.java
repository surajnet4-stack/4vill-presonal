package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AzureHudEditorScreen extends Screen {
    public enum Widget {
        WATERMARK("WATERMARK", "✦", "AZURE  |  1.20.x  |  144 FPS", 160, 24),
        FPS("FPS COUNTER", "F", "144 FPS", 80, 22),
        KEYCAPS("KEYSTROKES", "K", "W  A  S  D  [LMB] [RMB]", 90, 80),
        CPS("CPS COUNTER", "C", "12 | 14 CPS", 90, 22),
        PING("PING", "P", "PING 24 ms", 80, 22),
        ARMOR("ARMOR STATUS", "A", "Armor & Durability", 80, 90),
        POTIONS("POTION EFFECTS", "✦", "Speed II  02:45", 95, 45),
        COORDINATES("COORDINATES", "X", "XYZ 128 64 -20", 120, 22),
        DIRECTION("DIRECTION", "N", "FACING NORTH (0°)", 120, 22),
        SPEED("SPEEDOMETER", "S", "SPEED 7.4 m/s", 95, 22),
        COMBO("COMBO COUNTER", "⚔", "COMBO 5 HITS", 95, 22),
        REACH("REACH DISPLAY", "R", "REACH 3.02m", 95, 22),
        CLOCK("CLOCK", "T", "TIME 22:45:10", 95, 22),
        MEMORY("RAM USAGE", "M", "RAM 42% (2048M)", 110, 22),
        BLOCK("BLOCK INFO", "B", "Stone  12, 64, -8", 120, 22);

        public final String label;
        public final String icon;
        public final String preview;
        public final int width;
        public final int height;

        Widget(String label, String icon, String preview, int width, int height) {
            this.label = label;
            this.icon = icon;
            this.preview = preview;
            this.width = width;
            this.height = height;
        }
    }

    private static final int[] PALETTE = {
        0xfff5f7ff, 0xff49d8ce, 0xff8ab4ff, 0xffffca78,
        0xffff7f9f, 0xffc6a0ff, 0xff80e0a0, 0xffff4757
    };

    private Widget selected = Widget.WATERMARK;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private final Screen parent;

    public AzureHudEditorScreen() {
        this(null);
    }

    public AzureHudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
    }

    private static int colorFor(Widget widget) {
        AzureConfig cfg = AzureClient.config();
        return switch (widget) {
            case WATERMARK -> cfg.watermarkColor;
            case FPS -> cfg.fpsColor;
            case KEYCAPS -> cfg.keycapsColor;
            case CPS -> cfg.cpsColor;
            case PING -> cfg.pingColor;
            case COORDINATES -> cfg.coordsColor;
            case DIRECTION -> cfg.directionColor;
            case SPEED -> cfg.speedColor;
            case COMBO -> cfg.comboColor;
            case REACH -> cfg.reachColor;
            case CLOCK -> cfg.clockColor;
            case MEMORY -> cfg.memoryColor;
            case BLOCK -> cfg.blockOverlayColor;
            default -> cfg.accentColor();
        };
    }

    private static void setColor(Widget widget, int color) {
        AzureConfig cfg = AzureClient.config();
        switch (widget) {
            case WATERMARK -> cfg.watermarkColor = color;
            case FPS -> cfg.fpsColor = color;
            case KEYCAPS -> cfg.keycapsColor = color;
            case CPS -> cfg.cpsColor = color;
            case PING -> cfg.pingColor = color;
            case COORDINATES -> cfg.coordsColor = color;
            case DIRECTION -> cfg.directionColor = color;
            case SPEED -> cfg.speedColor = color;
            case COMBO -> cfg.comboColor = color;
            case REACH -> cfg.reachColor = color;
            case CLOCK -> cfg.clockColor = color;
            case MEMORY -> cfg.memoryColor = color;
            case BLOCK -> cfg.blockOverlayColor = color;
            default -> {}
        }
    }

    private float widgetScale(Widget widget) {
        AzureConfig cfg = AzureClient.config();
        return switch (widget) {
            case WATERMARK -> cfg.watermarkScale;
            case FPS -> cfg.fpsScale;
            case KEYCAPS -> cfg.keycapsScale;
            case CPS -> cfg.cpsScale;
            case PING -> cfg.pingScale;
            case ARMOR -> cfg.armorScale;
            case POTIONS -> cfg.potionsScale;
            case COORDINATES -> cfg.coordsScale;
            case DIRECTION -> cfg.directionScale;
            case SPEED -> cfg.speedScale;
            case COMBO -> cfg.comboScale;
            case REACH -> cfg.reachScale;
            case CLOCK -> cfg.clockScale;
            case MEMORY -> cfg.memoryScale;
            case BLOCK -> cfg.blockOverlayScale;
        };
    }

    private void resize(float amount) {
        AzureConfig cfg = AzureClient.config();
        cfg.hudPreset = "custom";
        switch (selected) {
            case WATERMARK -> cfg.watermarkScale = AzureConfig.clampScale(cfg.watermarkScale + amount);
            case FPS -> cfg.fpsScale = AzureConfig.clampScale(cfg.fpsScale + amount);
            case KEYCAPS -> cfg.keycapsScale = AzureConfig.clampScale(cfg.keycapsScale + amount);
            case CPS -> cfg.cpsScale = AzureConfig.clampScale(cfg.cpsScale + amount);
            case PING -> cfg.pingScale = AzureConfig.clampScale(cfg.pingScale + amount);
            case ARMOR -> cfg.armorScale = AzureConfig.clampScale(cfg.armorScale + amount);
            case POTIONS -> cfg.potionsScale = AzureConfig.clampScale(cfg.potionsScale + amount);
            case COORDINATES -> cfg.coordsScale = AzureConfig.clampScale(cfg.coordsScale + amount);
            case DIRECTION -> cfg.directionScale = AzureConfig.clampScale(cfg.directionScale + amount);
            case SPEED -> cfg.speedScale = AzureConfig.clampScale(cfg.speedScale + amount);
            case COMBO -> cfg.comboScale = AzureConfig.clampScale(cfg.comboScale + amount);
            case REACH -> cfg.reachScale = AzureConfig.clampScale(cfg.reachScale + amount);
            case CLOCK -> cfg.clockScale = AzureConfig.clampScale(cfg.clockScale + amount);
            case MEMORY -> cfg.memoryScale = AzureConfig.clampScale(cfg.memoryScale + amount);
            case BLOCK -> cfg.blockOverlayScale = AzureConfig.clampScale(cfg.blockOverlayScale + amount);
        }
        cfg.save();
    }

    private int[] widgetPosition(Widget widget) {
        AzureConfig cfg = AzureClient.config();
        return switch (widget) {
            case WATERMARK -> new int[]{cfg.watermarkX, cfg.watermarkY};
            case FPS -> new int[]{cfg.fpsX, cfg.fpsY};
            case KEYCAPS -> new int[]{
                cfg.keycapsX < 0 ? width / 2 - 34 : cfg.keycapsX,
                cfg.keycapsY < 0 ? height - 95 : cfg.keycapsY
            };
            case CPS -> new int[]{cfg.cpsX < 0 ? width - 110 : cfg.cpsX, cfg.cpsY < 0 ? 32 : cfg.cpsY};
            case PING -> new int[]{cfg.pingX < 0 ? width - 80 : cfg.pingX, cfg.pingY < 0 ? 10 : cfg.pingY};
            case ARMOR -> new int[]{cfg.armorX < 0 ? 10 : cfg.armorX, cfg.armorY < 0 ? height - 145 : cfg.armorY};
            case POTIONS -> new int[]{cfg.potionsX < 0 ? width - 110 : cfg.potionsX, cfg.potionsY};
            case COORDINATES -> new int[]{cfg.coordsX, cfg.coordsY};
            case DIRECTION -> new int[]{cfg.directionX, cfg.directionY};
            case SPEED -> new int[]{cfg.speedX, cfg.speedY};
            case COMBO -> new int[]{cfg.comboX < 0 ? width / 2 - 40 : cfg.comboX, cfg.comboY < 0 ? height / 2 + 18 : cfg.comboY};
            case REACH -> new int[]{cfg.reachX < 0 ? width / 2 - 40 : cfg.reachX, cfg.reachY < 0 ? height / 2 + 38 : cfg.reachY};
            case CLOCK -> new int[]{cfg.clockX, cfg.clockY};
            case MEMORY -> new int[]{cfg.memoryX, cfg.memoryY};
            case BLOCK -> new int[]{cfg.blockOverlayX, cfg.blockOverlayY};
        };
    }

    private void setWidgetPosition(Widget widget, int x, int y) {
        AzureConfig cfg = AzureClient.config();
        cfg.hudPreset = "custom";
        switch (widget) {
            case WATERMARK -> { cfg.watermarkX = x; cfg.watermarkY = y; }
            case FPS -> { cfg.fpsX = x; cfg.fpsY = y; }
            case KEYCAPS -> { cfg.keycapsX = x; cfg.keycapsY = y; }
            case CPS -> { cfg.cpsX = x; cfg.cpsY = y; }
            case PING -> { cfg.pingX = x; cfg.pingY = y; }
            case ARMOR -> { cfg.armorX = x; cfg.armorY = y; }
            case POTIONS -> { cfg.potionsX = x; cfg.potionsY = y; }
            case COORDINATES -> { cfg.coordsX = x; cfg.coordsY = y; }
            case DIRECTION -> { cfg.directionX = x; cfg.directionY = y; }
            case SPEED -> { cfg.speedX = x; cfg.speedY = y; }
            case COMBO -> { cfg.comboX = x; cfg.comboY = y; }
            case REACH -> { cfg.reachX = x; cfg.reachY = y; }
            case CLOCK -> { cfg.clockX = x; cfg.clockY = y; }
            case MEMORY -> { cfg.memoryX = x; cfg.memoryY = y; }
            case BLOCK -> { cfg.blockOverlayX = x; cfg.blockOverlayY = y; }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Subtle grid overlay
        context.fill(0, 0, width, height, 0x44070b12);

        // Center guide lines
        context.fill(width / 2, 0, width / 2 + 1, height, 0x15ffffff);
        context.fill(0, height / 2, width, height / 2 + 1, 0x15ffffff);

        // Header instructions
        context.fill(12, 12, width - 12, 42, 0xcc101722);
        context.fill(12, 12, width - 12, 14, 0xff49d8ce);
        context.drawTextWithShadow(textRenderer, "✦ HUD LAYOUT EDITOR", 24, 23, 0xfff3f7ff);
        context.drawTextWithShadow(textRenderer, "Click & drag widgets to place  ·  Scroll over items to resize  ·  Use bottom bar to customize",
            160, 23, 0xffaab8cb);

        for (Widget widget : Widget.values()) {
            int[] pos = widgetPosition(widget);
            float scale = widgetScale(widget);
            boolean isSel = widget == selected;

            context.getMatrices().push();
            context.getMatrices().translate(pos[0], pos[1], 0);
            context.getMatrices().scale(scale, scale, 1);

            int w = widget.width;
            int h = widget.height;
            context.fill(0, 0, w, h, isSel ? 0xee1c2d42 : 0xaa101724);
            context.fill(0, 0, 3, h, colorFor(widget));
            AzureHud.drawBorder(context, 0, 0, w, h, isSel ? 0xff49d8ce : 0x44364c6b);

            context.drawTextWithShadow(textRenderer, widget.icon, 7, 5, colorFor(widget));
            context.drawTextWithShadow(textRenderer, widget.label, 20, 5, 0xfff0f4fa);
            context.drawTextWithShadow(textRenderer, widget.preview, 7, 14, 0xff8ba0b8);

            context.getMatrices().pop();
        }

        renderToolbar(context, mouseX, mouseY);
    }

    private void renderToolbar(DrawContext context, int mouseX, int mouseY) {
        int barWidth = Math.min(480, width - 24);
        int left = (width - barWidth) / 2;
        int top = Math.max(12, height - 42);

        context.fill(left - 8, top - 4, left + barWidth + 8, top + 30, 0xee101724);
        AzureHud.drawBorder(context, left - 8, top - 4, barWidth + 16, 34, 0x444b6485);

        context.drawTextWithShadow(textRenderer, selected.label, left, top + 9, colorFor(selected));

        drawToolbarButton(context, left + 110, top, 34, "−", mouseX, mouseY);
        drawToolbarButton(context, left + 148, top, 34, "+", mouseX, mouseY);

        int paletteX = left + 192;
        for (int i = 0; i < PALETTE.length; i++) {
            int px = paletteX + i * 18;
            if (PALETTE[i] == colorFor(selected)) {
                context.fill(px - 2, top + 4, px + 14, top + 20, 0xffffffff);
            }
            context.fill(px, top + 6, px + 12, top + 18, PALETTE[i]);
        }

        drawToolbarButton(context, left + barWidth - 65, top, 65, "DONE", mouseX, mouseY);
    }

    private void drawToolbarButton(DrawContext context, int x, int y, int btnWidth, String label, int mouseX, int mouseY) {
        boolean hov = mouseX >= x && mouseX < x + btnWidth && mouseY >= y && mouseY < y + 24;
        context.fill(x, y, x + btnWidth, y + 24, hov ? 0xff25384c : 0xff162232);
        AzureHud.drawBorder(context, x, y, btnWidth, 24, hov ? 0xff49d8ce : 0x33445873);
        context.drawCenteredTextWithShadow(textRenderer, label, x + btnWidth / 2, y + 8,
            hov ? 0xff49d8ce : 0xffe8edf5);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int barWidth = Math.min(480, width - 24);
        int left = (width - barWidth) / 2;
        int top = Math.max(12, height - 42);
        int cx = (int) mouseX;
        int cy = (int) mouseY;

        // Toolbar actions
        if (cy >= top && cy < top + 26) {
            if (cx >= left + 110 && cx < left + 144) {
                resize(-0.1f);
                return true;
            } else if (cx >= left + 148 && cx < left + 182) {
                resize(0.1f);
                return true;
            } else if (cx >= left + 192 && cx < left + 192 + PALETTE.length * 18) {
                int palIdx = Math.max(0, Math.min(PALETTE.length - 1, (cx - left - 192) / 18));
                setColor(selected, PALETTE[palIdx]);
                AzureClient.config().hudPreset = "custom";
                AzureClient.config().save();
                return true;
            } else if (cx >= left + barWidth - 65) {
                close();
                return true;
            }
        }

        // Check clicking on a widget
        for (Widget widget : Widget.values()) {
            int[] pos = widgetPosition(widget);
            float scale = widgetScale(widget);
            int w = (int) (widget.width * scale);
            int h = (int) (widget.height * scale);

            if (cx >= pos[0] && cx < pos[0] + w && cy >= pos[1] && cy < pos[1] + h) {
                selected = widget;
                dragging = true;
                dragOffsetX = cx - pos[0];
                dragOffsetY = cy - pos[1];
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;
            float scale = widgetScale(selected);
            int w = (int) (selected.width * scale);
            int h = (int) (selected.height * scale);

            newX = Math.max(0, Math.min(width - w, newX));
            newY = Math.max(0, Math.min(height - h, newY));

            // Snap to screen center
            if (Math.abs(newX + w / 2 - width / 2) < 8) newX = width / 2 - w / 2;
            if (Math.abs(newY + h / 2 - height / 2) < 8) newY = height / 2 - h / 2;

            setWidgetPosition(selected, newX, newY);
            AzureClient.config().save();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        resize(amount > 0 ? 0.05f : -0.05f);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
