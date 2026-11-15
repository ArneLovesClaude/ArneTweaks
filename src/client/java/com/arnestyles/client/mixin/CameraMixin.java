package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.Comfort;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Freelook: die Kamera schaut in die Freelook-Richtung, der Spieler selbst behaelt seine Blickrichtung. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0), require = 0)
    private void arnestyles$freelookCart(Camera camera, float yaw, float pitch, Operation<Void> original) {
        arnestyles$rotate(camera, yaw, pitch, original);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1), require = 0)
    private void arnestyles$freelook(Camera camera, float yaw, float pitch, Operation<Void> original) {
        arnestyles$rotate(camera, yaw, pitch, original);
    }

    private static void arnestyles$rotate(Camera camera, float yaw, float pitch, Operation<Void> original) {
        if (Comfort.freelook()) {
            original.call(camera, Comfort.freeYaw(), Comfort.freePitch());
        } else {
            original.call(camera, yaw, pitch);
        }
    }
}
