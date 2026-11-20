package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.essentials.SettingSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.particle.ParticlesMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Performance-Reiter: Profile, Regler und Schalter fuer die Minecraft-Grafikoptionen, die am meisten FPS kosten.
 * Es werden die echten Minecraft-Optionen geaendert (wie unter Optionen > Grafik), damit nichts doppelt gespeichert
 * wird. In options.txt geschrieben wird beim Schliessen des Menues ({@link #flush()}).
 */
public final class PerformanceSettings {
    private static boolean changed;

    private PerformanceSettings() {
    }

    // ---- Schalter ----

    /** Ein Schalter im Performance-Reiter. "An" heisst immer: schneller. "Aus" stellt den Minecraft-Standard her. */
    public enum Toggle {
        CLOUDS_OFF("clouds_off"),
        PARTICLES_MINIMAL("particles_minimal"),
        NO_SHADOWS("no_shadows"),
        NO_SMOOTH_LIGHT("no_smooth_light"),
        FAST_LEAVES("fast_leaves"),
        LESS_EFFECTS("less_effects"),
        VSYNC_OFF("vsync_off"),
        NO_MIPMAPS("no_mipmaps"),
        NO_BIOME_BLEND("no_biome_blend"),
        NO_CHUNK_FADE("no_chunk_fade"),
        NO_MENU_BLUR("no_menu_blur"),
        LESS_WEATHER("less_weather");

        private final String id;

        Toggle(String id) {
            this.id = id;
        }

        public String nameKey() {
            return "arnestyles.performance." + id;
        }

        public String descriptionKey() {
            return "arnestyles.performance." + id + ".desc";
        }

        public boolean isOn() {
            GameOptions o = options();
            if (o == null) {
                return false;
            }
            return switch (this) {
                case CLOUDS_OFF -> o.getCloudRenderMode().getValue() == CloudRenderMode.OFF;
                case PARTICLES_MINIMAL -> o.getParticles().getValue() == ParticlesMode.MINIMAL;
                case NO_SHADOWS -> !o.getEntityShadows().getValue();
                case NO_SMOOTH_LIGHT -> !o.getAo().getValue();
                case FAST_LEAVES -> !o.getCutoutLeaves().getValue();
                case LESS_EFFECTS -> Essential.NO_SMOKE.isActive() && Essential.NO_EXPLOSION_PARTICLES.isActive();
                case VSYNC_OFF -> !o.getEnableVsync().getValue();
                case NO_MIPMAPS -> o.getMipmapLevels().getValue() == 0;
                case NO_BIOME_BLEND -> o.getBiomeBlendRadius().getValue() == 0;
                case NO_CHUNK_FADE -> o.getChunkFade().getValue() <= 0.0;
                case NO_MENU_BLUR -> o.getMenuBackgroundBlurriness().getValue() == 0;
                case LESS_WEATHER -> o.getWeatherRadius().getValue() <= 3;
            };
        }

        public void set(boolean on) {
            GameOptions o = options();
            if (o == null) {
                return;
            }
            switch (this) {
                case CLOUDS_OFF -> setOpt(o.getCloudRenderMode(), on ? CloudRenderMode.OFF : CloudRenderMode.FANCY);
                case PARTICLES_MINIMAL -> setOpt(o.getParticles(), on ? ParticlesMode.MINIMAL : ParticlesMode.ALL);
                case NO_SHADOWS -> setOpt(o.getEntityShadows(), !on);
                case NO_SMOOTH_LIGHT -> setOpt(o.getAo(), !on);
                case FAST_LEAVES -> setOpt(o.getCutoutLeaves(), !on);
                case LESS_EFFECTS -> setLessEffects(on);
                case VSYNC_OFF -> setOpt(o.getEnableVsync(), !on);
                case NO_MIPMAPS -> setOpt(o.getMipmapLevels(), on ? 0 : 4);
                case NO_BIOME_BLEND -> setOpt(o.getBiomeBlendRadius(), on ? 0 : 2);
                case NO_CHUNK_FADE -> setOpt(o.getChunkFade(), on ? 0.0 : 0.75);
                case NO_MENU_BLUR -> setOpt(o.getMenuBackgroundBlurriness(), on ? 0 : 5);
                case LESS_WEATHER -> setOpt(o.getWeatherRadius(), on ? 3 : 10);
            }
        }

        public void toggle() {
            set(!isOn());
        }
    }

    /** "Weniger Effekte" = die Essentials "Kein Rauch" und "Keine Explosionspartikel" zusammen. */
    private static void setLessEffects(boolean on) {
        ArneStylesConfig config = ConfigManager.get();
        config.setEssentialOn(Essential.NO_SMOKE.id(), on);
        config.setEssentialOn(Essential.NO_EXPLOSION_PARTICLES.id(), on);
        if (on) {
            // Essentials wirken nur, wenn ihre Kategorie aktiv ist.
            config.setEnabled(StyleCategory.ESSENTIALS, true);
        }
        ConfigManager.save();
    }

    // ---- Regler ----

    /** Regler mit festen Stufen fuer eine Minecraft-Option mit ganzen Zahlen. */
    private record IntSlider(String id, int min, int max, int step, int resetValue, String unit, int unlimitedAt,
                             java.util.function.Function<GameOptions, SimpleOption<Integer>> option) implements SettingSlider {
        @Override
        public String nameKey() {
            return "arnestyles.performance." + id;
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.performance." + id + ".desc";
        }

        private int value() {
            GameOptions o = options();
            return o == null ? resetValue : option.apply(o).getValue();
        }

        @Override
        public float fraction() {
            return (Math.max(min, Math.min(max, value())) - min) / (float) (max - min);
        }

        @Override
        public void setFraction(float fraction) {
            int steps = (max - min) / step;
            set(min + Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * steps) * step);
        }

        @Override
        public void reset() {
            set(resetValue);
        }

        void set(int value) {
            GameOptions o = options();
            if (o != null) {
                setOpt(option.apply(o), value);
            }
        }

        @Override
        public String display() {
            int v = value();
            if (unlimitedAt > 0 && v >= unlimitedAt) {
                return "∞";
            }
            return v + unit;
        }
    }

    /** Sichtweite in Chunks. 16 geht auf jedem PC (32 nur mit viel Speicher). */
    public static final SettingSlider VIEW_DISTANCE = new IntSlider("view_distance", 2, 16, 1, 12, " Ch", 0, GameOptions::getViewDistance);
    /** Wie weit um dich herum die Welt "lebt" (Mobs, Redstone, Pflanzen). Wirkt nur in Einzelspieler-Welten. */
    public static final SettingSlider SIMULATION_DISTANCE = new IntSlider("simulation_distance", 5, 16, 1, 12, " Ch", 0,
        GameOptions::getSimulationDistance);
    /** FPS-Limit. 260 = unbegrenzt. */
    public static final SettingSlider FPS_LIMIT = new IntSlider("fps_limit", 30, 260, 10, 120, "", 260, GameOptions::getMaxFps);

    /** Ab welcher Entfernung Spieler, Mobs und Items gezeichnet werden (50 % bis 200 %). */
    public static final SettingSlider ENTITY_DISTANCE = new SettingSlider() {
        private static final double MIN = 0.5;
        private static final double MAX = 2.0;

        @Override
        public String nameKey() {
            return "arnestyles.performance.entity_distance";
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.performance.entity_distance.desc";
        }

        private double value() {
            GameOptions o = options();
            return o == null ? 1.0 : o.getEntityDistanceScaling().getValue();
        }

        @Override
        public float fraction() {
            return (float) ((Math.max(MIN, Math.min(MAX, value())) - MIN) / (MAX - MIN));
        }

        @Override
        public void setFraction(float fraction) {
            // Minecraft erlaubt nur Viertel-Schritte
            double v = MIN + Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * (MAX - MIN) * 4.0) / 4.0;
            set(v);
        }

        @Override
        public void reset() {
            set(1.0);
        }

        private void set(double v) {
            GameOptions o = options();
            if (o != null) {
                setOpt(o.getEntityDistanceScaling(), v);
            }
        }

        @Override
        public String display() {
            return Math.round(value() * 100) + "%";
        }
    };

    public static List<SettingSlider> sliders() {
        return List.of(VIEW_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, FPS_LIMIT);
    }

    // ---- Profile ----

    /** Ein Klick stellt alles auf einmal um. */
    public enum Profile {
        /** So viele FPS wie moeglich, auch wenn es haesslich wird. */
        POTATO("potato"),
        /** Gute FPS, sieht aber noch normal aus. */
        BALANCED("balanced"),
        /** Minecraft-Standard in schoen (fuer starke PCs). */
        PRETTY("pretty");

        private final String id;

        Profile(String id) {
            this.id = id;
        }

        public String nameKey() {
            return "arnestyles.performance.profile." + id;
        }

        public String descriptionKey() {
            return "arnestyles.performance.profile." + id + ".desc";
        }

        public void apply() {
            GameOptions o = options();
            if (o == null) {
                return;
            }
            switch (this) {
                case POTATO -> {
                    for (Toggle toggle : Toggle.values()) {
                        toggle.set(true);
                    }
                    setOpt(o.getViewDistance(), 6);
                    setOpt(o.getSimulationDistance(), 5);
                    setOpt(o.getEntityDistanceScaling(), 0.5);
                    setOpt(o.getMaxFps(), 260);
                    setOpt(o.getImprovedTransparency(), false);
                }
                case BALANCED -> {
                    setOpt(o.getCloudRenderMode(), CloudRenderMode.FAST);
                    setOpt(o.getParticles(), ParticlesMode.DECREASED);
                    setOpt(o.getEntityShadows(), false);
                    setOpt(o.getAo(), true);
                    setOpt(o.getCutoutLeaves(), true);
                    setLessEffects(true);
                    setOpt(o.getEnableVsync(), false);
                    setOpt(o.getMipmapLevels(), 2);
                    setOpt(o.getBiomeBlendRadius(), 1);
                    setOpt(o.getChunkFade(), 0.25);
                    setOpt(o.getMenuBackgroundBlurriness(), 0);
                    setOpt(o.getWeatherRadius(), 6);
                    setOpt(o.getViewDistance(), 10);
                    setOpt(o.getSimulationDistance(), 8);
                    setOpt(o.getEntityDistanceScaling(), 1.0);
                    setOpt(o.getMaxFps(), 260);
                    setOpt(o.getImprovedTransparency(), false);
                }
                case PRETTY -> {
                    for (Toggle toggle : Toggle.values()) {
                        toggle.set(false);
                    }
                    setOpt(o.getViewDistance(), 16);
                    setOpt(o.getSimulationDistance(), 12);
                    setOpt(o.getEntityDistanceScaling(), 1.25);
                }
            }
        }

        /** true, wenn die wichtigsten Werte zu diesem Profil passen (Knopf wird dann markiert). */
        public boolean isActive() {
            GameOptions o = options();
            if (o == null) {
                return false;
            }
            return switch (this) {
                case POTATO -> java.util.Arrays.stream(Toggle.values()).allMatch(Toggle::isOn) && o.getViewDistance().getValue() <= 6;
                case BALANCED -> o.getCloudRenderMode().getValue() == CloudRenderMode.FAST && o.getParticles().getValue() == ParticlesMode.DECREASED
                    && o.getViewDistance().getValue() == 10 && o.getMipmapLevels().getValue() == 2;
                case PRETTY -> java.util.Arrays.stream(Toggle.values()).noneMatch(Toggle::isOn) && o.getViewDistance().getValue() >= 16;
            };
        }
    }

    // ---- Hilfen ----

    private static <T> void setOpt(SimpleOption<T> option, T value) {
        if (!Objects.equals(option.getValue(), value)) {
            option.setValue(value);
            changed = true;
        }
    }

    /** Geaenderte Minecraft-Optionen in options.txt schreiben (beim Schliessen des Menues). */
    public static void flush() {
        GameOptions options = options();
        if (changed && options != null) {
            changed = false;
            options.write();
        }
    }

    private static @Nullable GameOptions options() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.options;
    }

}
