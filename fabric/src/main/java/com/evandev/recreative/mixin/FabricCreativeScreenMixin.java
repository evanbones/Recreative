package com.evandev.recreative.mixin;

import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class FabricCreativeScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$assignFabricPages(CallbackInfo ci) {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            FabricCreativeModeTabImpl fabricTab = (FabricCreativeModeTabImpl) tab;
            try {
                fabricTab.fabric_getPage();
            } catch (IllegalStateException e) {
                fabricTab.fabric_setPage(-2);
            }
        }

        int visibleCustomIndex = 0;
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (FabricCreativeGuiComponents.COMMON_TABS.contains(tab)) continue;

            int page = visibleCustomIndex / 10;
            ((FabricCreativeModeTabImpl) tab).fabric_setPage(page);

            visibleCustomIndex++;
        }
    }
}