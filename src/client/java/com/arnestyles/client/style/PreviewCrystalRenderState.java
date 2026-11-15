package com.arnestyles.client.style;

import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Render-State fuer den Vorschau-Kristall im Menue. Traegt die Textur des Styles,
 * EndCrystalEntityRendererMixin verwendet sie statt der normalen Kristall-Textur.
 */
public class PreviewCrystalRenderState extends EndCrystalEntityRenderState {
    public @Nullable Identifier texture;
}
