package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.CombatHitboxes;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Combat Hitboxes: eigene Hitbox-Darstellung (F3+B). */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true, require = 0)
    private void arnestyles$combatHitbox(Entity entity, float tickProgress, boolean inLocalServer, CallbackInfo ci) {
        if (CombatHitboxes.draw(entity, tickProgress, inLocalServer)) {
            ci.cancel();
        }
    }
}
