package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.Comfort;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Zoom: Sichtfeld durch die Zoom-Stufe teilen. */
@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    private float arnestyles$zoom(float fov) {
        return Comfort.applyZoom(fov);
    }
}
