package dev.azurepersonal.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

public final class AzureCrownFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier TEXTURE = AzureCompat.id("azureclient", "textures/cosmetics/white.png");
    private static final int CROWN_COLOR = 0xff11151b;
    private static final int LOGO_COLOR = 0xff49d8ce;
    private final ModelPart crown;
    private final ModelPart logo;

    public AzureCrownFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
        crown = createCrown();
        logo = createLogo();
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (!AzureClient.config().azureCrownEquipped ||
            entity != MinecraftClient.getInstance().player || entity.isInvisible()) {
            return;
        }

        matrices.push();
        getContextModel().head.rotate(matrices);
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        crown.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, colorRed(CROWN_COLOR),
            colorGreen(CROWN_COLOR), colorBlue(CROWN_COLOR), 1.0f);
        logo.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV, colorRed(LOGO_COLOR),
            colorGreen(LOGO_COLOR), colorBlue(LOGO_COLOR), 1.0f);
        matrices.pop();
    }

    private static ModelPart createCrown() {
        ModelData data = new ModelData();
        ModelPartBuilder geometry = ModelPartBuilder.create()
            .uv(0, 0).cuboid(-4.5f, -9.0f, -4.5f, 9.0f, 2.0f, 9.0f)
            .uv(0, 0).cuboid(-4.5f, -12.0f, -4.5f, 2.0f, 3.0f, 2.5f)
            .uv(0, 0).cuboid(-1.0f, -13.0f, -4.5f, 2.0f, 4.0f, 2.5f)
            .uv(0, 0).cuboid(2.5f, -12.0f, -4.5f, 2.0f, 3.0f, 2.5f);
        data.getRoot().addChild("crown", geometry, ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32).createModel().getChild("crown");
    }

    private static ModelPart createLogo() {
        ModelData data = new ModelData();
        ModelPartBuilder geometry = ModelPartBuilder.create()
            .uv(0, 0).cuboid(-1.8f, -8.3f, -4.72f, 0.7f, 2.5f, 0.35f)
            .uv(0, 0).cuboid(1.1f, -8.3f, -4.72f, 0.7f, 2.5f, 0.35f)
            .uv(0, 0).cuboid(-1.1f, -7.0f, -4.76f, 2.2f, 0.55f, 0.4f);
        data.getRoot().addChild("azure_logo", geometry, ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32).createModel().getChild("azure_logo");
    }

    private static float colorRed(int color) {
        return ((color >> 16) & 0xff) / 255.0f;
    }

    private static float colorGreen(int color) {
        return ((color >> 8) & 0xff) / 255.0f;
    }

    private static float colorBlue(int color) {
        return (color & 0xff) / 255.0f;
    }
}
