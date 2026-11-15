package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.ShieldStatus;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Shield Status: Griff und Platte des Schilds bekommen die Status-Farbe (gruen = bereit, rot = ausgeschaltet). */
@Mixin(ShieldModelRenderer.class)
public abstract class ShieldModelRendererMixin {
    @ModifyArg(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"),
        index = 8,
        require = 0
    )
    private int arnestyles$shieldStatusColor(int color, @Local(argsOnly = true) ItemDisplayContext context) {
        if (color != -1) {
            return color;
        }
        return ShieldStatus.tint(context);
    }
}
