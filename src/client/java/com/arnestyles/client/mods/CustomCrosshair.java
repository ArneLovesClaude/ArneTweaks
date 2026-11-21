package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Eigenes Fadenkreuz: ein 15x15-Raster aus dem Fadenkreuz-Editor, in eigener Farbe, Deckkraft und Groesse,
 * optional mit dunklem Rand. Die Pixel werden als waagrechte Streifen zwischengespeichert (pro Bild nur fill-Aufrufe).
 */
public final class CustomCrosshair {
    public static final int SIZE = 15;
    public static final String[] PRESETS = {"cross", "gap", "dot", "circle", "x", "t"};

    /** Zwischengespeicherte Streifen: je {Zeile, Startspalte, Endspalte exklusiv}. */
    private static @Nullable List<String> cachedFor;
    private static int[][] runs = new int[0][];
    private static boolean[][] mask = new boolean[SIZE][SIZE];

    private CustomCrosshair() {
    }

    public static boolean active() {
        return ModSetting.Toggle.CUSTOM_CROSSHAIR.isActive();
    }

    /** Fertige Vorlage als 15 Zeilen ('#' = an, '.' = aus). */
    public static List<String> preset(String name) {
        boolean[][] g = new boolean[SIZE][SIZE];
        int c = SIZE / 2;
        switch (name) {
            case "dot" -> {
                g[c][c] = true;
                g[c - 1][c] = g[c + 1][c] = g[c][c - 1] = g[c][c + 1] = true;
            }
            case "gap" -> {
                for (int i = 0; i < SIZE; i++) {
                    if (Math.abs(i - c) >= 2) {
                        g[c][i] = true;
                        g[i][c] = true;
                    }
                }
                g[c][c] = true;
            }
            case "circle" -> {
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        double d = Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
                        g[y][x] = Math.abs(d - 5.0) < 0.6;
                    }
                }
                g[c][c] = true;
            }
            case "x" -> {
                for (int i = 2; i < SIZE - 2; i++) {
                    g[i][i] = true;
                    g[i][SIZE - 1 - i] = true;
                }
            }
            case "t" -> {
                for (int i = 0; i < SIZE; i++) {
                    if (Math.abs(i - c) >= 2) {
                        g[c][i] = true;
                    }
                }
                for (int y = c + 2; y < SIZE; y++) {
                    g[y][c] = true;
                }
                g[c][c] = true;
            }
            default -> {
                for (int i = 0; i < SIZE; i++) {
                    g[c][i] = true;
                    g[i][c] = true;
                }
            }
        }
        return toRows(g);
    }

    public static List<String> toRows(boolean[][] g) {
        List<String> rows = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            StringBuilder b = new StringBuilder(SIZE);
            for (int x = 0; x < SIZE; x++) {
                b.append(g[y][x] ? '#' : '.');
            }
            rows.add(b.toString());
        }
        return rows;
    }

    /** Immer genau 15 Zeilen mit 15 Zeichen aus '#' und '.'. */
    public static List<String> sanitize(@Nullable List<String> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>(preset("cross"));
        }
        List<String> out = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            String row = y < rows.size() && rows.get(y) != null ? rows.get(y) : "";
            StringBuilder b = new StringBuilder(SIZE);
            for (int x = 0; x < SIZE; x++) {
                b.append(x < row.length() && row.charAt(x) == '#' ? '#' : '.');
            }
            out.add(b.toString());
        }
        return out;
    }

    public static boolean[][] grid(List<String> rows) {
        boolean[][] g = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE && y < rows.size(); y++) {
            String row = rows.get(y);
            for (int x = 0; x < SIZE && x < row.length(); x++) {
                g[y][x] = row.charAt(x) == '#';
            }
        }
        return g;
    }

    private static void rebuild(List<String> rows) {
        if (rows == cachedFor) {
            return;
        }
        cachedFor = rows;
        mask = grid(rows);
        List<int[]> list = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            int x = 0;
            while (x < SIZE) {
                if (!mask[y][x]) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < SIZE && mask[y][x]) {
                    x++;
                }
                list.add(new int[] {y, start, x});
            }
        }
        runs = list.toArray(new int[0][]);
    }

    /** Neu aufbauen, wenn der Editor die Liste geaendert hat (gleiches Objekt, anderer Inhalt). */
    public static void invalidate() {
        cachedFor = null;
    }

    /**
     * Zeichnet das Fadenkreuz zentriert um (cx, cy) in GUI-Pixeln. rgb: Farbe ohne Alpha.
     * scale: Groesse eines Rasterpixels in GUI-Pixeln.
     */
    public static void draw(DrawContext context, float cx, float cy, int rgb, float scale) {
        ArneStylesConfig config = ConfigManager.get();
        rebuild(config.crosshairPixels);
        int alpha = Math.round(255 * Math.max(0.0f, Math.min(1.0f, config.crosshairOpacity)));
        int color = (alpha << 24) | (rgb & 0xFFFFFF);
        var m = context.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        int o = SIZE / 2;
        if (config.crosshairOutline) {
            int outline = (alpha << 24) | (config.crosshairOutlineRgb & 0xFFFFFF);
            for (int[] r : runs) {
                context.fill(r[1] - o - 1, r[0] - o - 1, r[2] - o + 1, r[0] - o + 2, outline);
            }
        }
        for (int[] r : runs) {
            context.fill(r[1] - o, r[0] - o, r[2] - o, r[0] - o + 1, color);
        }
        m.popMatrix();
    }

    /** Fuer den Editor: Pixel umschalten. */
    public static void set(List<String> rows, int x, int y, boolean on) {
        if (y < 0 || y >= rows.size() || x < 0 || x >= SIZE) {
            return;
        }
        char[] chars = rows.get(y).toCharArray();
        chars[x] = on ? '#' : '.';
        rows.set(y, new String(chars));
        invalidate();
    }

    public static List<String> empty() {
        return new ArrayList<>(Collections.nCopies(SIZE, ".".repeat(SIZE)));
    }
}
