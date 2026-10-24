package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Invoker("setRotation")
    protected abstract void azure$setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void azure$applyFreeLook(net.minecraft.world.BlockView area, Entity focusedEntity,
                                     boolean thirdPerson, boolean inverseView, float tickDelta,
                                     CallbackInfo callback) {
        if (focusedEntity == net.minecraft.client.MinecraftClient.getInstance().player &&
            AzureClient.isFreeLookActive()) {
            azure$setRotation(AzureClient.freeLookYaw(), AzureClient.freeLookPitch());
        }
    }
}
