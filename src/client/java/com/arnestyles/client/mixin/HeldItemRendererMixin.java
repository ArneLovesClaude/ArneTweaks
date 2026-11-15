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

/** Regler Item-Groesse: Items in der Hand (Egoperspektive) groesser oder kleiner zeichnen. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    private static boolean arnestyles$scaled(ItemDisplayContext mode) {
        return mode.isFirstPerson() && Math.abs(EssentialSlider.HELD_ITEM_SIZE.effective() - 1.0f) > 0.001f;
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At("HEAD")
    )
    private void arnestyles$scaleHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, MatrixStack matrices,
                                          OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (arnestyles$scaled(mode)) {
            float scale = EssentialSlider.HELD_ITEM_SIZE.effective();
            matrices.push();
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At("RETURN")
    )
    private void arnestyles$restoreHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext mode, MatrixStack matrices,
                                            OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (arnestyles$scaled(mode)) {
            matrices.pop();
        }
    }
}
