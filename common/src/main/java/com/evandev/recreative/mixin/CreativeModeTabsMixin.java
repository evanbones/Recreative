package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.evandev.recreative.mixin.accessor.CreativeModeTabsInvoker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {

    // These tabs have native UI behaviors and must NOT be moved visually
    @Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "validate", at = @At("HEAD"))
    private static void safeRepackPageOne(CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;
        List<String> order = CreativeTabManager.getConfig().tabOrder;

        List<CreativeModeTab> allTabs = CreativeModeTabsInvoker.invokeStreamAllTabs().toList();
        List<CreativeModeTab> sorted = recreative$sortCreativeModeTabs(allTabs, order);

        int pageOneSlot = 0;

        for (CreativeModeTab tab : sorted) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            String tabId = id != null ? id.toString() : "";

            if (CreativeTabManager.getConfig().removedTabs.contains(tabId)) continue;

            if (SPECIAL_TABS.contains(tabId)) continue;

            if (pageOneSlot >= 10) break;

            CreativeModeTab.Row row = pageOneSlot < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            int column = pageOneSlot % 5;

            ((CreativeModeTabAccessor) tab).setRow(row);
            ((CreativeModeTabAccessor) tab).setColumn(column);

            pageOneSlot++;
        }
    }

    @Unique
    private static @NotNull List<CreativeModeTab> recreative$sortCreativeModeTabs(List<CreativeModeTab> allTabs, List<String> order) {
        List<CreativeModeTab> sorted = new ArrayList<>(allTabs);

        sorted.sort((tab1, tab2) -> {
            ResourceLocation id1 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab1);
            ResourceLocation id2 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab2);

            int idx1 = (id1 != null && order != null) ? order.indexOf(id1.toString()) : -1;
            int idx2 = (id2 != null && order != null) ? order.indexOf(id2.toString()) : -1;

            if (idx1 == -1 && idx2 == -1) return 0;
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });
        return sorted;
    }
}