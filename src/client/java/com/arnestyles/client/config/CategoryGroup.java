package com.arnestyles.client.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ordner in der Seitenleiste: die Spielmodi (Kits wie bei MCTiers) und Terrain.
 * Ein Ordner zeigt beim Aufklappen die Kategorien der Items, die man in diesem Kit benutzt.
 * Eine Kategorie darf in mehreren Ordnern stehen, es bleibt trotzdem dieselbe Einstellung.
 *
 * Neuen Spielmodus hinzufuegen: hier einen Eintrag anlegen, in SIDEBAR einordnen
 * und in den Sprachdateien "arnestyles.group.<name>" eintragen.
 */
public enum CategoryGroup {
    /** Crystal PvP: Netherite-Kit mit Kristallen, Ankern, Perlen, Totems. */
    VANILLA("textures/item/end_crystal.png",
        StyleCategory.CRYSTAL, StyleCategory.OBSIDIAN, StyleCategory.GLOWSTONE, StyleCategory.ANCHOR, StyleCategory.FIRE,
        StyleCategory.TOTEMS, StyleCategory.PEARLS, StyleCategory.GAPS, StyleCategory.XP_BOTTLES, StyleCategory.SWORDS,
        StyleCategory.ARMOR, StyleCategory.GLINT, StyleCategory.ITEMS),
    /** Diamant-Ruestung, Schwert und Axt, Eimer, Boegen, Gaps, Bloecke, Spinnweben. */
    UHC("textures/item/lava_bucket.png",
        StyleCategory.SWORDS, StyleCategory.AXES, StyleCategory.ARMOR, StyleCategory.SHIELD, StyleCategory.BUCKETS, StyleCategory.BOWS,
        StyleCategory.GAPS, StyleCategory.BLOCKS, StyleCategory.COBWEBS, StyleCategory.TOOLS, StyleCategory.GLINT),
    /** Diamant-Ruestung, Schwert, Steak, Heil-, Regen-, Speed- und Strength-Traenke. */
    POT("textures/item/splash_potion.png",
        StyleCategory.SWORDS, StyleCategory.ARMOR, StyleCategory.POTIONS, StyleCategory.FOOD, StyleCategory.GLINT),
    /** Verzauberte Netherite-Ruestung und -Schwert, Traenke, XP-Flaschen, Totems. */
    NETH_OP("textures/item/netherite_sword.png",
        StyleCategory.SWORDS, StyleCategory.ARMOR, StyleCategory.GLINT, StyleCategory.POTIONS, StyleCategory.XP_BOTTLES, StyleCategory.TOTEMS),
    /** Netherite, zwei Schwerter, Axt, Schild, Totem, Gaps, Perlen, Traenke. */
    SMP("textures/item/netherite_chestplate.png",
        StyleCategory.SWORDS, StyleCategory.AXES, StyleCategory.ARMOR, StyleCategory.SHIELD, StyleCategory.TOTEMS, StyleCategory.GAPS,
        StyleCategory.PEARLS, StyleCategory.POTIONS, StyleCategory.GLINT),
    /** Verzauberte Diamant-Ruestung und -Schwert. */
    SWORD("textures/item/diamond_sword.png",
        StyleCategory.SWORDS, StyleCategory.ARMOR, StyleCategory.GLINT),
    /** Diamant-Ruestung, Schwert, Axt, Schild, Bogen und Armbrust. */
    AXE("textures/item/diamond_axe.png",
        StyleCategory.AXES, StyleCategory.SWORDS, StyleCategory.SHIELD, StyleCategory.ARMOR, StyleCategory.BOWS, StyleCategory.GLINT),
    /** Mace, Netherite, Traenke, Wind Charges, Perlen, Axt, Gaps, Schild, Elytra. */
    MACE("textures/item/mace.png",
        StyleCategory.MACE, StyleCategory.SWORDS, StyleCategory.AXES, StyleCategory.ARMOR, StyleCategory.POTIONS, StyleCategory.WIND_CHARGES,
        StyleCategory.PEARLS, StyleCategory.GAPS, StyleCategory.SHIELD, StyleCategory.ELYTRA, StyleCategory.GLINT),
    /** Speer und Mace, Netherite, Totem, Perlen, Axt, Gaps, Schild, Steak, Wind Charges, Shulker, Traenke. */
    SPEAR_MACE("textures/item/netherite_spear.png",
        StyleCategory.SPEARS, StyleCategory.MACE, StyleCategory.SWORDS, StyleCategory.AXES, StyleCategory.ARMOR, StyleCategory.TOTEMS,
        StyleCategory.PEARLS, StyleCategory.GAPS, StyleCategory.SHIELD, StyleCategory.FOOD, StyleCategory.WIND_CHARGES,
        StyleCategory.SHULKER, StyleCategory.POTIONS, StyleCategory.GLINT),
    /** Wie die Welt aussieht. */
    TERRAIN("textures/block/grass_block_top.png",
        StyleCategory.STONE, StyleCategory.DEEPSLATE, StyleCategory.ORES, StyleCategory.GRASS, StyleCategory.SNOW, StyleCategory.FIRE, StyleCategory.SKY);

    /**
     * Reihenfolge der Seitenleiste: Ordner und einzelne Kategorien gemischt.
     * Jede Kategorie, die hier nicht direkt steht, ist ueber mindestens einen Ordner erreichbar.
     */
    public static final List<Object> SIDEBAR = List.of(
        VANILLA, UHC, POT, NETH_OP, SMP, SWORD, AXE, MACE, SPEAR_MACE, TERRAIN,
        StyleCategory.INVENTORY, StyleCategory.HUD, StyleCategory.CROSSHAIR,
        StyleCategory.PARTICLES, StyleCategory.SOUNDS, StyleCategory.ESSENTIALS
    );

    /** Fruehere Ordnernamen (Version 1.4.0) -> heutige, damit aufgeklappte Ordner erhalten bleiben. */
    public static final Map<String, String> RENAMED = Map.of(
        "CRYSTAL_PVP", "VANILLA", "SWORD_PVP", "SWORD", "AXE_PVP", "AXE", "MACE_PVP", "MACE"
    );

    private final String icon;
    private final List<StyleCategory> categories;

    CategoryGroup(String icon, StyleCategory... categories) {
        this.icon = icon;
        this.categories = List.of(categories);
    }

    /** Symbol in der Seitenleiste, relativ zu assets/minecraft/. */
    public String icon() {
        return icon;
    }

    public List<StyleCategory> categories() {
        return categories;
    }

    public String nameKey() {
        return "arnestyles.group." + name().toLowerCase(Locale.ROOT);
    }
}
