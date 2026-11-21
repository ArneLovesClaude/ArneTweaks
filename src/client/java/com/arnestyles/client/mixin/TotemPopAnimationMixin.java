package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.TotemPop;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem-Pop-Animation: ohne Drehung, ohne Flug (Mitte) und eigenes Tempo. */
@Mixin(InGameOverlayRenderer.class)
public abstract class TotemPopAnimationMixin {
    @Shadow
    private @Nullable ItemStack floatingItem;
    @Shadow
    private int floatingItemTimer;

    /** Ohne Flug: nur die Tiefe (Heranzoomen) bleibt, seitlich und hoch/runter bleibt das Totem in der Mitte. */
    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0), require = 0)
    private void arnestyles$center(MatrixStack matrices, float x, float y, float z, Operation<Void> original) {
        if (TotemPop.centered()) {
            original.call(matrices, 0.0f, 0.0f, z);
        } else {
            original.call(matrices, x, y, z);
        }
    }

    /** Ohne Drehung: die drei Drehungen werden weggelassen. */
    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"), require = 0)
    private void arnestyles$noSpin(MatrixStack matrices, Quaternionfc rotation, Operation<Void> original) {
        if (!TotemPop.noSpin()) {
            original.call(matrices, rotation);
        }
    }

    /** Tempo: der Zaehler laeuft schneller oder langsamer als ein Schritt pro Tick. */
    @Inject(method = "tickFloatingItemTimer", at = @At("HEAD"), cancellable = true)
    private void arnestyles$speed(CallbackInfo ci) {
        if (floatingItem == null || floatingItemTimer <= 0) {
            return;
        }
        int steps = TotemPop.ticksThisTick();
        floatingItemTimer = Math.max(0, floatingItemTimer - steps);
        if (floatingItemTimer == 0) {
            floatingItem = null;
        }
        ci.cancel();
    }
}
