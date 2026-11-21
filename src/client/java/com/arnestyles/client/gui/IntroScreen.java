package com.arnestyles.client.gui;

import com.arnestyles.client.ArneKeybinds;
import com.arnestyles.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Kurze Einfuehrung beim ersten Start: ein paar Seiten zu den wichtigsten Funktionen.
 * Jederzeit ueberspringbar (Knopf oder Esc), danach kommt sie nicht wieder.
 * Spaeter wieder aufrufbar: Mods -> Komfort -> Einfuehrung.
 */
public final class IntroScreen extends Screen {
    private static final int PAGES = 6;
    private static final ItemStack[] ICONS = {
        new ItemStack(Items.TOTEM_OF_UNDYING),
        new ItemStack(Items.COMPASS),
        new ItemStack(Items.END_CRYSTAL),
        new ItemStack(Items.COMPARATOR),
        new ItemStack(Items.FILLED_MAP),
        new ItemStack(Items.WRITABLE_BOOK)
    };
    private static boolean openedThisSession;

    private final @Nullable Screen parent;
    private int page;
    private ButtonWidget backButton;
    private ButtonWidget nextButton;
    private long pageStart = System.currentTimeMillis();

    public IntroScreen(@Nullable Screen parent) {
        super(Text.translatable("arnestyles.intro.title"));
        this.parent = parent;
    }

    /** Zeigt die Einfuehrung einmal, sobald das Hauptmenue das erste Mal offen ist. */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openedThisSession || ConfigManager.get().introShown) {
                return;
            }
            if (client.currentScreen instanceof TitleScreen title && client.getOverlay() == null) {
                openedThisSession = true;
                client.setScreen(new IntroScreen(title));
            }
        });
    }

    @Override
    protected void init() {
        int cw = cardWidth();
        int cx = this.width / 2;
        int by = cardBottom() - 28;
        addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.intro.skip"), b -> finish())
            .dimensions(cx - cw / 2 + 10, by, 80, 20).build());
        backButton = addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.intro.back"), b -> turn(-1))
            .dimensions(cx + cw / 2 - 180, by, 80, 20).build());
        nextButton = addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.intro.next"), b -> turn(1))
            .dimensions(cx + cw / 2 - 92, by, 82, 20).build());
        updateButtons();
    }

    private int cardWidth() {
        return Math.min(360, this.width - 32);
    }

    private int cardTop() {
        return Math.max(12, this.height / 2 - 105);
    }

    private int cardBottom() {
        return Math.min(this.height - 12, this.height / 2 + 105);
    }

    private void turn(int delta) {
        if (page + delta >= PAGES) {
            finish();
            return;
        }
        page = Math.max(0, page + delta);
        pageStart = System.currentTimeMillis();
        updateButtons();
    }

    private void updateButtons() {
        if (backButton != null) {
            backButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.setMessage(Text.translatable(page == PAGES - 1 ? "arnestyles.intro.done" : "arnestyles.intro.next"));
        }
    }

    private void finish() {
        ConfigManager.get().introShown = true;
        ConfigManager.save();
        close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            finish();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            turn(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            turn(-1);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
        context.fill(0, 0, this.width, this.height, GuiTheme.active().argb(GuiTheme.Slot.BACKDROP));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        GuiTheme.Theme theme = GuiTheme.active();
        int accent = theme.argb(GuiTheme.Slot.ACCENT);
        int textColor = theme.argb(GuiTheme.Slot.TEXT);
        int muted = theme.argb(GuiTheme.Slot.MUTED);
        int cw = cardWidth();
        int x1 = this.width / 2 - cw / 2;
        int x2 = x1 + cw;
        int y1 = cardTop();
        int y2 = cardBottom();

        // Karte
        context.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, theme.argb(GuiTheme.Slot.EDGE));
        context.fill(x1, y1, x2, y2, theme.argb(GuiTheme.Slot.PANEL));
        context.fill(x1, y1, x2, y1 + 2, accent);

        TextRenderer font = this.textRenderer;
        // Kopf: "ArneTweaks" + Seite
        context.drawTextWithShadow(font, Text.literal("ArneTweaks"), x1 + 10, y1 + 9, accent);
        String counter = (page + 1) + " / " + PAGES;
        context.drawTextWithShadow(font, counter, x2 - 10 - font.getWidth(counter), y1 + 9, muted);

        // Symbol, leicht einschwingend
        float t = Math.min(1.0f, (System.currentTimeMillis() - pageStart) / 220.0f);
        float ease = 1.0f - (1.0f - t) * (1.0f - t);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.width / 2.0f, y1 + 44.0f);
        float s = 2.0f + 0.4f * (1.0f - ease);
        context.getMatrices().scale(s, s);
        context.drawItem(ICONS[page], -8, -8);
        context.getMatrices().popMatrix();

        // Ueberschrift
        Text heading = Text.translatable("arnestyles.intro.page" + page + ".title");
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.width / 2.0f, y1 + 70.0f);
        context.getMatrices().scale(1.4f, 1.4f);
        context.drawTextWithShadow(font, heading, -font.getWidth(heading) / 2, 0, textColor);
        context.getMatrices().popMatrix();

        // Text (umgebrochen), die Menue-Taste wird eingesetzt
        Text body = Text.translatable("arnestyles.intro.page" + page + ".text", menuKey());
        List<OrderedText> lines = font.wrapLines(body, cw - 40);
        int ty = y1 + 90;
        for (OrderedText line : lines) {
            context.drawTextWithShadow(font, line, this.width / 2 - font.getWidth(line) / 2, ty, muted);
            ty += 11;
        }

        // Punkte fuer die Seiten
        int dots = PAGES * 10 - 4;
        int dx = this.width / 2 - dots / 2;
        int dy = y2 - 40;
        for (int i = 0; i < PAGES; i++) {
            int c = i == page ? accent : theme.argb(GuiTheme.Slot.OFF);
            context.fill(dx + i * 10, dy, dx + i * 10 + 6, dy + 3, c);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private static Text menuKey() {
        if (ArneKeybinds.OPEN_GUI == null) {
            return Text.literal("K");
        }
        return ArneKeybinds.OPEN_GUI.getBoundKeyLocalizedText().copy().withColor(GuiTheme.active().rgb(GuiTheme.Slot.ACCENT));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    /** Fuer den Selbsttest. */
    public static IntroScreen forTest(@Nullable Screen parent, int page) {
        IntroScreen screen = new IntroScreen(parent);
        screen.page = Math.max(0, Math.min(PAGES - 1, page));
        return screen;
    }
}
