package com.arnestyles.client.mods;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.essentials.SettingSlider;
import net.minecraft.client.sound.SoundInstance;

import java.util.List;
import java.util.Locale;

/**
 * Sound-Mixer: eigene Lautstaerke (0 bis 200 %) fuer typische Geraeusche, unabhaengig von den Minecraft-Kategorien.
 * Zugeordnet wird ueber die Sound-ID (z. B. entity.player.attack.crit gehoert zu "Treffer").
 */
public final class SoundMixer {
    private SoundMixer() {
    }

    public enum Group implements SettingSlider {
        HITS("hits", List.of("entity.player.attack.", "entity.player.hurt")),
        CRYSTAL("crystal", List.of("entity.generic.explode", "entity.end_crystal.")),
        ANCHOR("anchor", List.of("block.respawn_anchor.")),
        PEARL("pearl", List.of("entity.ender_pearl.", "entity.player.teleport")),
        EAT("eat", List.of("entity.generic.eat", "entity.generic.drink", "entity.player.burp", "item.honey_bottle.drink")),
        ARMOR("armor", List.of("item.armor.equip_", "entity.item.break")),
        SHIELD("shield", List.of("item.shield.")),
        MACE("mace", List.of("item.mace.")),
        BOW("bow", List.of("entity.arrow.", "item.crossbow.", "entity.player.attack.sweep")),
        XP("xp", List.of("entity.experience_orb.pickup", "entity.player.levelup", "entity.experience_bottle.")),
        STEPS("steps", List.of(".step")),
        WEATHER("weather", List.of("weather.", "entity.lightning_bolt."));

        private final String id;
        private final List<String> patterns;

        Group(String id, List<String> patterns) {
            this.id = id;
            this.patterns = patterns;
        }

        boolean matches(String path) {
            for (String p : patterns) {
                if (p.startsWith(".") ? path.endsWith(p) : path.startsWith(p)) {
                    return true;
                }
            }
            return false;
        }

        public float volume() {
            Float v = ConfigManager.get().soundVolumes.get(id);
            return v == null ? 1.0f : v;
        }

        @Override
        public String nameKey() {
            return "arnestyles.sound_mixer." + id;
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.sound_mixer." + id + ".desc";
        }

        @Override
        public float fraction() {
            return volume() / 2.0f;
        }

        @Override
        public void setFraction(float fraction) {
            float v = Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * 40.0f) / 20.0f;
            if (Math.abs(v - 1.0f) < 0.001f) {
                ConfigManager.get().soundVolumes.remove(id);
            } else {
                ConfigManager.get().soundVolumes.put(id, v);
            }
        }

        @Override
        public void reset() {
            ConfigManager.get().soundVolumes.remove(id);
        }

        @Override
        public String display() {
            return Math.round(volume() * 100) + "%";
        }
    }

    /** Faktor fuer diesen Sound (1, wenn keine Gruppe passt oder ArneTweaks aus ist). */
    public static float factor(SoundInstance instance) {
        if (instance == null || !ConfigManager.get().modEnabled || ConfigManager.get().soundVolumes.isEmpty()) {
            return 1.0f;
        }
        String path = instance.getId().getPath().toLowerCase(Locale.ROOT);
        for (Group group : Group.values()) {
            if (group.matches(path)) {
                return group.volume();
            }
        }
        return 1.0f;
    }
}
