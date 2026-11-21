package com.arnestyles.client.gui;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.style.StyleDefinition;
import com.arnestyles.client.style.StyleRegistry;
import com.arnestyles.client.style.TintedTextures;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * "Selbst zeichnen": eine Textur Pixel fuer Pixel bemalen (Stift, Radierer, Fuellen, Pipette, Spiegeln,
 * Raster, Rueckgaengig, mehrere Bilder fuer Animationen). Speichern legt einen neuen eigenen Style an:
 * eine Kopie des Ausgangs-Styles mit der gezeichneten Textur, der sofort gewaehlt wird.
 */
public class DrawScreen extends Screen {
    /** Groesste Textur, die sich noch sinnvoll Pixel fuer Pixel bemalen laesst. */
    public static final int MAX_SIZE = 64;
    private static final int UNDO_LIMIT = 60;
    private static final int NAME_MAX = 32;
    /** Ticks pro Bild fuer neue Animationen (Textur war vorher nicht animiert): eine halbe Sekunde. */
    private static final int NEW_FRAME_TIME = 10;
    private static final Gson GSON = new Gson();
    private static final Identifier PREVIEW_ID = Identifier.of(ArneStyles.MOD_ID, "draw_preview");
    private static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFC6C6C6, 0xFF8B8B8B, 0xFF555555, 0xFF2B2B2B, 0xFF000000, 0xFFFF4040, 0xFFFF8A1F, 0xFFFFD23F, 0xFF7CE04A, 0xFF2FBF4F, 0xFF22D3EE,
        0xFF3B9CFF, 0xFF2F55FF, 0xFF8B5CF6, 0xFFD946EF, 0xFFFF6EC7, 0xFF8B5A2B, 0xFFFF5A5A, 0xFF3CE070, 0xFF5CF2FF, 0xFFFFF05A, 0xFFB000B5, 0xFFFFA000
    };

    private enum Tool { PENCIL, ERASER, FILL, PICK }

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    private final StylesScreen parent;
    private boolean backToGame;
    private final StyleCategory category;
    private final StyleDefinition source;
    private final String asset;
    private final int texW;
    private final int texH;
    private final int frameTime;
    private final List<int[]> frames = new ArrayList<>();
    private int frame;
    private final Deque<Snapshot> undo = new ArrayDeque<>();

    private Tool tool = Tool.PENCIL;
    private boolean mirror;
    private boolean grid = true;
    private float hue;
    private float sat;
    private float val = 1.0f;

    private final List<Hit> hits = new ArrayList<>();
    private @Nullable TextFieldWidget nameField;
    private @Nullable NativeImageBackedTexture preview;
    private @Nullable Text message;
    private boolean messageError;

    // Layout (in init/render berechnet)
    private int canvasX;
    private int canvasY;
    private int cell;
    private int svX;
    private int svY;
    private int svW;
    private int svH;
    private int hueY;
    private int hueH;
    /** Was die Maus gerade zieht: 0 nichts, 1 malen, 2 Farbfeld, 3 Farbton. */
    private int dragging;
    private int dragButton;
    private int lastPx = -1;
    private int lastPy = -1;

    private record Snapshot(List<int[]> frames, int frame) {
    }

    private DrawScreen(StylesScreen parent, StyleCategory category, StyleDefinition source, String asset, NativeImage image, int frameTime) {
        super(Text.translatable("arnestyles.draw.title"));
        this.parent = parent;
        this.category = category;
        this.source = source;
        this.asset = asset;
        this.texW = image.getWidth();
        this.texH = image.getWidth();
        this.frameTime = frameTime;
        int count = Math.max(1, image.getHeight() / texW);
        for (int f = 0; f < count; f++) {
            int[] pixels = new int[texW * texH];
            for (int y = 0; y < texH; y++) {
                for (int x = 0; x < texW; x++) {
                    pixels[y * texW + x] = image.getColorArgb(x, f * texH + y);
                }
            }
            frames.add(pixels);
        }
        setColor(0xFFFFFFFF);
    }

    /**
     * Oeffnet den Editor fuer eine Textur eines Styles. Gibt einen Fehlertext zurueck, wenn die Textur
     * sich nicht bemalen laesst (zu gross, nicht quadratisch, nicht lesbar), sonst null.
     */
    public static @Nullable Text open(StylesScreen parent, StyleCategory category, StyleDefinition style, String asset) {
        return open(parent, category, style, asset, false);
    }

    /** backToGame: aus der Welt geoeffnet (Block bemalen), danach direkt zurueck ins Spiel statt ins Menue. */
    public static @Nullable Text open(StylesScreen parent, StyleCategory category, StyleDefinition style, String asset, boolean backToGame) {
        NativeImage image;
        try {
            image = TintedTextures.readImage(TintedTextures.previewRoot(style), asset);
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            return Text.translatable("arnestyles.draw.unreadable");
        }
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            int frameTime = readFrameTime(TintedTextures.previewRoot(style), asset);
            // Hohe Bilder sind nur mit "animation" in der .mcmeta eine Bildfolge, sonst ein einzelnes (nicht quadratisches) Bild
            if (w > MAX_SIZE || h < w || h % w != 0 || (h != w && frameTime < 0)) {
                return Text.translatable("arnestyles.draw.too_big", w, h, MAX_SIZE);
            }
            DrawScreen screen = new DrawScreen(parent, category, style, asset, image, frameTime < 0 ? NEW_FRAME_TIME : frameTime);
            screen.backToGame = backToGame;
            parent.openChild(screen);
            return null;
        } finally {
            image.close();
        }
    }

    /** Ticks pro Bild laut .mcmeta, oder -1 wenn die Textur nicht animiert ist. */
    private static int readFrameTime(@Nullable Path root, String asset) {
        try {
            if (root != null && Files.isRegularFile(root.resolve("assets").resolve("minecraft").resolve(asset))) {
                // Textur kommt aus dem Style: nur dessen .mcmeta zaehlt
                Path meta = root.resolve("assets").resolve("minecraft").resolve(asset + ".mcmeta");
                if (!Files.isRegularFile(meta)) {
                    return -1;
                }
                try (Reader reader = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
                    return frameTimeOf(GSON.fromJson(reader, JsonObject.class));
                }
            }
            var vanilla = net.minecraft.client.MinecraftClient.getInstance().getDefaultResourcePack()
                .open(net.minecraft.resource.ResourceType.CLIENT_RESOURCES, vanillaId(asset + ".mcmeta"));
            if (vanilla != null) {
                try (InputStream in = vanilla.get()) {
                    return frameTimeOf(GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class));
                }
            }
        } catch (IOException | RuntimeException e) {
            // Ohne lesbare Animationsdatei: nicht animiert
        }
        return -1;
    }

    private static int frameTimeOf(@Nullable JsonObject meta) {
        if (meta == null || !meta.has("animation")) {
            return -1;
        }
        JsonObject animation = meta.getAsJsonObject("animation");
        return animation.has("frametime") ? Math.max(1, animation.get("frametime").getAsInt()) : 1;
    }

    // ---- Farben ----

    private static int c(GuiTheme.Slot slot) {
        return GuiTheme.active().argb(slot);
    }

    private int color() {
        return 0xFF000000 | MathHelper.hsvToRgb(hue, sat, val);
    }

    private void setColor(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) {
                h = ((g - b) / d) % 6f;
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
            if (h < 0) {
                h += 1f;
            }
            hue = h;
        }
        sat = max == 0 ? 0 : d / max;
        val = max;
    }

    // ---- Aufbau ----

    @Override
    protected void init() {
        String old = nameField != null ? nameField.getText() : "";
        int[] p = panel();
        int rx = rightX(p);
        int rw = p[2] - 10 - rx;
        nameField = new TextFieldWidget(this.textRenderer, rx, p[3] - 10 - 18 - 4 - 16, rw, 16, Text.translatable("arnestyles.draw.name"));
        nameField.setMaxLength(NAME_MAX);
        nameField.setPlaceholder(Text.translatable("arnestyles.draw.name"));
        nameField.setText(old);
        addDrawableChild(nameField);
        if (preview == null) {
            preview = new NativeImageBackedTexture(() -> "arnestyles draw preview", texW, texH, true);
            this.client.getTextureManager().registerTexture(PREVIEW_ID, preview);
        }
    }

    /** Rahmen des Editors: x1, y1, x2, y2. */
    private int[] panel() {
        int m = this.width > 500 ? 14 : 6;
        return new int[]{m, m, this.width - m, this.height - m};
    }

    private int rightX(int[] p) {
        int contentH = p[3] - p[1] - 32;
        int maxCanvas = Math.min(contentH, (p[2] - p[0] - 30) * 54 / 100);
        cell = Math.max(1, maxCanvas / texW);
        canvasX = p[0] + 10;
        canvasY = p[1] + 24 + Math.max(0, (contentH - cell * texH) / 2);
        return canvasX + cell * texW + 14;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        hits.clear();
        int[] p = panel();
        int accent = c(GuiTheme.Slot.ACCENT);
        int text = c(GuiTheme.Slot.TEXT);
        int muted = c(GuiTheme.Slot.MUTED);
        // Rahmen
        context.fill(p[0] - 1, p[1] - 1, p[2] + 1, p[3] + 1, c(GuiTheme.Slot.EDGE));
        context.fill(p[0], p[1], p[2], p[3], (c(GuiTheme.Slot.PANEL) & 0xFFFFFF) | 0xE6000000);

        // Kopfzeile
        Text head = Text.literal("✎ ").append(Text.translatable("arnestyles.draw.title"));
        int hx = p[0] + 10;
        context.drawTextWithShadow(this.textRenderer, head, hx, p[1] + 8, accent);
        hx += this.textRenderer.getWidth(head);
        String file = asset.substring(asset.lastIndexOf('/') + 1);
        String sub = " — " + Text.translatable("arnestyles.category." + category.folderName()).getString() + "  (" + texW + "x" + texH + ")";
        context.drawTextWithShadow(this.textRenderer, sub, hx, p[1] + 8, text);
        hx += this.textRenderer.getWidth(sub) + 12;
        Text hint = message != null ? message : Text.translatable("arnestyles.draw.hint");
        String hintStr = this.textRenderer.trimToWidth(hint.getString(), Math.max(0, p[2] - 10 - hx));
        context.drawTextWithShadow(this.textRenderer, hintStr, p[2] - 10 - this.textRenderer.getWidth(hintStr), p[1] + 8,
            message == null ? muted : messageError ? c(GuiTheme.Slot.DANGER) : c(GuiTheme.Slot.OK));

        int rx = rightX(p);
        int rw = p[2] - 10 - rx;
        drawCanvas(context, mouseX, mouseY, accent);

        // Werkzeuge
        int y = p[1] + 24;
        int bh = 16;
        int gap = 4;
        int bw4 = (rw - 3 * gap) / 4;
        button(context, rx, y, bw4, bh, Text.translatable("arnestyles.draw.pencil"), tool == Tool.PENCIL, mouseX, mouseY, () -> tool = Tool.PENCIL);
        button(context, rx + (bw4 + gap), y, bw4, bh, Text.translatable("arnestyles.draw.eraser"), tool == Tool.ERASER, mouseX, mouseY, () -> tool = Tool.ERASER);
        button(context, rx + 2 * (bw4 + gap), y, bw4, bh, Text.translatable("arnestyles.draw.fill"), tool == Tool.FILL, mouseX, mouseY, () -> tool = Tool.FILL);
        button(context, rx + 3 * (bw4 + gap), y, rw - 3 * (bw4 + gap), bh, Text.translatable("arnestyles.draw.pick"), tool == Tool.PICK, mouseX, mouseY,
            () -> tool = Tool.PICK);
        y += bh + gap;
        button(context, rx, y, bw4, bh, Text.translatable("arnestyles.draw.mirror"), mirror, mouseX, mouseY, () -> mirror = !mirror);
        button(context, rx + (bw4 + gap), y, bw4, bh, Text.translatable("arnestyles.draw.grid"), grid, mouseX, mouseY, () -> grid = !grid);
        button(context, rx + 2 * (bw4 + gap), y, bw4, bh, Text.translatable("arnestyles.draw.undo"), false, mouseX, mouseY, this::undo);
        button(context, rx + 3 * (bw4 + gap), y, rw - 3 * (bw4 + gap), bh, Text.translatable("arnestyles.draw.clear"), false, mouseX, mouseY, () -> {
            pushUndo();
            java.util.Arrays.fill(frames.get(frame), 0);
        });
        y += bh + gap;
        // Bilder (Animation)
        int arrow = 16;
        int frameW = (rw - 2 * arrow - 4 * gap) / 3 + 10;
        int restW = (rw - 2 * arrow - frameW - 4 * gap) / 2;
        int x = rx;
        button(context, x, y, arrow, bh, Text.literal("<"), false, mouseX, mouseY, () -> frame = (frame - 1 + frames.size()) % frames.size());
        x += arrow + gap;
        button(context, x, y, frameW, bh, Text.translatable("arnestyles.draw.frame", frame + 1, frames.size()), false, mouseX, mouseY, () -> {
        });
        x += frameW + gap;
        button(context, x, y, arrow, bh, Text.literal(">"), false, mouseX, mouseY, () -> frame = (frame + 1) % frames.size());
        x += arrow + gap;
        button(context, x, y, restW, bh, Text.translatable("arnestyles.draw.add"), false, mouseX, mouseY, () -> {
            pushUndo();
            frames.add(frame + 1, frames.get(frame).clone());
            frame++;
        });
        x += restW + gap;
        button(context, x, y, rx + rw - x, bh, Text.translatable("arnestyles.draw.remove"), false, mouseX, mouseY, () -> {
            if (frames.size() > 1) {
                pushUndo();
                frames.remove(frame);
                frame = Math.min(frame, frames.size() - 1);
            }
        });
        y += bh + gap + 4;

        // Aktuelle Farbe und Hex
        int col = color();
        context.fill(rx - 1, y - 1, rx + 13, y + 13, 0xFF000000);
        context.fill(rx, y, rx + 12, y + 12, col);
        context.drawTextWithShadow(this.textRenderer, String.format(Locale.ROOT, "#%06X", col & 0xFFFFFF), rx + 18, y + 2, text);
        y += 16;

        // Platz unten: Name, Speichern/Abbrechen, Vorschau, Palette, Farbton
        int bottom = p[3] - 10;
        int saveY = bottom - 18;
        int nameY = saveY - 4 - 16;
        int sw = Math.max(8, Math.min(16, (rw - 11 * 2) / 12));
        int paletteH = 2 * sw + 2;
        int free = nameY - 6 - y;
        int previewMax = Math.max(16, Math.min(64, (free - paletteH - 10 - 8 - 12 - 30) / 1));
        int previewH = Math.min(previewMax, (rw - 40) * 4 / 7);
        int previewBlock = previewH + 12;
        hueH = 7;
        svH = Math.max(18, free - previewBlock - paletteH - 6 - hueH - 6 - 6);
        svX = rx;
        svY = y;
        svW = rw;
        drawSv(context);
        y += svH + 6;
        hueY = y;
        drawHue(context);
        y += hueH + 6;
        // Palette
        for (int i = 0; i < PALETTE.length; i++) {
            int px = rx + (i % 12) * (sw + 2);
            int py = y + (i / 12) * (sw + 2);
            int pc = PALETTE[i];
            boolean over = inside(mouseX, mouseY, px, py, px + sw, py + sw);
            context.fill(px - 1, py - 1, px + sw + 1, py + sw + 1, over || (pc & 0xFFFFFF) == (col & 0xFFFFFF) ? 0xFFFFFFFF : 0xFF000000);
            context.fill(px, py, px + sw, py + sw, pc);
            hits.add(new Hit(px, py, px + sw, py + sw, () -> setColor(pc)));
        }
        y += paletteH + 6;

        // Vorschau in drei Groessen
        context.drawTextWithShadow(this.textRenderer, Text.translatable("arnestyles.draw.preview"), rx, y, muted);
        int py = y + 11;
        updatePreview();
        int small = Math.max(8, previewH / 4);
        int mid = Math.max(12, previewH / 2);
        int px = rx + this.textRenderer.getWidth(Text.translatable("arnestyles.draw.preview")) + 10;
        int[] sizes = {small, mid, previewH};
        for (int size : sizes) {
            if (px + size > rx + rw) {
                break;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, PREVIEW_ID, px, py + (previewH - size) / 2, 0.0f, 0.0f, size, size, texW, texH, texW, texH,
                0xFFFFFFFF);
            px += size + 14;
        }

        // Name und Knoepfe
        if (nameField != null) {
            nameField.setX(rx);
            nameField.setY(nameY);
            nameField.setWidth(rw);
        }
        int half = (rw - gap) / 2;
        boolean overSave = inside(mouseX, mouseY, rx, saveY, rx + half, saveY + 18);
        context.fill(rx - 1, saveY - 1, rx + half + 1, saveY + 19, overSave ? 0xFFFFFFFF : accent);
        context.fill(rx, saveY, rx + half, saveY + 18, accent);
        Text save = Text.literal("✔ ").append(Text.translatable("arnestyles.draw.save"));
        context.drawTextWithShadow(this.textRenderer, save, rx + (half - this.textRenderer.getWidth(save)) / 2, saveY + 5, 0xFFFFFFFF);
        hits.add(new Hit(rx, saveY, rx + half, saveY + 18, this::save));
        button(context, rx + half + gap, saveY, rw - half - gap, 18, Text.translatable("arnestyles.draw.cancel"), false, mouseX, mouseY, this::close);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawCanvas(DrawContext context, int mouseX, int mouseY, int accent) {
        int w = cell * texW;
        int h = cell * texH;
        context.fill(canvasX - 1, canvasY - 1, canvasX + w + 1, canvasY + h + 1, accent);
        int[] pixels = frames.get(frame);
        for (int y = 0; y < texH; y++) {
            for (int x = 0; x < texW; x++) {
                int x1 = canvasX + x * cell;
                int y1 = canvasY + y * cell;
                context.fill(x1, y1, x1 + cell, y1 + cell, ((x + y) & 1) == 0 ? 0xFF1E2030 : 0xFF24263A);
                int argb = pixels[y * texW + x];
                if ((argb >>> 24) != 0) {
                    context.fill(x1, y1, x1 + cell, y1 + cell, argb);
                }
            }
        }
        if (grid && cell >= 4) {
            for (int i = 1; i < texW; i++) {
                context.fill(canvasX + i * cell, canvasY, canvasX + i * cell + 1, canvasY + h, 0x38FFFFFF);
            }
            for (int i = 1; i < texH; i++) {
                context.fill(canvasX, canvasY + i * cell, canvasX + w, canvasY + i * cell + 1, 0x38FFFFFF);
            }
        }
        if (mirror) {
            int mx = canvasX + w / 2;
            for (int y = canvasY; y < canvasY + h; y += 6) {
                context.fill(mx, y, mx + 1, Math.min(canvasY + h, y + 3), accent);
            }
        }
        int hx = (mouseX - canvasX) / Math.max(1, cell);
        int hy = (mouseY - canvasY) / Math.max(1, cell);
        if (mouseX >= canvasX && mouseY >= canvasY && hx < texW && hy < texH) {
            outline(context, canvasX + hx * cell, canvasY + hy * cell, cell, 0xFFFFFFFF);
            if (mirror && texW - 1 - hx != hx) {
                outline(context, canvasX + (texW - 1 - hx) * cell, canvasY + hy * cell, cell, 0x90FFFFFF);
            }
        }
    }

    private static void outline(DrawContext context, int x, int y, int size, int color) {
        context.fill(x - 1, y - 1, x + size + 1, y, color);
        context.fill(x - 1, y + size, x + size + 1, y + size + 1, color);
        context.fill(x - 1, y, x, y + size, color);
        context.fill(x + size, y, x + size + 1, y + size, color);
    }

    /** Farbfeld: nach rechts mehr Saettigung, nach unten dunkler. */
    private void drawSv(DrawContext context) {
        context.fill(svX - 1, svY - 1, svX + svW + 1, svY + svH + 1, 0xFF000000);
        for (int i = 0; i < svW; i++) {
            float s = svW <= 1 ? 1 : i / (float) (svW - 1);
            int top = 0xFF000000 | MathHelper.hsvToRgb(hue, s, 1.0f);
            context.fillGradient(svX + i, svY, svX + i + 1, svY + svH, top, 0xFF000000);
        }
        int mx = svX + Math.round(sat * (svW - 1));
        int my = svY + Math.round((1 - val) * (svH - 1));
        outline(context, mx - 2, my - 2, 4, 0xFF000000);
        outline(context, mx - 1, my - 1, 2, 0xFFFFFFFF);
    }

    private void drawHue(DrawContext context) {
        context.fill(svX - 1, hueY - 1, svX + svW + 1, hueY + hueH + 1, 0xFF000000);
        for (int i = 0; i < svW; i++) {
            context.fill(svX + i, hueY, svX + i + 1, hueY + hueH, 0xFF000000 | MathHelper.hsvToRgb(i / (float) svW, 1.0f, 1.0f));
        }
        int mx = svX + Math.round(hue * (svW - 1));
        context.fill(mx - 2, hueY - 2, mx + 2, hueY + hueH + 2, 0xFF000000);
        context.fill(mx - 1, hueY - 1, mx + 1, hueY + hueH + 1, 0xFFFFFFFF);
    }

    private void button(DrawContext context, int x, int y, int w, int h, Text label, boolean active, int mouseX, int mouseY, Runnable action) {
        boolean over = inside(mouseX, mouseY, x, y, x + w, y + h);
        int accent = c(GuiTheme.Slot.ACCENT);
        int edge = active ? accent : over ? c(GuiTheme.Slot.TEXT) : c(GuiTheme.Slot.EDGE);
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, edge);
        context.fill(x, y, x + w, y + h, active ? (accent & 0xFFFFFF) | 0x55000000 : over ? c(GuiTheme.Slot.CARD_HOVER) : c(GuiTheme.Slot.CARD));
        String s = this.textRenderer.trimToWidth(label.getString(), w - 4);
        context.drawTextWithShadow(this.textRenderer, s, x + (w - this.textRenderer.getWidth(s)) / 2, y + (h - 8) / 2, c(GuiTheme.Slot.TEXT));
        hits.add(new Hit(x, y, x + w, y + h, action));
    }

    private void updatePreview() {
        if (preview == null) {
            return;
        }
        // Bei mehreren Bildern laeuft die Vorschau als Animation, sonst zeigt sie das bearbeitete Bild
        int shown = frames.size() > 1 && dragging != 1 ? (int) ((System.currentTimeMillis() / (frameTime * 50L)) % frames.size()) : frame;
        int[] pixels = frames.get(shown);
        NativeImage image = preview.getImage();
        if (image == null) {
            return;
        }
        for (int y = 0; y < texH; y++) {
            for (int x = 0; x < texW; x++) {
                image.setColorArgb(x, y, pixels[y * texW + x]);
            }
        }
        preview.upload();
    }

    private static boolean inside(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    // ---- Malen ----

    private void pushUndo() {
        List<int[]> copy = new ArrayList<>();
        for (int[] f : frames) {
            copy.add(f.clone());
        }
        undo.push(new Snapshot(copy, frame));
        while (undo.size() > UNDO_LIMIT) {
            undo.removeLast();
        }
    }

    private void undo() {
        Snapshot s = undo.poll();
        if (s != null) {
            frames.clear();
            frames.addAll(s.frames());
            frame = Math.min(s.frame(), frames.size() - 1);
        }
    }

    private int @Nullable [] cellAt(double mx, double my) {
        if (mx < canvasX || my < canvasY) {
            return null;
        }
        int x = (int) ((mx - canvasX) / cell);
        int y = (int) ((my - canvasY) / cell);
        return x < texW && y < texH ? new int[]{x, y} : null;
    }

    private void paint(int x, int y, int argb) {
        int[] pixels = frames.get(frame);
        pixels[y * texW + x] = argb;
        if (mirror) {
            pixels[y * texW + (texW - 1 - x)] = argb;
        }
    }

    /** Linie zwischen zwei Pixeln, damit schnelles Ziehen keine Luecken laesst. */
    private void line(int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            paint(x0, y0, argb);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void flood(int x, int y, int argb) {
        int[] pixels = frames.get(frame);
        int target = pixels[y * texW + x];
        if (target == argb) {
            return;
        }
        Deque<int[]> todo = new ArrayDeque<>();
        todo.push(new int[]{x, y});
        while (!todo.isEmpty()) {
            int[] q = todo.pop();
            int qx = q[0];
            int qy = q[1];
            if (qx < 0 || qy < 0 || qx >= texW || qy >= texH || pixels[qy * texW + qx] != target) {
                continue;
            }
            paint(qx, qy, argb);
            todo.push(new int[]{qx + 1, qy});
            todo.push(new int[]{qx - 1, qy});
            todo.push(new int[]{qx, qy + 1});
            todo.push(new int[]{qx, qy - 1});
        }
    }

    private void strokeAt(double mx, double my, boolean first) {
        int[] cellPos = cellAt(mx, my);
        if (cellPos == null) {
            lastPx = -1;
            return;
        }
        int x = cellPos[0];
        int y = cellPos[1];
        boolean erase = dragButton == 1 || tool == Tool.ERASER;
        if (!erase && tool == Tool.PICK) {
            int picked = frames.get(frame)[y * texW + x];
            if ((picked >>> 24) != 0) {
                setColor(picked);
            }
            tool = Tool.PENCIL;
            dragging = 0;
            return;
        }
        if (!erase && tool == Tool.FILL) {
            if (first) {
                flood(x, y, color());
            }
            return;
        }
        int argb = erase ? 0 : color();
        if (lastPx >= 0 && !first) {
            line(lastPx, lastPy, x, y, argb);
        } else {
            paint(x, y, argb);
        }
        lastPx = x;
        lastPy = y;
    }

    private void applySv(double mx, double my) {
        sat = (float) MathHelper.clamp((mx - svX) / Math.max(1, svW - 1), 0, 1);
        val = (float) (1 - MathHelper.clamp((my - svY) / Math.max(1, svH - 1), 0, 1));
    }

    private void applyHue(double mx) {
        hue = (float) MathHelper.clamp((mx - svX) / Math.max(1, svW - 1), 0, 0.999);
    }

    // ---- Eingabe ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (nameField != null) {
            nameField.setFocused(false);
            setFocused(null);
        }
        message = null;
        if (click.button() > 1) {
            return false;
        }
        if (cellAt(mx, my) != null) {
            pushUndo();
            dragging = 1;
            dragButton = click.button();
            lastPx = -1;
            strokeAt(mx, my, true);
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        if (inside(mx, my, svX, svY, svX + svW, svY + svH)) {
            dragging = 2;
            applySv(mx, my);
            return true;
        }
        if (inside(mx, my, svX, hueY - 2, svX + svW, hueY + hueH + 2)) {
            dragging = 3;
            applyHue(mx);
            return true;
        }
        for (Hit hit : List.copyOf(hits)) {
            if (inside(mx, my, hit.x1(), hit.y1(), hit.x2(), hit.y2())) {
                hit.action().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        switch (dragging) {
            case 1 -> strokeAt(click.x(), click.y(), false);
            case 2 -> applySv(click.x(), click.y());
            case 3 -> applyHue(click.x());
            default -> {
                return super.mouseDragged(click, offsetX, offsetY);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != 0) {
            dragging = 0;
            lastPx = -1;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        boolean typing = nameField != null && nameField.isFocused();
        int key = input.key();
        if (!typing && input.hasCtrlOrCmd() && key == GLFW.GLFW_KEY_Z) {
            undo();
            return true;
        }
        if (!typing && !input.hasCtrlOrCmd()) {
            switch (key) {
                case GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_P -> tool = Tool.PENCIL;
                case GLFW.GLFW_KEY_E -> tool = Tool.ERASER;
                case GLFW.GLFW_KEY_F -> tool = Tool.FILL;
                case GLFW.GLFW_KEY_I -> tool = Tool.PICK;
                case GLFW.GLFW_KEY_M -> mirror = !mirror;
                case GLFW.GLFW_KEY_G -> grid = !grid;
                case GLFW.GLFW_KEY_LEFT -> frame = (frame - 1 + frames.size()) % frames.size();
                case GLFW.GLFW_KEY_RIGHT -> frame = (frame + 1) % frames.size();
                default -> {
                    return super.keyPressed(input);
                }
            }
            return true;
        }
        if (typing && key == GLFW.GLFW_KEY_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(input);
    }

    // ---- Speichern ----

    private void save() {
        String name = nameField == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            message = Text.translatable("arnestyles.draw.need_name");
            messageError = true;
            if (nameField != null) {
                setFocused(nameField);
                nameField.setFocused(true);
            }
            return;
        }
        String id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        if (id.isEmpty() || id.equalsIgnoreCase("default")) {
            id = "zeichnung";
        }
        Path dir = StyleRegistry.categoryDir(category).resolve(id);
        try {
            boolean overwrite = Files.isDirectory(dir) && isDrawing(dir);
            if (Files.exists(dir) && !overwrite) {
                int n = 2;
                while (Files.exists(StyleRegistry.categoryDir(category).resolve(id + "_" + n))) {
                    n++;
                }
                id = id + "_" + n;
                dir = StyleRegistry.categoryDir(category).resolve(id);
            }
            Files.createDirectories(dir);
            // Erst den ganzen Ausgangs-Style kopieren (die anderen Texturen bleiben so), dann die Zeichnung
            Path from = source.root();
            if (from != null && !from.equals(dir) && Files.isDirectory(from.resolve("assets"))) {
                copyTree(from.resolve("assets"), dir.resolve("assets"));
            } else if (from == null) {
                // Ausgangs-Style ist Minecraft selbst: die anderen Texturen der Kategorie mitnehmen,
                // damit der neue Style vollstaendig ist und im Customize-Tab weiter bearbeitet werden kann
                for (String other : TintedTextures.textures(source)) {
                    if (!other.equals(asset)) {
                        exportVanilla(other, dir.resolve("assets").resolve("minecraft").resolve(other));
                    }
                }
            }
            Path target = dir.resolve("assets").resolve("minecraft").resolve(asset);
            Files.createDirectories(target.getParent());
            try (NativeImage image = new NativeImage(texW, texH * frames.size(), true)) {
                for (int f = 0; f < frames.size(); f++) {
                    int[] pixels = frames.get(f);
                    for (int y = 0; y < texH; y++) {
                        for (int x = 0; x < texW; x++) {
                            image.setColorArgb(x, f * texH + y, pixels[y * texW + x]);
                        }
                    }
                }
                image.writeTo(target);
            }
            // .mcmeta: vorhandene Angaben (z. B. GUI-Skalierung) behalten, nur die Animation setzen oder entfernen
            Path meta = target.resolveSibling(target.getFileName() + ".mcmeta");
            JsonObject metaJson = readMeta(meta, asset);
            if (frames.size() > 1) {
                JsonObject animation = new JsonObject();
                animation.addProperty("frametime", frameTime);
                metaJson.add("animation", animation);
            } else {
                metaJson.remove("animation");
            }
            if (metaJson.size() == 0) {
                Files.deleteIfExists(meta);
            } else {
                Files.writeString(meta, GSON.toJson(metaJson), StandardCharsets.UTF_8);
            }
            JsonObject json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("description", Text.translatable("arnestyles.draw.description").getString());
            json.addProperty("drawn", true);
            Files.writeString(dir.resolve("style.json"), GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            ArneStyles.LOGGER.error("Zeichnung konnte nicht gespeichert werden: {}", dir, e);
            message = Text.translatable("arnestyles.draw.save_failed");
            messageError = true;
            return;
        }
        parent.drawingSaved(category, id, asset);
        if (backToGame && this.client != null) {
            // Direkt ins Spiel und neu laden, damit der bemalte Block sofort zu sehen ist
            this.client.setScreen(null);
            this.client.reloadResources();
            return;
        }
        close();
    }

    /** Minecraft-Id fuer einen Pfad aus einem Pack. Ungueltige Pfade (z. B. Grossbuchstaben) fuehren ins Leere statt zum Absturz. */
    private static Identifier vanillaId(String path) {
        return Identifier.isPathValid(path) ? Identifier.ofVanilla(path) : Identifier.ofVanilla("arnestyles_invalid_path");
    }

    private static void exportVanilla(String asset, Path out) throws IOException {
        if (Files.exists(out)) {
            return;
        }
        var pack = net.minecraft.client.MinecraftClient.getInstance().getDefaultResourcePack();
        var png = pack.open(net.minecraft.resource.ResourceType.CLIENT_RESOURCES, vanillaId(asset));
        if (png == null) {
            return;
        }
        Files.createDirectories(out.getParent());
        try (InputStream in = png.get()) {
            Files.copy(in, out);
        }
        var meta = pack.open(net.minecraft.resource.ResourceType.CLIENT_RESOURCES, vanillaId(asset + ".mcmeta"));
        if (meta != null) {
            try (InputStream in = meta.get()) {
                Files.copy(in, out.resolveSibling(out.getFileName() + ".mcmeta"));
            }
        }
    }

    /** Vorhandene .mcmeta (aus dem kopierten Style oder von Minecraft), sonst ein leeres Objekt. */
    private static JsonObject readMeta(Path meta, String asset) {
        try {
            if (Files.isRegularFile(meta)) {
                return JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            var vanilla = net.minecraft.client.MinecraftClient.getInstance().getDefaultResourcePack()
                .open(net.minecraft.resource.ResourceType.CLIENT_RESOURCES, vanillaId(asset + ".mcmeta"));
            if (vanilla != null) {
                try (InputStream in = vanilla.get()) {
                    return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
        } catch (IOException | RuntimeException e) {
            // ungueltige Datei: neu anfangen
        }
        return new JsonObject();
    }

    private static boolean isDrawing(Path dir) {
        Path json = dir.resolve("style.json");
        if (!Files.isRegularFile(json)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            return object != null && object.has("drawn") && object.get("drawn").getAsBoolean();
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Ordner kopieren, auch aus der Mod-Jar (anderes Dateisystem, daher ueber Text-Pfade). */
    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path path : walk.toList()) {
                String rel = from.relativize(path).toString().replace('\\', '/');
                if (rel.endsWith("/")) {
                    rel = rel.substring(0, rel.length() - 1);
                }
                Path out = rel.isEmpty() ? to : to.resolve(rel);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(out);
                } else if (!Files.exists(out)) {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = Files.newInputStream(path)) {
                        Files.copy(in, out);
                    }
                }
            }
        }
    }

    // ---- Schliessen ----

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        if (preview != null && this.client != null) {
            this.client.getTextureManager().destroyTexture(PREVIEW_ID);
            preview = null;
        }
        super.removed();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(backToGame ? null : parent);
        }
    }
}
