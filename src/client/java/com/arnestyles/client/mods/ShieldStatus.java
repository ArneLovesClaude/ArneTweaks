package com.arnestyles.client.mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;

/**
 * Shield Status: der Schild wird gruen eingefaerbt, wenn er blocken kann, und rot, wenn er gerade
 * durch eine Axt ausgeschaltet ist. Optional geht Rot langsam in Gruen ueber.
 *
 * Eigener Schild: echte Abklingzeit vom Server. Andere Spieler: der Server schickt ihre Abklingzeit nicht,
 * deshalb wird das Schild-Brech-Geraeusch an ihrer Position erkannt und 5 Sekunden (Axt) angenommen.
 *
 * Nach "ShieldStatus" von Walksy (MIT-Lizenz, github.com/Walksy/ShieldStatus),
 * fuer ArneTweaks und Yarn-Mappings neu geschrieben, ohne WalksyLib.
 */
public final class ShieldStatus {
    private static final int READY = 0xFF77FF77;
    private static final int DISABLED = 0xFFFF5555;
    /** Abklingzeit durch eine Axt in Ticks. */
    private static final int AXE_DISABLE_TICKS = 100;
    /** Wie nah das Geraeusch am Spieler sein muss (Bloecke). */
    private static final double SOUND_MATCH_DISTANCE = 1.5;

    /** Bis zu welchem Tick der Schild eines anderen Spielers aus ist (Entity-ID). */
    private static final Map<Integer, Long> DISABLED_UNTIL = new HashMap<>();
    private static long tick;

    /** Der Spieler, dessen Hand-Item gerade gezeichnet wird (-1 = keiner). */
    private static int currentHolder = -1;

    private ShieldStatus() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            if (client.world == null) {
                DISABLED_UNTIL.clear();
            } else if (!DISABLED_UNTIL.isEmpty()) {
                DISABLED_UNTIL.values().removeIf(until -> until < tick);
            }
        });
    }

    public static void setHolder(int entityId) {
        currentHolder = entityId;
    }

    public static void clearHolder() {
        currentHolder = -1;
    }

    /** Schild-Brech-Geraeusch vom Server: dem Spieler mit Schild an dieser Stelle zuordnen. */
    public static void onSound(PlaySoundS2CPacket packet) {
        if (!packet.getSound().value().id().equals(SoundEvents.ITEM_SHIELD_BREAK.value().id())) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        AbstractClientPlayerEntity best = null;
        double bestDistance = SOUND_MATCH_DISTANCE * SOUND_MATCH_DISTANCE;
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !holdsShield(player)) {
                continue;
            }
            double distance = player.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        if (best != null) {
            DISABLED_UNTIL.put(best.getId(), tick + AXE_DISABLE_TICKS);
        }
    }

    private static boolean holdsShield(PlayerEntity player) {
        return isShield(player.getStackInHand(Hand.MAIN_HAND)) || isShield(player.getStackInHand(Hand.OFF_HAND));
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.contains(DataComponentTypes.BLOCKS_ATTACKS);
    }

    /**
     * Farbe fuer den gerade gezeichneten Schild, oder -1 (weiss, unveraendert).
     * Im Inventar und auf dem Boden bleibt der Schild normal.
     */
    public static int tint(ItemDisplayContext context) {
        if (!ModSetting.Toggle.SHIELD_STATUS.isActive()) {
            return -1;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return -1;
        }
        PlayerEntity player;
        if (context.isFirstPerson()) {
            player = client.player;
        } else if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            Entity entity = currentHolder >= 0 ? client.world.getEntityById(currentHolder) : null;
            if (!(entity instanceof PlayerEntity holder)) {
                return -1;
            }
            player = holder;
        } else {
            return -1;
        }

        float disabled; // 1 = gerade ausgeschaltet, 0 = bereit
        if (player == client.player) {
            disabled = ownCooldown(client, client.getRenderTickCounter().getTickProgress(false));
        } else {
            if (!ModSetting.Toggle.SHIELD_STATUS_OTHERS.isActive()) {
                return -1;
            }
            Long until = DISABLED_UNTIL.get(player.getId());
            disabled = until == null ? 0.0f : Math.max(0.0f, Math.min(1.0f, (until - tick) / (float) AXE_DISABLE_TICKS));
        }
        if (disabled <= 0.0f) {
            return READY;
        }
        if (!ModSetting.Toggle.SHIELD_STATUS_FADE.isActive()) {
            return DISABLED;
        }
        return lerp(READY, DISABLED, disabled);
    }

    private static float ownCooldown(MinecraftClient client, float tickProgress) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            if (isShield(stack) && client.player.getItemCooldownManager().isCoolingDown(stack)) {
                return Math.max(0.001f, client.player.getItemCooldownManager().getCooldownProgress(stack, tickProgress));
            }
        }
        return 0.0f;
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
