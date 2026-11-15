package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.PvpVisuals;
import com.arnestyles.client.style.PreviewCrystalRenderState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Endkristall:
 * - Vorschau im Menue mit der Textur des angeklickten Styles (echte Kristalle nicht betroffen).
 * - PvP-Optik: Groesse, Dreh-Tempo und Huepfen (nur bei dir sichtbar).
 */
@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    private static final String RENDER =
        "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V";

    @ModifyArg(
        method = RENDER,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"),
        index = 3
    )
    private RenderLayer arnestyles$previewTexture(RenderLayer layer, @Local(argsOnly = true) EndCrystalEntityRenderState state) {
        if (state instanceof PreviewCrystalRenderState preview && preview.texture != null) {
            return RenderLayers.entityCutoutNoCull(preview.texture);
        }
        return layer;
    }

    /** Groesse: Minecraft zeichnet den Kristall doppelt so gross, das wird mit dem Regler multipliziert. */
    @ModifyArgs(
        method = RENDER,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0),
        require = 0
    )
    private void arnestyles$crystalSize(Args args) {
        float size = PvpVisuals.crystalSize();
        if (size != 1.0f) {
            args.set(0, (float) args.get(0) * size);
            args.set(1, (float) args.get(1) * size);
            args.set(2, (float) args.get(2) * size);
        }
    }

    /** Dreh-Tempo: das Alter steuert Drehung und Huepfen. */
    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void arnestyles$crystalSpeed(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo ci) {
        float speed = PvpVisuals.crystalSpeed();
        if (speed != 1.0f) {
            state.age = (entity.endCrystalAge + tickProgress) * speed;
        }
    }

    /** Ohne Huepfen: immer auf mittlerer Hoehe. */
    @Inject(method = "getYOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private static void arnestyles$noBounce(float age, CallbackInfoReturnable<Float> cir) {
        if (!PvpVisuals.crystalBounce()) {
            cir.setReturnValue(-1.1f);
        }
    }
}
