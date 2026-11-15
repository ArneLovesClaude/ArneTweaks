package com.arnestyles.client.gui;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;

import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Aufbau des ArneTweaks-Menues (unabhaengig von den Farben): Ecken, Kopfzeile, Seitenleiste, Markierung,
 * Knoepfe, Schalter, Mods-Ansicht und Durchsichtigkeit. Vorlagen stellen Farben und Aufbau mit einem Klick ein.
 * Alles auf 0 ist der normale ArneTweaks-Look.
 */
public final class GuiLayout {
    public enum Option {
        /** 0 = leicht rund, 1 = eckig, 2 = sehr rund. */
        CORNERS("corners", 3, c -> c.guiCorners, (c, v) -> c.guiCorners = v),
        /** 0 = Verlauf, 1 = flach. */
        HEADER("header", 2, c -> c.guiHeader, (c, v) -> c.guiHeader = v),
        /** 0 = mit Namen, 1 = nur Symbole (schmal). */
        SIDEBAR("sidebar", 2, c -> c.guiSidebar, (c, v) -> c.guiSidebar = v),
        /** Markierung des aktiven Eintrags: 0 = Flaeche, 1 = Leiste links, 2 = Umrandung. */
        ACTIVE("active", 3, c -> c.guiActive, (c, v) -> c.guiActive = v),
        /** 0 = flach, 1 = Minecraft (3D-Kante). */
        BUTTONS("buttons", 2, c -> c.guiButtons, (c, v) -> c.guiButtons = v),
        /** 0 = Schalter, 1 = AN/AUS als Text, 2 = Kaestchen. */
        SWITCHES("switches", 3, c -> c.guiSwitches, (c, v) -> c.guiSwitches = v),
        /** 0 = Oberkategorien zum Aufklappen, 1 = Reiter, 2 = Kacheln. */
        MODS_VIEW("mods_view", 3, c -> c.guiModsView, (c, v) -> c.guiModsView = v),
        /** 0 = aus, 1 = Fenster durchsichtig. */
        GLASS("glass", 2, c -> c.guiGlass, (c, v) -> c.guiGlass = v);

        public final String id;
        public final int count;
        private final ToIntFunction<ArneStylesConfig> getter;
        private final ObjIntConsumer<ArneStylesConfig> setter;

        Option(String id, int count, ToIntFunction<ArneStylesConfig> getter, ObjIntConsumer<ArneStylesConfig> setter) {
            this.id = id;
            this.count = count;
            this.getter = getter;
            this.setter = setter;
        }

        public int value() {
            int v = getter.applyAsInt(ConfigManager.get());
            return v >= 0 && v < count ? v : 0;
        }

        /** Naechster (delta = 1) oder vorheriger (delta = -1) Wert, im Kreis. */
        public void cycle(int delta) {
            setter.accept(ConfigManager.get(), ((value() + delta) % count + count) % count);
            ConfigManager.save();
        }

        public String nameKey() {
            return "arnestyles.layout." + id;
        }

        public String valueKey() {
            return "arnestyles.layout." + id + "." + value();
        }

        public String descriptionKey() {
            return "arnestyles.layout." + id + ".desc";
        }

        void set(ArneStylesConfig config, int value) {
            setter.accept(config, value >= 0 && value < count ? value : 0);
        }

        int get(ArneStylesConfig config) {
            return getter.applyAsInt(config);
        }
    }

    /** Vorlage: Farb-Design plus Werte aller Optionen (Reihenfolge wie {@link Option}). */
    public record Template(String id, String theme, int... values) {
        public String nameKey() {
            return "arnestyles.layout.template." + id;
        }

        public String descriptionKey() {
            return "arnestyles.layout.template." + id + ".desc";
        }
    }

    //                          Ecken Kopf Leiste Markierung Knoepfe Schalter Mods Glas
    public static final List<Template> TEMPLATES = List.of(
        new Template("standard", GuiTheme.STANDARD, 0, 0, 0, 0, 0, 0, 0, 0),
        new Template("glass", "glass", 2, 1, 0, 1, 0, 0, 0, 1),
        new Template("compact", "compact", 2, 1, 1, 0, 0, 0, 1, 0),
        new Template("tiles", GuiTheme.STANDARD, 2, 0, 0, 0, 0, 0, 2, 0),
        new Template("ocean", "ocean", 0, 1, 0, 0, 0, 0, 1, 0),
        new Template("fire", "fire", 1, 1, 0, 1, 0, 1, 0, 0),
        new Template("mint", "mint", 2, 1, 0, 2, 0, 0, 2, 0),
        new Template("gold", "gold", 1, 0, 0, 2, 0, 2, 1, 0),
        new Template("sakura", "sakura", 2, 0, 0, 0, 0, 0, 2, 1),
        new Template("minecraft", "minecraft", 1, 1, 0, 2, 1, 1, 2, 0)
    );

    private GuiLayout() {
    }

    /** Stellt Farben und Aufbau der Vorlage ein. */
    public static void apply(Template template) {
        ArneStylesConfig config = ConfigManager.get();
        Option[] options = Option.values();
        for (int i = 0; i < options.length; i++) {
            options[i].set(config, i < template.values().length ? template.values()[i] : 0);
        }
        GuiTheme.select(template.theme());
    }

    public static void apply(String id) {
        TEMPLATES.stream().filter(t -> t.id().equals(id)).findFirst().ifPresent(GuiLayout::apply);
    }

    /** Ist genau diese Vorlage eingestellt (Farben und Aufbau)? */
    public static boolean matches(Template template) {
        if (!template.theme().equals(ConfigManager.get().guiTheme)) {
            return false;
        }
        Option[] options = Option.values();
        for (int i = 0; i < options.length; i++) {
            if (options[i].get(ConfigManager.get()) != template.values()[i]) {
                return false;
            }
        }
        return true;
    }

    /** Ungueltige Werte (z. B. von Hand in der Config) auf 0. */
    public static void sanitize(ArneStylesConfig config) {
        for (Option option : Option.values()) {
            int v = option.get(config);
            if (v < 0 || v >= option.count) {
                option.set(config, 0);
            }
        }
    }
}
