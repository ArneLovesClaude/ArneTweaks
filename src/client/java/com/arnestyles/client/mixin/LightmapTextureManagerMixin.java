package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fullbright: die Lichtkarte wird nach dem Berechnen komplett weiss gemacht, also ist jede Lichtstufe voll hell
 * und farbneutral. Frueher wurde die Helligkeit auf 1500 % gesetzt; das ueberdrehte die Lichtkarte, und Dinge mit
 * wenig Licht (z. B. fallende Blaetter unter Baumkronen) wurden grell orange.
 * Die Helligkeit in den Optionen bleibt unveraendert.
 * No Darkness: das Pulsieren des Dunkelheits-Effekts wird abgeschaltet.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    @Final
    private GpuTexture glTexture;

    /** No Darkness: das erste floatValue() in update() ist die Staerke des Dunkelheits-Effekts (Warden, Sculk-Kreischer). */
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0), require = 0)
    private float arnestyles$noDarkness(float scale) {
        return Essential.NO_DARKNESS.isActive() ? 0.0f : scale;
    }

    /** Fullbright: nach jeder Berechnung die ganze Lichtkarte mit Weiss fuellen. */
    @Inject(method = "update", at = @At("RETURN"))
    private void arnestyles$fullbright(float tickProgress, CallbackInfo ci) {
        if (Essential.FULLBRIGHT.isActive()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(glTexture, 0xFFFFFFFF);
        }
    }
}
