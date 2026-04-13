package com.evandev.recreative.mixin.accessor;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {

    @Accessor("CACHED_PARAMETERS")
    static CreativeModeTab.ItemDisplayParameters getCachedParameters() {
        throw new AssertionError("Mixin failed to apply");
    }
}