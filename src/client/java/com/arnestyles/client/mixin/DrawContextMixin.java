package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.InventoryScale;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Inventory Scale:
 * - Tooltips im skalierten Fenster bleiben innerhalb des Bildschirms.
 * - 3D-Figuren (Spieler im Inventar, Ruestungsstaender im Schmiedetisch) werden in einem eigenen Bild gezeichnet,
 *   das die Skalierung nicht mitbekommt. Position und Groesse werden deshalb hier mit dem Faktor umgerechnet.
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    private static final String ARNESTYLES$ADD_ENTITY =
        "addEntity(Lnet/minecraft/client/render/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V";

    @ModifyReturnValue(method = "getScaledWindowWidth", at = @At("RETURN"), require = 0)
    private int arnestyles$width(int width) {
        return InventoryScale.drawing ? (int) Math.ceil(width / InventoryScale.current()) : width;
    }

    @ModifyReturnValue(method = "getScaledWindowHeight", at = @At("RETURN"), require = 0)
    private int arnestyles$height(int height) {
        return InventoryScale.drawing ? (int) Math.ceil(height / InventoryScale.current()) : height;
    }

    private static int arnestyles$scaled(int value) {
        return InventoryScale.drawing ? Math.round(value * InventoryScale.current()) : value;
    }

    @ModifyVariable(method = ARNESTYLES$ADD_ENTITY, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float arnestyles$entityScale(float scale) {
        return InventoryScale.drawing ? scale * InventoryScale.current() : scale;
    }

    @ModifyVariable(method = ARNESTYLES$ADD_ENTITY, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int arnestyles$entityX1(int value) {
        return arnestyles$scaled(value);
    }

    @ModifyVariable(method = ARNESTYLES$ADD_ENTITY, at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int arnestyles$entityY1(int value) {
        return arnestyles$scaled(value);
    }

    @ModifyVariable(method = ARNESTYLES$ADD_ENTITY, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private int arnestyles$entityX2(int value) {
        return arnestyles$scaled(value);
    }

    @ModifyVariable(method = ARNESTYLES$ADD_ENTITY, at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private int arnestyles$entityY2(int value) {
        return arnestyles$scaled(value);
    }
}
