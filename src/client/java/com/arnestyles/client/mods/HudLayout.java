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
        // Vanilla-Teile zuerst: sie sind gross, eigene Anzeigen liegen darueber und gewinnen beim Anklicken
        V_TITLE("vanilla_title", true),
        V_SCOREBOARD("vanilla_scoreboard", true),
        V_BOSSBAR("vanilla_bossbar", true),
        V_EFFECTS("vanilla_effects", true),
        V_ACTIONBAR("vanilla_actionbar", true),
        V_ITEM_NAME("vanilla_item_name", true),
        V_XP("vanilla_xp", true),
        V_STATUS("vanilla_status", true),
        V_HOTBAR("vanilla_hotbar", true),
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
        TARGET("target_hud"),
        DEATH("death_point"),
        WAYPOINTS("waypoints"),
        SPOTIFY("spotify"),
        ITEM_COUNTER("item_counter"),
        CLOCK("clock"),
        ELYTRA("elytra"),
        SERVER("server"),
        DAY("day"),
        COOLDOWN("cooldown");

        private final String id;
        private final boolean vanilla;

        Element(String id) {
            this(id, false);
        }

        Element(String id, boolean vanilla) {
            this.id = id;
            this.vanilla = vanilla;
        }

        public String id() {
            return id;
        }

        /** Teil des normalen Minecraft-HUDs (Hotbar, Scoreboard ...), nicht eine eigene Anzeige. */
        public boolean vanilla() {
            return vanilla;
        }

        public String nameKey() {
            return "arnestyles.hud." + id;
        }
    }

    /** true, solange der HUD-Editor offen ist: Anzeigen zeigen dann Beispiel-Daten, auch ohne Ruestung oder Totems. */
    public static boolean editing;
    /** Im Editor auch die Minecraft-Teile (Hotbar, Scoreboard ...) zeigen und verschiebbar machen. */
    public static boolean showVanilla = true;

    /** Editor offen und Minecraft-Teile eingeblendet: dann zeigen sie Beispiele, auch wenn gerade nichts da ist. */
    public static boolean editingVanilla() {
        return editing && showVanilla;
    }

    /** Wo jede Anzeige zuletzt gezeichnet wurde (Bildschirm-Koordinaten x1, y1, x2, y2), fuer den Editor. */
    private static final Map<Element, int[]> LAST_BOUNDS = new EnumMap<>(Element.class);
    /** Ein Feld pro Anzeige, wird jedes Bild wiederverwendet (kein neues Array pro Bild). */
    private static final int[][] BOUNDS = new int[Element.values().length][4];

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
        begin(context, element, x, y, width, height, 0.0f, 0.0f);
    }

    /**
     * Wie {@link #begin(DrawContext, Element, int, int, int, int)}, aber die Groesse aendert sich um einen Ankerpunkt:
     * pivotX/pivotY 0 = links/oben, 0.5 = Mitte, 1 = rechts/unten (z. B. Hotbar waechst nach oben aus der Mitte).
     */
    public static void begin(DrawContext context, Element element, int x, int y, int width, int height, float pivotX, float pivotY) {
        ArneStylesConfig.HudPosition pos = position(element);
        float scale = pos.scale;
        float left = x + pos.x + width * (1.0f - scale) * pivotX;
        float top = y + pos.y + height * (1.0f - scale) * pivotY;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(left, top);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-x, -y);
        int[] b = BOUNDS[element.ordinal()];
        b[0] = Math.round(left);
        b[1] = Math.round(top);
        b[2] = Math.round(left + width * scale);
        b[3] = Math.round(top + height * scale);
        LAST_BOUNDS.put(element, b);
    }

    /** true, wenn die Anzeige an der normalen Stelle in normaler Groesse steht (dann muss nichts verschoben werden). */
    public static boolean untouched(Element element) {
        ArneStylesConfig.HudPosition pos = ConfigManager.get().hudPositions.get(element.id());
        return pos == null || (pos.x == 0.0f && pos.y == 0.0f && pos.scale == 1.0f);
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
