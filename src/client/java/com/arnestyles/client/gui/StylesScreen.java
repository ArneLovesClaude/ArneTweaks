package com.arnestyles.client.gui;

import net.minecraft.util.Identifier;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.CategoryGroup;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.PresetManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.essentials.EssentialSlider;
import com.arnestyles.client.essentials.SettingSlider;
import com.arnestyles.client.mods.PerformanceSettings;
import com.arnestyles.client.mods.ArneShaders;
import com.arnestyles.client.mods.ModSetting;
import com.arnestyles.client.mods.PvpVisuals;
import com.arnestyles.client.mods.WorldVisuals;
import com.arnestyles.client.mods.ShaderPacks;
import com.arnestyles.client.style.CloudStyles;
import com.arnestyles.client.style.PreviewCrystalRenderState;
import com.arnestyles.client.style.PackImporter;
import com.arnestyles.client.style.PreviewTextures;
import com.arnestyles.client.style.StyleDefinition;
import com.arnestyles.client.style.StyleRegistry;
import com.arnestyles.client.style.TintSlider;
import com.arnestyles.client.style.TintedTextures;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.EntityType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Styles-Menue: oben Titel und Hauptschalter, links die Kategorien mit Suche,
 * rechts die Styles als Karten mit Live-Vorschau (bzw. Regler und Schalter bei Essentials).
 * Aenderungen an Texturen werden beim Schliessen des Menues geladen.
 */
public class StylesScreen extends Screen {
    // Farben (dunkles Design mit violettem Akzent)
    private static int C_DIM = 0xC0080A10;
    private static int C_PANEL = 0xF5121420;
    private static int C_EDGE = 0xFF2A2E45;
    private static int C_HEADER_TOP = 0xFF1D2036;
    private static int C_HEADER_BOTTOM = 0xFF141727;
    private static int C_SIDEBAR = 0xFF0E1019;
    private static int C_FIELD = 0xFF0B0D15;
    private static int C_CARD = 0xFF1A1D2B;
    private static int C_CARD_HOVER = 0xFF242842;
    private static int C_ACCENT = 0xFF8B5CF6;
    /** Zweiter Akzent (Violett im Arne-Tweaks-Design): Texturen, Auswahl, kreative Bereiche. */
    private static int C_ACCENT2 = 0xFFA66AFF;
    private static int C_ACCENT_SOFT = 0x408B5CF6;
    private static int C_TEXT = 0xFFEDEFF7;
    private static int C_MUTED = 0xFF8A90A8;
    private static final int C_MOD_BLOCKED = 0xFFE07A5F;
    private static int C_OK = 0xFF4ADE80;
    private static int C_WARN = 0xFFFBBF24;
    private static int C_DANGER = 0xFFF87171;
    private static int C_OFF = 0xFF3A3F58;
    /** Rand unter der Maus und etwas gedaempfter Text (aus dem Design gemischt). */
    private static int C_EDGE_HOVER = 0xFF3C4266;
    private static int C_TEXT_SOFT = 0xFFC9CDE0;

    private static final int HEADER_H = 50;
    /** Spielmodus-Leiste in der Kopfzeile (Abstand von oben und Hoehe). */
    private static final int TAB_Y = 29;
    private static final int TAB_H = 18;
    private static final String KEY_OTHER_TAB = "T:OTHER";
    private static final String KEY_CUSTOMIZE_TAB = "T:CUSTOMIZE";
    private static final int FOOTER_H = 20;
    private static final int LIST_WIDTH_FULL = 128;
    private static final int LIST_WIDTH_ICONS = 32;
    /** Breite der Seitenleiste (schmal, wenn nur Symbole). */
    private int LIST_WIDTH = LIST_WIDTH_FULL;
    private static final int ROW_HEIGHT = 18;
    private static final int CARD_W = 84;
    private static final int PREVIEW = 68;
    private static final int CARD_H = PREVIEW + 30;
    private static final int CARD_GAP = 6;
    private static final int ESSENTIAL_ROW_H = 20;
    /** Zeilen der FPS-Einstellungen: Profile, Regler-Zeilen, dann je zwei Schalter. */
    private static final int PERFORMANCE_LINES = 1 + (PerformanceSettings.sliders().size() + 1) / 2
        + (PerformanceSettings.Toggle.values().length + 1) / 2;
    private static final int MAX_SEARCH_LENGTH = 16;
    private static final long INPUT_GRACE_MS = 150L;
    private static final float FIRE_STEP = 0.05f;
    /** Gras-Oberseite ist in Minecraft grau und wird im Spiel eingefaerbt. Fuer die Vorschau: Wiesengruen. */
    private static final int GRASS_TINT = 0xFF79C05A;

    /** Klickbare Flaeche, wird bei jedem Zeichnen neu gesammelt. */
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    /** Klickbarer Regler, wird bei jedem Zeichnen neu gesammelt. */
    private record SliderHit(int x1, int y1, int x2, int y2, int barX1, int barX2, SettingSlider slider) {
    }

    /** Eine Zeile in der Essentials-Liste: eingebaut oder eigener Ordner aus styles/essentials. */
    private record EssentialRow(Text name, Text description, String key, boolean needsReload,
                                String iconCacheKey, @Nullable Path iconRoot, @Nullable String iconAsset, @Nullable Path iconImage) {
    }

    private final StyleCategory[] allCategories = StyleCategory.values();
    private final StringBuilder search = new StringBuilder();
    private final long openedAt = System.currentTimeMillis();
    private final List<Hit> hits = new ArrayList<>();
    private final List<SliderHit> sliderHits = new ArrayList<>();
    private final Map<String, String> previewAssetCache = new HashMap<>();
    private final Map<String, Integer> soundCountCache = new HashMap<>();
    private final Map<String, Boolean> styleHasFileCache = new HashMap<>();
    private @Nullable SliderHit draggingSlider;

    private StyleCategory current;
    /** Zuletzt angeklickte Zeile der Seitenleiste (eine Kategorie kann in mehreren Ordnern stehen). */
    private int selectedRow = -1;
    /** true, wenn rechts die Presets-Seite offen ist. */
    private boolean presetsOpen;
    /** true, wenn rechts die Mods-Seite offen ist. */
    private boolean modsOpen;
    /** true, wenn rechts die Shader-Seite offen ist. */
    private boolean shadersOpen;
    /** true, wenn rechts die Design-Seite (Farben und Groesse des Menues) offen ist. */
    private boolean themesOpen;
    /** true, wenn rechts die Pack-Import-Seite offen ist. */
    private boolean importOpen;
    /** Customize-Tab: Kategorie, Style (Index) und Textur, die gerade bearbeitet werden. */
    private @Nullable StyleCategory customizeCategory;
    private int customizeStyle = -1;
    private @Nullable String customizeAsset;
    private int customizeTotalRows;
    private int customizeVisibleRows = 1;
    /** Farbkreis: Position, Durchmesser und welche Textur-Farbe er gerade aendert. */
    private int wheelX;
    private int wheelY;
    private int wheelD;
    /** Was ein Klick in den Farbkreis aendert (Farbton, Saettigung). null = kein Farbkreis zu sehen. */
    private java.util.function.@Nullable BiConsumer<Float, Float> wheelApply;
    private boolean wheelDragging;
    private static @Nullable Identifier wheelTexture;
    /** Name des Tabs unter der Maus, wenn nur sein Symbol zu sehen ist (wird als kleines Schild gezeigt). */
    private @Nullable Text hoveredTab;
    private int hoveredTabX;
    /** "Nur in einer Welt"-Hinweis fuer den HUD-Editor auf der Mods-Seite. */
    private boolean hudEditorHint;
    /** Sortier-Modus der Seitenleiste: Eintraege mit Pfeilen verschieben. */
    private boolean sortMode;
    /** Bildschirm, zu dem beim Schliessen zurueckgekehrt wird (Mod Menu, Optionen). */
    private final @Nullable Screen parent;
    /** Pro Kategorie/Style die Vorschau-Textur, die sonst kein Style der Kategorie zeigt ("" = nur Modelle). */
    private final Map<String, String> uniquePreviewCache = new HashMap<>();
    private final StringBuilder presetName = new StringBuilder();
    private @Nullable String presetMessage;
    private int presetMessageColor;
    private @Nullable String deleteArmed;
    private int listScroll;
    private int cardScroll;
    private boolean resetArmed;
    /** true, wenn sich Texturen geaendert haben und beim Schliessen neu geladen werden muss. */
    private boolean dirty;
    /** Ein Unterfenster (Zeichen-Editor) ist offen: beim Wechsel dorthin nicht neu laden. */
    private boolean childOpen;
    /** Hinweis, warum sich eine Textur nicht zeichnen laesst (nur kurz im Info-Text). */
    private @Nullable Text drawError;
    private long drawErrorAt;
    private int cloudGeneration = CloudStyles.generation();
    /** Erklaerung der Farb-Zeile unter der Maus (wird oben in der Info-Zeile gezeigt). */
    private @Nullable Text tintHover;
    private @Nullable Text shownTintHover;

    private int left;
    private int top;
    private int right;
    private int bottom;
    private int listTop;
    private int rowsVisible;
    private int paneX;
    private int paneRight;
    private int cardsTop;
    private int cardsBottom;
    private int columns;
    private int cardRows;
    /** Kartenbreite (fuellt die Zeile). */
    private int cardW = CARD_W;
    /** Linke Kante der Inspector-Spalte (= right, wenn sie aus ist). */
    private int inspectorX;
    /** Reiter im Inspector: Live-Vorschau oder Vorlagen. */
    private boolean inspectorThemes;
    /** Was der Inspector zeigt: Karte unter der Maus bzw. zuletzt gezeigter Mod. */
    private @Nullable StyleDefinition hoveredStyle;
    private ModSetting.@Nullable Toggle inspectToggle;
    private @Nullable SettingSlider inspectSlider;
    /** Aenderungen seit dem letzten Uebernehmen (fuer den Apply-Knopf). */
    private int pendingChanges;
    /** Herkunft eigener Styles (aus style.json), damit nicht jedes Bild neu gelesen wird. */
    private final Map<String, String> originCache = new HashMap<>();
    private static final net.minecraft.item.ItemStack TOTEM_STACK = new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING);
    private static final net.minecraft.util.math.random.Random PREVIEW_RANDOM = net.minecraft.util.math.random.Random.create();
    private static @Nullable String versionText;

    public StylesScreen() {
        this(null);
    }

    /** Mit Eltern-Bildschirm (z. B. Mod Menu oder Optionen): Schliessen kehrt dorthin zurueck. */
    public StylesScreen(@Nullable Screen parent) {
        super(Text.translatable("arnestyles.gui.title"));
        this.parent = parent;
        this.current = parseCategory(ConfigManager.get().activeCategory);
        revealCurrent();
        CloudStyles.request(current);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    protected void init() {
        super.init();
        // Erst hier ist die Fenstergroesse bekannt: Seitenleiste zur aktuellen Kategorie scrollen.
        ensureCategoryVisible();
    }

    private StyleCategory parseCategory(String name) {
        for (StyleCategory category : allCategories) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return StyleCategory.CRYSTAL;
    }

    private static Text categoryName(StyleCategory category) {
        return Text.translatable("arnestyles.category." + category.folderName());
    }

    private void computeLayout() {
        updateScale();
        LIST_WIDTH = iconSidebar ? LIST_WIDTH_ICONS : LIST_WIDTH_FULL;
        int panelW = Math.min(vw - 16, bigLayout ? 1000 : 600);
        int panelH = Math.min(vh - 16, bigLayout ? 560 : 362);
        this.left = (vw - panelW) / 2;
        this.top = (vh - panelH) / 2;
        this.right = left + panelW;
        this.bottom = top + panelH;
        this.listTop = top + HEADER_H + (iconSidebar ? 6 : 26);
        this.rowsVisible = Math.max(1, (bottom - FOOTER_H - 4 - listTop) / ROW_HEIGHT);
        this.paneX = left + LIST_WIDTH + 12;
        int inspectorW = !inspectorOn ? 0 : panelW >= 760 ? 172 : panelW >= 520 ? 132 : 0;
        this.inspectorX = right - inspectorW;
        this.paneRight = right - 10 - inspectorW;
        boolean stylePage = !presetsOpen && !modsOpen && !shadersOpen && !themesOpen && !importOpen;
        int extraRows = !stylePage || KEY_CUSTOMIZE_TAB.equals(ConfigManager.get().activeTab) ? 0 : current == StyleCategory.SOUNDS ? 22 : current == StyleCategory.ORES ? 66
            : TintedTextures.supports(current) ? 44 : 0;
        this.cardsTop = top + HEADER_H + 44 + extraRows;
        this.cardsBottom = bottom - FOOTER_H - 4;
        this.columns = Math.max(1, (paneRight - paneX + CARD_GAP) / (CARD_W + CARD_GAP));
        // Karten so breit machen, dass sie die Zeile fuellen (hoechstens etwas breiter als normal)
        this.cardW = Math.min(CARD_W + 24, Math.max(CARD_W, (paneRight - paneX + CARD_GAP) / columns - CARD_GAP));
        this.cardRows = Math.max(1, (cardsBottom - cardsTop + CARD_GAP) / (CARD_H + CARD_GAP));
    }

    /** Eine Zeile in der Seitenleiste: entweder ein Ordner oder eine Kategorie (ggf. eingerueckt in einem Ordner). */
    private record Row(@Nullable CategoryGroup group, @Nullable StyleCategory category, boolean child, boolean presets, boolean mods,
                       @Nullable String key) {
        Row(@Nullable CategoryGroup group, @Nullable StyleCategory category, boolean child) {
            this(group, category, child, false, false, null);
        }
    }

    private static final String KEY_PRESETS = "P:PRESETS";
    private static final String KEY_MODS = "P:MODS";
    private static final String KEY_SHADERS = "P:SHADERS";
    private static final String KEY_THEMES = "P:THEMES";
    private static final String KEY_IMPORT = "P:IMPORT";

    private static boolean isShaders(Row row) {
        return KEY_SHADERS.equals(row.key());
    }

    /** Standard-Reihenfolge der obersten Ebene. */
    private static List<String> defaultOrder() {
        List<String> order = new ArrayList<>(List.of(KEY_PRESETS, KEY_MODS, KEY_SHADERS, KEY_THEMES, KEY_IMPORT));
        for (Object entry : CategoryGroup.SIDEBAR) {
            order.add(entry instanceof CategoryGroup g ? "G:" + g.name() : "C:" + ((StyleCategory) entry).name());
        }
        return order;
    }

    /** Reihenfolge der obersten Ebene: die eigene (falls gesetzt), ergaenzt um neue Eintraege. */
    private static List<String> sidebarOrder() {
        List<String> defaults = defaultOrder();
        List<String> order = new ArrayList<>();
        for (String key : ConfigManager.get().sidebarOrder) {
            if (defaults.contains(key) && !order.contains(key)) {
                order.add(key);
            }
        }
        for (String key : defaults) {
            if (!order.contains(key)) {
                order.add(key);
            }
        }
        return order;
    }

    private void moveSidebarEntry(String key, int delta) {
        List<String> order = sidebarOrder();
        int i = order.indexOf(key);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= order.size()) {
            return;
        }
        order.set(i, order.get(j));
        order.set(j, key);
        ConfigManager.get().sidebarOrder = order;
        ConfigManager.save();
    }

    private void resetSidebarOrder() {
        ConfigManager.get().sidebarOrder = new ArrayList<>();
        ConfigManager.save();
    }

    private static @Nullable CategoryGroup groupOf(String key) {
        try {
            return key.startsWith("G:") ? CategoryGroup.valueOf(key.substring(2)) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static @Nullable StyleCategory categoryOf(String key) {
        try {
            return key.startsWith("C:") ? StyleCategory.valueOf(key.substring(2)) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isExpanded(CategoryGroup group) {
        return ConfigManager.get().expandedGroups.contains(group.name());
    }

    private static boolean matches(String query, StyleCategory category) {
        return category.name().toLowerCase(Locale.ROOT).contains(query)
            || categoryName(category).getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** Alle sichtbaren Zeilen. Bei einer Suche: flache Liste aller passenden Kategorien (auch aus Ordnern mit passendem Namen). */
    private List<Row> sidebarRows() {
        String query = search.toString().trim().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        if (!query.isEmpty()) {
            java.util.Set<StyleCategory> seen = new java.util.LinkedHashSet<>();
            for (Object entry : CategoryGroup.SIDEBAR) {
                if (entry instanceof CategoryGroup group) {
                    boolean groupMatches = Text.translatable(group.nameKey()).getString().toLowerCase(Locale.ROOT).contains(query);
                    for (StyleCategory category : group.categories()) {
                        if (groupMatches || matches(query, category)) {
                            seen.add(category);
                        }
                    }
                } else if (entry instanceof StyleCategory category && matches(query, category)) {
                    seen.add(category);
                }
            }
            for (StyleCategory category : seen) {
                rows.add(new Row(null, category, false));
            }
            return rows;
        }
        for (String key : sidebarOrder()) {
            if (key.equals(KEY_PRESETS)) {
                rows.add(new Row(null, null, false, true, false, key));
                continue;
            }
            if (key.equals(KEY_MODS)) {
                rows.add(new Row(null, null, false, false, true, key));
                continue;
            }
            if (key.equals(KEY_SHADERS) || key.equals(KEY_THEMES) || key.equals(KEY_IMPORT)) {
                rows.add(new Row(null, null, false, false, false, key));
                continue;
            }
            if (!sortMode) {
                // Spielmodi und einzelne Kategorien stehen als Tabs oben, links nur die Items des gewaehlten Tabs
                continue;
            }
            CategoryGroup group = groupOf(key);
            StyleCategory single = categoryOf(key);
            if (group != null) {
                rows.add(new Row(group, null, false, false, false, key));
            } else if (single != null) {
                rows.add(new Row(null, single, false, false, false, key));
            }
        }
        if (!sortMode) {
            for (StyleCategory category : tabCategories(currentTab())) {
                rows.add(new Row(null, category, false));
            }
        }
        return rows;
    }

    // ---- Spielmodus-Leiste (Tabs) ----

    /** Tabs in der Reihenfolge der Seitenleisten-Sortierung: alle Spielmodi, dann "Allgemein" fuer einzelne Kategorien. */
    private static List<String> tabKeys() {
        List<String> tabs = new ArrayList<>();
        boolean singles = false;
        for (String key : sidebarOrder()) {
            if (key.startsWith("G:") && groupOf(key) != null) {
                tabs.add(key);
            } else if (key.startsWith("C:") && categoryOf(key) != null) {
                singles = true;
            }
        }
        if (singles) {
            tabs.add(KEY_OTHER_TAB);
        }
        tabs.add(KEY_CUSTOMIZE_TAB);
        return tabs;
    }

    private static List<StyleCategory> tabCategories(String tab) {
        if (KEY_CUSTOMIZE_TAB.equals(tab)) {
            return TintedTextures.customizableCategories();
        }
        CategoryGroup group = groupOf(tab);
        if (group != null) {
            return group.categories();
        }
        List<StyleCategory> singles = new ArrayList<>();
        for (String key : sidebarOrder()) {
            StyleCategory single = categoryOf(key);
            if (single != null) {
                singles.add(single);
            }
        }
        return singles;
    }

    /** Gewaehlter Tab: der gespeicherte, sonst der erste, der die aktuelle Kategorie enthaelt. */
    private String currentTab() {
        List<String> tabs = tabKeys();
        String saved = ConfigManager.get().activeTab;
        if (saved != null && tabs.contains(saved)) {
            return saved;
        }
        for (String tab : tabs) {
            if (tabCategories(tab).contains(current)) {
                return tab;
            }
        }
        return tabs.isEmpty() ? KEY_OTHER_TAB : tabs.get(0);
    }

    private static Text tabName(String tab) {
        CategoryGroup group = groupOf(tab);
        if (KEY_CUSTOMIZE_TAB.equals(tab)) {
            return Text.translatable("arnestyles.gui.tab_customize");
        }
        return group != null ? Text.translatable(group.nameKey()) : Text.translatable("arnestyles.gui.tab_other");
    }

    private static String tabIcon(String tab) {
        CategoryGroup group = groupOf(tab);
        if (KEY_CUSTOMIZE_TAB.equals(tab)) {
            return "textures/item/brush.png";
        }
        return group != null ? group.icon() : "textures/item/paper.png";
    }

    private void openTab(String tab) {
        ConfigManager.get().activeTab = tab;
        ConfigManager.save();
        listScroll = 0;
        selectedRow = -1;
        List<StyleCategory> categories = tabCategories(tab);
        if (!categories.isEmpty()) {
            select(categories.contains(current) ? current : categories.get(0));
        }
    }

    /**
     * Tabs unter Titel und Knoepfen. Passen nicht alle Namen hinein, zeigen die nicht gewaehlten Tabs nur ihr Symbol.
     */
    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        List<String> tabs = tabKeys();
        String active = currentTab();
        int y1 = top + TAB_Y;
        int y2 = y1 + TAB_H;
        int available = right - left - 16;
        int gap = 3;
        int full = 0;
        for (String tab : tabs) {
            full += 24 + this.textRenderer.getWidth(tabName(tab)) + gap;
        }
        boolean compact = full > available;
        int x = left + 8;
        for (String tab : tabs) {
            boolean selected = tab.equals(active);
            boolean showName = !compact || selected;
            int w = showName ? 24 + this.textRenderer.getWidth(tabName(tab)) : 20;
            boolean hovered = inside(mouseX, mouseY, x, y1, x + w, y2);
            if (bigLayout) {
                // Dashboard: keine Kaesten, nur eine Linie unter dem aktiven Reiter
                if (hovered && !selected) {
                    round(context, x, y1, x + w, y2, C_CARD_HOVER);
                }
                if (selected) {
                    context.fill(x + 2, y2 - 1, x + w - 2, y2 + 1, C_ACCENT);
                }
            } else if (selected) {
                roundBordered(context, x, y1, x + w, y2, C_ACCENT, C_ACCENT_SOFT);
            } else {
                roundBordered(context, x, y1, x + w, y2, hovered ? C_ACCENT : C_EDGE, hovered ? C_CARD_HOVER : C_CARD);
            }
            drawIconAsset(context, tabIcon(tab), x + 2, y1 + 1);
            if (showName) {
                text(context, tabName(tab), x + 20, y1 + 5, selected ? C_TEXT : hovered ? C_TEXT_SOFT : C_MUTED);
            }
            final String key = tab;
            hits.add(new Hit(x, y1, x + w, y2, () -> openTab(key)));
            if (hovered && !showName) {
                hoveredTab = tabName(tab);
                hoveredTabX = x;
            }
            x += w + gap;
        }
    }

    /** Index der markierten Zeile: die zuletzt angeklickte, wenn sie noch zur aktuellen Kategorie passt, sonst die erste passende. */
    private int selectedRowIndex(List<Row> rows) {
        if (selectedRow >= 0 && selectedRow < rows.size() && rows.get(selectedRow).category() == current) {
            return selectedRow;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).category() == current) {
                return i;
            }
        }
        return -1;
    }

    private void toggleGroup(CategoryGroup group) {
        List<String> expanded = ConfigManager.get().expandedGroups;
        if (!expanded.remove(group.name())) {
            expanded.add(group.name());
        }
        ConfigManager.save();
        selectedRow = -1;
    }

    /** Sorgt dafuer, dass die aktuelle Kategorie sichtbar ist: ggf. einen Ordner aufklappen, der sie enthaelt. */
    private void revealCurrent() {
        for (Object entry : CategoryGroup.SIDEBAR) {
            if (entry == current) {
                return;
            }
        }
        for (Object entry : CategoryGroup.SIDEBAR) {
            if (entry instanceof CategoryGroup group && isExpanded(group) && group.categories().contains(current)) {
                return;
            }
        }
        for (Object entry : CategoryGroup.SIDEBAR) {
            if (entry instanceof CategoryGroup group && group.categories().contains(current)) {
                ConfigManager.get().expandedGroups.add(group.name());
                return;
            }
        }
    }

    // ---- Aktionen ----

    private void select(StyleCategory category) {
        this.presetsOpen = false;
        this.modsOpen = false;
        this.shadersOpen = false;
        this.themesOpen = false;
        this.importOpen = false;
        this.current = category;
        CloudStyles.request(category);
        if (!tabCategories(currentTab()).contains(category)) {
            for (String tab : tabKeys()) {
                if (tabCategories(tab).contains(category)) {
                    ConfigManager.get().activeTab = tab;
                    break;
                }
            }
        }
        this.cardScroll = 0;
        ConfigManager.get().activeCategory = category.name();
        ConfigManager.save();
        ensureCategoryVisible();
    }

    private void ensureCategoryVisible() {
        computeLayout();
        if (presetsOpen || modsOpen || shadersOpen || themesOpen || importOpen) {
            // Presets, Mods und Shader stehen oben in der Seitenleiste
            listScroll = 0;
            return;
        }
        int index = selectedRowIndex(sidebarRows());
        if (index < 0) {
            return;
        }
        if (index < listScroll) {
            listScroll = index;
        } else if (index >= listScroll + rowsVisible) {
            listScroll = index - rowsVisible + 1;
        }
    }

    /** Pfeiltasten hoch/runter: zur naechsten Kategorie-Zeile (Ordner werden uebersprungen). */
    private void moveCategory(int delta) {
        List<Row> rows = sidebarRows();
        if (rows.isEmpty()) {
            return;
        }
        int index = selectedRowIndex(rows);
        for (int step = 0; step < rows.size(); step++) {
            index = Math.floorMod(index + delta, rows.size());
            if (rows.get(index).category() != null) {
                selectedRow = index;
                select(rows.get(index).category());
                return;
            }
        }
    }

    private void onSearchChanged() {
        listScroll = 0;
        selectedRow = -1;
        List<Row> rows = sidebarRows();
        if (!search.isEmpty() && !rows.isEmpty() && rows.stream().noneMatch(r -> r.category() == current)) {
            select(rows.get(0).category());
        }
        ensureCategoryVisible();
    }

    private void toggleMod() {
        ArneStylesConfig config = ConfigManager.get();
        config.modEnabled = !config.modEnabled;
        ConfigManager.save();
        markDirty();
    }

    private void toggleCurrent() {
        ArneStylesConfig config = ConfigManager.get();
        config.setEnabled(current, !config.isEnabled(current));
        ConfigManager.save();
        markDirty();
    }

    private void chooseStyle(StyleDefinition style) {
        ArneStylesConfig config = ConfigManager.get();
        if (style.id().equals(StyleRegistry.getSelected(current).id())) {
            return;
        }
        config.setSelectedStyle(current, style.id());
        // Grosse Styles (Himmel) werden erst beim Auswaehlen geladen.
        CloudStyles.requestStyle(style);
        if (!style.isVanilla()) {
            // Wer einen eigenen Style anklickt, will ihn auch sehen.
            config.setEnabled(current, true);
        }
        ConfigManager.save();
        markDirty();
    }

    private void moveStyle(int delta) {
        if (current == StyleCategory.ESSENTIALS) {
            return;
        }
        List<StyleDefinition> styles = StyleRegistry.getStyles(current);
        int index = styles.indexOf(StyleRegistry.getSelected(current));
        int next = Math.max(0, Math.min(styles.size() - 1, index + delta));
        chooseStyle(styles.get(next));
        computeLayout();
        int row = next / columns;
        if (row < cardScroll) {
            cardScroll = row;
        } else if (row >= cardScroll + cardRows) {
            cardScroll = row - cardRows + 1;
        }
    }

    private void toggleEssential(String key, boolean needsReload) {
        ArneStylesConfig config = ConfigManager.get();
        boolean on = !config.isEssentialOn(key);
        config.setEssentialOn(key, on);
        if (on && !config.isEnabled(StyleCategory.ESSENTIALS)) {
            config.setEnabled(StyleCategory.ESSENTIALS, true);
            // Andere Essentials mit eigenen Dateien werden dadurch ebenfalls aktiv.
            markDirty();
        }
        if (needsReload) {
            markDirty();
        }
        ConfigManager.save();
    }

    private void adjustFireDepth(float step) {
        ArneStylesConfig config = ConfigManager.get();
        float next = Math.round((config.lowFireOffset + step) * 100f) / 100f;
        config.lowFireOffset = Math.max(0.0f, Math.min(0.5f, next));
        ConfigManager.save();
    }

    /** Unterfenster oeffnen, ohne dass dieses Menue dabei neu laedt. */
    void openChild(net.minecraft.client.gui.screen.Screen child) {
        if (this.client == null) {
            return;
        }
        childOpen = true;
        this.client.setScreen(child);
        childOpen = false;
    }

    /** Nach dem Speichern einer Zeichnung: neuen Style einlesen, waehlen und im Customize-Tab zeigen. */
    void drawingSaved(StyleCategory category, String id, String asset) {
        reloadStyles();
        current = category;
        List<StyleDefinition> styles = StyleRegistry.getStyles(category);
        for (int i = 0; i < styles.size(); i++) {
            if (styles.get(i).id().equals(id)) {
                chooseStyle(styles.get(i));
                customizeCategory = category;
                customizeStyle = i;
                customizeAsset = asset;
                break;
            }
        }
        markDirty();
    }

    private void openDrawEditor(StyleDefinition style, String asset) {
        Text error = DrawScreen.open(this, current, style, asset);
        if (error != null) {
            drawError = error;
            drawErrorAt = System.currentTimeMillis();
        }
    }

    private void reloadStyles() {
        CloudStyles.retry(current);
        StyleRegistry.reload();
        PreviewTextures.clear();
        previewAssetCache.clear();
        uniquePreviewCache.clear();
        soundCountCache.clear();
        styleHasFileCache.clear();
        modelFaceCache.clear();
        cardScroll = 0;
        markDirty();
    }

    private void openFolder() {
        if (importOpen) {
            try {
                Files.createDirectories(PackImporter.importDir());
            } catch (java.io.IOException e) {
                // Ordner oeffnen klappt dann eben nicht
            }
            Util.getOperatingSystem().open(PackImporter.importDir());
            return;
        }
        if (themesOpen) {
            try {
                Files.createDirectories(GuiTheme.dir());
            } catch (java.io.IOException e) {
                // Ordner oeffnen klappt dann eben nicht
            }
            Util.getOperatingSystem().open(GuiTheme.dir());
            return;
        }
        if (shadersOpen) {
            ShaderPacks.ensureDir();
            Util.getOperatingSystem().open(ShaderPacks.dir());
            return;
        }
        StyleRegistry.reload();
        Util.getOperatingSystem().open(StyleRegistry.categoryDir(current));
    }

    private static boolean inside(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    // ---- Eingabe ----

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (hexEditKey != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (hexBuffer.length() > 0) {
                    hexBuffer.setLength(hexBuffer.length() - 1);
                }
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                applyHex();
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                hexEditKey = null;
                hexBuffer.setLength(0);
            }
            return true;
        }
        if (modsOpen && key == GLFW.GLFW_KEY_ESCAPE && modSearching()) {
            modSearch.setLength(0);
            modFilter = 0;
            cardScroll = 0;
            return true;
        }
        if (modsOpen && key == GLFW.GLFW_KEY_BACKSPACE) {
            if (modSearch.length() > 0) {
                modSearch.setLength(modSearch.offsetByCodePoints(modSearch.length(), -1));
                cardScroll = 0;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_DELETE) {
            if (resetArmed) {
                ConfigManager.resetToDefaults();
                search.setLength(0);
                listScroll = 0;
                cardScroll = 0;
                current = parseCategory(ConfigManager.get().activeCategory);
                resetArmed = false;
                markDirty();
            } else {
                resetArmed = true;
            }
            return true;
        }
        resetArmed = false;

        if (themesOpen && key == GLFW.GLFW_KEY_BACKSPACE && !GuiTheme.active().builtIn()) {
            GuiTheme.Theme theme = GuiTheme.active();
            String name = theme.name();
            if (!name.isEmpty()) {
                GuiTheme.rename(theme, name.substring(0, name.offsetByCodePoints(name.length(), -1)));
            }
            return true;
        }
        if (presetsOpen && (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (presetName.length() > 0) {
                    presetName.setLength(presetName.offsetByCodePoints(presetName.length(), -1));
                }
            } else {
                savePreset();
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (search.length() > 0) {
                    search.setLength(search.offsetByCodePoints(search.length(), -1));
                    onSearchChanged();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveCategory(1);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveCategory(-1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveStyle(1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveStyle(-1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                toggleCurrent();
                return true;
            }
            case GLFW.GLFW_KEY_F5 -> {
                reloadStyles();
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                if (current == StyleCategory.ESSENTIALS) {
                    adjustFireDepth(FIRE_STEP);
                }
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                if (current == StyleCategory.ESSENTIALS) {
                    adjustFireDepth(-FIRE_STEP);
                }
                return true;
            }
            default -> {
                return super.keyPressed(input);
            }
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (System.currentTimeMillis() - openedAt < INPUT_GRACE_MS) {
            return true;
        }
        int codepoint = input.codepoint();
        if (Character.isISOControl(codepoint)) {
            return true;
        }
        if (themesOpen && !GuiTheme.active().builtIn()) {
            GuiTheme.Theme theme = GuiTheme.active();
            if (theme.name().codePointCount(0, theme.name().length()) < THEME_NAME_MAX) {
                GuiTheme.rename(theme, theme.name() + Character.toString(codepoint));
            }
            return true;
        }
        if (presetsOpen) {
            if (presetName.length() < PresetManager.MAX_NAME_LENGTH) {
                presetName.appendCodePoint(codepoint);
            }
            return true;
        }
        if (hexEditKey != null) {
            if (hexBuffer.length() < 6 && Character.digit(codepoint, 16) >= 0) {
                hexBuffer.appendCodePoint(Character.toUpperCase(codepoint));
            }
            return true;
        }
        if (modsOpen) {
            if (modSearch.length() < 24) {
                modSearch.appendCodePoint(codepoint);
                cardScroll = 0;
            }
            return true;
        }
        if (search.codePointCount(0, search.length()) < MAX_SEARCH_LENGTH) {
            search.appendCodePoint(codepoint);
            onSearchChanged();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x() / scale;
        double my = click.y() / scale;
        if (click.button() == 0 && wheelApply != null && insideWheel(mx, my)) {
            wheelDragging = true;
            applyWheel(mx, my);
            return true;
        }
        // Regler: links klicken/ziehen setzt den Wert, rechts setzt ihn auf normal zurueck.
        for (SliderHit hit : sliderHits) {
            if (inside(mx, my, hit.x1(), hit.y1(), hit.x2(), hit.y2())) {
                resetArmed = false;
                if (click.button() == 1) {
                    hit.slider().reset();
                    ConfigManager.save();
                } else if (click.button() == 0) {
                    draggingSlider = hit;
                    applySlider(hit, mx);
                }
                return true;
            }
        }
        if (click.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (inside(mx, my, hit.x1(), hit.y1(), hit.x2(), hit.y2())) {
                    resetArmed = false;
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (wheelDragging) {
            applyWheel(click.x() / scale, click.y() / scale);
            return true;
        }
        if (draggingSlider != null) {
            applySlider(draggingSlider, click.x() / scale);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (wheelDragging || draggingSlider != null) {
            wheelDragging = false;
            draggingSlider = null;
            ConfigManager.save();
            if (themesOpen && !GuiTheme.active().builtIn()) {
                GuiTheme.save(GuiTheme.active());
            }
            return true;
        }
        return super.mouseReleased(click);
    }

    private void applySlider(SliderHit hit, double mouseX) {
        float fraction = (float) ((mouseX - hit.barX1()) / Math.max(1, hit.barX2() - hit.barX1()));
        hit.slider().setFraction(fraction);
        if (hit.slider() instanceof TintSlider tintSlider && TintedTextures.get(tintSlider.category()).on) {
            markDirty();
        }
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontalAmount, double verticalAmount) {
        computeLayout();
        double mouseX = rawX / scale;
        int step = (int) Math.signum(verticalAmount);
        if (mouseX < left + LIST_WIDTH) {
            int max = Math.max(0, sidebarRows().size() - rowsVisible);
            listScroll = Math.max(0, Math.min(max, listScroll - step));
        } else {
            cardScroll = Math.max(0, Math.min(maxCardScroll(), cardScroll - step));
        }
        return true;
    }

    private int maxCardScroll() {
        if (presetsOpen) {
            return Math.max(0, keybindPage ? com.arnestyles.client.config.KeybindSets.list().size() : PresetManager.list().size() - 1);
        }
        if (modsOpen) {
            return Math.max(0, modLineCount - 1);
        }
        if (shadersOpen) {
            return Math.max(0, shaderRows().size() - 1);
        }
        if (themesOpen) {
            return Math.max(0, themeRowCount() - 1);
        }
        if (importOpen) {
            return Math.max(0, PackImporter.packs().size() - 1);
        }
        if (customizing()) {
            return Math.max(0, customizeTotalRows - customizeVisibleRows);
        }
        if (current == StyleCategory.ESSENTIALS) {
            return Math.max(0, EssentialSlider.values().length + essentialRows().size() - essentialRowsVisible());
        }
        if (current == StyleCategory.PERFORMANCE_SETTINGS) {
            return Math.max(0, PERFORMANCE_LINES - performanceLinesVisible());
        }
        int total = StyleRegistry.getStyles(current).size();
        int rows = (total + columns - 1) / columns;
        return Math.max(0, rows - cardRows);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        if (childOpen) {
            // Nur kurz im Zeichen-Editor: neu geladen wird erst, wenn das Menue wirklich zugeht
            super.removed();
            return;
        }
        PreviewTextures.clear();
        PerformanceSettings.flush();
        if (dirty && this.client != null) {
            dirty = false;
            this.client.reloadResources();
        }
        super.removed();
    }

    // ---- Zeichen-Bausteine ----

    /** Rechteck mit abgeschnittenen Ecken (wirkt abgerundet). Eckig oder sehr rund je nach Aufbau. */
    private static void round(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        if (corners == 1) {
            context.fill(x1, y1, x2, y2, color);
            return;
        }
        if (corners == 2 && x2 - x1 >= 6 && y2 - y1 >= 6) {
            context.fill(x1 + 2, y1, x2 - 2, y2, color);
            context.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, color);
            context.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, color);
            context.fill(x1, y1 + 2, x1 + 1, y2 - 2, color);
            context.fill(x2 - 1, y1 + 2, x2, y2 - 2, color);
            return;
        }
        context.fill(x1 + 1, y1, x2 - 1, y2, color);
        context.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        context.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** Abgerundete Flaeche mit 1-Pixel-Rand. Bei Minecraft-Knoepfen mit heller Kante oben links und dunkler unten rechts. */
    private static void roundBordered(DrawContext context, int x1, int y1, int x2, int y2, int border, int fill) {
        if (bevel && x2 - x1 >= 8 && y2 - y1 >= 8) {
            // Nur der Rahmen in der Randfarbe, damit eine halb durchsichtige Fuellung nicht eingefaerbt wird
            context.fill(x1, y1, x2, y1 + 1, border);
            context.fill(x1, y2 - 1, x2, y2, border);
            context.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
            context.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
            context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
            int light = GuiTheme.mix(fill, 0xFFFFFFFF, 0.3f);
            int dark = GuiTheme.mix(fill, 0xFF000000, 0.35f);
            context.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, light);
            context.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, light);
            context.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, dark);
            context.fill(x2 - 2, y1 + 2, x2 - 1, y2 - 1, dark);
            return;
        }
        round(context, x1, y1, x2, y2, border);
        round(context, x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    /** Kleiner Kippschalter (22x11), je nach Aufbau als Schalter, AN/AUS-Text oder Kaestchen. */
    private void drawSwitch(DrawContext context, int x, int y, boolean on, boolean dimmed) {
        int track = on ? (dimmed ? C_WARN : C_OK) : C_OFF;
        if (switchStyle == 1) {
            Text label = Text.translatable(on ? "arnestyles.layout.on" : "arnestyles.layout.off");
            int tw = this.textRenderer.getWidth(label);
            text(context, label, x + 22 - tw, y + 2, on ? track : C_MUTED);
            return;
        }
        if (switchStyle == 2) {
            int bx = x + 11;
            roundBordered(context, bx, y, bx + 11, y + 11, on ? track : C_OFF, on ? track : C_FIELD);
            if (on) {
                // Haekchen
                int c = 0xFF000000 | (GuiTheme.toHsv(track & 0xFFFFFF)[2] > 0.6f ? 0x101010 : 0xFFFFFF);
                context.fill(bx + 2, y + 5, bx + 4, y + 7, c);
                context.fill(bx + 4, y + 7, bx + 5, y + 8, c);
                context.fill(bx + 5, y + 6, bx + 6, y + 7, c);
                context.fill(bx + 6, y + 5, bx + 7, y + 6, c);
                context.fill(bx + 7, y + 4, bx + 8, y + 5, c);
                context.fill(bx + 8, y + 3, bx + 9, y + 4, c);
            }
            return;
        }
        drawClassicSwitch(context, x, y, track);
    }

    /** Schiebeschalter, unabhaengig vom Aufbau (Hauptschalter in der Kopfzeile). */
    private static void drawClassicSwitch(DrawContext context, int x, int y, int track) {
        boolean on = track != C_OFF;
        round(context, x, y, x + 22, y + 11, track);
        int knobX = on ? x + 12 : x + 2;
        round(context, knobX, y + 2, knobX + 8, y + 9, C_TEXT);
    }

    /** Markierung des aktiven Eintrags: Flaeche mit kurzem Strich, Leiste links oder Umrandung. */
    private static void drawActive(DrawContext context, int x1, int y1, int x2, int y2) {
        switch (activeStyle) {
            case 1 -> {
                round(context, x1, y1, x2, y2, C_CARD_HOVER);
                context.fill(x1, y1, x1 + 3, y2, C_ACCENT);
            }
            case 2 -> roundBordered(context, x1, y1, x2, y2, C_ACCENT, 0xFF000000 | GuiTheme.mix(C_CARD, C_ACCENT, 0.18f));
            default -> {
                round(context, x1, y1, x2, y2, C_ACCENT_SOFT);
                context.fill(x1, y1 + 2, x1 + 2, y2 - 2, C_ACCENT);
            }
        }
    }

    /** Name eines Eintrags der Seitenleiste (nur Symbole: als Schild, wenn die Maus drauf ist). */
    private void sideLabel(DrawContext context, Text label, int x, int y, boolean hovered) {
        if (iconSidebar) {
            if (hovered) {
                sideTip = label;
                sideTipY = y;
            }
        } else {
            text(context, label, x, y + 4, C_TEXT);
        }
    }

    /** Lesbare Textfarbe auf der Akzentfarbe. */
    private static int onAccent() {
        return GuiTheme.toHsv(C_ACCENT & 0xFFFFFF)[2] > 0.75f && GuiTheme.toHsv(C_ACCENT & 0xFFFFFF)[1] < 0.8f ? 0xFF101010 : 0xFFFFFFFF;
    }

    /** Pillen-Knopf, rechte Kante bei x2. Gibt die linke Kante zurueck. */
    private int drawPillRight(DrawContext context, int x2, int y, Text label, int color, int mouseX, int mouseY, Runnable action) {
        int x1 = x2 - this.textRenderer.getWidth(label) - 12;
        int y2 = y + 14;
        boolean hovered = inside(mouseX, mouseY, x1, y, x2, y2);
        roundBordered(context, x1, y, x2, y2, hovered ? C_ACCENT : C_EDGE, hovered ? C_CARD_HOVER : C_CARD);
        text(context, label, x1 + 6, y + 3, color);
        hits.add(new Hit(x1, y, x2, y2, action));
        return x1;
    }

    /** Text in groesserer Schrift. */
    private void drawBigText(DrawContext context, Text text, int x, int y, float scale, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        text(context, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    /** Zeichnet eine (ggf. animierte) Textur als Quadrat. */
    private static void drawTile(DrawContext context, PreviewTextures.Entry entry, int x, int y, int size, int color) {
        int frameH = entry.frameHeight();
        context.drawTexture(RenderPipelines.GUI_TEXTURED, entry.id(), x, y, 0.0f, (float) entry.currentFrame() * frameH,
            size, size, entry.width(), frameH, entry.width(), entry.height(), color);
    }

    /** Zeichnet einen Ausschnitt (u, v, w, h in Pixeln einer Textur mit Grundbreite baseWidth). */
    private static void drawRegion(DrawContext context, PreviewTextures.Entry entry, int baseWidth, float u, float v, float w, float h,
                                   int x, int y, int drawW, int drawH) {
        float s = entry.width() / (float) baseWidth;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, entry.id(), x, y, u * s, v * s, drawW, drawH,
            Math.round(w * s), Math.round(h * s), entry.width(), entry.height());
    }

    // ---- Zeichnen ----

    @Override
    public void render(DrawContext context, int rawMouseX, int rawMouseY, float deltaTicks) {
        if (cloudGeneration != CloudStyles.generation()) {
            // Ein Download ist fertig: Styles neu einlesen, Vorschauen neu aufbauen
            cloudGeneration = CloudStyles.generation();
            StyleRegistry.reload();
            PreviewTextures.clear();
            previewAssetCache.clear();
            uniquePreviewCache.clear();
            soundCountCache.clear();
            styleHasFileCache.clear();
            if (CloudStyles.consumeActiveChanged()) {
                markDirty();
            }
        }
        applyTheme();
        computeLayout();
        int mouseX = (int) Math.floor(rawMouseX / scale);
        int mouseY = (int) Math.floor(rawMouseY / scale);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        renderScaled(context, mouseX, mouseY);
        context.getMatrices().popMatrix();
    }

    /** Das ganze Menue in eigener Groesse: alle Koordinaten sind durch {@link #scale} geteilt. */
    private void renderScaled(DrawContext context, int mouseX, int mouseY) {
        hits.clear();
        hoveredStyle = null;
        wheelApply = null;
        if (PackImporter.pollFinished()) {
            // Neue Styles: Vorschauen neu laden, beim Schliessen Texturen neu laden
            PreviewTextures.clear();
            previewAssetCache.clear();
            uniquePreviewCache.clear();
            soundCountCache.clear();
            styleHasFileCache.clear();
            markDirty();
        }
        sliderHits.clear();
        shownTintHover = tintHover;
        tintHover = null;
        computeLayout();
        cardScroll = Math.max(0, Math.min(maxCardScroll(), cardScroll));
        ArneStylesConfig config = ConfigManager.get();

        context.fill(0, 0, vw, vh, C_DIM);
        roundBordered(context, left - 1, top - 1, right + 1, bottom + 1, C_EDGE, C_PANEL);

        // Kopfzeile mit Verlauf
        if (flatHeader) {
            context.fill(left, top, right, top + HEADER_H, C_HEADER_TOP);
        } else {
            context.fillGradient(left, top, right, top + HEADER_H, C_HEADER_TOP, C_HEADER_BOTTOM);
        }
        context.fill(left, top + HEADER_H, right, top + HEADER_H + 1, C_EDGE);
        round(context, left + 10, top + 9, left + 14, top + 19, C_ACCENT);
        drawBigText(context, this.title, left + 20, top + 9, 1.15f, C_TEXT);
        hoveredTab = null;
        drawTabs(context, mouseX, mouseY);

        int x = right - 8;
        x = drawPillRight(context, x, top + 7, Text.translatable("arnestyles.gui.reload"), C_TEXT, mouseX, mouseY, this::reloadStyles);
        x = drawPillRight(context, x - 4, top + 7, Text.translatable("arnestyles.gui.open_folder"), C_TEXT, mouseX, mouseY, this::openFolder);
        // Hauptschalter
        Text modLabel = Text.translatable(config.modEnabled ? "arnestyles.gui.mod_on" : "arnestyles.gui.mod_off");
        int switchX2 = x - 8;
        int labelW = this.textRenderer.getWidth(modLabel);
        int switchX1 = switchX2 - labelW - 30;
        boolean modHover = inside(mouseX, mouseY, switchX1, top + 7, switchX2, top + 21);
        roundBordered(context, switchX1, top + 7, switchX2, top + 21, modHover ? C_ACCENT : C_EDGE, modHover ? C_CARD_HOVER : C_CARD);
        drawClassicSwitch(context, switchX1 + 3, top + 8 + 1, config.modEnabled ? C_OK : C_OFF);
        text(context, modLabel, switchX1 + 28, top + 10, config.modEnabled ? C_OK : C_DANGER);
        hits.add(new Hit(switchX1, top + 7, switchX2, top + 21, this::toggleMod));

        // Seitenleiste
        context.fill(left, top + HEADER_H + 1, left + LIST_WIDTH, bottom - FOOTER_H, C_SIDEBAR);
        context.fill(left + LIST_WIDTH, top + HEADER_H + 1, left + LIST_WIDTH + 1, bottom - FOOTER_H, C_EDGE);
        sideTip = null;
        if (!iconSidebar) {
            drawSearchBox(context);
        }
        drawCategoryList(context, mouseX, mouseY);
        drawDetails(context, mouseX, mouseY);
        if (inspectorX < right) {
            drawInspector(context, mouseX, mouseY);
        }
        drawFooter(context, mouseX, mouseY);
        if (sideTip != null) {
            // Seitenleiste nur mit Symbolen: Name des Eintrags unter der Maus
            int tw = this.textRenderer.getWidth(sideTip) + 8;
            int tx = left + LIST_WIDTH + 3;
            roundBordered(context, tx, sideTipY + 1, tx + tw, sideTipY + 15, C_ACCENT, C_PANEL);
            text(context, sideTip, tx + 4, sideTipY + 4, C_TEXT);
        }
        if (hoveredTab != null) {
            // Name des Tabs, der nur als Symbol zu sehen ist
            int tw = this.textRenderer.getWidth(hoveredTab) + 8;
            int ty = top + TAB_Y + TAB_H + 2;
            roundBordered(context, hoveredTabX, ty, hoveredTabX + tw, ty + 13, C_ACCENT, C_PANEL);
            text(context, hoveredTab, hoveredTabX + 4, ty + 3, C_TEXT);
        }
    }

    private void drawSearchBox(DrawContext context) {
        int x1 = left + 6;
        int y1 = top + HEADER_H + 6;
        int x2 = left + LIST_WIDTH - 26;
        int y2 = y1 + 16;
        // Sortier-Knopf rechts neben der Suche
        int sx1 = x2 + 3;
        int sx2 = left + LIST_WIDTH - 6;
        roundBordered(context, sx1, y1, sx2, y2, sortMode ? C_ACCENT : C_EDGE, sortMode ? C_ACCENT_SOFT : C_FIELD);
        int cx = (sx1 + sx2) / 2;
        drawArrowUp(context, cx - 3, y1 + 4, C_TEXT);
        drawArrowDown(context, cx + 2, y1 + 8, C_TEXT);
        hits.add(new Hit(sx1, y1, sx2, y2, () -> {
            sortMode = !sortMode;
            listScroll = 0;
        }));
        roundBordered(context, x1, y1, x2, y2, search.length() > 0 ? C_ACCENT : C_EDGE, C_FIELD);
        if (search.length() == 0) {
            text(context, Text.translatable("arnestyles.gui.search"), x1 + 6, y1 + 4, C_MUTED);
        } else {
            boolean cursor = (System.currentTimeMillis() / 500L) % 2L == 0L;
            text(context, cursor ? search + "_" : search.toString(), x1 + 6, y1 + 4, C_TEXT);
        }
    }

    private void drawCategoryList(DrawContext context, int mouseX, int mouseY) {
        List<Row> rows = sidebarRows();
        if (rows.isEmpty()) {
            text(context, Text.translatable("arnestyles.gui.no_results"), left + 10, listTop + 4, C_MUTED);
            return;
        }
        listScroll = Math.max(0, Math.min(Math.max(0, rows.size() - rowsVisible), listScroll));
        ArneStylesConfig config = ConfigManager.get();
        int marked = presetsOpen || modsOpen || shadersOpen || themesOpen || importOpen ? -1 : selectedRowIndex(rows);
        for (int r = 0; r < rowsVisible; r++) {
            int index = listScroll + r;
            if (index >= rows.size()) {
                break;
            }
            Row row = rows.get(index);
            int y = listTop + r * ROW_HEIGHT;
            int x1 = left + 4;
            int x2 = left + LIST_WIDTH - 4;
            int y2 = y + ROW_HEIGHT - 2;
            boolean hovered = inside(mouseX, mouseY, x1, y, x2, y2);

            if (sortMode && row.key() != null) {
                drawSortRow(context, row, x1, y, x2, y2, mouseX, mouseY);
                continue;
            }
            if (row.mods()) {
                if (modsOpen) {
                    drawActive(context, x1, y, x2, y2);
                } else if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, "textures/item/command_block_minecart.png", x1 + 3, y + 1);
                sideLabel(context, Text.translatable("arnestyles.gui.mods"), x1 + 22, y, hovered);
                hits.add(new Hit(x1, y, x2, y2, this::openMods));
                continue;
            }
            if (KEY_IMPORT.equals(row.key())) {
                if (importOpen) {
                    drawActive(context, x1, y, x2, y2);
                } else if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, IMPORT_ICON, x1 + 3, y + 1);
                sideLabel(context, Text.translatable("arnestyles.gui.import"), x1 + 22, y, hovered);
                if (PackImporter.running() != null) {
                    round(context, x2 - 6, y + 6, x2 - 2, y + 10, C_WARN);
                }
                hits.add(new Hit(x1, y, x2, y2, this::openImport));
                continue;
            }
            if (KEY_THEMES.equals(row.key())) {
                if (themesOpen) {
                    drawActive(context, x1, y, x2, y2);
                } else if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, THEME_ICON, x1 + 3, y + 1);
                sideLabel(context, Text.translatable("arnestyles.gui.themes"), x1 + 22, y, hovered);
                hits.add(new Hit(x1, y, x2, y2, this::openThemes));
                continue;
            }
            if (isShaders(row)) {
                if (shadersOpen) {
                    drawActive(context, x1, y, x2, y2);
                } else if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, "textures/item/spyglass.png", x1 + 3, y + 1);
                sideLabel(context, Text.translatable("arnestyles.gui.shaders"), x1 + 22, y, hovered);
                hits.add(new Hit(x1, y, x2, y2, this::openShaders));
                continue;
            }
            if (row.presets()) {
                if (presetsOpen) {
                    drawActive(context, x1, y, x2, y2);
                } else if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, "textures/item/writable_book.png", x1 + 3, y + 1);
                sideLabel(context, Text.translatable("arnestyles.gui.presets"), x1 + 22, y, hovered);
                hits.add(new Hit(x1, y, x2, y2, this::openPresets));
                continue;
            }
            if (row.category() == null) {
                // Ordner
                CategoryGroup group = row.group();
                boolean open = isExpanded(group);
                if (hovered) {
                    round(context, x1, y, x2, y2, C_CARD);
                }
                drawIconAsset(context, group.icon(), x1 + 3, y + 1);
                sideLabel(context, Text.translatable(group.nameKey()), x1 + 22, y, hovered);
                if (group.categories().stream().anyMatch(config::isEnabled)) {
                    int dot = iconSidebar ? 6 : 16;
                    round(context, x2 - dot, y + 6, x2 - dot + 4, y + 10, config.modEnabled ? C_OK : C_WARN);
                }
                if (!iconSidebar) {
                    drawArrow(context, x2 - 8, y + 5, open);
                }
                final int rowIndex = index;
                hits.add(new Hit(x1, y, x2, y2, () -> {
                    toggleGroup(group);
                    selectedRow = -1;
                }));
                continue;
            }

            StyleCategory category = row.category();
            int indent = row.child() && !iconSidebar ? 10 : 0;
            if (index == marked) {
                drawActive(context, x1 + indent, y, x2, y2);
            } else if (hovered) {
                round(context, x1 + indent, y, x2, y2, C_CARD);
            }
            if (row.child() && !iconSidebar) {
                context.fill(x1 + 4, y, x1 + 5, y2 + 2, C_EDGE);
            }
            drawCategoryIcon(context, category, x1 + indent + 3, y + 1);
            if (iconSidebar) {
                if (hovered) {
                    sideTip = categoryName(category);
                    sideTipY = y;
                }
            } else {
                String name = this.textRenderer.trimToWidth(categoryName(category).getString(), x2 - x1 - indent - 34);
                text(context, name, x1 + indent + 22, y + 4, index == marked ? C_TEXT : C_TEXT_SOFT);
            }
            if (config.isEnabled(category)) {
                int dot = iconSidebar ? 6 : 8;
                round(context, x2 - dot, y + 6, x2 - dot + 4, y + 10, config.modEnabled ? C_OK : C_WARN);
            }
            final int rowIndex = index;
            hits.add(new Hit(x1, y, x2, y2, () -> {
                selectedRow = rowIndex;
                select(category);
            }));
        }
        if (listScroll > 0) {
            text(context, "^", left + LIST_WIDTH - 12, listTop - 9, C_MUTED);
        }
        if (listScroll + rowsVisible < rows.size()) {
            text(context, "v", left + LIST_WIDTH - 12, bottom - FOOTER_H - 10, C_MUTED);
        }
    }

    /** Kleines Dreieck: zu (nach rechts) oder offen (nach unten). */
    private static void drawArrow(DrawContext context, int x, int y, boolean open) {
        if (open) {
            context.fill(x - 3, y + 1, x + 4, y + 2, C_MUTED);
            context.fill(x - 2, y + 2, x + 3, y + 3, C_MUTED);
            context.fill(x - 1, y + 3, x + 2, y + 4, C_MUTED);
            context.fill(x, y + 4, x + 1, y + 5, C_MUTED);
        } else {
            context.fill(x - 1, y - 1, x, y + 6, C_MUTED);
            context.fill(x, y, x + 1, y + 5, C_MUTED);
            context.fill(x + 1, y + 1, x + 2, y + 4, C_MUTED);
            context.fill(x + 2, y + 2, x + 3, y + 3, C_MUTED);
        }
    }

    /** Symbol aus einer Minecraft-Textur (Gras wird gruen eingefaerbt). */
    private void drawIconAsset(DrawContext context, String asset, int x, int y) {
        PreviewTextures.Entry entry = PreviewTextures.forRoot("icon", null, asset);
        if (entry != null) {
            drawTile(context, entry, x, y, 16, asset.endsWith("grass_block_top.png") ? GRASS_TINT : 0xFFFFFFFF);
        }
    }

    /** Symbol der Kategorie: die erste Vorschau-Textur aus normalem Minecraft. */
    private void drawCategoryIcon(DrawContext context, StyleCategory category, int x, int y) {
        String asset = category.previewAssets().isEmpty() ? null : category.previewAssets().get(0);
        PreviewTextures.Entry entry = asset == null ? null : PreviewTextures.forRoot("icon", null, asset);
        if (entry == null) {
            return;
        }
        if (category == StyleCategory.ARMOR) {
            // Ruestungs-Textur: nur den Helm zeigen
            drawRegion(context, entry, 64, 8, 8, 8, 8, x + 2, y + 2, 12, 12);
        } else if (category == StyleCategory.SHIELD) {
            drawRegion(context, entry, 64, 1, 1, 12, 22, x + 4, y, 8, 15);
        } else if (category == StyleCategory.INVENTORY) {
            drawRegion(context, entry, 256, 0, 0, 176, 166, x, y + 1, 16, 15);
        } else if (category == StyleCategory.HUD) {
            drawRegion(context, entry, entry.width(), 0, 0, 22, 22, x + 1, y + 1, 14, 14);
        } else {
            drawTile(context, entry, x, y, 16, asset.endsWith("grass_block_top.png") ? GRASS_TINT : 0xFFFFFFFF);
        }
    }

    private void drawDetails(DrawContext context, int mouseX, int mouseY) {
        if (presetsOpen) {
            drawPresets(context, mouseX, mouseY);
            return;
        }
        if (modsOpen) {
            drawMods(context, mouseX, mouseY);
            return;
        }
        if (shadersOpen) {
            drawShaders(context, mouseX, mouseY);
            return;
        }
        if (themesOpen) {
            drawThemes(context, mouseX, mouseY);
            return;
        }
        if (importOpen) {
            drawImport(context, mouseX, mouseY);
            return;
        }
        if (customizing()) {
            drawCustomize(context, mouseX, mouseY);
            return;
        }
        ArneStylesConfig config = ConfigManager.get();
        List<StyleDefinition> styles = StyleRegistry.getStyles(current);
        StyleDefinition selected = StyleRegistry.getSelected(current);
        boolean enabled = config.isEnabled(current);
        int headY = top + HEADER_H + 7;

        drawBigText(context, categoryName(current), paneX, headY, 1.3f, C_TEXT);
        if (current == StyleCategory.PERFORMANCE_SETTINGS) {
            drawPerformanceSettings(context, mouseX, mouseY);
            return;
        }

        // Schalter "Aktiv" rechts neben der Ueberschrift
        Text activeLabel = Text.translatable("arnestyles.gui.active");
        int ax2 = paneRight;
        int ax1 = ax2 - this.textRenderer.getWidth(activeLabel) - 34;
        boolean activeHover = inside(mouseX, mouseY, ax1, headY - 2, ax2, headY + 12);
        roundBordered(context, ax1, headY - 2, ax2, headY + 12, activeHover ? C_ACCENT : C_EDGE, activeHover ? C_CARD_HOVER : C_CARD);
        text(context, activeLabel, ax1 + 6, headY + 1, C_TEXT);
        drawSwitch(context, ax2 - 26, headY, enabled, !config.modEnabled);
        hits.add(new Hit(ax1, headY - 2, ax2, headY + 12, this::toggleCurrent));

        if (!config.modEnabled) {
            text(context, Text.translatable("arnestyles.gui.mod_disabled"), paneX, top + HEADER_H + 26, C_DANGER);
        }

        if (current == StyleCategory.ESSENTIALS) {
            drawEssentials(context, mouseX, mouseY);
            return;
        }

        if (config.modEnabled) {
            Text info;
            int infoColor = C_MUTED;
            Text cloud = CloudStyles.status(current);
            if (cloud != null) {
                info = cloud;
                infoColor = C_WARN;
            } else if (shownTintHover != null && TintedTextures.supports(current)) {
                info = shownTintHover;
            } else if (dirty) {
                info = Text.translatable("arnestyles.gui.pending");
                infoColor = C_WARN;
            } else if (current == StyleCategory.PERFORMANCE) {
                info = Text.translatable("arnestyles.gui.performance_hint");
            } else if (current == StyleCategory.SKY) {
                info = Text.translatable("arnestyles.gui.sky_hint");
            } else {
                info = Text.translatable("arnestyles.gui.folder_info",
                    styles.stream().filter(StyleDefinition::builtIn).count(),
                    styles.stream().filter(st -> !st.isVanilla() && !st.builtIn()).count(),
                    current.folderName());
            }
            String infoText = this.textRenderer.trimToWidth(info.getString(), paneRight - paneX);
            text(context, infoText, paneX, top + HEADER_H + 26, infoColor);
        }

        if (current == StyleCategory.SOUNDS) {
            // Lautstaerke aller Sounds aus eingebauten Packs
            drawSliderRow(context, EssentialSlider.PACK_SOUND_VOLUME, paneX, top + HEADER_H + 42, paneRight - paneX,
                config.modEnabled, mouseX, mouseY, null);
        }
        if (TintedTextures.supports(current)) {
            drawTintRows(context, mouseX, mouseY, config.modEnabled);
        }

        int first = cardScroll * columns;
        int last = Math.min(styles.size(), first + cardRows * columns);
        for (int i = first; i < last; i++) {
            int slot = i - first;
            int cx = paneX + (slot % columns) * (cardW + CARD_GAP);
            int cy = cardsTop + (slot / columns) * (CARD_H + CARD_GAP);
            StyleDefinition style = styles.get(i);
            drawCard(context, style, cx, cy, style.id().equals(selected.id()), enabled && config.modEnabled, mouseX, mouseY);
        }

        if (styles.size() == 1 && columns > 1) {
            int textX = paneX + cardW + CARD_GAP + 6;
            int textY = cardsTop + 6;
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("arnestyles.gui.empty", current.folderName()), paneRight - textX)) {
                text(context, line, textX, textY, C_MUTED);
                textY += 10;
            }
        }

        drawScrollMarkers(context);
    }

    private void drawScrollMarkers(DrawContext context) {
        int max = maxCardScroll();
        if (max <= 0) {
            return;
        }
        // Schmale Bildlaufleiste rechts
        int trackX = right - 5;
        int trackTop = cardsTop;
        int trackBottom = cardsBottom;
        context.fill(trackX, trackTop, trackX + 2, trackBottom, C_EDGE);
        int thumbH = Math.max(12, (trackBottom - trackTop) / (max + 1));
        int thumbY = trackTop + Math.round((trackBottom - trackTop - thumbH) * (cardScroll / (float) max));
        context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, C_ACCENT);
    }

    private void drawCard(DrawContext context, StyleDefinition style, int x, int y, boolean selected, boolean active, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, x + cardW, y + CARD_H);
        if (hovered) {
            hoveredStyle = style;
        }
        int border = selected ? C_ACCENT2 : (hovered ? C_EDGE_HOVER : C_EDGE);
        int fill = selected ? GuiTheme.mix(hovered ? C_CARD_HOVER : C_CARD, C_ACCENT2, 0.10f) : hovered ? C_CARD_HOVER : C_CARD;
        roundBordered(context, x, y, x + cardW, y + CARD_H, border, fill);
        if (selected) {
            round(context, x + 1, y + 1, x + cardW - 1, y + 3, C_ACCENT2);
        }

        int px = x + (cardW - PREVIEW) / 2;
        int py = y + 5;
        round(context, px, py, px + PREVIEW, py + PREVIEW, C_FIELD);
        drawPreview(context, style, px, py, PREVIEW);

        String name = this.textRenderer.trimToWidth(style.name().getString(), cardW - 8);
        text(context, name, x + 4, py + PREVIEW + 4, hovered || selected ? C_TEXT : C_TEXT_SOFT);
        if (selected && !active) {
            text(context, Text.translatable("arnestyles.gui.inactive"), x + 4, py + PREVIEW + 14, C_WARN);
        } else {
            String origin = originOf(style);
            text(context, Text.translatable("arnestyles.origin." + origin), x + 4, py + PREVIEW + 14, originColor(origin));
        }
        if (selected && active) {
            // Gruener Haken oben rechts in der Vorschau
            int cx2 = px + PREVIEW - 1;
            round(context, cx2 - 11, py + 1, cx2, py + 12, C_OK);
            text(context, "\u2714", cx2 - 9, py + 3, 0xFF0B1A10);
        }
        hits.add(new Hit(x, y, x + cardW, y + CARD_H, () -> chooseStyle(style)));
    }

    // ---- Performance-Reiter: FPS-Einstellungen ----

    private int performanceLinesVisible() {
        return Math.max(1, (cardsBottom - (top + HEADER_H + 42)) / (ESSENTIAL_ROW_H + 2));
    }

    /**
     * Seite "FPS-Einstellungen": oben die Profile, dann die Regler und die Schalter, je zwei pro Zeile.
     * Alles aendert die echten Minecraft-Optionen und wirkt sofort. Mit dem Mausrad scrollbar.
     */
    private void drawPerformanceSettings(DrawContext context, int mouseX, int mouseY) {
        int w = paneRight - paneX;
        int half = (w - 4) / 2;
        int y0 = top + HEADER_H + 42;
        int lineH = ESSENTIAL_ROW_H + 2;
        int visible = performanceLinesVisible();
        cardScroll = Math.max(0, Math.min(cardScroll, Math.max(0, PERFORMANCE_LINES - visible)));

        Text info = shownTintHover != null ? shownTintHover : Text.translatable("arnestyles.gui.performance_settings_hint");
        text(context, this.textRenderer.trimToWidth(info.getString(), w), paneX, top + HEADER_H + 26, C_MUTED);

        int line = 0;
        // Profile: drei Knoepfe nebeneinander
        if (line >= cardScroll && line < cardScroll + visible) {
            int y = y0 + (line - cardScroll) * lineH;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            PerformanceSettings.Profile[] profiles = PerformanceSettings.Profile.values();
            int bw = (w - 4 * (profiles.length - 1)) / profiles.length;
            for (int i = 0; i < profiles.length; i++) {
                PerformanceSettings.Profile profile = profiles[i];
                int x1 = paneX + i * (bw + 4);
                int x2 = i == profiles.length - 1 ? paneRight : x1 + bw;
                boolean over = inside(mouseX, mouseY, x1, y, x2, y2);
                boolean activeProfile = profile.isActive();
                if (activeProfile) {
                    roundBordered(context, x1, y, x2, y2, C_OK, C_CARD);
                } else {
                    roundBordered(context, x1, y, x2, y2, C_ACCENT, over ? C_ACCENT : C_ACCENT_SOFT);
                }
                String label = this.textRenderer.trimToWidth(Text.translatable(profile.nameKey()).getString(), x2 - x1 - 6);
                text(context, label, x1 + (x2 - x1 - this.textRenderer.getWidth(label)) / 2, y + 5, activeProfile ? C_OK : C_TEXT);
                hits.add(new Hit(x1, y, x2, y2, profile::apply));
                if (over) {
                    tintHover = Text.translatable(profile.descriptionKey());
                }
            }
        }
        line++;

        // Regler, je zwei pro Zeile
        List<SettingSlider> sliders = PerformanceSettings.sliders();
        for (int i = 0; i < sliders.size(); i += 2, line++) {
            if (line < cardScroll || line >= cardScroll + visible) {
                continue;
            }
            int y = y0 + (line - cardScroll) * lineH;
            for (int j = 0; j < 2 && i + j < sliders.size(); j++) {
                SettingSlider slider = sliders.get(i + j);
                int x = j == 0 ? paneX : paneX + half + 4;
                int sw = j == 0 ? half : w - half - 4;
                if (drawSliderRow(context, slider, x, y, sw, true, mouseX, mouseY, null)) {
                    tintHover = Text.translatable(slider.descriptionKey());
                }
            }
        }

        // Schalter, je zwei pro Zeile
        PerformanceSettings.Toggle[] toggles = PerformanceSettings.Toggle.values();
        for (int i = 0; i < toggles.length; i += 2, line++) {
            if (line < cardScroll || line >= cardScroll + visible) {
                continue;
            }
            int y = y0 + (line - cardScroll) * lineH;
            for (int j = 0; j < 2 && i + j < toggles.length; j++) {
                PerformanceSettings.Toggle toggle = toggles[i + j];
                int x = j == 0 ? paneX : paneX + half + 4;
                int rowW = j == 0 ? half : w - half - 4;
                int y2 = y + ESSENTIAL_ROW_H - 2;
                boolean over = inside(mouseX, mouseY, x, y, x + rowW, y2);
                round(context, x, y, x + rowW, y2, over ? C_CARD_HOVER : C_CARD);
                text(context, this.textRenderer.trimToWidth(Text.translatable(toggle.nameKey()).getString(), rowW - 40), x + 8, y + 5, C_TEXT);
                drawSwitch(context, x + rowW - 28, y + 3, toggle.isOn(), false);
                hits.add(new Hit(x, y, x + rowW, y2, toggle::toggle));
                if (over) {
                    tintHover = Text.translatable(toggle.descriptionKey());
                }
            }
        }
        drawScrollMarkers(context);
    }

    // ---- Eigene Farbe (Stone, Deepslate, Gras, Feuer) ----

    /**
     * Zwei Zeilen ueber den Karten: Schalter "Eigene Farbe" mit Farbfeld und Farbspektrum (Farbton),
     * darunter Saettigung und Helligkeit. Alle Karten zeigen die Farbe sofort (Live-Vorschau).
     */
    private void drawTintRows(DrawContext context, int mouseX, int mouseY, boolean modEnabled) {
        ArneStylesConfig.CategoryTint tint = TintedTextures.get(current);
        int w = paneRight - paneX;
        int half = (w - 4) / 2;
        int y = top + HEADER_H + 42;
        int y2 = y + ESSENTIAL_ROW_H - 2;
        boolean active = modEnabled && tint.on;

        // Schalter mit Farbfeld
        boolean over = inside(mouseX, mouseY, paneX, y, paneX + half, y2);
        round(context, paneX, y, paneX + half, y2, over ? C_CARD_HOVER : C_CARD);
        text(context, Text.translatable("arnestyles.tint.on"), paneX + 8, y + 5, C_TEXT);
        int swatchX = paneX + half - 46;
        round(context, swatchX - 1, y + 2, swatchX + 13, y2 - 2, C_EDGE);
        round(context, swatchX, y + 3, swatchX + 12, y2 - 3, 0xFF000000 | TintedTextures.color(tint));
        drawSwitch(context, paneX + half - 28, y + 3, tint.on, tint.on && !modEnabled);
        hits.add(new Hit(paneX, y, paneX + half, y2, () -> {
            tint.on = !tint.on;
            ConfigManager.save();
            markDirty();
        }));
        if (over) {
            tintHover = Text.translatable(current == StyleCategory.STONE || current == StyleCategory.DEEPSLATE
                ? "arnestyles.tint.on.desc_stone" : "arnestyles.tint.on.desc");
        }

        drawTintSlider(context, new TintSlider(current, TintSlider.Part.HUE), paneX + half + 4, y, w - half - 4, active, mouseX, mouseY);
        int y3 = y + ESSENTIAL_ROW_H + 2;
        drawTintSlider(context, new TintSlider(current, TintSlider.Part.SATURATION), paneX, y3, half, active, mouseX, mouseY);
        drawTintSlider(context, new TintSlider(current, TintSlider.Part.BRIGHTNESS), paneX + half + 4, y3, w - half - 4, active, mouseX, mouseY);

        if (current == StyleCategory.ORES) {
            // Erze: nur die Flecken faerben, und Stein-Hintergrund an die Stone-/Deepslate-Farbe anpassen
            int y4 = y3 + ESSENTIAL_ROW_H + 2;
            drawTintSwitch(context, paneX, y4, half, Text.translatable("arnestyles.tint.ore_spots"), tint.oreSpots,
                "arnestyles.tint.ore_spots.desc", mouseX, mouseY, () -> tint.oreSpots = !tint.oreSpots);
            ArneStylesConfig config = ConfigManager.get();
            drawTintSwitch(context, paneX + half + 4, y4, w - half - 4, Text.translatable("arnestyles.tint.ore_follow"), config.oresFollowStone,
                "arnestyles.tint.ore_follow.desc", mouseX, mouseY, () -> config.oresFollowStone = !config.oresFollowStone);
        }
    }

    /** Zeile mit Namen und Schalter im Farb-Bereich. */
    private void drawTintSwitch(DrawContext context, int x, int y, int w, Text label, boolean on, String descKey, int mouseX, int mouseY,
                                Runnable toggle) {
        int y2 = y + ESSENTIAL_ROW_H - 2;
        boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
        round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
        text(context, this.textRenderer.trimToWidth(label.getString(), w - 40), x + 8, y + 5, C_TEXT);
        drawSwitch(context, x + w - 28, y + 3, on, false);
        hits.add(new Hit(x, y, x + w, y2, () -> {
            toggle.run();
            ConfigManager.save();
            markDirty();
        }));
        if (over) {
            tintHover = Text.translatable(descKey);
        }
    }

    /**
     * Kompakte Regler-Zeile fuer den Farb-Bereich: der Balken beginnt direkt hinter dem Namen und nutzt den ganzen
     * freien Platz. Beim Farbton ist der Balken ein Farbspektrum, bei Saettigung und Helligkeit ein Verlauf in der Farbe.
     */
    private void drawTintSlider(DrawContext context, TintSlider slider, int x, int y, int w, boolean active, int mouseX, int mouseY) {
        int y2 = y + ESSENTIAL_ROW_H - 2;
        boolean hovered = inside(mouseX, mouseY, x, y, x + w, y2);
        round(context, x, y, x + w, y2, hovered ? C_CARD_HOVER : C_CARD);
        Text name = Text.translatable(slider.nameKey());
        int barX1 = x + 8 + this.textRenderer.getWidth(name) + 8;
        int barX2 = x + w - this.textRenderer.getWidth("360°") - 12;
        text(context, name, x + 8, y + 5, C_TEXT);
        ArneStylesConfig.CategoryTint tint = slider.current();
        int barY1 = y + 5;
        int barY2 = y2 - 5;
        int steps = Math.max(1, barX2 - barX1);
        float shade = active ? 1.0f : 0.55f;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            int rgb = switch (slider.part()) {
                case HUE -> net.minecraft.util.math.MathHelper.hsvToRgb(t, Math.max(0.35f, tint.saturation), shade);
                case SATURATION -> net.minecraft.util.math.MathHelper.hsvToRgb(tint.hue, t, shade);
                case BRIGHTNESS -> net.minecraft.util.math.MathHelper.hsvToRgb(tint.hue, tint.saturation, (0.15f + 0.85f * t) * shade);
            };
            context.fill(barX1 + i, barY1, barX1 + i + 1, barY2, 0xFF000000 | rgb);
        }
        int knob = barX1 + Math.round(steps * slider.fraction());
        round(context, knob - 2, y + 2, knob + 2, y2 - 2, C_TEXT);
        String value = slider.display();
        text(context, value, x + w - this.textRenderer.getWidth(value) - 6, y + 5, active ? C_TEXT : C_MUTED);
        sliderHits.add(new SliderHit(barX1 - 4, y, barX2 + 4, y2, barX1, barX2, slider));
        if (hovered) {
            tintHover = Text.translatable(slider.descriptionKey());
        }
    }

    // ---- Customize-Tab: jede einzelne Textur einfaerben ----

    private boolean customizing() {
        return KEY_CUSTOMIZE_TAB.equals(currentTab()) && !presetsOpen && !modsOpen && !shadersOpen && !themesOpen && !importOpen;
    }

    private static boolean grassLike(String asset) {
        String file = asset.substring(asset.lastIndexOf('/') + 1);
        return file.startsWith("grass_block_top") || file.startsWith("grass_block_side_overlay") || file.startsWith("short_grass")
            || file.startsWith("tall_grass") || file.startsWith("fern") || file.startsWith("large_fern");
    }

    /**
     * Links alle Texturen des gewaehlten Styles, rechts Vorschau, Farbkreis und Einstellungen der gewaehlten Textur.
     * Die Farbe gilt nur fuer diese eine Textur und geht vor der Farbe der Kategorie.
     */
    private void drawCustomize(DrawContext context, int mouseX, int mouseY) {
        List<StyleDefinition> styles = StyleRegistry.getStyles(current);
        if (customizeCategory != current || customizeStyle < 0 || customizeStyle >= styles.size()) {
            customizeCategory = current;
            customizeStyle = Math.max(0, styles.indexOf(StyleRegistry.getSelected(current)));
            customizeAsset = null;
            cardScroll = 0;
        }
        StyleDefinition style = styles.get(customizeStyle);
        String styleKey = TintedTextures.styleKey(style);
        Path root = TintedTextures.previewRoot(style);
        // Zustaende (z. B. Anchor-Ladestufen 0 bis 4) sind eine Kachel, ihre Farbe gilt fuer alle
        // Einfach: eine Kachel pro Textur (Farbe fuer alle Zustaende), Erweitert: jeder Zustand einzeln
        boolean advanced = ConfigManager.get().customizeAdvanced;
        List<String> allTextures = TintedTextures.textures(style);
        List<String> textures = advanced ? allTextures : TintedTextures.groupStates(allTextures);
        if (customizeAsset != null && !textures.contains(customizeAsset)) {
            String group = TintedTextures.stateGroup(customizeAsset);
            customizeAsset = textures.stream().filter(t -> TintedTextures.stateGroup(t).equals(group)).findFirst().orElse(null);
        }
        if (customizeAsset == null) {
            customizeAsset = textures.isEmpty() ? null : textures.get(0);
        }

        // Kopfzeile: Kategorie und Style-Auswahl
        int headY = top + HEADER_H + 7;
        drawBigText(context, categoryName(current), paneX, headY, 1.3f, C_TEXT);
        int count = styles.size();
        int x = drawPillRight(context, paneRight, headY - 2, Text.literal(">"), C_TEXT, mouseX, mouseY, () -> {
            customizeStyle = (customizeStyle + 1) % count;
            customizeAsset = null;
            cardScroll = 0;
        });
        String styleName = this.textRenderer.trimToWidth(style.name().getString(), 110);
        int nameW = this.textRenderer.getWidth(styleName);
        text(context, styleName, x - 6 - nameW, headY + 1, C_ACCENT);
        int leftEnd = drawPillRight(context, x - 12 - nameW, headY - 2, Text.literal("<"), C_TEXT, mouseX, mouseY, () -> {
            customizeStyle = (customizeStyle - 1 + count) % count;
            customizeAsset = null;
            cardScroll = 0;
        });

        int infoY = top + HEADER_H + 26;
        // Einfach/Erweitert umschalten: in der Kopfzeile, sonst rechts neben dem Info-Text
        Text modeLabel = Text.translatable(advanced ? "arnestyles.customize.mode.advanced" : "arnestyles.customize.mode.simple");
        int modeW = this.textRenderer.getWidth(modeLabel) + 12;
        int titleEnd = paneX + Math.round(this.textRenderer.getWidth(categoryName(current)) * 1.3f) + 8;
        boolean modeInHeader = leftEnd - 8 - modeW >= titleEnd;
        int modeX2 = modeInHeader ? leftEnd - 8 : paneRight;
        int modeY = modeInHeader ? headY - 2 : infoY - 3;
        drawPillRight(context, modeX2, modeY, modeLabel, C_ACCENT, mouseX, mouseY, () -> {
            ConfigManager.get().customizeAdvanced = !ConfigManager.get().customizeAdvanced;
            ConfigManager.save();
            cardScroll = 0;
        });
        Text modeHover = inside(mouseX, mouseY, modeX2 - modeW, modeY, modeX2, modeY + 14)
            ? Text.translatable(advanced ? "arnestyles.customize.mode.advanced.desc" : "arnestyles.customize.mode.simple.desc") : null;
        int infoW = modeInHeader ? paneRight - paneX : paneRight - modeW - 6 - paneX;
        int y0 = top + HEADER_H + 42;
        int bottomY = cardsBottom;
        int paneW = paneRight - paneX;
        if (customizeAsset == null) {
            text(context, Text.translatable("arnestyles.customize.empty"), paneX, infoY, C_MUTED);
            return;
        }

        // Links: Texturen als Kacheln
        int gridW = Math.max(70, paneW * 42 / 100);
        int step = 23;
        int cols = Math.max(1, gridW / step);
        customizeVisibleRows = Math.max(1, (bottomY - y0 + 3) / step);
        customizeTotalRows = (textures.size() + cols - 1) / cols;
        cardScroll = Math.max(0, Math.min(Math.max(0, customizeTotalRows - customizeVisibleRows), cardScroll));
        Text hovered = modeHover != null ? modeHover : shownTintHover;
        int first = cardScroll * cols;
        int last = Math.min(textures.size(), first + customizeVisibleRows * cols);
        for (int i = first; i < last; i++) {
            String asset = textures.get(i);
            int tx = paneX + ((i - first) % cols) * step;
            int ty = y0 + ((i - first) / cols) * step;
            boolean selected = asset.equals(customizeAsset);
            boolean over = inside(mouseX, mouseY, tx, ty, tx + 20, ty + 20);
            roundBordered(context, tx, ty, tx + 20, ty + 20, selected ? C_ACCENT : over ? C_EDGE_HOVER : C_EDGE, C_FIELD);
            String tileKey = asset;
            PreviewTextures.Entry entry = PreviewTextures.colored(current, styleKey, root, asset,
                () -> PreviewTextures.plain(current.name() + "/" + styleKey, root, tileKey));
            if (entry != null) {
                boolean colored = !TintedTextures.signature(current, styleKey, asset).isEmpty();
                drawTile(context, entry, tx + 2, ty + 2, 16, !colored && grassLike(asset) ? GRASS_TINT : 0xFFFFFFFF);
            }
            if (TintedTextures.textureTint(current, styleKey, asset) != null) {
                context.fill(tx + 15, ty + 1, tx + 19, ty + 5, C_ACCENT);
            }
            int states = advanced ? 1 : TintedTextures.stateCount(allTextures, asset);
            if (states > 1) {
                // Kleine Zahl unten rechts: so viele Zustaende stecken in der Kachel
                String n = String.valueOf(states);
                int nx = tx + 20 - this.textRenderer.getWidth(n);
                context.fill(nx - 1, ty + 12, tx + 20, ty + 20, 0xC0000000);
                text(context, n, nx, ty + 12, 0xFFFFFFFF);
            }
            if (over) {
                String group = TintedTextures.stateGroup(asset);
                String file = group.substring(group.lastIndexOf('/') + 1);
                hovered = states > 1 ? Text.translatable("arnestyles.customize.states", file, states)
                    : Text.literal(asset.substring(asset.lastIndexOf('/') + 1));
            }
            hits.add(new Hit(tx, ty, tx + 20, ty + 20, () -> customizeAsset = tileKey));
        }

        // Rechts: Vorschau, Farbkreis, Einstellungen
        String asset = customizeAsset;
        String key = advanced ? TintedTextures.stateKey(current, styleKey, asset) : TintedTextures.textureKey(current, styleKey, asset);
        ArneStylesConfig.CategoryTint tint = advanced ? TintedTextures.editStateTint(current, styleKey, asset)
            : TintedTextures.editTextureTint(current, styleKey, asset);
        int ex = paneX + cols * step + 8;
        int ew = paneRight - ex;
        int rowStep = ESSENTIAL_ROW_H + 2;
        // Farbkreis links oben, so gross wie Platz ist (mindestens 32, hoechstens 60 Pixel)
        int wheelSize = Math.max(32, Math.min(60, Math.min(ew / 2, bottomY - y0 - 2 * rowStep - 4)));
        wheelX = ex;
        wheelY = y0;
        wheelD = wheelSize;
        wheelApply = (hue, saturation) -> {
            ArneStylesConfig.CategoryTint edit = ConfigManager.get().textureTints.computeIfAbsent(key, k -> new ArneStylesConfig.CategoryTint());
            edit.hue = hue;
            edit.saturation = saturation;
            edit.on = true;
            if (!advanced) {
                // Einfach: die ganze Textur bekommt die Farbe, auch Zustaende mit eigener Farbe
                TintedTextures.clearStateTints(current, styleKey, asset);
            }
            markDirty();
        };
        drawWheel(context, tint);

        // Rechts neben dem Farbkreis: eigene Farbe an/aus und Farbton drehen
        int sx = ex + wheelSize + 6;
        int sw = paneRight - sx;
        drawTintSwitch(context, sx, y0, sw, Text.translatable("arnestyles.customize.own"), tint.on, "arnestyles.customize.own.desc",
            mouseX, mouseY, () -> tint.on = !tint.on);
        drawTintSwitch(context, sx, y0 + rowStep, sw, Text.translatable("arnestyles.customize.shift"), tint.mode == TintedTextures.MODE_SHIFT,
            "arnestyles.customize.shift.desc", mouseX, mouseY, () -> {
                boolean shift = tint.mode != TintedTextures.MODE_SHIFT;
                tint.mode = shift ? TintedTextures.MODE_SHIFT : TintedTextures.MODE_REPLACE;
                // Neutral starten: beim Drehen 0 Grad und 100 %, beim Ersetzen die Standardfarbe
                ArneStylesConfig.CategoryTint defaults = new ArneStylesConfig.CategoryTint();
                tint.hue = shift ? 0.0f : defaults.hue;
                tint.saturation = shift ? 0.5f : defaults.saturation;
                tint.brightness = 0.5f;
                tint.on = true;
            });

        // Regenbogen: nur fuer Texturen, die Minecraft animieren kann (Items, Bloecke, HUD)
        boolean canRainbow = TintedTextures.animatable(asset);
        if (canRainbow) {
            drawTintSwitch(context, sx, y0 + 2 * rowStep, sw, Text.translatable("arnestyles.customize.rainbow"), tint.rainbow,
                "arnestyles.customize.rainbow.desc", mouseX, mouseY, () -> {
                    tint.rainbow = !tint.rainbow;
                    tint.on = true;
                    if (!advanced) {
                        TintedTextures.clearStateTints(current, styleKey, asset);
                    }
                });
        }

        // Darunter: Helligkeit, dann (bei Erzen) Nur-Flecken und die Knoepfe
        int ry = y0 + Math.max(wheelSize, (canRainbow ? 3 : 2) * rowStep) + 4;
        drawTintSlider(context, new TintSlider(current, TintSlider.Part.BRIGHTNESS, key), ex, ry, ew, tint.on, mouseX, mouseY);
        ry += rowStep;
        int bx = drawPillRight(context, paneRight, ry + 2, Text.translatable("arnestyles.customize.all"), C_TEXT, mouseX, mouseY, () -> {
            for (String other : textures) {
                ArneStylesConfig.CategoryTint copy = tint.copy();
                copy.on = true;
                if (advanced) {
                    ConfigManager.get().textureTints.put(TintedTextures.stateKey(current, styleKey, other), copy);
                } else {
                    TintedTextures.clearStateTints(current, styleKey, other);
                    ConfigManager.get().textureTints.put(TintedTextures.textureKey(current, styleKey, other), copy);
                }
            }
            ConfigManager.save();
            markDirty();
        });
        bx = drawPillRight(context, bx - 4, ry + 2, Text.translatable("arnestyles.customize.reset"), C_MUTED, mouseX, mouseY, () -> {
            if (advanced) {
                TintedTextures.clearStateTint(current, styleKey, asset);
            } else {
                TintedTextures.clearTextureTint(current, styleKey, asset);
            }
            ConfigManager.save();
            markDirty();
        });
        // Zeichnen: neben den Knoepfen, wenn Platz ist, sonst in einer eigenen Zeile darunter
        Text drawLabel = Text.translatable("arnestyles.draw.open");
        int drawRowExtra = 0;
        if (bx - 4 - (this.textRenderer.getWidth(drawLabel) + 12) >= ex) {
            bx = drawPillRight(context, bx - 4, ry + 2, drawLabel, C_ACCENT, mouseX, mouseY, () -> openDrawEditor(style, asset));
        } else {
            drawPillRight(context, paneRight, ry + rowStep + 2, drawLabel, C_ACCENT, mouseX, mouseY, () -> openDrawEditor(style, asset));
            drawRowExtra = rowStep;
        }
        if (current == StyleCategory.ORES && asset.contains("_ore") && bx - 4 - ex >= 60) {
            drawTintSwitch(context, ex, ry, bx - 4 - ex, Text.translatable("arnestyles.tint.ore_spots"), tint.oreSpots, "arnestyles.tint.ore_spots.desc",
                mouseX, mouseY, () -> tint.oreSpots = !tint.oreSpots);
        }

        // Vorschau: links das Pack wie auf den Karten, rechts die gewaehlte Textur gross (beides mit der Farbe)
        // Platz rechts unter den Knoepfen oder links unter den Kacheln, je nachdem wo mehr frei ist
        int rightY = ry + rowStep + 4 + drawRowExtra;
        int rightSize = Math.min(bottomY - rightY, Math.min((ew - 6) / 2, 80));
        int usedRows = Math.max(1, Math.min(customizeVisibleRows, customizeTotalRows - cardScroll));
        int leftY = y0 + usedRows * step + 4;
        int leftSize = Math.min(bottomY - leftY, Math.min((cols * step - 9) / 2, 80));
        boolean left = leftSize > rightSize;
        int px = left ? paneX : ex;
        int py = left ? leftY : rightY;
        int size = left ? leftSize : rightSize;
        if (size >= 28) {
            round(context, px, py, px + size, py + size, C_FIELD);
            drawPreview(context, style, px, py, size);
            int tx = px + size + 6;
            round(context, tx, py, tx + size, py + size, C_FIELD);
            PreviewTextures.Entry big = PreviewTextures.colored(current, styleKey, root, asset,
                () -> PreviewTextures.plain(current.name() + "/" + styleKey, root, asset));
            if (big != null) {
                boolean colored = !TintedTextures.signature(current, styleKey, asset).isEmpty();
                drawTile(context, big, tx + 4, py + 4, size - 8, !colored && grassLike(asset) ? GRASS_TINT : 0xFFFFFFFF);
            }
            if (inside(mouseX, mouseY, px, py, px + size, py + size)) {
                hovered = Text.translatable("arnestyles.customize.preview_pack", style.name());
            } else if (inside(mouseX, mouseY, tx, py, tx + size, py + size)) {
                hovered = Text.translatable("arnestyles.customize.preview_texture");
            }
            hits.add(new Hit(tx, py, tx + size, py + size, () -> openDrawEditor(style, asset)));
        }

        if (drawError != null && System.currentTimeMillis() - drawErrorAt > 4000) {
            drawError = null;
        }
        Text info = drawError != null ? drawError : hovered != null ? hovered
            : Text.translatable("arnestyles.customize.info", asset.substring(asset.lastIndexOf('/') + 1));
        text(context, this.textRenderer.trimToWidth(info.getString(), infoW), paneX, infoY, C_MUTED);
    }

    /** Runder Farbkreis: Winkel = Farbton, Abstand zur Mitte = Saettigung. Die Markierung zeigt die aktuelle Farbe. */
    private void drawWheel(DrawContext context, ArneStylesConfig.CategoryTint tint) {
        float shade = tint.mode == TintedTextures.MODE_SHIFT ? 1.0f : 0.35f + 0.65f * tint.brightness;
        drawWheel(context, tint.hue, tint.saturation, shade, tint.on);
    }

    /** Farbkreis an (wheelX, wheelY) mit Durchmesser wheelD; shade dunkelt ihn ab (Helligkeit). */
    private void drawWheel(DrawContext context, float hue, float saturation, float shade, boolean on) {
        Identifier texture = wheelTexture();
        int v = Math.round(255 * Math.max(0.0f, Math.min(1.0f, shade)));
        int color = 0xFF000000 | (v << 16) | (v << 8) | v;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, wheelX, wheelY, 0.0f, 0.0f, wheelD, wheelD, WHEEL_SIZE, WHEEL_SIZE,
            WHEEL_SIZE, WHEEL_SIZE, color);
        double angle = hue * Math.PI * 2.0;
        double radius = Math.min(1.0, saturation) * (wheelD / 2.0 - 1);
        int mx = (int) Math.round(wheelX + wheelD / 2.0 + Math.cos(angle) * radius);
        int my = (int) Math.round(wheelY + wheelD / 2.0 + Math.sin(angle) * radius);
        context.fill(mx - 3, my - 3, mx + 3, my + 3, 0xFF000000);
        context.fill(mx - 2, my - 2, mx + 2, my + 2, on ? 0xFFFFFFFF : C_MUTED);
    }

    private static final int WHEEL_SIZE = 128;

    /** Farbkreis-Bild, einmal erzeugt. */
    private static Identifier wheelTexture() {
        if (wheelTexture != null) {
            return wheelTexture;
        }
        net.minecraft.client.texture.NativeImage image = new net.minecraft.client.texture.NativeImage(WHEEL_SIZE, WHEEL_SIZE, true);
        double r = WHEEL_SIZE / 2.0;
        for (int y = 0; y < WHEEL_SIZE; y++) {
            for (int x = 0; x < WHEEL_SIZE; x++) {
                double dx = x + 0.5 - r;
                double dy = y + 0.5 - r;
                double dist = Math.sqrt(dx * dx + dy * dy) / r;
                if (dist > 1.0) {
                    image.setColorArgb(x, y, 0);
                    continue;
                }
                float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
                int rgb = net.minecraft.util.math.MathHelper.hsvToRgb(hue, (float) dist, 1.0f);
                // Weicher Rand gegen Treppchen
                int alpha = dist > 0.97 ? (int) Math.round(255 * (1.0 - dist) / 0.03) : 255;
                image.setColorArgb(x, y, (alpha << 24) | (rgb & 0xFFFFFF));
            }
        }
        Identifier id = Identifier.of(com.arnestyles.ArneStyles.MOD_ID, "customize_wheel");
        net.minecraft.client.MinecraftClient.getInstance().getTextureManager()
            .registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(() -> "arnestyles wheel", image));
        wheelTexture = id;
        return id;
    }

    private boolean insideWheel(double x, double y) {
        double dx = x - (wheelX + wheelD / 2.0);
        double dy = y - (wheelY + wheelD / 2.0);
        return dx * dx + dy * dy <= (wheelD / 2.0 + 3) * (wheelD / 2.0 + 3);
    }

    /** Klick oder Ziehen im Farbkreis: Farbton aus dem Winkel, Saettigung aus dem Abstand zur Mitte. */
    private void applyWheel(double x, double y) {
        if (wheelApply == null) {
            return;
        }
        double dx = x - (wheelX + wheelD / 2.0);
        double dy = y - (wheelY + wheelD / 2.0);
        double dist = Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / (wheelD / 2.0));
        wheelApply.accept((float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0), (float) dist);
    }

    // ---- Gemeinsame Bausteine ----

    /** Regler-Zeile (Name, Balken, Wert). Gibt true zurueck, wenn die Maus darueber ist. */
    private boolean drawSliderRow(DrawContext context, SettingSlider slider, int x, int y, int w, boolean active, int mouseX, int mouseY,
                                  @Nullable Text label) {
        int y2 = y + ESSENTIAL_ROW_H - 2;
        boolean hovered = inside(mouseX, mouseY, x, y, x + w, y2);
        round(context, x, y, x + w, y2, hovered ? C_CARD_HOVER : C_CARD);
        String value = slider.display();
        int valueW = this.textRenderer.getWidth("200%");
        int barX1 = x + Math.max(96, w * 46 / 100);
        int barX2 = x + w - valueW - 12;
        Text name = label != null ? label : Text.translatable(slider.nameKey());
        text(context, this.textRenderer.trimToWidth(name.getString(), barX1 - x - 14), x + 8, y + 5, C_TEXT);
        int barY = y + 7;
        round(context, barX1, barY, barX2, barY + 4, C_FIELD);
        int knob = barX1 + Math.round((barX2 - barX1) * Math.max(0.0f, Math.min(1.0f, slider.fraction())));
        if (knob > barX1 + 1) {
            round(context, barX1, barY, knob, barY + 4, active ? C_ACCENT : C_OFF);
        }
        round(context, knob - 3, y + 3, knob + 3, y2 - 3, C_TEXT);
        text(context, value, x + w - this.textRenderer.getWidth(value) - 6, y + 5, active ? C_TEXT : C_MUTED);
        sliderHits.add(new SliderHit(barX1 - 4, y, barX2 + 4, y2, barX1, barX2, slider));
        return hovered;
    }

    private static void drawArrowUp(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 1, y + 1, color);
        context.fill(x - 1, y + 1, x + 2, y + 2, color);
        context.fill(x - 2, y + 2, x + 3, y + 3, color);
    }

    private static void drawArrowDown(DrawContext context, int x, int y, int color) {
        context.fill(x - 2, y, x + 3, y + 1, color);
        context.fill(x - 1, y + 1, x + 2, y + 2, color);
        context.fill(x, y + 2, x + 1, y + 3, color);
    }

    /** Seitenleisten-Zeile im Sortier-Modus: Name und Pfeile zum Verschieben. */
    private void drawSortRow(DrawContext context, Row row, int x1, int y, int x2, int y2, int mouseX, int mouseY) {
        round(context, x1, y, x2, y2, C_CARD);
        Text name;
        String icon;
        if (row.presets()) {
            name = Text.translatable("arnestyles.gui.presets");
            icon = "textures/item/writable_book.png";
        } else if (row.mods()) {
            name = Text.translatable("arnestyles.gui.mods");
            icon = "textures/item/command_block_minecart.png";
        } else if (isShaders(row)) {
            name = Text.translatable("arnestyles.gui.shaders");
            icon = "textures/item/spyglass.png";
        } else if (KEY_THEMES.equals(row.key())) {
            name = Text.translatable("arnestyles.gui.themes");
            icon = THEME_ICON;
        } else if (KEY_IMPORT.equals(row.key())) {
            name = Text.translatable("arnestyles.gui.import");
            icon = IMPORT_ICON;
        } else if (row.group() != null) {
            name = Text.translatable(row.group().nameKey());
            icon = row.group().icon();
        } else {
            name = categoryName(row.category());
            icon = row.category().previewAssets().isEmpty() ? null : row.category().previewAssets().get(0);
        }
        if (icon != null) {
            drawIconAsset(context, icon, x1 + 3, y + 1);
        }
        text(context, this.textRenderer.trimToWidth(name.getString(), x2 - x1 - 50), x1 + 22, y + 4, C_TEXT);
        String key = row.key();
        int bx = x2 - 22;
        boolean upHover = inside(mouseX, mouseY, bx, y, bx + 10, y2);
        boolean downHover = inside(mouseX, mouseY, bx + 11, y, bx + 21, y2);
        drawArrowUp(context, bx + 4, y + 6, upHover ? C_ACCENT : C_MUTED);
        drawArrowDown(context, bx + 15, y + 6, downHover ? C_ACCENT : C_MUTED);
        hits.add(new Hit(bx, y, bx + 10, y2, () -> moveSidebarEntry(key, -1)));
        hits.add(new Hit(bx + 11, y, bx + 21, y2, () -> moveSidebarEntry(key, 1)));
    }

    /** HUD-Editor oeffnen. Geht nur in einer Welt, weil das HUD sonst nicht gezeichnet wird. */
    private volatile boolean exporting;

    /** Aktuelles Setup als Texture-Pack (.zip) in den resourcepacks-Ordner legen und den Ordner oeffnen. */
    private void exportPack() {
        if (exporting || this.client == null) {
            return;
        }
        exporting = true;
        java.nio.file.Path dir = this.client.getResourcePackDir();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                java.nio.file.Path out = com.arnestyles.client.style.PackExporter.export(dir);
                presetMessage = Text.translatable("arnestyles.export.done", out.getFileName().toString()).getString();
                presetMessageColor = C_OK;
                Util.getOperatingSystem().open(dir);
            } catch (java.io.IOException | RuntimeException e) {
                presetMessage = Text.translatable("arnestyles.export.failed", String.valueOf(e.getMessage())).getString();
                presetMessageColor = C_DANGER;
            } finally {
                exporting = false;
            }
        });
    }

    private void openHandEditor() {
        if (this.client == null) {
            return;
        }
        if (this.client.world == null) {
            hudEditorHint = true;
            return;
        }
        openChild(new HandEditorScreen(this));
    }

    /** Was eine Aktions-Zeile im Mods-Menue beim Klick tut. */
    private Runnable actionFor(String action) {
        return switch (action) {
            case "arnestyles.mod.hand_editor" -> this::openHandEditor;
            case "arnestyles.mod.customhud_editor" -> () -> com.arnestyles.client.mods.CustomHudBridge.openEditor(this);
            case "arnestyles.mod.customhud_folder" -> com.arnestyles.client.mods.CustomHudBridge::openProfileFolder;
            case "arnestyles.mod.crosshair_editor" -> () -> openChild(new CrosshairEditorScreen(this));
            case "arnestyles.mod.hud_editor" -> () -> {
                com.arnestyles.client.mods.HudLayout.showVanilla = false;
                openHudEditor();
            };
            case "arnestyles.mod.vanilla_hud_editor" -> () -> {
                com.arnestyles.client.mods.HudLayout.showVanilla = true;
                openHudEditor();
            };
            case "arnestyles.mod.show_intro" -> () -> openChild(new IntroScreen(this));
            case "arnestyles.mod.spotify_keys" -> () -> {
                if (this.client != null) {
                    openChild(new net.minecraft.client.gui.screen.option.KeybindsScreen(this, this.client.options));
                }
            };
            default -> this::openHudEditor;
        };
    }

    private void openHudEditor() {
        if (this.client == null) {
            return;
        }
        if (this.client.world == null) {
            hudEditorHint = true;
            return;
        }
        this.client.setScreen(new HudEditorScreen(this));
    }

    // ---- Shader ----

    private void openShaders() {
        shadersOpen = true;
        themesOpen = false;
        importOpen = false;
        presetsOpen = false;
        modsOpen = false;
        cardScroll = 0;
        ShaderPacks.ensureDir();
    }

    /** Eine Zeile der Shader-Seite. */
    private record ShaderRow(int kind, Text label, @Nullable String key) {
        static final int SECTION = 0;
        static final int ARNE = 1;
        static final int IRIS_TOGGLE = 2;
        static final int IRIS_PACK = 3;
        static final int NOTE = 4;
        /** Fester Hinweis in Orange (was die ArneTweaks-Shader koennen und was nicht). */
        static final int HINT = 5;
    }

    private static List<ShaderRow> shaderRows() {
        List<ShaderRow> rows = new ArrayList<>();
        rows.add(new ShaderRow(ShaderRow.SECTION, Text.translatable("arnestyles.gui.shaders_section_arne"), null));
        rows.add(new ShaderRow(ShaderRow.HINT, Text.translatable("arnestyles.gui.shaders_hint_1"), "arnestyles.gui.shaders_hint.desc"));
        rows.add(new ShaderRow(ShaderRow.HINT, Text.translatable("arnestyles.gui.shaders_hint_2"), "arnestyles.gui.shaders_hint.desc"));
        rows.add(new ShaderRow(ShaderRow.ARNE, Text.translatable("arnestyles.gui.shaders_off"), ""));
        for (ArneShaders.Entry entry : ArneShaders.list()) {
            rows.add(new ShaderRow(ShaderRow.ARNE, entry.name(), entry.key()));
        }
        rows.add(new ShaderRow(ShaderRow.SECTION, Text.translatable("arnestyles.gui.shaders_section_iris"), null));
        if (!ShaderPacks.irisLoaded()) {
            rows.add(new ShaderRow(ShaderRow.NOTE, Text.translatable("arnestyles.gui.shaders_no_iris"), "arnestyles.gui.shaders_no_iris.desc"));
            return rows;
        }
        List<String> packs = ShaderPacks.list();
        if (packs.isEmpty()) {
            rows.add(new ShaderRow(ShaderRow.NOTE, Text.translatable("arnestyles.gui.shaders_empty"), "arnestyles.gui.shaders_empty"));
            return rows;
        }
        rows.add(new ShaderRow(ShaderRow.IRIS_TOGGLE, Text.empty(), null));
        for (String pack : packs) {
            String label = pack.toLowerCase(Locale.ROOT).endsWith(".zip") ? pack.substring(0, pack.length() - 4) : pack;
            rows.add(new ShaderRow(ShaderRow.IRIS_PACK, Text.literal(label), pack));
        }
        return rows;
    }

    /**
     * Shader-Seite. Oben die ArneTweaks-Shader (eingebaut und eigene Ordner mit shader.fsh), die ohne Iris laufen.
     * Darunter normale Shader-Packs (.zip), die nur mit Iris gehen.
     */
    private void drawShaders(DrawContext context, int mouseX, int mouseY) {
        int headY = top + HEADER_H + 7;
        drawBigText(context, Text.translatable("arnestyles.gui.shaders"), paneX, headY, 1.3f, C_TEXT);
        int w = paneRight - paneX;
        int infoY = top + HEADER_H + 26;
        int listTopY = top + HEADER_H + 42;

        String arneError = ArneShaders.lastError();
        String irisError = ShaderPacks.irisLoaded() ? ShaderPacks.lastError() : null;
        Text info;
        int infoColor = C_DANGER;
        if (arneError != null) {
            info = Text.translatable("arnestyles.gui.shaders_error", arneError);
        } else if (irisError != null) {
            info = Text.translatable("arnestyles.gui.shaders_error", irisError);
        } else {
            info = Text.translatable("arnestyles.gui.shaders_info");
            infoColor = C_MUTED;
        }

        List<ShaderRow> rows = shaderRows();
        int visible = Math.max(1, (cardsBottom - listTopY + 2) / ESSENTIAL_ROW_H);
        cardScroll = Math.max(0, Math.min(Math.max(0, rows.size() - visible), cardScroll));
        String selected = ArneShaders.selected();
        String irisCurrent = ShaderPacks.irisLoaded() ? ShaderPacks.current() : null;
        boolean irisOn = ShaderPacks.irisLoaded() && ShaderPacks.enabled();
        Text hovered = null;

        for (int i = cardScroll; i < Math.min(rows.size(), cardScroll + visible); i++) {
            ShaderRow row = rows.get(i);
            int y = listTopY + (i - cardScroll) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, paneX, y, paneX + w, y2);
            switch (row.kind()) {
                case ShaderRow.SECTION -> {
                    int textEnd = paneX + 2 + this.textRenderer.getWidth(row.label());
                    text(context, row.label(), paneX + 2, y + 6, C_ACCENT);
                    context.fill(textEnd + 6, y + 10, paneX + w, y + 11, C_EDGE);
                }
                case ShaderRow.NOTE, ShaderRow.HINT -> {
                    boolean hint = row.kind() == ShaderRow.HINT;
                    text(context, this.textRenderer.trimToWidth(row.label().getString(), w - 8), paneX + 8, y + 5,
                        hint ? C_MOD_BLOCKED : C_MUTED);
                    if (over && row.key() != null) {
                        hovered = Text.translatable(row.key());
                    }
                }
                case ShaderRow.ARNE -> {
                    String key = row.key();
                    boolean active = selected.equals(key);
                    drawShaderRow(context, row.label(), y, y2, w, active, over);
                    hits.add(new Hit(paneX, y, paneX + w, y2, () -> {
                        ArneShaders.select(key);
                        // Nur ein Shader gleichzeitig: Iris aus, wenn ein ArneTweaks-Shader gewaehlt wird
                        if (!key.isEmpty() && ShaderPacks.irisLoaded() && ShaderPacks.enabled()) {
                            ShaderPacks.disable();
                        }
                    }));
                }
                case ShaderRow.IRIS_TOGGLE -> {
                    round(context, paneX, y, paneX + w, y2, over ? C_CARD_HOVER : C_CARD);
                    Text state = irisCurrent == null
                        ? Text.translatable("arnestyles.gui.shaders_none")
                        : Text.translatable("arnestyles.gui.shaders_using", irisCurrent);
                    text(context, this.textRenderer.trimToWidth(state.getString(), w - 44), paneX + 8, y + 5,
                        irisCurrent == null ? C_MUTED : C_TEXT);
                    drawSwitch(context, paneX + w - 28, y + 3, irisOn, false);
                    if (irisCurrent != null) {
                        String current = irisCurrent;
                        hits.add(new Hit(paneX, y, paneX + w, y2, () -> {
                            if (ShaderPacks.enabled()) {
                                ShaderPacks.disable();
                            } else {
                                ArneShaders.select("");
                                ShaderPacks.select(current);
                            }
                        }));
                    }
                }
                case ShaderRow.IRIS_PACK -> {
                    String pack = row.key();
                    drawShaderRow(context, row.label(), y, y2, w, irisOn && pack.equals(irisCurrent), over);
                    hits.add(new Hit(paneX, y, paneX + w, y2, () -> {
                        ArneShaders.select("");
                        ShaderPacks.select(pack);
                    }));
                }
                default -> {
                }
            }
        }
        Text line = hovered != null ? hovered : info;
        text(context, this.textRenderer.trimToWidth(line.getString(), w), paneX, infoY, hovered != null ? C_MUTED : infoColor);
    }

    private void drawShaderRow(DrawContext context, Text label, int y, int y2, int w, boolean active, boolean over) {
        if (active) {
            drawActive(context, paneX, y, paneX + w, y2);
        } else {
            round(context, paneX, y, paneX + w, y2, over ? C_CARD_HOVER : C_CARD);
        }
        text(context, this.textRenderer.trimToWidth(label.getString(), w - 70), paneX + 8, y + 5, C_TEXT);
        if (active) {
            Text tag = Text.translatable("arnestyles.gui.shaders_active");
            text(context, tag, paneX + w - 8 - this.textRenderer.getWidth(tag), y + 5, C_OK);
        }
    }

    // ---- Design: Farben und Groesse des Menues ----

    private static final String THEME_ICON = "textures/item/painting.png";
    private static final int THEME_NAME_MAX = 24;
    /** Groesse des Menues: wird erst nach dem Loslassen des Reglers uebernommen, sonst springt alles unter der Maus weg. */
    private float scale = 1.0f;
    /** Breite und Hoehe in Menue-Pixeln (Fenster geteilt durch scale). */
    private int vw = 1;
    private int vh = 1;
    /** Textschatten nur bei dunklen Designs, auf hellem Grund verschmiert er die Schrift. */
    private static boolean shadow = true;
    /** Aufbau (aus GuiLayout, bei jedem Zeichnen uebernommen). */
    private static int corners;
    private static boolean flatHeader;
    private static boolean iconSidebar;
    private static int activeStyle;
    private static boolean bevel;
    private static int switchStyle;
    /** Inspector rechts an, grosses Dashboard-Fenster. */
    private static boolean inspectorOn = true;
    private static boolean bigLayout;
    /** Name des Seitenleisten-Eintrags unter der Maus, wenn nur Symbole zu sehen sind. */
    private @Nullable Text sideTip;
    private int sideTipY;
    /** Design-Seite: false = Farben, true = Aufbau. */
    private boolean layoutPage;
    /** Presets-Seite: false = Style-Presets, true = Keybind-Sets. */
    private boolean keybindPage;
    /** Mods-Seite mit Reitern oder Kacheln: gewaehlte Oberkategorie. */
    private static String modTab = "hud";
    /** Anzahl Zeilen der Mods-Seite beim letzten Zeichnen (zum Scrollen). */
    private int modLineCount;
    /** Suche und Filter der Mods-Seite (0 = alle, 1 = an, 2 = aus). */
    private final StringBuilder modSearch = new StringBuilder();
    private int modFilter;
    /** Ueberschrift des Blocks, dessen Reset einmal angeklickt wurde. */
    private @Nullable String blockResetArmed;
    /** Farbe, deren Hex-Wert gerade eingetippt wird. */
    private @Nullable String hexEditKey;
    /** Pack, dessen Entfernen einmal angeklickt wurde. */
    private @Nullable Path packRemoveArmed;
    private final StringBuilder hexBuffer = new StringBuilder();
    private GuiTheme.Slot themeSlot = GuiTheme.Slot.ACCENT;
    private @Nullable String themeDeleteArmed;

    /** Regler fuer die Groesse des ganzen Menues (50 bis 150 %). */
    private static final SettingSlider SCALE_SLIDER = new SettingSlider() {
        @Override
        public String nameKey() {
            return "arnestyles.theme.scale";
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.theme.scale.desc";
        }

        @Override
        public float fraction() {
            return (ConfigManager.get().guiScale - 0.5f) / 1.0f;
        }

        @Override
        public void setFraction(float fraction) {
            float value = 0.5f + Math.max(0.0f, Math.min(1.0f, fraction));
            ConfigManager.get().guiScale = Math.round(value * 20.0f) / 20.0f;
        }

        @Override
        public void reset() {
            ConfigManager.get().guiScale = 1.0f;
        }

        @Override
        public String display() {
            return Math.round(ConfigManager.get().guiScale * 100.0f) + "%";
        }
    };

    /** Helligkeit einer Farbe des eigenen Designs. */
    private record ThemeValueSlider(GuiTheme.Theme theme, GuiTheme.Slot slot) implements SettingSlider {
        @Override
        public String nameKey() {
            return "arnestyles.tint.brightness";
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.theme.brightness.desc";
        }

        @Override
        public float fraction() {
            return GuiTheme.toHsv(theme.rgb(slot))[2];
        }

        @Override
        public void setFraction(float fraction) {
            float[] hsv = GuiTheme.toHsv(theme.rgb(slot));
            float value = Math.max(0.0f, Math.min(1.0f, fraction));
            GuiTheme.setColor(theme, slot, net.minecraft.util.math.MathHelper.hsvToRgb(hsv[0], hsv[1], value));
        }

        @Override
        public void reset() {
            GuiTheme.setColor(theme, slot, GuiTheme.baseOf(theme).rgb(slot));
            GuiTheme.save(theme);
        }

        @Override
        public String display() {
            return Math.round(fraction() * 100.0f) + "%";
        }
    }

    private void updateScale() {
        if (draggingSlider == null || draggingSlider.slider() != SCALE_SLIDER) {
            float wanted = ConfigManager.get().guiScale;
            if (bigLayout && this.client != null) {
                // Dashboard: eigene, scharfe Pixelgroesse (ganzzahlig), damit bei 1920x1080 ein grosses Fenster mit
                // kleiner Schrift entsteht, unabhaengig von der GUI-Groesse von Minecraft
                var window = this.client.getWindow();
                int eff = Math.max(1, Math.min(window.getFramebufferWidth() / 960, window.getFramebufferHeight() / 540));
                wanted *= eff / (float) window.getScaleFactor();
            } else if (inspectorOn && wanted <= 1.0f && this.client != null) {
                // Normales Fenster mit Live-Vorschau: so weit verkleinern (scharf, ganzzahlig), dass 600 Pixel Breite passen
                var window = this.client.getWindow();
                int eff = Math.max(1, Math.min(window.getFramebufferWidth() / 616, window.getFramebufferHeight() / 378));
                wanted = Math.min(wanted, eff / (float) window.getScaleFactor());
            }
            if (wanted > 1.0f) {
                // Nie so gross, dass das Menue nicht mehr ins Fenster passt
                wanted = Math.max(1.0f, Math.min(wanted, Math.min(this.width / 360.0f, this.height / 240.0f)));
            }
            scale = wanted;
        }
        vw = Math.max(1, (int) Math.floor(this.width / scale));
        vh = Math.max(1, (int) Math.floor(this.height / scale));
    }

    /** Uebernimmt die Farben des gewaehlten Designs. */
    private static void applyTheme() {
        GuiTheme.Theme t = GuiTheme.active();
        C_DIM = t.argb(GuiTheme.Slot.BACKDROP);
        C_PANEL = t.argb(GuiTheme.Slot.PANEL);
        C_EDGE = t.argb(GuiTheme.Slot.EDGE);
        C_HEADER_TOP = t.argb(GuiTheme.Slot.HEADER_TOP);
        C_HEADER_BOTTOM = t.argb(GuiTheme.Slot.HEADER_BOTTOM);
        C_SIDEBAR = t.argb(GuiTheme.Slot.SIDEBAR);
        C_FIELD = t.argb(GuiTheme.Slot.FIELD);
        C_CARD = t.argb(GuiTheme.Slot.CARD);
        C_CARD_HOVER = t.argb(GuiTheme.Slot.CARD_HOVER);
        C_ACCENT = t.argb(GuiTheme.Slot.ACCENT);
        C_ACCENT_SOFT = 0x40000000 | t.rgb(GuiTheme.Slot.ACCENT);
        C_ACCENT2 = t.argb(GuiTheme.Slot.ACCENT2);
        C_TEXT = t.argb(GuiTheme.Slot.TEXT);
        C_MUTED = t.argb(GuiTheme.Slot.MUTED);
        C_OK = t.argb(GuiTheme.Slot.OK);
        C_WARN = t.argb(GuiTheme.Slot.WARN);
        C_DANGER = t.argb(GuiTheme.Slot.DANGER);
        C_OFF = t.argb(GuiTheme.Slot.OFF);
        C_EDGE_HOVER = GuiTheme.mix(C_EDGE, C_ACCENT, 0.35f);
        C_TEXT_SOFT = GuiTheme.mix(C_TEXT, C_MUTED, 0.3f);
        shadow = GuiTheme.toHsv(t.rgb(GuiTheme.Slot.PANEL))[2] < 0.6f;
        corners = GuiLayout.Option.CORNERS.value();
        flatHeader = GuiLayout.Option.HEADER.value() == 1;
        iconSidebar = GuiLayout.Option.SIDEBAR.value() == 1;
        activeStyle = GuiLayout.Option.ACTIVE.value();
        bevel = GuiLayout.Option.BUTTONS.value() == 1;
        switchStyle = GuiLayout.Option.SWITCHES.value();
        inspectorOn = GuiLayout.Option.INSPECTOR.value() == 0;
        bigLayout = GuiLayout.Option.SIZE.value() == 1;
        if (GuiLayout.Option.GLASS.value() == 2) {
            // Leicht durchsichtig: die (von Minecraft verschwommene) Spielwelt bleibt erkennbar, Inhalte gut lesbar
            C_DIM = 0x60000000 | (C_DIM & 0xFFFFFF);
            C_PANEL = 0xD8000000 | (C_PANEL & 0xFFFFFF);
            C_SIDEBAR = 0xC0000000 | (C_SIDEBAR & 0xFFFFFF);
            C_HEADER_TOP = 0xE4000000 | (C_HEADER_TOP & 0xFFFFFF);
            C_HEADER_BOTTOM = 0xE4000000 | (C_HEADER_BOTTOM & 0xFFFFFF);
            C_CARD = 0xE8000000 | (C_CARD & 0xFFFFFF);
        }
        if (GuiLayout.Option.GLASS.value() == 1) {
            // Durchsichtig: Spielwelt scheint durch
            C_DIM = 0x50000000 | (C_DIM & 0xFFFFFF);
            C_PANEL = 0xB0000000 | (C_PANEL & 0xFFFFFF);
            C_SIDEBAR = 0x80000000 | (C_SIDEBAR & 0xFFFFFF);
            C_HEADER_TOP = 0xC8000000 | (C_HEADER_TOP & 0xFFFFFF);
            C_HEADER_BOTTOM = 0xC8000000 | (C_HEADER_BOTTOM & 0xFFFFFF);
            C_CARD = 0xD0000000 | (C_CARD & 0xFFFFFF);
        }
    }

    private void text(DrawContext context, Text value, int x, int y, int color) {
        context.drawText(this.textRenderer, value, x, y, color, shadow);
    }

    private void text(DrawContext context, String value, int x, int y, int color) {
        context.drawText(this.textRenderer, value, x, y, color, shadow);
    }

    private void text(DrawContext context, OrderedText value, int x, int y, int color) {
        context.drawText(this.textRenderer, value, x, y, color, shadow);
    }

    private void openThemes() {
        themesOpen = true;
        importOpen = false;
        presetsOpen = false;
        modsOpen = false;
        shadersOpen = false;
        cardScroll = 0;
        themeDeleteArmed = null;
        GuiTheme.reload();
    }

    /** Menue direkt auf der Design-Seite oeffnen (Selbsttest). */
    public static StylesScreen onThemes() {
        StylesScreen screen = new StylesScreen();
        screen.openThemes();
        return screen;
    }

    /** Eine Zeile der Design-Liste: Ueberschrift, Design oder "+ Neues Design". */
    private record ThemeRow(int kind, GuiTheme.@Nullable Theme theme, Text label) {
        static final int SECTION = 0;
        static final int THEME = 1;
        static final int NEW = 2;
    }

    private static Text themeName(GuiTheme.Theme theme) {
        return theme.builtIn() ? Text.translatableWithFallback("arnestyles.theme." + theme.id(), theme.name()) : Text.literal(theme.name());
    }

    private static List<ThemeRow> themeRows() {
        List<ThemeRow> rows = new ArrayList<>();
        rows.add(new ThemeRow(ThemeRow.SECTION, null, Text.translatable("arnestyles.theme.section_builtin")));
        for (GuiTheme.Theme theme : GuiTheme.builtIns()) {
            rows.add(new ThemeRow(ThemeRow.THEME, theme, themeName(theme)));
        }
        rows.add(new ThemeRow(ThemeRow.SECTION, null, Text.translatable("arnestyles.theme.section_custom")));
        for (GuiTheme.Theme theme : GuiTheme.custom()) {
            rows.add(new ThemeRow(ThemeRow.THEME, theme, themeName(theme)));
        }
        rows.add(new ThemeRow(ThemeRow.NEW, null, Text.translatable("arnestyles.theme.new")));
        return rows;
    }

    private int themeRowCount() {
        return themeRows().size();
    }

    /**
     * Design-Seite. Oben die Groesse, links fertige und eigene Designs, rechts der Editor fuer das eigene Design:
     * Name, alle Farben als Liste, Farbkreis und Helligkeit. Das Menue selbst zeigt jede Aenderung sofort.
     */
    private void drawThemes(DrawContext context, int mouseX, int mouseY) {
        int headY = top + HEADER_H + 7;
        drawBigText(context, Text.translatable("arnestyles.gui.themes"), paneX, headY, 1.3f, C_TEXT);
        // Umschalten zwischen Farben und Aufbau
        int px = paneRight;
        for (int k = 1; k >= 0; k--) {
            boolean layoutTab = k == 1;
            Text label = Text.translatable(layoutTab ? "arnestyles.layout.tab.layout" : "arnestyles.layout.tab.colors");
            int pw = this.textRenderer.getWidth(label) + 12;
            int x1 = px - pw;
            boolean selected = layoutPage == layoutTab;
            boolean over = inside(mouseX, mouseY, x1, headY - 2, px, headY + 12);
            roundBordered(context, x1, headY - 2, px, headY + 12, selected || over ? C_ACCENT : C_EDGE, selected ? C_ACCENT : over ? C_CARD_HOVER : C_CARD);
            text(context, label, x1 + 6, headY + 1, selected ? onAccent() : C_TEXT);
            hits.add(new Hit(x1, headY - 2, px, headY + 12, () -> {
                layoutPage = layoutTab;
                cardScroll = 0;
            }));
            px = x1 - 4;
        }
        if (layoutPage) {
            drawLayoutPage(context, mouseX, mouseY);
            return;
        }
        int w = paneRight - paneX;
        int infoY = top + HEADER_H + 26;
        int y0 = top + HEADER_H + 42;
        Text hovered = null;
        if (drawSliderRow(context, SCALE_SLIDER, paneX, y0, w, true, mouseX, mouseY, null)) {
            hovered = Text.translatable(SCALE_SLIDER.descriptionKey());
        }
        if (draggingSlider != null && draggingSlider.slider() == SCALE_SLIDER) {
            hovered = Text.translatable("arnestyles.theme.scale.release");
        }

        int y1 = y0 + ESSENTIAL_ROW_H + 4;
        // Saison-Designs: Weihnachten, Ostern und Halloween automatisch
        String season = GuiTheme.seasonTheme(java.time.LocalDate.now());
        Text seasonLabel = Text.translatable("arnestyles.theme.seasonal");
        drawTintSwitch(context, paneX, y1, w, seasonLabel, ConfigManager.get().seasonalThemes, "arnestyles.theme.seasonal.desc", mouseX, mouseY,
            () -> {
                ConfigManager.get().seasonalThemes = !ConfigManager.get().seasonalThemes;
                ConfigManager.save();
                applyTheme();
            });
        if (inside(mouseX, mouseY, paneX, y1, paneX + w, y1 + ESSENTIAL_ROW_H)) {
            hovered = season != null
                ? Text.translatable("arnestyles.theme.seasonal.now", Text.translatable("arnestyles.theme.season." + season))
                : Text.translatable("arnestyles.theme.seasonal.desc");
        }
        y1 += ESSENTIAL_ROW_H + 2;
        GuiTheme.Theme active = GuiTheme.active();
        int lw = Math.max(130, (w - 8) * 45 / 100);
        int rx = paneX + lw + 8;

        // Links: Liste der Designs
        List<ThemeRow> rows = themeRows();
        int visible = Math.max(1, (cardsBottom - y1 + 2) / ESSENTIAL_ROW_H);
        cardScroll = Math.max(0, Math.min(Math.max(0, rows.size() - visible), cardScroll));
        for (int i = cardScroll; i < Math.min(rows.size(), cardScroll + visible); i++) {
            ThemeRow row = rows.get(i);
            int y = y1 + (i - cardScroll) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, paneX, y, paneX + lw, y2);
            switch (row.kind()) {
                case ThemeRow.SECTION -> {
                    int textEnd = paneX + 2 + this.textRenderer.getWidth(row.label());
                    text(context, row.label(), paneX + 2, y + 6, C_ACCENT);
                    context.fill(textEnd + 6, y + 10, paneX + lw, y + 11, C_EDGE);
                }
                case ThemeRow.NEW -> {
                    round(context, paneX, y, paneX + lw, y2, over ? C_CARD_HOVER : C_CARD);
                    text(context, this.textRenderer.trimToWidth(row.label().getString(), lw - 12), paneX + 8, y + 5, over ? C_ACCENT : C_TEXT);
                    hits.add(new Hit(paneX, y, paneX + lw, y2, () -> {
                        GuiTheme.createFrom(GuiTheme.active());
                        themeSlot = GuiTheme.Slot.ACCENT;
                    }));
                    if (over) {
                        hovered = Text.translatable("arnestyles.theme.new.desc");
                    }
                }
                default -> {
                    GuiTheme.Theme theme = row.theme();
                    if (theme == null) {
                        continue;
                    }
                    boolean isActive = theme.id().equals(active.id());
                    if (isActive) {
                        drawActive(context, paneX, y, paneX + lw, y2);
                    } else {
                        round(context, paneX, y, paneX + lw, y2, over ? C_CARD_HOVER : C_CARD);
                    }
                    drawThemeSwatches(context, theme, paneX + 6, y + 3);
                    hits.add(new Hit(paneX, y, paneX + lw, y2, () -> {
                        GuiTheme.select(theme.id());
                        themeDeleteArmed = null;
                    }));
                    int nameEnd = paneX + lw - 6;
                    if (!theme.builtIn()) {
                        boolean armed = theme.id().equals(themeDeleteArmed);
                        nameEnd = drawPillRight(context, paneX + lw - 2, y + 2, Text.translatable(armed ? "arnestyles.gui.preset_delete_confirm" : "arnestyles.theme.delete"),
                            armed ? C_DANGER : C_MUTED, mouseX, mouseY, () -> {
                                if (theme.id().equals(themeDeleteArmed)) {
                                    GuiTheme.delete(theme);
                                    themeDeleteArmed = null;
                                } else {
                                    themeDeleteArmed = theme.id();
                                }
                            }) - 4;
                    }
                    if (isActive) {
                        Text tag = Text.translatable("arnestyles.gui.shaders_active");
                        nameEnd -= this.textRenderer.getWidth(tag);
                        text(context, tag, nameEnd, y + 5, C_OK);
                        nameEnd -= 4;
                    }
                    text(context, this.textRenderer.trimToWidth(row.label().getString(), Math.max(10, nameEnd - paneX - 34)), paneX + 32, y + 5, C_TEXT);
                }
            }
        }

        // Rechts: Editor oder Hinweis
        Text editorHover = active.builtIn()
            ? drawThemeHint(context, active, rx, y1, mouseX, mouseY)
            : drawThemeEditor(context, active, rx, y1, mouseX, mouseY);
        if (editorHover != null) {
            hovered = editorHover;
        }

        Text line = hovered != null ? hovered : Text.translatable("arnestyles.theme.info");
        text(context, this.textRenderer.trimToWidth(line.getString(), w), paneX, infoY, C_MUTED);
    }

    /** Fuer den Selbsttest: Design-Seite mit dem Aufbau-Reiter. */
    public static StylesScreen onLayout() {
        StylesScreen screen = onThemes();
        screen.layoutPage = true;
        return screen;
    }

    /**
     * Aufbau-Seite: links Vorlagen (Farben und Aufbau mit einem Klick), rechts jede Option einzeln.
     * Klick auf eine Option schaltet zum naechsten Wert, die Pfeile gehen vor und zurueck.
     */
    private void drawLayoutPage(DrawContext context, int mouseX, int mouseY) {
        int w = paneRight - paneX;
        int infoY = top + HEADER_H + 26;
        int y0 = top + HEADER_H + 42;
        Text hovered = null;
        if (drawSliderRow(context, SCALE_SLIDER, paneX, y0, w, true, mouseX, mouseY, null)) {
            hovered = Text.translatable(SCALE_SLIDER.descriptionKey());
        }
        int y1 = y0 + ESSENTIAL_ROW_H + 4;
        int lw = Math.max(110, (w - 8) * 40 / 100);
        int rx = paneX + lw + 8;
        int rw = paneRight - rx;

        // Links: Vorlagen
        List<GuiLayout.Template> templates = GuiLayout.TEMPLATES;
        int visible = Math.max(1, (cardsBottom - y1 - 12 + 2) / ESSENTIAL_ROW_H);
        cardScroll = Math.max(0, Math.min(Math.max(0, templates.size() - visible), cardScroll));
        text(context, Text.translatable("arnestyles.layout.templates"), paneX + 2, y1 + 1, C_ACCENT);
        int ly = y1 + 12;
        for (int i = cardScroll; i < Math.min(templates.size(), cardScroll + visible); i++) {
            GuiLayout.Template template = templates.get(i);
            int y = ly + (i - cardScroll) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, paneX, y, paneX + lw, y2);
            boolean isActive = GuiLayout.matches(template);
            if (isActive) {
                drawActive(context, paneX, y, paneX + lw, y2);
            } else {
                round(context, paneX, y, paneX + lw, y2, over ? C_CARD_HOVER : C_CARD);
            }
            GuiTheme.Theme theme = GuiTheme.builtIns().stream().filter(t -> t.id().equals(template.theme())).findFirst()
                .orElse(GuiTheme.builtIns().get(0));
            drawThemeSwatches(context, theme, paneX + 6, y + 4);
            String letter = String.valueOf((char) ('A' + i));
            text(context, this.textRenderer.trimToWidth(letter + "  " + Text.translatable(template.nameKey()).getString(), lw - 36),
                paneX + 32, y + 5, isActive ? C_TEXT : C_TEXT_SOFT);
            hits.add(new Hit(paneX, y, paneX + lw, y2, () -> GuiLayout.apply(template)));
            if (over) {
                hovered = Text.translatable(template.descriptionKey());
            }
        }

        // Rechts: jede Option einzeln
        text(context, Text.translatable("arnestyles.layout.options"), rx + 2, y1 + 1, C_ACCENT);
        int oy = y1 + 12;
        for (GuiLayout.Option option : GuiLayout.Option.values()) {
            if (oy + ESSENTIAL_ROW_H - 2 > cardsBottom + 1) {
                break;
            }
            int y2 = oy + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, rx, oy, paneRight, y2);
            round(context, rx, oy, paneRight, y2, over ? C_CARD_HOVER : C_CARD);
            hits.add(new Hit(rx, oy, paneRight, y2, () -> option.cycle(1)));
            Text value = Text.translatable(option.valueKey());
            int nx = drawPillRight(context, paneRight - 2, oy + 2, Text.literal(">"), C_TEXT, mouseX, mouseY, () -> option.cycle(1));
            int vw2 = this.textRenderer.getWidth(value);
            text(context, value, nx - 4 - vw2, oy + 5, C_ACCENT);
            int bx = drawPillRight(context, nx - 8 - vw2, oy + 2, Text.literal("<"), C_TEXT, mouseX, mouseY, () -> option.cycle(-1));
            text(context, this.textRenderer.trimToWidth(Text.translatable(option.nameKey()).getString(), Math.max(0, bx - rx - 10)),
                rx + 6, oy + 5, C_TEXT);
            if (over) {
                hovered = Text.translatable(option.descriptionKey());
            }
            oy += ESSENTIAL_ROW_H;
        }
        if (rw < 120 && hovered == null) {
            hovered = Text.translatable("arnestyles.layout.small");
        }
        Text line = hovered != null ? hovered : Text.translatable("arnestyles.layout.info");
        text(context, this.textRenderer.trimToWidth(line.getString(), w), paneX, infoY, C_MUTED);
    }

    /** Vier kleine Farbfelder als Vorschau eines Designs (Kopfzeile, Fenster, Akzent, Text). */
    private static void drawThemeSwatches(DrawContext context, GuiTheme.Theme theme, int x, int y) {
        GuiTheme.Slot[] slots = {GuiTheme.Slot.HEADER_TOP, GuiTheme.Slot.PANEL, GuiTheme.Slot.ACCENT, GuiTheme.Slot.TEXT};
        context.fill(x - 1, y - 1, x + slots.length * 5 + 1, y + 11, 0xFF000000);
        for (int i = 0; i < slots.length; i++) {
            context.fill(x + i * 5, y, x + i * 5 + 5, y + 10, 0xFF000000 | theme.rgb(slots[i]));
        }
    }

    /** Fertiges Design gewaehlt: kurze Erklaerung und ein Knopf zum Kopieren. */
    private @Nullable Text drawThemeHint(DrawContext context, GuiTheme.Theme active, int x, int y, int mouseX, int mouseY) {
        int w = paneRight - x;
        round(context, x, y, paneRight, cardsBottom, C_CARD);
        int ty = y + 6;
        for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("arnestyles.theme.builtin_hint", themeName(active)), w - 12)) {
            if (ty + 9 > cardsBottom - 20) {
                break;
            }
            text(context, line, x + 6, ty, C_TEXT_SOFT);
            ty += 10;
        }
        ty += 3;
        // Knopf vor dem Farb-Raster, damit er auch in kleinen Fenstern zu sehen ist
        Text copy = Text.translatable("arnestyles.theme.copy");
        int pillW = this.textRenderer.getWidth(copy) + 12;
        drawPillRight(context, x + 6 + pillW, ty, copy, C_ACCENT, mouseX, mouseY, () -> {
            GuiTheme.createFrom(GuiTheme.active());
            themeSlot = GuiTheme.Slot.ACCENT;
        });
        boolean overCopy = inside(mouseX, mouseY, x + 6, ty, x + 6 + pillW, ty + 14);
        ty += 20;
        // Alle Farben des Designs als Raster (so viel wie hineinpasst)
        GuiTheme.Slot[] slots = GuiTheme.Slot.values();
        int cell = 12;
        int perRow = Math.max(1, (w - 12) / cell);
        for (int i = 0; i < slots.length; i++) {
            int sx = x + 6 + (i % perRow) * cell;
            int sy = ty + (i / perRow) * cell;
            if (sy + 10 > cardsBottom - 2) {
                break;
            }
            context.fill(sx, sy, sx + 10, sy + 10, 0xFF000000);
            context.fill(sx + 1, sy + 1, sx + 9, sy + 9, 0xFF000000 | active.rgb(slots[i]));
        }
        return overCopy ? Text.translatable("arnestyles.theme.new.desc") : null;
    }

    /**
     * Editor fuer das eigene Design: Name, darunter Farbkreis mit Helligkeit und Zuruecksetzen,
     * darunter alle Farben. Bei wenig Platz bekommt die Farbliste mehr Spalten und zeigt nur noch Farbfelder.
     */
    private @Nullable Text drawThemeEditor(DrawContext context, GuiTheme.Theme theme, int x, int y, int mouseX, int mouseY) {
        Text hovered = null;
        int w = paneRight - x;

        // Name (einfach lostippen)
        roundBordered(context, x, y, paneRight, y + 16, C_ACCENT, C_FIELD);
        boolean cursor = (System.currentTimeMillis() / 500L) % 2L == 0L;
        String name = this.textRenderer.trimToWidth(theme.name(), w - 16);
        text(context, cursor ? name + "_" : name, x + 6, y + 4, C_TEXT);
        if (inside(mouseX, mouseY, x, y, paneRight, y + 16)) {
            hovered = Text.translatable("arnestyles.theme.name.desc");
        }
        y += 20;

        // Farbkreis und Helligkeit fuer die gewaehlte Farbe
        GuiTheme.Slot slot = themeSlot;
        float[] hsv = GuiTheme.toHsv(theme.rgb(slot));
        int wheelSize = 2 * ESSENTIAL_ROW_H;
        wheelX = x;
        wheelY = y;
        wheelD = wheelSize;
        drawWheel(context, hsv[0], hsv[1], Math.max(0.3f, hsv[2]), true);
        wheelApply = (hue, saturation) -> {
            float value = GuiTheme.toHsv(theme.rgb(slot))[2];
            // Aus Schwarz heraus wuerde man sonst keine Farbe sehen
            GuiTheme.setColor(theme, slot, net.minecraft.util.math.MathHelper.hsvToRgb(hue, saturation, value < 0.08f ? 0.8f : value));
        };
        int cx = x + wheelSize + 6;
        int cw = paneRight - cx;
        if (drawSliderRow(context, new ThemeValueSlider(theme, slot), cx, y, cw, true, mouseX, mouseY, null)) {
            hovered = Text.translatable("arnestyles.theme.brightness.desc");
        }
        int ry = y + ESSENTIAL_ROW_H + 2;
        round(context, cx, ry + 1, cx + 14, ry + 13, C_EDGE);
        round(context, cx + 1, ry + 2, cx + 13, ry + 12, 0xFF000000 | theme.rgb(slot));
        String slotLabel = String.format(Locale.ROOT, "#%06X  ", theme.rgb(slot)) + Text.translatable("arnestyles.theme.slot." + slot.key()).getString();
        int resetX = drawPillRight(context, paneRight, ry, Text.translatable("arnestyles.customize.reset"), C_MUTED, mouseX, mouseY, () -> {
            GuiTheme.setColor(theme, slot, GuiTheme.baseOf(theme).rgb(slot));
            GuiTheme.save(theme);
        });
        text(context, this.textRenderer.trimToWidth(slotLabel, Math.max(0, resetX - cx - 22)), cx + 18, ry + 4, C_TEXT_SOFT);
        if (inside(mouseX, mouseY, wheelX, wheelY, wheelX + wheelD, wheelY + wheelD)) {
            hovered = Text.translatable("arnestyles.theme.wheel.desc");
        }
        y += wheelSize + 4;

        // Alle Farben
        GuiTheme.Slot[] slots = GuiTheme.Slot.values();
        int cellH = 14;
        int rowsFit = Math.max(1, (cardsBottom - y) / cellH);
        int cols = Math.max(w >= 230 ? 3 : 2, (slots.length + rowsFit - 1) / rowsFit);
        int cellW = w / cols;
        boolean names = cellW >= 64;
        for (int i = 0; i < slots.length; i++) {
            GuiTheme.Slot s = slots[i];
            int sx = x + (i % cols) * cellW;
            int sy = y + (i / cols) * cellH;
            boolean selected = s == themeSlot;
            boolean over = inside(mouseX, mouseY, sx, sy, sx + cellW - 2, sy + cellH - 1);
            if (selected || over) {
                roundBordered(context, sx, sy, sx + cellW - 2, sy + cellH - 1, selected ? C_ACCENT : C_EDGE_HOVER, selected ? C_ACCENT_SOFT : C_CARD_HOVER);
            }
            context.fill(sx + 2, sy + 2, sx + 12, sy + 12, 0xFF000000);
            context.fill(sx + 3, sy + 3, sx + 11, sy + 11, 0xFF000000 | theme.rgb(s));
            Text slotName = Text.translatable("arnestyles.theme.slot." + s.key());
            if (names) {
                text(context, this.textRenderer.trimToWidth(slotName.getString(), cellW - 18), sx + 15, sy + 3, selected ? C_TEXT : C_TEXT_SOFT);
            }
            hits.add(new Hit(sx, sy, sx + cellW - 2, sy + cellH - 1, () -> themeSlot = s));
            if (over) {
                hovered = Text.literal(slotName.getString() + ": " + Text.translatable("arnestyles.theme.slot." + s.key() + ".desc").getString());
            }
        }
        return hovered;
    }

    // ---- Pack-Import ----

    private static final String IMPORT_ICON = "textures/item/bundle.png";
    private static final int IMPORT_ROW_H = 32;

    private void openImport() {
        importOpen = true;
        themesOpen = false;
        presetsOpen = false;
        modsOpen = false;
        shadersOpen = false;
        cardScroll = 0;
    }

    /** Menue direkt auf der Import-Seite oeffnen (Selbsttest). */
    public static StylesScreen onImport() {
        StylesScreen screen = new StylesScreen();
        screen.openImport();
        return screen;
    }

    /** Packs (.zip oder Ordner) ins Fenster gezogen: sofort importieren. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        boolean any = false;
        for (Path path : paths) {
            if (PackImporter.isPack(path)) {
                PackImporter.addDropped(path);
                PackImporter.enqueue(path);
                any = true;
            }
        }
        if (any) {
            openImport();
        }
    }

    /** Kategorie oeffnen und den importierten Style darin auswaehlen. */
    private void showImported(StyleCategory category, String styleId) {
        select(category);
        for (StyleDefinition style : StyleRegistry.getStyles(category)) {
            if (style.id().equalsIgnoreCase(styleId)) {
                chooseStyle(style);
                return;
            }
        }
    }

    /**
     * Import-Seite: oben die Ablage-Flaeche (Packs ins Fenster ziehen), darunter alle Packs aus dem Import-Ordner.
     * Nach dem Import zeigt jede Zeile, in welche Kategorien das Pack einsortiert wurde; ein Klick darauf oeffnet sie.
     */
    private void drawImport(DrawContext context, int mouseX, int mouseY) {
        int headY = top + HEADER_H + 7;
        drawBigText(context, Text.translatable("arnestyles.gui.import"), paneX, headY, 1.3f, C_TEXT);
        // Knoepfe oben rechts: Ordner oeffnen (neutral), Upload Ordner und Upload .zip (violett)
        int bx = drawPillRight(context, paneRight, headY - 2, Text.translatable("arnestyles.import.open_folder"), C_TEXT, mouseX, mouseY, this::openFolder);
        bx = drawAccentButton(context, bx - 4, headY - 2, Text.translatable("arnestyles.import.upload_folder"), mouseX, mouseY, () -> pickPacks(true));
        bx = drawAccentButton(context, bx - 4, headY - 2, Text.translatable("arnestyles.import.upload_zip"), mouseX, mouseY, () -> pickPacks(false));
        int removed = PackImporter.removedCount();
        if (removed > 0) {
            drawPillRight(context, bx - 4, headY - 2, Text.translatable("arnestyles.import.restore", removed), C_WARN, mouseX, mouseY,
                PackImporter::restoreAll);
        }
        int w = paneRight - paneX;
        int infoY = top + HEADER_H + 26;
        int y0 = top + HEADER_H + 42;
        Text hovered = null;

        List<Path> packs = PackImporter.packs();
        // Ablage-Flaeche mit "Alle importieren"
        int boxH = 30;
        context.fill(paneX, y0, paneRight, y0 + boxH, (C_ACCENT2 & 0xFFFFFF) | 0x18000000);
        drawDashedRect(context, paneX, y0, paneRight, y0 + boxH, C_ACCENT2);
        drawIconAsset(context, IMPORT_ICON, paneX + 6, y0 + 7);
        int allX = paneRight - 6;
        if (!packs.isEmpty()) {
            allX = drawPillRight(context, paneRight - 6, y0 + 8, Text.translatable("arnestyles.import.all"), C_OK, mouseX, mouseY, () -> {
                for (Path pack : PackImporter.packs()) {
                    PackImporter.enqueue(pack);
                }
            });
        }
        int textW = allX - paneX - 32;
        text(context, this.textRenderer.trimToWidth(Text.translatable("arnestyles.import.drop").getString(), textW), paneX + 26, y0 + 6, C_TEXT);
        text(context, this.textRenderer.trimToWidth(Text.translatable("arnestyles.import.drop2").getString(), textW), paneX + 26, y0 + 17, C_TEXT_SOFT);
        if (inside(mouseX, mouseY, paneX, y0, allX - 4, y0 + boxH)) {
            hovered = Text.translatable("arnestyles.import.drop.desc");
        }

        int listY = y0 + boxH + 6;
        if (packs.isEmpty()) {
            int ty = listY + 4;
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("arnestyles.import.empty"), w)) {
                text(context, line, paneX, ty, C_MUTED);
                ty += 10;
            }
        }
        int visible = Math.max(1, (cardsBottom - listY + 2) / IMPORT_ROW_H);
        cardScroll = Math.max(0, Math.min(Math.max(0, packs.size() - visible), cardScroll));
        Path running = PackImporter.running();
        for (int i = cardScroll; i < Math.min(packs.size(), cardScroll + visible); i++) {
            Path pack = packs.get(i);
            int y = listY + (i - cardScroll) * IMPORT_ROW_H;
            int y2 = y + IMPORT_ROW_H - 3;
            boolean over = inside(mouseX, mouseY, paneX, y, paneRight, y2);
            round(context, paneX, y, paneRight, y2, over ? C_CARD_HOVER : C_CARD);
            boolean zip = !Files.isDirectory(pack);
            drawIconAsset(context, zip ? "textures/item/book.png" : "textures/block/barrel_top.png", paneX + 4, y + 2);

            PackImporter.Result result = PackImporter.result(pack);
            int nameEnd;
            if (pack.equals(running)) {
                Text state = Text.translatable("arnestyles.import.running");
                nameEnd = paneRight - 8 - this.textRenderer.getWidth(state);
                text(context, state, nameEnd, y + 5, C_WARN);
            } else if (PackImporter.isQueued(pack)) {
                Text state = Text.translatable("arnestyles.import.waiting");
                nameEnd = paneRight - 8 - this.textRenderer.getWidth(state);
                text(context, state, nameEnd, y + 5, C_MUTED);
            } else {
                boolean armedRemove = pack.equals(packRemoveArmed);
                nameEnd = drawPillRight(context, paneRight - 3, y + 2,
                    Text.translatable(armedRemove ? "arnestyles.import.remove_confirm" : "arnestyles.import.remove"),
                    armedRemove ? C_DANGER : C_MUTED, mouseX, mouseY, () -> {
                        if (pack.equals(packRemoveArmed)) {
                            PackImporter.remove(pack);
                            packRemoveArmed = null;
                        } else {
                            packRemoveArmed = pack;
                        }
                    });
                nameEnd = drawPillRight(context, nameEnd - 4, y + 2,
                    Text.translatable(result == null ? "arnestyles.import.one" : "arnestyles.import.again"),
                    result == null ? C_OK : C_MUTED, mouseX, mouseY, () -> PackImporter.enqueue(pack));
            }
            String name = this.textRenderer.trimToWidth(PackImporter.displayName(pack), nameEnd - paneX - 30);
            text(context, name, paneX + 24, y + 5, C_TEXT);

            // Zweite Zeile: Ergebnis als anklickbare Kategorien
            int ly = y + 16;
            int lx = paneX + 24;
            if (result == null) {
                if (!pack.equals(running) && !PackImporter.isQueued(pack)) {
                    text(context, this.textRenderer.trimToWidth(Text.translatable("arnestyles.import.not_yet").getString(), w - 30), lx, ly, C_MUTED);
                }
                continue;
            }
            if (result.error() != null) {
                text(context, this.textRenderer.trimToWidth(Text.translatable("arnestyles.import.error", result.error()).getString(), w - 30), lx, ly,
                    C_DANGER);
                continue;
            }
            if (result.categories().isEmpty()) {
                text(context, Text.translatable("arnestyles.import.nothing"), lx, ly, C_WARN);
                continue;
            }
            int shown = 0;
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(result.categories().entrySet());
            for (Map.Entry<String, Integer> entry : entries) {
                StyleCategory category = PackImporter.category(entry.getKey());
                if (category == null) {
                    continue;
                }
                Text label = categoryName(category);
                int cw = this.textRenderer.getWidth(label) + 8;
                int restW = this.textRenderer.getWidth("+99") + 8;
                boolean last = shown == entries.size() - 1;
                if (lx + cw > paneRight - 4 - (last ? 0 : restW)) {
                    Text more = Text.literal("+" + (entries.size() - shown));
                    text(context, more, lx + 2, ly, C_MUTED);
                    if (inside(mouseX, mouseY, lx, ly - 2, lx + restW, ly + 10)) {
                        hovered = Text.literal(importSummary(result));
                    }
                    break;
                }
                boolean chipOver = inside(mouseX, mouseY, lx, ly - 2, lx + cw, ly + 10);
                round(context, lx, ly - 2, lx + cw, ly + 10, chipOver ? C_ACCENT : C_ACCENT_SOFT);
                text(context, label, lx + 4, ly, C_TEXT);
                String styleId = result.styleId();
                hits.add(new Hit(lx, ly - 2, lx + cw, ly + 10, () -> showImported(category, styleId)));
                if (chipOver) {
                    hovered = Text.translatable("arnestyles.import.chip", entry.getValue(), label);
                }
                lx += cw + 3;
                shown++;
            }
            if (over && hovered == null) {
                hovered = Text.literal(importSummary(result));
            }
        }

        Text line;
        int color = C_MUTED;
        if (hovered != null) {
            line = hovered;
        } else if (running != null) {
            line = Text.translatable("arnestyles.import.status_running", PackImporter.displayName(running));
            color = C_WARN;
        } else {
            line = Text.translatable("arnestyles.import.info");
        }
        text(context, this.textRenderer.trimToWidth(line.getString(), w), paneX, infoY, color);
    }

    /** Violetter Hauptknopf, rechtsbuendig bei x2. Gibt die linke Kante zurueck. */
    private int drawAccentButton(DrawContext context, int x2, int y, Text label, int mouseX, int mouseY, Runnable action) {
        int x1 = x2 - this.textRenderer.getWidth(label) - 12;
        boolean over = inside(mouseX, mouseY, x1, y, x2, y + 14);
        roundBordered(context, x1, y, x2, y + 14, C_ACCENT2, over ? GuiTheme.mix(C_ACCENT2, 0xFFFFFFFF, 0.15f) : C_ACCENT2);
        text(context, label, x1 + 6, y + 3, 0xFFFFFFFF);
        hits.add(new Hit(x1, y, x2, y + 14, action));
        return x1;
    }

    /** Gestrichelter Rahmen (Ablage-Flaeche). */
    private static void drawDashedRect(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        for (int x = x1; x < x2; x += 7) {
            int e = Math.min(x + 4, x2);
            context.fill(x, y1, e, y1 + 1, color);
            context.fill(x, y2 - 1, e, y2, color);
        }
        for (int y = y1; y < y2; y += 7) {
            int e = Math.min(y + 4, y2);
            context.fill(x1, y, x1 + 1, e, color);
            context.fill(x2 - 1, y, x2, e, color);
        }
    }

    /** Windows-Dateidialog (eigener Thread, damit das Spiel weiterlaeuft); gewaehlte Packs werden wie gezogene importiert. */
    private void pickPacks(boolean folder) {
        String title = Text.translatable(folder ? "arnestyles.import.upload_folder" : "arnestyles.import.upload_zip").getString();
        String start = PackImporter.importDir().toAbsolutePath() + java.io.File.separator;
        Thread thread = new Thread(() -> {
            String result;
            try {
                if (folder) {
                    result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_selectFolderDialog(title, start);
                } else {
                    try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                        org.lwjgl.PointerBuffer filters = stack.mallocPointer(1);
                        filters.put(stack.UTF8("*.zip"));
                        filters.flip();
                        result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, start, filters, "Resource Pack (.zip)", true);
                    }
                }
            } catch (Throwable e) {
                com.arnestyles.ArneStyles.LOGGER.warn("Dateidialog nicht verfuegbar: {}", e.toString());
                return;
            }
            if (result == null || result.isBlank()) {
                return;
            }
            List<Path> paths = new ArrayList<>();
            for (String part : result.split("\\|")) {
                if (!part.isBlank()) {
                    paths.add(Path.of(part));
                }
            }
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> onFilesDropped(paths));
        }, "ArneTweaks-FilePicker");
        thread.setDaemon(true);
        thread.start();
    }

    /** "Totems 3, Schwerter 12, ... (5 Dateien ohne Kategorie)" */
    private static String importSummary(PackImporter.Result result) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : result.categories().entrySet()) {
            StyleCategory category = PackImporter.category(entry.getKey());
            if (category == null) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(categoryName(category).getString()).append(' ').append(entry.getValue());
        }
        if (result.unsorted() > 0) {
            text.append(Text.translatable("arnestyles.import.unsorted", result.unsorted()).getString());
        }
        return text.toString();
    }

    // ---- Mods ----

    private void openMods() {
        modsOpen = true;
        themesOpen = false;
        importOpen = false;
        presetsOpen = false;
        shadersOpen = false;
        cardScroll = 0;
    }

    /**
     * Eine Zeile der Mods-Seite: Ueberschrift, Schalter, Regler oder Farbauswahl.
     * blockedBy: ID einer einzelnen Mod mit derselben Funktion (nur bei Ueberschriften).
     */
    private record ModRow(@Nullable String section, ModSetting.@Nullable Toggle toggle, @Nullable SettingSlider slider, boolean color,
                          @Nullable String blockedBy, @Nullable String action) {
        ModRow(@Nullable String section, ModSetting.@Nullable Toggle toggle, @Nullable SettingSlider slider, boolean color, @Nullable String blockedBy) {
            this(section, toggle, slider, color, blockedBy, null);
        }

        static ModRow section(String key, @Nullable String blockedBy) {
            return new ModRow(key, null, null, false, blockedBy);
        }

        static ModRow toggle(ModSetting.Toggle toggle) {
            return new ModRow(null, toggle, null, false, null);
        }

        static ModRow slider(SettingSlider slider) {
            return new ModRow(null, null, slider, false, null);
        }

        /** Zeile, die beim Anklicken etwas oeffnet (z. B. den HUD-Editor). */
        static ModRow action(String key) {
            return new ModRow(null, null, null, false, null, key);
        }
    }

    /** Oberkategorie der Mods-Seite: per Linksklick auf- und zuklappbar, darin die einzelnen Mods. */
    private record ModGroup(String key, List<ModRow> rows) {
        /** Anzahl Mods (Ueberschriften) in der Gruppe. */
        int mods() {
            return (int) rows.stream().filter(row -> row.section() != null).count();
        }

        /** Wie viele Schalter in der Gruppe an sind. */
        int activeToggles() {
            return (int) rows.stream().filter(row -> row.toggle() != null && row.toggle().value()).count();
        }
    }

    private static final String GROUP_ROW = "group:";
    private static final String POP_SOUND_ROW = "pop_sound";
    /** Aufgeklappte Oberkategorien (bleibt so, solange das Spiel laeuft). */
    private static final java.util.Set<String> OPEN_MOD_GROUPS = new java.util.HashSet<>();

    /** Fuer den Selbsttest: alle Oberkategorien auf- oder zuklappen. */
    public static void setAllModGroupsOpen(boolean open) {
        OPEN_MOD_GROUPS.clear();
        if (open) {
            modGroups().forEach(group -> OPEN_MOD_GROUPS.add(group.key()));
        }
    }

    /** Fuer den Selbsttest: eine Oberkategorie aufklappen. */
    public static void openModGroup(String key) {
        OPEN_MOD_GROUPS.add(key);
    }

    private static List<ModGroup> modGroups() {
        return List.of(
            new ModGroup("hud", List.of(
                ModRow.section("arnestyles.mod.section.hud_editor", null),
                ModRow.action("arnestyles.mod.hud_editor"),
                ModRow.action("arnestyles.mod.hand_editor"),
                ModRow.section("arnestyles.mod.section.vanilla_hud", null),
                ModRow.action("arnestyles.mod.vanilla_hud_editor"),
                ModRow.toggle(ModSetting.Toggle.SCOREBOARD_HIDE),
                ModRow.toggle(ModSetting.Toggle.SCOREBOARD_HIDE_NUMBERS),
                ModRow.toggle(ModSetting.Toggle.SCOREBOARD_SHADOW),
                ModRow.slider(ModSetting.Slider.SCOREBOARD_BG_OPACITY),
                ModRow.toggle(ModSetting.Toggle.BOSSBAR_HIDE),
                ModRow.toggle(ModSetting.Toggle.HELD_ITEM_NAME_HIDE),
                ModRow.toggle(ModSetting.Toggle.EFFECT_ICONS_HIDE),
                ModRow.section("arnestyles.mod.section.hotbar", null),
                ModRow.toggle(ModSetting.Toggle.HOTBAR_SMOOTH),
                new ModRow(null, null, null, true, null, "hotbar_select_color"),
                ModRow.slider(ModSetting.Slider.HOTBAR_OPACITY),
                ModRow.section("arnestyles.mod.section.hud_style", null),
                new ModRow(null, null, null, true, null, "hud_text_color"),
                new ModRow(null, null, null, true, null, "hud_bg_color"),
                ModRow.slider(ModSetting.Slider.HUD_BG_OPACITY),
                ModRow.toggle(ModSetting.Toggle.HUD_TEXT_SHADOW),
                ModRow.toggle(ModSetting.Toggle.HUD_BORDER),
                new ModRow(null, null, null, true, null, "hud_border_color"),
                new ModRow(null, null, null, true, null, "hud_accent_color"),
                ModRow.section("arnestyles.mod.section.info_hud", null),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES_CPS),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES_MOUSE),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES_SPACE),
                ModRow.toggle(ModSetting.Toggle.CPS_HUD),
                ModRow.toggle(ModSetting.Toggle.FPS_HUD),
                ModRow.toggle(ModSetting.Toggle.PING_HUD),
                ModRow.toggle(ModSetting.Toggle.COORDS_HUD),
                ModRow.toggle(ModSetting.Toggle.COORDS_DIRECTION),
                ModRow.section("arnestyles.mod.section.extra_hud", null),
                ModRow.toggle(ModSetting.Toggle.CLOCK_HUD),
                ModRow.toggle(ModSetting.Toggle.CLOCK_SECONDS),
                ModRow.toggle(ModSetting.Toggle.CLOCK_12H),
                ModRow.toggle(ModSetting.Toggle.CLOCK_DATE),
                ModRow.toggle(ModSetting.Toggle.CLOCK_YEAR),
                ModRow.toggle(ModSetting.Toggle.ELYTRA_HUD),
                ModRow.toggle(ModSetting.Toggle.ELYTRA_ONLY_FLYING),
                ModRow.toggle(ModSetting.Toggle.ELYTRA_KMH),
                ModRow.toggle(ModSetting.Toggle.SERVER_HUD),
                ModRow.toggle(ModSetting.Toggle.DAY_HUD),
                ModRow.toggle(ModSetting.Toggle.DAY_TIME),
                ModRow.section("arnestyles.mod.section.pvp_hud", null),
                ModRow.toggle(ModSetting.Toggle.COMBO_HUD),
                ModRow.slider(ModSetting.Slider.COMBO_TIMEOUT),
                ModRow.toggle(ModSetting.Toggle.PEARL_COOLDOWN),
                ModRow.toggle(ModSetting.Toggle.ARMOR_WARNING),
                ModRow.slider(ModSetting.Slider.ARMOR_WARNING_PERCENT),
                ModRow.toggle(ModSetting.Toggle.ARMOR_WARNING_SOUND),
                ModRow.toggle(ModSetting.Toggle.TARGET_HUD),
                ModRow.section("arnestyles.mod.section.item_counter", null),
                ModRow.toggle(ModSetting.Toggle.ITEM_COUNTER),
                ModRow.toggle(ModSetting.Toggle.ITEM_COUNTER_VERTICAL),
                ModRow.toggle(ModSetting.Toggle.ITEM_COUNTER_HIDE_ZERO),
                ModRow.slider(ModSetting.Slider.ITEM_COUNTER_WARN),
                new ModRow(null, null, null, true, null, "item_counter_warn_color"),
                ModRow.toggle(ModSetting.Toggle.COUNT_TOTEM),
                ModRow.toggle(ModSetting.Toggle.COUNT_PEARL),
                ModRow.toggle(ModSetting.Toggle.COUNT_CRYSTAL),
                ModRow.toggle(ModSetting.Toggle.COUNT_OBSIDIAN),
                ModRow.toggle(ModSetting.Toggle.COUNT_ANCHOR),
                ModRow.toggle(ModSetting.Toggle.COUNT_GLOWSTONE),
                ModRow.toggle(ModSetting.Toggle.COUNT_GAPPLE),
                ModRow.toggle(ModSetting.Toggle.COUNT_XP),
                ModRow.toggle(ModSetting.Toggle.COUNT_ARROW),
                ModRow.toggle(ModSetting.Toggle.COUNT_COBWEB),
                ModRow.toggle(ModSetting.Toggle.COUNT_WIND_CHARGE),
                ModRow.toggle(ModSetting.Toggle.COUNT_SPLASH),
                ModRow.section("arnestyles.mod.section.tab_list", null),
                ModRow.toggle(ModSetting.Toggle.TAB_PING),
                ModRow.toggle(ModSetting.Toggle.TAB_PING_COLORS),
                ModRow.toggle(ModSetting.Toggle.TAB_PING_SUFFIX),
                ModRow.slider(ModSetting.Slider.TAB_PING_GOOD),
                ModRow.slider(ModSetting.Slider.TAB_PING_BAD),
                ModRow.section("arnestyles.mod.section.spotify", null),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_HUD),
                ModRow.slider(ModSetting.Slider.SPOTIFY_MODE),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_COVER),
                ModRow.slider(ModSetting.Slider.SPOTIFY_WIDTH),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_LYRICS),
                ModRow.slider(ModSetting.Slider.SPOTIFY_OFFSET),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_HIDE_PAUSED),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_ANY_PLAYER),
                ModRow.action("arnestyles.mod.spotify_keys"),
                new ModRow(null, null, null, true, null, "spotify_accent"),
                ModRow.slider(ModSetting.Slider.SPOTIFY_BG_OPACITY),
                ModRow.toggle(ModSetting.Toggle.SPOTIFY_SHADOW),
                ModRow.section("arnestyles.mod.section.customhud", null),
                ModRow.toggle(ModSetting.Toggle.CUSTOMHUD_ON),
                ModRow.action("arnestyles.mod.customhud_editor"),
                ModRow.action("arnestyles.mod.customhud_folder"),
                ModRow.section("arnestyles.mod.section.totem_counter", ModSetting.TOTEM_COUNTER_MOD),
                ModRow.toggle(ModSetting.Toggle.TOTEM_COUNTER),
                ModRow.toggle(ModSetting.Toggle.TOTEM_COUNTER_HUD),
                ModRow.section("arnestyles.mod.section.armor_hud", ModSetting.ARMOR_HUD_MOD),
                ModRow.toggle(ModSetting.Toggle.ARMOR_HUD),
                ModRow.toggle(ModSetting.Toggle.ARMOR_HUD_RIGHT),
                ModRow.toggle(ModSetting.Toggle.ARMOR_HUD_PERCENT),
                ModRow.section("arnestyles.mod.section.effect_timer", ModSetting.EFFECT_TIMER_MOD),
                ModRow.toggle(ModSetting.Toggle.EFFECT_TIMER),
                ModRow.toggle(ModSetting.Toggle.EFFECT_TIMER_LEVEL),
                ModRow.section("arnestyles.mod.section.appleskin", ModSetting.APPLESKIN_MOD),
                ModRow.toggle(ModSetting.Toggle.APPLESKIN_SATURATION),
                ModRow.toggle(ModSetting.Toggle.APPLESKIN_PREVIEW),
                ModRow.toggle(ModSetting.Toggle.APPLESKIN_HEALTH),
                ModRow.toggle(ModSetting.Toggle.APPLESKIN_TOOLTIP)
            )),
            new ModGroup("pvp", List.of(
                ModRow.section("arnestyles.mod.section.crosshair", null),
                ModRow.toggle(ModSetting.Toggle.CUSTOM_CROSSHAIR),
                ModRow.action("arnestyles.mod.crosshair_editor"),
                new ModRow(null, null, null, true, null, "crosshair_color"),
                ModRow.slider(ModSetting.Slider.CROSSHAIR_OPACITY),
                ModRow.slider(ModSetting.Slider.CROSSHAIR_SCALE),
                ModRow.toggle(ModSetting.Toggle.CROSSHAIR_OUTLINE),
                new ModRow(null, null, null, true, null, "crosshair_outline_color"),
                ModRow.section("arnestyles.mod.section.cooldown", null),
                ModRow.toggle(ModSetting.Toggle.COOLDOWN_INDICATOR),
                ModRow.slider(ModSetting.Slider.COOLDOWN_STYLE),
                new ModRow(null, null, null, true, null, "cooldown_color"),
                ModRow.section("arnestyles.mod.section.pvp_visuals", null),
                ModRow.toggle(ModSetting.Toggle.HIT_COLOR),
                new ModRow(null, null, null, true, null, "hit_color"),
                ModRow.toggle(ModSetting.Toggle.CROSSHAIR_TARGET),
                new ModRow(null, null, null, true, null, "crosshair_target_color"),
                ModRow.toggle(ModSetting.Toggle.HIT_CRITS),
                ModRow.toggle(ModSetting.Toggle.HIT_SHARPNESS),
                ModRow.slider(ModSetting.Slider.HIT_PARTICLES),
                ModRow.slider(ModSetting.Slider.PLAYER_FIRE),
                ModRow.section("arnestyles.mod.section.crystal", null),
                ModRow.slider(ModSetting.Slider.CRYSTAL_SIZE),
                ModRow.slider(ModSetting.Slider.CRYSTAL_SPEED),
                ModRow.toggle(ModSetting.Toggle.CRYSTAL_BOUNCE),
                ModRow.section("arnestyles.mod.section.kill_effects", null),
                ModRow.toggle(ModSetting.Toggle.KILL_EFFECTS),
                ModRow.toggle(ModSetting.Toggle.KILL_ONLY_PLAYERS),
                ModRow.toggle(ModSetting.Toggle.KILL_LIGHTNING),
                ModRow.toggle(ModSetting.Toggle.KILL_FIREWORK),
                ModRow.toggle(ModSetting.Toggle.KILL_TOTEM),
                ModRow.toggle(ModSetting.Toggle.KILL_SOUND),
                ModRow.section("arnestyles.mod.section.totem_pop", null),
                ModRow.action(POP_SOUND_ROW),
                ModRow.slider(ModSetting.Slider.TOTEM_POP_VOLUME),
                ModRow.slider(ModSetting.Slider.TOTEM_POP_PITCH),
                ModRow.slider(ModSetting.Slider.TOTEM_POP_STYLE),
                ModRow.slider(ModSetting.Slider.TOTEM_POP_SPEED),
                ModRow.slider(ModSetting.Slider.TOTEM_PARTICLE_COLOR),
                ModRow.section("arnestyles.mod.section.hitboxes", ModSetting.COMBAT_HITBOXES_MOD),
                ModRow.toggle(ModSetting.Toggle.HITBOXES),
                new ModRow(null, null, null, true, null, "hitbox_color"),
                ModRow.toggle(ModSetting.Toggle.HITBOX_TARGET_COLOR),
                new ModRow(null, null, null, true, null, "hitbox_target_color"),
                ModRow.toggle(ModSetting.Toggle.HITBOX_HURT_COLOR),
                new ModRow(null, null, null, true, null, "hitbox_hurt_color"),
                ModRow.toggle(ModSetting.Toggle.HITBOX_EYES),
                new ModRow(null, null, null, true, null, "hitbox_eye_color"),
                ModRow.toggle(ModSetting.Toggle.HITBOX_LOOK),
                ModRow.toggle(ModSetting.Toggle.HITBOX_LOOK_ARROW),
                new ModRow(null, null, null, true, null, "hitbox_look_color"),
                ModRow.slider(ModSetting.Slider.HITBOX_WIDTH),
                ModRow.slider(ModSetting.Slider.HITBOX_FAR_WIDTH),
                ModRow.slider(ModSetting.Slider.HITBOX_FAR_DISTANCE),
                ModRow.toggle(ModSetting.Toggle.HITBOX_OUTLINE),
                new ModRow(null, null, null, true, null, "hitbox_outline_color"),
                ModRow.slider(ModSetting.Slider.HITBOX_OUTLINE_SIZE),
                ModRow.toggle(ModSetting.Toggle.HITBOX_HIDE_FIREWORKS),
                ModRow.toggle(ModSetting.Toggle.HITBOX_HIDE_ITEMS),
                ModRow.toggle(ModSetting.Toggle.HIDE_STUCK_ARROWS),
                ModRow.section("arnestyles.mod.section.pop_chams", ModSetting.POP_CHAMS_MOD),
                ModRow.toggle(ModSetting.Toggle.POP_CHAMS),
                ModRow.toggle(ModSetting.Toggle.POP_CHAMS_OWN),
                ModRow.toggle(ModSetting.Toggle.POP_CHAMS_FADE),
                ModRow.toggle(ModSetting.Toggle.POP_CHAMS_DISPERSE),
                ModRow.slider(ModSetting.Slider.POP_CHAMS_LIFETIME),
                ModRow.slider(ModSetting.Slider.POP_CHAMS_OPACITY),
                new ModRow(null, null, null, true, null),
                ModRow.section("arnestyles.mod.section.shield_status", ModSetting.SHIELD_STATUS_MOD),
                ModRow.toggle(ModSetting.Toggle.SHIELD_STATUS),
                ModRow.toggle(ModSetting.Toggle.SHIELD_STATUS_OTHERS),
                ModRow.toggle(ModSetting.Toggle.SHIELD_STATUS_FADE),
                ModRow.section("arnestyles.mod.section.shield_fixes", null),
                ModRow.toggle(ModSetting.Toggle.SHIELD_FIX),
                ModRow.toggle(ModSetting.Toggle.SHIELD_FIX_DELAY),
                ModRow.toggle(ModSetting.Toggle.SHIELD_SOUNDS)
            )),
            new ModGroup("visuals", List.of(
                ModRow.section("arnestyles.mod.section.nametags", null),
                ModRow.toggle(ModSetting.Toggle.NAMETAGS),
                ModRow.toggle(ModSetting.Toggle.NAMETAG_HEALTH),
                ModRow.toggle(ModSetting.Toggle.NAMETAG_PING),
                ModRow.toggle(ModSetting.Toggle.NAMETAG_SHADOW),
                ModRow.slider(ModSetting.Slider.NAMETAG_SCALE),
                ModRow.slider(ModSetting.Slider.NAMETAG_BG_OPACITY),
                ModRow.section("arnestyles.mod.section.block_outline", null),
                ModRow.toggle(ModSetting.Toggle.BLOCK_OUTLINE),
                new ModRow(null, null, null, true, null, "outline_color"),
                ModRow.toggle(ModSetting.Toggle.OUTLINE_RAINBOW),
                ModRow.slider(ModSetting.Slider.OUTLINE_WIDTH),
                ModRow.section("arnestyles.mod.section.sky", null),
                ModRow.toggle(ModSetting.Toggle.CUSTOM_TIME),
                ModRow.slider(ModSetting.Slider.CUSTOM_TIME_VALUE),
                ModRow.toggle(ModSetting.Toggle.SKY_COLOR),
                new ModRow(null, null, null, true, null, "sky_color"),
                ModRow.section("arnestyles.mod.section.items", null),
                ModRow.toggle(ModSetting.Toggle.ITEM_PHYSICS),
                ModRow.toggle(ModSetting.Toggle.HIDE_OWN_EFFECT_PARTICLES),
                ModRow.section("arnestyles.mod.section.animations", null),
                ModRow.toggle(ModSetting.Toggle.ANIM_NO_COOLDOWN),
                ModRow.slider(ModSetting.Slider.ANIM_EQUIP),
                ModRow.slider(ModSetting.Slider.ANIM_SWING_SPEED),
                ModRow.slider(ModSetting.Slider.ANIM_SWING_MOVE),
                ModRow.slider(ModSetting.Slider.ANIM_SWING_ROTATION),
                ModRow.slider(ModSetting.Slider.ANIM_EAT),
                ModRow.toggle(ModSetting.Toggle.ANIM_EAT_BOB),
                ModRow.slider(ModSetting.Slider.HAND_X),
                ModRow.slider(ModSetting.Slider.HAND_Y),
                ModRow.slider(ModSetting.Slider.HAND_Z),
                ModRow.section("arnestyles.mod.section.motion_blur", ModSetting.MOTION_BLUR_MOD),
                ModRow.toggle(ModSetting.Toggle.MOTION_BLUR),
                ModRow.slider(ModSetting.Slider.MOTION_BLUR_STRENGTH),
                ModRow.section("arnestyles.mod.section.inventory_scale", null),
                ModRow.slider(ModSetting.Slider.INVENTORY_SCALE)
            )),
            new ModGroup("sounds", soundRows()),
            new ModGroup("comfort", List.of(
                ModRow.section("arnestyles.mod.section.zoom", ModSetting.ZOOMIFY_MOD),
                ModRow.toggle(ModSetting.Toggle.ZOOM),
                ModRow.slider(ModSetting.Slider.ZOOM_LEVEL),
                ModRow.toggle(ModSetting.Toggle.ZOOM_SMOOTH),
                ModRow.toggle(ModSetting.Toggle.ZOOM_SCROLL),
                ModRow.toggle(ModSetting.Toggle.ZOOM_SLOW_MOUSE),
                ModRow.section("arnestyles.mod.section.comfort", null),
                ModRow.toggle(ModSetting.Toggle.FREELOOK),
                ModRow.toggle(ModSetting.Toggle.FREELOOK_THIRD_PERSON),
                ModRow.toggle(ModSetting.Toggle.AUTO_GG),
                ModRow.toggle(ModSetting.Toggle.TOGGLE_SPRINT),
                ModRow.toggle(ModSetting.Toggle.TOGGLE_SNEAK),
                ModRow.toggle(ModSetting.Toggle.DEATH_POINT),
                ModRow.slider(ModSetting.Slider.DEATH_POINT_MINUTES),
                ModRow.toggle(ModSetting.Toggle.WAYPOINTS),
                ModRow.slider(ModSetting.Slider.WAYPOINT_COUNT),
                ModRow.section("arnestyles.mod.section.chat", null),
                ModRow.toggle(ModSetting.Toggle.CHAT_TIMESTAMPS),
                ModRow.toggle(ModSetting.Toggle.CHAT_SECONDS),
                new ModRow(null, null, null, true, null, "chat_time_color"),
                ModRow.toggle(ModSetting.Toggle.CHAT_STACK),
                ModRow.section("arnestyles.mod.section.no_chat_restrictions", ModSetting.NO_CHAT_RESTRICTIONS_MOD),
                ModRow.toggle(ModSetting.Toggle.NO_CHAT_RESTRICTIONS),
                ModRow.section("arnestyles.mod.section.borderless", ModSetting.CUBES_WITHOUT_BORDERS_MOD),
                ModRow.toggle(ModSetting.Toggle.BORDERLESS_FULLSCREEN),
                ModRow.section("arnestyles.mod.section.intro", null),
                ModRow.action("arnestyles.mod.show_intro")
            ))
        );
    }

    private static List<ModRow> soundRows() {
        List<ModRow> rows = new ArrayList<>();
        rows.add(ModRow.section("arnestyles.mod.section.sound_mixer", null));
        for (com.arnestyles.client.mods.SoundMixer.Group group : com.arnestyles.client.mods.SoundMixer.Group.values()) {
            rows.add(ModRow.slider(group));
        }
        return rows;
    }

    /** Sichtbare Zeilen: jede Oberkategorie als Kopfzeile, darunter ihre Mods, wenn sie aufgeklappt ist. */
    private static List<ModRow> modRows() {
        List<ModRow> rows = new ArrayList<>();
        for (ModGroup group : modGroups()) {
            rows.add(ModRow.action(GROUP_ROW + group.key()));
            if (OPEN_MOD_GROUPS.contains(group.key())) {
                rows.addAll(group.rows());
            }
        }
        return rows;
    }

    /** Eine Reihe Kacheln (Kachel-Ansicht der Mods): Name, darunter der Schalter. Klick schaltet um. */
    private @Nullable Text drawModTiles(DrawContext context, List<ModRow> line, int x, int y, int w, int cols, boolean modEnabled,
                                        int mouseX, int mouseY) {
        Text hovered = null;
        int gap = 4;
        int tw = (w - gap * (cols - 1)) / cols;
        int y2 = y + 2 * ESSENTIAL_ROW_H - 3;
        for (int c = 0; c < line.size(); c++) {
            ModSetting.Toggle toggle = line.get(c).toggle();
            if (toggle == null) {
                continue;
            }
            int tx = x + c * (tw + gap);
            boolean on = toggle.value();
            boolean over = inside(mouseX, mouseY, tx, y, tx + tw, y2);
            int fill = over ? C_CARD_HOVER : on ? GuiTheme.mix(C_CARD, C_ACCENT, 0.15f) : C_CARD;
            roundBordered(context, tx, y, tx + tw, y2, on ? C_ACCENT : over ? C_EDGE_HOVER : C_EDGE, fill);
            String name = this.textRenderer.trimToWidth(Text.translatable(toggle.nameKey()).getString(), tw - 10);
            text(context, name, tx + (tw - this.textRenderer.getWidth(name)) / 2, y + 6, toggle.blockedByMod() ? C_MUTED : C_TEXT);
            drawSwitch(context, tx + (tw - 22) / 2, y + 21, on, on && (!modEnabled || toggle.blockedByMod()));
            hits.add(new Hit(tx, y, tx + tw, y2, toggle::toggle));
            if (over) {
                inspectToggle = toggle;
                inspectSlider = null;
                hovered = toggle.blockedByMod()
                    ? Text.translatable("arnestyles.gui.mod_blocked.desc", toggle.externalMod())
                    : Text.translatable(toggle.descriptionKey());
            }
        }
        return hovered;
    }

    private void drawMods(DrawContext context, int mouseX, int mouseY) {
        ArneStylesConfig config = ConfigManager.get();
        int headY = top + HEADER_H + 7;
        Text modsTitle = Text.translatable("arnestyles.gui.mods");
        drawBigText(context, modsTitle, paneX, headY, 1.3f, C_TEXT);
        drawModSearch(context, paneX + Math.round(this.textRenderer.getWidth(modsTitle) * 1.3f), headY, mouseX, mouseY);
        boolean searching = modSearching();

        int view = GuiLayout.Option.MODS_VIEW.value();
        int paneW = paneRight - paneX;
        int listTopY = top + HEADER_H + 42;
        Text hovered = null;
        List<ModRow> rows;
        if (searching) {
            rows = filteredModRows();
            if (rows.isEmpty()) {
                text(context, Text.translatable("arnestyles.mods.no_results"), paneX, listTopY + 4, C_MUTED);
            }
        } else if (view == 0) {
            rows = modRows();
        } else {
            // Reiter fuer die Oberkategorien, darunter nur deren Mods
            if (modGroups().stream().noneMatch(g -> g.key().equals(modTab))) {
                modTab = modGroups().get(0).key();
            }
            int cx = paneX;
            for (ModGroup group : modGroups()) {
                Text label = Text.translatable("arnestyles.mod.group." + group.key());
                int cw = this.textRenderer.getWidth(label) + 14;
                boolean selected = group.key().equals(modTab);
                boolean over = inside(mouseX, mouseY, cx, listTopY, cx + cw, listTopY + 14);
                if (selected) {
                    roundBordered(context, cx, listTopY, cx + cw, listTopY + 14, C_ACCENT, C_ACCENT);
                } else {
                    roundBordered(context, cx, listTopY, cx + cw, listTopY + 14, over ? C_ACCENT : C_EDGE, over ? C_CARD_HOVER : C_CARD);
                }
                text(context, label, cx + 7, listTopY + 3, selected ? onAccent() : C_TEXT);
                if (over) {
                    hovered = Text.translatable("arnestyles.mod.group." + group.key() + ".desc");
                }
                String key = group.key();
                hits.add(new Hit(cx, listTopY, cx + cw, listTopY + 14, () -> {
                    modTab = key;
                    cardScroll = 0;
                }));
                cx += cw + 4;
            }
            listTopY += 18;
            rows = modGroups().stream().filter(g -> g.key().equals(modTab)).findFirst().orElseThrow().rows();
        }
        if (view == 3) {
            Text dashHover = drawModDashboard(context, rows, paneX, listTopY, paneW, cardsBottom, mouseX, mouseY);
            if (dashHover != null) {
                hovered = dashHover;
            }
            if (hudEditorHint && hovered == null) {
                hovered = Text.translatable("arnestyles.mod.hud_editor.no_world");
            }
            String dashInfo = hovered != null ? hovered.getString() : Text.translatable("arnestyles.gui.mods_info").getString();
            text(context, this.textRenderer.trimToWidth(dashInfo, paneRight - paneX), paneX, top + HEADER_H + 26,
                hovered != null ? C_TEXT : C_MUTED);
            return;
        }
        // Zeilen bilden: bei Kacheln stehen mehrere Schalter nebeneinander
        int tileCols = Math.max(2, Math.min(4, paneW / 105));
        List<List<ModRow>> lines = new ArrayList<>();
        for (ModRow row : rows) {
            List<ModRow> last = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (view == 2 && row.toggle() != null && last != null && last.get(0).toggle() != null && last.size() < tileCols) {
                last.add(row);
            } else {
                List<ModRow> line = new ArrayList<>();
                line.add(row);
                lines.add(line);
            }
        }
        modLineCount = lines.size();
        cardScroll = Math.max(0, Math.min(Math.max(0, lines.size() - 1), cardScroll));
        int lineY = listTopY;
        for (int li = cardScroll; li < lines.size(); li++) {
            List<ModRow> line = lines.get(li);
            boolean tiles = view == 2 && line.get(0).toggle() != null;
            int lineH = tiles ? 2 * ESSENTIAL_ROW_H : modRowHeight(line.get(0), paneW);
            if (lineY + lineH - 2 > cardsBottom + 1) {
                break;
            }
            if (tiles) {
                Text tileHover = drawModTiles(context, line, paneX, lineY, paneW, tileCols, config.modEnabled, mouseX, mouseY);
                if (tileHover != null) {
                    hovered = tileHover;
                }
                lineY += lineH;
                continue;
            }
            ModRow row = line.get(0);
            boolean groupRow = row.action() != null && row.action().startsWith(GROUP_ROW);
            // Mods in einer Oberkategorie leicht eingerueckt
            int x = groupRow || view > 0 || searching ? paneX : paneX + 8;
            int w = groupRow || view > 0 || searching ? paneW : paneW - 8;
            int y = lineY;
            lineY += lineH;
            Text rowHover = drawModRow(context, row, x, y, w, mouseX, mouseY);
            if (rowHover != null) {
                hovered = rowHover;
            }
        }
        if (hudEditorHint && hovered == null) {
            hovered = Text.translatable("arnestyles.mod.hud_editor.no_world");
        }
        String info = hovered != null ? hovered.getString() : Text.translatable("arnestyles.gui.mods_info").getString();
        text(context, this.textRenderer.trimToWidth(info, paneRight - paneX), paneX, top + HEADER_H + 26,
            hovered != null ? C_TEXT : C_MUTED);
    }

    /** Breite, unter der Farbzeilen zweizeilig werden (Name oben, Farben unten). */
    private static final int COLOR_ROW_SPLIT = 330;

    private static int modRowHeight(ModRow row, int w) {
        return row.color() && w < COLOR_ROW_SPLIT ? 2 * ESSENTIAL_ROW_H : ESSENTIAL_ROW_H;
    }

    /** Zeichnet eine Zeile der Mods-Seite (Schalter, Regler, Farbe, Aktion, ...). Gibt die Erklaerung unter der Maus zurueck. */
    private @Nullable Text drawModRow(DrawContext context, ModRow row, int x, int y, int w, int mouseX, int mouseY) {
        ArneStylesConfig config = ConfigManager.get();
        Text hovered = null;
        int y2 = y + ESSENTIAL_ROW_H - 2;
        boolean groupRow = row.action() != null && row.action().startsWith(GROUP_ROW);
        if (groupRow) {
            String groupKey = row.action().substring(GROUP_ROW.length());
            ModGroup group = modGroups().stream().filter(g -> g.key().equals(groupKey)).findFirst().orElseThrow();
            boolean open = OPEN_MOD_GROUPS.contains(groupKey);
            boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
            if (over) {
                hovered = Text.translatable("arnestyles.mod.group." + groupKey + ".desc");
            }
            roundBordered(context, x, y, x + w, y2, over || open ? C_ACCENT : C_EDGE, over ? C_CARD_HOVER : C_CARD);
            // Pfeil: zeigt nach rechts (zu) oder nach unten (offen)
            int ax = x + 8;
            int ay = y + 5;
            for (int k = 0; k < 4; k++) {
                if (open) {
                    context.fill(ax + k, ay + 2 + k, ax + 7 - k, ay + 3 + k, C_ACCENT);
                } else {
                    context.fill(ax + 1 + k, ay + k, ax + 2 + k, ay + 7 - k, C_ACCENT);
                }
            }
            text(context, Text.translatable("arnestyles.mod.group." + groupKey), x + 20, y + 5, C_TEXT);
            // Sound-Mixer: Anzahl Regler und wie viele davon veraendert sind
            long changedSounds = "sounds".equals(group.key()) ? ConfigManager.get().soundVolumes.size() : 0;
            Text side = "sounds".equals(group.key())
                ? Text.translatable("arnestyles.mod.group.sound_count", com.arnestyles.client.mods.SoundMixer.Group.values().length, changedSounds)
                : Text.translatable("arnestyles.mod.group.count", group.mods(), group.activeToggles());
            text(context, side, x + w - 8 - this.textRenderer.getWidth(side), y + 5, C_MUTED);
            hits.add(new Hit(x, y, x + w, y2, () -> {
                if (!OPEN_MOD_GROUPS.remove(groupKey)) {
                    OPEN_MOD_GROUPS.add(groupKey);
                }
            }));
        } else if (POP_SOUND_ROW.equals(row.action())) {
            // Pop-Sound: Name mit Pfeilen zum Durchblaettern, Klick auf den Namen spielt ihn ab
            boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
            if (over) {
                hovered = Text.translatable("arnestyles.mod.totem_pop_sound.desc");
            }
            round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
            text(context, Text.translatable("arnestyles.mod.totem_pop_sound"), x + 8, y + 5, C_TEXT);
            int index = PvpVisuals.popSoundIndex();
            int nameW = 0;
            for (String soundKey : PvpVisuals.POP_SOUND_KEYS) {
                nameW = Math.max(nameW, this.textRenderer.getWidth(Text.translatable("arnestyles.mod.totem_pop_sound." + soundKey)));
            }
            int px = drawPillRight(context, x + w - 4, y + 2, Text.literal(">"), C_TEXT, mouseX, mouseY,
                () -> PvpVisuals.selectPopSound(PvpVisuals.popSoundIndex() + 1));
            int nx2 = px - 4;
            int nx1 = nx2 - nameW - 22;
            boolean overName = inside(mouseX, mouseY, nx1, y + 2, nx2, y + 16);
            roundBordered(context, nx1, y + 2, nx2, y + 16, overName ? C_ACCENT : C_EDGE, C_FIELD);
            Text name = Text.translatable("arnestyles.mod.totem_pop_sound." + PvpVisuals.POP_SOUND_KEYS[index]);
            text(context, name, nx1 + 6, y + 5, C_ACCENT);
            // kleines Lautsprecher-Zeichen zum Anhoeren
            int sx = nx2 - 12;
            context.fill(sx, y + 7, sx + 2, y + 11, overName ? C_ACCENT : C_MUTED);
            context.fill(sx + 2, y + 6, sx + 3, y + 12, overName ? C_ACCENT : C_MUTED);
            context.fill(sx + 3, y + 5, sx + 4, y + 13, overName ? C_ACCENT : C_MUTED);
            context.fill(sx + 6, y + 7, sx + 7, y + 11, overName ? C_ACCENT : C_MUTED);
            if (overName) {
                hovered = Text.translatable("arnestyles.mod.totem_pop_sound.play", index + 1, PvpVisuals.POP_SOUNDS.length);
            }
            hits.add(new Hit(nx1, y + 2, nx2, y + 16, PvpVisuals::previewPopSound));
            drawPillRight(context, nx1 - 4, y + 2, Text.literal("<"), C_TEXT, mouseX, mouseY,
                () -> PvpVisuals.selectPopSound(PvpVisuals.popSoundIndex() - 1));
        } else if (row.section() != null) {
            int textEnd = x + 2 + this.textRenderer.getWidth(Text.translatable(row.section()));
            text(context, Text.translatable(row.section()), x + 2, y + 6, C_ACCENT);
            if (ModSetting.isLoaded(row.blockedBy())) {
                // Die einzelne Mod ist installiert: die eingebaute Version bleibt aus, damit nichts doppelt kommt
                Text note = Text.translatable("arnestyles.gui.mod_blocked");
                text(context, note, textEnd + 6, y + 6, C_MOD_BLOCKED);
                textEnd += 6 + this.textRenderer.getWidth(note);
                if (inside(mouseX, mouseY, x, y, x + w, y2)) {
                    hovered = Text.translatable("arnestyles.gui.mod_blocked.desc", row.blockedBy());
                }
            }
            context.fill(textEnd + 6, y + 10, x + w, y + 11, C_EDGE);
        } else if (row.toggle() != null) {
            ModSetting.Toggle toggle = row.toggle();
            boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
            if (over) {
                inspectToggle = toggle;
                inspectSlider = null;
                hovered = toggle.blockedByMod()
                    ? Text.translatable("arnestyles.gui.mod_blocked.desc", toggle.externalMod())
                    : Text.translatable(toggle.descriptionKey());
            }
            round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
            text(context, Text.translatable(toggle.nameKey()), x + 8, y + 5,
                toggle.blockedByMod() ? C_MUTED : C_TEXT);
            drawSwitch(context, x + w - 28, y + 3, toggle.value(), toggle.value() && (!config.modEnabled || toggle.blockedByMod()));
            hits.add(new Hit(x, y, x + w, y2, toggle::toggle));
        } else if (row.slider() != null) {
            if (drawSliderRow(context, row.slider(), x, y, w, config.modEnabled, mouseX, mouseY, null)) {
                inspectSlider = row.slider();
                inspectToggle = null;
                hovered = Text.translatable(row.slider().descriptionKey());
            }
        } else if (row.action() != null && !row.color()) {
            boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
            if (over) {
                hovered = Text.translatable(row.action() + ".desc");
            }
            round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
            text(context, Text.translatable(row.action()), x + 8, y + 5, C_TEXT);
            Text open = Text.translatable("arnestyles.gui.open");
            text(context, open, x + w - 8 - this.textRenderer.getWidth(open), y + 5, over ? C_ACCENT : C_MUTED);
            hits.add(new Hit(x, y, x + w, y2, actionFor(row.action())));
        } else if (row.color()) {
            // Farbzeile: Pop Chams (ohne Schluessel), Treffer-Farbe, Umrandung oder Himmel
            String colorKey = row.action() == null ? "pop_chams_color" : row.action();
            String nameKey = colorNameKey(colorKey);
            int[] palette = "sky_color".equals(colorKey) ? WorldVisuals.SKY_COLORS
                : colorKey.startsWith("hitbox_") ? com.arnestyles.client.mods.CombatHitboxes.COLORS : ModSetting.POP_CHAMS_COLORS;
            int current = colorValue(config, colorKey) & 0xFFFFFF;
            boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
            if (over) {
                hovered = Text.translatable(nameKey + ".desc");
            }
            boolean tall = w < COLOR_ROW_SPLIT;
            if (tall) {
                y2 = y + 2 * ESSENTIAL_ROW_H - 2;
                over = inside(mouseX, mouseY, x, y, x + w, y2);
            }
            round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
            text(context, Text.translatable(nameKey), x + 8, y + 5, C_TEXT);
            drawHexField(context, colorKey, current, x + w - 6, y + 2, mouseX, mouseY);
            int sx = x + w - 6;
            int sy = y;
            if (tall) {
                // Schmal: Farben in der zweiten Zeile, links beginnend
                sy = y + ESSENTIAL_ROW_H - 1;
                sx = x + 8 + palette.length * 15;
            } else {
                sx = x + w - 66;
            }
            int rowY2 = sy + ESSENTIAL_ROW_H - 2;
            for (int c = palette.length - 1; c >= 0; c--) {
                int rgb = palette[c];
                int x1 = sx - 12;
                if (current == rgb) {
                    round(context, x1 - 1, sy + 2, sx + 1, rowY2 - 2, C_TEXT);
                }
                round(context, x1, sy + 3, sx, rowY2 - 3, 0xFF000000 | rgb);
                final int pick = rgb;
                hits.add(new Hit(x1, sy + 2, sx, rowY2 - 2, () -> setModColor(colorKey, pick)));
                sx = x1 - 3;
            }
        }
        return hovered;
    }

    private static String colorNameKey(String colorKey) {
        com.arnestyles.client.mods.ModColors.Entry entry = com.arnestyles.client.mods.ModColors.get(colorKey);
        if (entry != null) {
            return entry.nameKey();
        }
        return switch (colorKey) {
            case "hit_color" -> "arnestyles.mod.hit_color_pick";
            case "outline_color" -> "arnestyles.mod.outline_color";
            case "crosshair_target_color" -> "arnestyles.mod.crosshair_target_color";
            case "sky_color" -> "arnestyles.mod.sky_color_pick";
            case "hitbox_color", "hitbox_target_color", "hitbox_hurt_color", "hitbox_eye_color", "hitbox_look_color",
                 "hitbox_outline_color" -> "arnestyles.mod." + colorKey + "_pick";
            default -> "arnestyles.mod.pop_chams_color";
        };
    }

    private static int colorValue(ArneStylesConfig config, String colorKey) {
        com.arnestyles.client.mods.ModColors.Entry entry = com.arnestyles.client.mods.ModColors.get(colorKey);
        if (entry != null) {
            return entry.getter().applyAsInt(config);
        }
        return switch (colorKey) {
            case "hit_color" -> config.hitColorRgb;
            case "outline_color" -> config.outlineColorRgb;
            case "crosshair_target_color" -> config.crosshairTargetRgb;
            case "sky_color" -> config.skyColorRgb;
            case "hitbox_color" -> config.hitboxRgb;
            case "hitbox_target_color" -> config.hitboxTargetRgb;
            case "hitbox_hurt_color" -> config.hitboxHurtRgb;
            case "hitbox_eye_color" -> config.hitboxEyeRgb;
            case "hitbox_look_color" -> config.hitboxLookRgb;
            case "hitbox_outline_color" -> config.hitboxOutlineRgb;
            default -> config.popChamsColor;
        };
    }

    private static void setModColor(String colorKey, int rgb) {
        ArneStylesConfig edit = ConfigManager.get();
        int pick = rgb & 0xFFFFFF;
        com.arnestyles.client.mods.ModColors.Entry entry = com.arnestyles.client.mods.ModColors.get(colorKey);
        if (entry != null) {
            entry.setter().accept(edit, pick);
            ConfigManager.save();
            return;
        }
        switch (colorKey) {
            case "hit_color" -> edit.hitColorRgb = pick;
            case "outline_color" -> edit.outlineColorRgb = pick;
            case "crosshair_target_color" -> edit.crosshairTargetRgb = pick;
            case "sky_color" -> edit.skyColorRgb = pick;
            case "hitbox_color" -> edit.hitboxRgb = pick;
            case "hitbox_target_color" -> edit.hitboxTargetRgb = pick;
            case "hitbox_hurt_color" -> edit.hitboxHurtRgb = pick;
            case "hitbox_eye_color" -> edit.hitboxEyeRgb = pick;
            case "hitbox_look_color" -> edit.hitboxLookRgb = pick;
            case "hitbox_outline_color" -> edit.hitboxOutlineRgb = pick;
            default -> edit.popChamsColor = pick;
        }
        ConfigManager.save();
    }

    /** Hex-Feld "#RRGGBB" rechtsbuendig bei x2: Klick zum Eintippen, Enter uebernimmt, Esc bricht ab. */
    private void drawHexField(DrawContext context, String colorKey, int rgb, int x2, int y, int mouseX, int mouseY) {
        boolean editing = colorKey.equals(hexEditKey);
        String shown = editing ? "#" + hexBuffer + ((System.currentTimeMillis() / 400) % 2 == 0 ? "_" : " ")
            : String.format(Locale.ROOT, "#%06X", rgb);
        int x1 = x2 - 58;
        boolean over = inside(mouseX, mouseY, x1, y, x2, y + 14);
        roundBordered(context, x1, y, x2, y + 14, editing ? C_ACCENT2 : over ? C_EDGE_HOVER : C_EDGE, C_FIELD);
        context.fill(x1 + 3, y + 3, x1 + 11, y + 11, 0xFF000000 | rgb);
        text(context, shown, x1 + 14, y + 3, editing ? C_TEXT : C_TEXT_SOFT);
        hits.add(new Hit(x1, y, x2, y + 14, () -> {
            hexEditKey = colorKey;
            hexBuffer.setLength(0);
        }));
    }

    private void applyHex() {
        if (hexEditKey != null && hexBuffer.length() == 6) {
            try {
                setModColor(hexEditKey, Integer.parseInt(hexBuffer.toString(), 16));
            } catch (NumberFormatException ignored) {
                // nur 0-9 und A-F werden angenommen
            }
        }
        hexEditKey = null;
        hexBuffer.setLength(0);
    }

    // ---- Suche und Filter ----

    private boolean modSearching() {
        return modSearch.length() > 0 || modFilter != 0;
    }

    private static String rowNameKey(ModRow row) {
        if (row.toggle() != null) {
            return row.toggle().nameKey();
        }
        if (row.slider() != null) {
            return row.slider().nameKey();
        }
        if (row.color()) {
            return colorNameKey(row.action() == null ? "pop_chams_color" : row.action());
        }
        if (POP_SOUND_ROW.equals(row.action())) {
            return "arnestyles.mod.totem_pop_sound";
        }
        return row.action() == null ? "" : row.action();
    }

    private boolean rowMatches(ModRow row, String query) {
        if (modFilter != 0) {
            if (row.toggle() == null || row.toggle().value() != (modFilter == 1)) {
                return false;
            }
        }
        if (query.isEmpty()) {
            return true;
        }
        String key = rowNameKey(row);
        if (key.isEmpty()) {
            return false;
        }
        if (Text.translatable(key).getString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        return Text.translatable(key + ".desc").getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** Alle Mods aller Gruppen, die zu Suche und Filter passen (mit ihren Ueberschriften). */
    private List<ModRow> filteredModRows() {
        String query = modSearch.toString().toLowerCase(Locale.ROOT).trim();
        List<ModRow> out = new ArrayList<>();
        for (ModGroup group : modGroups()) {
            ModRow section = null;
            for (ModRow row : group.rows()) {
                if (row.section() != null) {
                    section = row;
                    continue;
                }
                if (!rowMatches(row, query)) {
                    continue;
                }
                if (section != null) {
                    out.add(section);
                    section = null;
                }
                out.add(row);
            }
        }
        return out;
    }

    /** Suchfeld und Filter-Chips in der Kopfzeile der Mods-Seite. */
    private void drawModSearch(DrawContext context, int titleEnd, int headY, int mouseX, int mouseY) {
        String[] filters = {"all", "on", "off"};
        int fx = paneRight;
        for (int i = filters.length - 1; i >= 0; i--) {
            Text label = Text.translatable("arnestyles.mods.filter." + filters[i]);
            int cw = this.textRenderer.getWidth(label) + 12;
            boolean selected = modFilter == i;
            boolean over = inside(mouseX, mouseY, fx - cw, headY - 2, fx, headY + 12);
            roundBordered(context, fx - cw, headY - 2, fx, headY + 12, selected ? C_ACCENT : over ? C_EDGE_HOVER : C_EDGE,
                selected ? GuiTheme.mix(C_CARD, C_ACCENT, 0.25f) : over ? C_CARD_HOVER : C_CARD);
            text(context, label, fx - cw + 6, headY + 1, selected ? C_TEXT : C_MUTED);
            final int pick = i;
            hits.add(new Hit(fx - cw, headY - 2, fx, headY + 12, () -> {
                modFilter = pick;
                cardScroll = 0;
            }));
            fx -= cw + 3;
        }
        int sx1 = titleEnd + 12;
        int sx2 = Math.min(fx - 8, sx1 + 170);
        if (sx2 - sx1 < 60) {
            return;
        }
        boolean active = modSearch.length() > 0;
        roundBordered(context, sx1, headY - 2, sx2, headY + 12, active ? C_ACCENT : C_EDGE, C_FIELD);
        // Lupe
        context.fill(sx1 + 5, headY + 1, sx1 + 9, headY + 2, C_MUTED);
        context.fill(sx1 + 4, headY + 2, sx1 + 5, headY + 6, C_MUTED);
        context.fill(sx1 + 9, headY + 2, sx1 + 10, headY + 6, C_MUTED);
        context.fill(sx1 + 5, headY + 6, sx1 + 9, headY + 7, C_MUTED);
        context.fill(sx1 + 9, headY + 7, sx1 + 11, headY + 9, C_MUTED);
        String shown = active ? modSearch + ((System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "")
            : Text.translatable("arnestyles.mods.search").getString();
        text(context, this.textRenderer.trimToWidth(shown, sx2 - sx1 - 30), sx1 + 15, headY + 1, active ? C_TEXT : C_MUTED);
        if (active) {
            boolean overX = inside(mouseX, mouseY, sx2 - 12, headY - 2, sx2, headY + 12);
            text(context, "x", sx2 - 9, headY + 1, overX ? C_DANGER : C_MUTED);
            hits.add(new Hit(sx2 - 12, headY - 2, sx2, headY + 12, () -> {
                modSearch.setLength(0);
                cardScroll = 0;
            }));
        }
    }

    // ---- Dashboard-Ansicht: Bloecke je Ueberschrift in mehreren Spalten ----

    private @Nullable Text drawModDashboard(DrawContext context, List<ModRow> rows, int x, int y, int w, int yBottom,
                                            int mouseX, int mouseY) {
        // Bloecke bilden: jede Ueberschrift beginnt einen neuen Block
        List<List<ModRow>> blocks = new ArrayList<>();
        for (ModRow row : rows) {
            if (row.section() != null || blocks.isEmpty()) {
                blocks.add(new ArrayList<>());
            }
            blocks.get(blocks.size() - 1).add(row);
        }
        int gap = 8;
        int cols = Math.max(1, Math.min(3, (w + gap) / 240));
        int colW = (w - (cols - 1) * gap) / cols;
        int[] colY = new int[cols];
        int[] bx = new int[blocks.size()];
        int[] byPos = new int[blocks.size()];
        int[] bh = new int[blocks.size()];
        for (int b = 0; b < blocks.size(); b++) {
            List<ModRow> block = blocks.get(b);
            int h = block.get(0).section() != null ? 18 : 4;
            for (ModRow row : block) {
                if (row.section() == null) {
                    h += modRowHeight(row, colW - 10);
                }
            }
            h += 4;
            int col = 0;
            for (int c = 1; c < cols; c++) {
                if (colY[c] < colY[col]) {
                    col = c;
                }
            }
            bx[b] = x + col * (colW + gap);
            byPos[b] = colY[col];
            bh[b] = h;
            colY[col] += h + gap;
        }
        int total = 0;
        for (int c : colY) {
            total = Math.max(total, c);
        }
        modLineCount = Math.max(1, (total - (yBottom - y)) / ESSENTIAL_ROW_H + 2);
        cardScroll = Math.max(0, Math.min(modLineCount - 1, cardScroll));
        int offset = cardScroll * ESSENTIAL_ROW_H;
        int hitStart = hits.size();
        int sliderStart = sliderHits.size();
        Text hovered = null;
        int blockFill = GuiTheme.mix(C_PANEL, C_CARD, 0.45f) | 0xFF000000;
        context.enableScissor(x - 2, y, x + w + 2, yBottom);
        for (int b = 0; b < blocks.size(); b++) {
            int by = y + byPos[b] - offset;
            if (by > yBottom || by + bh[b] < y) {
                continue;
            }
            List<ModRow> block = blocks.get(b);
            int x1 = bx[b];
            roundBordered(context, x1, by, x1 + colW, by + bh[b], C_EDGE, blockFill);
            int ry = by + 4;
            ModRow head = block.get(0);
            if (head.section() != null) {
                text(context, this.textRenderer.trimToWidth(Text.translatable(head.section()).getString(), colW - 60), x1 + 6, by + 5,
                    C_ACCENT);
                if (ModSetting.isLoaded(head.blockedBy())) {
                    text(context, Text.translatable("arnestyles.gui.mod_blocked"), x1 + 6, by + 5, C_MOD_BLOCKED);
                }
                // Reset des Blocks (zweimal klicken)
                String armedKey = head.section();
                boolean armed = armedKey.equals(blockResetArmed);
                Text reset = Text.translatable(armed ? "arnestyles.mods.reset_confirm" : "arnestyles.mods.reset");
                int rw = this.textRenderer.getWidth(reset);
                int rx = x1 + colW - 6 - rw;
                boolean overReset = inside(mouseX, mouseY, rx - 3, by + 2, x1 + colW - 3, by + 15);
                text(context, reset, rx, by + 5, armed ? C_DANGER : overReset ? C_TEXT : C_MUTED);
                hits.add(new Hit(rx - 3, by + 2, x1 + colW - 3, by + 15, () -> {
                    if (armedKey.equals(blockResetArmed)) {
                        resetBlock(block);
                        blockResetArmed = null;
                    } else {
                        blockResetArmed = armedKey;
                    }
                }));
                context.fill(x1 + 6, by + 15, x1 + colW - 6, by + 16, C_EDGE);
                ry = by + 18;
            }
            for (ModRow row : block) {
                if (row.section() != null) {
                    continue;
                }
                Text h = drawModRow(context, row, x1 + 5, ry, colW - 10, mouseX, mouseY);
                if (h != null && mouseY >= y && mouseY < yBottom) {
                    hovered = h;
                }
                ry += modRowHeight(row, colW - 10);
            }
        }
        context.disableScissor();
        // Klickflaechen ausserhalb des sichtbaren Bereichs entfernen
        for (int i = hits.size() - 1; i >= hitStart; i--) {
            Hit hit = hits.get(i);
            if (hit.y1() < y || hit.y2() > yBottom) {
                hits.remove(i);
            }
        }
        for (int i = sliderHits.size() - 1; i >= sliderStart; i--) {
            SliderHit hit = sliderHits.get(i);
            if (hit.y1() < y || hit.y2() > yBottom) {
                sliderHits.remove(i);
            }
        }
        return hovered;
    }

    /** Setzt alle Schalter, Regler und Farben eines Blocks auf den Standard. */
    private void resetBlock(List<ModRow> block) {
        ArneStylesConfig defaults = new ArneStylesConfig();
        for (ModRow row : block) {
            if (row.toggle() != null) {
                row.toggle().reset();
            } else if (row.slider() != null) {
                row.slider().reset();
            } else if (row.color()) {
                String key = row.action() == null ? "pop_chams_color" : row.action();
                setModColor(key, colorValue(defaults, key));
            }
        }
        ConfigManager.save();
    }

    // ---- Eindeutige Vorschauen ----

    /**
     * Waehlt fuer jeden Style der aktuellen Kategorie eine Vorschau-Textur, die kein anderer Style zeigt.
     * Zuerst die typischen Texturen der Kategorie, dann jede andere Textur des Styles.
     */
    private void computeUniquePreviews() {
        String marker = current.name() + "/#done";
        if (uniquePreviewCache.containsKey(marker)) {
            return;
        }
        java.util.Set<Long> used = new java.util.HashSet<>();
        for (StyleDefinition style : StyleRegistry.getStyles(current)) {
            if (style.isVanilla()) {
                continue;
            }
            Path mc = style.root().resolve("assets").resolve("minecraft");
            List<String> candidates = new ArrayList<>();
            for (String asset : current.previewAssets()) {
                if (Files.isRegularFile(mc.resolve(asset))) {
                    candidates.add(asset);
                }
            }
            Path textures = mc.resolve("textures");
            if (Files.isDirectory(textures)) {
                try (var walk = Files.walk(textures)) {
                    walk.filter(f -> f.toString().endsWith(".png")).limit(400).sorted()
                        .forEach(f -> {
                            String rel = mc.relativize(f).toString().replace('\\', '/');
                            if (!candidates.contains(rel)) {
                                candidates.add(rel);
                            }
                        });
                } catch (java.io.IOException e) {
                    // dann eben nur die bekannten Texturen
                }
            }
            String chosen = "";
            for (String candidate : candidates) {
                long hash = fileHash(mc.resolve(candidate));
                if (chosen.isEmpty()) {
                    chosen = candidate;
                }
                if (used.add(hash)) {
                    chosen = candidate;
                    break;
                }
            }
            uniquePreviewCache.put(current.name() + "/" + style.id(), chosen);
        }
        uniquePreviewCache.put(marker, "");
    }

    private static long fileHash(Path file) {
        try {
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(Files.readAllBytes(file));
            return crc.getValue();
        } catch (java.io.IOException e) {
            return file.hashCode();
        }
    }

    /** true, wenn der Style in dieser Kategorie keine Texturen hat (aendert nur Modelle, z. B. die Haltung). */
    private boolean modelOnly(StyleDefinition style) {
        if (style.isVanilla()) {
            return false;
        }
        computeUniquePreviews();
        return "".equals(uniquePreviewCache.get(current.name() + "/" + style.id()));
    }

    // ---- Presets ----

    /** Menue direkt auf der Mods-Seite oeffnen (Selbsttest). */
    public static StylesScreen onMods() {
        StylesScreen screen = new StylesScreen();
        screen.openMods();
        return screen;
    }

    /** Menue direkt auf der Shader-Seite oeffnen (Selbsttest). */
    public static StylesScreen openShaderPage() {
        StylesScreen screen = new StylesScreen();
        screen.openShaders();
        return screen;
    }

    /** Menue direkt auf der Presets-Seite oeffnen (Selbsttest). */
    public static StylesScreen onPresets() {
        StylesScreen screen = new StylesScreen();
        screen.openPresets();
        return screen;
    }

    private void openPresets() {
        presetsOpen = true;
        themesOpen = false;
        importOpen = false;
        modsOpen = false;
        shadersOpen = false;
        presetMessage = null;
        deleteArmed = null;
        cardScroll = 0;
    }

    private void savePreset() {
        String name = PresetManager.cleanName(presetName.toString());
        if (name.isEmpty()) {
            presetMessage = Text.translatable("arnestyles.gui.preset_need_name").getString();
            presetMessageColor = C_WARN;
            return;
        }
        if (keybindPage ? com.arnestyles.client.config.KeybindSets.save(name) : PresetManager.save(name)) {
            presetMessage = Text.translatable("arnestyles.gui.preset_saved", name).getString();
            presetMessageColor = C_OK;
            presetName.setLength(0);
        } else {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
            presetMessageColor = C_DANGER;
        }
    }

    /** Preset als Code in die Zwischenablage (zum Teilen in Discord usw.). */
    private void copyPresetCode(PresetManager.Preset preset) {
        String code = PresetManager.exportCode(preset);
        if (code == null || this.client == null) {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
            presetMessageColor = C_DANGER;
            return;
        }
        this.client.keyboard.setClipboard(code);
        presetMessage = Text.translatable("arnestyles.gui.preset_copied", preset.name()).getString();
        presetMessageColor = C_OK;
    }

    /** Alle aktuellen Einstellungen als Code in die Zwischenablage (Name = eigener Spielername). */
    private void copyCurrentCode() {
        if (this.client == null) {
            return;
        }
        String code = PresetManager.currentCode(this.client.getSession().getUsername());
        if (code == null) {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
            presetMessageColor = C_DANGER;
            return;
        }
        this.client.keyboard.setClipboard(code);
        presetMessage = Text.translatable("arnestyles.gui.share_copied", code.length()).getString();
        presetMessageColor = C_OK;
    }

    /** Preset-Code aus der Zwischenablage als neues Preset speichern. */
    private void pastePresetCode() {
        String clip = this.client == null ? null : this.client.keyboard.getClipboard();
        String name = clip == null ? null : keybindPage ? com.arnestyles.client.config.KeybindSets.importCode(clip) : PresetManager.importCode(clip);
        if (name == null) {
            presetMessage = Text.translatable("arnestyles.gui.preset_bad_code").getString();
            presetMessageColor = C_WARN;
            return;
        }
        presetMessage = Text.translatable("arnestyles.gui.preset_imported", name).getString();
        presetMessageColor = C_OK;
    }

    private void loadPreset(PresetManager.Preset preset) {
        if (PresetManager.load(preset)) {
            presetMessage = Text.translatable("arnestyles.gui.preset_loaded", preset.name()).getString();
            presetMessageColor = C_OK;
            markDirty();
            previewAssetCache.clear();
        } else {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
            presetMessageColor = C_DANGER;
        }
    }

    /** Umbenennen: der neue Name kommt aus dem Eingabefeld oben. */
    private void renamePreset(PresetManager.Preset preset) {
        if (presetName.length() == 0) {
            presetMessage = Text.translatable("arnestyles.gui.preset_rename_hint").getString();
            presetMessageColor = C_WARN;
            return;
        }
        String newName = presetName.toString();
        if (PresetManager.rename(preset, newName)) {
            presetMessage = Text.translatable("arnestyles.gui.preset_renamed", preset.name(), PresetManager.cleanName(newName)).getString();
            presetMessageColor = C_OK;
            presetName.setLength(0);
        } else {
            presetMessage = Text.translatable("arnestyles.gui.preset_rename_failed").getString();
            presetMessageColor = C_DANGER;
        }
    }

    private void deletePreset(PresetManager.Preset preset) {
        if (!preset.name().equals(deleteArmed)) {
            deleteArmed = preset.name();
            return;
        }
        PresetManager.delete(preset);
        deleteArmed = null;
        presetMessage = Text.translatable("arnestyles.gui.preset_deleted", preset.name()).getString();
        presetMessageColor = C_MUTED;
    }

    /** Keybind-Set laden, als Code kopieren oder loeschen (Presets-Seite, Reiter Tasten). */
    private void loadKeySet(com.arnestyles.client.config.KeybindSets.KeySet set) {
        boolean ok = com.arnestyles.client.config.KeybindSets.load(set);
        presetMessage = Text.translatable(ok ? "arnestyles.keys.loaded" : "arnestyles.gui.preset_error", set.name()).getString();
        presetMessageColor = ok ? C_OK : C_DANGER;
    }

    private void copyKeySetCode(com.arnestyles.client.config.KeybindSets.KeySet set) {
        String code = com.arnestyles.client.config.KeybindSets.exportCode(set);
        if (code == null || this.client == null) {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
            presetMessageColor = C_DANGER;
            return;
        }
        this.client.keyboard.setClipboard(code);
        presetMessage = Text.translatable("arnestyles.gui.preset_copied", set.name()).getString();
        presetMessageColor = C_OK;
    }

    private void deleteKeySet(com.arnestyles.client.config.KeybindSets.KeySet set) {
        String armedKey = "keys:" + set.name();
        if (!armedKey.equals(deleteArmed)) {
            deleteArmed = armedKey;
            return;
        }
        com.arnestyles.client.config.KeybindSets.delete(set);
        deleteArmed = null;
        presetMessage = Text.translatable("arnestyles.gui.preset_deleted", set.name()).getString();
        presetMessageColor = C_MUTED;
    }

    /** Fuer den Selbsttest: Presets-Seite mit dem Tasten-Reiter. */
    public static StylesScreen onKeybinds() {
        StylesScreen screen = onPresets();
        screen.keybindPage = true;
        return screen;
    }

    private void drawPresets(DrawContext context, int mouseX, int mouseY) {
        int headY = top + HEADER_H + 7;
        drawBigText(context, Text.translatable("arnestyles.gui.presets"), paneX, headY, 1.3f, C_TEXT);
        // Reiter: Style-Presets oder Tastenbelegung
        int tabX = paneRight;
        for (int k = 1; k >= 0; k--) {
            boolean keysTab = k == 1;
            Text label = Text.translatable(keysTab ? "arnestyles.keys.tab" : "arnestyles.keys.tab_styles");
            int pw = this.textRenderer.getWidth(label) + 12;
            int x1 = tabX - pw;
            boolean selected = keybindPage == keysTab;
            boolean over = inside(mouseX, mouseY, x1, headY - 2, tabX, headY + 12);
            roundBordered(context, x1, headY - 2, tabX, headY + 12, selected || over ? C_ACCENT : C_EDGE, selected ? C_ACCENT : over ? C_CARD_HOVER : C_CARD);
            text(context, label, x1 + 6, headY + 1, selected ? onAccent() : C_TEXT);
            hits.add(new Hit(x1, headY - 2, tabX, headY + 12, () -> {
                keybindPage = keysTab;
                cardScroll = 0;
                presetMessage = null;
                deleteArmed = null;
            }));
            tabX = x1 - 4;
        }

        String info = presetMessage != null ? presetMessage
            : Text.translatable(keybindPage ? "arnestyles.keys.info" : "arnestyles.gui.presets_info").getString();
        text(context, this.textRenderer.trimToWidth(info, paneRight - paneX), paneX, top + HEADER_H + 26,
            presetMessage != null ? presetMessageColor : C_MUTED);

        // Eingabefeld + Speichern
        int fy = cardsTop;
        Text saveLabel = Text.translatable("arnestyles.gui.preset_save");
        Text pasteLabel = Text.translatable("arnestyles.gui.preset_paste");
        int saveW = this.textRenderer.getWidth(saveLabel) + 12;
        int pasteW = this.textRenderer.getWidth(pasteLabel) + 12;
        int fieldX2 = paneRight - saveW - pasteW - 10;
        roundBordered(context, paneX, fy, fieldX2, fy + 16, presetName.length() > 0 ? C_ACCENT : C_EDGE, C_FIELD);
        if (presetName.length() == 0) {
            text(context, Text.translatable("arnestyles.gui.preset_name"), paneX + 6, fy + 4, C_MUTED);
        } else {
            boolean cursor = (System.currentTimeMillis() / 500L) % 2L == 0L;
            text(context, cursor ? presetName + "_" : presetName.toString(), paneX + 6, fy + 4, C_TEXT);
        }
        int saveX = drawPillRight(context, paneRight, fy + 1, saveLabel, C_OK, mouseX, mouseY, this::savePreset);
        drawPillRight(context, saveX - 4, fy + 1, pasteLabel, C_TEXT, mouseX, mouseY, this::pastePresetCode);
        int listY = fy + 24;
        if (keybindPage) {
            drawKeySets(context, listY, mouseX, mouseY);
            return;
        }

        // Ganzes Setup als normales Texture-Pack exportieren
        {
            int y2 = listY + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, paneX, listY, paneRight, y2);
            round(context, paneX, listY, paneRight, y2, over ? C_CARD_HOVER : C_CARD);
            drawIconAsset(context, "textures/item/bundle.png", paneX + 4, listY + 1);
            text(context, Text.translatable(exporting ? "arnestyles.export.running" : "arnestyles.export.row"), paneX + 24, listY + 5, C_TEXT);
            Text go = Text.translatable("arnestyles.export.button");
            text(context, go, paneRight - 8 - this.textRenderer.getWidth(go), listY + 5, over ? C_ACCENT : C_MUTED);
            hits.add(new Hit(paneX, listY, paneRight, y2, this::exportPack));
            listY += ESSENTIAL_ROW_H;
        }
        // Alle aktuellen Einstellungen direkt als Code kopieren (ohne Preset zu speichern)
        {
            int y2 = listY + ESSENTIAL_ROW_H - 2;
            boolean over = inside(mouseX, mouseY, paneX, listY, paneRight, y2);
            round(context, paneX, listY, paneRight, y2, over ? C_CARD_HOVER : C_CARD);
            drawIconAsset(context, "textures/item/paper.png", paneX + 4, listY + 1);
            text(context, Text.translatable("arnestyles.gui.share_row"), paneX + 24, listY + 5, C_TEXT);
            Text go = Text.translatable("arnestyles.gui.share_button");
            text(context, go, paneRight - 8 - this.textRenderer.getWidth(go), listY + 5, over ? C_ACCENT : C_MUTED);
            hits.add(new Hit(paneX, listY, paneRight, y2, this::copyCurrentCode));
            listY += ESSENTIAL_ROW_H;
        }

        // Liste
        List<PresetManager.Preset> presets = PresetManager.list();
        if (presets.isEmpty()) {
            text(context, Text.translatable("arnestyles.gui.presets_empty"), paneX, listY + 4, C_MUTED);
            return;
        }
        int rowsFit = Math.max(1, (cardsBottom - listY) / ESSENTIAL_ROW_H);
        int maxScroll = Math.max(0, presets.size() - rowsFit);
        cardScroll = Math.max(0, Math.min(maxScroll, cardScroll));
        for (int i = cardScroll; i < Math.min(presets.size(), cardScroll + rowsFit); i++) {
            PresetManager.Preset preset = presets.get(i);
            int y = listY + (i - cardScroll) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean hovered = inside(mouseX, mouseY, paneX, y, paneRight, y2);
            round(context, paneX, y, paneRight, y2, hovered ? C_CARD_HOVER : C_CARD);
            drawIconAsset(context, "textures/item/writable_book.png", paneX + 4, y + 1);
            boolean armed = preset.name().equals(deleteArmed);
            Text deleteLabel = Text.translatable(armed ? "arnestyles.gui.preset_delete_confirm" : "arnestyles.gui.preset_delete");
            int x = drawPillRight(context, paneRight - 3, y + 2, deleteLabel, armed ? C_DANGER : C_MUTED, mouseX, mouseY, () -> deletePreset(preset));
            x = drawPillRight(context, x - 4, y + 2, Text.translatable("arnestyles.gui.preset_load"), C_TEXT, mouseX, mouseY, () -> loadPreset(preset));
            x = drawPillRight(context, x - 4, y + 2, Text.translatable("arnestyles.gui.preset_copy"), C_MUTED, mouseX, mouseY, () -> copyPresetCode(preset));
            x = drawPillRight(context, x - 4, y + 2, Text.translatable("arnestyles.gui.preset_rename"), C_MUTED, mouseX, mouseY, () -> renamePreset(preset));
            String server = com.arnestyles.client.mods.ServerPresets.currentServer();
            if (server != null) {
                // Auf einem Server: Preset fest an diesen Server binden (wird beim Betreten automatisch geladen)
                boolean bound = preset.name().equals(ConfigManager.get().serverPresets.get(server));
                x = drawPillRight(context, x - 4, y + 2, Text.translatable(bound ? "arnestyles.preset.auto_on" : "arnestyles.preset.auto"),
                    bound ? C_OK : C_MUTED, mouseX, mouseY, () -> {
                        com.arnestyles.client.mods.ServerPresets.toggle(server, preset.name());
                        presetMessage = Text.translatable(bound ? "arnestyles.preset.auto_removed" : "arnestyles.preset.auto_set", preset.name(), server)
                            .getString();
                        presetMessageColor = C_OK;
                    });
            }
            String name = this.textRenderer.trimToWidth(preset.name(), x - paneX - 30);
            text(context, name, paneX + 24, y + 5, C_TEXT);
        }
    }

    /** Liste der Keybind-Sets, oben ein Eintrag "Alles auf Standard". */
    private void drawKeySets(DrawContext context, int listY, int mouseX, int mouseY) {
        List<com.arnestyles.client.config.KeybindSets.KeySet> sets = com.arnestyles.client.config.KeybindSets.list();
        int rowsFit = Math.max(1, (cardsBottom - listY) / ESSENTIAL_ROW_H);
        int total = sets.size() + 1;
        cardScroll = Math.max(0, Math.min(Math.max(0, total - rowsFit), cardScroll));
        for (int i = cardScroll; i < Math.min(total, cardScroll + rowsFit); i++) {
            int y = listY + (i - cardScroll) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean hovered = inside(mouseX, mouseY, paneX, y, paneRight, y2);
            round(context, paneX, y, paneRight, y2, hovered ? C_CARD_HOVER : C_CARD);
            if (i == 0) {
                // Alle Tasten auf Minecraft-Standard
                drawIconAsset(context, "textures/item/barrier.png", paneX + 4, y + 1);
                boolean armed = "keys:#reset".equals(deleteArmed);
                drawPillRight(context, paneRight - 3, y + 2, Text.translatable(armed ? "arnestyles.gui.preset_delete_confirm" : "arnestyles.keys.reset_button"),
                    armed ? C_DANGER : C_TEXT, mouseX, mouseY, () -> {
                        if ("keys:#reset".equals(deleteArmed)) {
                            com.arnestyles.client.config.KeybindSets.resetAll();
                            deleteArmed = null;
                            presetMessage = Text.translatable("arnestyles.keys.reset_done").getString();
                            presetMessageColor = C_OK;
                        } else {
                            deleteArmed = "keys:#reset";
                        }
                    });
                text(context, Text.translatable("arnestyles.keys.reset"), paneX + 24, y + 5, C_TEXT_SOFT);
                continue;
            }
            com.arnestyles.client.config.KeybindSets.KeySet set = sets.get(i - 1);
            drawIconAsset(context, "textures/item/tripwire_hook.png", paneX + 4, y + 1);
            boolean armed = ("keys:" + set.name()).equals(deleteArmed);
            Text deleteLabel = Text.translatable(armed ? "arnestyles.gui.preset_delete_confirm" : "arnestyles.gui.preset_delete");
            int x = drawPillRight(context, paneRight - 3, y + 2, deleteLabel, armed ? C_DANGER : C_MUTED, mouseX, mouseY, () -> deleteKeySet(set));
            x = drawPillRight(context, x - 4, y + 2, Text.translatable("arnestyles.gui.preset_load"), C_TEXT, mouseX, mouseY, () -> loadKeySet(set));
            x = drawPillRight(context, x - 4, y + 2, Text.translatable("arnestyles.gui.preset_copy"), C_MUTED, mouseX, mouseY, () -> copyKeySetCode(set));
            String name = this.textRenderer.trimToWidth(set.name(), x - paneX - 30);
            text(context, name, paneX + 24, y + 5, C_TEXT);
        }
    }

    // ---- Vorschauen ----

    private void drawPreview(DrawContext context, StyleDefinition style, int x, int y, int size) {
        switch (current.previewKind()) {
            case CRYSTAL -> drawCrystal(context, style, x, y, size);
            case TILE -> {
                String asset = previewAsset(style);
                boolean tinted = !TintedTextures.signature(current, TintedTextures.styleKey(style), asset).isEmpty();
                PreviewTextures.Entry entry = PreviewTextures.colored(style, asset);
                if (entry == null) {
                    drawNoPreview(context, x, y, size);
                } else {
                    int pad = 8;
                    // Gras faerbt Minecraft je nach Biom, mit eigener Farbe ist die Textur schon fertig gefaerbt
                    int tileColor = !tinted && asset.endsWith("grass_block_top.png") ? GRASS_TINT : 0xFFFFFFFF;
                    boolean model3d = isModel3d(style, asset);
                    // 3D-Modell: aus den Modellteilen zusammensetzen statt den flachen "Bastelbogen" zu zeigen
                    if (!model3d || !drawModel3d(context, style, asset, x + pad, y + pad, size - 2 * pad)) {
                        drawTile(context, entry, x + pad, y + pad, size - 2 * pad, tileColor);
                    }
                    if (modelOnly(style)) {
                        Text badge = Text.translatable("arnestyles.gui.model_only");
                        int bw = this.textRenderer.getWidth(badge) + 6;
                        round(context, x + size - bw - 3, y + 3, x + size - 3, y + 13, C_WARN);
                        text(context, badge, x + size - bw, y + 4, 0xFF1A1D2B);
                    } else if (model3d) {
                        // Eigenes 3D-Modell: die flache Textur ist nur der "Bastelbogen", im Spiel sieht es anders aus.
                        round(context, x + size - 19, y + 3, x + size - 3, y + 13, C_ACCENT);
                        text(context, "3D", x + size - 17, y + 4, C_TEXT);
                    }
                }
            }
            case INVENTORY -> {
                PreviewTextures.Entry entry = PreviewTextures.forStyle(style, previewAsset(style));
                if (entry == null) {
                    drawNoPreview(context, x, y, size);
                } else {
                    // Das Inventar liegt oben links in einer 256x256-Textur (bei HD-Packs entsprechend groesser).
                    float fit = Math.min((size - 4) / 176.0f, (size - 4) / 166.0f);
                    int drawW = Math.round(176 * fit);
                    int drawH = Math.round(166 * fit);
                    drawRegion(context, entry, 256, 0, 0, 176, 166, x + (size - drawW) / 2, y + (size - drawH) / 2, drawW, drawH);
                }
            }
            case FIT -> {
                // Ein eigenes preview.png im Style-Ordner hat Vorrang.
                PreviewTextures.Entry entry = style.previewImage() != null
                    ? PreviewTextures.forFile(style.previewImage())
                    : PreviewTextures.forStyle(style, previewAsset(style));
                if (entry == null) {
                    drawNoPreview(context, x, y, size);
                } else {
                    int pad = 4;
                    int frameH = entry.frameHeight();
                    float fit = Math.min((float) (size - 2 * pad) / entry.width(), (float) (size - 2 * pad) / frameH);
                    int drawW = Math.max(1, Math.round(entry.width() * fit));
                    int drawH = Math.max(1, Math.round(frameH * fit));
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, entry.id(), x + (size - drawW) / 2, y + (size - drawH) / 2,
                        0.0f, (float) entry.currentFrame() * frameH, drawW, drawH, entry.width(), frameH, entry.width(), entry.height());
                }
            }
            case SHIELD -> {
                PreviewTextures.Entry entry = PreviewTextures.forStyle(style, previewAsset(style));
                if (entry == null) {
                    drawNoPreview(context, x, y, size);
                } else {
                    // Vorderseite des Schilds: 12x22 Pixel ab (1, 1) in der 64x64-Schildtextur.
                    int drawH = size - 8;
                    int drawW = Math.round(drawH * 12 / 22.0f);
                    drawRegion(context, entry, 64, 1, 1, 12, 22, x + (size - drawW) / 2, y + 4, drawW, drawH);
                }
            }
            case ARMOR -> drawArmor(context, style, x, y, size);
            case SOUND -> drawSoundPreview(context, style, x, y, size);
            default -> drawNoPreview(context, x, y, size);
        }
    }

    /**
     * Ruestung: Netherite und Diamant nebeneinander als kleine Figur (Helm, Oberkoerper, Arme)
     * aus der Ruestungs-Textur. Bringt der Style nur Item-Bilder mit, werden die Brustplatten-Items gezeigt.
     */
    private void drawArmor(DrawContext context, StyleDefinition style, int x, int y, int size) {
        String[] materials = {"netherite", "diamond"};
        boolean styleHasEquipment = style.isVanilla()
            || hasFile(style, "textures/entity/equipment/humanoid/netherite.png")
            || hasFile(style, "textures/entity/equipment/humanoid/diamond.png");
        int half = size / 2;
        for (int i = 0; i < materials.length; i++) {
            int cx = x + i * half;
            if (styleHasEquipment) {
                PreviewTextures.Entry tex = PreviewTextures.forStyle(style, "textures/entity/equipment/humanoid/" + materials[i] + ".png");
                if (tex == null) {
                    continue;
                }
                // Figur: 16 Einheiten breit (Arm 4, Koerper 8, Arm 4), 20 hoch (Kopf 8, Koerper 12)
                int unit = Math.max(1, Math.min((half - 4) / 16, (size - 8) / 20));
                int fx = cx + (half - 16 * unit) / 2;
                int fy = y + (size - 20 * unit) / 2;
                drawRegion(context, tex, 64, 8, 8, 8, 8, fx + 4 * unit, fy, 8 * unit, 8 * unit);
                drawRegion(context, tex, 64, 20, 20, 8, 12, fx + 4 * unit, fy + 8 * unit, 8 * unit, 12 * unit);
                drawRegion(context, tex, 64, 44, 20, 4, 12, fx, fy + 8 * unit, 4 * unit, 12 * unit);
                drawRegion(context, tex, 64, 44, 20, 4, 12, fx + 12 * unit, fy + 8 * unit, 4 * unit, 12 * unit);
            } else {
                PreviewTextures.Entry item = PreviewTextures.forStyle(style, "textures/item/" + materials[i] + "_chestplate.png");
                if (item != null) {
                    int s = half - 6;
                    drawTile(context, item, cx + 3, y + (size - s) / 2, s, 0xFFFFFFFF);
                }
            }
        }
    }

    private final Map<String, Boolean> model3dCache = new HashMap<>();

    /** Eine sichtbare Flaeche eines 3D-Modells, von vorne (Suedseite) gesehen: Rechteck, Tiefe, Textur und UV (0 bis 16). */
    private record ModelFace(float x1, float y1, float x2, float y2, float depth, String texture, float u1, float v1, float u2, float v2) {
    }

    private final Map<String, List<ModelFace>> modelFaceCache = new HashMap<>();

    /** Liest die Vorderseiten aller Wuerfel eines Item-Modells (ohne Drehung, reicht fuer eine Vorschau). */
    private List<ModelFace> modelFaces(StyleDefinition style, String asset) {
        String item = asset.substring("textures/item/".length(), asset.length() - ".png".length());
        return modelFaceCache.computeIfAbsent(current.name() + "/" + style.id() + "/" + item, key -> {
            List<ModelFace> faces = new ArrayList<>();
            if (style.root() == null) {
                return faces;
            }
            Path model = style.root().resolve("assets").resolve("minecraft").resolve("models").resolve("item").resolve(item + ".json");
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(Files.readString(model)).getAsJsonObject();
                Map<String, String> textures = new HashMap<>();
                if (json.has("textures")) {
                    for (var e : json.getAsJsonObject("textures").entrySet()) {
                        textures.put(e.getKey(), e.getValue().getAsString());
                    }
                }
                for (var el : json.getAsJsonArray("elements")) {
                    com.google.gson.JsonObject element = el.getAsJsonObject();
                    var from = element.getAsJsonArray("from");
                    var to = element.getAsJsonArray("to");
                    var faceMap = element.getAsJsonObject("faces");
                    if (from == null || to == null || faceMap == null) {
                        continue;
                    }
                    com.google.gson.JsonObject face = faceMap.has("south") ? faceMap.getAsJsonObject("south")
                        : faceMap.has("north") ? faceMap.getAsJsonObject("north") : null;
                    if (face == null || !face.has("texture")) {
                        continue;
                    }
                    String ref = face.get("texture").getAsString();
                    for (int i = 0; i < 5 && ref.startsWith("#"); i++) {
                        ref = textures.getOrDefault(ref.substring(1), "");
                    }
                    if (ref.isEmpty()) {
                        continue;
                    }
                    if (ref.contains(":")) {
                        ref = ref.substring(ref.indexOf(':') + 1);
                    }
                    float[] uv = {0, 0, 16, 16};
                    if (face.has("uv")) {
                        var a = face.getAsJsonArray("uv");
                        for (int i = 0; i < 4; i++) {
                            uv[i] = a.get(i).getAsFloat();
                        }
                    }
                    faces.add(new ModelFace(from.get(0).getAsFloat(), 16 - to.get(1).getAsFloat(), to.get(0).getAsFloat(),
                        16 - from.get(1).getAsFloat(), to.get(2).getAsFloat(), "textures/" + ref + ".png", uv[0], uv[1], uv[2], uv[3]));
                }
                faces.sort(java.util.Comparator.comparingDouble(ModelFace::depth));
            } catch (java.io.IOException | RuntimeException e) {
                faces.clear();
            }
            return faces;
        });
    }

    /** Zeichnet die Vorderansicht eines 3D-Item-Modells ins Quadrat. false, wenn das nicht geht. */
    private boolean drawModel3d(DrawContext context, StyleDefinition style, String asset, int x, int y, int size) {
        List<ModelFace> faces = modelFaces(style, asset);
        if (faces.isEmpty()) {
            return false;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (ModelFace f : faces) {
            minX = Math.min(minX, f.x1());
            minY = Math.min(minY, f.y1());
            maxX = Math.max(maxX, f.x2());
            maxY = Math.max(maxY, f.y2());
        }
        float span = Math.max(maxX - minX, maxY - minY);
        if (span <= 0) {
            return false;
        }
        float scale = size / span;
        float offX = x + (size - (maxX - minX) * scale) / 2;
        float offY = y + (size - (maxY - minY) * scale) / 2;
        for (ModelFace f : faces) {
            PreviewTextures.Entry tex = PreviewTextures.forStyle(style, f.texture());
            if (tex == null) {
                continue;
            }
            int x1 = Math.round(offX + (f.x1() - minX) * scale);
            int y1 = Math.round(offY + (f.y1() - minY) * scale);
            int x2 = Math.max(x1 + 1, Math.round(offX + (f.x2() - minX) * scale));
            int y2 = Math.max(y1 + 1, Math.round(offY + (f.y2() - minY) * scale));
            float sx = tex.width() / 16.0f;
            float sy = tex.frameHeight() / 16.0f;
            float u = Math.min(f.u1(), f.u2()) * sx;
            float v = Math.min(f.v1(), f.v2()) * sy + tex.currentFrame() * tex.frameHeight();
            int rw = Math.max(1, Math.round(Math.abs(f.u2() - f.u1()) * sx));
            int rh = Math.max(1, Math.round(Math.abs(f.v2() - f.v1()) * sy));
            context.drawTexture(RenderPipelines.GUI_TEXTURED, tex.id(), x1, y1, u, v, x2 - x1, y2 - y1, rw, rh, tex.width(), tex.height());
        }
        return true;
    }

    /** true, wenn der Style fuer dieses Item ein eigenes 3D-Modell (mit "elements") mitbringt. */
    private boolean isModel3d(StyleDefinition style, String asset) {
        if (style.root() == null || !asset.startsWith("textures/item/")) {
            return false;
        }
        String item = asset.substring("textures/item/".length(), asset.length() - ".png".length());
        return model3dCache.computeIfAbsent(current.name() + "/" + style.id() + "/" + item, key -> {
            Path model = style.root().resolve("assets").resolve("minecraft").resolve("models").resolve("item").resolve(item + ".json");
            try {
                return Files.isRegularFile(model) && Files.readString(model).contains("\"elements\"");
            } catch (java.io.IOException e) {
                return false;
            }
        });
    }

    private boolean hasFile(StyleDefinition style, String asset) {
        if (style.root() == null) {
            return false;
        }
        return styleHasFileCache.computeIfAbsent(current.name() + "/" + style.id() + "/" + asset,
            key -> Files.isRegularFile(style.root().resolve("assets").resolve("minecraft").resolve(asset)));
    }

    private void drawSoundPreview(DrawContext context, StyleDefinition style, int x, int y, int size) {
        PreviewTextures.Entry icon = PreviewTextures.forRoot("icon", null, current.previewAssets().get(0));
        int iconSize = size / 2;
        if (icon != null) {
            drawTile(context, icon, x + (size - iconSize) / 2, y + 8, iconSize, 0xFFFFFFFF);
        }
        if (style.root() == null) {
            return;
        }
        int count = soundCountCache.computeIfAbsent(current.name() + "/" + style.id(), key -> {
            Path sounds = style.root().resolve("assets").resolve("minecraft").resolve("sounds");
            if (!Files.isDirectory(sounds)) {
                return 0;
            }
            try (var files = Files.walk(sounds)) {
                return (int) files.filter(p -> p.toString().endsWith(".ogg")).count();
            } catch (java.io.IOException e) {
                return 0;
            }
        });
        Text label = Text.translatable("arnestyles.gui.sound_count", count);
        int w = this.textRenderer.getWidth(label);
        text(context, label, x + (size - w) / 2, y + size - 16, C_MUTED);
    }

    /** Erste Vorschau-Textur der Kategorie, die der Style selbst enthaelt. Sonst die erste aus Minecraft. */
    private String previewAsset(StyleDefinition style) {
        List<String> assets = current.previewAssets();
        if (style.root() != null && (current.previewKind() == StyleCategory.PreviewKind.TILE
            || current.previewKind() == StyleCategory.PreviewKind.FIT)) {
            computeUniquePreviews();
            String unique = uniquePreviewCache.get(current.name() + "/" + style.id());
            return unique == null || unique.isEmpty() ? assets.get(0) : unique;
        }
        if (style.root() == null || assets.size() == 1) {
            return assets.get(0);
        }
        return previewAssetCache.computeIfAbsent(current.name() + "/" + style.id(), key -> {
            for (String asset : assets) {
                if (Files.isRegularFile(style.root().resolve("assets").resolve("minecraft").resolve(asset))) {
                    return asset;
                }
            }
            return assets.get(0);
        });
    }

    /**
     * Groesse und Mitte des Vorschau-Kristalls. Kristall mit Sockel ist etwa 3,5 Bloecke hoch,
     * seine Mitte liegt ungefaehr 0,6 Bloecke ueber dem Ursprung.
     */
    private static final float CRYSTAL_SCALE = 0.26f;
    private static final float CRYSTAL_Y = 0.6f;
    private static final float CRYSTAL_TILT = 0.35f;

    private void drawCrystal(DrawContext context, StyleDefinition style, int x, int y, int size) {
        PreviewTextures.Entry entry = PreviewTextures.colored(style, previewAsset(style));
        if (entry == null) {
            drawNoPreview(context, x, y, size);
            return;
        }
        PreviewCrystalRenderState state = new PreviewCrystalRenderState();
        state.entityType = EntityType.END_CRYSTAL;
        state.texture = entry.id();
        state.age = (System.currentTimeMillis() % 1_000_000L) / 50.0f;
        state.baseVisible = true;
        state.beamOffset = null;
        state.width = 2.0f;
        state.height = 2.0f;
        state.outlineColor = 0;
        // Die GUI zeichnet nach unten positiv, deshalb wird das Modell wie im Inventar um 180 Grad gedreht.
        // Leicht nach vorne gekippt, damit man auf den Sockel schaut.
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateX(CRYSTAL_TILT);
        // 3D-Modelle zeichnet Minecraft ohne die Groessen-Matrix, deshalb hier selbst umrechnen
        int sx = Math.round(x * scale);
        int sy = Math.round(y * scale);
        int ss = Math.round(size * scale);
        context.addEntity(state, ss * CRYSTAL_SCALE, new Vector3f(0.0f, CRYSTAL_Y, 0.0f), rotation, null, sx, sy, sx + ss, sy + ss);
    }

    private void drawNoPreview(DrawContext context, int x, int y, int size) {
        Text text = Text.translatable("arnestyles.gui.no_preview");
        int lineY = y + size / 2 - 8;
        for (OrderedText line : this.textRenderer.wrapLines(text, size - 6)) {
            int w = this.textRenderer.getWidth(line);
            text(context, line, x + (size - w) / 2, lineY, C_MUTED);
            lineY += 10;
        }
    }

    // ---- Essentials ----

    private List<EssentialRow> essentialRows() {
        List<EssentialRow> rows = new ArrayList<>();
        for (Essential essential : Essential.values()) {
            boolean pack = essential.kind() == Essential.Kind.PACK;
            Path root = pack ? StyleRegistry.essentialRoot(essential).orElse(null) : null;
            rows.add(new EssentialRow(Text.translatable(essential.nameKey()), Text.translatable(essential.descriptionKey()),
                essential.id(), pack, "essential/" + essential.id(), root, essential.icon(), null));
        }
        for (StyleDefinition custom : StyleRegistry.getStyles(StyleCategory.ESSENTIALS)) {
            if (!custom.isVanilla()) {
                rows.add(new EssentialRow(custom.name(), Text.translatable("arnestyles.gui.custom_essential", custom.id()),
                    StyleRegistry.customEssentialKey(custom), true, "", null, null, custom.previewImage()));
            }
        }
        return rows;
    }

    private int essentialRowsVisible() {
        return Math.max(1, (cardsBottom - cardsTop + 2) / ESSENTIAL_ROW_H);
    }

    private void drawEssentials(DrawContext context, int mouseX, int mouseY) {
        ArneStylesConfig config = ConfigManager.get();
        boolean active = config.isEnabled(StyleCategory.ESSENTIALS) && config.modEnabled;
        EssentialSlider[] sliders = EssentialSlider.values();
        List<EssentialRow> rows = essentialRows();
        int total = sliders.length + rows.size();
        int w = paneRight - paneX;

        Text hoveredDescription = null;
        int first = cardScroll;
        int last = Math.min(total, first + essentialRowsVisible());
        for (int i = first; i < last; i++) {
            int x = paneX;
            int y = cardsTop + (i - first) * ESSENTIAL_ROW_H;
            int y2 = y + ESSENTIAL_ROW_H - 2;
            boolean hovered = inside(mouseX, mouseY, x, y, x + w, y2);
            round(context, x, y, x + w, y2, hovered ? C_CARD_HOVER : C_CARD);

            if (i < sliders.length) {
                EssentialSlider slider = sliders[i];
                if (drawSliderRow(context, slider, x, y, w, active, mouseX, mouseY, null)) {
                    hoveredDescription = Text.translatable(slider.descriptionKey());
                }
            } else {
                EssentialRow row = rows.get(i - sliders.length);
                boolean on = config.isEssentialOn(row.key());
                if (hovered) {
                    hoveredDescription = row.description();
                }
                drawEssentialIcon(context, row, x + 5, y + 1);
                String name = this.textRenderer.trimToWidth(row.name().getString(), w - 26 - 34);
                text(context, name, x + 26, y + 5, C_TEXT);
                drawSwitch(context, x + w - 28, y + 3, on, on && !active);
                hits.add(new Hit(x, y, x + w, y2, () -> toggleEssential(row.key(), row.needsReload())));
            }
        }

        if (config.modEnabled) {
            Text info;
            int infoColor = C_MUTED;
            if (hoveredDescription != null) {
                info = hoveredDescription;
                infoColor = C_TEXT;
            } else if (dirty) {
                info = Text.translatable("arnestyles.gui.pending");
                infoColor = C_WARN;
            } else {
                info = Text.translatable("arnestyles.gui.essentials_info");
            }
            String infoText = this.textRenderer.trimToWidth(info.getString(), paneRight - paneX);
            text(context, infoText, paneX, top + HEADER_H + 26, infoColor);
        }
        drawScrollMarkers(context);
    }

    private void drawEssentialIcon(DrawContext context, EssentialRow row, int x, int y) {
        PreviewTextures.Entry entry;
        if (row.iconImage() != null) {
            entry = PreviewTextures.forFile(row.iconImage());
        } else if (row.iconAsset() != null) {
            entry = PreviewTextures.forRoot(row.iconCacheKey(), row.iconRoot(), row.iconAsset());
        } else {
            entry = null;
        }
        if (entry == null) {
            round(context, x, y, x + 16, y + 16, C_FIELD);
            return;
        }
        drawTile(context, entry, x, y, 16, 0xFFFFFFFF);
    }

    private void markDirty() {
        dirty = true;
        pendingChanges++;
    }

    /** Laedt geaenderte Texturen sofort (statt erst beim Schliessen). */
    private void applyChanges() {
        PerformanceSettings.flush();
        if (dirty && this.client != null) {
            dirty = false;
            PreviewTextures.clear();
            this.client.reloadResources();
        }
        pendingChanges = 0;
    }

    private static String versionText() {
        if (versionText == null) {
            versionText = "v" + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(com.arnestyles.ArneStyles.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
        }
        return versionText;
    }

    /** Fussleiste: links Version, Mitte Hinweise, rechts Presets und Uebernehmen (mit Anzahl offener Aenderungen). */
    private void drawFooter(DrawContext context, int mouseX, int mouseY) {
        int fy = bottom - FOOTER_H;
        context.fill(left, fy, right, fy + 1, C_EDGE);
        String version = "Arne Tweaks " + versionText();
        text(context, version, left + 10, fy + 6, C_MUTED);
        int textStart = left + 18 + this.textRenderer.getWidth(version);

        // Rechts: Uebernehmen (hervorgehoben, wenn etwas offen ist) und Presets
        boolean pending = dirty || pendingChanges > 0;
        Text applyLabel = pendingChanges > 0
            ? Text.translatable("arnestyles.gui.apply_count", pendingChanges)
            : Text.translatable("arnestyles.gui.apply");
        int bw = this.textRenderer.getWidth(applyLabel) + 14;
        int bx2 = right - 6;
        int bx1 = bx2 - bw;
        int by1 = fy + 3;
        int by2 = bottom - 3;
        boolean over = inside(mouseX, mouseY, bx1, by1, bx2, by2);
        int fill = pending ? (over ? GuiTheme.mix(C_ACCENT, 0xFFFFFFFF, 0.15f) : C_ACCENT) : (over ? C_CARD_HOVER : C_CARD);
        roundBordered(context, bx1, by1, bx2, by2, pending ? C_ACCENT : over ? C_EDGE_HOVER : C_EDGE, fill);
        text(context, applyLabel, bx1 + 7, by1 + 3, pending ? onAccent() : C_MUTED);
        hits.add(new Hit(bx1, by1, bx2, by2, this::applyChanges));
        int x = bx1 - 4;
        Text presets = Text.translatable("arnestyles.gui.presets");
        int pw = this.textRenderer.getWidth(presets) + 12;
        boolean pOver = inside(mouseX, mouseY, x - pw, by1, x, by2);
        roundBordered(context, x - pw, by1, x, by2, pOver ? C_EDGE_HOVER : C_EDGE, pOver ? C_CARD_HOVER : C_CARD);
        text(context, presets, x - pw + 6, by1 + 3, C_TEXT_SOFT);
        hits.add(new Hit(x - pw, by1, x, by2, this::openPresets));
        x -= pw + 8;

        boolean danger = resetArmed;
        String key = resetArmed ? "arnestyles.gui.reset_confirm" : sortMode ? "arnestyles.gui.sort_hint" : "arnestyles.gui.hint";
        int maxWidth = x - textStart;
        if (maxWidth > 30) {
            String hint = this.textRenderer.trimToWidth(Text.translatable(key).getString(), maxWidth);
            text(context, hint, textStart, fy + 6, danger ? C_DANGER : C_MUTED);
        }
    }

    // ---- Inspector: rechte Spalte mit Live-Vorschau ----

    private String originOf(StyleDefinition style) {
        if (style.isVanilla()) {
            return "vanilla";
        }
        if (style.builtIn()) {
            return "style";
        }
        return originCache.computeIfAbsent(style.category().name() + "/" + style.id(), k -> {
            Path json = style.root() == null ? null : style.root().resolve("style.json");
            if (json == null || !Files.isRegularFile(json)) {
                return "custom";
            }
            try {
                String content = Files.readString(json);
                return content.contains("\"drawn\": true") || content.contains("\"drawn\":true") ? "drawn"
                    : content.contains("\"imported\": true") || content.contains("\"imported\":true") ? "pack" : "custom";
            } catch (java.io.IOException e) {
                return "custom";
            }
        });
    }

    private static int originColor(String origin) {
        return switch (origin) {
            case "vanilla" -> 0xFF929BAD;
            case "style" -> 0xFFE8B44C;
            case "pack" -> 0xFF35D6E8;
            case "drawn" -> 0xFFFF9F43;
            default -> 0xFFF472B6;
        };
    }

    private void drawInspector(DrawContext context, int mouseX, int mouseY) {
        int x1 = inspectorX;
        int x2 = right;
        int y1 = top + HEADER_H + 1;
        int y2 = bottom - FOOTER_H;
        context.fill(x1, y1, x2, y2, C_SIDEBAR);
        context.fill(x1, y1, x1 + 1, y2, C_EDGE);

        // Reiter: INSPECTOR | VORLAGEN
        int tx = x1 + 8;
        int ty = y1 + 6;
        Text[] tabs = {Text.translatable("arnestyles.inspector.tab"), Text.translatable("arnestyles.inspector.themes")};
        for (int i = 0; i < 2; i++) {
            boolean active = (i == 1) == inspectorThemes;
            int tw = this.textRenderer.getWidth(tabs[i]);
            boolean over = inside(mouseX, mouseY, tx - 2, ty - 2, tx + tw + 2, ty + 11);
            text(context, tabs[i], tx, ty, active ? C_TEXT : over ? C_TEXT_SOFT : C_MUTED);
            if (active) {
                context.fill(tx, ty + 10, tx + tw, ty + 12, C_ACCENT);
            }
            final boolean themes = i == 1;
            hits.add(new Hit(tx - 2, ty - 2, tx + tw + 2, ty + 11, () -> inspectorThemes = themes));
            tx += tw + 12;
        }
        context.fill(x1 + 1, ty + 14, x2, ty + 15, C_EDGE);
        int cy = ty + 20;
        int w = x2 - x1;
        if (inspectorThemes) {
            drawInspectorTemplates(context, x1, cy, w, y2, mouseX, mouseY);
        } else if (modsOpen) {
            drawInspectorMod(context, x1, cy, w, y2, mouseX, mouseY);
        } else if (!presetsOpen && !shadersOpen && !themesOpen && !importOpen
            && current != StyleCategory.PERFORMANCE_SETTINGS && current != StyleCategory.ESSENTIALS) {
            drawInspectorStyle(context, x1, cy, w, y2, mouseX, mouseY);
        } else {
            int yy = cy;
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("arnestyles.inspector.page_hint"), w - 16)) {
                text(context, line, x1 + 8, yy, C_MUTED);
                yy += 10;
            }
        }
    }

    /** Grosse Live-Vorschau der Karte unter der Maus, sonst des gewaehlten Styles. */
    private void drawInspectorStyle(DrawContext context, int x1, int cy, int w, int y2, int mouseX, int mouseY) {
        StyleCategory category = customizing() && customizeCategory != null ? customizeCategory : current;
        StyleDefinition selected = StyleRegistry.getSelected(category);
        StyleDefinition style = hoveredStyle != null ? hoveredStyle : selected;
        boolean isSelected = style.id().equals(selected.id());
        boolean active = isSelected && ConfigManager.get().isEnabled(category) && ConfigManager.get().modEnabled;

        text(context, Text.translatable(hoveredStyle != null ? "arnestyles.inspector.live" : "arnestyles.inspector.current"), x1 + 8, cy, C_ACCENT);
        int size = Math.max(48, Math.min(w - 20, y2 - cy - 92));
        int px = x1 + (w - size) / 2;
        int py = cy + 12;
        roundBordered(context, px - 2, py - 2, px + size + 2, py + size + 2, isSelected ? C_ACCENT2 : C_EDGE, C_FIELD);
        drawPreview(context, style, px, py, size);

        int ty = py + size + 8;
        String name = this.textRenderer.trimToWidth(style.name().getString(), w - 16);
        text(context, name, x1 + 8, ty, C_TEXT);
        text(context, categoryName(category), x1 + 8, ty + 11, C_MUTED);
        String origin = originOf(style);
        Text originText = Text.translatable("arnestyles.origin." + origin);
        text(context, originText, x2(x1, w) - 8 - this.textRenderer.getWidth(originText), ty + 11, originColor(origin));

        int sy = ty + 25;
        if (active) {
            roundBordered(context, x1 + 8, sy, x1 + w - 8, sy + 15, C_OK, GuiTheme.mix(C_CARD, C_OK, 0.18f));
            Text t = Text.translatable("arnestyles.inspector.active");
            text(context, t, x1 + (w - this.textRenderer.getWidth(t)) / 2, sy + 4, C_OK);
        } else {
            // Knopf: diesen Style waehlen
            boolean over = inside(mouseX, mouseY, x1 + 8, sy, x1 + w - 8, sy + 15);
            roundBordered(context, x1 + 8, sy, x1 + w - 8, sy + 15, C_ACCENT2, over ? GuiTheme.mix(C_ACCENT2, 0xFFFFFFFF, 0.15f) : C_ACCENT2);
            Text t = Text.translatable(isSelected ? "arnestyles.inspector.enable" : "arnestyles.inspector.choose");
            text(context, t, x1 + (w - this.textRenderer.getWidth(t)) / 2, sy + 4, 0xFFFFFFFF);
            hits.add(new Hit(x1 + 8, sy, x1 + w - 8, sy + 15, isSelected ? this::toggleCurrent : () -> chooseStyle(style)));
        }
    }

    private static int x2(int x1, int w) {
        return x1 + w;
    }

    /** Mod unter der Maus (bleibt stehen, bis ein anderer gezeigt wird): Name, Beschreibung, Status, Vorschau. */
    private void drawInspectorMod(DrawContext context, int x1, int cy, int w, int y2, int mouseX, int mouseY) {
        if (inspectToggle == null && inspectSlider == null) {
            int yy = cy;
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("arnestyles.inspector.mod_hint"), w - 16)) {
                text(context, line, x1 + 8, yy, C_MUTED);
                yy += 10;
            }
            return;
        }
        String nameKey = inspectToggle != null ? inspectToggle.nameKey() : inspectSlider.nameKey();
        String descKey = inspectToggle != null ? inspectToggle.descriptionKey() : inspectSlider.descriptionKey();
        text(context, Text.translatable("arnestyles.inspector.live"), x1 + 8, cy, C_ACCENT);
        int bx = x1 + 8;
        int bw = w - 16;
        int bh = Math.min(84, Math.max(56, (y2 - cy) / 3));
        int by = cy + 12;
        roundBordered(context, bx - 1, by - 1, bx + bw + 1, by + bh + 1, C_EDGE, C_FIELD);
        context.enableScissor(bx, by, bx + bw, by + bh);
        boolean drew = drawModPreview(context, nameKey, bx, by, bw, bh);
        context.disableScissor();
        if (!drew) {
            Text none = Text.translatable("arnestyles.inspector.no_preview");
            text(context, none, bx + (bw - this.textRenderer.getWidth(none)) / 2, by + bh / 2 - 4, C_MUTED);
        }

        int ty = by + bh + 8;
        for (OrderedText line : this.textRenderer.wrapLines(Text.translatable(nameKey), w - 16)) {
            text(context, line, x1 + 8, ty, C_TEXT);
            ty += 10;
        }
        ty += 3;
        if (inspectToggle != null) {
            ModSetting.Toggle toggle = inspectToggle;
            boolean on = toggle.value();
            boolean over = inside(mouseX, mouseY, x1 + 8, ty, x1 + w - 8, ty + 15);
            int color = on ? C_OK : C_OFF;
            roundBordered(context, x1 + 8, ty, x1 + w - 8, ty + 15, over ? C_EDGE_HOVER : color, GuiTheme.mix(C_CARD, color, 0.2f));
            Text t = Text.translatable(on ? "arnestyles.inspector.on" : "arnestyles.inspector.off");
            text(context, t, x1 + (w - this.textRenderer.getWidth(t)) / 2, ty + 4, on ? C_OK : C_MUTED);
            hits.add(new Hit(x1 + 8, ty, x1 + w - 8, ty + 15, toggle::toggle));
        } else {
            String value = inspectSlider.display();
            roundBordered(context, x1 + 8, ty, x1 + w - 8, ty + 15, C_EDGE, C_CARD);
            text(context, value, x1 + (w - this.textRenderer.getWidth(value)) / 2, ty + 4, C_ACCENT);
        }
        ty += 22;
        for (OrderedText line : this.textRenderer.wrapLines(Text.translatable(descKey), w - 16)) {
            if (ty > y2 - 12) {
                break;
            }
            text(context, line, x1 + 8, ty, C_MUTED);
            ty += 10;
        }
    }

    /** Kleine bewegte Vorschau fuer einige Mods. false = keine Vorschau fuer diesen Mod. */
    private boolean drawModPreview(DrawContext context, String nameKey, int x, int y, int w, int h) {
        String id = nameKey.substring(nameKey.lastIndexOf('.') + 1);
        long now = System.currentTimeMillis();
        if (id.startsWith("spotify")) {
            int cw = Math.min(w - 12, 150);
            int cx = x + (w - cw) / 2;
            int ch = 44;
            int cy = y + (h - ch) / 2;
            context.fill(cx, cy, cx + cw, cy + ch, 0xD8121212);
            context.fill(cx, cy, cx + 2, cy + ch, com.arnestyles.client.mods.SpotifyHud.accent());
            int tx = cx + 7;
            if (ConfigManager.get().spotifyCover) {
                if (!com.arnestyles.client.mods.SpotifyHud.drawCurrentCover(context, cx + 5, cy + 4, 30)) {
                    context.fill(cx + 5, cy + 4, cx + 35, cy + 34, 0xFF2A2A2A);
                }
                tx = cx + 40;
            }
            text(context, "Song", tx, cy + 5, 0xFFFFFFFF);
            text(context, "Artist", tx, cy + 16, 0xFFB3B3B3);
            boolean lyrics = ConfigManager.get().spotifyLyrics;
            text(context, lyrics ? "\u266A Lyrics \u2026" : "\u266A", tx, cy + 27, com.arnestyles.client.mods.SpotifyHud.accent());
            float f = (now % 8000L) / 8000.0f;
            context.fill(cx + 7, cy + ch - 5, cx + cw - 6, cy + ch - 3, 0x40FFFFFF);
            context.fill(cx + 7, cy + ch - 5, cx + 7 + Math.round((cw - 13) * f), cy + ch - 3, com.arnestyles.client.mods.SpotifyHud.accent());
            return true;
        }
        if (id.startsWith("crosshair") || id.equals("custom_crosshair")) {
            // Eigenes Fadenkreuz gross auf Spiel-Hintergrund, Farbe wie im Spiel
            int s = Math.min(w, h) - 16;
            int bx = x + (w - s) / 2;
            int by = y + (h - s) / 2;
            context.fill(bx, by, bx + s, by + s / 2, 0xFF7FA6D9);
            context.fill(bx, by + s / 2, bx + s, by + s, 0xFF5E8C3A);
            float scale = Math.max(1.0f, (s - 8) / (float) com.arnestyles.client.mods.CustomCrosshair.SIZE);
            int rgb = id.equals("crosshair_target_color") ? ConfigManager.get().crosshairTargetRgb : ConfigManager.get().crosshairRgb;
            com.arnestyles.client.mods.CustomCrosshair.draw(context, bx + s / 2.0f - scale / 2.0f, by + s / 2.0f - scale / 2.0f, rgb, scale);
            return true;
        }
        if (id.startsWith("item_counter") || id.startsWith("count_")) {
            // Kleine Hotbar-Leiste mit den gewaehlten Items
            net.minecraft.item.Item[] items = {net.minecraft.item.Items.TOTEM_OF_UNDYING, net.minecraft.item.Items.ENDER_PEARL,
                net.minecraft.item.Items.END_CRYSTAL, net.minecraft.item.Items.OBSIDIAN, net.minecraft.item.Items.GOLDEN_APPLE,
                net.minecraft.item.Items.EXPERIENCE_BOTTLE};
            int[] counts = {3, 16, 64, 128, 12, 1};
            int cell = 20;
            int bw = items.length * cell + 4;
            int bx = x + (w - bw) / 2;
            int by = y + (h - 30) / 2;
            com.arnestyles.client.mods.HudStyle.background(context, bx, by, bx + bw, by + 30);
            int warn = Math.round(ConfigManager.get().itemCounterWarn);
            for (int i = 0; i < items.length; i++) {
                int ix = bx + 4 + i * cell;
                context.drawItem(new net.minecraft.item.ItemStack(items[i]), ix, by + 2);
                String n = String.valueOf(counts[i]);
                int color = warn > 0 && counts[i] <= warn ? 0xFF000000 | ConfigManager.get().itemCounterWarnRgb
                    : com.arnestyles.client.mods.HudStyle.text();
                context.drawText(this.textRenderer, n, ix + (16 - this.textRenderer.getWidth(n)) / 2, by + 20, color, true);
            }
            return true;
        }
        if (id.startsWith("totem_pop") || id.startsWith("totem_particle")) {
            // Totem fliegt im eingestellten Stil, bunte Partikel drumherum
            float speed = ConfigManager.get().totemPopSpeed;
            float t = ((now * speed) % 2000L) / 2000.0f;
            int cxm = x + w / 2;
            int cym = y + h / 2;
            PREVIEW_RANDOM.setSeed(7L);
            for (int i = 0; i < 18; i++) {
                double a = i * 0.7 + t * 4.0;
                double r = 10 + (i % 5) * 6 + t * 18;
                int rgb = com.arnestyles.client.mods.TotemPop.particleColor(PREVIEW_RANDOM);
                if (rgb < 0) {
                    rgb = (i % 2 == 0) ? 0xC8E85A : 0x8FD14F;
                }
                int px = cxm + (int) Math.round(Math.cos(a) * r);
                int py = cym + (int) Math.round(Math.sin(a) * r * 0.6);
                context.fill(px, py, px + 2, py + 2, 0xFF000000 | rgb);
            }
            var m = context.getMatrices();
            m.pushMatrix();
            float scale = 2.2f * (0.6f + 0.4f * (float) Math.sin(Math.min(1.0f, t * 1.4f) * Math.PI / 2));
            m.translate(cxm, cym);
            if (!com.arnestyles.client.mods.TotemPop.noSpin()) {
                m.rotate((float) (Math.sin(t * Math.PI * 2) * 0.35));
            }
            if (!com.arnestyles.client.mods.TotemPop.centered()) {
                m.translate((float) Math.sin(t * Math.PI * 2) * 8.0f, 0.0f);
            }
            m.scale(scale, scale);
            context.drawItem(TOTEM_STACK, -8, -8);
            m.popMatrix();
            return true;
        }
        return false;
    }

    /** Vorlagen (Farben + Aufbau) mit einem Klick wechseln. */
    private void drawInspectorTemplates(DrawContext context, int x1, int cy, int w, int y2, int mouseX, int mouseY) {
        int y = cy;
        for (GuiLayout.Template template : GuiLayout.TEMPLATES) {
            if (y + 16 > y2 - 2) {
                break;
            }
            boolean on = GuiLayout.matches(template);
            boolean over = inside(mouseX, mouseY, x1 + 6, y, x1 + w - 6, y + 16);
            roundBordered(context, x1 + 6, y, x1 + w - 6, y + 16, on ? C_ACCENT : over ? C_EDGE_HOVER : C_EDGE, over ? C_CARD_HOVER : C_CARD);
            drawThemeSwatches(context, GuiTheme.find(template.theme()), x1 + 10, y + 4);
            String name = this.textRenderer.trimToWidth(Text.translatable(template.nameKey()).getString(), w - 52);
            text(context, name, x1 + 40, y + 4, on ? C_TEXT : C_TEXT_SOFT);
            hits.add(new Hit(x1 + 6, y, x1 + w - 6, y + 16, () -> GuiLayout.apply(template)));
            y += 18;
        }
    }
}
