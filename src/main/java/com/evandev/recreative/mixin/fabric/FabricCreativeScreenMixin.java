package com.evandev.recreative.mixin.fabric;

//? if fabric {
/*import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21 {
/^import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
^///?} else if <26.1 {
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroupImpl;
//?} else {
/^import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
^///?}

@Mixin(CreativeModeInventoryScreen.class)
public class FabricCreativeScreenMixin {
    //? if <1.21 {
    /^@Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$assignFabricPages(CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;

        List<CreativeModeTab> visibleTabs = CreativeModeTabs.tabs();
        int visibleIndex = 0;

        for (CreativeModeTab tab : visibleTabs) {
            String id = CreativeTabManager.getTabId(tab);
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
            String id = CreativeTabManager.getTabId(tab);
            return !SPECIAL_TABS.contains(id) && ((FabricItemGroup) tab).getPage() > 0;
        });

        if (hasExtraPages) {
            cir.setReturnValue(true);
        }
    }
    ^///?} else {
    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$assignFabricPages(CallbackInfo ci) {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            //? if <26.1 {
            FabricItemGroupImpl fabricTab = (FabricItemGroupImpl) tab;
            //?} else {
            /^FabricCreativeModeTabImpl fabricTab = (FabricCreativeModeTabImpl) tab;
            ^///?}
            try {
                fabricTab.fabric_getPage();
            } catch (IllegalStateException e) {
                fabricTab.fabric_setPage(-2);
            }
        }

        int visibleCustomIndex = 0;
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            //? if <26.1 {
            if (FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab)) continue;
            ((FabricItemGroupImpl) tab).fabric_setPage(visibleCustomIndex / 10);
            //?} else {
            /^if (FabricCreativeGuiComponents.COMMON_TABS.contains(tab)) continue;
            ((FabricCreativeModeTabImpl) tab).fabric_setPage(visibleCustomIndex / 10);
            ^///?}

            visibleCustomIndex++;
        }
    }
    //?}
}
*///?}
