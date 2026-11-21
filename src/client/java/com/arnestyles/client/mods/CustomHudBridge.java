package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verbindung zu CustomHud (Minenash, MIT), das in ArneTweaks mitgeliefert wird.
 * Nur per Reflection, damit ArneTweaks auch ohne CustomHud startet.
 */
public final class CustomHudBridge {
    public static final String MOD_ID = "custom_hud";

    private CustomHudBridge() {
    }

    public static boolean loaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** Oeffnet den Profil-Editor von CustomHud. false, wenn das nicht geht. */
    public static boolean openEditor(Screen parent) {
        if (!loaded()) {
            return false;
        }
        try {
            Screen screen = (Screen) Class.forName("com.minenash.customhud.gui.NewConfigScreen")
                .getConstructor(Screen.class).newInstance(parent);
            MinecraftClient.getInstance().setScreen(screen);
            return true;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            ArneStyles.LOGGER.warn("CustomHud-Editor konnte nicht geoeffnet werden", e);
            return false;
        }
    }

    /** Zeigt CustomHud gerade etwas an? */
    public static boolean enabled() {
        try {
            return loaded() && Class.forName("com.minenash.customhud.ProfileManager").getField("enabled").getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    /** CustomHud an- oder ausschalten und in dessen Config speichern. */
    public static void setEnabled(boolean on) {
        if (!loaded()) {
            return;
        }
        try {
            Class.forName("com.minenash.customhud.ProfileManager").getField("enabled").setBoolean(null, on);
            Class.forName("com.minenash.customhud.ConfigManager").getMethod("save").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            ArneStyles.LOGGER.warn("CustomHud konnte nicht umgeschaltet werden", e);
        }
    }

    /**
     * Einmalig beim ersten Start mit CustomHud: Beispiel-Profil nicht sofort anzeigen (aus, im Menue einschaltbar)
     * und dessen Taste B freigeben, die ArneTweaks schon fuer "Block bemalen" nutzt.
     */
    public static void firstSetup(MinecraftClient client) {
        com.arnestyles.client.config.ArneStylesConfig config = com.arnestyles.client.config.ConfigManager.get();
        if (config.customHudSetupDone || !loaded()) {
            return;
        }
        setEnabled(false);
        for (net.minecraft.client.option.KeyBinding key : client.options.allKeys) {
            if ("key.custom_hud.show_errors".equals(key.getId()) && key.getBoundKeyTranslationKey().equals("key.keyboard.b")) {
                key.setBoundKey(net.minecraft.client.util.InputUtil.UNKNOWN_KEY);
            }
        }
        net.minecraft.client.option.KeyBinding.updateKeysByCode();
        client.options.write();
        config.customHudSetupDone = true;
        com.arnestyles.client.config.ConfigManager.save();
    }

    /** Ordner mit den CustomHud-Profilen (Textdateien). */
    public static Path profileFolder() {
        try {
            Object folder = Class.forName("com.minenash.customhud.CustomHud").getField("PROFILE_FOLDER").get(null);
            if (folder instanceof Path path) {
                return path;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            // Standard-Ordner unten
        }
        return FabricLoader.getInstance().getConfigDir().resolve("custom-hud").resolve("profiles");
    }

    public static void openProfileFolder() {
        Path folder = profileFolder();
        try {
            Files.createDirectories(folder);
        } catch (java.io.IOException e) {
            ArneStyles.LOGGER.warn("CustomHud-Ordner fehlt: {}", folder);
        }
        Util.getOperatingSystem().open(folder);
    }
}
