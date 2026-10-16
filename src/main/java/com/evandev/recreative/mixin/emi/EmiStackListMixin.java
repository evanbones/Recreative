package com.evandev.recreative.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * EMI builds its index from {@link CreativeModeTabs#allTabs()}, which ignores Recreative's tab order and removed tabs
 */
@IfModLoaded("emi")
@Mixin(targets = "dev.emi.emi.registry.EmiStackList")
public class EmiStackListMixin {

    @WrapOperation(
            method = "reload",
            remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;allTabs()Ljava/util/List;", remap = true)
    )
    private static List<CreativeModeTab> recreative$visibleTabsFirst(Operation<List<CreativeModeTab>> original) {
        Set<CreativeModeTab> ordered = new LinkedHashSet<>(CreativeModeTabs.tabs());
        ordered.addAll(original.call());
        return List.copyOf(ordered);
    }
}
