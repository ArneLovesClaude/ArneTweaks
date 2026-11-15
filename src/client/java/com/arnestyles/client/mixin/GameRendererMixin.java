package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.InventoryScale;
import com.arnestyles.client.mods.ArneShaders;
import com.arnestyles.client.mods.MotionBlur;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.memory.ObjectPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory Scale: das offene Container-Fenster mit dem eingestellten Faktor zeichnen.
 * Motion Blur: nach der Welt (mit Hand und Umrissen), vor HUD und Menues.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private ObjectPool pool;

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawEntityOutlinesFramebuffer()V", shift = At.Shift.AFTER),
        require = 0
    )
    private void arnestyles$motionBlur(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // Erst der ArneTweaks-Shader (Farben, Gluehen ...), dann der Motion Blur ueber das fertige Bild
        ArneShaders.apply(this.client, this.pool);
        MotionBlur.apply(this.client, this.pool);
    }

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"),
        require = 0
    )
    private void arnestyles$scaleScreen(Screen screen, DrawContext context, int mouseX, int mouseY, float delta, Operation<Void> original) {
        float factor = InventoryScale.factor(screen);
        if (factor == 1.0f) {
            original.call(screen, context, mouseX, mouseY, delta);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(factor, factor);
        InventoryScale.drawing = true;
        try {
            original.call(screen, context, mouseX, mouseY, delta);
        } finally {
            InventoryScale.drawing = false;
            context.getMatrices().popMatrix();
        }
    }
}
