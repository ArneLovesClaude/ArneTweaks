package com.arnestyles.client.mixin;

import net.minecraft.client.sound.Source;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OpenAL begrenzt die Lautstaerke einer Quelle standardmaessig auf 100 %.
 * Fuer die Pack-Sound-Lautstaerke bis 200 % wird die Grenze bei Bedarf angehoben.
 */
@Mixin(Source.class)
public abstract class SourceMixin {
    private static final int ARNESTYLES$AL_MAX_GAIN = 0x100E;

    @Shadow
    @Final
    private int pointer;

    @Inject(method = "setVolume", at = @At("HEAD"))
    private void arnestyles$allowLouder(float volume, CallbackInfo ci) {
        if (volume > 1.0f) {
            AL10.alSourcef(this.pointer, ARNESTYLES$AL_MAX_GAIN, volume);
        }
    }
}
