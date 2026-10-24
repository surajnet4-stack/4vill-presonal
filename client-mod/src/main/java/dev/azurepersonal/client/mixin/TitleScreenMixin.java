package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void azure$addClientHomeToggle(CallbackInfo callback) {
        if (AzureClient.config().useClientHome) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int x = screenWidth - 112;
        ((ScreenAccessor) (Object) this).azure$addDrawableChild(
            ButtonWidget.builder(Text.literal("CLIENT UI"), button ->
                AzureClient.setClientHomeEnabled(true)
            ).dimensions(x, 12, 98, 20).build()
        );
    }
}
