package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public final class AzureHomeScreen extends Screen {
    private static final int TEXT = 0xfff3f6fc;
    private static final int MUTED = 0xff95a4b8;
    private static final int PANEL = 0xf20d1420;
    private static final int SURFACE = 0xff141e2c;
    private static final int BORDER = 0xff2a384b;
    private static final int BUTTON_HEIGHT = 28;
    private float entrance;
    private float singleHover;
    private float multiHover;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int entranceOffset;
    private int singleX;
    private int singleY;
    private int singleWidth;
    private int cardHeight;
    private int multiX;

    public AzureHomeScreen() {
        super(Text.literal("Azure Personal"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        entrance = Math.min(1.0f, entrance + delta * 2.8f);
        float ease = 1.0f - (float) Math.pow(1.0f - entrance, 3.0);
        entranceOffset = Math.round((1.0f - ease) * 16.0f);

        renderBackdrop(context);
        renderTopBar(context, mouseX, mouseY);
        calculatePanelLayout();
        panelY += entranceOffset;
        renderPanel(context, mouseX, mouseY, delta);
        renderFooter(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackdrop(DrawContext context) {
        for (int band = 0; band < 18; band++) {
            int y0 = height * band / 18;
            int y1 = height * (band + 1) / 18;
            float blend = band / 17.0f;
            context.fill(0, y0, width, y1, blendColor(0xff080c14, 0xff111a28, blend));
        }

        long time = System.currentTimeMillis();
        int accent = AzureClient.config().accentColor();
        for (int i = 0; i < 16; i++) {
            double phase = time / 3200.0 + i * 1.71;
            int x = (int) ((i * 97L + Math.sin(phase) * 28 + width) % Math.max(1, width));
            int y = (int) ((i * 61L + Math.cos(phase * 0.73) * 18 + height) % Math.max(1, height));
            int alpha = 18 + (i % 4) * 5;
            context.fill(x, y, x + 2, y + 2, (alpha << 24) | (accent & 0x00ffffff));
        }

        int glowX = width * 3 / 4;
        int glowY = height / 3;
        context.fill(glowX - 1, glowY - 1, glowX + 1, glowY + 1, 0x1649d8ce);
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY) {
        int top = 14;
        int left = 24;
        context.fill(left, top, left + 31, top + 31, 0xff12272d);
        context.fill(left, top, left + 3, top + 31, AzureClient.config().accentColor());
        context.drawCenteredTextWithShadow(textRenderer, "A", left + 17, top + 10, AzureClient.config().accentColor());
        context.drawTextWithShadow(textRenderer, "AZURE", left + 42, top + 3, TEXT);
        context.drawTextWithShadow(textRenderer, "PERSONAL CLIENT", left + 42, top + 18, MUTED);

        int[] widths = actionWidths();
        int gap = 7;
        int total = widths[0] + widths[1] + widths[2] + gap * 2;
        int x = width - 24 - total;
        drawTopAction(context, "CLIENT MODS", x, top + 2, widths[0], mouseX, mouseY, 0);
        x += widths[0] + gap;
        drawTopAction(context, "OPTIONS", x, top + 2, widths[1], mouseX, mouseY, 1);
        x += widths[1] + gap;
        drawTopAction(context, "VANILLA UI", x, top + 2, widths[2], mouseX, mouseY, 2);
    }

    private int[] actionWidths() {
        return width < 720 ? new int[] { 84, 64, 82 } : new int[] { 98, 76, 96 };
    }

    private void drawTopAction(DrawContext context, String label, int x, int y, int buttonWidth,
                               int mouseX, int mouseY, int action) {
        boolean hovered = inside(mouseX, mouseY, x, y, buttonWidth, BUTTON_HEIGHT);
        int accent = AzureClient.config().accentColor();
        context.fill(x, y, x + buttonWidth, y + BUTTON_HEIGHT, hovered ? 0xff202f3f : 0xb516202e);
        AzureHud.drawBorder(context, x, y, buttonWidth, BUTTON_HEIGHT, hovered ? accent : BORDER);
        if (hovered) context.fill(x + 1, y + BUTTON_HEIGHT - 2, x + buttonWidth - 1, y + BUTTON_HEIGHT - 1, accent);
        context.drawCenteredTextWithShadow(textRenderer, label, x + buttonWidth / 2, y + 10,
            hovered ? TEXT : (action == 0 ? accent : MUTED));
    }

    private void calculatePanelLayout() {
        panelWidth = Math.max(280, Math.min(1040, width - 48));
        panelHeight = Math.max(188, Math.min(256, height - 150));
        panelX = Math.max(8, (width - panelWidth) / 2);
        panelY = Math.max(70, (height - panelHeight) / 2 + 10);

        int introWidth = Math.min(320, Math.max(208, panelWidth * 39 / 100));
        int gap = 14;
        int tileGap = 12;
        int tileStart = panelX + introWidth + gap;
        singleWidth = Math.max(76, (panelWidth - introWidth - gap * 2 - tileGap) / 2);
        singleX = tileStart;
        multiX = tileStart + singleWidth + tileGap;
        cardHeight = panelHeight - 36;
        singleY = panelY + 18;
    }

    private void renderPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        int accent = AzureClient.config().accentColor();
        context.fill(panelX + 7, panelY + 9, panelX + panelWidth + 7, panelY + panelHeight + 9, 0x55000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, accent);
        AzureHud.drawBorder(context, panelX, panelY, panelWidth, panelHeight, BORDER);

        int introX = panelX + 22;
        int introTop = panelY + 30;
        context.drawTextWithShadow(textRenderer, "YOUR NEXT ADVENTURE", introX, introTop, accent);
        context.drawTextWithShadow(textRenderer, "Make it your world.", introX, introTop + 28, TEXT);
        context.drawTextWithShadow(textRenderer, "A focused home for Minecraft.", introX, introTop + 47, MUTED);
        context.drawTextWithShadow(textRenderer, "Choose a world or meet friends", introX, introTop + 60, MUTED);
        context.drawTextWithShadow(textRenderer, "online. Your client, your way.", introX, introTop + 73, MUTED);

        int badgeY = panelY + panelHeight - 48;
        context.fill(introX, badgeY, introX + 128, badgeY + 24, 0xff14252b);
        context.fill(introX, badgeY, introX + 2, badgeY + 24, accent);
        context.drawTextWithShadow(textRenderer, "AZURE CLIENT READY", introX + 10, badgeY + 8, 0xffc7d7e6);

        boolean singleTarget = inside(mouseX, mouseY, singleX, singleY, singleWidth, cardHeight);
        boolean multiTarget = inside(mouseX, mouseY, multiX, singleY, singleWidth, cardHeight);
        singleHover += ((singleTarget ? 1.0f : 0.0f) - singleHover) * Math.min(1.0f, delta * 12.0f);
        multiHover += ((multiTarget ? 1.0f : 0.0f) - multiHover) * Math.min(1.0f, delta * 12.0f);

        int singleOffset = Math.round(singleHover * 4.0f);
        int multiOffset = Math.round(multiHover * 4.0f);
        drawPlayCard(context, "SINGLEPLAYER", "Create or continue a world", "LOCAL WORLDS",
            singleX, singleY - singleOffset, singleWidth, cardHeight, mouseX, mouseY,
            singleHover, 0xff49d8ce, "S");
        drawPlayCard(context, "MULTIPLAYER", "Join a server with friends", "ONLINE PLAY",
            multiX, singleY - multiOffset, singleWidth, cardHeight, mouseX, mouseY,
            multiHover, 0xff8ab4ff, "M");
    }

    private void drawPlayCard(DrawContext context, String title, String subtitle, String eyebrow,
                              int x, int y, int cardWidth, int cardHeight, int mouseX, int mouseY,
                              float hover, int color, String icon) {
        int bg = hover > 0.05f ? 0xff182536 : SURFACE;
        context.fill(x, y, x + cardWidth, y + cardHeight, bg);
        context.fill(x, y, x + cardWidth, y + 1, blendColor(BORDER, color, hover));
        context.fill(x, y + cardHeight - 2, x + cardWidth, y + cardHeight, blendColor(BORDER, color, hover));
        context.fill(x, y, x + 2, y + cardHeight, blendColor(BORDER, color, hover));
        context.fill(x + cardWidth - 2, y, x + cardWidth, y + cardHeight, blendColor(BORDER, color, hover));

        int iconX = x + 16;
        int iconY = y + 18;
        context.fill(iconX, iconY, iconX + 34, iconY + 34, 0xff1d2b3b);
        context.fill(iconX, iconY, iconX + 2, iconY + 34, color);
        context.drawCenteredTextWithShadow(textRenderer, icon, iconX + 19, iconY + 13, color);
        context.drawTextWithShadow(textRenderer, eyebrow, x + 16, y + 66, MUTED);
        context.drawTextWithShadow(textRenderer, title, x + 16, y + 88, TEXT);
        context.drawTextWithShadow(textRenderer, subtitle, x + 16, y + 105, MUTED);

        int actionY = y + cardHeight - 39;
        int actionColor = hover > 0.05f ? color : 0xff243246;
        context.fill(x + 14, actionY, x + cardWidth - 14, actionY + 25, actionColor);
        context.drawCenteredTextWithShadow(textRenderer, "PLAY  >", x + cardWidth / 2, actionY + 9,
            hover > 0.05f ? 0xff0a1119 : TEXT);
    }

    private void renderFooter(DrawContext context) {
        String version = "AZURE PERSONAL  ·  FABRIC CLIENT";
        context.drawTextWithShadow(textRenderer, version, 24, height - 22, 0xff75869b);
        context.drawTextWithShadow(textRenderer, "RIGHT SHIFT  ·  CLIENT MODS", width - 204, height - 22, 0xff75869b);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int[] widths = actionWidths();
        int gap = 7;
        int x = width - 24 - widths[0] - widths[1] - widths[2] - gap * 2;
        int top = 16;
        if (inside(mouseX, mouseY, x, top, widths[0], BUTTON_HEIGHT)) {
            client.setScreen(new AzureMenuScreen(this));
            return true;
        }
        x += widths[0] + gap;
        if (inside(mouseX, mouseY, x, top, widths[1], BUTTON_HEIGHT)) {
            client.setScreen(new OptionsScreen(this, client.options));
            return true;
        }
        x += widths[1] + gap;
        if (inside(mouseX, mouseY, x, top, widths[2], BUTTON_HEIGHT)) {
            AzureClient.setClientHomeEnabled(false);
            return true;
        }

        if (inside(mouseX, mouseY, singleX, singleY - Math.round(singleHover * 4.0f),
            singleWidth, cardHeight)) {
            client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (inside(mouseX, mouseY, multiX, singleY - Math.round(multiHover * 4.0f),
            singleWidth, cardHeight)) {
            client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            AzureClient.setClientHomeEnabled(false);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int buttonWidth, int buttonHeight) {
        return mouseX >= x && mouseX < x + buttonWidth && mouseY >= y && mouseY < y + buttonHeight;
    }

    private static int blendColor(int from, int to, float amount) {
        float ratio = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 0xff) + (((to >>> 24) & 0xff) - ((from >>> 24) & 0xff)) * ratio);
        int r = Math.round(((from >>> 16) & 0xff) + (((to >>> 16) & 0xff) - ((from >>> 16) & 0xff)) * ratio);
        int g = Math.round(((from >>> 8) & 0xff) + (((to >>> 8) & 0xff) - ((from >>> 8) & 0xff)) * ratio);
        int b = Math.round((from & 0xff) + ((to & 0xff) - (from & 0xff)) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
