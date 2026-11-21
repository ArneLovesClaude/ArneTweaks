package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Normales Minecraft-HUD anpassen: Hotbar, Herzen/Hunger, XP-Leiste, Item-Name, Effekte, Bossbar, Scoreboard,
 * Titel und Actionbar lassen sich im HUD-Editor verschieben und skalieren. Dazu kommen Scoreboard-Optionen,
 * Hotbar-Stil und eine eigene Angriffs-Cooldown-Anzeige. Die Mixins dafuer stehen in VanillaHudMixin.
 */
public final class VanillaHud {
    public static final Identifier HOTBAR = Identifier.ofVanilla("hud/hotbar");
    public static final Identifier HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    public static final Identifier HOTBAR_OFFHAND_LEFT = Identifier.ofVanilla("hud/hotbar_offhand_left");
    public static final Identifier HOTBAR_OFFHAND_RIGHT = Identifier.ofVanilla("hud/hotbar_offhand_right");
    private static final Identifier EFFECT_BACKGROUND = Identifier.ofVanilla("hud/effect_background");
    private static final Identifier BOSS_BACKGROUND = Identifier.ofVanilla("boss_bar/purple_background");
    private static final Identifier BOSS_PROGRESS = Identifier.ofVanilla("boss_bar/purple_progress");

    private static final Comparator<ScoreboardEntry> SCORE_ORDER = Comparator.comparing(ScoreboardEntry::value)
        .reversed()
        .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static final int MAX_LINES = 15;

    // Wiederverwendete Puffer fuer das Scoreboard (kein neues Array pro Bild)
    private static final List<ScoreboardEntry> ENTRIES = new ArrayList<>();
    private static final Text[] NAMES = new Text[MAX_LINES];
    private static final Text[] SCORES = new Text[MAX_LINES];
    private static final int[] SCORE_WIDTHS = new int[MAX_LINES];

    private static Text[] sampleNames;
    private static Text[] sampleScores;

    /** Weiche Hotbar-Auswahl: aktuelle x-Position des Rahmens. */
    private static float selectionX = Float.NaN;
    private static long selectionTime;

    private VanillaHud() {
    }

    // ---- Verschieben/Skalieren ----

    public static void beginHotbar(DrawContext context) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        HudLayout.begin(context, HudLayout.Element.V_HOTBAR, w / 2 - 91, h - 23, 182, 23, 0.5f, 1.0f);
    }

    public static void beginStatus(DrawContext context) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        HudLayout.begin(context, HudLayout.Element.V_STATUS, w / 2 - 91, h - 50, 182, 21, 0.5f, 1.0f);
    }

    public static void beginXp(DrawContext context) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        HudLayout.begin(context, HudLayout.Element.V_XP, w / 2 - 91, h - 36, 182, 12, 0.5f, 1.0f);
    }

    public static void beginItemName(DrawContext context, int textWidth) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        int width = Math.max(40, textWidth) + 4;
        HudLayout.begin(context, HudLayout.Element.V_ITEM_NAME, w / 2 - width / 2, h - 61, width, 13, 0.5f, 1.0f);
    }

    public static void beginEffects(DrawContext context, MinecraftClient client) {
        int good = 0;
        int bad = 0;
        if (client.player != null) {
            for (StatusEffectInstance effect : client.player.getStatusEffects()) {
                if (!effect.shouldShowIcon()) {
                    continue;
                }
                if (effect.getEffectType().value().isBeneficial()) {
                    good++;
                } else {
                    bad++;
                }
            }
        }
        int cols = Math.max(1, Math.max(good, bad));
        int w = context.getScaledWindowWidth();
        int height = bad > 0 ? 51 : 25;
        HudLayout.begin(context, HudLayout.Element.V_EFFECTS, w - 25 * cols, 1, 25 * cols, height, 1.0f, 0.0f);
    }

    public static void beginBossBar(DrawContext context, int bars) {
        int w = context.getScaledWindowWidth();
        int n = Math.max(1, bars);
        HudLayout.begin(context, HudLayout.Element.V_BOSSBAR, w / 2 - 91, 2, 182, 16 + 19 * (n - 1), 0.5f, 0.0f);
    }

    public static void beginTitle(DrawContext context, TextRenderer font, Text title, Text subtitle) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        int width = Math.max(font.getWidth(title) * 4, subtitle == null ? 0 : font.getWidth(subtitle) * 2) + 16;
        int top = h / 2 - 48;
        int height = subtitle == null ? 46 : 78;
        HudLayout.begin(context, HudLayout.Element.V_TITLE, w / 2 - width / 2, top, width, height, 0.5f, 0.5f);
    }

    public static void beginActionbar(DrawContext context, TextRenderer font, Text message) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        int k = font.getWidth(message);
        HudLayout.begin(context, HudLayout.Element.V_ACTIONBAR, w / 2 - k / 2 - 3, h - 74, k + 6, 14, 0.5f, 1.0f);
    }

    // ---- Beispiele im HUD-Editor (wenn das echte Teil gerade nicht zu sehen ist) ----

    public static void sampleTitle(DrawContext context, TextRenderer font) {
        Text title = Text.translatable("arnestyles.hud.sample_title");
        Text subtitle = Text.translatable("arnestyles.hud.sample_subtitle");
        beginTitle(context, font, title, subtitle);
        try {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2.0f, context.getScaledWindowHeight() / 2.0f);
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(4.0f, 4.0f);
            int tw = font.getWidth(title);
            context.drawTextWithBackground(font, title, -tw / 2, -10, tw, 0xFFFFFFFF);
            context.getMatrices().popMatrix();
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(2.0f, 2.0f);
            int sw = font.getWidth(subtitle);
            context.drawTextWithBackground(font, subtitle, -sw / 2, 5, sw, 0xFFFFFFFF);
            context.getMatrices().popMatrix();
            context.getMatrices().popMatrix();
        } finally {
            HudLayout.end(context);
        }
    }

    public static void sampleActionbar(DrawContext context, TextRenderer font) {
        Text message = Text.translatable("arnestyles.hud.sample_actionbar");
        beginActionbar(context, font, message);
        try {
            int k = font.getWidth(message);
            context.drawTextWithBackground(font, message, context.getScaledWindowWidth() / 2 - k / 2, context.getScaledWindowHeight() - 72, k, 0xFFFFFFFF);
        } finally {
            HudLayout.end(context);
        }
    }

    public static void sampleItemName(DrawContext context, TextRenderer font) {
        Text name = Text.translatable("item.minecraft.diamond_sword");
        int k = font.getWidth(name);
        beginItemName(context, k);
        try {
            context.drawTextWithBackground(font, name, (context.getScaledWindowWidth() - k) / 2, context.getScaledWindowHeight() - 59, k, 0xFFFFFFFF);
        } finally {
            HudLayout.end(context);
        }
    }

    public static void sampleBossBar(DrawContext context, TextRenderer font) {
        beginBossBar(context, 1);
        try {
            int x = context.getScaledWindowWidth() / 2 - 91;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BOSS_BACKGROUND, 182, 5, 0, 0, x, 12, 182, 5);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BOSS_PROGRESS, 182, 5, 0, 0, x, 12, 120, 5);
            Text name = Text.translatable("entity.minecraft.ender_dragon");
            context.drawTextWithShadow(font, name, context.getScaledWindowWidth() / 2 - font.getWidth(name) / 2, 3, Colors.WHITE);
        } finally {
            HudLayout.end(context);
        }
    }

    public static void sampleEffects(DrawContext context) {
        beginEffects(context, MinecraftClient.getInstance());
        try {
            int x = context.getScaledWindowWidth() - 25;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND, x, 1, 24, 24);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(StatusEffects.SPEED), x + 3, 4, 18, 18);
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Scoreboard ----

    /** Scoreboard rechts, wie Vanilla, aber mit eigenen Optionen. Wird anstelle der Vanilla-Methode gezeichnet. */
    public static void renderScoreboard(DrawContext context, TextRenderer font, ScoreboardObjective objective) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat format = objective.getNumberFormatOr(StyledNumberFormat.RED);
        ENTRIES.clear();
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            if (!entry.hidden()) {
                ENTRIES.add(entry);
            }
        }
        ENTRIES.sort(SCORE_ORDER);
        int count = Math.min(MAX_LINES, ENTRIES.size());
        boolean numbers = !ConfigManager.get().scoreboardHideNumbers;
        for (int i = 0; i < count; i++) {
            ScoreboardEntry entry = ENTRIES.get(i);
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            NAMES[i] = Team.decorateName(team, entry.name());
            SCORES[i] = numbers ? entry.formatted(format) : Text.empty();
            SCORE_WIDTHS[i] = numbers ? font.getWidth(SCORES[i]) : 0;
        }
        ENTRIES.clear();
        drawScoreboard(context, font, objective.getDisplayName(), NAMES, SCORES, SCORE_WIDTHS, count);
    }

    public static void sampleScoreboard(DrawContext context, TextRenderer font) {
        if (sampleNames == null) {
            sampleNames = new Text[] {Text.literal("Kills"), Text.literal("Deaths"), Text.literal("Totems"), Text.literal("Coins")};
            sampleScores = new Text[] {Text.literal("12").withColor(0xFF5555), Text.literal("3").withColor(0xFF5555),
                Text.literal("8").withColor(0xFF5555), Text.literal("250").withColor(0xFF5555)};
        }
        boolean numbers = !ConfigManager.get().scoreboardHideNumbers;
        int[] widths = new int[sampleNames.length];
        Text[] scores = new Text[sampleNames.length];
        for (int i = 0; i < sampleNames.length; i++) {
            scores[i] = numbers ? sampleScores[i] : Text.empty();
            widths[i] = numbers ? font.getWidth(sampleScores[i]) : 0;
        }
        drawScoreboard(context, font, Text.literal("ArneTweaks"), sampleNames, scores, widths, sampleNames.length);
    }

    private static void drawScoreboard(DrawContext context, TextRenderer font, Text title, Text[] names, Text[] scores, int[] scoreWidths, int count) {
        ArneStylesConfig c = ConfigManager.get();
        MinecraftClient client = MinecraftClient.getInstance();
        int titleWidth = font.getWidth(title);
        int width = titleWidth;
        int joiner = font.getWidth(": ");
        for (int i = 0; i < count; i++) {
            width = Math.max(width, font.getWidth(names[i]) + (scoreWidths[i] > 0 ? joiner + scoreWidths[i] : 0));
        }
        int bottom = context.getScaledWindowHeight() / 2 + count * 9 / 3;
        int left = context.getScaledWindowWidth() - width - 3;
        int right = context.getScaledWindowWidth() - 3 + 2;
        int top = bottom - count * 9;
        int body;
        int head;
        if (Math.abs(c.scoreboardBgOpacity - 0.3f) < 0.001f) {
            // Standardwert: wie Vanilla (beachtet die Barrierefreiheits-Einstellung fuer Text-Hintergruende)
            body = client.options.getTextBackgroundColor(0.3f);
            head = client.options.getTextBackgroundColor(0.4f);
        } else {
            body = ColorHelper.fromFloats(c.scoreboardBgOpacity, 0.0f, 0.0f, 0.0f);
            head = ColorHelper.fromFloats(Math.min(1.0f, c.scoreboardBgOpacity + 0.1f), 0.0f, 0.0f, 0.0f);
        }
        HudLayout.begin(context, HudLayout.Element.V_SCOREBOARD, left - 2, top - 10, right - left + 2, bottom - top + 10, 1.0f, 0.5f);
        try {
            if ((head >>> 24) != 0) {
                context.fill(left - 2, top - 9 - 1, right, top - 1, head);
            }
            if ((body >>> 24) != 0) {
                context.fill(left - 2, top - 1, right, bottom, body);
            }
            boolean shadow = c.scoreboardShadow;
            context.drawText(font, title, left + width / 2 - titleWidth / 2, top - 9, Colors.WHITE, shadow);
            for (int i = 0; i < count; i++) {
                int y = bottom - (count - i) * 9;
                context.drawText(font, names[i], left, y, Colors.WHITE, shadow);
                if (scoreWidths[i] > 0) {
                    context.drawText(font, scores[i], right - scoreWidths[i], y, Colors.WHITE, shadow);
                }
            }
        } finally {
            HudLayout.end(context);
        }
    }

    // ---- Hotbar-Stil ----

    /**
     * Ein Hotbar-Bild selbst zeichnen: Deckkraft, Farbe und weiche Bewegung des Auswahl-Rahmens.
     * false = nichts geaendert, dann zeichnet Vanilla wie immer.
     */
    public static boolean drawHotbarSprite(DrawContext context, Identifier sprite, int x, int y, int width, int height) {
        ArneStylesConfig c = ConfigManager.get();
        if (!c.modEnabled) {
            return false;
        }
        if (HOTBAR_SELECTION.equals(sprite)) {
            float drawX = x;
            if (ModSetting.Toggle.HOTBAR_SMOOTH.isActive()) {
                long now = Util.getMeasuringTimeMs();
                if (Float.isNaN(selectionX) || Math.abs(selectionX - x) > 200.0f || now - selectionTime > 500L) {
                    selectionX = x;
                } else {
                    float dt = Math.min(100.0f, now - selectionTime);
                    float k = 1.0f - (float) Math.exp(-dt / 45.0f);
                    selectionX += (x - selectionX) * k;
                    if (Math.abs(selectionX - x) < 0.05f) {
                        selectionX = x;
                    }
                }
                selectionTime = now;
                drawX = selectionX;
            } else {
                selectionX = Float.NaN;
            }
            int color = 0xFF000000 | (c.hotbarSelectRgb & 0xFFFFFF);
            if (drawX == x && color == 0xFFFFFFFF) {
                return false;
            }
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(drawX - x, 0.0f);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, color);
            context.getMatrices().popMatrix();
            return true;
        }
        if (c.hotbarOpacity < 0.999f && (HOTBAR.equals(sprite) || HOTBAR_OFFHAND_LEFT.equals(sprite) || HOTBAR_OFFHAND_RIGHT.equals(sprite))) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, ColorHelper.getWhite(c.hotbarOpacity));
            return true;
        }
        return false;
    }

    // ---- Eigene Angriffs-Cooldown-Anzeige ----

    public static boolean customCooldown() {
        return ModSetting.Toggle.COOLDOWN_INDICATOR.isActive();
    }

    public static void renderCooldown(DrawContext context, MinecraftClient client, RenderTickCounter tickCounter) {
        if (!customCooldown()) {
            return;
        }
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        float progress = player.getAttackCooldownProgress(tickCounter.getTickProgress(false));
        if (progress >= 1.0f && !HudLayout.editing) {
            return;
        }
        if (HudLayout.editing && progress >= 1.0f) {
            progress = 0.6f;
        }
        ArneStylesConfig c = ConfigManager.get();
        int style = Math.round(c.cooldownStyle);
        int color = 0xFF000000 | (c.cooldownRgb & 0xFFFFFF);
        int faint = 0x50000000 | (c.cooldownRgb & 0xFFFFFF);
        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;
        TextRenderer font = client.textRenderer;
        switch (style) {
            case 1 -> { // Ring um das Fadenkreuz
                int r = 10;
                HudLayout.begin(context, HudLayout.Element.COOLDOWN, cx - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2, 0.5f, 0.5f);
                try {
                    int points = 40;
                    int lit = Math.round(progress * points);
                    for (int i = 0; i < points; i++) {
                        double a = -Math.PI / 2 + i * (Math.PI * 2 / points);
                        int px = cx + (int) Math.round(Math.cos(a) * r);
                        int py = cy + (int) Math.round(Math.sin(a) * r);
                        context.fill(px - 1, py - 1, px + 1, py + 1, i < lit ? color : faint);
                    }
                } finally {
                    HudLayout.end(context);
                }
            }
            case 2 -> { // Fuenf Punkte
                int w = 5 * 3 + 4 * 2;
                int x = cx - w / 2;
                int y = cy + 9;
                HudLayout.begin(context, HudLayout.Element.COOLDOWN, x, y, w, 3, 0.5f, 0.0f);
                try {
                    int lit = (int) (progress * 5.0f);
                    for (int i = 0; i < 5; i++) {
                        int dx = x + i * 5;
                        context.fill(dx, y, dx + 3, y + 3, i < lit ? color : faint);
                    }
                } finally {
                    HudLayout.end(context);
                }
            }
            case 3 -> { // Prozent
                String text = Math.round(progress * 100.0f) + "%";
                int w = font.getWidth(text);
                int x = cx - w / 2;
                int y = cy + 9;
                HudLayout.begin(context, HudLayout.Element.COOLDOWN, x, y, w, 9, 0.5f, 0.0f);
                try {
                    context.drawText(font, text, x, y, color, true);
                } finally {
                    HudLayout.end(context);
                }
            }
            default -> { // Balken
                int w = 24;
                int x = cx - w / 2;
                int y = cy + 9;
                HudLayout.begin(context, HudLayout.Element.COOLDOWN, x - 1, y - 1, w + 2, 4, 0.5f, 0.0f);
                try {
                    context.fill(x - 1, y - 1, x + w + 1, y + 3, 0x80000000);
                    context.fill(x, y, x + Math.round(w * progress), y + 2, color);
                } finally {
                    HudLayout.end(context);
                }
            }
        }
    }
}
