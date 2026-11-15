package com.arnestyles.client.mods;

import com.arnestyles.ArneStyles;
import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Totem Pop Chams: Wenn ein Spieler einen Totem poppt, bleibt an der Stelle fuer kurze Zeit eine
 * farbige, durchsichtige Silhouette in genau seiner Pose stehen. Optional verblasst sie und fliegt auseinander.
 *
 * Idee und Ablauf nach "TotemPopChams" von Walksy (MIT-Lizenz, github.com/Walksy/TotemPopChams),
 * fuer ArneTweaks und Yarn-Mappings neu geschrieben, ohne WalksyLib.
 */
public final class PopChams {
    private static final Identifier WHITE = Identifier.of(ArneStyles.MOD_ID, "textures/misc/white.png");
    private static final int FULL_LIGHT = 0xF000F0;
    /** Wie weit die Teile beim Auseinanderfliegen hoechstens fliegen (Bloecke). */
    private static final double DISPERSE_DISTANCE = 1.5;

    /** Eine Silhouette: fertig posiertes Modell, Position, Drehung, Startzeit. */
    private record Cham(PlayerEntityModel model, double x, double y, double z, float bodyYaw, long startMs, long seed) {
    }

    private static final List<Cham> CHAMS = new CopyOnWriteArrayList<>();
    /** Modell-Ebene fuer schlanke Skins (in den Yarn-Namen gibt es zwei Felder namens PLAYER_SLIM). */
    private static final EntityModelLayer SLIM_LAYER = new EntityModelLayer(Identifier.ofVanilla("player_slim"), "main");

    private PopChams() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(PopChams::render);
    }

    /** Aufruf beim Totem-Pop (Entity-Status 35). */
    public static void onTotemPop(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModSetting.Toggle.POP_CHAMS.isActive() || !(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        if (player == client.player && !ModSetting.Toggle.POP_CHAMS_OWN.value()) {
            return;
        }
        try {
            CHAMS.add(capture(client, player));
        } catch (RuntimeException e) {
            ArneStyles.LOGGER.warn("Pop Cham konnte nicht erstellt werden.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Cham capture(MinecraftClient client, AbstractClientPlayerEntity player) {
        EntityRenderer<? super AbstractClientPlayerEntity, ?> renderer = client.getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState raw = renderer.getAndUpdateRenderState(player, 1.0f);
        PlayerEntityRenderState state = (PlayerEntityRenderState) raw;
        boolean slim = state.skinTextures.model() == PlayerSkinType.SLIM;
        PlayerEntityModel model = new PlayerEntityModel(client.getLoadedEntityModels().getModelPart(slim ? SLIM_LAYER : EntityModelLayers.PLAYER), slim);
        model.setAngles(state);
        // Nur die Grundform zeigen, ohne zweite Hautschicht
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        return new Cham(model, player.getX(), player.getY(), player.getZ(), state.bodyYaw, System.currentTimeMillis(),
            player.getUuid().getMostSignificantBits() ^ System.nanoTime());
    }

    private static void render(WorldRenderContext context) {
        if (CHAMS.isEmpty()) {
            return;
        }
        ArneStylesConfig config = ConfigManager.get();
        long now = System.currentTimeMillis();
        float lifetime = config.popChamsLifetime;
        CHAMS.removeIf(cham -> (now - cham.startMs()) / 1000.0f >= lifetime || !ModSetting.Toggle.POP_CHAMS.isActive());
        if (CHAMS.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.entityTranslucent(WHITE));
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();

        for (Cham cham : CHAMS) {
            float age = (now - cham.startMs()) / 1000.0f;
            float progress = age / lifetime;
            float alpha = config.popChamsOpacity;
            if (config.popChamsFade && progress > 0.6f) {
                alpha *= 1.0f - (progress - 0.6f) / 0.4f;
            }
            int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
            int color = (a << 24) | (config.popChamsColor & 0xFFFFFF);

            matrices.push();
            matrices.translate(cham.x() - camera.x, cham.y() - camera.y, cham.z() - camera.z);
            // Gleiche Ausrichtung wie beim normalen Zeichnen eines Spielers
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - cham.bodyYaw()));
            matrices.scale(-1.0f, -1.0f, 1.0f);
            matrices.scale(0.9375f, 0.9375f, 0.9375f);
            matrices.translate(0.0f, -1.501f, 0.0f);

            PlayerEntityModel model = cham.model();
            if (config.popChamsDisperse) {
                double distance = Math.min(1.0, progress * 1.6) * DISPERSE_DISTANCE / 0.9375;
                Random random = Random.create(cham.seed());
                ModelPart[] parts = {model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg};
                double[][] directions = {{0, -1, 0}, {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0.3, 1, 0}, {-0.3, 1, 0}};
                for (int i = 0; i < parts.length; i++) {
                    double dx = directions[i][0] + (random.nextDouble() - 0.5) * 0.5;
                    double dy = directions[i][1] + (random.nextDouble() - 0.5) * 0.5;
                    double dz = directions[i][2] + (random.nextDouble() - 0.5) * 0.5;
                    double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
                    matrices.push();
                    matrices.translate(dx / len * distance, dy / len * distance, dz / len * distance);
                    parts[i].render(matrices, buffer, FULL_LIGHT, OverlayTexture.DEFAULT_UV, color);
                    matrices.pop();
                }
            } else {
                model.getRootPart().render(matrices, buffer, FULL_LIGHT, OverlayTexture.DEFAULT_UV, color);
            }
            matrices.pop();
        }
    }
}
