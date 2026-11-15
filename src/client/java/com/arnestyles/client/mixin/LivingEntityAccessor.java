package com.arnestyles.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf den aktiven Gegenstand anderer Spieler (fuer Shield Fixes). */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("activeItemStack")
    void arnestyles$setActiveItemStack(ItemStack stack);

    @Accessor("itemUseTimeLeft")
    void arnestyles$setItemUseTimeLeft(int ticks);
}
