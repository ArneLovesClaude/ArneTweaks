package com.arnestyles.client.mods;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mixin.OverlayTextureAccessor;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * PvP-Optik (nur bei dir sichtbar): eigene Treffer-Farbe, Kill-Effekte, Kristall-Groesse/-Tempo/-Huepfen,
 * Feuer auf Spielern tiefer und eigener Totem-Pop-Sound.
 */
public final class PvpVisuals {
    /** Vanilla: halbtransparentes Rot. */
    private static final int VANILLA_HIT = 0xB2FF0000;
    private static final long KILL_WINDOW_MS = 5000L;
    /** Pop-Sounds zur Auswahl (0 = Minecraft bzw. Sound-Pack). */
    public static final SoundEvent[] POP_SOUNDS = {
        SoundEvents.ITEM_TOTEM_USE, SoundEvents.BLOCK_BELL_USE, SoundEvents.BLOCK_ANVIL_LAND, SoundEvents.ENTITY_PLAYER_LEVELUP,
        SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
        SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
        SoundEvents.BLOCK_GLASS_BREAK, SoundEvents.ENTITY_ITEM_BREAK.value(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
        SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundEvents.ENTITY_WITHER_SPAWN,
        SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundEvents.ENTITY_GHAST_SCREAM,
        SoundEvents.ENTITY_VILLAGER_NO, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value(),
        SoundEvents.ENTITY_CHICKEN_EGG,
        // Eigene Datei: config/arnestyles/sounds/totem_pop.ogg
        net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("arnestyles", "custom_totem_pop"))
    };
    public static final String[] POP_SOUND_KEYS = {"normal", "bell", "anvil", "levelup", "pling", "xp", "chime", "firework", "twinkle",
        "explode", "glass", "item_break", "crit", "arrow", "beacon", "challenge", "teleport", "anchor", "wither", "dragon", "guardian",
        "ghast", "villager", "bit", "xylophone", "egg", "custom"};

    /** Gewaehlter Pop-Sound (Index in POP_SOUNDS). */
    public static int popSoundIndex() {
        return Math.max(0, Math.min(POP_SOUNDS.length - 1, Math.round(ConfigManager.get().totemPopSound)));
    }

    /** Waehlt den Pop-Sound und spielt ihn einmal zur Probe ab. */
    public static void selectPopSound(int index) {
        int n = POP_SOUNDS.length;
        int pick = ((index % n) + n) % n;
        ConfigManager.get().totemPopSound = pick;
        ConfigManager.save();
        previewPopSound();
    }

    public static void previewPopSound() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(POP_SOUNDS[popSoundIndex()], 1.0f,
            Math.max(0.05f, popVolume())));
    }

    private static @Nullable OverlayTexture overlay;
    private static int appliedHit;
    private static @Nullable LivingEntity lastTarget;
    private static long lastHitAt;

    private PvpVisuals() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity living && player == MinecraftClient.getInstance().player) {
                lastTarget = living;
                lastHitAt = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(PvpVisuals::tick);
    }

    private static void tick(MinecraftClient client) {
        updateHitColor();
        LivingEntity target = lastTarget;
        if (target == null) {
            return;
        }
        if (client.world == null || System.currentTimeMillis() - lastHitAt > KILL_WINDOW_MS) {
            lastTarget = null;
            return;
        }
        boolean dead = target.isDead() || target.getHealth() <= 0.0f || target.getRemovalReason() == Entity.RemovalReason.KILLED;
        if (dead) {
            lastTarget = null;
            if (target instanceof PlayerEntity) {
                Comfort.onPlayerKill();
            }
            if (ModSetting.Toggle.KILL_EFFECTS.isActive() && (!ModSetting.Toggle.KILL_ONLY_PLAYERS.value() || target instanceof PlayerEntity)) {
                killEffect(client, client.world, target.getX(), target.getY(), target.getZ());
            }
        }
    }

    // ---- Treffer-Farbe ----

    public static void onOverlayCreated(OverlayTexture texture) {
        overlay = texture;
        appliedHit = VANILLA_HIT;
    }

    /** Faerbt die Treffer-Flaeche der Overlay-Textur, sobald sich die gewuenschte Farbe aendert. */
    private static void updateHitColor() {
        OverlayTexture texture = overlay;
        if (texture == null) {
            return;
        }
        int wanted = ModSetting.Toggle.HIT_COLOR.isActive() ? 0xB2000000 | (ConfigManager.get().hitColorRgb & 0xFFFFFF) : VANILLA_HIT;
        if (wanted == appliedHit) {
            return;
        }
        NativeImageBackedTexture backed = ((OverlayTextureAccessor) texture).arnestyles$texture();
        NativeImage image = backed.getImage();
        if (image == null) {
            return;
        }
        // Zeilen 0 bis 7 sind die Treffer-Farbe, darunter das weisse Aufblitzen (Creeper usw.)
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                image.setColorArgb(x, y, wanted);
            }
        }
        backed.upload();
        appliedHit = wanted;
    }

    // ---- Kill-Effekte ----

    /** Kill-Effekte an der Stelle des Gegners. Oeffentlich fuer den Selbsttest. */
    public static void killEffect(MinecraftClient client, ClientWorld world, double x, double y, double z) {
        Random random = world.getRandom();
        if (ModSetting.Toggle.KILL_LIGHTNING.value()) {
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setPosition(x, y, z);
                bolt.setCosmetic(true);
                world.addEntity(bolt);
            }
        }
        if (ModSetting.Toggle.KILL_FIREWORK.value()) {
            int accent = 0x8B5CF6;
            FireworkExplosionComponent burst = new FireworkExplosionComponent(FireworkExplosionComponent.Type.LARGE_BALL,
                IntList.of(accent, 0xFFFFFF, 0xF2C14E), IntList.of(0xFF5555), true, true);
            world.addFireworkParticle(x, y + 1.5, z, 0.0, 0.0, 0.0, List.of(burst));
        }
        if (ModSetting.Toggle.KILL_TOTEM.value()) {
            for (int i = 0; i < 60; i++) {
                world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z,
                    random.nextGaussian() * 0.45, random.nextDouble() * 0.7, random.nextGaussian() * 0.45);
            }
        }
        if (ModSetting.Toggle.KILL_SOUND.value()) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
        }
    }

    // ---- Kristall ----

    public static float crystalSize() {
        return ConfigManager.get().modEnabled ? ConfigManager.get().crystalSize : 1.0f;
    }

    public static float crystalSpeed() {
        return ConfigManager.get().modEnabled ? ConfigManager.get().crystalRotationSpeed : 1.0f;
    }

    public static boolean crystalBounce() {
        return !ConfigManager.get().modEnabled || ConfigManager.get().crystalBounce;
    }

    // ---- Feuer auf Spielern ----

    /** Wie weit das Feuer auf Spielern nach unten rutscht (0 = normal, 1 = ganz im Boden). */
    public static float playerFireOffset() {
        return ConfigManager.get().modEnabled ? ConfigManager.get().playerFireOffset : 0.0f;
    }

    // ---- Totem-Pop-Sound ----

    public static SoundEvent popSound() {
        int index = Math.round(ConfigManager.get().totemPopSound);
        if (index == POP_SOUNDS.length - 1 && !TotemPop.hasCustomSound()) {
            return SoundEvents.ITEM_TOTEM_USE;
        }
        return ConfigManager.get().modEnabled && index > 0 && index < POP_SOUNDS.length ? POP_SOUNDS[index] : SoundEvents.ITEM_TOTEM_USE;
    }

    public static float popVolume() {
        return ConfigManager.get().modEnabled ? ConfigManager.get().totemPopVolume : 1.0f;
    }
}
