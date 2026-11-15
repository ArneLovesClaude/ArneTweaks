package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.PackSoundVolume;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pack-Sound-Lautstaerke: Sounds aus eingebauten Styles lauter oder leiser machen (0 bis 200 %). */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    /** Beim Abspielen. */
    @ModifyExpressionValue(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundSystem;getAdjustedVolume(FLnet/minecraft/sound/SoundCategory;)F")
    )
    private float arnestyles$packVolumeOnPlay(float volume, @Local(argsOnly = true) SoundInstance sound) {
        return volume * PackSoundVolume.factor(sound);
    }

    /** Bei laufenden Sounds, deren Lautstaerke jeden Tick neu berechnet wird. */
    @ModifyReturnValue(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"))
    private float arnestyles$packVolumeOnTick(float volume, SoundInstance sound) {
        return volume * PackSoundVolume.factor(sound);
    }
}
