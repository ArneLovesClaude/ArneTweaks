package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Darkness: der dunkle Nebel des Dunkelheits-Effekts (Warden) faellt weg. Blindheit bleibt normal. */
@Mixin(StatusEffectFogModifier.class)
public abstract class StatusEffectFogModifierMixin {
    @Shadow
    public abstract RegistryEntry<StatusEffect> getStatusEffect();

    @Inject(method = "shouldApply", at = @At("HEAD"), cancellable = true, require = 0)
    private void arnestyles$noDarknessFog(@Nullable CameraSubmersionType submersionType, Entity cameraEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Essential.NO_DARKNESS.isActive() && getStatusEffect() == StatusEffects.DARKNESS) {
            cir.setReturnValue(false);
        }
    }
}
