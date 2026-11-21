package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.ModSetting;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat-Upgrade: Uhrzeit vor jeder Nachricht und gleiche Nachrichten hintereinander zusammenfassen ("(x3)").
 * Nur Anzeige, gesendet wird nichts.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Unique
    private static final DateTimeFormatter ARNESTYLES_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ARNESTYLES_TIME_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Shadow
    protected abstract void refresh();

    @Unique
    private @Nullable String arnestyles$lastRaw;
    @Unique
    private int arnestyles$repeat = 1;

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"), argsOnly = true, require = 0)
    private Text arnestyles$decorate(Text message) {
        String raw = message.getString();
        Text shown = message;
        if (ModSetting.Toggle.CHAT_STACK.isActive() && raw.equals(arnestyles$lastRaw) && !messages.isEmpty()) {
            // Gleiche Nachricht wie eben: alte Zeile entfernen und mit Zaehler neu einfuegen
            arnestyles$repeat++;
            messages.remove(0);
            refresh();
            shown = message.copy().append(Text.literal(" (x" + arnestyles$repeat + ")").formatted(Formatting.GRAY));
        } else {
            arnestyles$repeat = 1;
        }
        arnestyles$lastRaw = raw;
        if (ModSetting.Toggle.CHAT_TIMESTAMPS.isActive()) {
            var config = com.arnestyles.client.config.ConfigManager.get();
            String clock = LocalTime.now().format(config.chatSeconds ? ARNESTYLES_TIME_SECONDS : ARNESTYLES_TIME);
            MutableText time = Text.literal("[" + clock + "] ").withColor(config.chatTimeRgb & 0xFFFFFF);
            shown = time.append(shown);
        }
        return shown;
    }
}
