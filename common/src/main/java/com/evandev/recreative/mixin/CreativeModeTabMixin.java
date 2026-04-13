package com.evandev.recreative.mixin;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.data.TabConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {

    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;

    @Unique
    private String recreative$getTabId() {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(self);
        return id != null ? id.toString() : "";
    }

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void hideRemovedTabs(CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        if (CreativeTabManager.getConfig().removedTabs.contains(id)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void modifyDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        if (ModConfig.get().showInternalTabIds && !id.isEmpty()) {
            cir.setReturnValue(Component.literal(id));
            return;
        }

        TabConfig.ModifyTabDef modifier = CreativeTabManager.getConfig().modifyTabs.get(id);
        if (modifier != null && modifier.name != null && !modifier.name.isEmpty()) {
            cir.setReturnValue(Component.translatable(modifier.name));
        }
    }

    @Inject(method = "getIconItem", at = @At("RETURN"), cancellable = true)
    private void modifyIcon(CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        TabConfig.ModifyTabDef modifier = CreativeTabManager.getConfig().modifyTabs.get(id);
        if (modifier != null && modifier.icon != null && !modifier.icon.isEmpty()) {
            Item customIcon = BuiltInRegistries.ITEM.get(new ResourceLocation(modifier.icon));
            cir.setReturnValue(new ItemStack(customIcon));
        }
    }

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void postBuildContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        TabConfig.ModifyTabDef modifier = CreativeTabManager.getConfig().modifyTabs.get(id);
        if (modifier == null) return;

        if (!modifier.removeItems.isEmpty()) {
            this.displayItems.removeIf(stack -> {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return modifier.removeItems.contains(itemId.toString());
            });
            this.displayItemsSearchTab.removeIf(stack -> {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return modifier.removeItems.contains(itemId.toString());
            });
        }

        if (!modifier.addItems.isEmpty()) {
            for (String itemStr : modifier.addItems) {
                Item itemToAdd = BuiltInRegistries.ITEM.get(new ResourceLocation(itemStr));
                ItemStack stack = new ItemStack(itemToAdd);
                this.displayItems.add(stack);
                this.displayItemsSearchTab.add(stack);
            }
        }
    }
}