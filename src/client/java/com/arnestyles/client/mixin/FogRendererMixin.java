package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

/**
 * NoFog: kein Nebel unter Wasser, in Lava, in Pulverschnee und in der Ferne (Sichtweite, Himmel, Wolken).
 * Nebel von Blindheit und Dunkelheit bleibt, weil er zum Spiel gehoert (Dunkelheit schaltet No Darkness ab).
 * Fuer 1.21.11 neu geschrieben, nach der Idee von NoFog (Virtuoel, MIT).
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    /** Ob der Nebel in diesem Bild entfernt werden darf (false bei Blindheit/Dunkelheit). */
    @Unique
    private boolean arnestyles$clearFog;

    @WrapOperation(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogModifier;applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V"),
        require = 0
    )
    private void arnestyles$whichFog(FogModifier modifier, FogData data, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter counter,
                                     Operation<Void> original) {
        original.call(modifier, data, camera, world, viewDistance, counter);
        this.arnestyles$clearFog = !(modifier instanceof StatusEffectFogModifier);
    }

    @WrapOperation(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"),
        require = 0
    )
    private void arnestyles$noFog(FogRenderer renderer, ByteBuffer buffer, int bufPos, Vector4f fogColor, float environmentalStart, float environmentalEnd,
                                  float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudEnd, Operation<Void> original) {
        boolean clear = this.arnestyles$clearFog && Essential.NO_FOG.isActive();
        // Damit ein Bild ohne passenden Modifier nicht den alten Wert erbt
        this.arnestyles$clearFog = true;
        if (clear) {
            float far = Float.MAX_VALUE;
            original.call(renderer, buffer, bufPos, fogColor, far, far, far, far, far, far);
        } else {
            original.call(renderer, buffer, bufPos, fogColor, environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd);
        }
    }
}
