package com.arnestyles.client;

import com.arnestyles.client.gui.StylesScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Knopf "ArneTweaks" oben links in den Optionen und in der Texture-Pack-Auswahl. */
public final class OptionsButton {
    private OptionsButton() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof OptionsScreen || screen instanceof PackScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.translatable("arnestyles.gui.title"),
                        button -> client.setScreen(new StylesScreen(screen)))
                    .dimensions(6, 6, 84, 20)
                    .build());
            }
        });
    }
}
