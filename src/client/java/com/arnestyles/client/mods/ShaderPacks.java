package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.style.StyleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Shader-Uebersicht: Shader-Packs aus config/arnestyles/styles/shaders auflisten und mit Iris laden.
 * Iris bleibt eine eigene Mod (LGPL-3.0), ArneTweaks enthaelt keinen Iris-Code. Es ruft nur die
 * oeffentlichen Methoden auf, die auch das Shader-Menue von Iris benutzt (ueber Reflection,
 * damit ArneTweaks auch ohne Iris startet). Iris setzt den Pack-Namen mit Path.resolve() zusammen,
 * deshalb funktioniert ein voller Pfad in diesen Ordner wie ein Name aus dem normalen shaderpacks-Ordner.
 */
public final class ShaderPacks {
    public static final String IRIS_MOD = "iris";
    private static final String IRIS_CLASS = "net.irisshaders.iris.Iris";

    /** Letzter Fehler beim Laden, wird auf der Shader-Seite angezeigt. */
    private static @Nullable String lastError;

    private ShaderPacks() {
    }

    public static Path dir() {
        return StyleRegistry.stylesRoot().resolve("shaders");
    }

    public static boolean irisLoaded() {
        return FabricLoader.getInstance().isModLoaded(IRIS_MOD);
    }

    public static @Nullable String lastError() {
        return lastError;
    }

    /** Ordner anlegen, damit man ihn ueber "Ordner oeffnen" findet. */
    public static void ensureDir() {
        try {
            Files.createDirectories(dir());
        } catch (IOException e) {
            ArneStyles.LOGGER.error("Shader-Ordner {} konnte nicht angelegt werden.", dir(), e);
        }
    }

    /** Namen der Packs im Ordner: .zip-Dateien und Ordner mit einem Unterordner "shaders" (wie bei Iris). */
    public static List<String> list() {
        List<String> names = new ArrayList<>();
        Path dir = dir();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(ShaderPacks::isPack)
                .map(path -> path.getFileName().toString())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(names::add);
        } catch (IOException e) {
            ArneStyles.LOGGER.warn("Shader-Ordner {} konnte nicht gelesen werden.", dir, e);
        }
        return names;
    }

    private static boolean isPack(Path path) {
        if (Files.isDirectory(path)) {
            return Files.isDirectory(path.resolve("shaders"));
        }
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
    }

    /** Name des Packs aus unserem Ordner, den Iris gerade geladen hat, sonst null. */
    public static @Nullable String current() {
        if (!irisLoaded()) {
            return null;
        }
        try {
            Object config = config();
            Optional<?> name = (Optional<?>) config.getClass().getMethod("getShaderPackName").invoke(config);
            if (name.isEmpty()) {
                return null;
            }
            Path path = Path.of(name.get().toString());
            if (path.isAbsolute() && dir().toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize().getParent())) {
                return path.getFileName().toString();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ArneStyles.LOGGER.debug("Iris-Einstellungen konnten nicht gelesen werden.", e);
        }
        return null;
    }

    /** Shader an (in Iris eingeschaltet) und ein Pack aus unserem Ordner ausgewaehlt. */
    public static boolean enabled() {
        if (current() == null) {
            return false;
        }
        try {
            Object config = config();
            return (Boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** Pack aus dem Ordner laden und Shader einschalten. Muss im Render-Thread laufen (Klick im Menue). */
    public static void select(String name) {
        apply(dir().resolve(name).toAbsolutePath().normalize().toString(), true);
    }

    /** Shader ausschalten. Die Auswahl bleibt gespeichert. */
    public static void disable() {
        String current = current();
        apply(current == null ? null : dir().resolve(current).toAbsolutePath().normalize().toString(), false);
    }

    private static void apply(@Nullable String packPath, boolean enabled) {
        if (!irisLoaded()) {
            return;
        }
        lastError = null;
        try {
            Object config = config();
            Class<?> configClass = config.getClass();
            if (packPath != null) {
                configClass.getMethod("setShaderPackName", String.class).invoke(config, packPath);
            }
            configClass.getMethod("setShadersEnabled", boolean.class).invoke(config, enabled);
            // Wie im Shader-Menue von Iris: erst speichern, dann neu laden (reload liest die Datei neu ein)
            configClass.getMethod("save").invoke(config);
            irisClass().getMethod("reload").invoke(null);
            // Iris faengt Ladefehler selbst ab und zeichnet dann ohne Shader weiter
            if (enabled && (Boolean) irisClass().getMethod("isFallback").invoke(null)) {
                Optional<?> stored = (Optional<?>) irisClass().getMethod("getStoredError").invoke(null);
                lastError = stored.map(error -> ((Throwable) error).getMessage()).map(String::valueOf)
                    .orElse("Iris konnte den Pack nicht laden (Details im Log)");
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            lastError = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            ArneStyles.LOGGER.error("Shader-Pack {} konnte nicht geladen werden.", packPath, cause);
        } catch (ReflectiveOperationException | RuntimeException e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            ArneStyles.LOGGER.error("Iris konnte nicht angesprochen werden.", e);
        }
    }

    private static Class<?> irisClass() throws ClassNotFoundException {
        return Class.forName(IRIS_CLASS);
    }

    private static Object config() throws ReflectiveOperationException {
        Method getter = irisClass().getMethod("getIrisConfig");
        return getter.invoke(null);
    }
}
