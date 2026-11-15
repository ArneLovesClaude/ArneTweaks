package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.WorldVisuals;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Eigene Himmelsfarbe in der Oberwelt. */
@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void arnestyles$skyColor(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (WorldVisuals.customSky() && state.skybox == DimensionType.Skybox.OVERWORLD) {
            state.skyColor = WorldVisuals.skyColor();
        }
    }
}
