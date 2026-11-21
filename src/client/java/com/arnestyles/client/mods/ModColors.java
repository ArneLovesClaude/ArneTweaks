package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/** Farb-Zeilen der Mods-Seite (neuere Farben), Schluessel -> Config-Feld. Name: arnestyles.mod.color.&lt;key&gt;. */
public final class ModColors {
    public record Entry(String key, ToIntFunction<ArneStylesConfig> getter, ObjIntConsumer<ArneStylesConfig> setter) {
        public String nameKey() {
            return "arnestyles.mod.color." + key;
        }
    }

    private static final Map<String, Entry> MAP = new LinkedHashMap<>();

    static {
        add("spotify_accent", c -> c.spotifyAccentRgb, (c, v) -> c.spotifyAccentRgb = v);
        add("item_counter_warn_color", c -> c.itemCounterWarnRgb, (c, v) -> c.itemCounterWarnRgb = v);
        add("crosshair_color", c -> c.crosshairRgb, (c, v) -> c.crosshairRgb = v);
        add("crosshair_outline_color", c -> c.crosshairOutlineRgb, (c, v) -> c.crosshairOutlineRgb = v);
        add("hud_text_color", c -> c.hudTextRgb, (c, v) -> c.hudTextRgb = v);
        add("hud_bg_color", c -> c.hudBgRgb, (c, v) -> c.hudBgRgb = v);
        add("hud_border_color", c -> c.hudBorderRgb, (c, v) -> c.hudBorderRgb = v);
        add("hud_accent_color", c -> c.hudAccentRgb, (c, v) -> c.hudAccentRgb = v);
        add("chat_time_color", c -> c.chatTimeRgb, (c, v) -> c.chatTimeRgb = v);
        add("hotbar_select_color", c -> c.hotbarSelectRgb, (c, v) -> c.hotbarSelectRgb = v);
        add("cooldown_color", c -> c.cooldownRgb, (c, v) -> c.cooldownRgb = v);
    }

    private ModColors() {
    }

    private static void add(String key, ToIntFunction<ArneStylesConfig> getter, ObjIntConsumer<ArneStylesConfig> setter) {
        MAP.put(key, new Entry(key, getter, setter));
    }

    public static @Nullable Entry get(String key) {
        return MAP.get(key);
    }
}
