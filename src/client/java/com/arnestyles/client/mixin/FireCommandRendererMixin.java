package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.PvpVisuals;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.FireCommandRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feuer auf brennenden Spielern tiefer zeichnen, damit man den Spieler besser sieht. */
@Mixin(FireCommandRenderer.class)
public abstract class FireCommandRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/texture/AtlasManager;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void arnestyles$lowPlayerFire(MatrixStack.Entry entry, VertexConsumerProvider consumers, EntityRenderState state, Quaternionf rotation,
                                          AtlasManager atlas, CallbackInfo ci) {
        float offset = PvpVisuals.playerFireOffset();
        if (offset > 0.0f && state.entityType == EntityType.PLAYER) {
            entry.translate(0.0f, -state.height * offset, 0.0f);
        }
    }
}
