package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void azure$countAttack(CallbackInfoReturnable<Boolean> callback) {
        AzureClient.recordAttack();
    }
}
