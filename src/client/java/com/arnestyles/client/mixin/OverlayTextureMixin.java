package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.PvpVisuals;
import net.minecraft.client.render.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Merkt sich die Overlay-Textur, damit die Treffer-Farbe umgefaerbt werden kann. */
@Mixin(OverlayTexture.class)
public abstract class OverlayTextureMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void arnestyles$remember(CallbackInfo ci) {
        PvpVisuals.onOverlayCreated((OverlayTexture) (Object) this);
    }
}
