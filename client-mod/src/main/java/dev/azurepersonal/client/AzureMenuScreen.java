package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AzureMenuScreen extends Screen {
    private enum Tab {
        HUD("HUD", "On-screen widgets"),
        CROSSHAIR("CROSSHAIR", "Shape, scale and color"),
        WORLD("WORLD", "Visual enhancements"),
        TOOLS("TOOLS", "PvP and survival utilities"),
        THEMES("THEMES", "Colors and transparency"),
        PRESETS("PRESETS", "Profiles for this instance");

        private final String title;
        private final String subtitle;

        Tab(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 530;
    private static final int[] PALETTE = {
        0xfff5f7ff, 0xff49d8ce, 0xff8ab4ff, 0xffffca78,
        0xffff7f9f, 0xffc6a0ff, 0xff80e0a0
    };
    private static final Tab[] TABS = Tab.values();
    private static final int[] HUD_FEATURES = {0, 1, 2, 3, 4, 5};
    private Tab selectedTab = Tab.HUD;
    private long openedAt;
    private final Screen parent;

    public AzureMenuScreen() {
        this(null);
    }

    public AzureMenuScreen(Screen parent) {
        super(Text.literal("Azure Personal"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
    }

    private int panelLeft() {
        return Math.max(8, (width - PANEL_WIDTH) / 2);
    }

    private int panelTop() {
        return Math.max(8, (height - PANEL_HEIGHT) / 2);
    }

    private int contentLeft() {
        return panelLeft() + 184;
    }

    private int contentTop() {
        return panelTop() + 82;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x70060a12);
        AzureConfig config = AzureClient.config();
        int accent = config.accentColor();
        double progress = Math.min(1.0, (System.currentTimeMillis() - openedAt) / 260.0);
        int lift = (int) ((1.0 - progress) * 12);
        int left = panelLeft();
        int top = panelTop() + lift;
        context.fill(left + 4, top + 7, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 7, 0x66000000);
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, withOpacity(0xff10151e, config.menuOpacity));
        context.fill(left, top, left + PANEL_WIDTH, top + 2, accent);
        context.fill(left + 1, top + 1, left + 168, top + PANEL_HEIGHT - 1,
            withOpacity(0xff141b27, config.menuOpacity));
        context.fill(left + 168, top + 1, left + 169, top + PANEL_HEIGHT - 1, 0xff2a3544);

        context.fill(left + 20, top + 20, left + 45, top + 45, accent);
        context.drawCenteredTextWithShadow(textRenderer, "A", left + 32, top + 28, 0xff10151e);
        context.drawTextWithShadow(textRenderer, "AZURE PERSONAL", left + 54, top + 22, 0xfff1f5fb);
        context.drawTextWithShadow(textRenderer, "CLIENT MENU", left + 54, top + 35, 0xff91a0b4);

        int navY = top + 78;
        for (int index = 0; index < TABS.length; index++) {
            int y = navY + index * 34;
            boolean active = selectedTab == TABS[index];
            boolean hovered = mouseX >= left + 13 && mouseX <= left + 155 &&
                mouseY >= y && mouseY <= y + 30;
            if (active || hovered) {
                context.fill(left + 13, y, left + 155, y + 30, active ? 0xff253444 : 0xff1b2734);
            }
            if (active) context.fill(left + 13, y, left + 16, y + 30, accent);
            context.drawTextWithShadow(textRenderer, active ? ">" : "-", left + 25, y + 10,
                active ? accent : 0xff8292a7);
            context.drawTextWithShadow(textRenderer, TABS[index].title, left + 43, y + 10,
                active ? 0xfff3f7fc : 0xffb8c2d0);
        }
        context.drawTextWithShadow(textRenderer, "RIGHT SHIFT  ·  CLOSE", left + 19,
            top + PANEL_HEIGHT - 29, 0xff8e9caf);

        int translatedMouseY = mouseY - lift;
        int x = contentLeft();
        int y = contentTop() + lift;
        context.drawTextWithShadow(textRenderer, selectedTab.subtitle, x, top + 58, 0xffaab8cb);
        switch (selectedTab) {
            case HUD -> renderHudTab(context, x, y, mouseX, translatedMouseY);
            case CROSSHAIR -> renderCrosshairTab(context, x, y, mouseX, translatedMouseY);
            case WORLD -> renderWorldTab(context, x, y, mouseX, translatedMouseY);
            case TOOLS -> renderToolsTab(context, x, y, mouseX, translatedMouseY);
            case THEMES -> renderThemesTab(context, x, y, mouseX, translatedMouseY);
            case PRESETS -> renderPresetsTab(context, x, y, mouseX, translatedMouseY);
        }
        context.drawTextWithShadow(textRenderer, "Settings save automatically", x,
            top + PANEL_HEIGHT - 29, 0xff7f8da0);
    }

    private void renderHudTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        String[] labels = {"FPS counter", "Movement keys", "Block information", "Armor status", "Coordinates", "Direction"};
        String[] icons = {"F", "K", "B", "A", "X", "N"};
        String[] descriptions = {"Live frame rate", "WASD and jump", "Target block + position", "Equipment durability", "XYZ position", "Facing direction"};
        int columns = 2;
        int gap = 9;
        int cardWidth = (PANEL_WIDTH - 184 - 30 - gap) / columns;
        int cardHeight = 68;
        for (int index : HUD_FEATURES) {
            int column = index % columns;
            int row = index / columns;
            int cardX = x + column * (cardWidth + gap);
            int cardY = y + row * (cardHeight + 9);
            boolean enabled = hudFeatureEnabled(index);
            drawFeatureCard(context, cardX, cardY, cardWidth, cardHeight, mouseX, mouseY,
                labels[index], icons[index], descriptions[index], enabled);
        }
        int editY = y + 3 * (cardHeight + 9);
        drawActionCard(context, x, editY, PANEL_WIDTH - 184 - 30, 36,
            mouseX, mouseY, "EDIT HUD  ·  DRAG, RESIZE & COLOR", 0xff49d8ce);
    }

    private void renderCrosshairTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        AzureConfig config = AzureClient.config();
        drawFeatureCard(context, x, y, 190, 68, mouseX, mouseY,
            "Custom crosshair", "+", "Replace vanilla reticle", config.customCrosshair);
        drawCrosshairPreview(context, x + 230, y + 4, mouseX, mouseY);

        int styleY = y + 83;
        String[] styles = {"CROSS", "DOT", "RING"};
        int styleWidth = 91;
        for (int index = 0; index < styles.length; index++) {
            int styleX = x + index * (styleWidth + 7);
            boolean active = config.crosshairStyle == index;
            boolean hovered = inside(mouseX, mouseY, styleX, styleY, styleWidth, 30);
            drawChip(context, styleX, styleY, styleWidth, 30, styles[index], active, hovered);
        }
        drawValueControl(context, x, y + 124, "SIZE", config.crosshairSize, 3, 16, 0, mouseX, mouseY);
        drawValueControl(context, x, y + 165, "GAP", config.crosshairGap, 1, 12, 1, mouseX, mouseY);
        drawValueControl(context, x, y + 206, "THICKNESS", config.crosshairThickness, 1, 6, 2, mouseX, mouseY);
        drawPalette(context, x, y + 255, config.crosshairColor, mouseX, mouseY);
        drawCompactToggleCard(context, x, y + 284, 190, 42, mouseX, mouseY,
            "HIT COLOR", config.crosshairHitColor);
        drawCompactToggleCard(context, x + 199, y + 284, 190, 42, mouseX, mouseY,
            "CENTER DOT", config.crosshairCenterDot);
        drawCompactToggleCard(context, x, y + 333, 190, 42, mouseX, mouseY,
            "DARK OUTLINE", config.crosshairOutline);
        drawPalette(context, x + 199, y + 345, config.crosshairHitColorValue, PALETTE, mouseX, mouseY);
        drawValueControl(context, x, y + 382, "CROSSHAIR OPACITY", config.crosshairOpacity, 20, 100, 6, mouseX, mouseY);
    }

    private void renderWorldTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        AzureConfig config = AzureClient.config();
        drawFeatureCard(context, x, y, 190, 68, mouseX, mouseY,
            "Fullbright", "+", "Adjustable brightness boost", config.fullBright);
        drawFeatureCard(context, x + 199, y, 190, 68, mouseX, mouseY,
            "Block outline", "O", "Custom target edge color", config.blockOutline);
        drawFeatureCard(context, x, y + 78, 190, 68, mouseX, mouseY,
            "Block information", "B", "Name and coordinates", config.showBlockOverlay);
        drawValueControl(context, x, y + 158, "BRIGHTNESS", config.fullBrightLevel, 100, 300, 3, mouseX, mouseY);
        int colorY = y + 196;
        context.drawTextWithShadow(textRenderer, "OUTLINE COLOR", x, colorY, 0xffaab8cb);
        drawPalette(context, x, colorY + 20, config.blockOutlineColor, mouseX, mouseY);
        int freeLookY = y + 261;
        drawActionCard(context, x, freeLookY, PANEL_WIDTH - 184 - 30, 42, mouseX, mouseY,
            AzureClient.isFreeLookActive()
                ? "FREE LOOK  ·  ON  (V to return)"
                : "FREE LOOK  ·  OFF  (V to look around)",
            AzureClient.isFreeLookActive() ? 0xff49d8ce : 0xffaab8cb);
    }

    private void renderToolsTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        drawFeatureCard(context, x, y, 190, 68, mouseX, mouseY,
            "Toggle sprint", "S", "Press K while playing", AzureClient.isSprintToggled());
        drawFeatureCard(context, x + 199, y, 190, 68, mouseX, mouseY,
            "CPS counter", "C", "Attack clicks per second", AzureClient.config().showCps);
        drawActionCard(context, x, y + 80, 190, 45, mouseX, mouseY,
            "RECIPE BOOK  ·  PRESS R", 0xff8ab4ff);
        drawActionCard(context, x + 199, y + 80, 190, 45, mouseX, mouseY,
            "INVENTORY SORT  ·  SORT BUTTON", 0xff49d8ce);
        drawCompactToggleCard(context, x, y + 137, PANEL_WIDTH - 184 - 30, 42, mouseX, mouseY,
            "AUTO SPRINT  ·  FORWARD TO SPRINT, SNEAK TO WALK", AzureClient.config().autoSprint);
        drawValueControl(context, x, y + 194, "ZOOM (HOLD C)", AzureClient.config().zoomLevel, 2, 10, 7, mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Sorting uses normal inventory clicks and only moves",
            x, y + 238, 0xffaab8cb);
        context.drawTextWithShadow(textRenderer, "the main inventory rows; your hotbar stays untouched.",
            x, y + 254, 0xff8292a7);
        context.drawTextWithShadow(textRenderer, "Recipe Book opens Minecraft's built-in crafting helper.",
            x, y + 280, 0xffaab8cb);
    }

    private void renderThemesTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        AzureConfig config = AzureClient.config();
        String[] themes = {"AZURE", "EMBER", "VIOLET", "ROSE"};
        int[] colors = {0xff49d8ce, 0xffff8a5b, 0xffa78bfa, 0xfff472b6};
        for (int index = 0; index < themes.length; index++) {
            int chipX = x + index * 97;
            boolean selected = config.theme == index;
            boolean hovered = inside(mouseX, mouseY, chipX, y, 90, 48);
            context.fill(chipX, y, chipX + 90, y + 48, selected || hovered ? 0xff253444 : 0xff1a222d);
            context.fill(chipX, y, chipX + 90, y + 3, colors[index]);
            context.drawCenteredTextWithShadow(textRenderer, themes[index], chipX + 45, y + 19,
                selected ? colors[index] : 0xffc2cbd6);
        }
        context.drawTextWithShadow(textRenderer, "LIVE PREVIEW", x, y + 72, 0xffaab8cb);
        int accent = config.accentColor();
        context.fill(x, y + 94, x + 389, y + 130, withOpacity(0xff1a222d, config.menuOpacity));
        context.fill(x, y + 94, x + 3, y + 130, accent);
        context.drawTextWithShadow(textRenderer, "Azure Personal  ·  Active theme", x + 14, y + 107, 0xfff1f5fa);
        drawValueControl(context, x, y + 148, "MENU OPACITY", config.menuOpacity, 35, 100, 4, mouseX, mouseY);
        drawValueControl(context, x, y + 194, "HUD BACKGROUND", config.hudOpacity, 20, 100, 5, mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Opacity changes affect menu panels and HUD chips.",
            x, y + 238, 0xff91a0b3);
        context.drawTextWithShadow(textRenderer, "Theme and opacity preferences are saved locally.",
            x, y + 256, 0xff8292a7);
    }

    private void renderPresetsTab(DrawContext context, int x, int y, int mouseX, int mouseY) {
        AzureConfig config = AzureClient.config();
        int cardWidth = 190;
        int cardHeight = 92;
        drawPresetCard(context, x, y, cardWidth, cardHeight, mouseX, mouseY,
            "PVP", "Fast read, low clutter", "FPS  ·  CPS  ·  keys", "pvp".equals(config.hudPreset),
            0xfff472b6);
        drawPresetCard(context, x + 199, y, cardWidth, cardHeight, mouseX, mouseY,
            "SMP", "World details at a glance", "XYZ  ·  facing  ·  armor", "smp".equals(config.hudPreset),
            0xff49d8ce);
        drawPresetCard(context, x, y + 103, cardWidth, cardHeight, mouseX, mouseY,
            "MINIMAL", "Only the essentials", "FPS  ·  CPS", "minimal".equals(config.hudPreset),
            0xffffca78);
        drawPresetCard(context, x + 199, y + 103, cardWidth, cardHeight, mouseX, mouseY,
            "CUSTOM", "Your current arrangement", "Keep your own choices", "custom".equals(config.hudPreset),
            0xffa78bfa);

        int infoY = y + 218;
        context.fill(x, infoY, x + 389, infoY + 68, withOpacity(0xff1a222d, config.menuOpacity));
        context.fill(x, infoY, x + 3, infoY + 68, config.accentColor());
        context.drawTextWithShadow(textRenderer, "SAVED FOR THIS INSTANCE", x + 14, infoY + 12, config.accentColor());
        context.drawTextWithShadow(textRenderer, AzureConfig.instanceLabel(), x + 14, infoY + 31, 0xfff1f5fa);
        context.drawTextWithShadow(textRenderer, "HUD layout stays private to this game instance.",
            x + 14, infoY + 49, 0xff91a0b3);
        context.drawTextWithShadow(textRenderer, "Tip  ·  Use EDIT HUD to place and scale each widget.",
            x, infoY + 91, 0xff8292a7);
    }

    private void drawPresetCard(DrawContext context, int x, int y, int cardWidth, int cardHeight,
                                int mouseX, int mouseY, String title, String description,
                                String features, boolean active, int accent) {
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
        context.fill(x, y, x + cardWidth, y + cardHeight,
            active ? withOpacity(accent, 42) : hovered ? 0xff253444 : 0xff1a222d);
        context.fill(x, y, x + 3, y + cardHeight, active ? accent : 0xff344355);
        context.drawTextWithShadow(textRenderer, title, x + 13, y + 13, active ? accent : 0xfff1f5fa);
        context.drawTextWithShadow(textRenderer, active ? "ACTIVE" : "APPLY", x + cardWidth - 48, y + 14,
            active ? accent : 0xff8292a7);
        context.drawTextWithShadow(textRenderer, description, x + 13, y + 39, 0xffc2cbd6);
        context.drawTextWithShadow(textRenderer, features, x + 13, y + 62, 0xff8292a7);
        if (active) context.fill(x + 13, y + cardHeight - 5, x + cardWidth - 13, y + cardHeight - 3, accent);
    }

    private void drawFeatureCard(DrawContext context, int x, int y, int cardWidth, int cardHeight,
                                 int mouseX, int mouseY, String label, String icon, String description,
                                 boolean enabled) {
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
        context.fill(x, y, x + cardWidth, y + cardHeight,
            hovered ? 0xff253444 : enabled ? 0xff202a36 : 0xff1a222d);
        int accent = AzureClient.config().accentColor();
        context.fill(x, y, x + 2, y + cardHeight, enabled ? accent : 0xff344355);
        context.drawTextWithShadow(textRenderer, icon, x + 12, y + 12, enabled ? accent : 0xff92a1b4);
        context.drawTextWithShadow(textRenderer, label, x + 32, y + 9, 0xfff1f5fa);
        context.drawTextWithShadow(textRenderer, description, x + 32, y + 25, 0xff91a0b3);
        int pillX = x + cardWidth - 38;
        int pillY = y + cardHeight - 21;
        context.fill(pillX, pillY, pillX + 27, pillY + 13, enabled ? accent : 0xff394452);
        context.fill(enabled ? pillX + 15 : pillX + 2, pillY + 2, enabled ? pillX + 25 : pillX + 12,
            pillY + 11, 0xfff5f7ff);
    }

    private void drawCompactToggleCard(DrawContext context, int x, int y, int cardWidth, int cardHeight,
                                       int mouseX, int mouseY, String label, boolean enabled) {
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
        int accent = AzureClient.config().accentColor();
        context.fill(x, y, x + cardWidth, y + cardHeight, hovered ? 0xff253444 : 0xff1a222d);
        context.fill(x, y, x + 2, y + cardHeight, enabled ? accent : 0xff344355);
        context.drawTextWithShadow(textRenderer, label, x + 12, y + 16, 0xfff1f5fa);
        String state = enabled ? "ON" : "OFF";
        context.drawTextWithShadow(textRenderer, state, x + cardWidth - 50, y + 16,
            enabled ? accent : 0xff8292a7);
        context.fill(x + cardWidth - 28, y + 14, x + cardWidth - 8, y + 27,
            enabled ? accent : 0xff394452);
        int knobX = enabled ? x + cardWidth - 17 : x + cardWidth - 27;
        context.fill(knobX, y + 16, knobX + 7, y + 25, 0xfff5f7ff);
    }

    private void drawActionCard(DrawContext context, int x, int y, int cardWidth, int cardHeight,
                                int mouseX, int mouseY, String label, int accent) {
        boolean hovered = inside(mouseX, mouseY, x, y, cardWidth, cardHeight);
        context.fill(x, y, x + cardWidth, y + cardHeight, hovered ? 0xff253444 : 0xff1a222d);
        context.fill(x, y, x + 2, y + cardHeight, accent);
        context.drawCenteredTextWithShadow(textRenderer, label, x + cardWidth / 2, y + (cardHeight - 8) / 2,
            hovered ? 0xfff5f7ff : accent);
    }

    private void drawChip(DrawContext context, int x, int y, int chipWidth, int chipHeight,
                          String label, boolean active, boolean hovered) {
        context.fill(x, y, x + chipWidth, y + chipHeight,
            active ? 0xff25423f : hovered ? 0xff253444 : 0xff1a222d);
        int accent = AzureClient.config().accentColor();
        if (active) context.fill(x, y, x + chipWidth, y + 2, accent);
        context.drawCenteredTextWithShadow(textRenderer, label, x + chipWidth / 2, y + (chipHeight - 8) / 2,
            active ? accent : 0xffc2cbd6);
    }

    private void drawValueControl(DrawContext context, int x, int y, String label, int value,
                                  int min, int max, int property, int mouseX, int mouseY) {
        int controlWidth = PANEL_WIDTH - 184 - 30;
        context.drawTextWithShadow(textRenderer, label, x, y + 8, 0xffaab8cb);
        int buttonX = x + 128;
        drawChip(context, buttonX, y, 36, 25, "−", false, inside(mouseX, mouseY, buttonX, y, 36, 25));
        context.drawCenteredTextWithShadow(textRenderer, Integer.toString(value), buttonX + 69, y + 8, 0xfff0f4fa);
        int plusX = buttonX + 102;
        drawChip(context, plusX, y, 36, 25, "+", false, inside(mouseX, mouseY, plusX, y, 36, 25));
        int barX = plusX + 47;
        int barWidth = Math.max(18, controlWidth - (barX - x) - 4);
        context.fill(barX, y + 11, barX + barWidth, y + 14, 0xff344152);
        int fillWidth = (int) ((barWidth - 4) * ((value - min) / (double) Math.max(1, max - min)));
        context.fill(barX, y + 11, barX + 3 + fillWidth, y + 14, AzureClient.config().accentColor());
    }

    private void drawPalette(DrawContext context, int x, int y, int selectedColor, int mouseX, int mouseY) {
        drawPalette(context, x, y, selectedColor, PALETTE, mouseX, mouseY);
    }

    private void drawPalette(DrawContext context, int x, int y, int selectedColor, int[] colors, int mouseX, int mouseY) {
        for (int index = 0; index < colors.length; index++) {
            int colorX = x + index * 30;
            boolean selected = colors[index] == selectedColor;
            if (selected) context.fill(colorX - 3, y - 3, colorX + 20, y + 20, 0xfff0f4fa);
            boolean hovered = inside(mouseX, mouseY, colorX - 2, y - 2, 20, 20);
            context.fill(colorX, y, colorX + (hovered ? 17 : 15), y + (hovered ? 17 : 15),
                colors[index]);
        }
    }

    private static int withOpacity(int color, int opacityPercent) {
        int alpha = Math.max(0, Math.min(255, opacityPercent * 255 / 100));
        return (color & 0x00ffffff) | (alpha << 24);
    }

    private void drawCrosshairPreview(DrawContext context, int x, int y, int mouseX, int mouseY) {
        AzureConfig config = AzureClient.config();
        int size = Math.min(config.crosshairSize, 12);
        int centerX = x + 54;
        int centerY = y + 30;
        context.fill(x, y, x + 126, y + 67, 0xff111821);
        context.drawCenteredTextWithShadow(textRenderer, "PREVIEW", centerX, y + 7, 0xff8292a7);
        int color = withOpacity(config.crosshairColor, config.crosshairOpacity);
        switch (config.crosshairStyle) {
            case 1 -> context.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, color);
            case 2 -> {
                context.fill(centerX - size, centerY - 1, centerX + size + 1, centerY + 2, color);
                context.fill(centerX - 1, centerY - size, centerX + 2, centerY + size + 1, color);
                context.fill(centerX - size + 2, centerY - 2, centerX + size - 1, centerY + 3, 0xff111821);
                context.fill(centerX - 2, centerY - size + 2, centerX + 3, centerY + size - 1, 0xff111821);
            }
            default -> {
                int gap = config.crosshairGap;
                int thickness = config.crosshairThickness;
                context.fill(centerX - thickness / 2, centerY - gap - size, centerX + (thickness + 1) / 2,
                    centerY - gap, color);
                context.fill(centerX - thickness / 2, centerY + gap + 1, centerX + (thickness + 1) / 2,
                    centerY + gap + size + 1, color);
                context.fill(centerX - gap - size, centerY - thickness / 2, centerX - gap,
                    centerY + (thickness + 1) / 2, color);
                context.fill(centerX + gap + 1, centerY - thickness / 2, centerX + gap + size + 1,
                    centerY + (thickness + 1) / 2, color);
            }
        }
        if (config.crosshairCenterDot) {
            context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int targetWidth, int targetHeight) {
        return mouseX >= x && mouseX < x + targetWidth && mouseY >= y && mouseY < y + targetHeight;
    }

    private static boolean hudFeatureEnabled(int index) {
        AzureConfig config = AzureClient.config();
        return switch (index) {
            case 0 -> config.showFps;
            case 1 -> config.showKeycaps;
            case 2 -> config.showBlockOverlay;
            case 3 -> config.showArmor;
            case 4 -> config.showCoordinates;
            default -> config.showDirection;
        };
    }

    private static void toggleHudFeature(int index) {
        AzureConfig config = AzureClient.config();
        switch (index) {
            case 0 -> config.showFps = !config.showFps;
            case 1 -> config.showKeycaps = !config.showKeycaps;
            case 2 -> config.showBlockOverlay = !config.showBlockOverlay;
            case 3 -> config.showArmor = !config.showArmor;
            case 4 -> config.showCoordinates = !config.showCoordinates;
            default -> config.showDirection = !config.showDirection;
        }
        config.hudPreset = "custom";
        config.save();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int left = panelLeft();
        int top = panelTop();
        int lift = (int) ((1.0 - Math.min(1.0, (System.currentTimeMillis() - openedAt) / 260.0)) * 12);
        int adjustedY = (int) mouseY - lift;
        int navY = top + 78;
        for (int index = 0; index < TABS.length; index++) {
            if (inside((int) mouseX, adjustedY, left + 13, navY + index * 34, 142, 30)) {
                selectedTab = TABS[index];
                return true;
            }
        }

        int x = contentLeft();
        int y = contentTop();
        if (selectedTab == Tab.HUD) {
            int gap = 9;
            int cardWidth = (PANEL_WIDTH - 184 - 30 - gap) / 2;
            int cardHeight = 68;
            for (int index : HUD_FEATURES) {
                int cardX = x + (index % 2) * (cardWidth + gap);
                int cardY = y + (index / 2) * (cardHeight + 9);
                if (inside((int) mouseX, adjustedY, cardX, cardY, cardWidth, cardHeight)) {
                    toggleHudFeature(index);
                    return true;
                }
            }
            int editY = y + 3 * (cardHeight + 9);
            if (inside((int) mouseX, adjustedY, x, editY, PANEL_WIDTH - 184 - 30, 36)) {
                client.setScreen(new AzureHudEditorScreen(this));
                return true;
            }
        } else if (selectedTab == Tab.CROSSHAIR) {
            AzureConfig config = AzureClient.config();
            if (inside((int) mouseX, adjustedY, x, y, 190, 68)) {
                config.customCrosshair = !config.customCrosshair;
                config.save();
                return true;
            }
            int styleY = y + 83;
            int styleWidth = 91;
            for (int index = 0; index < 3; index++) {
                int styleX = x + index * (styleWidth + 7);
                if (inside((int) mouseX, adjustedY, styleX, styleY, styleWidth, 30)) {
                    config.crosshairStyle = index;
                    config.save();
                    return true;
                }
            }
            if (handleValueClick(config, (int) mouseX, adjustedY, x, y + 124, 0) ||
                handleValueClick(config, (int) mouseX, adjustedY, x, y + 165, 1) ||
                handleValueClick(config, (int) mouseX, adjustedY, x, y + 206, 2)) return true;
            int colorIndex = paletteIndex((int) mouseX, adjustedY, x, y + 255);
            if (colorIndex >= 0) {
                config.crosshairColor = PALETTE[colorIndex];
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x, y + 284, 190, 42)) {
                config.crosshairHitColor = !config.crosshairHitColor;
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x + 199, y + 284, 190, 42)) {
                config.crosshairCenterDot = !config.crosshairCenterDot;
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x, y + 333, 190, 42)) {
                config.crosshairOutline = !config.crosshairOutline;
                config.save();
                return true;
            }
            colorIndex = paletteIndex((int) mouseX, adjustedY, x + 199, y + 345);
            if (colorIndex >= 0) {
                config.crosshairHitColorValue = PALETTE[colorIndex];
                config.save();
                return true;
            }
            if (handleValueClick(config, (int) mouseX, adjustedY, x, y + 382, 6)) return true;
        } else if (selectedTab == Tab.WORLD) {
            AzureConfig config = AzureClient.config();
            if (inside((int) mouseX, adjustedY, x, y, 190, 68)) {
                config.fullBright = !config.fullBright;
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x + 199, y, 190, 68)) {
                config.blockOutline = !config.blockOutline;
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x, y + 78, 190, 68)) {
                config.showBlockOverlay = !config.showBlockOverlay;
                config.save();
                return true;
            }
            if (handleValueClick(config, (int) mouseX, adjustedY, x, y + 158, 3)) return true;
            int colorIndex = paletteIndex((int) mouseX, adjustedY, x, y + 190);
            if (colorIndex >= 0) {
                config.blockOutlineColor = PALETTE[colorIndex];
                config.save();
                return true;
            }
            if (inside((int) mouseX, adjustedY, x, y + 235, PANEL_WIDTH - 184 - 30, 42)) {
                AzureClient.toggleFreeLook(client);
                return true;
            }
        } else if (selectedTab == Tab.TOOLS &&
            inside((int) mouseX, adjustedY, x, y, 190, 68)) {
            AzureClient.setSprintToggled(!AzureClient.isSprintToggled());
            return true;
        } else if (selectedTab == Tab.TOOLS &&
            inside((int) mouseX, adjustedY, x + 199, y, 190, 68)) {
            AzureClient.config().showCps = !AzureClient.config().showCps;
            AzureClient.config().hudPreset = "custom";
            AzureClient.config().save();
            return true;
        } else if (selectedTab == Tab.TOOLS &&
            inside((int) mouseX, adjustedY, x, y + 137, PANEL_WIDTH - 184 - 30, 45)) {
            AzureClient.setAutoSprint(!AzureClient.config().autoSprint);
            return true;
        } else if (selectedTab == Tab.TOOLS &&
            inside((int) mouseX, adjustedY, x, y + 80, 190, 45)) {
            AzureClient.openRecipeBook(client);
            return true;
        } else if (selectedTab == Tab.TOOLS &&
            inside((int) mouseX, adjustedY, x + 199, y + 80, 190, 45)) {
            if (client.player != null) {
                client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
            }
            return true;
        } else if (selectedTab == Tab.THEMES) {
            AzureConfig config = AzureClient.config();
            for (int index = 0; index < 4; index++) {
                if (inside((int) mouseX, adjustedY, x + index * 97, y, 90, 48)) {
                    config.theme = index;
                    config.save();
                    return true;
                }
            }
            if (handleValueClick(config, (int) mouseX, adjustedY, x, y + 148, 4) ||
                handleValueClick(config, (int) mouseX, adjustedY, x, y + 194, 5)) return true;
        } else if (selectedTab == Tab.PRESETS) {
            String[] presetIds = {"pvp", "smp", "minimal", "custom"};
            int cardWidth = 190;
            int cardHeight = 92;
            for (int index = 0; index < presetIds.length; index++) {
                int cardX = x + (index % 2) * 199;
                int cardY = y + (index / 2) * 103;
                if (inside((int) mouseX, adjustedY, cardX, cardY, cardWidth, cardHeight)) {
                    AzureClient.config().applyHudPreset(presetIds[index]);
                    return true;
                }
            }
        }
        return true;
    }

    private static boolean handleValueClick(AzureConfig config, int mouseX, int mouseY, int x, int y, int property) {
        int minusX = x + 128;
        int plusX = minusX + 102;
        int next;
        if (inside(mouseX, mouseY, minusX, y, 36, 25)) {
            next = changeCrosshairValue(config, property, -1);
        } else if (inside(mouseX, mouseY, plusX, y, 36, 25)) {
            next = changeCrosshairValue(config, property, 1);
        } else {
            return false;
        }
        config.save();
        return true;
    }

    private static int changeCrosshairValue(AzureConfig config, int property, int amount) {
        return switch (property) {
            case 0 -> config.crosshairSize = Math.max(3, Math.min(16, config.crosshairSize + amount));
            case 1 -> config.crosshairGap = Math.max(1, Math.min(12, config.crosshairGap + amount));
            case 2 -> config.crosshairThickness = Math.max(1, Math.min(6, config.crosshairThickness + amount));
            case 3 -> config.fullBrightLevel = Math.max(100, Math.min(300, config.fullBrightLevel + amount * 25));
            case 4 -> config.menuOpacity = Math.max(35, Math.min(100, config.menuOpacity + amount * 5));
            case 5 -> config.hudOpacity = Math.max(20, Math.min(100, config.hudOpacity + amount * 5));
            case 7 -> config.zoomLevel = Math.max(2, Math.min(10, config.zoomLevel + amount));
            default -> config.crosshairOpacity = Math.max(20, Math.min(100, config.crosshairOpacity + amount * 5));
        };
    }

    private static int paletteIndex(int mouseX, int mouseY, int x, int y) {
        for (int index = 0; index < PALETTE.length; index++) {
            int colorX = x + index * 30;
            if (inside(mouseX, mouseY, colorX - 2, y - 2, 20, 20)) return index;
        }
        return -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            selectedTab = TABS[(selectedTab.ordinal() + (keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : 2)) % TABS.length];
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
