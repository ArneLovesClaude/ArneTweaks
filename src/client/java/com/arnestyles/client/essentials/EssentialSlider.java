package com.arnestyles.client.essentials;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Regler in der Kategorie Essentials. Werte liegen in der Config.
 * Ist die Kategorie Essentials aus, gilt jeweils der normale Minecraft-Wert.
 */
public enum EssentialSlider implements SettingSlider {
    TOTEM_POP_SIZE("totem_pop_size", 0.0f, 1.0f, 1.0f, c -> c.totemPopScale, (c, v) -> c.totemPopScale = v),
    TOTEM_PARTICLES("totem_particles", 0.0f, 1.0f, 1.0f, c -> c.totemParticles, (c, v) -> c.totemParticles = v),
    HELD_ITEM_SIZE("held_item_size", 0.3f, 1.5f, 1.0f, c -> c.heldItemScale, (c, v) -> c.heldItemScale = v),
    LOW_FIRE_DEPTH("low_fire_depth", 0.0f, 0.5f, 0.25f, c -> c.lowFireOffset, (c, v) -> c.lowFireOffset = v),
    PACK_SOUND_VOLUME("pack_sound_volume", 0.0f, 2.0f, 1.0f, c -> c.packSoundVolume, (c, v) -> c.packSoundVolume = v);

    private final String id;
    private final float min;
    private final float max;
    private final float vanilla;
    private final Function<ArneStylesConfig, Float> getter;
    private final BiConsumer<ArneStylesConfig, Float> setter;

    EssentialSlider(String id, float min, float max, float vanilla, Function<ArneStylesConfig, Float> getter, BiConsumer<ArneStylesConfig, Float> setter) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.vanilla = vanilla;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String nameKey() {
        return "arnestyles.slider." + id;
    }

    @Override
    public String descriptionKey() {
        return "arnestyles.slider." + id + ".desc";
    }

    /** Wert, wie er in der Config steht (unabhaengig davon, ob Essentials an ist). */
    public float value() {
        return getter.apply(ConfigManager.get());
    }

    /** Wert, der im Spiel gilt: bei ausgeschalteten Essentials der normale Minecraft-Wert. */
    public float effective() {
        ArneStylesConfig config = ConfigManager.get();
        if (this == PACK_SOUND_VOLUME) {
            // Gilt fuer alle Sounds aus eingebauten Packs, auch wenn die Kategorie Essentials aus ist.
            return config.modEnabled ? value() : vanilla;
        }
        return config.modEnabled && config.isEnabled(StyleCategory.ESSENTIALS) ? value() : vanilla;
    }

    /** Position des Reglers von 0 bis 1. */
    @Override
    public float fraction() {
        return (value() - min) / (max - min);
    }

    /** Setzt den Wert ueber die Reglerposition (0 bis 1), auf 5-%-Schritte gerundet. */
    @Override
    public void setFraction(float fraction) {
        float f = Math.max(0.0f, Math.min(1.0f, fraction));
        float raw = min + f * (max - min);
        float rounded = Math.round(raw * 20.0f) / 20.0f;
        setter.accept(ConfigManager.get(), Math.max(min, Math.min(max, rounded)));
    }

    /** Zurueck auf den normalen Minecraft-Wert (Low-Fire-Tiefe: Standardtiefe). */
    @Override
    public void reset() {
        setter.accept(ConfigManager.get(), vanilla);
    }

    /** Anzeige im Menue, z. B. "45%". Die Low-Fire-Tiefe wird in Prozent der maximalen Tiefe gezeigt. */
    @Override
    public String display() {
        float shown = this == LOW_FIRE_DEPTH ? value() / max : value();
        return String.format(Locale.ROOT, "%d%%", Math.round(shown * 100.0f));
    }
}
