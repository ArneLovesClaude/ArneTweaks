package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fullbright: die Helligkeit (Gamma) wird beim Berechnen der Lichtkarte stark angehoben.
 * Die Helligkeit in den Optionen bleibt unveraendert.
 * No Darkness: das Pulsieren des Dunkelheits-Effekts wird abgeschaltet.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    private static final float ARNESTYLES$FULLBRIGHT_GAMMA = 15.0f;

    /** No Darkness: das erste floatValue() in update() ist die Staerke des Dunkelheits-Effekts (Warden, Sculk-Kreischer). */
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0), require = 0)
    private float arnestyles$noDarkness(float scale) {
        return Essential.NO_DARKNESS.isActive() ? 0.0f : scale;
    }

    /** Das zweite floatValue() in update() ist die Gamma-Option (das erste ist die Dunkelheits-Staerke). */
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float arnestyles$fullbright(float gamma) {
        return Essential.FULLBRIGHT.isActive() ? ARNESTYLES$FULLBRIGHT_GAMMA : gamma;
    }
}
