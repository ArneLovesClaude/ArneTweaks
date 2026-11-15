package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;

import java.util.EnumMap;
import java.util.Map;

/**
 * Position und Groesse der eigenen HUD-Anzeigen (Ruestungsanzeige, Totem-Zaehler).
 * Jede Anzeige berechnet ihre normale Position selbst, hier kommen Verschiebung und Groesse dazu.
 * Im HUD-Editor werden die Anzeigen mit der Maus verschoben und mit dem Mausrad skaliert.
 */
public final class HudLayout {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;

    public enum Element {
        ARMOR("armor_hud"),
        TOTEMS("totem_counter"),
        KEYSTROKES("keystrokes"),
        CPS("cps"),
        FPS("fps"),
        PING("ping"),
        COORDS("coords"),
        COMBO("combo"),
        PEARL("pearl_cooldown"),
        ARMOR_WARNING("armor_warning"),
        TARGET("target_hud");

        private final String id;

        Element(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String nameKey() {
            return "arnestyles.hud." + id;
        }
    }

    /** true, solange der HUD-Editor offen ist: Anzeigen zeigen dann Beispiel-Daten, auch ohne Ruestung oder Totems. */
    public static boolean editing;

    /** Wo jede Anzeige zuletzt gezeichnet wurde (Bildschirm-Koordinaten x1, y1, x2, y2), fuer den Editor. */
    private static final Map<Element, int[]> LAST_BOUNDS = new EnumMap<>(Element.class);

    private HudLayout() {
    }

    public static ArneStylesConfig.HudPosition position(Element element) {
        return ConfigManager.get().hudPositions.computeIfAbsent(element.id(), key -> new ArneStylesConfig.HudPosition());
    }

    /**
     * Vor dem Zeichnen einer Anzeige: verschiebt und skaliert alles, was danach gezeichnet wird.
     * x, y, width, height ist die normale Position und Groesse der Anzeige. Danach immer {@link #end} aufrufen.
     */
    public static void begin(DrawContext context, Element element, int x, int y, int width, int height) {
        ArneStylesConfig.HudPosition pos = position(element);
        float scale = pos.scale;
        float left = x + pos.x;
        float top = y + pos.y;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(left, top);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-x, -y);
        LAST_BOUNDS.put(element, new int[] {Math.round(left), Math.round(top), Math.round(left + width * scale), Math.round(top + height * scale)});
    }

    public static void end(DrawContext context) {
        context.getMatrices().popMatrix();
    }

    /** Zuletzt gezeichnete Flaeche der Anzeige oder null, wenn sie gerade nicht gezeichnet wird. */
    public static int[] bounds(Element element) {
        return LAST_BOUNDS.get(element);
    }

    /** Vor jedem Bild im Editor leeren, damit ausgeschaltete Anzeigen keinen alten Rahmen behalten. */
    public static void clearBounds() {
        LAST_BOUNDS.clear();
    }

    public static void reset(Element element) {
        ConfigManager.get().hudPositions.put(element.id(), new ArneStylesConfig.HudPosition());
    }
}
