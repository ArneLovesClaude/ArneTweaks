package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.Nametags;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.command.LabelCommandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Nametags: Groesse und Hintergrund-Deckkraft der Namensschilder (beim Einreihen der Schilder). */
@Mixin(LabelCommandRenderer.Commands.class)
public abstract class NametagMixin {
    @ModifyConstant(method = "add", constant = @Constant(floatValue = 0.025F))
    private float arnestyles$scale(float original) {
        return original * Nametags.scale();
    }

    @ModifyConstant(method = "add", constant = @Constant(floatValue = -0.025F))
    private float arnestyles$scaleY(float original) {
        return original * Nametags.scale();
    }

    @ModifyExpressionValue(method = "add", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/GameOptions;getTextBackgroundOpacity(F)F"))
    private float arnestyles$background(float original) {
        return Nametags.backgroundOpacity(original);
    }
}
