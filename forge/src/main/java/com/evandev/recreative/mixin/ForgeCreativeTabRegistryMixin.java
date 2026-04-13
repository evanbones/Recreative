package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
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
        List<CreativeModeTab> filtered = new ArrayList<>();
        List<String> order = CreativeTabManager.getConfig().tabOrder;
        List<String> removed = CreativeTabManager.getConfig().removedTabs;

        for (CreativeModeTab tab : original) {
            String id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
            if (!removed.contains(id)) {
                filtered.add(tab);
            }
        }

        filtered.sort((t1, t2) -> {
            String id1 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t1).toString();
            String id2 = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t2).toString();

            boolean spec1 = SPECIAL_TABS.contains(id1);
            boolean spec2 = SPECIAL_TABS.contains(id2);
            if (spec1 && !spec2) return 1;
            if (!spec1 && spec2) return -1;

            int idx1 = order != null ? order.indexOf(id1) : -1;
            int idx2 = order != null ? order.indexOf(id2) : -1;

            if (idx1 == -1 && idx2 == -1) return Integer.compare(original.indexOf(t1), original.indexOf(t2));
            if (idx1 == -1) return 1;
            if (idx2 == -1) return -1;
            return Integer.compare(idx1, idx2);
        });

        cir.setReturnValue(filtered);
    }
}