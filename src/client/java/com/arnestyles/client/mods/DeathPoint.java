package com.arnestyles.client.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Todespunkt: merkt sich, wo man gestorben ist, schreibt die Position in den Chat und zeigt danach
 * Entfernung und Richtung im HUD (nur in derselben Dimension, 15 Minuten lang oder bis man dort ist).
 * Reine Anzeige, kein Teleport.
 */
public final class DeathPoint {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static @Nullable Vec3d pos;
    private static @Nullable RegistryKey<World> dimension;
    private static long diedAt;
    private static boolean wasDead;

    private DeathPoint() {
    }

    static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            wasDead = false;
            return;
        }
        boolean dead = player.isDead() || player.getHealth() <= 0.0f;
        if (dead && !wasDead && ModSetting.Toggle.DEATH_POINT.isActive()) {
            pos = player.getEntityPos();
            dimension = client.world.getRegistryKey();
            diedAt = System.currentTimeMillis();
            client.inGameHud.getChatHud().addMessage(Text.literal("[ArneTweaks] ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.translatable("arnestyles.death.chat", MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z),
                    dimension.getValue().getPath()).formatted(Formatting.GRAY)));
        }
        wasDead = dead;
        // Angekommen: Anzeige ausblenden
        if (pos != null && !dead && client.world.getRegistryKey() == dimension && player.getEntityPos().distanceTo(pos) < 3.0) {
            pos = null;
        }
    }

    /** Text fuer das HUD oder null, wenn nichts anzuzeigen ist. */
    static @Nullable String label(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (!ModSetting.Toggle.DEATH_POINT.isActive() || player == null || client.world == null) {
            return null;
        }
        if (pos == null || client.world.getRegistryKey() != dimension || System.currentTimeMillis() - diedAt > Math.round(com.arnestyles.client.config.ConfigManager.get().deathPointMinutes) * 60_000L || player.isDead()) {
            return null;
        }
        double dx = pos.x - player.getX();
        double dz = pos.z - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        // Richtung relativ zur Blickrichtung: 0 = geradeaus
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = MathHelper.wrapDegrees(target - player.getYaw());
        int index = Math.floorMod((int) Math.round(relative / 45.0), 8);
        return String.format(Locale.ROOT, "☠ %dm %s", Math.round(distance), ARROWS[index]);
    }
}
