package com.arnestyles.client.mods;

import com.arnestyles.client.mixin.PostEffectPassAccessor;
import com.arnestyles.client.mixin.PostEffectProcessorAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.List;
import java.util.Map;

/**
 * Motion Blur: jedes Bild wird mit dem vorherigen gemischt, so verwischen schnelle Bewegungen.
 * Gilt nur fuer die Welt (mit Hand), nicht fuer HUD und Menues.
 * Fuer ArneTweaks selbst geschrieben, die Idee stammt von Natural Motion Blur (ItsPasi).
 */
public final class MotionBlur {
    private static final Identifier EFFECT = Identifier.of("arnestyles", "motion_blur");
    private static final String UNIFORM = "MotionBlurConfig";

    /** Post-Effekt, in den zuletzt der eigene Uniform-Puffer eingesetzt wurde. Nach Ressourcen-Neuladen ist er neu. */
    private static @Nullable PostEffectProcessor installedFor;
    private static @Nullable GpuBuffer strengthBuffer;
    private static long lastFrameNanos;
    /** Beim naechsten Bild das alte Bild verwerfen (nach Aus/An), damit nichts Veraltetes durchscheint. */
    private static boolean clearHistory = true;

    private MotionBlur() {
    }

    /** Wird nach dem Zeichnen der Welt aufgerufen, vor HUD und Menues. */
    public static void apply(MinecraftClient client, ObjectAllocator pool) {
        if (!ModSetting.Toggle.MOTION_BLUR.isActive() || ModSetting.Slider.MOTION_BLUR_STRENGTH.value() <= 0.0f) {
            clearHistory = true;
            return;
        }
        PostEffectProcessor processor = client.getShaderLoader().loadPostEffect(EFFECT, DefaultFramebufferSet.MAIN_ONLY);
        if (processor == null) {
            return;
        }
        if (processor != installedFor && !install(processor)) {
            return;
        }
        if (clearHistory) {
            Map<Identifier, Framebuffer> buffers = ((PostEffectProcessorAccessor) processor).arnestyles$getFramebuffers();
            buffers.values().forEach(Framebuffer::delete);
            buffers.clear();
            clearHistory = false;
        }
        writeStrength(blendFactor());
        processor.render(client.getFramebuffer(), pool);
    }

    /** Ersetzt den festen Uniform-Puffer aus der JSON durch einen, der jedes Bild beschrieben werden kann. */
    private static boolean install(PostEffectProcessor processor) {
        List<PostEffectPass> passes = ((PostEffectProcessorAccessor) processor).arnestyles$getPasses();
        if (passes.isEmpty()) {
            return false;
        }
        Map<String, GpuBuffer> uniforms = ((PostEffectPassAccessor) passes.get(0)).arnestyles$getUniformBuffers();
        if (!uniforms.containsKey(UNIFORM)) {
            return false;
        }
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "ArneTweaks Motion Blur", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
        // Der Durchgang schliesst den Puffer selbst, wenn der Effekt beim Neuladen verworfen wird
        GpuBuffer old = uniforms.put(UNIFORM, buffer);
        if (old != null) {
            old.close();
        }
        strengthBuffer = buffer;
        installedFor = processor;
        clearHistory = true;
        return true;
    }

    private static void writeStrength(float factor) {
        GpuBuffer buffer = strengthBuffer;
        if (buffer == null || buffer.isClosed()) {
            installedFor = null;
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), Std140Builder.onStack(stack, 16).putFloat(factor).get());
        }
    }

    /**
     * Der Regler gibt den Anteil des alten Bildes bei 60 FPS an. Bei mehr FPS wird pro Bild weniger gemischt,
     * damit die Spur ungefaehr gleich lang bleibt, egal wie viele Bilder pro Sekunde laufen.
     */
    private static float blendFactor() {
        long now = System.nanoTime();
        float seconds = lastFrameNanos == 0L ? 1.0f / 60.0f : (now - lastFrameNanos) / 1.0e9f;
        lastFrameNanos = now;
        seconds = Math.max(1.0f / 1000.0f, Math.min(0.1f, seconds));
        float strength = ModSetting.Slider.MOTION_BLUR_STRENGTH.value();
        return (float) Math.pow(strength, seconds * 60.0f);
    }
}
