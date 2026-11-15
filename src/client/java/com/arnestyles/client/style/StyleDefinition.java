package com.arnestyles.client.style;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.StyleCategory;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Ein auswaehlbarer Style.
 *
 * @param id           Ordnername des Styles ("default" fuer normales Minecraft)
 * @param root         Style-Ordner mit assets/..., null beim Standard-Style
 * @param previewImage optionales preview.png im Style-Ordner
 * @param builtIn      true, wenn der Style in der Mod-Datei mitgeliefert wird
 * @param aliases      fruehere IDs, die auf diesen Style zeigen (zusammengelegte Packs)
 */
public record StyleDefinition(String id, StyleCategory category, Text name, @Nullable Path root, @Nullable Path previewImage, boolean builtIn,
                              List<String> aliases) {

    public static StyleDefinition vanilla(StyleCategory category) {
        return new StyleDefinition(ArneStylesConfig.DEFAULT_STYLE, category, Text.translatable("arnestyles.style.default"), null, null, false, List.of());
    }

    public boolean isVanilla() {
        return root == null;
    }
}
