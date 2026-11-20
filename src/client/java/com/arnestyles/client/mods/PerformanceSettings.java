package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.essentials.SettingSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.particle.ParticlesMode;

/**
 * Performance-Reiter: Schalter fuer die Minecraft-Grafikoptionen, die am meisten FPS kosten, und ein Knopf,
 * der alles auf einmal auf "schnell" stellt. Es werden die echten Minecraft-Optionen geaendert (wie unter
 * Optionen > Grafik), damit nichts doppelt gespeichert wird. Geschrieben wird beim Schliessen des Menues.
 */
public final class PerformanceSettings {
    /** Sichtweite, auf die "Max FPS" hoechstens stellt. */
    private static final int FAST_VIEW_DISTANCE = 8;
    private static final int FAST_SIMULATION_DISTANCE = 6;
    private static final int MIN_VIEW = 2;
    /** 16 geht auf jedem PC (32 nur mit viel Speicher), mehr braucht ein Performance-Regler nicht. */
    private static final int MAX_VIEW = 16;

    private static boolean changed;

    private PerformanceSettings() {
    }

    /** Ein Schalter im Performance-Reiter. "An" heisst immer: schneller. */
    public enum Toggle {
        CLOUDS_OFF("clouds_off"),
        PARTICLES_MINIMAL("particles_minimal"),
        NO_SHADOWS("no_shadows"),
        NO_SMOOTH_LIGHT("no_smooth_light"),
        FAST_LEAVES("fast_leaves"),
        LESS_EFFECTS("less_effects");

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
            GameOptions options = options();
            if (options == null) {
                return false;
            }
            return switch (this) {
                case CLOUDS_OFF -> options.getCloudRenderMode().getValue() == CloudRenderMode.OFF;
                case PARTICLES_MINIMAL -> options.getParticles().getValue() == ParticlesMode.MINIMAL;
                case NO_SHADOWS -> !options.getEntityShadows().getValue();
                case NO_SMOOTH_LIGHT -> !options.getAo().getValue();
                case FAST_LEAVES -> !options.getCutoutLeaves().getValue();
                case LESS_EFFECTS -> lessEffectsOn();
            };
        }

        public void set(boolean on) {
            GameOptions options = options();
            if (options == null) {
                return;
            }
            switch (this) {
                case CLOUDS_OFF -> options.getCloudRenderMode().setValue(on ? CloudRenderMode.OFF : CloudRenderMode.FANCY);
                case PARTICLES_MINIMAL -> options.getParticles().setValue(on ? ParticlesMode.MINIMAL : ParticlesMode.ALL);
                case NO_SHADOWS -> options.getEntityShadows().setValue(!on);
                case NO_SMOOTH_LIGHT -> options.getAo().setValue(!on);
                case FAST_LEAVES -> options.getCutoutLeaves().setValue(!on);
                case LESS_EFFECTS -> setLessEffects(on);
            }
            changed = true;
        }

        public void toggle() {
            set(!isOn());
        }
    }

    /** "Weniger Effekte" = die Essentials "Kein Rauch" und "Keine Explosionspartikel" zusammen. */
    private static boolean lessEffectsOn() {
        return Essential.NO_SMOKE.isActive() && Essential.NO_EXPLOSION_PARTICLES.isActive();
    }

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

    /** true, wenn alle Schalter an sind und die Sichtweite schon niedrig ist. */
    public static boolean isMaxFps() {
        GameOptions options = options();
        if (options == null) {
            return false;
        }
        for (Toggle toggle : Toggle.values()) {
            if (!toggle.isOn()) {
                return false;
            }
        }
        return options.getViewDistance().getValue() <= FAST_VIEW_DISTANCE;
    }

    /** Alles auf schnell: alle Schalter an, Sichtweite und Simulationsdistanz runter, Biom-Uebergang aus. */
    public static void applyMaxFps() {
        GameOptions options = options();
        if (options == null) {
            return;
        }
        for (Toggle toggle : Toggle.values()) {
            if (!toggle.isOn()) {
                toggle.set(true);
            }
        }
        if (options.getViewDistance().getValue() > FAST_VIEW_DISTANCE) {
            options.getViewDistance().setValue(FAST_VIEW_DISTANCE);
        }
        if (options.getSimulationDistance().getValue() > FAST_SIMULATION_DISTANCE) {
            options.getSimulationDistance().setValue(FAST_SIMULATION_DISTANCE);
        }
        if (options.getBiomeBlendRadius().getValue() > 0) {
            options.getBiomeBlendRadius().setValue(0);
        }
        if (options.getImprovedTransparency().getValue()) {
            options.getImprovedTransparency().setValue(false);
        }
        changed = true;
    }

    /** Regler fuer die Sichtweite (2 bis 16 Chunks). */
    public static final SettingSlider VIEW_DISTANCE = new SettingSlider() {
        @Override
        public String nameKey() {
            return "arnestyles.performance.view_distance";
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.performance.view_distance.desc";
        }

        @Override
        public float fraction() {
            GameOptions options = options();
            int value = options == null ? 12 : options.getViewDistance().getValue();
            return (Math.min(MAX_VIEW, value) - MIN_VIEW) / (float) (MAX_VIEW - MIN_VIEW);
        }

        @Override
        public void setFraction(float fraction) {
            set(MIN_VIEW + Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * (MAX_VIEW - MIN_VIEW)));
        }

        @Override
        public void reset() {
            set(12);
        }

        private void set(int chunks) {
            GameOptions options = options();
            if (options != null && options.getViewDistance().getValue() != chunks) {
                options.getViewDistance().setValue(chunks);
                changed = true;
            }
        }

        @Override
        public String display() {
            GameOptions options = options();
            return (options == null ? 12 : options.getViewDistance().getValue()) + " Ch";
        }
    };

    /** Geaenderte Minecraft-Optionen in options.txt schreiben (beim Schliessen des Menues). */
    public static void flush() {
        GameOptions options = options();
        if (changed && options != null) {
            changed = false;
            options.write();
        }
    }

    private static GameOptions options() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.options;
    }
}
