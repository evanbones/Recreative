package com.evandev.recreative.mixin.accessor;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.stream.Stream;

@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsInvoker {

    @Invoker("streamAllTabs")
    static Stream<CreativeModeTab> invokeStreamAllTabs() {
        throw new AssertionError("Mixin failed to apply");
    }
}