package com.arnestyles.client.gui;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.CustomCrosshair;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fadenkreuz-Editor: 15x15-Raster mit der Maus malen (links) und radieren (rechts), Vorlagen per Knopf.
 * Rechts daneben die Vorschau in Originalgroesse und vergroessert. Gespeichert wird beim Schliessen.
 */
public final class CrosshairEditorScreen extends Screen {
    private static final int BG = 0xF0080B12;
    private static final int PANEL = 0xFF0D1320;
    private static final int EDGE = 0xFF1E2A40;
    private static final int CELL_OFF = 0xFF141C2C;
    private static final int CELL_ON = 0xFF35D6E8;
    private static final int CENTER = 0xFFA66AFF;

    private final @Nullable Screen parent;
    private final List<String> rows;
    private int cell = 12;
    private int gridX;
    private int gridY;
    /** -1 = kein Malen, 1 = malen, 0 = radieren. */
    private int painting = -1;

    public CrosshairEditorScreen(@Nullable Screen parent) {
        super(Text.translatable("arnestyles.crosshair.title"));
        this.parent = parent;
        this.rows = new ArrayList<>(CustomCrosshair.sanitize(ConfigManager.get().crosshairPixels));
    }

    @Override
    protected void init() {
        cell = Math.max(6, Math.min(14, (this.height - 90) / CustomCrosshair.SIZE));
        int gridW = cell * CustomCrosshair.SIZE;
        int total = gridW + 24 + 150;
        gridX = (this.width - total) / 2;
        gridY = 36;
        int bx = gridX + gridW + 24;
        int by = gridY + 92;
        int i = 0;
        for (String preset : CustomCrosshair.PRESETS) {
            int col = i % 2;
            int row = i / 2;
            addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.crosshair.preset." + preset), b -> applyPreset(preset))
                .dimensions(bx + col * 76, by + row * 22, 74, 20).build());
            i++;
        }
        int ry = by + ((CustomCrosshair.PRESETS.length + 1) / 2) * 22 + 6;
        addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.crosshair.clear"), b -> {
            List<String> empty = CustomCrosshair.empty();
            for (int y = 0; y < rows.size(); y++) {
                rows.set(y, empty.get(y));
            }
            CustomCrosshair.invalidate();
        }).dimensions(bx, ry, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.crosshair.done"), b -> close())
            .dimensions(bx, ry + 24, 150, 20).build());
    }

    private void applyPreset(String preset) {
        List<String> p = CustomCrosshair.preset(preset);
        for (int y = 0; y < rows.size(); y++) {
            rows.set(y, p.get(y));
        }
        CustomCrosshair.invalidate();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, BG);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("arnestyles.crosshair.hint"), this.width / 2, 23, 0xFF8FA3BF);
        int n = CustomCrosshair.SIZE;
        int gridW = cell * n;
        context.fill(gridX - 3, gridY - 3, gridX + gridW + 3, gridY + gridW + 3, EDGE);
        context.fill(gridX - 2, gridY - 2, gridX + gridW + 2, gridY + gridW + 2, PANEL);
        int c = n / 2;
        int hoverX = (mouseX - gridX) / cell;
        int hoverY = (mouseY - gridY) / cell;
        boolean overGrid = mouseX >= gridX && mouseY >= gridY && hoverX < n && hoverY < n;
        for (int y = 0; y < n; y++) {
            String row = rows.get(y);
            for (int x = 0; x < n; x++) {
                boolean on = row.charAt(x) == '#';
                int color = on ? CELL_ON : CELL_OFF;
                if (!on && x == c && y == c) {
                    color = 0xFF2A2245;
                }
                int x1 = gridX + x * cell;
                int y1 = gridY + y * cell;
                context.fill(x1, y1, x1 + cell - 1, y1 + cell - 1, color);
                if (overGrid && x == hoverX && y == hoverY) {
                    context.fill(x1, y1, x1 + cell - 1, y1 + cell - 1, 0x40FFFFFF);
                }
            }
        }
        // Mitte markieren
        int mx = gridX + c * cell;
        int my = gridY + c * cell;
        context.fill(mx, my - 1, mx + cell - 1, my, CENTER);
        context.fill(mx, my + cell - 1, mx + cell - 1, my + cell, CENTER);

        // Vorschau: Originalgroesse und 3x, auf hellem und dunklem Grund
        int px = gridX + gridW + 24;
        int py = gridY;
        int rgb = ConfigManager.get().crosshairRgb;
        float scale = ConfigManager.get().crosshairScale;
        context.fill(px, py, px + 74, py + 74, 0xFF5E8C3A);
        context.fill(px + 76, py, px + 150, py + 74, 0xFF0A0A0A);
        // Vorschau nutzt dieselbe Zeichen-Routine wie im Spiel
        List<String> saved = ConfigManager.get().crosshairPixels;
        ConfigManager.get().crosshairPixels = rows;
        CustomCrosshair.invalidate();
        CustomCrosshair.draw(context, px + 37, py + 37, rgb, scale);
        CustomCrosshair.draw(context, px + 113, py + 37, rgb, 3.0f);
        ConfigManager.get().crosshairPixels = saved;
        CustomCrosshair.invalidate();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (paintAt(click.x(), click.y(), click.button() == 0 ? 1 : click.button() == 1 ? 0 : -1)) {
            painting = click.button() == 0 ? 1 : 0;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (painting >= 0 && paintAt(click.x(), click.y(), painting)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        painting = -1;
        return super.mouseReleased(click);
    }

    private boolean paintAt(double mouseX, double mouseY, int mode) {
        if (mode < 0 || mouseX < gridX || mouseY < gridY) {
            return false;
        }
        int x = (int) ((mouseX - gridX) / cell);
        int y = (int) ((mouseY - gridY) / cell);
        if (x >= CustomCrosshair.SIZE || y >= CustomCrosshair.SIZE) {
            return false;
        }
        CustomCrosshair.set(rows, x, y, mode == 1);
        return true;
    }

    @Override
    public void close() {
        ConfigManager.get().crosshairPixels = new ArrayList<>(rows);
        CustomCrosshair.invalidate();
        ConfigManager.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
