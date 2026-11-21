package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.TotemPop;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem-Partikel-Farbe: Minecraft (gruen-gelb), Regenbogen oder eine feste Farbe. */
@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin extends AnimatedParticle {
    protected TotemParticleMixin(ClientWorld world, double x, double y, double z, SpriteProvider sprites, float upwardsAcceleration) {
        super(world, x, y, z, sprites, upwardsAcceleration);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arnestyles$color(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites,
                                  CallbackInfo ci) {
        int rgb = TotemPop.particleColor(this.random);
        if (rgb >= 0) {
            this.setColor(((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f);
        }
    }
}
