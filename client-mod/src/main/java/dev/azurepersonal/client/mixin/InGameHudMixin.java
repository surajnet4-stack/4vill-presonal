package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import dev.azurepersonal.client.AzureHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void azure$renderCrosshair(DrawContext context, CallbackInfo callback) {
        if (!AzureClient.config().customCrosshair) return;
        AzureHud.renderCrosshair(context, AzureClient.config());
        callback.cancel();
    }
}
