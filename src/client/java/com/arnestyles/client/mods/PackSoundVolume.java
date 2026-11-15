package com.arnestyles.client.mods;

import com.arnestyles.client.essentials.EssentialSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lautstaerke fuer Sounds, die aus einem eingebauten Style kommen (0 bis 200 %).
 * Ob ein Sound aus einem Style stammt, wird anhand des Packs entschieden, das die .ogg-Datei liefert.
 */
public final class PackSoundVolume {
    private static final Map<Identifier, Boolean> FROM_STYLE = new ConcurrentHashMap<>();

    private PackSoundVolume() {
    }

    /** Nach jedem Neuladen der Ressourcen neu pruefen. */
    public static void clearCache() {
        FROM_STYLE.clear();
    }

    /** Faktor fuer diesen Sound: der Regler, wenn der Sound aus einem Style kommt, sonst 1. */
    public static float factor(SoundInstance instance) {
        float volume = EssentialSlider.PACK_SOUND_VOLUME.effective();
        if (Math.abs(volume - 1.0f) < 0.001f || instance == null) {
            return 1.0f;
        }
        Sound sound = instance.getSound();
        if (sound == null) {
            return 1.0f;
        }
        Identifier location = sound.getLocation();
        boolean fromStyle = FROM_STYLE.computeIfAbsent(location, id -> MinecraftClient.getInstance().getResourceManager()
            .getResource(id).map(Resource::getPackId).map(pack -> pack.startsWith("arnestyles/")).orElse(false));
        return fromStyle ? volume : 1.0f;
    }
}
