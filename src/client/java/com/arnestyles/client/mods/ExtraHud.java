package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Kleine Extra-Anzeigen: Uhrzeit mit Datum, Elytra (Haltbarkeit und Tempo), Server-Adresse und Tageszaehler.
 * Die Texte werden einmal pro Tick gebaut, beim Zeichnen wird nur noch gemalt.
 */
public final class ExtraHud {
    /** Standard-Hoehe rechts oben: unter den Effekt-Symbolen (zwei Reihen), damit nichts ueberlappt. */
    private static final int TOP = 56;
    private static final ItemStack ELYTRA_PREVIEW = new ItemStack(Items.ELYTRA);

    private static String clockText = "";
    private static String elytraText = "";
    private static int elytraColor = 0xFFFFFFFF;
    private static ItemStack elytraStack = ItemStack.EMPTY;
    private static String serverText = "";
    private static String dayText = "";

    private static String clockPattern = "";
    private static DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("HH:mm");

    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static boolean hasLast;
    /** Tempo in Bloecken pro Sekunde, leicht geglaettet. */
    private static double speed;

    private ExtraHud() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            hasLast = false;
            return;
        }
        ArneStylesConfig c = ConfigManager.get();
        if (ModSetting.Toggle.CLOCK_HUD.isActive()) {
            clockText = LocalDateTime.now().format(formatter(c, client));
        }
        if (ModSetting.Toggle.ELYTRA_HUD.isActive()) {
            tickElytra(client, player, c);
        } else {
            hasLast = false;
        }
        if (ModSetting.Toggle.SERVER_HUD.isActive()) {
            ServerInfo entry = client.getCurrentServerEntry();
            if (client.isInSingleplayer()) {
                serverText = Text.translatable("arnestyles.hud.server_singleplayer").getString();
            } else if (entry != null) {
                serverText = entry.address;
            } else {
                serverText = "?";
            }
        }
        if (ModSetting.Toggle.DAY_HUD.isActive()) {
            long time = client.world.getTimeOfDay();
            String day = Text.translatable("arnestyles.hud.day_value", time / 24000L).getString();
            dayText = c.dayTime ? day + "  " + WorldVisuals.clock(Math.floorMod(time, 24000L)) : day;
        }
    }

    private static DateTimeFormatter formatter(ArneStylesConfig c, MinecraftClient client) {
        boolean german = client.options.language != null && client.options.language.startsWith("de");
        String time = c.clock12h ? (c.clockSeconds ? "h:mm:ss a" : "h:mm a") : (c.clockSeconds ? "HH:mm:ss" : "HH:mm");
        String date = "";
        if (c.clockDate) {
            date = german ? (c.clockYear ? "dd.MM.yyyy" : "dd.MM.") : (c.clockYear ? "MMM d, yyyy" : "MMM d");
        }
        String pattern = date.isEmpty() ? time : time + "  '·'  " + date;
        if (!pattern.equals(clockPattern)) {
            clockPattern = pattern;
            clockFormat = DateTimeFormatter.ofPattern(pattern, german ? Locale.GERMAN : Locale.ENGLISH);
        }
        return clockFormat;
    }

    private static void tickElytra(MinecraftClient client, ClientPlayerEntity player, ArneStylesConfig c) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (hasLast) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            double now = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
            speed = speed * 0.6 + now * 0.4;
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasLast = true;

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        elytraStack = chest.contains(DataComponentTypes.GLIDER) ? chest : ItemStack.EMPTY;
        if (elytraStack.isEmpty()) {
            elytraText = "";
            return;
        }
        String durability = "";
        int percent = 100;
        if (elytraStack.isDamageable() && elytraStack.getMaxDamage() > 0) {
            int left = elytraStack.getMaxDamage() - elytraStack.getDamage();
            percent = Math.round(left * 100.0f / elytraStack.getMaxDamage());
            durability = percent + "%";
        }
        elytraColor = percent <= 10 ? 0xFFFF5555 : percent <= 25 ? 0xFFFFAA00 : HudStyle.text();
        String speedText = c.elytraKmh
            ? String.format(Locale.ROOT, "%.0f km/h", speed * 3.6)
            : String.format(Locale.ROOT, "%.1f m/s", speed);
        elytraText = durability.isEmpty() ? speedText : durability + "  " + speedText;
    }

    /** Alle Extra-Anzeigen zeichnen (aus dem Info-HUD). */
    public static void render(DrawContext context, MinecraftClient client) {
        TextRenderer font = client.textRenderer;
        int right = context.getScaledWindowWidth() - 4;
        if (ModSetting.Toggle.CLOCK_HUD.isActive()) {
            String text = clockText.isEmpty() ? "12:00" : clockText;
            textBox(context, font, HudLayout.Element.CLOCK, right - font.getWidth(text) - 6, TOP, text, HudStyle.text());
        }
        if (ModSetting.Toggle.SERVER_HUD.isActive()) {
            String text = serverText.isEmpty() ? "play.example.net" : serverText;
            textBox(context, font, HudLayout.Element.SERVER, right - font.getWidth(text) - 6, TOP + 14, text, HudStyle.text());
        }
        if (ModSetting.Toggle.DAY_HUD.isActive()) {
            String text = dayText.isEmpty() ? Text.translatable("arnestyles.hud.day_value", 12).getString() : dayText;
            textBox(context, font, HudLayout.Element.DAY, right - font.getWidth(text) - 6, TOP + 28, text, HudStyle.text());
        }
        if (ModSetting.Toggle.ELYTRA_HUD.isActive()) {
            renderElytra(context, client, font);
        }
    }

    private static void renderElytra(DrawContext context, MinecraftClient client, TextRenderer font) {
        boolean preview = HudLayout.editing && elytraStack.isEmpty();
        if (!preview) {
            if (elytraStack.isEmpty() || elytraText.isEmpty()) {
                return;
            }
            if (ConfigManager.get().elytraOnlyFlying && (client.player == null || !client.player.isGliding())) {
                return;
            }
        }
        ItemStack stack = preview ? ELYTRA_PREVIEW : elytraStack;
        String text = preview ? "100%  32.5 m/s" : elytraText;
        int color = preview ? HudStyle.text() : elytraColor;
        int w = 18 + font.getWidth(text) + 4;
        int h = 18;
        int x = context.getScaledWindowWidth() / 2 - w / 2;
        int y = context.getScaledWindowHeight() - 98; // ueber Actionbar und Item-Name
        HudLayout.begin(context, HudLayout.Element.ELYTRA, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            context.drawItem(stack, x + 1, y + 1);
            HudStyle.text(context, font, text, x + 19, y + 5, color);
        } finally {
            HudLayout.end(context);
        }
    }

    private static void textBox(DrawContext context, TextRenderer font, HudLayout.Element element, int x, int y, String text, int color) {
        int w = font.getWidth(text) + 6;
        int h = 12;
        HudLayout.begin(context, element, x, y, w, h);
        try {
            HudStyle.background(context, x, y, x + w, y + h);
            HudStyle.text(context, font, text, x + 3, y + 2, color);
        } finally {
            HudLayout.end(context);
        }
    }
}
