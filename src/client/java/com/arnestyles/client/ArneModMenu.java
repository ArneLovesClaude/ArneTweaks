package com.arnestyles.client;

import com.arnestyles.client.gui.StylesScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu: der Knopf "Einstellungen" bei ArneTweaks oeffnet das Styles-Menue. */
public class ArneModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return StylesScreen::new;
    }
}
