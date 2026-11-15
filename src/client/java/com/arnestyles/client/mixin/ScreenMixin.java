package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.InventoryScale;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Inventory Scale: Container-Fenster bekommen die logische Groesse "Bildschirm / Faktor". */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    private int arnestyles$scaled(int size) {
        float factor = InventoryScale.factor((Screen) (Object) this);
        return factor == 1.0f ? size : (int) Math.ceil(size / factor);
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int arnestyles$initWidth(int width) {
        return arnestyles$scaled(width);
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int arnestyles$initHeight(int height) {
        return arnestyles$scaled(height);
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int arnestyles$resizeWidth(int width) {
        return arnestyles$scaled(width);
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int arnestyles$resizeHeight(int height) {
        return arnestyles$scaled(height);
    }
}
