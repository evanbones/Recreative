package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        List<CreativeModeTab> original = cir.getReturnValue();
        List<CreativeModeTab> sorted = new ArrayList<>(original);
        List<String> order = CreativeTabManager.getConfig().tabOrder;
        List<String> removed = CreativeTabManager.getConfig().removedTabs;

        sorted.sort((t1, t2) -> {
            String id1 = Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t1)).toString();
            String id2 = Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t2)).toString();

            boolean isSpecial1 = SPECIAL_TABS.contains(id1);
            boolean isSpecial2 = SPECIAL_TABS.contains(id2);
            if (isSpecial1 && !isSpecial2) return 1;
            if (!isSpecial1 && isSpecial2) return -1;

            boolean hidden1 = removed.contains(id1) || !t1.shouldDisplay();
            boolean hidden2 = removed.contains(id2) || !t2.shouldDisplay();
            if (hidden1 && !hidden2) return 1;
            if (!hidden1 && hidden2) return -1;

            int idx1 = order != null ? order.indexOf(id1) : -1;
            int idx2 = order != null ? order.indexOf(id2) : -1;

            if (idx1 == -1 && idx2 == -1) return Integer.compare(original.indexOf(t1), original.indexOf(t2));
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });

        int visibleSlot = 0;
        int hiddenSlot = 100;

        for (CreativeModeTab tab : sorted) {
            String id = Objects.requireNonNull(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)).toString();
            if (SPECIAL_TABS.contains(id)) continue;

            if (removed.contains(id) || !tab.shouldDisplay()) {
                ((CreativeModeTabAccessor) tab).setRow(CreativeModeTab.Row.TOP);
                ((CreativeModeTabAccessor) tab).setColumn(hiddenSlot++);
                continue;
            }

            int pageSlot = visibleSlot % 10;
            CreativeModeTab.Row row = pageSlot < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            int column = pageSlot % 5;

            ((CreativeModeTabAccessor) tab).setRow(row);
            ((CreativeModeTabAccessor) tab).setColumn(column);
            visibleSlot++;
        }

        cir.setReturnValue(sorted);
    }
}