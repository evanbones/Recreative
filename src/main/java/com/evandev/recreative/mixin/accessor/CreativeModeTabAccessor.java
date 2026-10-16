package com.evandev.recreative.mixin.accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {

    @Accessor("row")
    @Mutable
    void setRow(CreativeModeTab.Row row);

    @Accessor("column")
    @Mutable
    void setColumn(int column);

    @Accessor("displayItemsGenerator")
    CreativeModeTab.DisplayItemsGenerator getDisplayItemsGenerator();

    @Accessor("displayName")
    Component getRawDisplayName();

    @Accessor("iconGenerator")
    Supplier<ItemStack> getIconGenerator();
}