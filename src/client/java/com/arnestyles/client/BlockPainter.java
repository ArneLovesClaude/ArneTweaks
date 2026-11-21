package com.arnestyles.client;

import com.arnestyles.client.config.ConfigManager;
import com.arnestyles.client.config.StyleCategory;
import com.arnestyles.client.gui.DrawScreen;
import com.arnestyles.client.gui.StylesScreen;
import com.arnestyles.client.style.PackImporter;
import com.arnestyles.client.style.StyleRegistry;
import com.arnestyles.client.style.TintedTextures;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * "Block bemalen": auf einen Block schauen, Taste druecken (Standard B) und die Textur genau dieser Blockseite
 * im Zeichen-Editor bemalen. Gespeichert wird als eigener Style in der passenden Kategorie (z. B. Obsidian),
 * sonst unter "Blocks". Der bisher gewaehlte Style der Kategorie wird mitkopiert, damit nichts verloren geht.
 */
public final class BlockPainter {
    private BlockPainter() {
    }

    public static void paintTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            say(client, Text.translatable("arnestyles.paint.no_block").formatted(Formatting.GRAY));
            return;
        }
        BlockState state = client.world.getBlockState(blockHit.getBlockPos());
        String asset = textureOf(client, state, blockHit.getSide());
        if (asset == null) {
            say(client, Text.translatable("arnestyles.paint.no_texture").formatted(Formatting.RED));
            return;
        }
        StyleCategory category = categoryFor(asset);
        // Menue im Customize-Tab dieser Kategorie als Eltern-Fenster: nach dem Speichern sieht man den neuen Style
        ConfigManager.get().activeTab = "T:CUSTOMIZE";
        ConfigManager.get().activeCategory = category.name();
        StylesScreen parent = new StylesScreen();
        Text error = DrawScreen.open(parent, category, StyleRegistry.getSelected(category), asset, true);
        if (error != null) {
            say(client, error.copy().formatted(Formatting.RED));
        }
    }

    /** Textur der angeschauten Seite (Pfad unter assets/minecraft/), oder null bei Modellen ohne passende Textur. */
    static @Nullable String textureOf(MinecraftClient client, BlockState state, Direction side) {
        BlockStateModel model = client.getBlockRenderManager().getModel(state);
        List<BlockModelPart> parts = model.getParts(Random.create(42L));
        Sprite sprite = null;
        for (BlockModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(side);
            if (quads.isEmpty()) {
                quads = part.getQuads(null);
            }
            // Bei Bloecken mit Overlay (Gras-Seite) zaehlt die erste, untere Ebene
            if (!quads.isEmpty()) {
                sprite = quads.get(0).sprite();
                break;
            }
        }
        if (sprite == null) {
            sprite = model.particleSprite();
        }
        if (sprite == null) {
            return null;
        }
        Identifier id = sprite.getContents().getId();
        if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || id.getPath().startsWith("missingno")) {
            return null;
        }
        return "textures/" + id.getPath() + ".png";
    }

    /** Kategorie, in die diese Textur gehoert (wie beim Pack-Import), sonst "Blocks". */
    static StyleCategory categoryFor(String asset) {
        String folder = PackImporter.categoryOf(asset);
        StyleCategory category = folder == null ? null : PackImporter.category(folder);
        if (category == null || !TintedTextures.customizable(category)) {
            return StyleCategory.BLOCKS;
        }
        return category;
    }

    private static void say(MinecraftClient client, Text text) {
        client.inGameHud.getChatHud().addMessage(Text.literal("[ArneTweaks] ").formatted(Formatting.LIGHT_PURPLE).append(text));
    }
}
