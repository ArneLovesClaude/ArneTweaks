package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.ModSetting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Eigene Effekt-Partikel ausblenden: Die Wirbel der eigenen Trank-Effekte steigen direkt vor der Kamera auf und
 * wirken in der Egoperspektive wie grosse bunte Flecken. Andere Spieler zeigen ihre Effekte weiter normal.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectParticlesMixin {
    @WrapOperation(method = "tickStatusEffects", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"), require = 0)
    private void arnestyles$hideOwnEffectParticles(World world, ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz,
                                                   Operation<Void> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this == client.player && client.options.getPerspective().isFirstPerson()
            && ModSetting.Toggle.HIDE_OWN_EFFECT_PARTICLES.isActive()) {
            return;
        }
        original.call(world, effect, x, y, z, vx, vy, vz);
    }
}
