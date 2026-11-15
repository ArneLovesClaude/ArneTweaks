package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.WorldVisuals;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item-Physik: gedroppte Items liegen flach auf dem Boden (Bloecke stehen darauf), ohne Drehen und Wippen.
 * Jedes Item bekommt eine feste, zufaellige Ausrichtung.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique
    private final Random arnestyles$random = Random.create();

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void arnestyles$itemPhysics(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera,
                                        CallbackInfo ci) {
        if (!WorldVisuals.itemPhysics() || state.itemRenderState.isEmpty()) {
            return;
        }
        ci.cancel();
        Box box = state.itemRenderState.getModelBoundingBox();
        float depth = (float) box.getLengthZ();
        matrices.push();
        if (depth <= 0.0625f) {
            // Flaches Item (Schwert, Perle ...): hinlegen
            float stack = depth * 1.5f * (state.renderedAmount - 1) / 2.0f;
            matrices.translate(0.0f, depth / 2.0f + stack + 0.01f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(state.uniqueOffset));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        } else {
            // Block-Modell: auf den Boden stellen
            matrices.translate(0.0f, (float) -box.minY, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(state.uniqueOffset));
        }
        ItemEntityRenderer.render(matrices, queue, state.light, state, arnestyles$random, box);
        matrices.pop();
    }
}
