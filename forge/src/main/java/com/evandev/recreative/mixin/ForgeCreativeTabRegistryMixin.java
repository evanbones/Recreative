package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeTabRegistry.class)
public class ForgeCreativeTabRegistryMixin {

    @Unique
    private static final Set<String> SPECIAL_TABS = Set.of(
            "minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks"
    );

    @Inject(method = "getSortedCreativeModeTabs", at = @At("RETURN"), cancellable = true, remap = false)
    private static void applyRecreativeSorting(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        if (!ModConfig.get().enabled) return;

        List<CreativeModeTab> original = new ArrayList<>(cir.getReturnValue());

        for (CreativeModeTab customTab : CreativeTabManager.RUNTIME_TABS.values()) {
            if (!original.contains(customTab)) {
                original.add(customTab);
            }
        }

        List<CreativeModeTab> filtered = new ArrayList<>();
        List<String> order = CreativeTabManager.TAB_ORDER;
        Set<String> removed = CreativeTabManager.REMOVED_TABS;

        for (CreativeModeTab tab : original) {
            String id = CreativeTabManager.getTabId(tab);
            if (!removed.contains(id)) {
                filtered.add(tab);
            }
        }

        filtered.sort((t1, t2) -> {
            String id1 = CreativeTabManager.getTabId(t1);
            String id2 = CreativeTabManager.getTabId(t2);

            boolean spec1 = SPECIAL_TABS.contains(id1);
            boolean spec2 = SPECIAL_TABS.contains(id2);
            if (spec1 && !spec2) return 1;
            if (!spec1 && spec2) return -1;

            int idx1 = order.indexOf(id1);
            int idx2 = order.indexOf(id2);

            if (idx1 == -1 && idx2 == -1) return Integer.compare(original.indexOf(t1), original.indexOf(t2));
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });

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
}