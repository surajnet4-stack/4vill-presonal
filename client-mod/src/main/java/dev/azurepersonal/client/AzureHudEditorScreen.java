package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AzureHudEditorScreen extends Screen {
    private enum Widget {
        FPS("FPS"),
        KEYCAPS("KEYCAPS"),
        BLOCK("BLOCK INFO");

        private final String label;

        Widget(String label) {
            this.label = label;
        }
    }

    private static final int[] PALETTE = {
        0xfff5f7ff, 0xff49d8ce, 0xff8ab4ff, 0xffffca78,
        0xffff7f9f, 0xffc6a0ff, 0xff80e0a0
    };
    private Widget selected = Widget.FPS;
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
        AzureConfig config = AzureClient.config();
        return switch (widget) {
            case FPS -> config.fpsColor;
            case KEYCAPS -> config.keycapsColor;
            case BLOCK -> config.blockOverlayColor;
        };
    }

    private static void setColor(Widget widget, int color) {
        AzureConfig config = AzureClient.config();
        switch (widget) {
            case FPS -> config.fpsColor = color;
            case KEYCAPS -> config.keycapsColor = color;
            case BLOCK -> config.blockOverlayColor = color;
        }
    }

    private float widgetScale(Widget widget) {
        AzureConfig config = AzureClient.config();
        return switch (widget) {
            case FPS -> config.fpsScale;
            case KEYCAPS -> config.keycapsScale;
            case BLOCK -> config.blockOverlayScale;
        };
    }

    private void resize(float amount) {
        AzureConfig config = AzureClient.config();
        config.hudPreset = "custom";
        switch (selected) {
            case FPS -> config.fpsScale = AzureClient.clampScale(config.fpsScale + amount);
            case KEYCAPS -> config.keycapsScale = AzureClient.clampScale(config.keycapsScale + amount);
            case BLOCK -> config.blockOverlayScale = AzureClient.clampScale(config.blockOverlayScale + amount);
        }
        config.save();
    }

    private int[] widgetPosition(Widget widget) {
        AzureConfig config = AzureClient.config();
        return switch (widget) {
            case FPS -> new int[]{config.fpsX, config.fpsY};
            case KEYCAPS -> new int[]{
                config.keycapsX < 0 ? width / 2 - 31 : config.keycapsX,
                config.keycapsY < 0 ? height - 75 : config.keycapsY
            };
            case BLOCK -> new int[]{config.blockOverlayX, config.blockOverlayY};
        };
    }

    private void setWidgetPosition(Widget widget, int x, int y) {
        AzureConfig config = AzureClient.config();
        config.hudPreset = "custom";
        switch (widget) {
            case FPS -> {
                config.fpsX = x;
                config.fpsY = y;
            }
            case KEYCAPS -> {
                config.keycapsX = x;
                config.keycapsY = y;
            }
            case BLOCK -> {
                config.blockOverlayX = x;
                config.blockOverlayY = y;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x44070b12);
        context.fill(12, 12, width - 12, 48, 0xcc101722);
        context.fill(12, 12, width - 12, 14, 0xff49d8ce);
        context.drawTextWithShadow(textRenderer, "HUD EDITOR", 24, 26, 0xfff3f7ff);
        context.drawTextWithShadow(textRenderer, "Drag to place  ·  Scroll over an item to resize  ·  Use the toolbar to color",
            116, 26, 0xffaab8cb);

        for (Widget widget : Widget.values()) {
            int[] position = widgetPosition(widget);
            float scale = widgetScale(widget);
            context.getMatrices().push();
            context.getMatrices().translate(position[0], position[1], 0);
            context.getMatrices().scale(scale, scale, 1);
            context.fill(-5, -4, 128, 30, widget == selected ? 0xdd203247 : 0xb9111924);
            context.fill(-5, -4, -2, 30, colorFor(widget));
            context.drawTextWithShadow(textRenderer, icon(widget), 3, 3, colorFor(widget));
            context.drawTextWithShadow(textRenderer, preview(widget), 19, 3, colorFor(widget));
            context.drawTextWithShadow(textRenderer, widget.label, 3, 16, 0xffaab8cb);
            context.getMatrices().pop();
        }
        renderToolbar(context, mouseX, mouseY);
    }

    private void renderToolbar(DrawContext context, int mouseX, int mouseY) {
        int toolbarWidth = Math.min(430, width - 24);
        int left = (width - toolbarWidth) / 2;
        int top = Math.max(12, height - 48);
        context.fill(left - 8, top - 5, left + toolbarWidth + 8, top + 30, 0xe610151e);
        context.drawTextWithShadow(textRenderer, selected.label, left, top + 8, colorFor(selected));
        drawToolbarButton(context, left + 82, top, 44, "−", mouseX, mouseY);
        drawToolbarButton(context, left + 130, top, 44, "+", mouseX, mouseY);
        int paletteX = left + 182;
        for (int index = 0; index < PALETTE.length; index++) {
            int x = paletteX + index * 18;
            if (PALETTE[index] == colorFor(selected)) {
                context.fill(x - 2, top + 3, x + 14, top + 19, 0xfff0f4fa);
            }
            context.fill(x, top + 5, x + 12, top + 17, PALETTE[index]);
        }
        drawToolbarButton(context, left + toolbarWidth - 70, top, 70, "DONE", mouseX, mouseY);
    }

    private void drawToolbarButton(DrawContext context, int x, int y, int buttonWidth,
                                  String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + buttonWidth &&
            mouseY >= y && mouseY < y + 24;
        context.fill(x, y, x + buttonWidth, y + 24, hovered ? 0xff2c3c4d : 0xff202a36);
        context.drawCenteredTextWithShadow(textRenderer, label, x + buttonWidth / 2, y + 8,
            hovered ? 0xff49d8ce : 0xffe8edf5);
    }

    private static String icon(Widget widget) {
        return switch (widget) {
            case FPS -> "F";
            case KEYCAPS -> "K";
            case BLOCK -> "B";
        };
    }

    private static String preview(Widget widget) {
        return switch (widget) {
            case FPS -> "144 FPS";
            case KEYCAPS -> "W  A  S  D";
            case BLOCK -> "Stone  12, 64, -8";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int toolbarWidth = Math.min(430, width - 24);
        int left = (width - toolbarWidth) / 2;
        int top = Math.max(12, height - 48);
        int clickX = (int) mouseX;
        int clickY = (int) mouseY;
        if (clickY >= top && clickY < top + 24) {
            if (clickX >= left + 82 && clickX < left + 126) {
                resize(-0.1f);
            } else if (clickX >= left + 130 && clickX < left + 174) {
                resize(0.1f);
            } else if (clickX >= left + 182 && clickX < left + 308) {
                int paletteIndex = Math.max(0, Math.min(6, (clickX - left - 182) / 18));
                setColor(selected, PALETTE[paletteIndex]);
                AzureClient.config().hudPreset = "custom";
                AzureClient.config().save();
            } else if (clickX >= left + toolbarWidth - 70) {
                close();
            }
            return true;
        }

        for (Widget widget : Widget.values()) {
            int[] position = widgetPosition(widget);
            if (mouseX >= position[0] - 5 && mouseX <= position[0] + 130 &&
                mouseY >= position[1] - 5 && mouseY <= position[1] + 34) {
                selected = widget;
                dragging = true;
                dragOffsetX = (int) mouseX - position[0];
                dragOffsetY = (int) mouseY - position[1];
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            setWidgetPosition(selected, Math.max(0, Math.min(width - 130, (int) mouseX - dragOffsetX)),
                Math.max(52, Math.min(height - 70, (int) mouseY - dragOffsetY)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            AzureClient.config().save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleMouseScrolled(mouseX, mouseY, amount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleMouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double amount) {
        for (Widget widget : Widget.values()) {
            int[] position = widgetPosition(widget);
            if (mouseX >= position[0] - 5 && mouseX <= position[0] + 130 &&
                mouseY >= position[1] - 5 && mouseY <= position[1] + 34) {
                selected = widget;
                resize(amount > 0 ? 0.1f : -0.1f);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            selected = Widget.values()[(selected.ordinal() + 1) % Widget.values().length];
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            selected = Widget.values()[(selected.ordinal() + Widget.values().length - 1) % Widget.values().length];
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
