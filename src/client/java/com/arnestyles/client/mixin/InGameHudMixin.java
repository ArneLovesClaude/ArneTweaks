package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.mods.AppleSkin;
import com.arnestyles.client.mods.EffectTimer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No Pumpkin Overlay: der Kuerbis-Rahmen beim Tragen eines geschnitzten Kuerbisses wird nicht gezeichnet.
 * Status Effect Timer: nach jedem Effekt-Symbol oben rechts Restzeit und Stufe darueber.
 * AppleSkin: Saettigung und Essens-Vorschau ueber den Keulen, geschaetzte Heilung ueber den Herzen.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private static final String ARNESTYLES$PUMPKIN_BLUR = "textures/misc/pumpkinblur.png";

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void arnestyles$noPumpkinOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (ARNESTYLES$PUMPKIN_BLUR.equals(texture.getPath()) && Essential.NO_PUMPKIN_OVERLAY.isActive()) {
            ci.cancel();
        }
    }

    /** Das Effekt-Symbol ist der einzige Aufruf mit Farbe; das Feld beginnt 3 Pixel weiter oben links. */
    @WrapOperation(
        method = "renderStatusEffectOverlay",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"),
        require = 0
    )
    private void arnestyles$effectTimer(DrawContext context, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color,
                                        Operation<Void> original, @Local StatusEffectInstance effect) {
        original.call(context, pipeline, sprite, x, y, width, height, color);
        EffectTimer.draw(context, effect, x - 3, y - 3);
    }

    @Inject(method = "renderFood", at = @At("RETURN"), require = 0)
    private void arnestyles$appleSkinFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        AppleSkin.renderFood(context, player, top, right);
    }

    @Inject(method = "renderHealthBar", at = @At("RETURN"), require = 0)
    private void arnestyles$appleSkinHealth(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex,
                                            float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        AppleSkin.renderHealth(context, player, x, y);
    }
}
