package com.arnestyles.client.mods;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * AppleSkin: Saettigung als gelber Rahmen ueber den Hungerkeulen, Vorschau beim Halten von Essen
 * (blinkende Keulen, Saettigung und Herzen) und Hunger/Saettigung im Tooltip.
 * Nachgebaut nach AppleSkin (squeek502, gemeinfrei / Unlicense). Nur clientseitig:
 * die Saettigung schickt der Server sowieso, Erschoepfung (Exhaustion) aber nicht, deshalb fehlt die Anzeige dafuer.
 */
public final class AppleSkin {
    private static final Identifier ICONS = Identifier.of("arnestyles", "textures/gui/appleskin_icons.png");
    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_EMPTY_HUNGER = Identifier.ofVanilla("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.ofVanilla("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.ofVanilla("hud/food_full_hunger");
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_CONTAINER_HARDCORE = Identifier.ofVanilla("hud/heart/container_hardcore");
    private static final Identifier HEART_FULL_HARDCORE = Identifier.ofVanilla("hud/heart/hardcore_full");
    private static final Identifier HEART_HALF_HARDCORE = Identifier.ofVanilla("hud/heart/hardcore_half");
    private static final int ICON = 9;
    private static final float MAX_FLASH_ALPHA = 0.65f;
    private static final float MAX_EXHAUSTION = 4.0f;
    private static final float REGEN_EXHAUSTION = 6.0f;

    private static float unclampedFlash;
    private static float flashAlpha;
    private static int flashDir = 1;

    /** Gleicher Zufall wie im Vanilla-HUD, damit die Overlays beim Wackeln der Keulen und Herzen mitwackeln. */
    private static final Random RANDOM = Random.create();
    private static final int[] FOOD_Y = new int[10];
    private static int[] heartX = new int[0];
    private static int[] heartY = new int[0];
    private static int offsetsTick = -1;

    private AppleSkin() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickFlash());
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> addTooltip(stack, lines));
    }

    private static boolean anyHudActive() {
        return ModSetting.Toggle.APPLESKIN_SATURATION.isActive() || ModSetting.Toggle.APPLESKIN_PREVIEW.isActive()
            || ModSetting.Toggle.APPLESKIN_HEALTH.isActive();
    }

    // ---- HUD ----

    /** Nach den Vanilla-Hungerkeulen. right und top wie in InGameHud.renderFood. */
    public static void renderFood(DrawContext context, PlayerEntity player, int top, int right) {
        if (!anyHudActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int ticks = client.inGameHud.getTicks();
        updateOffsets(player, ticks);
        HungerManager hunger = player.getHungerManager();
        float saturation = hunger.getSaturationLevel();
        boolean showSaturation = ModSetting.Toggle.APPLESKIN_SATURATION.isActive();
        if (showSaturation) {
            drawSaturation(context, 0.0f, saturation, right, top, 1.0f);
        }

        HeldFood food = heldFood(player);
        if (food == null || !ModSetting.Toggle.APPLESKIN_PREVIEW.isActive()) {
            if (food == null) {
                resetFlash();
            }
            return;
        }
        int nutrition = food.food().nutrition();
        drawHungerPreview(context, nutrition, hunger.getFoodLevel(), right, top, flashAlpha, food.rotten());
        if (showSaturation) {
            int newFood = hunger.getFoodLevel() + nutrition;
            float newSaturation = saturation + food.food().saturation();
            float gained = newSaturation > newFood ? newFood - saturation : food.food().saturation();
            drawSaturation(context, gained, saturation, right, top, flashAlpha);
        }
    }

    /** Nach den Vanilla-Herzen: geschaetzte Heilung durch das gehaltene Essen. */
    public static void renderHealth(DrawContext context, PlayerEntity player, int left, int top) {
        if (!ModSetting.Toggle.APPLESKIN_HEALTH.isActive()) {
            return;
        }
        HeldFood food = heldFood(player);
        if (food == null) {
            return;
        }
        updateOffsets(player, MinecraftClient.getInstance().inGameHud.getTicks());
        if (!canShowHealth(player)) {
            return;
        }
        float health = player.getHealth();
        float modified = Math.min(health + estimatedHealing(player, food), player.getMaxHealth());
        if (modified <= health) {
            return;
        }
        boolean hardcore = player.getEntityWorld().getLevelProperties().isHardcore();
        int color = ColorHelper.getWhite(flashAlpha);
        int background = ColorHelper.getWhite(flashAlpha * 0.25f);
        int fixedModified = (int) Math.ceil(modified);
        int start = (int) Math.max(0, Math.ceil(health) / 2.0f);
        int end = (int) Math.max(0, Math.ceil(modified / 2.0f));
        for (int i = start; i < end && i < heartX.length; i++) {
            int x = left + heartX[i];
            int y = top + heartY[i];
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, hardcore ? HEART_CONTAINER_HARDCORE : HEART_CONTAINER, x, y, ICON, ICON, background);
            boolean half = i * 2 + 1 == fixedModified;
            Identifier sprite = half ? (hardcore ? HEART_HALF_HARDCORE : HEART_HALF) : (hardcore ? HEART_FULL_HARDCORE : HEART_FULL);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON, ICON, color);
        }
    }

    private static void drawSaturation(DrawContext context, float gained, float saturation, int right, int top, float alpha) {
        if (saturation + gained < 0) {
            return;
        }
        int color = ColorHelper.getWhite(alpha);
        float modified = Math.max(0, Math.min(saturation + gained, 20));
        int start = gained != 0 ? (int) Math.max(saturation / 2.0f, 0) : 0;
        int end = (int) Math.ceil(modified / 2.0f);
        for (int i = start; i < end && i < FOOD_Y.length; i++) {
            int x = right - i * 8 - 9;
            int y = top + FOOD_Y[i];
            float bar = modified / 2.0f - i;
            int u = bar >= 1 ? 3 * ICON : bar > 0.5f ? 2 * ICON : bar > 0.25f ? ICON : 0;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, x, y, u, 0, ICON, ICON, 256, 256, color);
        }
    }

    private static void drawHungerPreview(DrawContext context, int restored, int foodLevel, int right, int top, float alpha, boolean rotten) {
        if (restored <= 0) {
            return;
        }
        int color = ColorHelper.getWhite(alpha);
        int background = ColorHelper.getWhite(alpha * 0.25f);
        int modified = Math.max(0, Math.min(20, foodLevel + restored));
        int start = Math.max(0, foodLevel / 2);
        int end = (int) Math.ceil(modified / 2.0f);
        for (int i = start; i < end && i < FOOD_Y.length; i++) {
            int x = right - i * 8 - 9;
            int y = top + FOOD_Y[i];
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, rotten ? FOOD_EMPTY_HUNGER : FOOD_EMPTY, x, y, ICON, ICON, background);
            boolean half = i * 2 + 1 == modified;
            Identifier sprite = half ? (rotten ? FOOD_HALF_HUNGER : FOOD_HALF) : (rotten ? FOOD_FULL_HUNGER : FOOD_FULL);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON, ICON, color);
        }
    }

    /**
     * Berechnet das Wackeln der Herzen und Keulen genau wie das Vanilla-HUD (gleicher Startwert fuer den Zufall,
     * gleiche Reihenfolge der Aufrufe), damit die Overlays auf den Symbolen bleiben.
     */
    private static void updateOffsets(PlayerEntity player, int ticks) {
        if (ticks == offsetsTick) {
            return;
        }
        offsetsTick = ticks;
        float absorption = (float) Math.ceil(player.getAbsorptionAmount());
        int hearts = (int) Math.ceil((player.getMaxHealth() + absorption) / 2.0f);
        if (hearts < 0 || hearts > 1000) {
            hearts = 0;
        }
        int rows = (int) Math.ceil(hearts / 10.0f);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        HungerManager hunger = player.getHungerManager();
        boolean shakeFood = hunger.getSaturationLevel() <= 0.0f && ticks % (hunger.getFoodLevel() * 3 + 1) == 0;
        boolean shakeHealth = Math.ceil(player.getHealth()) <= 4;

        RANDOM.setSeed(ticks * 312871L);
        if (heartX.length != hearts) {
            heartX = new int[hearts];
            heartY = new int[hearts];
        }
        for (int i = hearts - 1; i >= 0; i--) {
            int row = (int) Math.ceil((i + 1) / 10.0f) - 1;
            heartX[i] = i % 10 * 8;
            heartY[i] = -(row * rowHeight) + (shakeHealth ? RANDOM.nextInt(2) : 0);
        }
        for (int i = 0; i < FOOD_Y.length; i++) {
            FOOD_Y[i] = shakeFood ? RANDOM.nextInt(3) - 1 : 0;
        }
    }

    private static void tickFlash() {
        unclampedFlash += flashDir * 0.125f;
        if (unclampedFlash >= 1.5f) {
            flashDir = -1;
        } else if (unclampedFlash <= -0.5f) {
            flashDir = 1;
        }
        flashAlpha = Math.max(0.0f, Math.min(1.0f, unclampedFlash)) * MAX_FLASH_ALPHA;
    }

    private static void resetFlash() {
        unclampedFlash = 0.0f;
        flashAlpha = 0.0f;
        flashDir = 1;
    }

    // ---- Essen ----

    private record HeldFood(FoodComponent food, ConsumableComponent consumable, boolean rotten) {
    }

    /** Essen in der Haupthand, sonst in der Nebenhand. Nur, wenn man es gerade essen kann. */
    private static @Nullable HeldFood heldFood(PlayerEntity player) {
        HeldFood food = food(player.getMainHandStack());
        if (food == null || !player.canConsume(food.food().canAlwaysEat())) {
            food = food(player.getOffHandStack());
        }
        if (food == null || !player.canConsume(food.food().canAlwaysEat())) {
            return null;
        }
        return food;
    }

    private static @Nullable HeldFood food(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (stack.isEmpty() || food == null) {
            return null;
        }
        ConsumableComponent consumable = stack.getOrDefault(DataComponentTypes.CONSUMABLE, ConsumableComponents.FOOD);
        return new HeldFood(food, consumable, isRotten(consumable));
    }

    private static boolean isRotten(ConsumableComponent consumable) {
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyEffectsConsumeEffect apply) {
                for (StatusEffectInstance instance : apply.effects()) {
                    if (instance.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean canShowHealth(PlayerEntity player) {
        // Bei unendlich vielen Herzen (kaputte Werte) zeichnet auch Vanilla nichts
        if (heartX.length == 0) {
            return false;
        }
        if (player.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // Bei vollen Keulen, Gift, Wither oder Regeneration aendert sich das Leben ohnehin staendig
        return player.getHungerManager().getFoodLevel() < 18
            && !player.hasStatusEffect(StatusEffects.POISON)
            && !player.hasStatusEffect(StatusEffects.WITHER)
            && !player.hasStatusEffect(StatusEffects.REGENERATION);
    }

    /** Geschaetzte Heilung: natuerliche Regeneration durch die neuen Keulen plus Regeneration vom Essen (z. B. Goldapfel). */
    private static float estimatedHealing(PlayerEntity player, HeldFood held) {
        if (!player.canFoodHeal()) {
            return 0.0f;
        }
        HungerManager hunger = player.getHungerManager();
        int foodLevel = Math.min(hunger.getFoodLevel() + held.food().nutrition(), 20);
        float healing = 0.0f;
        if (foodLevel >= 18) {
            float saturation = Math.min(hunger.getSaturationLevel() + held.food().saturation(), (float) foodLevel);
            healing = naturalRegen(foodLevel, saturation, 0.0f);
        }
        for (ConsumeEffect effect : held.consumable().onConsumeEffects()) {
            if (effect instanceof ApplyEffectsConsumeEffect apply) {
                for (StatusEffectInstance instance : apply.effects()) {
                    if (instance.getEffectType() == StatusEffects.REGENERATION) {
                        healing += (float) Math.floor(instance.getDuration() / (double) Math.max(50 >> instance.getAmplifier(), 1));
                        break;
                    }
                }
            }
        }
        return healing;
    }

    /** Wie viel Leben die Keulen ueber 18 noch heilen, bevor die natuerliche Regeneration aufhoert. */
    private static float naturalRegen(int foodLevel, float saturation, float exhaustion) {
        float health = 0.0f;
        if (!Float.isFinite(exhaustion) || !Float.isFinite(saturation)) {
            return 0.0f;
        }
        while (foodLevel >= 18) {
            while (exhaustion > MAX_EXHAUSTION) {
                exhaustion -= MAX_EXHAUSTION;
                if (saturation > 0) {
                    saturation = Math.max(saturation - 1, 0);
                } else {
                    foodLevel -= 1;
                }
            }
            if (foodLevel >= 20 && Float.compare(saturation, Float.MIN_NORMAL) > 0) {
                // Schnelle Regeneration: mehrere Schritte auf einmal, sonst waeren es Millionen Durchlaeufe
                float limited = Math.min(saturation, REGEN_EXHAUSTION);
                float untilMax = Math.nextUp(MAX_EXHAUSTION) - exhaustion;
                int steps = Math.max(1, (int) Math.ceil(untilMax / limited));
                health += (limited / REGEN_EXHAUSTION) * steps;
                exhaustion += limited * steps;
            } else if (foodLevel >= 18) {
                health += 1;
                exhaustion += REGEN_EXHAUSTION;
            }
        }
        return health;
    }

    // ---- Tooltip ----

    private static void addTooltip(ItemStack stack, List<Text> lines) {
        if (!ModSetting.Toggle.APPLESKIN_TOOLTIP.isActive()) {
            return;
        }
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null || (food.nutrition() <= 0 && food.saturation() <= 0)) {
            return;
        }
        String saturation = String.format(Locale.ROOT, "%.1f", food.saturation());
        lines.add(Text.translatable("arnestyles.appleskin.tooltip", food.nutrition(), saturation).formatted(Formatting.GOLD));
    }
}
