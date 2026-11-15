import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Entfernt aus allen Styles Dateien, die genau so in Minecraft vorkommen (sie bewirken nichts).
 * PNGs werden Pixel fuer Pixel verglichen, JSON-Dateien ohne Leerzeichen. Styles, die danach leer sind, werden geloescht.
 *
 * Aufruf: java tools/RemoveVanillaCopies.java <bundled-styles> <vanilla assets/minecraft>
 */
public class RemoveVanillaCopies {
    public static void main(String[] args) throws IOException {
        Path styles = Path.of(args[0]);
        Path vanilla = Path.of(args[1]);
        AtomicInteger removed = new AtomicInteger();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(styles)) {
            files = walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().replace('\\', '/').contains("/assets/minecraft/"))
                .toList();
        }
        files.parallelStream().forEach(file -> {
            String rel = file.toString().replace('\\', '/');
            rel = rel.substring(rel.indexOf("/assets/minecraft/") + "/assets/minecraft/".length());
            if (rel.startsWith("models/arnestyles/") || rel.endsWith(".mcmeta")) {
                return;
            }
            Path original = vanilla.resolve(rel);
            if (!Files.isRegularFile(original)) {
                return;
            }
            try {
                boolean same;
                if (rel.endsWith(".png")) {
                    same = samePixels(file, original) && sameText(sibling(file, ".mcmeta"), sibling(original, ".mcmeta"));
                } else if (rel.endsWith(".json")) {
                    same = sameText(file, original);
                } else {
                    same = Arrays.equals(Files.readAllBytes(file), Files.readAllBytes(original));
                }
                if (same) {
                    Files.delete(file);
                    Files.deleteIfExists(sibling(file, ".mcmeta"));
                    removed.incrementAndGet();
                }
            } catch (IOException | RuntimeException e) {
                // Datei nicht lesbar: lieber behalten
            }
        });
        int emptied = removeEmptyStyles(styles);
        System.out.println(removed.get() + " unveraenderte Dateien entfernt, " + emptied + " leere Styles geloescht");
    }

    private static Path sibling(Path file, String suffix) {
        return file.resolveSibling(file.getFileName() + suffix);
    }

    private static boolean samePixels(Path a, Path b) throws IOException {
        BufferedImage x = ImageIO.read(a.toFile());
        BufferedImage y = ImageIO.read(b.toFile());
        if (x == null || y == null || x.getWidth() != y.getWidth() || x.getHeight() != y.getHeight()) {
            return false;
        }
        int w = x.getWidth();
        int h = x.getHeight();
        int[] px = x.getRGB(0, 0, w, h, null, 0, w);
        int[] py = y.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            // Voll durchsichtige Pixel gelten unabhaengig von ihrer Farbe als gleich.
            if (px[i] != py[i] && !((px[i] >>> 24) == 0 && (py[i] >>> 24) == 0)) {
                return false;
            }
        }
        return true;
    }

    /** Beide fehlen, oder beide gleich (ohne Leerzeichen). */
    private static boolean sameText(Path a, Path b) throws IOException {
        boolean ea = Files.isRegularFile(a);
        boolean eb = Files.isRegularFile(b);
        if (!ea || !eb) {
            return ea == eb;
        }
        return normalize(Files.readString(a, StandardCharsets.UTF_8)).equals(normalize(Files.readString(b, StandardCharsets.UTF_8)));
    }

    private static String normalize(String s) {
        return s.replace("﻿", "").replace("minecraft:", "").replaceAll("\\s+", "");
    }

    /** Loescht leere Ordner und Styles, in denen nur noch style.json / preview.png liegen. */
    private static int removeEmptyStyles(Path styles) throws IOException {
        int count = 0;
        try (Stream<Path> cats = Files.list(styles)) {
            for (Path cat : cats.filter(Files::isDirectory).toList()) {
                try (Stream<Path> list = Files.list(cat)) {
                    for (Path style : list.filter(Files::isDirectory).toList()) {
                        List<Path> content = new ArrayList<>();
                        try (Stream<Path> walk = Files.walk(style)) {
                            walk.filter(Files::isRegularFile).forEach(content::add);
                        }
                        boolean useful = content.stream().anyMatch(p -> {
                            String n = p.getFileName().toString();
                            return !n.equals("style.json") && !(n.equals("preview.png") && p.getParent().equals(style));
                        });
                        if (!useful) {
                            deleteTree(style);
                            count++;
                        } else {
                            deleteEmptyDirs(style);
                        }
                    }
                }
            }
        }
        return count;
    }

    private static void deleteEmptyDirs(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path dir : walk.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList()) {
                try (Stream<Path> l = Files.list(dir)) {
                    if (l.findAny().isEmpty() && !dir.equals(root)) {
                        Files.delete(dir);
                    }
                }
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
