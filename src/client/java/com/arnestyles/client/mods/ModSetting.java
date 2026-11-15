package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.essentials.SettingSlider;

import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Einstellungen der Seite "Mods". */
public final class ModSetting {
    /** IDs der einzelnen Mods mit derselben Funktion. Sind sie installiert, bleibt die eingebaute Version aus. */
    public static final String POP_CHAMS_MOD = "walksypopchams";
    public static final String SHIELD_STATUS_MOD = "shieldstatus";
    public static final String TOTEM_COUNTER_MOD = "totemcounter";
    public static final String ARMOR_HUD_MOD = "ukus-armor-hud";
    public static final String EFFECT_TIMER_MOD = "statuseffecttimer";
    public static final String MOTION_BLUR_MOD = "naturalmotionblur";
    public static final String NO_CHAT_RESTRICTIONS_MOD = "nochatrestrictions";
    public static final String APPLESKIN_MOD = "appleskin";
    public static final String CUBES_WITHOUT_BORDERS_MOD = "cubes-without-borders";
    public static final String ZOOMIFY_MOD = "zoomify";
    public static final String COMBAT_HITBOXES_MOD = "combat-hitboxes";

    private ModSetting() {
    }

    public static boolean isLoaded(@Nullable String modId) {
        return modId != null && FabricLoader.getInstance().isModLoaded(modId);
    }

    /** Schalter. Wirkt nur, wenn der Hauptschalter der Mod an ist. */
    public enum Toggle {
        POP_CHAMS("pop_chams", POP_CHAMS_MOD, c -> c.popChams, (c, v) -> c.popChams = v),
        POP_CHAMS_OWN("pop_chams_own", POP_CHAMS_MOD, c -> c.popChamsOwn, (c, v) -> c.popChamsOwn = v),
        POP_CHAMS_FADE("pop_chams_fade", POP_CHAMS_MOD, c -> c.popChamsFade, (c, v) -> c.popChamsFade = v),
        POP_CHAMS_DISPERSE("pop_chams_disperse", POP_CHAMS_MOD, c -> c.popChamsDisperse, (c, v) -> c.popChamsDisperse = v),
        SHIELD_FIX("shield_fix", null, c -> c.shieldFix, (c, v) -> c.shieldFix = v),
        SHIELD_FIX_DELAY("shield_fix_delay", null, c -> c.shieldFixDelay, (c, v) -> c.shieldFixDelay = v),
        SHIELD_SOUNDS("shield_sounds", null, c -> c.shieldSounds, (c, v) -> c.shieldSounds = v),
        SHIELD_STATUS("shield_status", SHIELD_STATUS_MOD, c -> c.shieldStatus, (c, v) -> c.shieldStatus = v),
        SHIELD_STATUS_OTHERS("shield_status_others", SHIELD_STATUS_MOD, c -> c.shieldStatusOthers, (c, v) -> c.shieldStatusOthers = v),
        SHIELD_STATUS_FADE("shield_status_fade", SHIELD_STATUS_MOD, c -> c.shieldStatusFade, (c, v) -> c.shieldStatusFade = v),
        TOTEM_COUNTER("totem_counter", TOTEM_COUNTER_MOD, c -> c.totemCounter, (c, v) -> c.totemCounter = v),
        TOTEM_COUNTER_HUD("totem_counter_hud", TOTEM_COUNTER_MOD, c -> c.totemCounterHud, (c, v) -> c.totemCounterHud = v),
        ARMOR_HUD("armor_hud", ARMOR_HUD_MOD, c -> c.armorHud, (c, v) -> c.armorHud = v),
        ARMOR_HUD_RIGHT("armor_hud_right", ARMOR_HUD_MOD, c -> c.armorHudRight, (c, v) -> c.armorHudRight = v),
        ARMOR_HUD_PERCENT("armor_hud_percent", ARMOR_HUD_MOD, c -> c.armorHudPercent, (c, v) -> c.armorHudPercent = v),
        EFFECT_TIMER("effect_timer", EFFECT_TIMER_MOD, c -> c.effectTimer, (c, v) -> c.effectTimer = v),
        EFFECT_TIMER_LEVEL("effect_timer_level", EFFECT_TIMER_MOD, c -> c.effectTimerLevel, (c, v) -> c.effectTimerLevel = v),
        MOTION_BLUR("motion_blur", MOTION_BLUR_MOD, c -> c.motionBlur, (c, v) -> c.motionBlur = v),
        NO_CHAT_RESTRICTIONS("no_chat_restrictions", NO_CHAT_RESTRICTIONS_MOD, c -> c.noChatRestrictions, (c, v) -> c.noChatRestrictions = v),
        APPLESKIN_SATURATION("appleskin_saturation", APPLESKIN_MOD, c -> c.appleSkinSaturation, (c, v) -> c.appleSkinSaturation = v),
        APPLESKIN_PREVIEW("appleskin_preview", APPLESKIN_MOD, c -> c.appleSkinPreview, (c, v) -> c.appleSkinPreview = v),
        APPLESKIN_HEALTH("appleskin_health", APPLESKIN_MOD, c -> c.appleSkinHealth, (c, v) -> c.appleSkinHealth = v),
        APPLESKIN_TOOLTIP("appleskin_tooltip", APPLESKIN_MOD, c -> c.appleSkinTooltip, (c, v) -> c.appleSkinTooltip = v),
        BORDERLESS_FULLSCREEN("borderless_fullscreen", CUBES_WITHOUT_BORDERS_MOD, c -> c.borderlessFullscreen, (c, v) -> c.borderlessFullscreen = v),
        KEYSTROKES("keystrokes", null, c -> c.keystrokes, (c, v) -> c.keystrokes = v),
        KEYSTROKES_CPS("keystrokes_cps", null, c -> c.keystrokesCps, (c, v) -> c.keystrokesCps = v),
        CPS_HUD("cps_hud", null, c -> c.cpsHud, (c, v) -> c.cpsHud = v),
        FPS_HUD("fps_hud", null, c -> c.fpsHud, (c, v) -> c.fpsHud = v),
        PING_HUD("ping_hud", null, c -> c.pingHud, (c, v) -> c.pingHud = v),
        COORDS_HUD("coords_hud", null, c -> c.coordsHud, (c, v) -> c.coordsHud = v),
        COMBO_HUD("combo_hud", null, c -> c.comboHud, (c, v) -> c.comboHud = v),
        PEARL_COOLDOWN("pearl_cooldown", null, c -> c.pearlCooldown, (c, v) -> c.pearlCooldown = v),
        ARMOR_WARNING("armor_warning", null, c -> c.armorWarning, (c, v) -> c.armorWarning = v),
        ARMOR_WARNING_SOUND("armor_warning_sound", null, c -> c.armorWarningSound, (c, v) -> c.armorWarningSound = v),
        TARGET_HUD("target_hud", null, c -> c.targetHud, (c, v) -> c.targetHud = v),
        HIT_COLOR("hit_color", null, c -> c.hitColor, (c, v) -> c.hitColor = v),
        CRYSTAL_BOUNCE("crystal_bounce", null, c -> c.crystalBounce, (c, v) -> c.crystalBounce = v),
        KILL_EFFECTS("kill_effects", null, c -> c.killEffects, (c, v) -> c.killEffects = v),
        KILL_ONLY_PLAYERS("kill_only_players", null, c -> c.killOnlyPlayers, (c, v) -> c.killOnlyPlayers = v),
        KILL_LIGHTNING("kill_lightning", null, c -> c.killLightning, (c, v) -> c.killLightning = v),
        KILL_FIREWORK("kill_firework", null, c -> c.killFirework, (c, v) -> c.killFirework = v),
        KILL_TOTEM("kill_totem", null, c -> c.killTotem, (c, v) -> c.killTotem = v),
        KILL_SOUND("kill_sound", null, c -> c.killSound, (c, v) -> c.killSound = v),
        BLOCK_OUTLINE("block_outline", null, c -> c.blockOutline, (c, v) -> c.blockOutline = v),
        OUTLINE_RAINBOW("outline_rainbow", null, c -> c.outlineRainbow, (c, v) -> c.outlineRainbow = v),
        CUSTOM_TIME("custom_time", null, c -> c.customTimeOn, (c, v) -> c.customTimeOn = v),
        SKY_COLOR("sky_color", null, c -> c.skyColorOn, (c, v) -> c.skyColorOn = v),
        ITEM_PHYSICS("item_physics", null, c -> c.itemPhysics, (c, v) -> c.itemPhysics = v),
        ZOOM("zoom", ZOOMIFY_MOD, c -> c.zoom, (c, v) -> c.zoom = v),
        ZOOM_SMOOTH("zoom_smooth", ZOOMIFY_MOD, c -> c.zoomSmooth, (c, v) -> c.zoomSmooth = v),
        ZOOM_SCROLL("zoom_scroll", ZOOMIFY_MOD, c -> c.zoomScroll, (c, v) -> c.zoomScroll = v),
        ZOOM_SLOW_MOUSE("zoom_slow_mouse", ZOOMIFY_MOD, c -> c.zoomSlowMouse, (c, v) -> c.zoomSlowMouse = v),
        FREELOOK("freelook", null, c -> c.freelook, (c, v) -> c.freelook = v),
        FREELOOK_THIRD_PERSON("freelook_third_person", null, c -> c.freelookThirdPerson, (c, v) -> c.freelookThirdPerson = v),
        AUTO_GG("auto_gg", null, c -> c.autoGg, (c, v) -> c.autoGg = v),
        HITBOXES("hitboxes", COMBAT_HITBOXES_MOD, c -> c.hitboxes, (c, v) -> c.hitboxes = v),
        HITBOX_TARGET_COLOR("hitbox_target_color", COMBAT_HITBOXES_MOD, c -> c.hitboxTargetColor, (c, v) -> c.hitboxTargetColor = v),
        HITBOX_HURT_COLOR("hitbox_hurt_color", COMBAT_HITBOXES_MOD, c -> c.hitboxHurtColor, (c, v) -> c.hitboxHurtColor = v),
        HITBOX_EYES("hitbox_eyes", COMBAT_HITBOXES_MOD, c -> c.hitboxEyes, (c, v) -> c.hitboxEyes = v),
        HITBOX_LOOK("hitbox_look", COMBAT_HITBOXES_MOD, c -> c.hitboxLook, (c, v) -> c.hitboxLook = v),
        HITBOX_LOOK_ARROW("hitbox_look_arrow", COMBAT_HITBOXES_MOD, c -> c.hitboxLookArrow, (c, v) -> c.hitboxLookArrow = v),
        HITBOX_OUTLINE("hitbox_outline", COMBAT_HITBOXES_MOD, c -> c.hitboxOutline, (c, v) -> c.hitboxOutline = v),
        HITBOX_HIDE_FIREWORKS("hitbox_hide_fireworks", COMBAT_HITBOXES_MOD, c -> c.hitboxHideFireworks, (c, v) -> c.hitboxHideFireworks = v),
        HITBOX_HIDE_ITEMS("hitbox_hide_items", COMBAT_HITBOXES_MOD, c -> c.hitboxHideItems, (c, v) -> c.hitboxHideItems = v),
        HIDE_STUCK_ARROWS("hide_stuck_arrows", COMBAT_HITBOXES_MOD, c -> c.hideStuckArrows, (c, v) -> c.hideStuckArrows = v);

        private final String id;
        private final @Nullable String externalMod;
        private final Predicate<ArneStylesConfig> getter;
        private final BiConsumer<ArneStylesConfig, Boolean> setter;

        Toggle(String id, @Nullable String externalMod, Predicate<ArneStylesConfig> getter, BiConsumer<ArneStylesConfig, Boolean> setter) {
            this.id = id;
            this.externalMod = externalMod;
            this.getter = getter;
            this.setter = setter;
        }

        /** true, wenn die gleiche Funktion schon als eigene Mod installiert ist. Dann bleibt die eingebaute aus. */
        public boolean blockedByMod() {
            return isLoaded(externalMod);
        }

        public @Nullable String externalMod() {
            return externalMod;
        }

        public String nameKey() {
            return "arnestyles.mod." + id;
        }

        public String descriptionKey() {
            return "arnestyles.mod." + id + ".desc";
        }

        public boolean value() {
            return getter.test(ConfigManager.get());
        }

        public void toggle() {
            setter.accept(ConfigManager.get(), !value());
            ConfigManager.save();
        }

        /** An, der Hauptschalter ist an, und keine eigene Mod mit derselben Funktion ist installiert. */
        public boolean isActive() {
            return ConfigManager.get().modEnabled && value() && !blockedByMod();
        }
    }

    /** Regler. */
    public enum Slider implements SettingSlider {
        POP_CHAMS_LIFETIME("pop_chams_lifetime", 0.5f, 5.0f, 1.5f, 0.1f, c -> c.popChamsLifetime, (c, v) -> c.popChamsLifetime = v),
        POP_CHAMS_OPACITY("pop_chams_opacity", 0.1f, 1.0f, 0.6f, 0.05f, c -> c.popChamsOpacity, (c, v) -> c.popChamsOpacity = v),
        INVENTORY_SCALE("inventory_scale", 0.5f, 2.0f, 1.0f, 0.05f, c -> c.inventoryScale, (c, v) -> c.inventoryScale = v),
        MOTION_BLUR_STRENGTH("motion_blur_strength", 0.1f, 0.9f, 0.5f, 0.05f, c -> c.motionBlurStrength, (c, v) -> c.motionBlurStrength = v),
        CRYSTAL_SIZE("crystal_size", 0.3f, 1.5f, 1.0f, 0.05f, c -> c.crystalSize, (c, v) -> c.crystalSize = v),
        CRYSTAL_SPEED("crystal_speed", 0.0f, 3.0f, 1.0f, 0.1f, c -> c.crystalRotationSpeed, (c, v) -> c.crystalRotationSpeed = v),
        PLAYER_FIRE("player_fire", 0.0f, 0.9f, 0.0f, 0.05f, c -> c.playerFireOffset, (c, v) -> c.playerFireOffset = v),
        TOTEM_POP_SOUND("totem_pop_sound", 0.0f, 25.0f, 0.0f, 1.0f, c -> c.totemPopSound, (c, v) -> c.totemPopSound = v),
        TOTEM_POP_VOLUME("totem_pop_volume", 0.0f, 2.0f, 1.0f, 0.05f, c -> c.totemPopVolume, (c, v) -> c.totemPopVolume = v),
        OUTLINE_WIDTH("outline_width", 1.0f, 6.0f, 2.0f, 0.5f, c -> c.outlineWidth, (c, v) -> c.outlineWidth = v),
        CUSTOM_TIME_VALUE("custom_time_value", 0.0f, 23750.0f, 6000.0f, 250.0f, c -> c.customTime, (c, v) -> c.customTime = v),
        ZOOM_LEVEL("zoom_level", 2.0f, 10.0f, 4.0f, 0.5f, c -> c.zoomLevel, (c, v) -> c.zoomLevel = v),
        HITBOX_WIDTH("hitbox_width", 0.5f, 6.0f, 2.5f, 0.5f, c -> c.hitboxWidth, (c, v) -> c.hitboxWidth = v),
        HITBOX_FAR_WIDTH("hitbox_far_width", 0.5f, 6.0f, 2.5f, 0.5f, c -> c.hitboxFarWidth, (c, v) -> c.hitboxFarWidth = v),
        HITBOX_FAR_DISTANCE("hitbox_far_distance", 4.0f, 64.0f, 32.0f, 1.0f, c -> c.hitboxFarDistance, (c, v) -> c.hitboxFarDistance = v),
        HITBOX_OUTLINE_SIZE("hitbox_outline_size", 1.0f, 4.0f, 2.0f, 0.25f, c -> c.hitboxOutlineSize, (c, v) -> c.hitboxOutlineSize = v);

        private final String id;
        private final float min;
        private final float max;
        private final float standard;
        private final float step;
        private final Function<ArneStylesConfig, Float> getter;
        private final BiConsumer<ArneStylesConfig, Float> setter;

        Slider(String id, float min, float max, float standard, float step, Function<ArneStylesConfig, Float> getter,
               BiConsumer<ArneStylesConfig, Float> setter) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.standard = standard;
            this.step = step;
            this.getter = getter;
            this.setter = setter;
        }

        public float value() {
            return getter.apply(ConfigManager.get());
        }

        @Override
        public String nameKey() {
            return "arnestyles.mod." + id;
        }

        @Override
        public String descriptionKey() {
            return "arnestyles.mod." + id + ".desc";
        }

        @Override
        public float fraction() {
            return (value() - min) / (max - min);
        }

        @Override
        public void setFraction(float fraction) {
            float f = Math.max(0.0f, Math.min(1.0f, fraction));
            float raw = min + f * (max - min);
            float rounded = Math.round(raw / step) * step;
            setter.accept(ConfigManager.get(), Math.max(min, Math.min(max, rounded)));
        }

        @Override
        public void reset() {
            setter.accept(ConfigManager.get(), standard);
        }

        @Override
        public String display() {
            return switch (this) {
                case POP_CHAMS_LIFETIME -> String.format(Locale.ROOT, "%.1f s", value());
                case INVENTORY_SCALE -> String.format(Locale.ROOT, "%.2fx", value());
                case OUTLINE_WIDTH, ZOOM_LEVEL, HITBOX_WIDTH, HITBOX_FAR_WIDTH -> String.format(Locale.ROOT, "%.1fx", value());
                case HITBOX_OUTLINE_SIZE -> String.format(Locale.ROOT, "%.2fx", value());
                case HITBOX_FAR_DISTANCE -> Math.round(value()) + " m";
                case CUSTOM_TIME_VALUE -> WorldVisuals.clock(value());
                case TOTEM_POP_SOUND -> net.minecraft.text.Text.translatable("arnestyles.mod.totem_pop_sound." + PvpVisuals.POP_SOUND_KEYS[
                    Math.max(0, Math.min(PvpVisuals.POP_SOUND_KEYS.length - 1, Math.round(value())))]).getString();
                default -> String.format(Locale.ROOT, "%d%%", Math.round(value() * 100.0f));
            };
        }
    }

    /** Farben zur Auswahl fuer die Pop Chams. */
    public static final int[] POP_CHAMS_COLORS = {
        0xB794F6, 0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xFF55FF
    };
}
