package com.arnestyles.client.gui;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.HudLayout;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * HUD-Editor: eigene Anzeigen (Ruestung, Totems) mit der Maus verschieben, mit dem Mausrad vergroessern
 * oder verkleinern, Rechtsklick setzt eine Anzeige zurueck. Das HUD laeuft im Hintergrund normal weiter.
 */
public class HudEditorScreen extends Screen {
    private static final int OUTLINE_HOVER = 0xFFFFFFFF;
    private static final int LABEL_BG = 0xC0141826;

    private final @Nullable Screen parent;
    private HudLayout.@Nullable Element dragging;
    /**
     * Rahmen aus dem letzten Bild. Das HUD traegt sie jedes Bild neu ein und der Editor leert sie danach;
     * Mausklicks kommen aber zwischen zwei Bildern an, deshalb wird hier eine Kopie fuer Klicks aufgehoben.
     */
    private final java.util.Map<HudLayout.Element, int[]> shown = new java.util.EnumMap<>(HudLayout.Element.class);
    private double grabX;
    private double grabY;

    public HudEditorScreen(@Nullable Screen parent) {
        this(parent, HudLayout.showVanilla);
    }

    /** vanilla = Minecraft-Teile (Hotbar, Scoreboard ...) gleich mit einblenden. */
    public HudEditorScreen(@Nullable Screen parent, boolean vanilla) {
        super(Text.translatable("arnestyles.hud.title"));
        this.parent = parent;
        HudLayout.showVanilla = vanilla;
    }

    private Text vanillaLabel() {
        return Text.translatable(HudLayout.showVanilla ? "arnestyles.hud.vanilla_on" : "arnestyles.hud.vanilla_off");
    }

    @Override
    protected void init() {
        HudLayout.editing = true;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
            .dimensions(this.width / 2 - 84, 22, 80, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(vanillaLabel(), button -> {
                HudLayout.showVanilla = !HudLayout.showVanilla;
                button.setMessage(vanillaLabel());
            })
            .dimensions(this.width / 2 + 4, 22, 120, 20)
            .build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Kein Abdunkeln: das HUD soll so sichtbar sein wie im Spiel
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("arnestyles.hud.help"), this.width / 2, 8, 0xFFFFFFFF);
        boolean any = false;
        shown.clear();
        for (HudLayout.Element element : HudLayout.Element.values()) {
            int[] b = HudLayout.bounds(element);
            if (b != null) {
                shown.put(element, b);
            }
            if (b == null || (element.vanilla() && !HudLayout.showVanilla)) {
                continue;
            }
            any = true;
            boolean over = element == dragging || inside(mouseX, mouseY, b);
            int color = over ? OUTLINE_HOVER : GuiTheme.active().argb(GuiTheme.Slot.ACCENT);
            context.fill(b[0] - 1, b[1] - 1, b[2] + 1, b[1], color);
            context.fill(b[0] - 1, b[3], b[2] + 1, b[3] + 1, color);
            context.fill(b[0] - 1, b[1], b[0], b[3], color);
            context.fill(b[2], b[1], b[2] + 1, b[3], color);
            ArneStylesConfig.HudPosition pos = HudLayout.position(element);
            String label = Text.translatable(element.nameKey()).getString() + String.format(Locale.ROOT, "  %d%%", Math.round(pos.scale * 100));
            int lw = this.textRenderer.getWidth(label);
            int ly = b[1] - 12 < 20 ? b[3] + 3 : b[1] - 12;
            // Beschriftung bleibt im Bild (Anzeigen am rechten Rand)
            int lx = Math.max(1, Math.min(b[0], this.width - lw - 4));
            context.fill(lx - 1, ly - 1, lx + lw + 3, ly + 9, LABEL_BG);
            context.drawTextWithShadow(this.textRenderer, label, lx + 1, ly, color);
        }
        if (!any) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("arnestyles.hud.none"), this.width / 2, 48, 0xFFE07A5F);
        }
        // Rahmen kommen jedes Bild neu aus dem HUD, ausgeschaltete Anzeigen sollen keinen alten Rahmen behalten
        HudLayout.clearBounds();
    }

    private static boolean inside(double x, double y, int[] b) {
        return x >= b[0] && x < b[2] && y >= b[1] && y < b[3];
    }

    private HudLayout.@Nullable Element elementAt(double x, double y) {
        // Von hinten nach vorne: die zuletzt gezeichnete (oberste) Anzeige gewinnt
        HudLayout.Element[] elements = HudLayout.Element.values();
        for (int i = elements.length - 1; i >= 0; i--) {
            int[] b = shown.get(elements[i]);
            if (b != null && inside(x, y, b) && (!elements[i].vanilla() || HudLayout.showVanilla)) {
                return elements[i];
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        HudLayout.Element element = elementAt(click.x(), click.y());
        if (element == null) {
            return false;
        }
        if (click.button() == 1) {
            HudLayout.reset(element);
            return true;
        }
        if (click.button() == 0) {
            ArneStylesConfig.HudPosition pos = HudLayout.position(element);
            dragging = element;
            grabX = click.x() - pos.x;
            grabY = click.y() - pos.y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            ArneStylesConfig.HudPosition pos = HudLayout.position(dragging);
            pos.x = (float) Math.round(click.x() - grabX);
            pos.y = (float) Math.round(click.y() - grabY);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudLayout.Element element = elementAt(mouseX, mouseY);
        if (element == null || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        ArneStylesConfig.HudPosition pos = HudLayout.position(element);
        float scale = pos.scale + (float) Math.signum(verticalAmount) * 0.1f;
        pos.scale = Math.round(Math.max(HudLayout.MIN_SCALE, Math.min(HudLayout.MAX_SCALE, scale)) * 10.0f) / 10.0f;
        ConfigManager.save();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        HudLayout.editing = false;
        ConfigManager.save();
        super.removed();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
