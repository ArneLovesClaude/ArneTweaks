package com.arnestyles.client.mods;

import com.arnestyles.client.mixin.LivingEntityAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shield Fixes: andere Spieler werden zuverlaessig blockend angezeigt, wenn sie den Schild oben haben
 * (Minecraft-Fehler MC-238293). Optional erst nach der 5-Tick-Verzoegerung des Schilds,
 * und optional mit sofortigem Schild-Geraeusch beim eigenen Treffer.
 *
 * Nach "ShieldFixes" von Walksy (MIT-Lizenz, github.com/Walksy/ShieldFixes),
 * fuer ArneTweaks und Yarn-Mappings neu geschrieben, ohne WalksyLib.
 */
public final class ShieldFix {
    /** Vanilla-Laufzeit eines Schilds (Ticks). */
    private static final int SHIELD_USE_TIME = 72000;
    private static final int SHIELD_DELAY_TICKS = 5;

    /** Seit welchem Tick ein Spieler den Schild oben hat. */
    private static final Map<UUID, Long> SHIELD_START = new HashMap<>();
    private static long tick;

    private ShieldFix() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ShieldFix::tick);
    }

    private static boolean holdsShieldUp(PlayerEntity player) {
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack stack = player.getStackInHand(player.getActiveHand());
        return !stack.isEmpty() && stack.getUseAction() == UseAction.BLOCK;
    }

    private static void tick(MinecraftClient client) {
        tick++;
        if (client.world == null) {
            SHIELD_START.clear();
            return;
        }
        boolean active = ModSetting.Toggle.SHIELD_FIX.isActive();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) {
                continue;
            }
            if (!holdsShieldUp(player)) {
                SHIELD_START.remove(player.getUuid());
                continue;
            }
            long start = SHIELD_START.computeIfAbsent(player.getUuid(), id -> tick);
            if (!active) {
                continue;
            }
            int usedTicks = (int) (tick - start);
            LivingEntityAccessor accessor = (LivingEntityAccessor) player;
            if (ModSetting.Toggle.SHIELD_FIX_DELAY.value() && usedTicks < SHIELD_DELAY_TICKS) {
                // Noch nicht wirklich am Blocken: nicht blockend zeigen
                accessor.arnestyles$setActiveItemStack(ItemStack.EMPTY);
                accessor.arnestyles$setItemUseTimeLeft(0);
                continue;
            }
            // Vanilla vergisst bei anderen Spielern manchmal den aktiven Gegenstand: hier immer richtig setzen.
            Hand hand = player.getActiveHand();
            accessor.arnestyles$setActiveItemStack(player.getStackInHand(hand));
            accessor.arnestyles$setItemUseTimeLeft(SHIELD_USE_TIME - usedTicks);
        }
        SHIELD_START.keySet().removeIf(id -> client.world.getPlayerByUuid(id) == null);
    }

    /** Eigener Treffer auf einen Spieler: sofort Schild-Geraeusch, wenn er zu uns blockt und unsere Waffe den Schild nicht ausschaltet. */
    public static void onAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModSetting.Toggle.SHIELD_SOUNDS.isActive() || client.player == null || client.world == null
            || !(target instanceof PlayerEntity player) || !holdsShieldUp(player)) {
            return;
        }
        WeaponComponent weapon = client.player.getMainHandStack().get(DataComponentTypes.WEAPON);
        if (weapon != null && weapon.disableBlockingForSeconds() > 0) {
            return;
        }
        // Nur wenn der Schild zu uns zeigt
        Vec3d view = player.getRotationVec(1.0f);
        Vec3d toPlayer = player.getEntityPos().subtract(client.player.getEntityPos()).normalize();
        if (new Vec3d(toPlayer.x, 0, toPlayer.z).dotProduct(view) >= 0.0) {
            return;
        }
        client.world.playSoundClient(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_SHIELD_BLOCK.value(),
            player.getSoundCategory(), 1.0f, 0.8f + client.world.random.nextFloat() * 0.4f, false);
    }
}
