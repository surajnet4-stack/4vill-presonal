package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void azure$applyZoom(Camera camera, float tickDelta, boolean changingFov,
                                 CallbackInfoReturnable<Double> callback) {
        double factor = AzureClient.zoomFactor();
        if (factor > 1.001) callback.setReturnValue(callback.getReturnValue() / factor);
    }
}
