package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Nametags anpassen: Leben und Ping hinter dem Namen anderer Spieler, Groesse, Hintergrund und Text-Schatten.
 * Der fertige Text wird pro Spieler gemerkt und nur neu gebaut, wenn sich Name, Leben oder Ping aendern.
 */
public final class Nametags {
    private static final class Cached {
        Text base;
        int health;
        int ping;
        Text result;
    }

    private static final Int2ObjectOpenHashMap<Cached> CACHE = new Int2ObjectOpenHashMap<>();

    private Nametags() {
    }

    public static boolean active() {
        return ModSetting.Toggle.NAMETAGS.isActive();
    }

    /** Name mit Leben/Ping fuer das Namensschild eines Spielers. */
    public static Text decorate(Entity entity, Text name) {
        if (!active()) {
            return name;
        }
        ArneStylesConfig c = ConfigManager.get();
        int health = -1;
        float max = 20.0f;
        if (c.nametagHealth && entity instanceof LivingEntity living) {
            health = (int) Math.ceil(living.getHealth() + living.getAbsorptionAmount());
            max = Math.max(1.0f, living.getMaxHealth());
        }
        int ping = -1;
        if (c.nametagPing) {
            ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            PlayerListEntry entry = handler == null ? null : handler.getPlayerListEntry(entity.getUuid());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }
        if (health < 0 && ping < 0) {
            return name;
        }
        Cached cached = CACHE.get(entity.getId());
        if (cached != null && cached.health == health && cached.ping == ping && cached.base.equals(name)) {
            return cached.result;
        }
        if (cached == null) {
            if (CACHE.size() > 256) {
                CACHE.clear();
            }
            cached = new Cached();
            CACHE.put(entity.getId(), cached);
        }
        MutableText text = name.copy();
        if (health >= 0) {
            float fraction = health / max;
            int color = fraction > 1.0f ? 0xFFAA00 : fraction > 0.6f ? 0x55FF55 : fraction > 0.3f ? 0xFFFF55 : 0xFF5555;
            text.append(Text.literal(" " + health + "❤").withColor(color));
        }
        if (ping >= 0) {
            int color = ping <= c.tabPingGood ? 0x55FF55 : ping >= c.tabPingBad ? 0xFF5555 : 0xFFFF55;
            text.append(Text.literal(" " + ping + "ms").withColor(color));
        }
        cached.base = name;
        cached.health = health;
        cached.ping = ping;
        cached.result = text;
        return text;
    }

    /** Faktor fuer die Groesse der Namensschilder (1 = normal). */
    public static float scale() {
        return active() ? ConfigManager.get().nametagScale : 1.0f;
    }

    /** Deckkraft des Hintergrunds, null-Ersatz: der Vanilla-Wert bleibt, wenn die Anpassung aus ist. */
    public static float backgroundOpacity(float vanilla) {
        return active() ? ConfigManager.get().nametagBgOpacity : vanilla;
    }

    public static boolean shadow() {
        return active() && ConfigManager.get().nametagShadow;
    }
}
