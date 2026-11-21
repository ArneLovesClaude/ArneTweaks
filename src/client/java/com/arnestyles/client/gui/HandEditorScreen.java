package com.arnestyles.client.gui;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.HandTransforms;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Hand-Editor: das Item in der Hand live verschieben, drehen und vergroessern. Die Einstellung gilt pro Item
 * (z. B. nur fuer Schwerter einer Sorte). Mausrad wechselt den Hotbar-Platz, damit man das naechste Item einstellen kann.
 */
public class HandEditorScreen extends Screen {
    private static final int PANEL_W = 170;

    private final @Nullable Screen parent;
    private String shownKey = "";

    private record Value(String key, float min, float max, float step, Function<ArneStylesConfig.HandTransform, Float> get,
                         BiConsumer<ArneStylesConfig.HandTransform, Float> set, String unit) {
    }

    private static final Value[] VALUES = {
        new Value("x", -1.0f, 1.0f, 0.01f, t -> t.x, (t, v) -> t.x = v, ""),
        new Value("y", -1.0f, 1.0f, 0.01f, t -> t.y, (t, v) -> t.y = v, ""),
        new Value("z", -1.0f, 1.0f, 0.01f, t -> t.z, (t, v) -> t.z = v, ""),
        new Value("rot_x", -180.0f, 180.0f, 1.0f, t -> t.rotX, (t, v) -> t.rotX = v, "°"),
        new Value("rot_y", -180.0f, 180.0f, 1.0f, t -> t.rotY, (t, v) -> t.rotY = v, "°"),
        new Value("rot_z", -180.0f, 180.0f, 1.0f, t -> t.rotZ, (t, v) -> t.rotZ = v, "°"),
        new Value("scale", 0.2f, 2.5f, 0.05f, t -> t.scale, (t, v) -> t.scale = v, "x"),
    };

    public HandEditorScreen(@Nullable Screen parent) {
        super(Text.translatable("arnestyles.hand.title"));
        this.parent = parent;
    }

    private ItemStack held() {
        return this.client == null || this.client.player == null ? ItemStack.EMPTY : this.client.player.getMainHandStack();
    }

    @Override
    protected void init() {
        ItemStack stack = held();
        shownKey = stack.isEmpty() ? "" : HandTransforms.key(stack);
        int x = 12;
        int y = 42;
        if (!stack.isEmpty()) {
            for (Value value : VALUES) {
                addDrawableChild(new ValueSlider(x, y, PANEL_W, 18, value, stack));
                y += 22;
            }
            addDrawableChild(ButtonWidget.builder(Text.translatable("arnestyles.hand.reset"), b -> {
                HandTransforms.reset(held());
                ConfigManager.save();
                clearAndInit();
            }).dimensions(x, y + 4, PANEL_W, 20).build());
            y += 28;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(x, y + 4, PANEL_W, 20).build());
    }

    @Override
    public void tick() {
        // Anderes Item in die Hand genommen: Regler neu aufbauen
        ItemStack stack = held();
        String key = stack.isEmpty() ? "" : HandTransforms.key(stack);
        if (!key.equals(shownKey)) {
            clearAndInit();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Kein Abdunkeln: die Hand soll sichtbar bleiben, nur hinter den Reglern ein Feld
        context.fill(6, 6, 18 + PANEL_W, this.height - 6, 0xB0101420);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        ItemStack stack = held();
        context.drawTextWithShadow(this.textRenderer, this.title, 12, 12, GuiTheme.active().argb(GuiTheme.Slot.ACCENT));
        Text sub = stack.isEmpty() ? Text.translatable("arnestyles.hand.empty") : stack.getName();
        context.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(sub, PANEL_W).getString(), 12, 26, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("arnestyles.hand.help"), 12, this.height - 18, 0xFFB0B4C8);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX > 18 + PANEL_W && this.client != null && this.client.player != null && verticalAmount != 0) {
            var inventory = this.client.player.getInventory();
            int slot = Math.floorMod(inventory.getSelectedSlot() - (int) Math.signum(verticalAmount), 9);
            inventory.setSelectedSlot(slot);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        ConfigManager.get().handTransforms.values().removeIf(ArneStylesConfig.HandTransform::isDefault);
        ConfigManager.save();
        super.removed();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    /** Regler fuer einen Wert des gehaltenen Items. */
    private static final class ValueSlider extends SliderWidget {
        private final Value v;
        private final ItemStack stack;

        ValueSlider(int x, int y, int w, int h, Value v, ItemStack stack) {
            super(x, y, w, h, Text.empty(), 0);
            this.v = v;
            this.stack = stack;
            ArneStylesConfig.HandTransform t = ConfigManager.get().handTransforms.get(HandTransforms.key(stack));
            float current = t == null ? (v.key().equals("scale") ? 1.0f : 0.0f) : v.get().apply(t);
            this.value = (current - v.min()) / (v.max() - v.min());
            updateMessage();
        }

        private float real() {
            float raw = v.min() + (float) value * (v.max() - v.min());
            return Math.round(raw / v.step()) * v.step();
        }

        @Override
        protected void updateMessage() {
            float r = real();
            String number = v.step() >= 1 ? Integer.toString(Math.round(r)) : String.format(Locale.ROOT, "%.2f", r);
            setMessage(Text.translatable("arnestyles.hand." + v.key()).append(": " + number + v.unit()));
        }

        @Override
        protected void applyValue() {
            v.set().accept(HandTransforms.edit(stack), real());
        }
    }
}
