package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ConfigManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Schlag-Tempo: nur fuer den eigenen Spieler auf dem eigenen Bildschirm laeuft die Schlag-Animation schneller oder
 * langsamer. Die Abklingzeit und alles, was der Server sieht, bleiben gleich.
 */
@Mixin(LivingEntity.class)
public abstract class SwingDurationMixin {
    @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
    private int arnestyles$swingSpeed(int duration) {
        float speed = ConfigManager.get().animSwingSpeed;
        if (Math.abs(speed - 1.0f) < 0.001f || (Object) this != MinecraftClient.getInstance().player) {
            return duration;
        }
        return Math.max(1, Math.round(duration / Math.max(0.1f, speed)));
    }
}
