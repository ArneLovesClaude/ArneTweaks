package com.arnestyles.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Motion Blur: die Uniform-Puffer eines Durchgangs, damit die Staerke jedes Bild neu gesetzt werden kann. */
@Mixin(PostEffectPass.class)
public interface PostEffectPassAccessor {
    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> arnestyles$getUniformBuffers();
}
