package com.arnestyles.client.mixin;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animationen in der Egoperspektive (Seite Mods, Abschnitt Animationen). Nur Optik, am Spiel aendert sich nichts:
 * Wechsel-Animation abschwaechen, kein Absenken nach einem Schlag, Schlag-Bewegung und -Drehung, Ess-Animation,
 * Wippen beim Essen und eine feste Verschiebung beider Haende.
 */
@Mixin(HeldItemRenderer.class)
public abstract class FirstPersonAnimationMixin {
    private static boolean arnestyles$default(float value) {
        return Math.abs(value - 1.0f) < 0.001f;
    }

    /** Wechsel-Animation: equipProgress (wie weit das Item abgesenkt ist) mit dem Regler verkleinern. */
    @ModifyArg(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"),
        index = 6)
    private float arnestyles$equipAmount(float equipProgress) {
        float amount = ConfigManager.get().animEquip;
        return arnestyles$default(amount) ? equipProgress : equipProgress * Math.max(0.0f, amount);
    }

    /** Kein Absenken nach einem Schlag: die Abklingzeit-Anzeige der Hand immer als "voll" behandeln. */
    @ModifyExpressionValue(method = "updateHeldItems", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/ClientPlayerEntity;getHandEquippingProgress(F)F"))
    private float arnestyles$noCooldownDip(float progress) {
        return ConfigManager.get().animNoCooldown ? 1.0f : progress;
    }

    /** Beide Haende fest verschieben (nach dem push am Anfang, gilt fuer alles in der Hand). */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER, ordinal = 0))
    private void arnestyles$handOffset(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress,
                                       ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                       CallbackInfo ci) {
        ArneStylesConfig c = ConfigManager.get();
        if (c.handX == 0.0f && c.handY == 0.0f && c.handZ == 0.0f) {
            return;
        }
        boolean right = (hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite()) == Arm.RIGHT;
        matrices.translate(right ? c.handX : -c.handX, c.handY, c.handZ);
    }

    /** Schlag: Bewegung und Drehung einzeln staerker oder schwaecher (eigene Rechnung, sonst wie Minecraft). */
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void arnestyles$swing(float swingProgress, MatrixStack matrices, int side, Arm arm, CallbackInfo ci) {
        ArneStylesConfig c = ConfigManager.get();
        float move = c.animSwingMove;
        float rot = c.animSwingRotation;
        if (arnestyles$default(move) && arnestyles$default(rot)) {
            return;
        }
        ci.cancel();
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float g = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
        float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate(side * f * move, g * move, h * move);
        int i = arm == Arm.RIGHT ? 1 : -1;
        float a = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + a * -20.0F * rot)));
        float b = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * b * -20.0F * rot));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(b * -80.0F * rot));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }

    /** Essen/Trinken: wie weit das Item zum Mund wandert und ob es wippt. */
    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void arnestyles$eat(MatrixStack matrices, float tickProgress, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        ArneStylesConfig c = ConfigManager.get();
        float amount = c.animEat;
        if (arnestyles$default(amount) && c.animEatBob) {
            return;
        }
        ci.cancel();
        float f = player.getItemUseTimeLeft() - tickProgress + 1.0F;
        float g = f / stack.getMaxUseTime(player);
        if (g < 0.8F && c.animEatBob) {
            float bob = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, bob, 0.0F);
        }
        float h = (1.0F - (float) Math.pow(g, 27.0)) * amount;
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * i, h * -0.5F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * h * 30.0F));
    }
}
