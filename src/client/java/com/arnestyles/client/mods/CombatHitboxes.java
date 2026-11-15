package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

/**
 * Combat Hitboxes: Hitboxen (F3+B) fuer PvP. Eigene Farben fuer Ziel und getroffene Gegner, Augenhoehe,
 * Blickrichtung, Linien-Dicke nah/fern, Umriss, und Feuerwerk/Items/steckende Pfeile ausblenden.
 * Uebertragen aus "Combat Hitboxes" von Sootysplash (Apache-2.0), siehe THIRD_PARTY_NOTICES.txt.
 */
public final class CombatHitboxes {
    /** Farben zur Auswahl (mit Schwarz fuer den Umriss). */
    public static final int[] COLORS = {
        0xFFFFFF, 0x000000, 0xFF5555, 0xFF55FF, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF
    };

    private CombatHitboxes() {
    }

    public static boolean active() {
        return ModSetting.Toggle.HITBOXES.isActive();
    }

    /** Statt der normalen Hitbox (true = Minecraft zeichnet nichts mehr). */
    public static boolean draw(Entity entity, float tickProgress, boolean inLocalServer) {
        if (inLocalServer || !active()) {
            return false;
        }
        ArneStylesConfig c = ConfigManager.get();
        if (c.hitboxHideFireworks && entity instanceof FireworkRocketEntity || c.hitboxHideItems && entity instanceof ItemEntity) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        float width = client.player != null && client.player.distanceTo(entity) > c.hitboxFarDistance ? c.hitboxFarWidth : c.hitboxWidth;

        Vec3d pos = entity.getEntityPos();
        Vec3d lerped = entity.getLerpedPos(tickProgress);
        Vec3d offset = lerped.subtract(pos);
        boolean hurt = c.hitboxHurtColor && entity instanceof LivingEntity living && living.hurtTime != 0;
        boolean target = c.hitboxTargetColor && client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == entity;
        int color = argb(hurt ? c.hitboxHurtRgb : target ? c.hitboxTargetRgb : c.hitboxRgb);
        Box box = entity.getBoundingBox().offset(offset);
        if (c.hitboxOutline) {
            GizmoDrawing.box(box, DrawStyle.stroked(argb(c.hitboxOutlineRgb), width * c.hitboxOutlineSize));
        }
        GizmoDrawing.box(box, DrawStyle.stroked(color, width));
        GizmoDrawing.point(lerped, color, 2.0F);

        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            float f = Math.min(vehicle.getWidth(), entity.getWidth()) / 2.0F;
            Vec3d seat = vehicle.getPassengerRidingPos(entity).add(offset);
            GizmoDrawing.box(new Box(seat.x - f, seat.y, seat.z - f, seat.x + f, seat.y + 0.0625, seat.z + f), DrawStyle.stroked(-256, width));
        }
        if (entity instanceof LivingEntity && c.hitboxEyes) {
            double eye = box.minY + entity.getStandingEyeHeight();
            GizmoDrawing.box(new Box(box.minX, eye - 0.01, box.minZ, box.maxX, eye + 0.01, box.maxZ), DrawStyle.stroked(argb(c.hitboxEyeRgb), width));
        }
        if (entity instanceof EnderDragonEntity dragon) {
            for (EnderDragonPart part : dragon.getBodyParts()) {
                Vec3d partOffset = part.getLerpedPos(tickProgress).subtract(part.getEntityPos());
                GizmoDrawing.box(part.getBoundingBox().offset(partOffset), DrawStyle.stroked(ColorHelper.fromFloats(1.0F, 0.25F, 1.0F, 0.0F)));
            }
        }
        if (c.hitboxLook) {
            Vec3d eyePos = lerped.add(0.0, entity.getStandingEyeHeight(), 0.0);
            Vec3d end = eyePos.add(entity.getRotationVec(tickProgress).multiply(2.0));
            if (c.hitboxLookArrow) {
                GizmoDrawing.arrow(eyePos, end, argb(c.hitboxLookRgb), width);
            } else {
                GizmoDrawing.line(eyePos, end, argb(c.hitboxLookRgb), width);
            }
        }
        return true;
    }

    /** Steckende Pfeile und Bienenstachel am Spieler ausblenden. */
    public static boolean hideStuck() {
        return ConfigManager.get().modEnabled && ConfigManager.get().hideStuckArrows
            && !ModSetting.isLoaded(ModSetting.COMBAT_HITBOXES_MOD);
    }

    private static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
