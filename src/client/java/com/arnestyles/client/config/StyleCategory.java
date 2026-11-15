package com.arnestyles.client.config;

import java.util.List;
import java.util.Locale;

/**
 * Kategorien im Styles-Menue. Jede Kategorie hat einen eigenen Ordner unter
 * config/arnestyles/styles/ und eine Art, wie die Vorschau gezeichnet wird.
 * Die Reihenfolge bestimmt auch den Vorrang: spaetere Kategorien ueberschreiben fruehere
 * (z. B. Gaps nach Items, damit ein Gap-Style auch ueber einem Items-Style gilt).
 */
public enum StyleCategory {
    CRYSTAL(PreviewKind.CRYSTAL, "textures/entity/end_crystal/end_crystal.png"),
    OBSIDIAN(PreviewKind.TILE, "textures/block/obsidian.png", "textures/block/crying_obsidian.png"),
    GLOWSTONE(PreviewKind.TILE, "textures/block/glowstone.png", "textures/item/glowstone_dust.png"),
    ANCHOR(PreviewKind.TILE, "textures/block/respawn_anchor_side4.png", "textures/block/respawn_anchor_top.png"),
    FIRE(PreviewKind.TILE, "textures/block/fire_0.png", "textures/block/soul_fire_0.png"),
    STONE(PreviewKind.TILE, "textures/block/stone.png", "textures/block/andesite.png", "textures/block/diorite.png"),
    DEEPSLATE(PreviewKind.TILE, "textures/block/deepslate.png", "textures/block/cobbled_deepslate.png", "textures/block/polished_deepslate.png"),
    /** Erze aus Stone und Deepslate, eigene Kategorie seit Version 2.1. */
    ORES(PreviewKind.TILE, "textures/block/diamond_ore.png", "textures/block/deepslate_diamond_ore.png", "textures/block/iron_ore.png",
        "textures/block/redstone_ore.png"),
    GRASS(PreviewKind.TILE, "textures/block/grass_block_top.png", "textures/block/dirt.png", "textures/block/short_grass.png"),
    SNOW(PreviewKind.TILE, "textures/block/snow.png", "textures/block/powder_snow.png", "textures/block/ice.png"),
    BLOCKS(PreviewKind.TILE, "textures/block/cobblestone.png", "textures/block/oak_planks.png"),
    COBWEBS(PreviewKind.TILE, "textures/block/cobweb.png"),
    SHULKER(PreviewKind.TILE, "textures/block/shulker_box.png", "textures/block/purple_shulker_box.png"),
    INVENTORY(PreviewKind.INVENTORY, "textures/gui/container/inventory.png"),
    HUD(PreviewKind.FIT, "textures/gui/sprites/hud/hotbar.png", "textures/gui/sprites/hud/heart/full.png"),
    CROSSHAIR(PreviewKind.FIT, "textures/gui/sprites/hud/crosshair.png"),
    ITEMS(PreviewKind.TILE, "textures/item/end_crystal.png", "textures/item/firework_rocket.png", "textures/item/fishing_rod.png"),
    TOTEMS(PreviewKind.TILE, "textures/item/totem_of_undying.png", "textures/particle/glitter_0.png"),
    GAPS(PreviewKind.TILE, "textures/item/golden_apple.png"),
    XP_BOTTLES(PreviewKind.TILE, "textures/item/experience_bottle.png", "textures/entity/experience_orb.png"),
    PEARLS(PreviewKind.TILE, "textures/item/ender_pearl.png"),
    POTIONS(PreviewKind.TILE, "textures/item/splash_potion.png", "textures/item/potion.png", "textures/item/potion_overlay.png"),
    WIND_CHARGES(PreviewKind.TILE, "textures/item/wind_charge.png"),
    BUCKETS(PreviewKind.TILE, "textures/item/water_bucket.png", "textures/item/lava_bucket.png", "textures/item/bucket.png"),
    FOOD(PreviewKind.TILE, "textures/item/cooked_beef.png", "textures/item/golden_carrot.png", "textures/item/bread.png"),
    SHIELD(PreviewKind.SHIELD, "textures/entity/shield_base_nopattern.png", "textures/entity/shield/base_nopattern.png"),
    SWORDS(PreviewKind.TILE, "textures/item/netherite_sword.png", "textures/item/diamond_sword.png", "textures/item/iron_sword.png"),
    AXES(PreviewKind.TILE, "textures/item/netherite_axe.png", "textures/item/diamond_axe.png", "textures/item/iron_axe.png"),
    MACE(PreviewKind.TILE, "textures/item/mace.png"),
    SPEARS(PreviewKind.TILE, "textures/item/netherite_spear.png", "textures/item/diamond_spear.png", "textures/item/iron_spear.png"),
    BOWS(PreviewKind.TILE, "textures/item/bow.png", "textures/item/crossbow_standby.png", "textures/item/arrow.png"),
    TOOLS(PreviewKind.TILE, "textures/item/netherite_pickaxe.png", "textures/item/diamond_pickaxe.png", "textures/item/trident.png"),
    ARMOR(PreviewKind.ARMOR, "textures/entity/equipment/humanoid/netherite.png", "textures/entity/equipment/humanoid/diamond.png",
        "textures/item/netherite_chestplate.png", "textures/item/diamond_chestplate.png"),
    ELYTRA(PreviewKind.TILE, "textures/item/elytra.png"),
    GLINT(PreviewKind.TILE, "textures/misc/enchanted_glint_item.png", "textures/misc/enchanted_glint_armor.png"),
    PARTICLES(PreviewKind.TILE, "textures/particle/critical_hit.png", "textures/particle/enchanted_hit.png", "textures/particle/explosion_0.png",
        "textures/particle/generic_0.png"),
    SKY(PreviewKind.FIT, "textures/environment/celestial/sun.png", "textures/environment/celestial/moon/full_moon.png",
        "textures/environment/clouds.png", "textures/environment/rain.png"),
    SOUNDS(PreviewKind.SOUND, "textures/block/note_block.png"),
    ESSENTIALS(PreviewKind.ESSENTIALS, "textures/item/glowstone_dust.png");

    /** Wie die Vorschau einer Kategorie aussieht. */
    public enum PreviewKind {
        /** Echter, drehender Endkristall. */
        CRYSTAL,
        /** Block- oder Item-Textur, animiert wenn die Textur mehrere Frames hat. */
        TILE,
        /** Ganze Textur, ins Vorschau-Feld eingepasst (z. B. Hotbar oder Sonne). */
        FIT,
        /** Ausschnitt aus der Inventar-Textur. */
        INVENTORY,
        /** Vorderseite des Schilds aus der Schild-Textur. */
        SHIELD,
        /** Netherite- und Diamant-Ruestung nebeneinander (Kopf und Oberkoerper). */
        ARMOR,
        /** Notenblock-Symbol und Anzahl der Sounds im Style. */
        SOUND,
        /** Keine Styles, sondern eine Liste von Schaltern (siehe Essential). */
        ESSENTIALS
    }

    private final PreviewKind previewKind;
    private final List<String> previewAssets;

    StyleCategory(PreviewKind previewKind, String... previewAssets) {
        this.previewKind = previewKind;
        this.previewAssets = List.of(previewAssets);
    }

    public PreviewKind previewKind() {
        return previewKind;
    }

    /**
     * Moegliche Texturen fuer die Vorschau, relativ zu assets/minecraft/.
     * Angezeigt wird die erste, die der Style mitbringt, sonst die erste aus Minecraft.
     * Die erste Textur dient auch als Symbol in der Kategorie-Liste.
     */
    public List<String> previewAssets() {
        return previewAssets;
    }

    /** Name des Unterordners in config/arnestyles/styles/. */
    public String folderName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
