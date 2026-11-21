package com.arnestyles.client.mods;

import com.arnestyles.client.ArneKeybinds;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Wegpunkte: Taste (Standard N) oder /arnetweaks waypoint add <name> merkt sich die aktuelle Position.
 * Das HUD zeigt die naechsten drei mit Entfernung und Richtung. Getrennt nach Welt bzw. Server und Dimension.
 * Reine Anzeige, kein Teleport.
 */
public final class Waypoints {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final int MAX = 200;

    private Waypoints() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ArneKeybinds.SET_WAYPOINT.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    add(client, null);
                }
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            ClientCommandManager.literal("arnetweaks").then(ClientCommandManager.literal("waypoint")
                .then(ClientCommandManager.literal("add")
                    .executes(c -> add(MinecraftClient.getInstance(), null))
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(c -> add(MinecraftClient.getInstance(), StringArgumentType.getString(c, "name")))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests((c, builder) -> {
                            here(MinecraftClient.getInstance()).forEach(w -> builder.suggest(w.name));
                            return builder.buildFuture();
                        })
                        .executes(c -> remove(MinecraftClient.getInstance(), StringArgumentType.getString(c, "name")))))
                .then(ClientCommandManager.literal("list").executes(c -> list(MinecraftClient.getInstance())))
                .then(ClientCommandManager.literal("clear").executes(c -> clear(MinecraftClient.getInstance()))))));
    }

    /** Welt (Einzelspieler-Name oder Server-Adresse), damit Wegpunkte nicht in fremden Welten auftauchen. */
    static @Nullable String worldKey(MinecraftClient client) {
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "sp:" + client.getServer().getSaveProperties().getLevelName();
        }
        String server = ServerPresets.currentServer();
        return server == null ? null : "mp:" + server;
    }

    static String dimension(MinecraftClient client) {
        return client.world == null ? "" : client.world.getRegistryKey().getValue().toString();
    }

    /** Wegpunkte dieser Welt und Dimension. */
    static List<ArneStylesConfig.Waypoint> here(MinecraftClient client) {
        String world = worldKey(client);
        String dim = dimension(client);
        List<ArneStylesConfig.Waypoint> result = new ArrayList<>();
        for (ArneStylesConfig.Waypoint w : ConfigManager.get().waypoints) {
            if (w.world.equals(world) && w.dimension.equals(dim)) {
                result.add(w);
            }
        }
        return result;
    }

    private static int add(MinecraftClient client, @Nullable String name) {
        ClientPlayerEntity player = client.player;
        String world = worldKey(client);
        if (player == null || world == null) {
            return 0;
        }
        List<ArneStylesConfig.Waypoint> all = ConfigManager.get().waypoints;
        if (all.size() >= MAX) {
            say(client, Text.translatable("arnestyles.waypoint.full", MAX).formatted(Formatting.RED));
            return 0;
        }
        ArneStylesConfig.Waypoint w = new ArneStylesConfig.Waypoint();
        w.world = world;
        w.dimension = dimension(client);
        w.x = MathHelper.floor(player.getX());
        w.y = MathHelper.floor(player.getY());
        w.z = MathHelper.floor(player.getZ());
        String clean = name == null ? "" : name.replaceAll("[\\p{Cntrl}§]", "").trim();
        w.name = clean.isEmpty() ? Text.translatable("arnestyles.waypoint.default_name", here(client).size() + 1).getString()
            : clean.substring(0, Math.min(32, clean.length()));
        all.add(w);
        ConfigManager.save();
        say(client, Text.translatable("arnestyles.waypoint.added", w.name, w.x, w.y, w.z).formatted(Formatting.GRAY));
        return 1;
    }

    private static int remove(MinecraftClient client, String name) {
        String world = worldKey(client);
        String dim = dimension(client);
        boolean removed = ConfigManager.get().waypoints.removeIf(w -> w.world.equals(world) && w.dimension.equals(dim) && w.name.equalsIgnoreCase(name.trim()));
        ConfigManager.save();
        say(client, Text.translatable(removed ? "arnestyles.waypoint.removed" : "arnestyles.waypoint.not_found", name).formatted(Formatting.GRAY));
        return removed ? 1 : 0;
    }

    private static int list(MinecraftClient client) {
        List<ArneStylesConfig.Waypoint> list = here(client);
        if (list.isEmpty()) {
            say(client, Text.translatable("arnestyles.waypoint.none").formatted(Formatting.GRAY));
        }
        for (ArneStylesConfig.Waypoint w : list) {
            say(client, Text.literal(w.name + ": " + w.x + " / " + w.y + " / " + w.z).formatted(Formatting.GRAY));
        }
        return list.size();
    }

    private static int clear(MinecraftClient client) {
        String world = worldKey(client);
        String dim = dimension(client);
        ConfigManager.get().waypoints.removeIf(w -> w.world.equals(world) && w.dimension.equals(dim));
        ConfigManager.save();
        say(client, Text.translatable("arnestyles.waypoint.cleared").formatted(Formatting.GRAY));
        return 1;
    }

    /** Zeilen fuers HUD: die naechsten drei Wegpunkte mit Entfernung und Pfeil. */
    static List<String> labels(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (!ModSetting.Toggle.WAYPOINTS.isActive() || player == null || client.world == null) {
            return List.of();
        }
        List<ArneStylesConfig.Waypoint> list = new ArrayList<>(here(client));
        list.sort(Comparator.comparingDouble(w -> distance(player, w)));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(Math.round(com.arnestyles.client.config.ConfigManager.get().waypointCount), list.size()); i++) {
            ArneStylesConfig.Waypoint w = list.get(i);
            double dx = w.x + 0.5 - player.getX();
            double dz = w.z + 0.5 - player.getZ();
            double target = Math.toDegrees(Math.atan2(-dx, dz));
            int index = Math.floorMod((int) Math.round(MathHelper.wrapDegrees(target - player.getYaw()) / 45.0), 8);
            out.add(String.format(Locale.ROOT, "◆ %s  %dm %s", w.name, Math.round(distance(player, w)), ARROWS[index]));
        }
        return out;
    }

    private static double distance(ClientPlayerEntity player, ArneStylesConfig.Waypoint w) {
        double dx = w.x + 0.5 - player.getX();
        double dz = w.z + 0.5 - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void say(MinecraftClient client, Text text) {
        client.inGameHud.getChatHud().addMessage(Text.literal("[ArneTweaks] ").formatted(Formatting.LIGHT_PURPLE).append(text));
    }
}
