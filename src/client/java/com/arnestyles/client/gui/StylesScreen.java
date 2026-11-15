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
        int panelW = Math.min(vw - 16, 600);
        int panelH = Math.min(vh - 16, 362);
        this.left = (vw - panelW) / 2;
        this.top = (vh - panelH) / 2;
        this.right = left + panelW;
        this.bottom = top + panelH;
        this.listTop = top + HEADER_H + (iconSidebar ? 6 : 26);
        this.rowsVisible = Math.max(1, (bottom - FOOTER_H - 4 - listTop) / ROW_HEIGHT);
        this.paneX = left + LIST_WIDTH + 12;
        this.paneRight = right - 10;
        boolean stylePage = !presetsOpen && !modsOpen && !shadersOpen && !themesOpen && !importOpen;
        int extraRows = !stylePage || KEY_CUSTOMIZE_TAB.equals(ConfigManager.get().activeTab) ? 0 : current == StyleCategory.SOUNDS ? 22 : current == StyleCategory.ORES ? 66
            : TintedTextures.supports(current) ? 44 : 0;
        this.cardsTop = top + HEADER_H + 44 + extraRows;
        this.cardsBottom = bottom - FOOTER_H - 4;
        this.columns = Math.max(1, (paneRight - paneX + CARD_GAP) / (CARD_W + CARD_GAP));
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
            if (selected) {
                roundBordered(context, x, y1, x + w, y2, C_ACCENT, C_ACCENT_SOFT);
            } else {
                roundBordered(context, x, y1, x + w, y2, hovered ? C_ACCENT : C_EDGE, hovered ? C_CARD_HOVER : C_CARD);
            }
            drawIconAsset(context, tabIcon(tab), x + 2, y1 + 1);
            if (showName) {
                text(context, tabName(tab), x + 20, y1 + 5, selected ? C_TEXT : C_MUTED);
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
        dirty = true;
    }

    private void toggleCurrent() {
        ArneStylesConfig config = ConfigManager.get();
        config.setEnabled(current, !config.isEnabled(current));
        ConfigManager.save();
        dirty = true;
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
        dirty = true;
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
            dirty = true;
        }
        if (needsReload) {
            dirty = true;
        }
        ConfigManager.save();
    }

    private void adjustFireDepth(float step) {
        ArneStylesConfig config = ConfigManager.get();
        float next = Math.round((config.lowFireOffset + step) * 100f) / 100f;
        config.lowFireOffset = Math.max(0.0f, Math.min(0.5f, next));
        ConfigManager.save();
    }

    private void reloadStyles() {
        CloudStyles.retry(current);
        StyleRegistry.reload();
        PreviewTextures.clear();
        previewAssetCache.clear();
        uniquePreviewCache.clear();
        soundCountCache.clear();
        styleHasFileCache.clear();
        cardScroll = 0;
        dirty = true;
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

        if (key == GLFW.GLFW_KEY_DELETE) {
            if (resetArmed) {
                ConfigManager.resetToDefaults();
                search.setLength(0);
                listScroll = 0;
                cardScroll = 0;
                current = parseCategory(ConfigManager.get().activeCategory);
                resetArmed = false;
                dirty = true;
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
            dirty = true;
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
        PreviewTextures.clear();
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
                dirty = true;
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
        wheelApply = null;
        if (PackImporter.pollFinished()) {
            // Neue Styles: Vorschauen neu laden, beim Schliessen Texturen neu laden
            PreviewTextures.clear();
            previewAssetCache.clear();
            uniquePreviewCache.clear();
            soundCountCache.clear();
            styleHasFileCache.clear();
            dirty = true;
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
        drawFooter(context);
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
            int cx = paneX + (slot % columns) * (CARD_W + CARD_GAP);
            int cy = cardsTop + (slot / columns) * (CARD_H + CARD_GAP);
            StyleDefinition style = styles.get(i);
            drawCard(context, style, cx, cy, style.id().equals(selected.id()), enabled && config.modEnabled, mouseX, mouseY);
        }

        if (styles.size() == 1 && columns > 1) {
            int textX = paneX + CARD_W + CARD_GAP + 6;
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
        boolean hovered = inside(mouseX, mouseY, x, y, x + CARD_W, y + CARD_H);
        int border = selected ? C_ACCENT : (hovered ? C_EDGE_HOVER : C_EDGE);
        roundBordered(context, x, y, x + CARD_W, y + CARD_H, border, hovered ? C_CARD_HOVER : C_CARD);
        if (selected) {
            round(context, x + 1, y + 1, x + CARD_W - 1, y + 3, C_ACCENT);
        }

        int px = x + (CARD_W - PREVIEW) / 2;
        int py = y + 5;
        round(context, px, py, px + PREVIEW, py + PREVIEW, C_FIELD);
        drawPreview(context, style, px, py, PREVIEW);

        String name = this.textRenderer.trimToWidth(style.name().getString(), CARD_W - 8);
        text(context, name, x + 4, py + PREVIEW + 4, C_TEXT);
        if (selected) {
            Text status = Text.translatable(active ? "arnestyles.gui.status_selected" : "arnestyles.gui.inactive");
            text(context, status, x + 4, py + PREVIEW + 14, active ? C_OK : C_WARN);
        }
        hits.add(new Hit(x, y, x + CARD_W, y + CARD_H, () -> chooseStyle(style)));
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
            dirty = true;
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
            dirty = true;
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
            dirty = true;
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

        // Darunter: Helligkeit, dann (bei Erzen) Nur-Flecken und die Knoepfe
        int ry = y0 + Math.max(wheelSize, 2 * rowStep) + 4;
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
            dirty = true;
        });
        bx = drawPillRight(context, bx - 4, ry + 2, Text.translatable("arnestyles.customize.reset"), C_MUTED, mouseX, mouseY, () -> {
            if (advanced) {
                TintedTextures.clearStateTint(current, styleKey, asset);
            } else {
                TintedTextures.clearTextureTint(current, styleKey, asset);
            }
            ConfigManager.save();
            dirty = true;
        });
        if (current == StyleCategory.ORES && asset.contains("_ore") && bx - 4 - ex >= 60) {
            drawTintSwitch(context, ex, ry, bx - 4 - ex, Text.translatable("arnestyles.tint.ore_spots"), tint.oreSpots, "arnestyles.tint.ore_spots.desc",
                mouseX, mouseY, () -> tint.oreSpots = !tint.oreSpots);
        }

        // Vorschau: links das Pack wie auf den Karten, rechts die gewaehlte Textur gross (beides mit der Farbe)
        // Platz rechts unter den Knoepfen oder links unter den Kacheln, je nachdem wo mehr frei ist
        int rightY = ry + rowStep + 4;
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
        }

        Text info = hovered != null ? hovered : Text.translatable("arnestyles.customize.info", asset.substring(asset.lastIndexOf('/') + 1));
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
        int w = paneRight - paneX;
        int infoY = top + HEADER_H + 26;
        int y0 = top + HEADER_H + 42;
        Text hovered = null;

        List<Path> packs = PackImporter.packs();
        // Ablage-Flaeche mit "Alle importieren"
        int boxH = 30;
        roundBordered(context, paneX, y0, paneRight, y0 + boxH, C_ACCENT, C_ACCENT_SOFT);
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
                nameEnd = drawPillRight(context, paneRight - 3, y + 2,
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
                ModRow.section("arnestyles.mod.section.info_hud", null),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES),
                ModRow.toggle(ModSetting.Toggle.KEYSTROKES_CPS),
                ModRow.toggle(ModSetting.Toggle.CPS_HUD),
                ModRow.toggle(ModSetting.Toggle.FPS_HUD),
                ModRow.toggle(ModSetting.Toggle.PING_HUD),
                ModRow.toggle(ModSetting.Toggle.COORDS_HUD),
                ModRow.section("arnestyles.mod.section.pvp_hud", null),
                ModRow.toggle(ModSetting.Toggle.COMBO_HUD),
                ModRow.toggle(ModSetting.Toggle.PEARL_COOLDOWN),
                ModRow.toggle(ModSetting.Toggle.ARMOR_WARNING),
                ModRow.toggle(ModSetting.Toggle.ARMOR_WARNING_SOUND),
                ModRow.toggle(ModSetting.Toggle.TARGET_HUD),
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
                ModRow.section("arnestyles.mod.section.pvp_visuals", null),
                ModRow.toggle(ModSetting.Toggle.HIT_COLOR),
                new ModRow(null, null, null, true, null, "hit_color"),
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
                ModRow.section("arnestyles.mod.section.motion_blur", ModSetting.MOTION_BLUR_MOD),
                ModRow.toggle(ModSetting.Toggle.MOTION_BLUR),
                ModRow.slider(ModSetting.Slider.MOTION_BLUR_STRENGTH),
                ModRow.section("arnestyles.mod.section.inventory_scale", null),
                ModRow.slider(ModSetting.Slider.INVENTORY_SCALE)
            )),
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
                ModRow.section("arnestyles.mod.section.no_chat_restrictions", ModSetting.NO_CHAT_RESTRICTIONS_MOD),
                ModRow.toggle(ModSetting.Toggle.NO_CHAT_RESTRICTIONS),
                ModRow.section("arnestyles.mod.section.borderless", ModSetting.CUBES_WITHOUT_BORDERS_MOD),
                ModRow.toggle(ModSetting.Toggle.BORDERLESS_FULLSCREEN)
            ))
        );
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
        drawBigText(context, Text.translatable("arnestyles.gui.mods"), paneX, headY, 1.3f, C_TEXT);

        int view = GuiLayout.Option.MODS_VIEW.value();
        int paneW = paneRight - paneX;
        int listTopY = top + HEADER_H + 42;
        Text hovered = null;
        List<ModRow> rows;
        if (view == 0) {
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
            int lineH = tiles ? 2 * ESSENTIAL_ROW_H : ESSENTIAL_ROW_H;
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
            int x = groupRow || view > 0 ? paneX : paneX + 8;
            int w = groupRow || view > 0 ? paneW : paneW - 8;
            int y = lineY;
            lineY += lineH;
            int y2 = y + ESSENTIAL_ROW_H - 2;
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
                Text side = Text.translatable("arnestyles.mod.group.count", group.mods(), group.activeToggles());
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
                hits.add(new Hit(x, y, x + w, y2, this::openHudEditor));
            } else if (row.color()) {
                // Farbzeile: Pop Chams (ohne Schluessel), Treffer-Farbe, Umrandung oder Himmel
                String colorKey = row.action() == null ? "pop_chams_color" : row.action();
                String nameKey = switch (colorKey) {
                    case "hit_color" -> "arnestyles.mod.hit_color_pick";
                    case "outline_color" -> "arnestyles.mod.outline_color";
                    case "sky_color" -> "arnestyles.mod.sky_color_pick";
                    case "hitbox_color", "hitbox_target_color", "hitbox_hurt_color", "hitbox_eye_color", "hitbox_look_color",
                         "hitbox_outline_color" -> "arnestyles.mod." + colorKey + "_pick";
                    default -> "arnestyles.mod.pop_chams_color";
                };
                int[] palette = "sky_color".equals(colorKey) ? WorldVisuals.SKY_COLORS
                    : colorKey.startsWith("hitbox_") ? com.arnestyles.client.mods.CombatHitboxes.COLORS : ModSetting.POP_CHAMS_COLORS;
                int current = switch (colorKey) {
                    case "hit_color" -> config.hitColorRgb;
                    case "outline_color" -> config.outlineColorRgb;
                    case "sky_color" -> config.skyColorRgb;
                    case "hitbox_color" -> config.hitboxRgb;
                    case "hitbox_target_color" -> config.hitboxTargetRgb;
                    case "hitbox_hurt_color" -> config.hitboxHurtRgb;
                    case "hitbox_eye_color" -> config.hitboxEyeRgb;
                    case "hitbox_look_color" -> config.hitboxLookRgb;
                    case "hitbox_outline_color" -> config.hitboxOutlineRgb;
                    default -> config.popChamsColor;
                } & 0xFFFFFF;
                boolean over = inside(mouseX, mouseY, x, y, x + w, y2);
                if (over) {
                    hovered = Text.translatable(nameKey + ".desc");
                }
                round(context, x, y, x + w, y2, over ? C_CARD_HOVER : C_CARD);
                text(context, Text.translatable(nameKey), x + 8, y + 5, C_TEXT);
                int sx = x + w - 6;
                for (int c = palette.length - 1; c >= 0; c--) {
                    int rgb = palette[c];
                    int x1 = sx - 12;
                    if (current == rgb) {
                        round(context, x1 - 1, y + 2, sx + 1, y2 - 2, C_TEXT);
                    }
                    round(context, x1, y + 3, sx, y2 - 3, 0xFF000000 | rgb);
                    final int pick = rgb;
                    hits.add(new Hit(x1, y + 2, sx, y2 - 2, () -> {
                        ArneStylesConfig edit = ConfigManager.get();
                        switch (colorKey) {
                            case "hit_color" -> edit.hitColorRgb = pick;
                            case "outline_color" -> edit.outlineColorRgb = pick;
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
                    }));
                    sx = x1 - 3;
                }
            }
        }
        if (hudEditorHint && hovered == null) {
            hovered = Text.translatable("arnestyles.mod.hud_editor.no_world");
        }
        String info = hovered != null ? hovered.getString() : Text.translatable("arnestyles.gui.mods_info").getString();
        text(context, this.textRenderer.trimToWidth(info, paneRight - paneX), paneX, top + HEADER_H + 26,
            hovered != null ? C_TEXT : C_MUTED);
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
            dirty = true;
            previewAssetCache.clear();
        } else {
            presetMessage = Text.translatable("arnestyles.gui.preset_error").getString();
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
                    drawTile(context, entry, x + pad, y + pad, size - 2 * pad, tileColor);
                    if (modelOnly(style)) {
                        Text badge = Text.translatable("arnestyles.gui.model_only");
                        int bw = this.textRenderer.getWidth(badge) + 6;
                        round(context, x + size - bw - 3, y + 3, x + size - 3, y + 13, C_WARN);
                        text(context, badge, x + size - bw, y + 4, 0xFF1A1D2B);
                    } else if (isModel3d(style, asset)) {
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

    private void drawFooter(DrawContext context) {
        context.fill(left, bottom - FOOTER_H, right, bottom - FOOTER_H + 1, C_EDGE);
        int maxWidth = right - left - 20;
        boolean danger = resetArmed;
        String key = resetArmed ? "arnestyles.gui.reset_confirm" : sortMode ? "arnestyles.gui.sort_hint" : "arnestyles.gui.hint";
        String text = this.textRenderer.trimToWidth(Text.translatable(key).getString(), maxWidth);
        text(context, text, left + 10, bottom - FOOTER_H + 6, danger ? C_DANGER : C_MUTED);
    }
}
