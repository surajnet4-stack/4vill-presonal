package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import dev.azurepersonal.client.AzureConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void azure$drawColoredOutline(MatrixStack matrices, VertexConsumer vertices, Entity entity,
                                          double cameraX, double cameraY, double cameraZ, BlockPos pos,
                                          BlockState state, CallbackInfo callback) {
        AzureConfig config = AzureClient.config();
        if (!config.blockOutline) return;
        VoxelShape shape = state.getOutlineShape(entity.getWorld(), pos);
        if (!shape.isEmpty()) {
            Box bounds = shape.getBoundingBox().expand(0.002).offset(pos)
                .offset(-cameraX, -cameraY, -cameraZ);
            int color = config.blockOutlineColor;
            WorldRenderer.drawBox(matrices, vertices, bounds,
                ((color >> 16) & 0xff) / 255.0f,
                ((color >> 8) & 0xff) / 255.0f,
                (color & 0xff) / 255.0f,
                ((color >>> 24) & 0xff) / 255.0f);
        }
        callback.cancel();
    }
}
