package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.Nametags;
import net.minecraft.client.render.command.LabelCommandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Nametags: Text-Schatten auf Wunsch (Vanilla zeichnet Namensschilder ohne Schatten). */
@Mixin(LabelCommandRenderer.class)
public abstract class NametagShadowMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"),
        index = 4)
    private boolean arnestyles$shadow(boolean original) {
        return original || Nametags.shadow();
    }
}
