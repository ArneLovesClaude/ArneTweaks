package com.arnestyles.client.mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TotemCounter: zaehlt die Totem-Pops jedes Spielers und zeigt sie im Namensschild ("Name -3").
 * Dazu im HUD, wie viele Totems du selbst noch im Inventar hast.
 * Der Zaehler eines Spielers wird geloescht, wenn er stirbt oder du die Welt wechselst.
 *
 * Nach "TotemCounter" von uku3lig (MIT-Lizenz, github.com/uku3lig/totemcounter),
 * fuer ArneTweaks neu geschrieben, ohne ukulib.
 */
public final class TotemCounter {
    private static final Map<UUID, Integer> POPS = new HashMap<>();
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);
    private static @Nullable Object lastWorld;

    private TotemCounter() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TotemCounter::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world != lastWorld) {
            lastWorld = client.world;
            POPS.clear();
        }
        if (client.world == null || POPS.isEmpty()) {
            return;
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.isDead() || player.getHealth() <= 0.0f) {
                POPS.remove(player.getUuid());
            }
        }
    }

    /** Ein Spieler hat einen Totem gepoppt (Server-Status 35). */
    public static void onTotemPop(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            POPS.merge(player.getUuid(), 1, Integer::sum);
        }
    }

    /** Haengt " -N" an den Namen, wenn der Spieler schon gepoppt hat. */
    public static Text withPops(UUID uuid, Text name) {
        if (!ModSetting.Toggle.TOTEM_COUNTER.isActive()) {
            return name;
        }
        int pops = POPS.getOrDefault(uuid, 0);
        if (pops <= 0) {
            return name;
        }
        int color = popColor(pops);
        return name.copy().append(Text.literal(" -" + pops).styled(style -> style.withColor(color)));
    }

    /** Wenige Pops gruen, viele rot. */
    private static int popColor(int pops) {
        if (pops <= 2) {
            return 0x55FF55;
        }
        if (pops <= 4) {
            return 0x00AA00;
        }
        if (pops <= 6) {
            return 0xFFFF55;
        }
        if (pops <= 8) {
            return 0xFFAA00;
        }
        return 0xFF5555;
    }

    /** Wenige Totems rot, viele gruen. */
    private static int totemColor(int count) {
        if (count <= 2) {
            return 0xFFFF5555;
        }
        if (count <= 4) {
            return 0xFFFFAA00;
        }
        if (count <= 6) {
            return 0xFFFFFF55;
        }
        if (count <= 8) {
            return 0xFF00AA00;
        }
        return 0xFF55FF55;
    }

    private static int countTotems(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** HUD: Totem-Symbol mit der Anzahl eigener Totems ueber der Hotbar. */
    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!ModSetting.Toggle.TOTEM_COUNTER_HUD.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isSpectator()) {
            return;
        }
        int count = countTotems(client.player);
        if (count <= 0 && HudLayout.editing) {
            // HUD-Editor: Beispiel-Anzahl, damit man die Anzeige auch ohne Totems verschieben kann
            count = 3;
        }
        if (count <= 0) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int x = context.getScaledWindowWidth() / 2 - 8;
        int y = context.getScaledWindowHeight() - 38 - font.fontHeight;
        if (client.player.experienceLevel > 0) {
            y -= 6;
        }
        HudLayout.begin(context, HudLayout.Element.TOTEMS, x, y, 18, 18);
        try {
            context.drawItem(TOTEM, x, y);
            String text = String.valueOf(count);
            context.drawTextWithShadow(font, text, x + 17 - font.getWidth(text), y + 9, totemColor(count));
        } finally {
            HudLayout.end(context);
        }
    }
}
