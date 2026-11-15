package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.mods.WorldVisuals;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * - No Darkness: der Himmel wird beim Dunkelheits-Effekt weiter gezeichnet (bei Blindheit nicht).
 * - Block-Umrandung: eigene Farbe (auch Regenbogen) und Dicke.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @ModifyReturnValue(method = "hasBlindnessOrDarkness", at = @At("RETURN"), require = 0)
    private boolean arnestyles$noDarknessSky(boolean original, Camera camera) {
        if (!original || !Essential.NO_DARKNESS.isActive()) {
            return original;
        }
        return camera.getFocusedEntity() instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    @WrapOperation(
        method = "renderTargetBlockOutline",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V"),
        require = 0
    )
    private void arnestyles$blockOutline(WorldRenderer renderer, MatrixStack matrices, VertexConsumer consumer, double x, double y, double z,
                                         OutlineRenderState state, int color, float lineWidth, Operation<Void> original) {
        // Die schwarze Hinterlegung bei "Hoher Kontrast" bleibt, nur die eigentliche Linie wird umgefaerbt
        if (WorldVisuals.outlineActive() && color != 0xFF000000) {
            original.call(renderer, matrices, consumer, x, y, z, state, WorldVisuals.outlineColor(), WorldVisuals.outlineWidth(lineWidth));
            return;
        }
        original.call(renderer, matrices, consumer, x, y, z, state, color, lineWidth);
    }
}
