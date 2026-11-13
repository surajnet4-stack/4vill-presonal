package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class AzureMenuScreen extends Screen {
    public AzureMenuScreen() {
        super(Text.literal("Azure Personal"));
    }

    @Override
    protected void init() {
        int buttonWidth = 220;
        int buttonX = (width - buttonWidth) / 2;
        int buttonY = height / 2;
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Change Azure key binding"),
            button -> AzureClient.openControls(MinecraftClient.getInstance(), this)
        ).dimensions(buttonX, buttonY, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            button -> close()
        ).dimensions(buttonX, buttonY + 28, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 48, 0xff49d8ce);
        context.drawCenteredTextWithShadow(
            textRenderer,
            "The menu shortcut can also be changed in Controls.",
            width / 2,
            height / 2 - 24,
            0xfff0f4fa
        );
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
