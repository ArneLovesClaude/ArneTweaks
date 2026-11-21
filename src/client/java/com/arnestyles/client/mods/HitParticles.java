package com.arnestyles.client.mods;

import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;

/**
 * Treffer-Partikel: bei jedem eigenen Schlag zusaetzlich Krit- und/oder Schaerfe-Partikel, mit einstellbarer Menge.
 * Nur Optik auf dem eigenen Bildschirm.
 */
public final class HitParticles {
    private HitParticles() {
    }

    public static void onAttack(Entity target) {
        boolean crits = ModSetting.Toggle.HIT_CRITS.isActive();
        boolean sharp = ModSetting.Toggle.HIT_SHARPNESS.isActive();
        if (!crits && !sharp) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int times = Math.max(1, Math.min(5, Math.round(ConfigManager.get().hitParticleAmount)));
        for (int i = 0; i < times; i++) {
            if (crits) {
                client.particleManager.addEmitter(target, ParticleTypes.CRIT);
            }
            if (sharp) {
                client.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }
}
