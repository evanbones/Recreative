package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(CreativeModeInventoryScreen.class)
public class FabricCreativeScreenMixin {

    @Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$repackFabricPages(CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;

        List<CreativeModeTab> visibleTabs = CreativeModeTabs.tabs();
        int visibleIndex = 0;

        for (CreativeModeTab tab : visibleTabs) {
            String id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            if (SPECIAL_TABS.contains(id)) continue;

            int page = visibleIndex / 10;
            ((FabricItemGroup) tab).setPage(page);
            visibleIndex++;
        }
    }

    @Dynamic("Added by Fabric API")
    @Inject(method = "fabric_isButtonVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private void recreative$fixPaginationButtons(FabricCreativeGuiComponents.Type type, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        boolean hasExtraPages = CreativeModeTabs.tabs().stream().anyMatch(tab -> {
            String id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            return !SPECIAL_TABS.contains(id) && ((FabricItemGroup) tab).getPage() > 0;
        });

        if (hasExtraPages) {
            cir.setReturnValue(true);
        }
    }
}