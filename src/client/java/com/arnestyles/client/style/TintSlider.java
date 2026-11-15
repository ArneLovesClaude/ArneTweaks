package com.arnestyles.client.style;

import com.arnestyles.client.config.ArneStylesConfig.CategoryTint;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.SettingSlider;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Regler fuer eine eigene Farbe: Farbton, Saettigung oder Helligkeit.
 * textureKey null = Farbe der ganzen Kategorie, sonst Farbe einer einzelnen Textur (Customize-Tab).
 */
public record TintSlider(StyleCategory category, Part part, @Nullable String textureKey) implements SettingSlider {
    public enum Part {
        HUE,
        SATURATION,
        BRIGHTNESS
    }

    public TintSlider(StyleCategory category, Part part) {
        this(category, part, null);
    }

    /** Die Farbe, die dieser Regler veraendert (zum Zeichnen des Verlaufs). */
    public CategoryTint current() {
        return tint();
    }

    private CategoryTint tint() {
        if (textureKey == null) {
            return TintedTextures.get(category);
        }
        return ConfigManager.get().textureTints.computeIfAbsent(textureKey, key -> new CategoryTint());
    }

    @Override
    public String nameKey() {
        return "arnestyles.tint." + part.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    @Override
    public float fraction() {
        CategoryTint tint = tint();
        return switch (part) {
            case HUE -> tint.hue;
            case SATURATION -> tint.saturation;
            case BRIGHTNESS -> tint.brightness;
        };
    }

    @Override
    public void setFraction(float fraction) {
        float value = Math.max(0.0f, Math.min(1.0f, fraction));
        CategoryTint tint = tint();
        switch (part) {
            case HUE -> tint.hue = value;
            case SATURATION -> tint.saturation = value;
            case BRIGHTNESS -> tint.brightness = value;
        }
        if (textureKey != null) {
            // Wer an der Farbe einer Textur dreht, will sie auch sehen
            tint.on = true;
        }
    }

    @Override
    public void reset() {
        CategoryTint tint = tint();
        boolean shift = tint.mode == TintedTextures.MODE_SHIFT;
        setFraction(switch (part) {
            case HUE -> shift ? 0.0f : new CategoryTint().hue;
            case SATURATION -> shift ? 0.5f : new CategoryTint().saturation;
            case BRIGHTNESS -> 0.5f;
        });
    }

    @Override
    public String display() {
        boolean shift = tint().mode == TintedTextures.MODE_SHIFT;
        if (part == Part.HUE) {
            return Math.round(fraction() * 360.0f) + "°";
        }
        if (shift) {
            // Beim Farbton-Drehen: 100 % = unveraendert
            return Math.round(fraction() * 200.0f) + "%";
        }
        return Math.round(fraction() * 100.0f) + "%";
    }
}
