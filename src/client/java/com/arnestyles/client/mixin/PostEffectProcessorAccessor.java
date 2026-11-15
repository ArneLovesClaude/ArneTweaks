package com.arnestyles.client.mixin;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/** Motion Blur: Zugriff auf die Durchgaenge und die bleibenden Bild-Puffer eines Post-Effekts. */
@Mixin(PostEffectProcessor.class)
public interface PostEffectProcessorAccessor {
    @Accessor("passes")
    List<PostEffectPass> arnestyles$getPasses();

    @Accessor("framebuffers")
    Map<Identifier, Framebuffer> arnestyles$getFramebuffers();
}
