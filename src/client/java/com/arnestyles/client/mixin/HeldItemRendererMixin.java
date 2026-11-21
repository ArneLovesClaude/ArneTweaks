package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.EssentialSlider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Regler Item-Groesse: Items in der Hand (Egoperspektive) groesser oder kleiner zeichnen.
 * Hand-Editor: zusaetzlich Position, Drehung und Groesse pro Item (linke Hand gespiegelt).
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private boolean arnestyles$pushed;

    private static boolean arnestyles$scaled(ItemDisplayContext mode) {
        return mode.isFirstPerson() && Math.abs(EssentialSlider.HELD_ITEM_SIZE.effective() - 1.0f) > 0.001f;
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At("HEAD")
    )
    private void arnestyles$scaleHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, MatrixStack matrices,
                                          OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        com.arnestyles.client.config.ArneStylesConfig.HandTransform t = mode.isFirstPerson()
            ? com.arnestyles.client.mods.HandTransforms.get(stack) : null;
        arnestyles$pushed = arnestyles$scaled(mode) || t != null;
        if (!arnestyles$pushed) {
            return;
        }
        matrices.push();
        if (t != null) {
            float side = mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0f : 1.0f;
            matrices.translate(t.x * side, t.y, t.z);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(t.rotX));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(t.rotY * side));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(t.rotZ * side));
            matrices.scale(t.scale, t.scale, t.scale);
        }
        if (arnestyles$scaled(mode)) {
            float scale = EssentialSlider.HELD_ITEM_SIZE.effective();
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At("RETURN")
    )
    private void arnestyles$restoreHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, MatrixStack matrices,
                                            OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (arnestyles$pushed) {
            arnestyles$pushed = false;
            matrices.pop();
        }
    }
}
