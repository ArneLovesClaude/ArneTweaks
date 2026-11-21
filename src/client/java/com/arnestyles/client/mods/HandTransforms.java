package com.arnestyles.client.mods;

import com.arnestyles.client.config.ArneStylesConfig;
import com.arnestyles.client.config.ConfigManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.jspecify.annotations.Nullable;

/** Hand-Editor: eigene Position, Drehung und Groesse pro Item in der Hand (nur Optik, nur Egoperspektive). */
public final class HandTransforms {
    private HandTransforms() {
    }

    public static String key(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    /** Eingestellte Werte fuer dieses Item oder null, wenn nichts veraendert ist. */
    public static ArneStylesConfig.@Nullable HandTransform get(ItemStack stack) {
        ArneStylesConfig config = ConfigManager.get();
        if (!config.modEnabled || stack.isEmpty()) {
            return null;
        }
        ArneStylesConfig.HandTransform t = config.handTransforms.get(key(stack));
        return t == null || t.isDefault() ? null : t;
    }

    /** Werte zum Bearbeiten (legt einen Eintrag an, falls noetig). */
    public static ArneStylesConfig.HandTransform edit(ItemStack stack) {
        return ConfigManager.get().handTransforms.computeIfAbsent(key(stack), k -> new ArneStylesConfig.HandTransform());
    }

    public static void reset(ItemStack stack) {
        ConfigManager.get().handTransforms.remove(key(stack));
    }
}
