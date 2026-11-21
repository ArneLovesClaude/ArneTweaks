package com.arnestyles.client.mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Info-HUD: Keystrokes mit CPS, CPS-Zaehler, FPS, Ping, Koordinaten, Combo-Zaehler, Pearl-Cooldown,
 * Ruestungs-Warnung und Gegner-Info. Jede Anzeige laesst sich einzeln an- und ausschalten und im HUD-Editor
 * verschieben und skalieren. CPS-Idee nach KeysPerSecond (RoanH), Code komplett neu geschrieben.
 */
public final class InfoHud {
    private static final int TEXT_PRESSED = 0xFF000000;
    private static final int KEY = 22;
    private static final int GAP = 2;
    private static final long CPS_WINDOW_MS = 1000L;
    private static final long TARGET_MEMORY_MS = 8000L;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ItemStack PEARL = new ItemStack(Items.ENDER_PEARL);

    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static int maxCps;
    private static int combo;
    private static int ticksSinceHit = 999;
    private static int lastHurtTime;
    private static @Nullable LivingEntity lastTarget;
    private static long lastTargetAt;
    private static final boolean[] WARNED = new boolean[4];

    private InfoHud() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity living && player == MinecraftClient.getInstance().player) {
                onAttack(living);
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(InfoHud::tick);
        ClientTickEvents.END_CLIENT_TICK.register(DeathPoint::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ItemCounter::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ExtraHud::tick);
    }

    // ---- Zaehlen ----

    /** Jeder Tastendruck im Spiel (auch Maustasten), aus KeyBinding.onKeyPressed. */
    public static void onKeyPressed(InputUtil.Key key) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (key.equals(KeyBindingHelper.getBoundKeyOf(client.options.attackKey))) {
            LEFT_CLICKS.addLast(now);
        } else if (key.equals(KeyBindingHelper.getBoundKeyOf(client.options.useKey))) {
            RIGHT_CLICKS.addLast(now);
        }
    }

    private static int cps(Deque<Long> clicks) {
        long limit = System.currentTimeMillis() - CPS_WINDOW_MS;
        while (!clicks.isEmpty() && clicks.peekFirst() < limit) {
            clicks.removeFirst();
        }
        return clicks.size();
    }

    private static void onAttack(LivingEntity target) {
        HitParticles.onAttack(target);
        combo++;
        ticksSinceHit = 0;
        lastTarget = target;
        lastTargetAt = System.currentTimeMillis();
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            combo = 0;
            lastTarget = null;
            return;
        }
        ticksSinceHit++;
        // Combo endet, wenn man selbst getroffen wird oder 3 Sekunden nichts trifft
        if (player.hurtTime > lastHurtTime || ticksSinceHit > Math.round(com.arnestyles.client.config.ConfigManager.get().comboTimeout * 20.0f)) {
            combo = 0;
        }
        lastHurtTime = player.hurtTime;
        int left = cps(LEFT_CLICKS);
        int right = cps(RIGHT_CLICKS);
        maxCps = Math.max(maxCps, Math.max(left, right));

        // Ruestungs-Warnung: einmal Ton, wenn ein Teil unter die Grenze faellt
        for (int i = 0; i < ARMOR.length; i++) {
            boolean low = lowArmor(player.getEquippedStack(ARMOR[i]));
            if (low && !WARNED[i] && ModSetting.Toggle.ARMOR_WARNING.isActive() && ModSetting.Toggle.ARMOR_WARNING_SOUND.isActive()) {
                client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 0.6f));
            }
            WARNED[i] = low;
        }
    }

    private static boolean lowArmor(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return false;
        }
        int max = stack.getMaxDamage();
        return max > 0 && (max - stack.getDamage()) < max * com.arnestyles.client.config.ConfigManager.get().armorWarningPercent;
    }

    // ---- Zeichnen ----

    /** Alles Info-HUD auf einmal (ein HUD-Element bei Fabric). */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (ModSetting.Toggle.KEYSTROKES.isActive()) {
            renderKeystrokes(context, client);
        }
        int col = 78;
        int row = 4;
        if (ModSetting.Toggle.FPS_HUD.isActive()) {
            label(context, client, HudLayout.Element.FPS, col, row, client.getCurrentFps() + " FPS");
        }
        if (ModSetting.Toggle.PING_HUD.isActive()) {
            label(context, client, HudLayout.Element.PING, col, row + 14, ping(client) + " ms");
        }
        if (ModSetting.Toggle.CPS_HUD.isActive()) {
            label(context, client, HudLayout.Element.CPS, col, row + 28,
                cps(LEFT_CLICKS) + " | " + cps(RIGHT_CLICKS) + " CPS  (max " + maxCps + ")");
        }
        if (ModSetting.Toggle.COORDS_HUD.isActive()) {
            label(context, client, HudLayout.Element.COORDS, col, row + 42, coords(client));
        }
        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;
        if (ModSetting.Toggle.COMBO_HUD.isActive() && (combo > 0 || HudLayout.editing)) {
            int shown = combo > 0 ? combo : 3;
            label(context, client, HudLayout.Element.COMBO, cx + 12, cy - 22, Text.translatable("arnestyles.hud.combo_value", shown).getString());
        }
        if (ModSetting.Toggle.PEARL_COOLDOWN.isActive()) {
            renderPearl(context, client, tickCounter, cx, cy);
        }
        if (ModSetting.Toggle.ARMOR_WARNING.isActive()) {
            renderArmorWarning(context, client, cx, cy);
        }
        if (ModSetting.Toggle.TARGET_HUD.isActive()) {
            renderTarget(context, client, cx, cy);
        }
        String death = DeathPoint.label(client);
        if (death != null) {
            label(context, client, HudLayout.Element.DEATH, col, row + 56, death);
        }
        java.util.List<String> waypoints = Waypoints.labels(client);
        if (!waypoints.isEmpty()) {
            renderWaypoints(context, client, col, row + 70, waypoints);
        }
        SpotifyHud.render(context, client, 4, context.getScaledWindowHeight() / 2 - 20);
        ItemCounter.render(context, client);
        ExtraHud.render(context, client);
        VanillaHud.renderCooldown(context, client, tickCounter);
    }

    /** Wegpunkte als ein verschiebbarer Block (mehrere Zeilen). */
    private static void renderWaypoints(DrawContext context, MinecraftClient client, int x, int y, java.util.List<String> lines) {
        TextRenderer font = client.textRenderer;
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, font.getWidth(line));
        }
        w += 6;
        int h = lines.size() * 11 + 2;
        HudLayout.begin(context, HudLayout.Element.WAYPOINTS, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            for (int i = 0; i < lines.size(); i++) {
                HudStyle.text(context, font, lines.get(i), x + 3, y + 2 + i * 11, HudStyle.text());
            }
        } finally {
            HudLayout.end(context);
        }
    }

    /** Text mit dunklem Hintergrund als verschiebbare Anzeige. */
    private static void label(DrawContext context, MinecraftClient client, HudLayout.Element element, int x, int y, String text) {
        TextRenderer font = client.textRenderer;
        int w = font.getWidth(text) + 6;
        int h = 12;
        HudLayout.begin(context, element, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            HudStyle.text(context, font, text, x + 3, y + 2, HudStyle.text());
        } finally {
            HudLayout.end(context);
        }
    }

    private static int ping(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) {
            return 0;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static String coords(MinecraftClient client) {
        PlayerEntity p = client.player;
        String xyz = String.format(Locale.ROOT, "XYZ %d / %d / %d", MathHelper.floor(p.getX()), MathHelper.floor(p.getY()), MathHelper.floor(p.getZ()));
        if (!com.arnestyles.client.config.ConfigManager.get().coordsDirection) {
            return xyz;
        }
        return xyz + "  " + Text.translatable("arnestyles.hud.dir." + p.getHorizontalFacing().asString()).getString();
    }

    /** W, A, S, D, Maustasten mit CPS und Leertaste. Gedrueckte Tasten werden hell. */
    private static void renderKeystrokes(DrawContext context, MinecraftClient client) {
        int x = 4;
        int y = 4;
        int w = 3 * KEY + 2 * GAP;
        var cfg = com.arnestyles.client.config.ConfigManager.get();
        int mouseH = !cfg.keystrokesMouse ? 0 : ModSetting.Toggle.KEYSTROKES_CPS.isActive() ? KEY : 16;
        int h = 2 * KEY + (cfg.keystrokesMouse ? mouseH + GAP : 0) + (cfg.keystrokesSpace ? 10 + GAP : 0);
        HudLayout.begin(context, HudLayout.Element.KEYSTROKES, x, y, w, h);
        try {
            var o = client.options;
            key(context, client, x + KEY + GAP, y, KEY, KEY, keyName(o.forwardKey), o.forwardKey.isPressed());
            int y2 = y + KEY + GAP;
            key(context, client, x, y2, KEY, KEY, keyName(o.leftKey), o.leftKey.isPressed());
            key(context, client, x + KEY + GAP, y2, KEY, KEY, keyName(o.backKey), o.backKey.isPressed());
            key(context, client, x + 2 * (KEY + GAP), y2, KEY, KEY, keyName(o.rightKey), o.rightKey.isPressed());
            int y3 = y2 + KEY + GAP;
            if (cfg.keystrokesMouse) {
                int half = (w - GAP) / 2;
                boolean showCps = ModSetting.Toggle.KEYSTROKES_CPS.isActive();
                mouseKey(context, client, x, y3, half, mouseH, "LMB", cps(LEFT_CLICKS), o.attackKey.isPressed(), showCps);
                mouseKey(context, client, x + half + GAP, y3, w - half - GAP, mouseH, "RMB", cps(RIGHT_CLICKS), o.useKey.isPressed(), showCps);
                y3 += mouseH + GAP;
            }
            if (cfg.keystrokesSpace) {
                boolean jump = o.jumpKey.isPressed();
                HudStyle.background(context, x, y3, x + w, y3 + 10, jump ? HudStyle.accent() : HudStyle.bg());
                context.fill(x + w / 2 - 10, y3 + 4, x + w / 2 + 10, y3 + 5, jump ? TEXT_PRESSED : HudStyle.text());
            }
        } finally {
            HudLayout.end(context);
        }
    }

    private static String keyName(KeyBinding binding) {
        String name = binding.getBoundKeyLocalizedText().getString();
        return name.length() > 3 ? name.substring(0, 3) : name.toUpperCase(Locale.ROOT);
    }

    private static void key(DrawContext context, MinecraftClient client, int x, int y, int w, int h, String label, boolean pressed) {
        HudStyle.background(context, x, y, x + w, y + h, pressed ? HudStyle.accent() : HudStyle.bg());
        TextRenderer font = client.textRenderer;
        context.drawText(font, label, x + (w - font.getWidth(label)) / 2, y + (h - 8) / 2, pressed ? TEXT_PRESSED : HudStyle.text(),
            !pressed && HudStyle.shadow());
    }

    private static void mouseKey(DrawContext context, MinecraftClient client, int x, int y, int w, int h, String label, int cps, boolean pressed,
                                 boolean showCps) {
        HudStyle.background(context, x, y, x + w, y + h, pressed ? HudStyle.accent() : HudStyle.bg());
        TextRenderer font = client.textRenderer;
        int color = pressed ? TEXT_PRESSED : HudStyle.text();
        if (!showCps) {
            context.drawText(font, label, x + (w - font.getWidth(label)) / 2, y + (h - 8) / 2, color, !pressed && HudStyle.shadow());
            return;
        }
        context.drawText(font, label, x + (w - font.getWidth(label)) / 2, y + 3, color, !pressed && HudStyle.shadow());
        String value = cps + " CPS";
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + w / 2.0f, y + 13);
        context.getMatrices().scale(0.75f, 0.75f);
        context.drawText(font, value, -font.getWidth(value) / 2, 0, color, false);
        context.getMatrices().popMatrix();
    }

    /** Pearl-Cooldown als Balken mit Perle unter dem Fadenkreuz. */
    private static void renderPearl(DrawContext context, MinecraftClient client, RenderTickCounter tickCounter, int cx, int cy) {
        float progress = client.player.getItemCooldownManager().getCooldownProgress(PEARL, tickCounter.getTickProgress(true));
        if (progress <= 0.0f && HudLayout.editing) {
            progress = 0.6f;
        }
        if (progress <= 0.0f) {
            return;
        }
        int w = 44;
        int x = cx - w / 2;
        int y = cy + 14;
        HudLayout.begin(context, HudLayout.Element.PEARL, x, y, w, 12);
        try {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(0.75f, 0.75f);
            context.drawItem(PEARL, 0, 0);
            context.getMatrices().popMatrix();
            int bx = x + 14;
            context.fill(bx, y + 4, x + w, y + 8, HudStyle.bg());
            context.fill(bx, y + 4, bx + Math.round((x + w - bx) * progress), y + 8, HudStyle.accent());
        } finally {
            HudLayout.end(context);
        }
    }

    /** Blinkende Warnung ueber dem Fadenkreuz, wenn ein Ruestungsteil fast kaputt ist. */
    private static void renderArmorWarning(DrawContext context, MinecraftClient client, int cx, int cy) {
        String part = null;
        for (EquipmentSlot slot : ARMOR) {
            if (lowArmor(client.player.getEquippedStack(slot))) {
                part = slot.getName();
                break;
            }
        }
        if (part == null && HudLayout.editing) {
            part = "chest";
        }
        if (part == null) {
            return;
        }
        boolean blink = (System.currentTimeMillis() / 400L) % 2L == 0L;
        String text = Text.translatable("arnestyles.hud.armor_low." + part).getString();
        TextRenderer font = client.textRenderer;
        int w = font.getWidth(text) + 6;
        int x = cx - w / 2;
        int y = cy - 40;
        HudLayout.begin(context, HudLayout.Element.ARMOR_WARNING, x, y, w, 12);
        try {
            HudStyle.background(context, x, y, x + w, y + 12, blink ? 0xC0B91C1C : HudStyle.bg());
            HudStyle.text(context, font, text, x + 3, y + 2, blink ? HudStyle.text() : 0xFFFF6B6B);
        } finally {
            HudLayout.end(context);
        }
    }

    /** Gegner-Info: Name, Leben (gelb = Absorption), Ruestung und Haende des Spielers, auf den man schaut oder den man zuletzt traf. */
    private static void renderTarget(DrawContext context, MinecraftClient client, int cx, int cy) {
        LivingEntity target = null;
        Entity looked = client.targetedEntity;
        if (looked instanceof LivingEntity living && living.isAlive()) {
            target = living;
        } else if (lastTarget != null && lastTarget.isAlive() && System.currentTimeMillis() - lastTargetAt < TARGET_MEMORY_MS
            && lastTarget.squaredDistanceTo(client.player) < 24 * 24) {
            target = lastTarget;
        }
        if (target == null && HudLayout.editing) {
            target = client.player;
        }
        if (target == null) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int w = 124;
        int h = 40;
        int x = cx + 16;
        int y = cy + 16;
        HudLayout.begin(context, HudLayout.Element.TARGET, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            String name = font.trimToWidth(target.getDisplayName().getString(), w - 50);
            HudStyle.text(context, font, name, x + 4, y + 4, HudStyle.text());
            float health = target.getHealth();
            float absorption = target.getAbsorptionAmount();
            float max = Math.max(1.0f, target.getMaxHealth());
            String hp = String.format(Locale.ROOT, "%.1f", (health + absorption) / 2.0f) + " ❤";
            context.drawTextWithShadow(font, hp, x + w - 4 - font.getWidth(hp), y + 4, absorption > 0 ? 0xFFFFD84D : 0xFFFF5555);
            int barX = x + 4;
            int barW = w - 8;
            int barY = y + 14;
            context.fill(barX, barY, barX + barW, barY + 3, 0xFF3A0D0D);
            int hw = Math.round(barW * Math.min(1.0f, health / max));
            context.fill(barX, barY, barX + hw, barY + 3, 0xFFE53935);
            if (absorption > 0) {
                int aw = Math.round(barW * Math.min(1.0f, absorption / max));
                context.fill(barX, barY - 1, barX + aw, barY, 0xFFFFD84D);
            }
            // Ruestung und Haende mit Haltbarkeit
            int ix = x + 4;
            int iy = y + 20;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND};
            for (EquipmentSlot slot : slots) {
                ItemStack stack = target.getEquippedStack(slot);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, ix, iy);
                    context.drawStackOverlay(font, stack, ix, iy);
                }
                ix += 19;
            }
        } finally {
            HudLayout.end(context);
        }
    }
}
