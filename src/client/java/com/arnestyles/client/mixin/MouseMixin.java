package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.InventoryScale;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Inventory Scale: Mausposition passend zum skalierten Container-Fenster umrechnen. */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @ModifyReturnValue(method = "scaleX", at = @At("RETURN"), require = 0)
    private static double arnestyles$scaleX(double x) {
        float factor = InventoryScale.current();
        return factor == 1.0f ? x : x / factor;
    }

    @ModifyReturnValue(method = "scaleY", at = @At("RETURN"), require = 0)
    private static double arnestyles$scaleY(double y) {
        float factor = InventoryScale.current();
        return factor == 1.0f ? y : y / factor;
    }
}
