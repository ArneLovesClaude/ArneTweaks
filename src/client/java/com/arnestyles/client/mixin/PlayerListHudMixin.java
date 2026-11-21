package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.mods.ModSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tab-Liste: Ping als Zahl (z. B. "42ms") statt der Balken, wahlweise farbig nach eigenen Grenzen.
 * Die Spalte fuer den Ping wird dafuer breiter gemacht (normal 13 Pixel fuer das Balken-Symbol).
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    /** Platz fuer "9999ms" statt des 10 Pixel breiten Symbols. */
    private static final int ARNESTYLES_PING_WIDTH = 34;

    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int arnestyles$widerPingColumn(int original) {
        return ModSetting.Toggle.TAB_PING.isActive() ? ARNESTYLES_PING_WIDTH : original;
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void arnestyles$pingNumber(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (!ModSetting.Toggle.TAB_PING.isActive()) {
            return;
        }
        ci.cancel();
        ArneStylesConfig config = ConfigManager.get();
        int latency = entry.getLatency();
        String text = latency < 0 ? "?" : latency + (config.tabPingSuffix ? "ms" : "");
        int color = 0xFFFFFFFF;
        if (config.tabPingColors) {
            if (latency < 0) {
                color = 0xFFAAAAAA;
            } else if (latency <= config.tabPingGood) {
                color = 0xFF55FF55;
            } else if (latency >= config.tabPingBad) {
                color = 0xFFFF5555;
            } else {
                color = 0xFFFFFF55;
            }
        }
        var font = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(font, text, x + width - 1 - font.getWidth(text), y, color);
    }
}
