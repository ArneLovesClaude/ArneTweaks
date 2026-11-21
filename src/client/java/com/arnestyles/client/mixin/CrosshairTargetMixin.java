package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.ModSetting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fadenkreuz-Farbe bei Ziel: Zielt man auf ein Lebewesen in Reichweite, wird das Fadenkreuz in der
 * gewaehlten Farbe gezeichnet (normal ist es invertiert, damit es auf jedem Untergrund sichtbar ist).
 */
@Mixin(InGameHud.class)
public abstract class CrosshairTargetMixin {
    @WrapOperation(method = "renderCrosshair", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
        ordinal = 0), require = 0)
    private void arnestyles$targetColor(DrawContext context, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h,
                                        Operation<Void> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean target = ModSetting.Toggle.CROSSHAIR_TARGET.isActive() && client.crosshairTarget instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity living && living.isAlive();
        if (com.arnestyles.client.mods.CustomCrosshair.active()) {
            // Eigenes Fadenkreuz statt der Textur (x/y/w/h ist das 15x15-Feld in der Bildschirmmitte)
            int rgb = target ? ConfigManager.get().crosshairTargetRgb : ConfigManager.get().crosshairRgb;
            float scale = ConfigManager.get().crosshairScale;
            // Mittleres Rasterpixel genau auf die Mitte des normalen Fadenkreuzes legen
            com.arnestyles.client.mods.CustomCrosshair.draw(context, x + w / 2.0f - scale / 2.0f, y + h / 2.0f - scale / 2.0f, rgb, scale);
            return;
        }
        if (target) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, w, h, 0xFF000000 | ConfigManager.get().crosshairTargetRgb);
            return;
        }
        original.call(context, pipeline, texture, x, y, w, h);
    }
}
