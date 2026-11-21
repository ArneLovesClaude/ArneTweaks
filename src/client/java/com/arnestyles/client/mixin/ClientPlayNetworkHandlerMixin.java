package com.arnestyles.client.mixin;

import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.essentials.EssentialSlider;
import com.arnestyles.client.mods.PopChams;
import com.arnestyles.client.mods.ShieldStatus;
import com.arnestyles.client.mods.TotemCounter;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.collection.WeightedPool;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Partikel, die der Server ausloest:
 * - Regler Totem-Partikel: wie lange der Totem-Pop Partikel spawnt.
 * - No Explosion Particles: keine Explosions-Wolke und keine Block-Partikel bei Explosionen.
 * Sound, Schaden und Rueckstoss bleiben unveraendert.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /**
     * Regler Totem-Partikel: Vanilla spawnt 30 Ticks lang je 16 Partikel.
     * Die Dauer wird mit dem Regler skaliert, bei 0 % gibt es gar keine Partikel.
     */
    @WrapOperation(
        method = "onEntityStatus",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V")
    )
    private void arnestyles$totemParticles(ParticleManager manager, Entity entity, ParticleEffect parameters, int maxAge, Operation<Void> original) {
        // Mods: Totem Pop Chams (Silhouette an der Pop-Stelle)
        PopChams.onTotemPop(entity);
        // Mods: TotemCounter (Pops im Namensschild)
        TotemCounter.onTotemPop(entity);
        float amount = EssentialSlider.TOTEM_PARTICLES.effective();
        if (amount >= 0.999f) {
            original.call(manager, entity, parameters, maxAge);
            return;
        }
        int scaled = Math.round(maxAge * amount);
        if (scaled > 0) {
            original.call(manager, entity, parameters, scaled);
        }
    }

    /** PvP-Optik: eigener Totem-Pop-Sound und Lautstaerke. */
    @WrapOperation(
        method = "onEntityStatus",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"),
        require = 0
    )
    private void arnestyles$popSound(ClientWorld world, double x, double y, double z, net.minecraft.sound.SoundEvent sound,
                                     net.minecraft.sound.SoundCategory category, float volume, float pitch, boolean useDistance, Operation<Void> original) {
        if (sound == net.minecraft.sound.SoundEvents.ITEM_TOTEM_USE) {
            float scaled = volume * com.arnestyles.client.mods.PvpVisuals.popVolume();
            if (scaled > 0.0f) {
                original.call(world, x, y, z, com.arnestyles.client.mods.PvpVisuals.popSound(), category, scaled,
                    pitch * com.arnestyles.client.mods.TotemPop.pitchFactor(), useDistance);
            }
            return;
        }
        original.call(world, x, y, z, sound, category, volume, pitch, useDistance);
    }

    /** Shield Status: Schild-Brech-Geraeusch erkennen (am Ende, also schon im Haupt-Thread). */
    @Inject(method = "onPlaySound", at = @At("TAIL"), require = 0)
    private void arnestyles$shieldBreakSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        ShieldStatus.onSound(packet);
    }

    @WrapOperation(
        method = "onExplosion",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V")
    )
    private void arnestyles$explosionCloud(ClientWorld world, ParticleEffect parameters, double x, double y, double z, double vx, double vy, double vz, Operation<Void> original) {
        if (!Essential.NO_EXPLOSION_PARTICLES.isActive()) {
            original.call(world, parameters, x, y, z, vx, vy, vz);
        }
    }

    @WrapOperation(
        method = "onExplosion",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addBlockParticleEffects(Lnet/minecraft/util/math/Vec3d;FILnet/minecraft/util/collection/WeightedPool;)V")
    )
    private void arnestyles$explosionBlockParticles(ClientWorld world, Vec3d center, float radius, int blockCount, WeightedPool<BlockParticleEffect> particles, Operation<Void> original) {
        if (!Essential.NO_EXPLOSION_PARTICLES.isActive()) {
            original.call(world, center, radius, blockCount, particles);
        }
    }
}
