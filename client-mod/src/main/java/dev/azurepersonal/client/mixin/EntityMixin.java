package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void azure$captureFreeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this != client.player || !AzureClient.isFreeLookActive()) return;
        float yaw = AzureClient.freeLookYaw() + (float) (cursorDeltaX * 0.15);
        float pitch = AzureClient.freeLookPitch() - (float) (cursorDeltaY * 0.15);
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
        AzureClient.setFreeLookRotation(yaw, pitch);
        callback.cancel();
    }
}
