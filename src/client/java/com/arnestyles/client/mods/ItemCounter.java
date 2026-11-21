package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Predicate;

/**
 * Item-Zaehler: wie viele Totems, Perlen, Kristalle usw. man noch im ganzen Inventar hat. Gezaehlt wird einmal pro Tick
 * (nicht pro Bild), gezeichnet als verschiebbare Anzeige neben der Hotbar.
 */
public final class ItemCounter {
    private record Kind(ModSetting.Toggle toggle, ItemStack icon, Predicate<ItemStack> matches) {
    }

    private static final Kind[] KINDS = {
        kind(ModSetting.Toggle.COUNT_TOTEM, Items.TOTEM_OF_UNDYING),
        kind(ModSetting.Toggle.COUNT_PEARL, Items.ENDER_PEARL),
        kind(ModSetting.Toggle.COUNT_CRYSTAL, Items.END_CRYSTAL),
        kind(ModSetting.Toggle.COUNT_OBSIDIAN, Items.OBSIDIAN),
        kind(ModSetting.Toggle.COUNT_ANCHOR, Items.RESPAWN_ANCHOR),
        kind(ModSetting.Toggle.COUNT_GLOWSTONE, Items.GLOWSTONE),
        new Kind(ModSetting.Toggle.COUNT_GAPPLE, new ItemStack(Items.GOLDEN_APPLE),
            s -> s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)),
        kind(ModSetting.Toggle.COUNT_XP, Items.EXPERIENCE_BOTTLE),
        new Kind(ModSetting.Toggle.COUNT_ARROW, new ItemStack(Items.ARROW),
            s -> s.isOf(Items.ARROW) || s.isOf(Items.SPECTRAL_ARROW) || s.isOf(Items.TIPPED_ARROW)),
        kind(ModSetting.Toggle.COUNT_COBWEB, Items.COBWEB),
        kind(ModSetting.Toggle.COUNT_WIND_CHARGE, Items.WIND_CHARGE),
        new Kind(ModSetting.Toggle.COUNT_SPLASH, new ItemStack(Items.SPLASH_POTION),
            s -> s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)),
    };
    private static final int[] COUNTS = new int[KINDS.length];
    private static final int SLOT = 18;

    private ItemCounter() {
    }

    private static Kind kind(ModSetting.Toggle toggle, Item item) {
        return new Kind(toggle, new ItemStack(item), s -> s.isOf(item));
    }

    /** Einmal pro Tick: alle Slots (Hotbar, Inventar, Ruestung, Zweithand) durchzaehlen. */
    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || !ModSetting.Toggle.ITEM_COUNTER.isActive()) {
            return;
        }
        java.util.Arrays.fill(COUNTS, 0);
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (int k = 0; k < KINDS.length; k++) {
                if (KINDS[k].matches.test(stack)) {
                    COUNTS[k] += stack.getCount();
                    break;
                }
            }
        }
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (!ModSetting.Toggle.ITEM_COUNTER.isActive() || client.player == null) {
            return;
        }
        ArneStylesConfig config = ConfigManager.get();
        int shown = 0;
        for (int k = 0; k < KINDS.length; k++) {
            if (visible(k, config)) {
                shown++;
            }
        }
        if (shown == 0) {
            return;
        }
        boolean vertical = config.itemCounterVertical;
        TextRenderer font = client.textRenderer;
        int cellW = vertical ? SLOT + 22 : SLOT + 4;
        int cellH = vertical ? SLOT : SLOT + 9;
        int w = vertical ? cellW : shown * cellW;
        int h = vertical ? shown * cellH : cellH;
        // Standardplatz: rechts neben der Hotbar
        int x = context.getScaledWindowWidth() / 2 + 98;
        int y = context.getScaledWindowHeight() - h - 2;
        int warn = Math.round(config.itemCounterWarn);
        HudLayout.begin(context, HudLayout.Element.ITEM_COUNTER, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            int i = 0;
            for (int k = 0; k < KINDS.length; k++) {
                if (!visible(k, config)) {
                    continue;
                }
                int count = HudLayout.editing && COUNTS[k] == 0 ? 3 + k : COUNTS[k];
                int ix = vertical ? x + 2 : x + 2 + i * cellW;
                int iy = vertical ? y + 1 + i * cellH : y + 1;
                context.drawItem(KINDS[k].icon, ix, iy);
                String text = String.valueOf(count);
                int color = warn > 0 && count <= warn ? 0xFF000000 | config.itemCounterWarnRgb : HudStyle.text();
                if (vertical) {
                    HudStyle.text(context, font, text, ix + 19, iy + 5, color);
                } else {
                    HudStyle.text(context, font, text, ix + (16 - font.getWidth(text)) / 2, iy + 17, color);
                }
                i++;
            }
        } finally {
            HudLayout.end(context);
        }
    }

    private static boolean visible(int k, ArneStylesConfig config) {
        if (!KINDS[k].toggle.value()) {
            return false;
        }
        return !config.itemCounterHideZero || COUNTS[k] > 0 || HudLayout.editing;
    }
}
