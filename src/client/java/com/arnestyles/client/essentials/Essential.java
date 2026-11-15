package com.arnestyles.client.essentials;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;

/**
 * Eingebaute PvP-Essentials. Jedes kann einzeln an- und ausgeschaltet werden.
 * CODE wirkt sofort ueber Mixins, PACK bringt eigene Modelle/Texturen aus der Mod mit
 * (src/main/resources/essentials/<id>/assets/...) und braucht ein Neuladen der Ressourcen.
 */
public enum Essential {
    FULLBRIGHT("fullbright", Kind.CODE, "textures/item/glowstone_dust.png"),
    NO_DARKNESS("no_darkness", Kind.CODE, "textures/item/echo_shard.png"),
    LOW_FIRE("low_fire", Kind.CODE, "textures/block/fire_0.png"),
    NO_EXPLOSION_PARTICLES("no_explosion_particles", Kind.CODE, "textures/item/end_crystal.png"),
    NO_SMOKE("no_smoke", Kind.CODE, "textures/item/campfire.png"),
    NO_PUMPKIN_OVERLAY("no_pumpkin_overlay", Kind.CODE, "textures/block/carved_pumpkin.png"),
    NO_FOG("no_fog", Kind.CODE, "textures/item/water_bucket.png"),
    SMALL_TOTEM("small_totem", Kind.PACK, "textures/item/totem_of_undying.png"),
    SMALL_GAP("small_gap", Kind.PACK, "textures/item/golden_apple.png"),
    SMALL_MACE("small_mace", Kind.PACK, "textures/item/mace.png"),
    SMALL_ITEMS("small_items", Kind.PACK, "textures/item/ender_pearl.png"),
    LOW_SHIELD("low_shield", Kind.PACK, "textures/item/empty_armor_slot_shield.png"),
    OBVIOUS_POWDER_SNOW("obvious_powder_snow", Kind.PACK, "textures/block/powder_snow.png");

    public enum Kind {
        CODE,
        PACK
    }

    private final String id;
    private final Kind kind;
    private final String icon;

    Essential(String id, Kind kind, String icon) {
        this.id = id;
        this.kind = kind;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** Textur fuer das Symbol im Menue, relativ zu assets/minecraft/. */
    public String icon() {
        return icon;
    }

    public String nameKey() {
        return "arnestyles.essential." + id;
    }

    public String descriptionKey() {
        return "arnestyles.essential." + id + ".desc";
    }

    /** Pfad der mitgelieferten Dateien in der Mod-Jar. */
    public String resourceFolder() {
        return "essentials/" + id;
    }

    /** true, wenn die Kategorie Essentials aktiv ist und dieses Essential an ist. */
    public boolean isActive() {
        ArneStylesConfig config = ConfigManager.get();
        return config.modEnabled && config.isEnabled(StyleCategory.ESSENTIALS) && config.isEssentialOn(id);
    }
}
