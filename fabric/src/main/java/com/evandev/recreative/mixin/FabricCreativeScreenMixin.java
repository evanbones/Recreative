package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeInventoryScreen.class)
public class FabricCreativeScreenMixin {

    @Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "init", at = @At("HEAD"))
    private void recreative$repackFabricTabs(CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;

        List<String> order = CreativeTabManager.getConfig().tabOrder;
        List<String> removed = CreativeTabManager.getConfig().removedTabs;
        List<CreativeModeTab> allTabs = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());

        allTabs.sort((t1, t2) -> {
            String id1 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t1).toString();
            String id2 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t2).toString();
            boolean spec1 = SPECIAL_TABS.contains(id1);
            boolean spec2 = SPECIAL_TABS.contains(id2);
            if (spec1 && !spec2) return 1;
            if (!spec1 && spec2) return -1;

            int idx1 = order != null ? order.indexOf(id1) : -1;
            int idx2 = order != null ? order.indexOf(id2) : -1;
            if (idx1 == -1 && idx2 == -1) return 0;
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });

        int visibleIndex = 0;
        int hiddenPage = 1000;

        for (CreativeModeTab tab : allTabs) {
            String tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            if (SPECIAL_TABS.contains(tabId)) continue;

            FabricItemGroup fabricTab = (FabricItemGroup) tab;

            if (removed.contains(tabId) || !tab.shouldDisplay()) {
                fabricTab.setPage(hiddenPage++);
                ((CreativeModeTabAccessor) tab).setRow(CreativeModeTab.Row.TOP);
                ((CreativeModeTabAccessor) tab).setColumn(0);
                continue;
            }

            int page = visibleIndex / 10;
            int positionOnPage = visibleIndex % 10;
            CreativeModeTab.Row row = positionOnPage < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            int column = positionOnPage % 5;

            fabricTab.setPage(page);
            ((CreativeModeTabAccessor) tab).setRow(row);
            ((CreativeModeTabAccessor) tab).setColumn(column);
            visibleIndex++;
        }
    }

    @Dynamic("Added by Fabric API")
    @Inject(method = "fabric_isButtonVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private void recreative$fixPaginationButtons(FabricCreativeGuiComponents.Type type, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        boolean hasExtraPages = CreativeModeTabs.allTabs().stream().anyMatch(tab -> {
            String id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            if (SPECIAL_TABS.contains(id)) return false;
            if (CreativeTabManager.getConfig().removedTabs.contains(id) || !tab.shouldDisplay()) return false;
            return ((FabricItemGroup) tab).getPage() > 0;
        });

        if (hasExtraPages) {
            cir.setReturnValue(true);
        }
    }
}