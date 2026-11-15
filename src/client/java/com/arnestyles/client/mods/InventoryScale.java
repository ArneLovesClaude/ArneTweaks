package com.arnestyles.client.mods;

import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.jspecify.annotations.Nullable;

/**
 * Inventory Scale: Inventar, Kisten und andere Container-Fenster groesser oder kleiner,
 * unabhaengig von der GUI-Groesse. Das Fenster bekommt eine passende logische Groesse,
 * wird beim Zeichnen skaliert, und die Maus wird umgerechnet.
 *
 * Idee nach "Inventory Scale" von dwmirlol; fuer ArneTweaks selbst geschrieben.
 */
public final class InventoryScale {
    /** true, waehrend ein skaliertes Fenster gezeichnet wird (fuer die Tooltip-Grenzen). */
    public static boolean drawing;

    private InventoryScale() {
    }

    /** Faktor fuer dieses Fenster, 1 = normal. */
    public static float factor(@Nullable Screen screen) {
        if (!(screen instanceof HandledScreen<?>) || !ConfigManager.get().modEnabled) {
            return 1.0f;
        }
        float value = ConfigManager.get().inventoryScale;
        return Math.abs(value - 1.0f) < 0.001f ? 1.0f : value;
    }

    public static float current() {
        return factor(MinecraftClient.getInstance().currentScreen);
    }
}
