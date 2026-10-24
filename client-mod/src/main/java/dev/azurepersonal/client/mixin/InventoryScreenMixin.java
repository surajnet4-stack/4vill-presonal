package dev.azurepersonal.client.mixin;

import dev.azurepersonal.client.AzureClient;
import dev.azurepersonal.client.AzureInventorySorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Accessor("recipeBook")
    protected abstract RecipeBookWidget azure$getRecipeBook();

    @Inject(method = "init", at = @At("TAIL"))
    private void azure$addInventorySortButton(CallbackInfo callback) {
        if (AzureClient.consumeRecipeBookRequest()) azure$getRecipeBook().toggleOpen();
        MinecraftClient client = MinecraftClient.getInstance();
        int buttonX = client.getWindow().getScaledWidth() / 2 + 94;
        int buttonY = client.getWindow().getScaledHeight() / 2 - 82;
        ((ScreenAccessor) (Object) this).azure$addDrawableChild(ButtonWidget.builder(
            net.minecraft.text.Text.literal("SORT"),
            button -> AzureInventorySorter.sort(MinecraftClient.getInstance())
        ).dimensions(buttonX, buttonY, 54, 18).build());
    }
}
