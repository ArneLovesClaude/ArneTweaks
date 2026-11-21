package com.arnestyles.client;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.gui.StylesScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Registriert den Keybind zum Oeffnen des Styles-GUI.
 * Die Taste ist in den Minecraft-Steuerungseinstellungen frei umbelegbar,
 * Minecraft speichert die Belegung selbst in options.txt.
 */
public final class ArneKeybinds {
    public static KeyBinding OPEN_GUI;
    public static KeyBinding ZOOM;
    public static KeyBinding FREELOOK;
    public static KeyBinding NEXT_PRESET;
    public static KeyBinding PAINT_BLOCK;
    public static KeyBinding SET_WAYPOINT;
    public static KeyBinding SPOTIFY_TOGGLE;
    public static KeyBinding SPOTIFY_NEXT;
    public static KeyBinding SPOTIFY_PREVIOUS;

    private ArneKeybinds() {
    }

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(ArneStyles.MOD_ID, "main"));

        OPEN_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arnestyles.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            category
        ));

        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        FREELOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, category));
        NEXT_PRESET = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.next_preset", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        SET_WAYPOINT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.set_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        PAINT_BLOCK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.paint_block", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, category));
        // Spotify steuern: standardmaessig frei, damit nichts aus Versehen pausiert
        SPOTIFY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.spotify_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        SPOTIFY_NEXT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.spotify_next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        SPOTIFY_PREVIOUS = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arnestyles.spotify_previous", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.wasPressed()) {
                client.setScreen(new StylesScreen());
            }
            while (SPOTIFY_TOGGLE.wasPressed()) {
                com.arnestyles.client.mods.SpotifyHud.control(com.arnestyles.client.mods.WinMedia.TOGGLE);
            }
            while (SPOTIFY_NEXT.wasPressed()) {
                com.arnestyles.client.mods.SpotifyHud.control(com.arnestyles.client.mods.WinMedia.NEXT);
            }
            while (SPOTIFY_PREVIOUS.wasPressed()) {
                com.arnestyles.client.mods.SpotifyHud.control(com.arnestyles.client.mods.WinMedia.PREVIOUS);
            }
            while (PAINT_BLOCK.wasPressed()) {
                if (client.currentScreen == null) {
                    BlockPainter.paintTarget(client);
                }
            }
        });
    }
}