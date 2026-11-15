package com.arnestyles.client.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Armor HUD: deine Ruestung in Hotbar-Feldern neben der Hotbar, mit Haltbarkeits-Balken.
 * Bei fast kaputter Ruestung blinkt ein rotes Ausrufezeichen. Optional Haltbarkeit in Prozent.
 *
 * Nach "Armor HUD" von BerdinskiyBear / uku3lig (MIT-Lizenz, github.com/uku3lig/armor-hud),
 * fuer ArneTweaks neu geschrieben, ohne ukulib.
 */
public final class ArmorHud {
    private static final Identifier HOTBAR = Identifier.ofVanilla("hud/hotbar");
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int SLOT = 20;
    private static final int HEIGHT = 22;
    private static final int GAP = 6;

    private ArmorHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!ModSetting.Toggle.ARMOR_HUD.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        if (items.isEmpty() && HudLayout.editing) {
            // HUD-Editor: Beispiel-Ruestung, damit man die Anzeige auch ohne Ruestung verschieben kann
            items.add(new ItemStack(Items.DIAMOND_HELMET));
            items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            items.add(new ItemStack(Items.DIAMOND_BOOTS));
        }
        if (items.isEmpty()) {
            return;
        }

        int width = SLOT * items.size() + 2;
        int center = context.getScaledWindowWidth() / 2;
        int y = context.getScaledWindowHeight() - HEIGHT;
        boolean offhand = !player.getOffHandStack().isEmpty();
        int x;
        if (ModSetting.Toggle.ARMOR_HUD_RIGHT.isActive()) {
            int edge = center + 91;
            if (offhand && player.getMainArm() == Arm.LEFT) {
                edge += 29;
            }
            if (client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
                edge += 24;
            }
            x = edge + GAP;
        } else {
            int edge = center - 91;
            if (offhand && player.getMainArm() == Arm.RIGHT) {
                edge -= 29;
            }
            x = edge - GAP - width;
        }

        // HUD-Editor: eigene Position und Groesse (Prozent-Text steht ueber der Anzeige, deshalb Hoehe mit Text)
        TextRenderer textFont = client.textRenderer;
        boolean showPercent = ModSetting.Toggle.ARMOR_HUD_PERCENT.isActive();
        int top = showPercent ? y - textFont.fontHeight : y;
        HudLayout.begin(context, HudLayout.Element.ARMOR, x, top, width, y + HEIGHT - top);
        try {
            drawArmor(context, client, items, x, y, width);
        } finally {
            HudLayout.end(context);
        }
    }

    private static void drawArmor(DrawContext context, MinecraftClient client, List<ItemStack> items, int x, int y, int width) {
        // Hintergrund aus der Hotbar-Grafik: linker Teil plus rechter Rand
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, HEIGHT, 0, 0, x, y, width - 3, HEIGHT);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, HEIGHT, 182 - 3, 0, x + width - 3, y, 3, HEIGHT);

        TextRenderer font = client.textRenderer;
        boolean percent = ModSetting.Toggle.ARMOR_HUD_PERCENT.isActive();
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int sx = x + 3 + i * SLOT;
            int sy = y + 3;
            context.drawItem(stack, sx, sy);
            context.drawStackOverlay(font, stack, sx, sy);
            if (!stack.isDamageable()) {
                continue;
            }
            int max = stack.getMaxDamage();
            int left = max - stack.getDamage();
            if (percent) {
                String text = Math.round(left * 100.0f / max) + "%";
                context.drawCenteredTextWithShadow(font, text, sx + 8, y - font.fontHeight, 0xFF000000 | stack.getItemBarColor());
            }
            if ((left <= 5 || left <= max * 0.05f) && blink) {
                context.drawTextWithShadow(font, "!", sx + 12, sy - 2, 0xFFFF4040);
            }
        }
    }
}
