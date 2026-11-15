package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.ShieldStatus;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield Status: merkt sich, welcher Spieler gerade sein Hand-Item gezeichnet bekommt. */
@Mixin(PlayerHeldItemFeatureRenderer.class)
public abstract class PlayerHeldItemFeatureRendererMixin {
    private static final String ARNESTYLES$RENDER_ITEM =
        "renderItem(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

    @Inject(method = ARNESTYLES$RENDER_ITEM, at = @At("HEAD"), require = 0)
    private void arnestyles$holderStart(PlayerEntityRenderState state, ItemRenderState item, ItemStack stack, Arm arm, MatrixStack matrices,
                                        OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        ShieldStatus.setHolder(state.id);
    }

    @Inject(method = ARNESTYLES$RENDER_ITEM, at = @At("RETURN"), require = 0)
    private void arnestyles$holderEnd(PlayerEntityRenderState state, ItemRenderState item, ItemStack stack, Arm arm, MatrixStack matrices,
                                      OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        ShieldStatus.clearHolder();
    }
}
