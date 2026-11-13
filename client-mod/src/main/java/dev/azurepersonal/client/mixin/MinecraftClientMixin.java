package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public HitResult crosshairTarget;

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void azure$countAttack(CallbackInfoReturnable<Boolean> callback) {
        AzureClient.recordAttack();
        if (crosshairTarget instanceof EntityHitResult entityHit) {
            MinecraftClient client = (MinecraftClient) (Object) this;
            if (client.player != null && entityHit.getEntity() != null) {
                double dist = client.player.getEyePos().distanceTo(entityHit.getPos());
                AzureClient.recordEntityHit(dist);
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void azure$countUse(CallbackInfo callback) {
        AzureClient.recordUse();
    }
}
