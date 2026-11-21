package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Totem-Pop anpassen (nur Optik und Klang bei dir): Animations-Stil, Tempo, Partikel-Farbe, Tonhoehe
 * und ein eigener Sound aus config/arnestyles/sounds/totem_pop.ogg.
 */
public final class TotemPop {
    public static final String[] STYLES = {"normal", "no_spin", "center", "center_no_spin"};
    private static final long MAX_SOUND_BYTES = 4L * 1024 * 1024;
    private static float tickRest;

    private TotemPop() {
    }

    private static boolean on() {
        return ConfigManager.get().modEnabled;
    }

    public static int style() {
        return on() ? MathHelper.clamp(Math.round(ConfigManager.get().totemPopStyle), 0, STYLES.length - 1) : 0;
    }

    /** Ohne Drehungen (Stil 1 und 3). */
    public static boolean noSpin() {
        int s = style();
        return s == 1 || s == 3;
    }

    /** Ohne Flug ueber den Bildschirm, bleibt in der Mitte (Stil 2 und 3). */
    public static boolean centered() {
        int s = style();
        return s == 2 || s == 3;
    }

    /** Wie viele Animations-Ticks in diesem Spiel-Tick vergehen (Tempo 0.5x bis 2x). */
    public static int ticksThisTick() {
        float speed = on() ? ConfigManager.get().totemPopSpeed : 1.0f;
        tickRest += speed;
        int whole = (int) tickRest;
        tickRest -= whole;
        return whole;
    }

    public static float pitchFactor() {
        return on() ? ConfigManager.get().totemPopPitch : 1.0f;
    }

    /** Partikel-Farbe als 0xRRGGBB, oder -1 fuer Minecraft (gruen-gelb). 0 = Minecraft, 1 = Regenbogen, 2+ = Palette. */
    public static int particleColor(net.minecraft.util.math.random.Random random) {
        int choice = on() ? Math.round(ConfigManager.get().totemParticleColor) : 0;
        if (choice <= 0) {
            return -1;
        }
        if (choice == 1) {
            return MathHelper.hsvToRgb(random.nextFloat(), 0.85f, 1.0f);
        }
        int[] palette = ModSetting.POP_CHAMS_COLORS;
        int base = palette[Math.min(palette.length - 1, choice - 2)];
        // Leicht schwankende Helligkeit, damit es nicht flach aussieht
        float f = 0.75f + random.nextFloat() * 0.25f;
        int r = Math.round(((base >> 16) & 0xFF) * f);
        int g = Math.round(((base >> 8) & 0xFF) * f);
        int b = Math.round((base & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    public static String particleColorName(int choice) {
        if (choice <= 0) {
            return Text.translatable("arnestyles.totem.color.normal").getString();
        }
        if (choice == 1) {
            return Text.translatable("arnestyles.totem.color.rainbow").getString();
        }
        return Text.translatable("arnestyles.totem.color.n", choice - 1).getString();
    }

    // ---- Eigener Sound ----

    public static Path soundFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve("sounds").resolve("totem_pop.ogg");
    }

    public static boolean hasCustomSound() {
        try {
            return Files.isRegularFile(soundFile()) && Files.size(soundFile()) <= MAX_SOUND_BYTES;
        } catch (IOException e) {
            return false;
        }
    }

    /** Kleines Pack mit dem eigenen Pop-Sound (arnestyles:custom_totem_pop), oder null ohne Datei. */
    public static @Nullable ResourcePack createSoundPack() {
        Path root = FabricLoader.getInstance().getConfigDir().resolve("arnestyles").resolve(".generated-sounds");
        try {
            if (Files.exists(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(p);
                    }
                }
            }
            Files.createDirectories(soundFile().getParent());
            if (!hasCustomSound()) {
                return null;
            }
            Path assets = root.resolve("assets").resolve("arnestyles");
            Files.createDirectories(assets.resolve("sounds"));
            Files.copy(soundFile(), assets.resolve("sounds").resolve("custom_totem_pop.ogg"), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(assets.resolve("sounds.json"),
                "{\"custom_totem_pop\":{\"sounds\":[\"arnestyles:custom_totem_pop\"]}}", StandardCharsets.UTF_8);
            ResourcePackInfo info = new ResourcePackInfo("arnestyles/sounds", Text.literal("ArneTweaks: Sounds"), ResourcePackSource.BUILTIN,
                Optional.empty());
            return new DirectoryResourcePack(info, root);
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Eigener Totem-Sound konnte nicht geladen werden", e);
            return null;
        }
    }
}
