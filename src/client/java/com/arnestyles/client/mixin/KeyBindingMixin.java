package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.InfoHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jeder Tastendruck im Spiel (auch Maustasten): fuer den CPS-Zaehler, auch bei Klicks schneller als ein Tick. */
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(method = "onKeyPressed", at = @At("HEAD"), require = 0)
    private static void arnestyles$countClick(InputUtil.Key key, CallbackInfo ci) {
        InfoHud.onKeyPressed(key);
    }
}
