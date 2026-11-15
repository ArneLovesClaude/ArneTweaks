package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.NoChatRestrictions;
import com.arnestyles.client.mods.ShieldFix;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No Chat Restrictions: Chat, Server-Liste und Realms freischalten, auch wenn das Konto sie sperrt.
 * Shield Fixes: beim eigenen Schlag auf einen blockenden Spieler sofort das Schild-Geraeusch.
 * Nur ein @Inject (liest mit), kein @Redirect: so vertraegt es sich mit anderen Mods, die hier ebenfalls eingreifen.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public @Nullable HitResult crosshairTarget;

    @Inject(
        method = "doAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"),
        require = 0
    )
    private void arnestyles$shieldSound(CallbackInfoReturnable<Boolean> cir) {
        if (this.crosshairTarget instanceof EntityHitResult hit) {
            ShieldFix.onAttack(hit.getEntity());
        }
    }

    @Inject(method = "getUserProperties", at = @At("RETURN"), cancellable = true, require = 0)
    private void arnestyles$noChatRestrictions(CallbackInfoReturnable<UserProperties> cir) {
        UserProperties properties = cir.getReturnValue();
        if (properties != null) {
            cir.setReturnValue(NoChatRestrictions.unlock(properties));
        }
    }
}
