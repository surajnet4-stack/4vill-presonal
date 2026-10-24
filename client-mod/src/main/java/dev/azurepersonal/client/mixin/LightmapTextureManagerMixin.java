package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import dev.azurepersonal.client.AzureConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow @Final private NativeImage image;
    @Shadow @Final private NativeImageBackedTexture texture;

    @Inject(method = "update", at = @At("TAIL"))
    private void azure$applyFullbright(float delta, CallbackInfo callback) {
        AzureConfig config = AzureClient.config();
        if (!config.fullBright) return;
        int intensity = config.fullBrightLevel;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                int color = image.getColor(x, y);
                int red = brighten(color & 0xff, intensity);
                int green = brighten((color >>> 8) & 0xff, intensity);
                int blue = brighten((color >>> 16) & 0xff, intensity);
                image.setColor(x, y, (color & 0xff000000) | (blue << 16) | (green << 8) | red);
            }
        }
        texture.upload();
    }

    private static int brighten(int channel, int intensity) {
        return 255 - (255 - channel) * 100 / intensity;
    }
}
