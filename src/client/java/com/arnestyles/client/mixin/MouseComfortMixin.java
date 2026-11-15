package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.Comfort;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom (Mausrad, langsamere Maus) und Freelook (Maus dreht nur die Kamera). */
@Mixin(Mouse.class)
public abstract class MouseComfortMixin {
    @Inject(
        method = "onMouseScroll",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Scroller;update(DD)Lorg/joml/Vector2i;"),
        cancellable = true,
        require = 0
    )
    private void arnestyles$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Comfort.onScroll(vertical)) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "updateMouse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
        require = 0
    )
    private void arnestyles$look(ClientPlayerEntity player, double dx, double dy, Operation<Void> original) {
        if (Comfort.freelook()) {
            Comfort.freelookTurn(dx, dy);
            return;
        }
        double factor = Comfort.mouseFactor();
        original.call(player, dx * factor, dy * factor);
    }
}
