package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.essentials.EssentialSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays in der Egoperspektive. Rein visuell, an Schaden und Spielmechanik aendert sich nichts.
 * - Low Fire: Feuer vor den Augen tiefer und kleiner.
 * - NoFog: kein Unterwasser-Schleier.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    /** Bei voller Tiefe (0.5) wird das Feuer auf diesen Anteil verkleinert. */
    private static final float ARNESTYLES$MIN_FIRE_SCALE = 0.6f;

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void arnestyles$lowerFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        matrices.push();
        if (Essential.LOW_FIRE.isActive()) {
            float offset = ConfigManager.get().lowFireOffset;
            float scale = 1.0f - (1.0f - ARNESTYLES$MIN_FIRE_SCALE) * (offset / 0.5f);
            matrices.translate(0.0f, -offset, 0.0f);
            matrices.scale(scale, scale, 1.0f);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void arnestyles$restoreFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        matrices.pop();
    }

    /** Regler Totem-Pop-Groesse bei 0 %: die Animation wird gar nicht gezeichnet. */
    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    private void arnestyles$hideTotemPop(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        if (EssentialSlider.TOTEM_POP_SIZE.effective() <= 0.001f) {
            ci.cancel();
        }
    }

    /** Regler Totem-Pop-Groesse: das Totem, das beim Pop ueber den Bildschirm fliegt, wird skaliert. */
    @Inject(
        method = "renderFloatingItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", shift = At.Shift.AFTER)
    )
    private void arnestyles$scaleTotemPop(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        float scale = EssentialSlider.TOTEM_POP_SIZE.effective();
        if (scale < 0.999f) {
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void arnestyles$noWaterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Essential.NO_FOG.isActive()) {
            ci.cancel();
        }
    }
}
