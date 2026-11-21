package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.HudLayout;
import com.arnestyles.client.mods.VanillaHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla-HUD anpassbar: jedes Teil (Hotbar, Herzen/Hunger, XP, Item-Name, Effekte, Bossbar, Scoreboard,
 * Titel, Actionbar) wird im HUD-Editor verschoben und skaliert. Steht ein Teil normal (keine Verschiebung,
 * 100 %), bleibt alles wie in Vanilla. Dazu Scoreboard-Optionen, Hotbar-Stil und eigene Cooldown-Anzeige.
 */
@Mixin(InGameHud.class)
public abstract class VanillaHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private @Nullable Text title;
    @Shadow
    private @Nullable Text subtitle;
    @Shadow
    private int titleRemainTicks;
    @Shadow
    private @Nullable Text overlayMessage;
    @Shadow
    private int overlayRemaining;
    @Shadow
    private int heldItemTooltipFade;
    @Shadow
    private ItemStack currentStack;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Unique
    private boolean arnestyles$hotbarPushed;
    @Unique
    private boolean arnestyles$titlePushed;
    @Unique
    private boolean arnestyles$actionbarPushed;

    @Unique
    private static boolean arnestyles$on() {
        return ConfigManager.get().modEnabled;
    }

    /** Verschieben lohnt nur, wenn das Teil nicht an der normalen Stelle steht oder der Editor offen ist. */
    @Unique
    private static boolean arnestyles$move(HudLayout.Element element) {
        return arnestyles$on() && (HudLayout.editingVanilla() || !HudLayout.untouched(element));
    }

    // ---- Hotbar ----

    /*
     * Hotbar direkt in renderHotbar verschieben (nicht um den Aufruf herum): Fabric haengt eigene HUD-Elemente
     * (Info-HUD, Ruestungsanzeige) hinter den Hotbar-Aufruf, die sollen nicht mitwandern.
     */
    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void arnestyles$hotbarStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        this.arnestyles$hotbarPushed = arnestyles$move(HudLayout.Element.V_HOTBAR);
        if (this.arnestyles$hotbarPushed) {
            VanillaHud.beginHotbar(context);
        }
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void arnestyles$hotbarEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.arnestyles$hotbarPushed) {
            this.arnestyles$hotbarPushed = false;
            HudLayout.end(context);
        }
    }

    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void arnestyles$hotbarStyle(DrawContext context, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height,
                                        Operation<Void> original) {
        if (!VanillaHud.drawHotbarSprite(context, sprite, x, y, width, height)) {
            original.call(context, pipeline, sprite, x, y, width, height);
        }
    }

    // ---- Herzen, Ruestung, Hunger, Luft (und Reittier-Leben) ----

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void arnestyles$statusBars(InGameHud hud, DrawContext context, Operation<Void> original) {
        if (!arnestyles$move(HudLayout.Element.V_STATUS)) {
            original.call(hud, context);
            return;
        }
        VanillaHud.beginStatus(context);
        try {
            original.call(hud, context);
        } finally {
            HudLayout.end(context);
        }
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;renderMountHealth(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void arnestyles$mountHealth(InGameHud hud, DrawContext context, Operation<Void> original) {
        if (!arnestyles$move(HudLayout.Element.V_STATUS)) {
            original.call(hud, context);
            return;
        }
        VanillaHud.beginStatus(context);
        try {
            original.call(hud, context);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- XP-Leiste (auch Sprung- und Ortungsleiste) mit Level ----

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void arnestyles$xpBar(Bar bar, DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        if (!arnestyles$move(HudLayout.Element.V_XP)) {
            original.call(bar, context, tickCounter);
            return;
        }
        VanillaHud.beginXp(context);
        try {
            original.call(bar, context, tickCounter);
        } finally {
            HudLayout.end(context);
        }
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/bar/Bar;renderAddons(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void arnestyles$xpAddons(Bar bar, DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        if (!arnestyles$move(HudLayout.Element.V_XP)) {
            original.call(bar, context, tickCounter);
            return;
        }
        VanillaHud.beginXp(context);
        try {
            original.call(bar, context, tickCounter);
        } finally {
            HudLayout.end(context);
        }
    }

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"))
    private void arnestyles$xpLevel(DrawContext context, TextRenderer font, int level, Operation<Void> original) {
        if (!arnestyles$move(HudLayout.Element.V_XP)) {
            original.call(context, font, level);
            return;
        }
        VanillaHud.beginXp(context);
        try {
            original.call(context, font, level);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Name des gehaltenen Items ----

    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHeldItemTooltip(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void arnestyles$itemName(InGameHud hud, DrawContext context, Operation<Void> original) {
        if (!arnestyles$on()) {
            original.call(hud, context);
            return;
        }
        if (ConfigManager.get().heldItemNameHide) {
            return;
        }
        boolean showing = this.heldItemTooltipFade > 0 && !this.currentStack.isEmpty();
        if (HudLayout.editingVanilla() && !showing) {
            VanillaHud.sampleItemName(context, getTextRenderer());
            return;
        }
        if (!arnestyles$move(HudLayout.Element.V_ITEM_NAME) || !showing) {
            original.call(hud, context);
            return;
        }
        VanillaHud.beginItemName(context, getTextRenderer().getWidth(this.currentStack.getName()));
        try {
            original.call(hud, context);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Effekt-Symbole oben rechts ----

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void arnestyles$effects(InGameHud hud, DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        if (!arnestyles$on()) {
            original.call(hud, context, tickCounter);
            return;
        }
        if (ConfigManager.get().effectIconsHide) {
            return;
        }
        boolean any = this.client.player != null && !this.client.player.getStatusEffects().isEmpty();
        if (HudLayout.editingVanilla() && !any) {
            VanillaHud.sampleEffects(context);
            return;
        }
        if (!arnestyles$move(HudLayout.Element.V_EFFECTS) || !any) {
            original.call(hud, context, tickCounter);
            return;
        }
        VanillaHud.beginEffects(context, this.client);
        try {
            original.call(hud, context, tickCounter);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Bossbar ----

    @WrapOperation(method = "renderBossBarHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/BossBarHud;render(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void arnestyles$bossBar(BossBarHud bossBars, DrawContext context, Operation<Void> original) {
        if (!arnestyles$on()) {
            original.call(bossBars, context);
            return;
        }
        if (ConfigManager.get().bossbarHide) {
            return;
        }
        int count = ((BossBarHudAccessor) bossBars).arnestyles$getBossBars().size();
        if (HudLayout.editingVanilla() && count == 0) {
            VanillaHud.sampleBossBar(context, getTextRenderer());
            return;
        }
        if (!arnestyles$move(HudLayout.Element.V_BOSSBAR) || count == 0) {
            original.call(bossBars, context);
            return;
        }
        VanillaHud.beginBossBar(context, count);
        try {
            original.call(bossBars, context);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Scoreboard ----

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At("HEAD"), cancellable = true)
    private void arnestyles$scoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (!arnestyles$on()) {
            return;
        }
        ci.cancel();
        if (!ConfigManager.get().scoreboardHide) {
            VanillaHud.renderScoreboard(context, getTextRenderer(), objective);
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
        at = @At("RETURN"))
    private void arnestyles$scoreboardSample(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HudLayout.editingVanilla() && arnestyles$on() && !ConfigManager.get().scoreboardHide && HudLayout.bounds(HudLayout.Element.V_SCOREBOARD) == null) {
            VanillaHud.sampleScoreboard(context, getTextRenderer());
        }
    }

    // ---- Titel und Untertitel ----

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void arnestyles$titleStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        this.arnestyles$titlePushed = false;
        if (!arnestyles$on()) {
            return;
        }
        boolean showing = this.title != null && this.titleRemainTicks > 0;
        if (HudLayout.editingVanilla() && !showing) {
            VanillaHud.sampleTitle(context, getTextRenderer());
            ci.cancel();
            return;
        }
        if (showing && arnestyles$move(HudLayout.Element.V_TITLE)) {
            VanillaHud.beginTitle(context, getTextRenderer(), this.title, this.subtitle);
            this.arnestyles$titlePushed = true;
        }
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("RETURN"))
    private void arnestyles$titleEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.arnestyles$titlePushed) {
            this.arnestyles$titlePushed = false;
            HudLayout.end(context);
        }
    }

    // ---- Actionbar ----

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void arnestyles$actionbarStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        this.arnestyles$actionbarPushed = false;
        if (!arnestyles$on()) {
            return;
        }
        boolean showing = this.overlayMessage != null && this.overlayRemaining > 0;
        if (HudLayout.editingVanilla() && !showing) {
            VanillaHud.sampleActionbar(context, getTextRenderer());
            ci.cancel();
            return;
        }
        if (showing && arnestyles$move(HudLayout.Element.V_ACTIONBAR)) {
            VanillaHud.beginActionbar(context, getTextRenderer(), this.overlayMessage);
            this.arnestyles$actionbarPushed = true;
        }
    }

    @Inject(method = "renderOverlayMessage", at = @At("RETURN"))
    private void arnestyles$actionbarEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.arnestyles$actionbarPushed) {
            this.arnestyles$actionbarPushed = false;
            HudLayout.end(context);
        }
    }

    // ---- Eigene Cooldown-Anzeige ersetzt die Vanilla-Anzeige ----

    @ModifyExpressionValue(method = {"renderCrosshair", "renderHotbar"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object arnestyles$hideVanillaCooldown(Object value) {
        return VanillaHud.customCooldown() ? AttackIndicator.OFF : value;
    }
}
