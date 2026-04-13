package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {

    @Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "getDefaultTab", at = @At("HEAD"), cancellable = true)
    private static void recreative$onGetDefaultTab(CallbackInfoReturnable<CreativeModeTab> cir) {
        if (!ModConfig.get().enabled) return;

        List<String> order = CreativeTabManager.getConfig().tabOrder;
        List<CreativeModeTab> sorted = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());

        sorted.sort((t1, t2) -> {
            ResourceLocation id1 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t1);
            ResourceLocation id2 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t2);
            int idx1 = (id1 != null && order != null) ? order.indexOf(id1.toString()) : -1;
            int idx2 = (id2 != null && order != null) ? order.indexOf(id2.toString()) : -1;
            if (idx1 == -1 && idx2 == -1) return 0;
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });

        for (CreativeModeTab tab : sorted) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            String tabId = id != null ? id.toString() : "";

            if (!SPECIAL_TABS.contains(tabId) && !CreativeTabManager.getConfig().removedTabs.contains(tabId) && tab.shouldDisplay()) {
                cir.setReturnValue(tab);
                return;
            }
        }
    }
}