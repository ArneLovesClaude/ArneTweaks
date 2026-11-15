package com.arnestyles.client;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.essentials.Essential;
import com.arnestyles.client.gui.GuiTheme;
import com.arnestyles.client.gui.HudEditorScreen;
import com.arnestyles.client.gui.StylesScreen;
import com.arnestyles.client.mods.HudLayout;
import com.arnestyles.client.mods.ModSetting;
import com.arnestyles.client.mods.PvpVisuals;
import com.arnestyles.client.mods.ArneShaders;
import com.arnestyles.client.style.PackImporter;
import com.arnestyles.client.style.TintedTextures;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.rule.GameRules;

import java.util.Locale;

/**
 * Nur fuer die Entwicklung: wird mit der Umgebungsvariable ARNESTYLES_SELFTEST=1 aktiviert.
 * Oeffnet das Menue fuer alle Kategorien und speichert je einen Screenshot.
 * Danach startet es eine Testwelt, gibt Ruestung, Schild, Totems und Effekte und macht Screenshots vom HUD
 * und vom Inventar. Am Ende beendet es das Spiel.
 */
final class SelfTest {
    /**
     * Showcase-Modus (ARNESTYLES_SHOWCASE=1): gleicher Ablauf, aber ohne Chat und Toasts im Bild,
     * Dateien heissen arnestyles-showcase-*.png. Fuer schoene Bilder mit
     * {@code runClient --args="--width 1920 --height 1080 --username Arne"} starten.
     */
    static final boolean SHOWCASE = "1".equals(System.getenv("ARNESTYLES_SHOWCASE"));
    private static final StyleCategory[] CATEGORIES = StyleCategory.values();
    private static final int WAIT_TICKS = 40;
    private static final String WORLD = "ArneTweaksTest";
    private static final String[] COMMANDS = {
        "time set day",
        "item replace entity @s armor.head with diamond_helmet",
        "item replace entity @s armor.chest with netherite_chestplate[damage=590]",
        "item replace entity @s armor.legs with diamond_leggings",
        "item replace entity @s armor.feet with diamond_boots",
        "item replace entity @s weapon.offhand with totem_of_undying",
        "item replace entity @s hotbar.0 with shield",
        "give @s totem_of_undying 3",
        "give @s end_crystal 64",
        "effect give @s speed 95 1",
        "effect give @s strength 30 0",
        "effect give @s fire_resistance infinite 0",
        "effect give @s darkness 120 0"
    };

    private static int index = -1;
    private static int shaderIndex;
    private static int ticks;
    /** Schritte nach den Menue-Screenshots: 0 = Welt starten, 1 = warten, 2.. = Schritte in der Welt. */
    private static int worldStep = -1;
    private static GuiTheme.@org.jspecify.annotations.Nullable Theme customTheme;

    private SelfTest() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
    }

    private static void tick(MinecraftClient client) {
        if (SHOWCASE) {
            // Befehls-Rueckmeldungen und Popups sofort wieder weg, bevor sie gezeichnet werden
            client.inGameHud.getChatHud().clear(false);
            client.getToastManager().clear();
        }
        if (worldStep >= 0) {
            worldTick(client);
            return;
        }
        if (index < 0) {
            if (client.getOverlay() == null && client.currentScreen instanceof TitleScreen) {
                for (String path : new String[] {
                    "textures/entity/end_crystal/end_crystal.png", "textures/block/obsidian.png",
                    "textures/block/fire_0.png", "textures/gui/container/inventory.png",
                    "models/item/totem_of_undying.json", "models/item/shield.json"
                }) {
                    String pack = client.getResourceManager().getResource(Identifier.ofVanilla(path)).map(Resource::getPackId).orElse("-");
                    ArneStyles.LOGGER.info("Selbsttest: {} kommt aus {}", path, pack);
                }
                index = 0;
                open(client);
            }
            return;
        }
        if (++ticks < WAIT_TICKS) {
            return;
        }
        String[] extra = {"presets", "mods", "themes-halloween", "themes-easter-big", "themes-custom", "import", "import-done", "import-pearl", "options", "layout", "tpl-fire", "tpl-minecraft", "tpl-compact", "tpl-mint", "keybinds"};
        screenshot(client, index < CATEGORIES.length ? CATEGORIES[index].folderName() : extra[index - CATEGORIES.length]);
        index++;
        ticks = 0;
        int n = CATEGORIES.length;
        if (index < n) {
            open(client);
        } else if (index == n) {
            client.setScreen(StylesScreen.onPresets());
        } else if (index == n + 1) {
            StylesScreen.setAllModGroupsOpen(false);
            StylesScreen.openModGroup("pvp");
            client.setScreen(StylesScreen.onMods());
        } else if (index == n + 2) {
            GuiTheme.select("halloween");
            client.setScreen(StylesScreen.onThemes());
        } else if (index == n + 3) {
            // Ostern (helles Design) und groesseres Menue
            GuiTheme.select("easter");
            ConfigManager.get().guiScale = 1.3f;
            client.setScreen(StylesScreen.onThemes());
        } else if (index == n + 4) {
            // Eigenes Design: Kopie von Standard mit rotem Akzent und blauer Kopfzeile, Menue kleiner
            ConfigManager.get().guiScale = 0.8f;
            customTheme = GuiTheme.createFrom(GuiTheme.builtIns().get(0));
            GuiTheme.setColor(customTheme, GuiTheme.Slot.ACCENT, 0xFF3366);
            GuiTheme.setColor(customTheme, GuiTheme.Slot.HEADER_TOP, 0x0E5A8A);
            GuiTheme.save(customTheme);
            client.setScreen(StylesScreen.onThemes());
        } else if (index == n + 5) {
            if (customTheme != null) {
                GuiTheme.delete(customTheme);
            }
            GuiTheme.select(GuiTheme.STANDARD);
            ConfigManager.get().guiScale = 1.0f;
            ConfigManager.save();
            // Pack-Import: alle Packs aus dem Import-Ordner
            for (java.nio.file.Path pack : PackImporter.packs()) {
                PackImporter.enqueue(pack);
            }
            client.setScreen(StylesScreen.onImport());
        } else if (index == n + 6) {
            ticks = -200;
            client.setScreen(StylesScreen.onImport());
        } else if (index == n + 7) {
            // Importierte Perle in der Kategorie Perlen auswaehlen
            ConfigManager.get().setSelectedStyle(StyleCategory.PEARLS, "arne_testpack");
            ConfigManager.get().activeCategory = StyleCategory.PEARLS.name();
            client.setScreen(new StylesScreen());
        } else if (index == n + 8) {
            ConfigManager.get().setSelectedStyle(StyleCategory.PEARLS, ArneStylesConfig.DEFAULT_STYLE);
            ConfigManager.save();
            client.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(new TitleScreen(), client.options));
        } else if (index == n + 9) {
            client.setScreen(StylesScreen.onLayout());
        } else if (index == n + 10) {
            com.arnestyles.client.gui.GuiLayout.apply("fire");
            client.setScreen(StylesScreen.onMods());
        } else if (index == n + 11) {
            com.arnestyles.client.gui.GuiLayout.apply("minecraft");
            // Kleiner, damit die Kacheln ins Testfenster passen
            ConfigManager.get().guiScale = 0.75f;
            client.setScreen(StylesScreen.onMods());
        } else if (index == n + 12) {
            com.arnestyles.client.gui.GuiLayout.apply("compact");
            ConfigManager.get().guiScale = 1.0f;
            ConfigManager.get().activeTab = "";
            ConfigManager.get().activeCategory = StyleCategory.TOTEMS.name();
            client.setScreen(new StylesScreen());
        } else if (index == n + 13) {
            com.arnestyles.client.gui.GuiLayout.apply("mint");
            ConfigManager.get().guiScale = 0.75f;
            StylesScreen.openModGroup("pvp");
            client.setScreen(StylesScreen.onMods());
        } else if (index == n + 14) {
            com.arnestyles.client.gui.GuiLayout.apply("standard");
            ConfigManager.get().guiScale = 1.0f;
            ConfigManager.save();
            com.arnestyles.client.config.KeybindSets.save("PvP Tasten");
            client.setScreen(StylesScreen.onKeybinds());
        } else {
            for (var set : com.arnestyles.client.config.KeybindSets.list()) {
                if (set.name().equals("PvP Tasten")) {
                    com.arnestyles.client.config.KeybindSets.delete(set);
                }
            }
            com.arnestyles.client.gui.GuiLayout.apply("standard");
            client.setScreen(new TitleScreen());
            worldStep = 0;
        }
    }

    private static void worldTick(MinecraftClient client) {
        ticks++;
        switch (worldStep) {
            case 0 -> {
                ArneStyles.LOGGER.info("Selbsttest: starte Testwelt");
                LevelInfo info = new LevelInfo(WORLD, GameMode.SURVIVAL, false, Difficulty.PEACEFUL, true,
                    new GameRules(DataConfiguration.SAFE_MODE.enabledFeatures()), DataConfiguration.SAFE_MODE);
                GeneratorOptions options = new GeneratorOptions("arnetweaks".hashCode(), false, false);
                worldStep = 1;
                ticks = 0;
                client.createIntegratedServerLoader().createAndStart(WORLD + System.currentTimeMillis(), info, options,
                    WorldPresets::createDemoOptions, new TitleScreen());
            }
            case 1 -> {
                if (client.player != null && client.world != null && client.currentScreen == null && ticks > 100) {
                    client.options.pauseOnLostFocus = false;
                    for (String command : COMMANDS) {
                        client.player.networkHandler.sendChatCommand(command);
                    }
                    next();
                } else if (ticks > 2400) {
                    ArneStyles.LOGGER.error("Selbsttest: Testwelt hat nicht geladen");
                    finish(client);
                }
            }
            case 2 -> {
                if (ticks > 80) {
                    client.inGameHud.getChatHud().clear(false);
                    screenshot(client, "world-darkness");
                    ArneStylesConfig config = ConfigManager.get();
                    config.setEnabled(StyleCategory.ESSENTIALS, true);
                    config.setEssentialOn(Essential.NO_DARKNESS.id(), true);
                    next();
                }
            }
            case 3 -> {
                if (ticks > 60) {
                    screenshot(client, "world-hud");
                    ConfigManager.get().inventoryScale = 1.0f;
                    client.setScreen(new InventoryScreen(client.player));
                    next();
                }
            }
            case 4 -> {
                if (ticks > 40) {
                    screenshot(client, "world-inventory");
                    client.setScreen(null);
                    ConfigManager.get().inventoryScale = 0.75f;
                    client.setScreen(new InventoryScreen(client.player));
                    next();
                }
            }
            case 5 -> {
                if (ticks > 40) {
                    screenshot(client, "world-inventory-scaled");
                    client.setScreen(null);
                    // AppleSkin: hungrig machen und Essen in die Hand geben
                    client.player.networkHandler.sendChatCommand("difficulty easy");
                    client.player.networkHandler.sendChatCommand("effect give @s hunger 4 255");
                    client.player.networkHandler.sendChatCommand("item replace entity @s weapon.mainhand with cooked_beef");
                    next();
                }
            }
            case 6 -> {
                // Bild in der hellen Phase des Blinkens nehmen
                if (ticks > 120 && ticks % 16 == 6) {
                    screenshot(client, "world-appleskin");
                    client.player.networkHandler.sendChatCommand("difficulty peaceful");
                    ConfigManager.get().motionBlur = true;
                    next();
                }
            }
            case 7 -> {
                // Kamera drehen, damit der Motion Blur etwas zum Verwischen hat
                client.player.setYaw(client.player.getYaw() + 15.0f);
                if (ticks > 30) {
                    screenshot(client, "world-motionblur-turning");
                    next();
                }
            }
            case 8 -> {
                // Stillstand: die Spur muss verschwunden sein, sonst wird das alte Bild nicht weitergereicht
                if (ticks > 40) {
                    screenshot(client, "world-motionblur-still");
                    ConfigManager.get().motionBlur = false;
                    client.player.networkHandler.sendChatCommand("fill ~-4 ~-1 ~-4 ~4 ~3 ~4 water");
                    next();
                }
            }
            case 9 -> {
                if (ticks > 40) {
                    screenshot(client, "world-water");
                    ConfigManager.get().setEssentialOn(Essential.NO_FOG.id(), true);
                    next();
                }
            }
            case 10 -> {
                if (ticks > 20) {
                    screenshot(client, "world-water-nofog");
                    ConfigManager.get().setEssentialOn(Essential.NO_FOG.id(), false);
                    // Wasser weg (der Spieler sinkt beim Warten etwas ab, deshalb groesser) und nach oben
                    client.player.networkHandler.sendChatCommand("fill ~-6 ~-5 ~-6 ~6 ~5 ~6 air replace water");
                    client.player.networkHandler.sendChatCommand("tp @s ~ ~6 ~");
                    client.player.networkHandler.sendChatCommand("effect give @s slow_falling 60 0");
                    shaderIndex = 0;
                    next();
                }
            }
            case 11 -> {
                // ArneTweaks-Shader ohne Iris: alle eingebauten nacheinander
                if (ticks == 30) {
                    ArneShaders.select("builtin:" + ArneShaders.BUILTINS[shaderIndex]);
                } else if (ticks > 50) {
                    String id = ArneShaders.BUILTINS[shaderIndex];
                    screenshot(client, "shader-" + id);
                    ArneStyles.LOGGER.info("Selbsttest: eingebauter Shader {}, Fehler: {}", id, ArneShaders.lastError());
                    shaderIndex++;
                    ticks = 0;
                    if (shaderIndex >= ArneShaders.BUILTINS.length) {
                        // Dann ein eigener aus dem Ordner (laedt die Ressourcen neu)
                        ArneShaders.select("folder:Tausch");
                        next();
                    }
                }
            }
            case 12 -> {
                if (ticks > 240 && client.getOverlay() == null) {
                    screenshot(client, "world-shader-folder");
                    ArneStyles.LOGGER.info("Selbsttest: eigener Shader {}, Fehler: {}", ArneShaders.selected(), ArneShaders.lastError());
                    client.setScreen(StylesScreen.openShaderPage());
                    next();
                }
            }
            case 13 -> {
                if (ticks > 20) {
                    screenshot(client, "menu-shaders");
                    client.setScreen(null);
                    ArneShaders.select("");
                    // Eigene Farben: Stone rot, Gras blau, Feuer gruen, dann Ressourcen neu laden
                    setTint(StyleCategory.STONE, 0.0f);
                    setTint(StyleCategory.GRASS, 0.62f);
                    setTint(StyleCategory.FIRE, 0.33f);
                    setTint(StyleCategory.CRYSTAL, 0.33f);
                    // Farbe pro Textur: Totem mit gedrehtem Farbton (bunt bleibt bunt)
                    ArneStylesConfig.CategoryTint totem = new ArneStylesConfig.CategoryTint();
                    totem.on = true;
                    totem.mode = TintedTextures.MODE_SHIFT;
                    totem.hue = 0.5f;
                    totem.saturation = 0.5f;
                    totem.brightness = 0.5f;
                    ConfigManager.get().textureTints.put(TintedTextures.textureKey(StyleCategory.TOTEMS, TintedTextures.DEFAULT_KEY,
                        "textures/item/totem_of_undying.png"), totem);
                    ConfigManager.get().oresFollowStone = true;
                    client.player.networkHandler.sendChatCommand("fill ~-3 ~-1 ~-3 ~1 ~-1 ~1 stone");
                    client.player.networkHandler.sendChatCommand("setblock ~-1 ~-1 ~1 diamond_ore");
                    client.player.networkHandler.sendChatCommand("setblock ~0 ~-1 ~1 coal_ore");
                    client.player.networkHandler.sendChatCommand("setblock ~-2 ~-1 ~1 iron_ore");
                    client.player.networkHandler.sendChatCommand("summon end_crystal ~-2 ~ ~4 {ShowBottom:0b}");
                    client.player.networkHandler.sendChatCommand("setblock ~1 ~-1 ~3 netherrack");
                    client.player.networkHandler.sendChatCommand("setblock ~1 ~ ~3 fire");
                    client.reloadResources();
                    next();
                }
            }
            case 14 -> {
                if (ticks > 240 && client.getOverlay() == null) {
                    client.player.setYaw(0.0f);
                    client.player.setPitch(35.0f);
                    next();
                }
            }
            case 15 -> {
                if (ticks > 20) {
                    screenshot(client, "world-tint");
                    ConfigManager.get().activeCategory = StyleCategory.STONE.name();
                    client.setScreen(new StylesScreen());
                    next();
                }
            }
            case 16 -> {
                if (ticks > 30) {
                    screenshot(client, "menu-tint-stone");
                    // Customize-Tab auf Erze mit Farbkreis
                    ConfigManager.get().activeTab = "T:CUSTOMIZE";
                    ConfigManager.get().activeCategory = StyleCategory.ORES.name();
                    client.setScreen(new StylesScreen());
                    next();
                }
            }
            case 17 -> {
                if (ticks > 30) {
                    screenshot(client, "menu-customize");
                    client.setScreen(null);
                    ConfigManager.get().activeTab = "";
                    ConfigManager.get().textureTints.clear();
                    for (StyleCategory category : TintedTextures.CATEGORIES) {
                        TintedTextures.get(category).on = false;
                    }
                    ConfigManager.save();
                    client.reloadResources();
                    next();
                }
            }
            case 18 -> {
                // HUD-Editor: Ruestung verschoben und groesser, Totems kleiner
                if (ticks > 200 && client.getOverlay() == null) {
                    ArneStylesConfig.HudPosition armor = HudLayout.position(HudLayout.Element.ARMOR);
                    armor.x = 60;
                    armor.y = -70;
                    armor.scale = 1.5f;
                    HudLayout.position(HudLayout.Element.TOTEMS).scale = 0.7f;
                    // Info-HUD (2.6): alles an
                    for (ModSetting.Toggle toggle : new ModSetting.Toggle[] {ModSetting.Toggle.KEYSTROKES, ModSetting.Toggle.CPS_HUD,
                        ModSetting.Toggle.FPS_HUD, ModSetting.Toggle.PING_HUD, ModSetting.Toggle.COORDS_HUD, ModSetting.Toggle.COMBO_HUD,
                        ModSetting.Toggle.PEARL_COOLDOWN, ModSetting.Toggle.ARMOR_WARNING, ModSetting.Toggle.TARGET_HUD}) {
                        if (!toggle.value()) {
                            toggle.toggle();
                        }
                    }
                    client.setScreen(new HudEditorScreen(null));
                    next();
                }
            }
            case 19 -> {
                if (ticks == 15 && client.currentScreen instanceof HudEditorScreen editor) {
                    // Wie mit der Maus: in die Keystrokes klicken, 100 Pixel nach rechts ziehen, loslassen
                    var button = new net.minecraft.client.input.MouseInput(0, 0);
                    editor.mouseClicked(new net.minecraft.client.gui.Click(30, 40, button), false);
                    editor.mouseDragged(new net.minecraft.client.gui.Click(130, 40, button), 100, 0);
                    editor.mouseReleased(new net.minecraft.client.gui.Click(130, 40, button));
                    ArneStyles.LOGGER.info("Selbsttest: Keystrokes nach Ziehen bei x = {} (erwartet 100)",
                        HudLayout.position(HudLayout.Element.KEYSTROKES).x);
                }
                if (ticks > 20) {
                    screenshot(client, "hud-editor");
                    HudLayout.reset(HudLayout.Element.KEYSTROKES);
                    client.setScreen(null);
                    HudLayout.reset(HudLayout.Element.ARMOR);
                    HudLayout.reset(HudLayout.Element.TOTEMS);
                    ConfigManager.save();
                    // Pearl werfen (Cooldown), Ruestung fast kaputt
                    client.player.networkHandler.sendChatCommand("item replace entity @s armor.chest with diamond_chestplate[damage=520]");
                    client.player.networkHandler.sendChatCommand("give @s ender_pearl 4");
                    next();
                }
            }
            case 20 -> {
                if (ticks == 20) {
                    client.player.getInventory().setSelectedSlot(findPearl(client));
                    client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
                }
                if (ticks > 26) {
                    screenshot(client, "hud-info");
                    ConfigManager.get().activeTab = "T:CUSTOMIZE";
                    ConfigManager.get().activeCategory = StyleCategory.ANCHOR.name();
                    client.setScreen(new StylesScreen());
                    next();
                }
            }
            case 21 -> {
                if (ticks == 31) {
                    screenshot(client, "customize-anchor");
                    ConfigManager.get().customizeAdvanced = true;
                    client.setScreen(new StylesScreen());
                }
                if (ticks > 60) {
                    screenshot(client, "customize-anchor-advanced");
                    ConfigManager.get().customizeAdvanced = false;
                    client.setScreen(null);
                    ConfigManager.get().activeTab = "";
                    for (ModSetting.Toggle toggle : ModSetting.Toggle.values()) {
                        if (toggle.name().endsWith("_HUD") && toggle != ModSetting.Toggle.ARMOR_HUD && toggle.value()
                            || toggle == ModSetting.Toggle.KEYSTROKES && toggle.value()
                            || toggle == ModSetting.Toggle.PEARL_COOLDOWN && toggle.value()
                            || toggle == ModSetting.Toggle.ARMOR_WARNING && toggle.value()) {
                            toggle.toggle();
                        }
                    }
                    ConfigManager.save();
                    next();
                }
            }
            case 22 -> {
                // PvP-Optik: Treffer-Farbe, kleine Kristalle ohne Huepfen, Spieler-Feuer tiefer, Kill-Effekt
                if (ticks == 1) {
                    var c = ConfigManager.get();
                    c.hitColor = true;
                    c.hitColorRgb = 0x55FFFF;
                    c.crystalSize = 0.6f;
                    c.crystalBounce = false;
                    c.killFirework = true;
                    c.killTotem = true;
                    c.killLightning = false;
                    // Freie Flaeche vor dem Spieler (die Perle hat ihn evtl. in Baeume gebracht)
                    client.player.networkHandler.sendChatCommand("fill ~-5 ~ ~-1 ~5 ~6 ~9 air");
                    client.player.networkHandler.sendChatCommand("fill ~-5 ~-1 ~-1 ~5 ~-1 ~9 stone");
                    client.player.setYaw(0.0f);
                    client.player.setPitch(15.0f);
                    client.player.networkHandler.sendChatCommand("summon pig ~1 ~ ~4 {NoAI:1b,Silent:1b}");
                    client.player.networkHandler.sendChatCommand("summon end_crystal ~-2 ~ ~5 {ShowBottom:0b}");
                }
                if (ticks == 30) {
                    client.player.networkHandler.sendChatCommand("damage @e[type=pig,limit=1,sort=nearest] 1 minecraft:generic");
                    PvpVisuals.killEffect(client, client.world, client.player.getX() - 1, client.player.getY(), client.player.getZ() + 5);
                }
                if (ticks == 33) {
                    screenshot(client, "world-pvp");
                    next();
                }
            }
            case 23 -> {
                var c = ConfigManager.get();
                c.hitColor = false;
                c.crystalSize = 1.0f;
                c.crystalBounce = true;
                c.killTotem = false;
                c.killLightning = true;
                // Optik (2.8): rote dicke Umrandung, Sonnenuntergang mit pinkem Himmel, Items flach am Boden
                c.blockOutline = true;
                c.outlineColorRgb = 0xFF5555;
                c.outlineWidth = 4.0f;
                c.customTimeOn = true;
                c.customTime = 12500.0f;
                c.skyColorOn = true;
                c.skyColorRgb = 0xFF6EC7;
                c.itemPhysics = true;
                client.player.networkHandler.sendChatCommand("kill @e[type=pig]");
                client.player.networkHandler.sendChatCommand("summon item ~0.5 ~ ~2.5 {Item:{id:\"minecraft:diamond_sword\",count:1},PickupDelay:32767}");
                client.player.networkHandler.sendChatCommand("summon item ~-0.7 ~ ~2.2 {Item:{id:\"minecraft:ender_pearl\",count:5},PickupDelay:32767}");
                client.player.networkHandler.sendChatCommand("summon item ~1.5 ~ ~3 {Item:{id:\"minecraft:obsidian\",count:1},PickupDelay:32767}");
                client.player.setPitch(30.0f);
                next();
            }
            case 24 -> {
                if (ticks > 40) {
                    screenshot(client, "world-optik");
                    var c = ConfigManager.get();
                    c.blockOutline = false;
                    c.outlineWidth = 2.0f;
                    c.customTimeOn = false;
                    c.skyColorOn = false;
                    c.itemPhysics = false;
                    ConfigManager.save();
                    // Komfort (2.9): Zoom-Taste druecken
                    client.player.setPitch(5.0f);
                    com.arnestyles.client.ArneKeybinds.ZOOM.setPressed(true);
                    next();
                }
            }
            case 25 -> {
                if (ticks > 30) {
                    screenshot(client, "world-zoom");
                    com.arnestyles.client.ArneKeybinds.ZOOM.setPressed(false);
                    ConfigManager.get().freelook = true;
                    com.arnestyles.client.ArneKeybinds.FREELOOK.setPressed(true);
                    next();
                }
            }
            case 26 -> {
                if (ticks == 3) {
                    // Kamera um 180 Grad drehen: man sieht den Spieler von vorne
                    com.arnestyles.client.mods.Comfort.freelookTurn(1200.0, 0.0);
                }
                if (ticks > 20) {
                    screenshot(client, "world-freelook");
                    com.arnestyles.client.ArneKeybinds.FREELOOK.setPressed(false);
                    ConfigManager.get().freelook = false;
                    // Preset per Code: speichern, als Code exportieren, wieder einlesen
                    com.arnestyles.client.config.PresetManager.save("Codetest");
                    for (var preset : com.arnestyles.client.config.PresetManager.list()) {
                        if (preset.name().equals("Codetest")) {
                            String code = com.arnestyles.client.config.PresetManager.exportCode(preset);
                            String name = com.arnestyles.client.config.PresetManager.importCode(code);
                            ArneStyles.LOGGER.info("Selbsttest: Preset-Code {} Zeichen, eingelesen als {}", code == null ? -1 : code.length(), name);
                            com.arnestyles.client.config.PresetManager.delete(preset);
                        }
                    }
                    for (var preset : com.arnestyles.client.config.PresetManager.list()) {
                        if (preset.name().startsWith("Codetest")) {
                            com.arnestyles.client.config.PresetManager.delete(preset);
                        }
                    }
                    ConfigManager.save();
                    // Combat Hitboxes (2.12): Hitboxen an, Schwein vor den Spieler
                    client.debugHudEntryList.setEntryVisibility(net.minecraft.client.gui.hud.debug.DebugHudEntries.ENTITY_HITBOXES,
                        net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility.ALWAYS_ON);
                    var c = ConfigManager.get();
                    c.hitboxes = true;
                    c.hitboxOutline = true;
                    c.hitboxLookArrow = true;
                    c.hitboxRgb = 0x55FFFF;
                    client.player.setYaw(0.0f);
                    client.player.setPitch(15.0f);
                    client.player.networkHandler.sendChatCommand("summon minecraft:pig ~ ~ ~3 {NoAI:1b}");
                    next();
                }
            }
            case 27 -> {
                if (ticks > 40) {
                    screenshot(client, "world-hitboxes");
                    client.debugHudEntryList.setEntryVisibility(net.minecraft.client.gui.hud.debug.DebugHudEntries.ENTITY_HITBOXES,
                        net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility.NEVER);
                    var c = ConfigManager.get();
                    c.hitboxOutline = false;
                    c.hitboxLookArrow = false;
                    c.hitboxRgb = 0xFFFFFF;
                    // Keybind-Sets: Springen auf K, speichern, als Code teilen, zuruecksetzen, Code laden
                    var jump = client.options.jumpKey;
                    jump.setBoundKey(net.minecraft.client.util.InputUtil.fromTranslationKey("key.keyboard.k"));
                    net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    com.arnestyles.client.config.KeybindSets.save("Keytest");
                    String code = null;
                    for (var set : com.arnestyles.client.config.KeybindSets.list()) {
                        if (set.name().equals("Keytest")) {
                            code = com.arnestyles.client.config.KeybindSets.exportCode(set);
                            com.arnestyles.client.config.KeybindSets.delete(set);
                        }
                    }
                    com.arnestyles.client.config.KeybindSets.resetAll();
                    String afterReset = jump.getBoundKeyTranslationKey();
                    String name = com.arnestyles.client.config.KeybindSets.importCode(code);
                    for (var set : com.arnestyles.client.config.KeybindSets.list()) {
                        if (set.name().equals(name)) {
                            com.arnestyles.client.config.KeybindSets.load(set);
                            com.arnestyles.client.config.KeybindSets.delete(set);
                        }
                    }
                    ArneStyles.LOGGER.info("Selbsttest: Keybind-Code {} Zeichen, nach Reset {}, nach Laden {} (erwartet key.keyboard.k)",
                        code == null ? -1 : code.length(), afterReset, jump.getBoundKeyTranslationKey());
                    com.arnestyles.client.config.KeybindSets.resetAll();
                    ConfigManager.save();
                    finish(client);
                }
            }
            default -> {
            }
        }
    }

    private static void setTint(StyleCategory category, float hue) {
        ArneStylesConfig.CategoryTint tint = TintedTextures.get(category);
        tint.on = true;
        tint.hue = hue;
        tint.saturation = 0.8f;
        tint.brightness = 0.55f;
    }

    private static int findPearl(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(net.minecraft.item.Items.ENDER_PEARL)) {
                return i;
            }
        }
        return 0;
    }

    private static void next() {
        worldStep++;
        ticks = 0;
    }

    private static void finish(MinecraftClient client) {
        ArneStyles.LOGGER.info("Selbsttest fertig.");
        worldStep = 99;
        client.setScreen(null);
        client.scheduleStop();
    }

    private static void screenshot(MinecraftClient client, String name) {
        ScreenshotRecorder.saveScreenshot(client.runDirectory, (SHOWCASE ? "arnestyles-showcase-" : "arnestyles-selftest-") + name + ".png", client.getFramebuffer(), 1,
            message -> ArneStyles.LOGGER.info("Selbsttest: {}", message.getString()));
    }

    private static void open(MinecraftClient client) {
        ticks = 0;
        ConfigManager.get().activeCategory = CATEGORIES[index].name();
        client.setScreen(new StylesScreen());
        ArneStyles.LOGGER.info("Selbsttest: Kategorie {}", CATEGORIES[index].name().toLowerCase(Locale.ROOT));
    }
}
