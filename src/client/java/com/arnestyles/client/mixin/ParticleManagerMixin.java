package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** No Smoke: kein Rauch von Feuer, Fackeln, Lagerfeuern, Lava und brennenden Mobs. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    private static final Set<ParticleType<?>> ARNESTYLES$SMOKE = Set.of(
        ParticleTypes.SMOKE,
        ParticleTypes.LARGE_SMOKE,
        ParticleTypes.WHITE_SMOKE,
        ParticleTypes.CAMPFIRE_COSY_SMOKE,
        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
    );

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void arnestyles$noSmoke(ParticleEffect parameters, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (ARNESTYLES$SMOKE.contains(parameters.getType()) && Essential.NO_SMOKE.isActive()) {
            cir.setReturnValue(null);
        }
    }
}
