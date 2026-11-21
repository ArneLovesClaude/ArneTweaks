package com.arnestyles.client.mixin;

import com.arnestyles.client.mods.PackSoundVolume;
import com.arnestyles.client.mods.ArneShaders;
import com.arnestyles.client.style.StyleRegistry;
import com.arnestyles.client.style.TintedTextures;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Haengt die aktiven Styles bei jedem Laden der Client-Ressourcen als oberste Packs an.
 * Dadurch ueberschreiben sie normale Texture-Packs, tauchen aber nicht in der Pack-Auswahl auf.
 */
@Mixin(ReloadableResourceManagerImpl.class)
public abstract class ReloadableResourceManagerImplMixin {
    @Shadow
    @Final
    private ResourceType type;

    @ModifyVariable(method = "reload", at = @At("HEAD"), argsOnly = true)
    private List<ResourcePack> arnestyles$addStylePacks(List<ResourcePack> packs) {
        if (this.type != ResourceType.CLIENT_RESOURCES) {
            return packs;
        }
        PackSoundVolume.clearCache();
        List<ResourcePack> stylePacks = StyleRegistry.createActivePacks();
        // ArneTweaks-Shader: Effekt-Beschreibungen fuer eingebaute und eigene Shader (ohne Iris)
        ResourcePack shaderPack = ArneShaders.createPack();
        // Eigene Farben (Stone, Deepslate, Gras, Feuer) ganz oben, damit sie ueber dem gewaehlten Style liegen
        ResourcePack tintPack = TintedTextures.createPack();
        ResourcePack soundPack = com.arnestyles.client.mods.TotemPop.createSoundPack();
        if (soundPack != null) {
            stylePacks = new ArrayList<>(stylePacks);
            stylePacks.add(soundPack);
        }
        if (stylePacks.isEmpty() && shaderPack == null && tintPack == null) {
            return packs;
        }
        List<ResourcePack> all = new ArrayList<>(packs);
        if (shaderPack != null) {
            all.add(shaderPack);
        }
        all.addAll(stylePacks);
        if (tintPack != null) {
            all.add(tintPack);
        }
        return all;
    }
}
