package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.CombatHitboxes;
import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckStingersFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Combat Hitboxes: steckende Pfeile und Bienenstachel ausblenden. */
@Mixin({StuckArrowsFeatureRenderer.class, StuckStingersFeatureRenderer.class})
public abstract class StuckObjectsMixin {
    @Inject(method = "getObjectCount", at = @At("RETURN"), cancellable = true, require = 0)
    private void arnestyles$hideStuck(PlayerEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (CombatHitboxes.hideStuck()) {
            cir.setReturnValue(0);
        }
    }
}
