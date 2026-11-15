package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.WorldVisuals;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eigene Zeit: nur bei dir (Himmel, Sonne, Mond, Licht), der Server merkt davon nichts. */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
    private void arnestyles$customTime(CallbackInfoReturnable<Long> cir) {
        if (WorldVisuals.customTime()) {
            cir.setReturnValue(WorldVisuals.time());
        }
    }
}
