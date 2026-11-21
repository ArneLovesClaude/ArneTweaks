package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.TotemCounter;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TotemCounter: Pops hinter dem Namen im Namensschild ("Name -3"). Nur die Anzeige, der echte Name bleibt. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void arnestyles$totemPops(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.displayName != null) {
            state.displayName = TotemCounter.withPops(entity.getUuid(), state.displayName);
            state.displayName = com.arnestyles.client.mods.Nametags.decorate(entity, state.displayName);
        }
    }
}
