package com.arnestyles.client;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.AppleSkin;
import com.arnestyles.client.mods.ArneShaders;
import com.arnestyles.client.mods.ArmorHud;
import com.arnestyles.client.mods.Comfort;
import com.arnestyles.client.mods.InfoHud;
import com.arnestyles.client.mods.PvpVisuals;
import com.arnestyles.client.mods.PopChams;
import com.arnestyles.client.mods.ShaderPacks;
import com.arnestyles.client.mods.ShieldFix;
import com.arnestyles.client.mods.ShieldStatus;
import com.arnestyles.client.mods.TotemCounter;
import com.arnestyles.client.style.StyleRegistry;
import com.arnestyles.client.style.TintedTextures;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

public class ArneStylesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        // Muss vor dem ersten Laden der Ressourcen passieren, damit aktive Styles sofort greifen.
        // Fehlende Packs der aktiven Styles werden dafuer zuerst aus GitHub geladen.
        com.arnestyles.client.style.CloudStyles.init();
        StyleRegistry.reload();
        ArneKeybinds.register();
        UpdateChecker.register();
        OptionsButton.register();
        PopChams.register();
        ShieldFix.register();
        ShieldStatus.register();
        TotemCounter.register();
        AppleSkin.register();
        InfoHud.register();
        PvpVisuals.register();
        Comfort.register();
        ShaderPacks.ensureDir();
        ArneShaders.writeReadmeIfMissing();
        TintedTextures.registerGrassColors();
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("arnestyles", "armor_hud"), ArmorHud::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("arnestyles", "totem_counter"), TotemCounter::renderHud);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("arnestyles", "info_hud"), InfoHud::render);
        if ("1".equals(System.getenv("ARNESTYLES_SELFTEST")) || SelfTest.SHOWCASE) {
            SelfTest.register();
        }
    }
}
