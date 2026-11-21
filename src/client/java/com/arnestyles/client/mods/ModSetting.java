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
        HIT_CRITS("hit_crits", null, c -> c.hitCrits, (c, v) -> c.hitCrits = v),
        HIT_SHARPNESS("hit_sharpness", null, c -> c.hitSharpness, (c, v) -> c.hitSharpness = v),
        WAYPOINTS("waypoints", null, c -> c.waypointsHud, (c, v) -> c.waypointsHud = v),
        SPOTIFY_HUD("spotify_hud", null, c -> c.spotifyHud, (c, v) -> c.spotifyHud = v),
        CUSTOMHUD_ON("customhud_on", null, c -> CustomHudBridge.enabled(), (c, v) -> CustomHudBridge.setEnabled(v)),
        SPOTIFY_LYRICS("spotify_lyrics", null, c -> c.spotifyLyrics, (c, v) -> c.spotifyLyrics = v),
        SPOTIFY_HIDE_PAUSED("spotify_hide_paused", null, c -> c.spotifyHidePaused, (c, v) -> c.spotifyHidePaused = v),
        SPOTIFY_COVER("spotify_cover", null, c -> c.spotifyCover, (c, v) -> c.spotifyCover = v),
        SPOTIFY_ANY_PLAYER("spotify_any_player", null, c -> c.spotifyAnyPlayer, (c, v) -> c.spotifyAnyPlayer = v),
        CROSSHAIR_TARGET("crosshair_target", null, c -> c.crosshairTarget, (c, v) -> c.crosshairTarget = v),
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
        HIDE_OWN_EFFECT_PARTICLES("hide_own_effect_particles", null, c -> c.hideOwnEffectParticles, (c, v) -> c.hideOwnEffectParticles = v),
        CHAT_TIMESTAMPS("chat_timestamps", null, c -> c.chatTimestamps, (c, v) -> c.chatTimestamps = v),
        CHAT_STACK("chat_stack", null, c -> c.chatStack, (c, v) -> c.chatStack = v),
        DEATH_POINT("death_point", null, c -> c.deathPoint, (c, v) -> c.deathPoint = v),
        // Minecraft-eigene Optionen, hier nur schneller erreichbar
        TOGGLE_SPRINT("toggle_sprint", null, c -> net.minecraft.client.MinecraftClient.getInstance().options.getSprintToggled().getValue(),
            (c, v) -> {
                var options = net.minecraft.client.MinecraftClient.getInstance().options;
                options.getSprintToggled().setValue(v);
                options.write();
            }),
        TOGGLE_SNEAK("toggle_sneak", null, c -> net.minecraft.client.MinecraftClient.getInstance().options.getSneakToggled().getValue(),
            (c, v) -> {
                var options = net.minecraft.client.MinecraftClient.getInstance().options;
                options.getSneakToggled().setValue(v);
                options.write();
            }),
        HITBOXES("hitboxes", COMBAT_HITBOXES_MOD, c -> c.hitboxes, (c, v) -> c.hitboxes = v),
        HITBOX_TARGET_COLOR("hitbox_target_color", COMBAT_HITBOXES_MOD, c -> c.hitboxTargetColor, (c, v) -> c.hitboxTargetColor = v),
        HITBOX_HURT_COLOR("hitbox_hurt_color", COMBAT_HITBOXES_MOD, c -> c.hitboxHurtColor, (c, v) -> c.hitboxHurtColor = v),
        HITBOX_EYES("hitbox_eyes", COMBAT_HITBOXES_MOD, c -> c.hitboxEyes, (c, v) -> c.hitboxEyes = v),
        HITBOX_LOOK("hitbox_look", COMBAT_HITBOXES_MOD, c -> c.hitboxLook, (c, v) -> c.hitboxLook = v),
        HITBOX_LOOK_ARROW("hitbox_look_arrow", COMBAT_HITBOXES_MOD, c -> c.hitboxLookArrow, (c, v) -> c.hitboxLookArrow = v),
        HITBOX_OUTLINE("hitbox_outline", COMBAT_HITBOXES_MOD, c -> c.hitboxOutline, (c, v) -> c.hitboxOutline = v),
        HITBOX_HIDE_FIREWORKS("hitbox_hide_fireworks", COMBAT_HITBOXES_MOD, c -> c.hitboxHideFireworks, (c, v) -> c.hitboxHideFireworks = v),
        HITBOX_HIDE_ITEMS("hitbox_hide_items", COMBAT_HITBOXES_MOD, c -> c.hitboxHideItems, (c, v) -> c.hitboxHideItems = v),
        HIDE_STUCK_ARROWS("hide_stuck_arrows", COMBAT_HITBOXES_MOD, c -> c.hideStuckArrows, (c, v) -> c.hideStuckArrows = v),
        SPOTIFY_SHADOW("spotify_shadow", null, c -> c.spotifyShadow, (c, v) -> c.spotifyShadow = v),
        ITEM_COUNTER("item_counter", null, c -> c.itemCounter, (c, v) -> c.itemCounter = v),
        ITEM_COUNTER_VERTICAL("item_counter_vertical", null, c -> c.itemCounterVertical, (c, v) -> c.itemCounterVertical = v),
        ITEM_COUNTER_HIDE_ZERO("item_counter_hide_zero", null, c -> c.itemCounterHideZero, (c, v) -> c.itemCounterHideZero = v),
        COUNT_TOTEM("count_totem", null, c -> c.countTotem, (c, v) -> c.countTotem = v),
        COUNT_PEARL("count_pearl", null, c -> c.countPearl, (c, v) -> c.countPearl = v),
        COUNT_CRYSTAL("count_crystal", null, c -> c.countCrystal, (c, v) -> c.countCrystal = v),
        COUNT_OBSIDIAN("count_obsidian", null, c -> c.countObsidian, (c, v) -> c.countObsidian = v),
        COUNT_ANCHOR("count_anchor", null, c -> c.countAnchor, (c, v) -> c.countAnchor = v),
        COUNT_GLOWSTONE("count_glowstone", null, c -> c.countGlowstone, (c, v) -> c.countGlowstone = v),
        COUNT_GAPPLE("count_gapple", null, c -> c.countGapple, (c, v) -> c.countGapple = v),
        COUNT_XP("count_xp", null, c -> c.countXp, (c, v) -> c.countXp = v),
        COUNT_ARROW("count_arrow", null, c -> c.countArrow, (c, v) -> c.countArrow = v),
        COUNT_COBWEB("count_cobweb", null, c -> c.countCobweb, (c, v) -> c.countCobweb = v),
        COUNT_WIND_CHARGE("count_wind_charge", null, c -> c.countWindCharge, (c, v) -> c.countWindCharge = v),
        COUNT_SPLASH("count_splash", null, c -> c.countSplash, (c, v) -> c.countSplash = v),
        TAB_PING("tab_ping", null, c -> c.tabPing, (c, v) -> c.tabPing = v),
        TAB_PING_COLORS("tab_ping_colors", null, c -> c.tabPingColors, (c, v) -> c.tabPingColors = v),
        TAB_PING_SUFFIX("tab_ping_suffix", null, c -> c.tabPingSuffix, (c, v) -> c.tabPingSuffix = v),
        CUSTOM_CROSSHAIR("custom_crosshair", null, c -> c.customCrosshair, (c, v) -> c.customCrosshair = v),
        CROSSHAIR_OUTLINE("crosshair_outline", null, c -> c.crosshairOutline, (c, v) -> c.crosshairOutline = v),
        ANIM_NO_COOLDOWN("anim_no_cooldown", null, c -> c.animNoCooldown, (c, v) -> c.animNoCooldown = v),
        ANIM_EAT_BOB("anim_eat_bob", null, c -> c.animEatBob, (c, v) -> c.animEatBob = v),
        HUD_TEXT_SHADOW("hud_text_shadow", null, c -> c.hudTextShadow, (c, v) -> c.hudTextShadow = v),
        HUD_BORDER("hud_border", null, c -> c.hudBorder, (c, v) -> c.hudBorder = v),
        KEYSTROKES_MOUSE("keystrokes_mouse", null, c -> c.keystrokesMouse, (c, v) -> c.keystrokesMouse = v),
        KEYSTROKES_SPACE("keystrokes_space", null, c -> c.keystrokesSpace, (c, v) -> c.keystrokesSpace = v),
        COORDS_DIRECTION("coords_direction", null, c -> c.coordsDirection, (c, v) -> c.coordsDirection = v),
        CHAT_SECONDS("chat_seconds", null, c -> c.chatSeconds, (c, v) -> c.chatSeconds = v),
        CLOCK_HUD("clock_hud", null, c -> c.clockHud, (c, v) -> c.clockHud = v),
        CLOCK_SECONDS("clock_seconds", null, c -> c.clockSeconds, (c, v) -> c.clockSeconds = v),
        CLOCK_12H("clock_12h", null, c -> c.clock12h, (c, v) -> c.clock12h = v),
        CLOCK_DATE("clock_date", null, c -> c.clockDate, (c, v) -> c.clockDate = v),
        CLOCK_YEAR("clock_year", null, c -> c.clockYear, (c, v) -> c.clockYear = v),
        ELYTRA_HUD("elytra_hud", null, c -> c.elytraHud, (c, v) -> c.elytraHud = v),
        ELYTRA_ONLY_FLYING("elytra_only_flying", null, c -> c.elytraOnlyFlying, (c, v) -> c.elytraOnlyFlying = v),
        ELYTRA_KMH("elytra_kmh", null, c -> c.elytraKmh, (c, v) -> c.elytraKmh = v),
        SERVER_HUD("server_hud", null, c -> c.serverHud, (c, v) -> c.serverHud = v),
        DAY_HUD("day_hud", null, c -> c.dayHud, (c, v) -> c.dayHud = v),
        DAY_TIME("day_time", null, c -> c.dayTime, (c, v) -> c.dayTime = v),
        SCOREBOARD_HIDE("scoreboard_hide", null, c -> c.scoreboardHide, (c, v) -> c.scoreboardHide = v),
        SCOREBOARD_HIDE_NUMBERS("scoreboard_hide_numbers", null, c -> c.scoreboardHideNumbers, (c, v) -> c.scoreboardHideNumbers = v),
        SCOREBOARD_SHADOW("scoreboard_shadow", null, c -> c.scoreboardShadow, (c, v) -> c.scoreboardShadow = v),
        BOSSBAR_HIDE("bossbar_hide", null, c -> c.bossbarHide, (c, v) -> c.bossbarHide = v),
        HELD_ITEM_NAME_HIDE("held_item_name_hide", null, c -> c.heldItemNameHide, (c, v) -> c.heldItemNameHide = v),
        EFFECT_ICONS_HIDE("effect_icons_hide", null, c -> c.effectIconsHide, (c, v) -> c.effectIconsHide = v),
        HOTBAR_SMOOTH("hotbar_smooth", null, c -> c.hotbarSmooth, (c, v) -> c.hotbarSmooth = v),
        COOLDOWN_INDICATOR("cooldown_indicator", null, c -> c.cooldownIndicator, (c, v) -> c.cooldownIndicator = v),
        NAMETAGS("nametags", null, c -> c.nametags, (c, v) -> c.nametags = v),
        NAMETAG_HEALTH("nametag_health", null, c -> c.nametagHealth, (c, v) -> c.nametagHealth = v),
        NAMETAG_PING("nametag_ping", null, c -> c.nametagPing, (c, v) -> c.nametagPing = v),
        NAMETAG_SHADOW("nametag_shadow", null, c -> c.nametagShadow, (c, v) -> c.nametagShadow = v);

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

        /** Zurueck auf den Standardwert (aus einer frischen Config). */
        public void reset() {
            setter.accept(ConfigManager.get(), getter.test(new ArneStylesConfig()));
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
        SPOTIFY_MODE("spotify_mode", 0.0f, 4.0f, 4.0f, 1.0f, c -> c.spotifyMode, (c, v) -> c.spotifyMode = v),
        SPOTIFY_WIDTH("spotify_width", 150.0f, 320.0f, 210.0f, 10.0f, c -> c.spotifyWidth, (c, v) -> c.spotifyWidth = v),
        SPOTIFY_OFFSET("spotify_offset", -5.0f, 5.0f, 0.0f, 0.25f, c -> c.spotifyOffset, (c, v) -> c.spotifyOffset = v),
        TOTEM_POP_STYLE("totem_pop_style", 0.0f, 3.0f, 0.0f, 1.0f, c -> c.totemPopStyle, (c, v) -> c.totemPopStyle = v),
        TOTEM_POP_SPEED("totem_pop_speed", 0.5f, 2.0f, 1.0f, 0.25f, c -> c.totemPopSpeed, (c, v) -> c.totemPopSpeed = v),
        TOTEM_POP_PITCH("totem_pop_pitch", 0.5f, 2.0f, 1.0f, 0.05f, c -> c.totemPopPitch, (c, v) -> c.totemPopPitch = v),
        TOTEM_PARTICLE_COLOR("totem_particle_color", 0.0f, 10.0f, 0.0f, 1.0f, c -> c.totemParticleColor, (c, v) -> c.totemParticleColor = v),
        TOTEM_POP_SOUND("totem_pop_sound", 0.0f, 26.0f, 0.0f, 1.0f, c -> c.totemPopSound, (c, v) -> c.totemPopSound = v),
        TOTEM_POP_VOLUME("totem_pop_volume", 0.0f, 2.0f, 1.0f, 0.05f, c -> c.totemPopVolume, (c, v) -> c.totemPopVolume = v),
        HIT_PARTICLES("hit_particles", 1.0f, 5.0f, 1.0f, 1.0f, c -> c.hitParticleAmount, (c, v) -> c.hitParticleAmount = v),
        OUTLINE_WIDTH("outline_width", 1.0f, 6.0f, 2.0f, 0.5f, c -> c.outlineWidth, (c, v) -> c.outlineWidth = v),
        CUSTOM_TIME_VALUE("custom_time_value", 0.0f, 23750.0f, 6000.0f, 250.0f, c -> c.customTime, (c, v) -> c.customTime = v),
        ZOOM_LEVEL("zoom_level", 2.0f, 10.0f, 4.0f, 0.5f, c -> c.zoomLevel, (c, v) -> c.zoomLevel = v),
        HITBOX_WIDTH("hitbox_width", 0.5f, 6.0f, 2.5f, 0.5f, c -> c.hitboxWidth, (c, v) -> c.hitboxWidth = v),
        HITBOX_FAR_WIDTH("hitbox_far_width", 0.5f, 6.0f, 2.5f, 0.5f, c -> c.hitboxFarWidth, (c, v) -> c.hitboxFarWidth = v),
        HITBOX_FAR_DISTANCE("hitbox_far_distance", 4.0f, 64.0f, 32.0f, 1.0f, c -> c.hitboxFarDistance, (c, v) -> c.hitboxFarDistance = v),
        HITBOX_OUTLINE_SIZE("hitbox_outline_size", 1.0f, 4.0f, 2.0f, 0.25f, c -> c.hitboxOutlineSize, (c, v) -> c.hitboxOutlineSize = v),
        SPOTIFY_BG_OPACITY("spotify_bg_opacity", 0.0f, 1.0f, 0.85f, 0.05f, c -> c.spotifyBgOpacity, (c, v) -> c.spotifyBgOpacity = v),
        ITEM_COUNTER_WARN("item_counter_warn", 0.0f, 32.0f, 2.0f, 1.0f, c -> c.itemCounterWarn, (c, v) -> c.itemCounterWarn = v),
        TAB_PING_GOOD("tab_ping_good", 20.0f, 300.0f, 80.0f, 10.0f, c -> c.tabPingGood, (c, v) -> c.tabPingGood = v),
        TAB_PING_BAD("tab_ping_bad", 50.0f, 1000.0f, 200.0f, 10.0f, c -> c.tabPingBad, (c, v) -> c.tabPingBad = v),
        CROSSHAIR_OPACITY("crosshair_opacity", 0.1f, 1.0f, 1.0f, 0.05f, c -> c.crosshairOpacity, (c, v) -> c.crosshairOpacity = v),
        CROSSHAIR_SCALE("crosshair_scale", 0.5f, 3.0f, 1.0f, 0.25f, c -> c.crosshairScale, (c, v) -> c.crosshairScale = v),
        ANIM_EQUIP("anim_equip", 0.0f, 1.0f, 1.0f, 0.05f, c -> c.animEquip, (c, v) -> c.animEquip = v),
        ANIM_SWING_SPEED("anim_swing_speed", 0.5f, 2.0f, 1.0f, 0.05f, c -> c.animSwingSpeed, (c, v) -> c.animSwingSpeed = v),
        ANIM_SWING_MOVE("anim_swing_move", 0.0f, 2.0f, 1.0f, 0.05f, c -> c.animSwingMove, (c, v) -> c.animSwingMove = v),
        ANIM_SWING_ROTATION("anim_swing_rotation", 0.0f, 2.0f, 1.0f, 0.05f, c -> c.animSwingRotation, (c, v) -> c.animSwingRotation = v),
        ANIM_EAT("anim_eat", 0.0f, 2.0f, 1.0f, 0.05f, c -> c.animEat, (c, v) -> c.animEat = v),
        HAND_X("hand_x", -0.5f, 0.5f, 0.0f, 0.02f, c -> c.handX, (c, v) -> c.handX = v),
        HAND_Y("hand_y", -0.5f, 0.5f, 0.0f, 0.02f, c -> c.handY, (c, v) -> c.handY = v),
        HAND_Z("hand_z", -0.5f, 0.5f, 0.0f, 0.02f, c -> c.handZ, (c, v) -> c.handZ = v),
        HUD_BG_OPACITY("hud_bg_opacity", 0.0f, 1.0f, 0.56f, 0.02f, c -> c.hudBgOpacity, (c, v) -> c.hudBgOpacity = v),
        ARMOR_WARNING_PERCENT("armor_warning_percent", 0.05f, 0.5f, 0.1f, 0.05f, c -> c.armorWarningPercent, (c, v) -> c.armorWarningPercent = v),
        COMBO_TIMEOUT("combo_timeout", 1.0f, 10.0f, 3.0f, 0.5f, c -> c.comboTimeout, (c, v) -> c.comboTimeout = v),
        WAYPOINT_COUNT("waypoint_count", 1.0f, 10.0f, 3.0f, 1.0f, c -> c.waypointCount, (c, v) -> c.waypointCount = v),
        DEATH_POINT_MINUTES("death_point_minutes", 1.0f, 60.0f, 15.0f, 1.0f, c -> c.deathPointMinutes, (c, v) -> c.deathPointMinutes = v),
        SCOREBOARD_BG_OPACITY("scoreboard_bg_opacity", 0.0f, 1.0f, 0.3f, 0.05f, c -> c.scoreboardBgOpacity, (c, v) -> c.scoreboardBgOpacity = v),
        HOTBAR_OPACITY("hotbar_opacity", 0.2f, 1.0f, 1.0f, 0.05f, c -> c.hotbarOpacity, (c, v) -> c.hotbarOpacity = v),
        COOLDOWN_STYLE("cooldown_style", 0.0f, 3.0f, 0.0f, 1.0f, c -> c.cooldownStyle, (c, v) -> c.cooldownStyle = v),
        NAMETAG_SCALE("nametag_scale", 0.5f, 2.0f, 1.0f, 0.05f, c -> c.nametagScale, (c, v) -> c.nametagScale = v),
        NAMETAG_BG_OPACITY("nametag_bg_opacity", 0.0f, 1.0f, 0.25f, 0.05f, c -> c.nametagBgOpacity, (c, v) -> c.nametagBgOpacity = v);

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

        /** Alle Regler auf ihren Bereich begrenzen (beim Laden der Config), NaN wird zum Standardwert. */
        public static void clampAll(ArneStylesConfig config) {
            for (Slider slider : values()) {
                Float v = slider.getter.apply(config);
                if (v == null || v.isNaN()) {
                    slider.setter.accept(config, slider.standard);
                } else if (v < slider.min || v > slider.max) {
                    slider.setter.accept(config, Math.max(slider.min, Math.min(slider.max, v)));
                }
            }
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
                case HIT_PARTICLES -> Math.round(value()) + "x";
                case SPOTIFY_MODE -> SpotifyHud.modeName(Math.round(value()));
                case SPOTIFY_WIDTH -> Math.round(value()) + " px";
                case SPOTIFY_OFFSET -> String.format(Locale.ROOT, "%+.2f s", value());
                case TOTEM_POP_STYLE -> net.minecraft.text.Text.translatable("arnestyles.totem.style." + TotemPop.STYLES[
                    Math.max(0, Math.min(TotemPop.STYLES.length - 1, Math.round(value())))]).getString();
                case TOTEM_POP_SPEED, TOTEM_POP_PITCH -> String.format(Locale.ROOT, "%.2fx", value());
                case TOTEM_PARTICLE_COLOR -> TotemPop.particleColorName(Math.round(value()));
                case TOTEM_POP_SOUND -> net.minecraft.text.Text.translatable("arnestyles.mod.totem_pop_sound." + PvpVisuals.POP_SOUND_KEYS[
                    Math.max(0, Math.min(PvpVisuals.POP_SOUND_KEYS.length - 1, Math.round(value())))]).getString();
                case ITEM_COUNTER_WARN -> Math.round(value()) == 0 ? net.minecraft.text.Text.translatable("arnestyles.gui.off").getString() : "\u2264 " + Math.round(value());
                case TAB_PING_GOOD -> Math.round(value()) + " ms";
                case TAB_PING_BAD -> Math.round(value()) + " ms";
                case CROSSHAIR_SCALE -> String.format(Locale.ROOT, "%.2fx", value());
                case ANIM_SWING_SPEED -> String.format(Locale.ROOT, "%.2fx", value());
                case HAND_X -> String.format(Locale.ROOT, "%+.2f", value());
                case HAND_Y -> String.format(Locale.ROOT, "%+.2f", value());
                case HAND_Z -> String.format(Locale.ROOT, "%+.2f", value());
                case COMBO_TIMEOUT -> String.format(Locale.ROOT, "%.1f s", value());
                case WAYPOINT_COUNT -> String.valueOf(Math.round(value()));
                case DEATH_POINT_MINUTES -> Math.round(value()) + " min";
                case COOLDOWN_STYLE -> net.minecraft.text.Text.translatable("arnestyles.mod.cooldown_style." + Math.round(value())).getString();
                case NAMETAG_SCALE -> String.format(Locale.ROOT, "%.2fx", value());
                default -> String.format(Locale.ROOT, "%d%%", Math.round(value() * 100.0f));
            };
        }
    }

    /** Farben zur Auswahl fuer die Pop Chams. */
    public static final int[] POP_CHAMS_COLORS = {
        0xB794F6, 0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xFF55FF
    };
}
