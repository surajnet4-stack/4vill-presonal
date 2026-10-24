package dev.azurepersonal.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public final class AzureHomeScreen extends Screen {
    private static final int MUTED = 0xffa3a8b7;
    private static final int BUTTON_HEIGHT = 39;
    private static final int BUTTON_GAP = 8;
    private float entrance = 0.0f;

    public AzureHomeScreen() {
        super(Text.literal("Azure Personal"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xff0b0d15);
        context.fill(0, 0, width, height, 0xc90b0d15);
        entrance = Math.min(1.0f, entrance + delta * 0.08f);

        renderBrand(context, entrance);
        renderTopBar(context, mouseX, mouseY);
        renderMainButtons(context, mouseX, mouseY, entrance);

        String version = "AZURE PERSONAL  |  FABRIC 1.20.1";
        context.drawCenteredTextWithShadow(textRenderer, version, width / 2, height - 17, MUTED);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBrand(DrawContext context, float progress) {
        int accent = AzureClient.config().accentColor();
        int centerX = width / 2;
        int brandY = Math.max(42, height / 2 - 145) + Math.round((1.0f - progress) * 12.0f);
        context.fill(centerX - 100, brandY - 3, centerX - 68, brandY + 29, 0xff12272c);
        context.fill(centerX - 100, brandY - 3, centerX - 97, brandY + 29, accent);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("A"), centerX - 84, brandY + 6, accent);
        context.drawTextWithShadow(textRenderer, Text.literal("AZURE"), centerX - 59, brandY - 1, 0xfff5f7ff);
        context.drawTextWithShadow(textRenderer, Text.literal("PERSONAL CLIENT"), centerX - 58, brandY + 15, MUTED);
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY) {
        int accent = AzureClient.config().accentColor();
        int top = 14;
        int start = Math.max(8, width - 318);
        int nameWidth = 112;
        int buttonX = start + nameWidth + 7;
        String playerName = MinecraftClientName.current();

        context.fill(start, top, start + nameWidth, top + 27, 0xb51b1e2a);
        context.drawTextWithShadow(textRenderer, Text.literal(">"), start + 9, top + 9, accent);
        context.drawTextWithShadow(textRenderer, Text.literal(playerName), start + 24, top + 9, 0xfff5f7ff);
        drawTopButton(context, "OPT", buttonX, top, 36, mouseX, mouseY);
        drawTopButton(context, "HUD", buttonX + 42, top, 36, mouseX, mouseY);
        drawTopButton(context, "DEFAULT UI", buttonX + 84, top, 104, mouseX, mouseY);
    }

    private void renderMainButtons(DrawContext context, int mouseX, int mouseY, float progress) {
        int buttonWidth = Math.min(330, width - 40);
        int buttonX = (width - buttonWidth) / 2;
        int baseY = height / 2 - 32 + Math.round((1.0f - progress) * 12.0f);
        String[] labels = { "SINGLEPLAYER", "MULTIPLAYER", "CLIENT SETTINGS", "OPTIONS" };
        for (int index = 0; index < labels.length; index++) {
            drawMainButton(
                context,
                labels[index],
                buttonX,
                baseY + index * (BUTTON_HEIGHT + BUTTON_GAP),
                buttonWidth,
                BUTTON_HEIGHT,
                mouseX,
                mouseY
            );
        }
        int quitY = baseY + labels.length * (BUTTON_HEIGHT + BUTTON_GAP) + 8;
        drawMainButton(context, "QUIT GAME", buttonX, quitY, buttonWidth, 31, mouseX, mouseY);
    }

    private void drawTopButton(DrawContext context, String label, int x, int y, int buttonWidth, int mouseX, int mouseY) {
        int accent = AzureClient.config().accentColor();
        boolean hovered = inside(mouseX, mouseY, x, y, buttonWidth, 27);
        int background = hovered ? 0xff26343c : 0xb51b1e2a;
        context.fill(x, y, x + buttonWidth, y + 27, background);
        if (hovered) context.fill(x, y + 25, x + buttonWidth, y + 27, accent);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + buttonWidth / 2, y + 9,
            hovered ? accent : 0xfff5f7ff);
    }

    private void drawMainButton(
        DrawContext context,
        String label,
        int x,
        int y,
        int buttonWidth,
        int buttonHeight,
        int mouseX,
        int mouseY
    ) {
        int accent = AzureClient.config().accentColor();
        boolean hovered = inside(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
        int background = hovered ? 0xff202a34 : 0xc9161923;
        int border = hovered ? accent : 0xff343844;
        context.fill(x, y, x + buttonWidth, y + buttonHeight, background);
        context.fill(x, y, x + buttonWidth, y + 1, border);
        context.fill(x, y + buttonHeight - 1, x + buttonWidth, y + buttonHeight, border);
        context.fill(x, y, x + 1, y + buttonHeight, border);
        context.fill(x + buttonWidth - 1, y, x + buttonWidth, y + buttonHeight, border);
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal(label),
            x + buttonWidth / 2,
            y + (buttonHeight - textRenderer.fontHeight) / 2,
            hovered ? accent : 0xfff5f7ff
        );
        if (hovered) {
            context.fill(x + 13, y + 10, x + 15, y + buttonHeight - 10, accent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int start = Math.max(8, width - 318);
        int top = 14;
        int buttonX = start + 119;
        if (inside(mouseX, mouseY, buttonX, top, 36, 27)) {
            client.setScreen(new OptionsScreen(this, client.options));
            return true;
        }
        if (inside(mouseX, mouseY, buttonX + 42, top, 36, 27)) {
            client.setScreen(new AzureMenuScreen(this));
            return true;
        }
        if (inside(mouseX, mouseY, buttonX + 84, top, 104, 27)) {
            AzureClient.setClientHomeEnabled(false);
            return true;
        }

        int buttonWidth = Math.min(330, width - 40);
        int mainX = (width - buttonWidth) / 2;
        int baseY = height / 2 - 32;
        for (int index = 0; index < 5; index++) {
            int buttonY = index < 4
                ? baseY + index * (BUTTON_HEIGHT + BUTTON_GAP)
                : baseY + 4 * (BUTTON_HEIGHT + BUTTON_GAP) + 8;
            int buttonHeight = index == 4 ? 31 : BUTTON_HEIGHT;
            if (!inside(mouseX, mouseY, mainX, buttonY, buttonWidth, buttonHeight)) continue;

            switch (index) {
                case 0 -> client.setScreen(new SelectWorldScreen(this));
                case 1 -> client.setScreen(new MultiplayerScreen(this));
                case 2 -> client.setScreen(new AzureMenuScreen(this));
                case 3 -> client.setScreen(new OptionsScreen(this, client.options));
                case 4 -> client.scheduleStop();
                default -> { }
            }
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int buttonWidth, int buttonHeight) {
        return mouseX >= x && mouseX < x + buttonWidth && mouseY >= y && mouseY < y + buttonHeight;
    }

    private static final class MinecraftClientName {
        private static String current() {
            String username = net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername();
            return username.length() > 13 ? username.substring(0, 12) + "..." : username;
        }
    }
}
