package com.arnestyles.client.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf die Overlay-Textur (rote Treffer-Farbe) fuer die eigene Treffer-Farbe. */
@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    @Accessor("texture")
    NativeImageBackedTexture arnestyles$texture();
}
