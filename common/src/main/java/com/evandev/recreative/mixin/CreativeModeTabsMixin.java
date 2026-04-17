package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
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

        List<String> order = CreativeTabManager.TAB_ORDER;
        Set<String> removed = CreativeTabManager.REMOVED_TABS;

        List<CreativeModeTab> allTabs = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
        allTabs.addAll(CreativeTabManager.RUNTIME_TABS.values());

        allTabs.sort((t1, t2) -> recreative$sortTabs(t1, t2, order, allTabs));

        for (CreativeModeTab tab : allTabs) {
            String tabId = CreativeTabManager.getTabId(tab);
            if (!SPECIAL_TABS.contains(tabId) && !removed.contains(tabId) && tab.shouldDisplay()) {
                cir.setReturnValue(tab);
                return;
            }
        }
    }

    @Inject(method = "tabs", at = @At("RETURN"), cancellable = true)
    private static void recreative$repackTabs(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        if (!ModConfig.get().enabled) return;

        List<CreativeModeTab> original = new ArrayList<>(cir.getReturnValue());

        List<CreativeModeTab> filtered = new ArrayList<>();
        List<String> order = CreativeTabManager.TAB_ORDER;
        Set<String> removed = CreativeTabManager.REMOVED_TABS;

        for (CreativeModeTab tab : original) {
            String id = CreativeTabManager.getTabId(tab);
            if (!removed.contains(id)) {
                filtered.add(tab);
            }
        }

        filtered.sort((t1, t2) -> recreative$sortTabs(t1, t2, order, original));

        int visibleSlot = 0;
        for (CreativeModeTab tab : filtered) {
            String id = CreativeTabManager.getTabId(tab);
            if (SPECIAL_TABS.contains(id)) continue;

            int pageSlot = visibleSlot % 10;
            CreativeModeTab.Row row = pageSlot < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            int column = pageSlot % 5;

            ((CreativeModeTabAccessor) tab).setRow(row);
            ((CreativeModeTabAccessor) tab).setColumn(column);
            visibleSlot++;
        }

        cir.setReturnValue(filtered);
    }

    @Unique
    private static int recreative$sortTabs(CreativeModeTab t1, CreativeModeTab t2, List<String> order, List<CreativeModeTab> nativeOrder) {
        String id1 = CreativeTabManager.getTabId(t1);
        String id2 = CreativeTabManager.getTabId(t2);

        boolean spec1 = SPECIAL_TABS.contains(id1);
        boolean spec2 = SPECIAL_TABS.contains(id2);
        if (spec1 && !spec2) return 1;
        if (!spec1 && spec2) return -1;

        int idx1 = order != null ? order.indexOf(id1) : -1;
        int idx2 = order != null ? order.indexOf(id2) : -1;

        if (idx1 == -1 && idx2 == -1) {
            return Integer.compare(nativeOrder.indexOf(t1), nativeOrder.indexOf(t2));
        }
        if (idx1 == -1) return 1;
        if (idx2 == -1) return -1;
        return Integer.compare(idx1, idx2);
    }
}