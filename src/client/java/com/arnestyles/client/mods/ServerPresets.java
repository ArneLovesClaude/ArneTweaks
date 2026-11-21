package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.PresetManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Auto-Preset: ein Preset fest an einen Server binden. Beim Betreten genau dieses Servers wird es geladen
 * (z. B. Crystal-Setup auf dem Crystal-Server). Ist es schon aktiv, passiert nichts (kein unnoetiges Neuladen).
 */
public final class ServerPresets {
    private ServerPresets() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> onJoin(client)));
    }

    /** Adresse des aktuellen Servers (klein geschrieben) oder null in Einzelspieler. */
    public static @Nullable String currentServer() {
        ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
        if (info == null || info.address == null || info.address.isBlank() || MinecraftClient.getInstance().isInSingleplayer()) {
            return null;
        }
        return info.address.trim().toLowerCase(Locale.ROOT);
    }

    public static void toggle(String server, String preset) {
        var map = ConfigManager.get().serverPresets;
        if (preset.equals(map.get(server))) {
            map.remove(server);
        } else {
            map.put(server, preset);
        }
        ConfigManager.save();
    }

    private static void onJoin(MinecraftClient client) {
        String server = currentServer();
        if (server == null) {
            return;
        }
        String name = ConfigManager.get().serverPresets.get(server);
        if (name == null || name.equals(ConfigManager.get().lastPreset)) {
            return;
        }
        for (PresetManager.Preset preset : PresetManager.list()) {
            if (preset.name().equals(name) && PresetManager.load(preset)) {
                ConfigManager.get().lastPreset = name;
                ConfigManager.save();
                ArneStyles.LOGGER.info("Auto-Preset {} fuer {} geladen.", name, server);
                client.reloadResources();
                client.inGameHud.getChatHud().addMessage(Text.literal("[ArneTweaks] ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.translatable("arnestyles.preset.auto_loaded", name).formatted(Formatting.GRAY)));
                return;
            }
        }
    }
}
