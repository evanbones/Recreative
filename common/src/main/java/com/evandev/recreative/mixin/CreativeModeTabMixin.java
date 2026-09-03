package com.evandev.recreative.mixin;

import com.evandev.recreative.Constants;
import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.ComponentUtil;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.data.ItemEntry;
import com.evandev.recreative.data.ItemRef;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

import java.util.*;
import java.util.function.Predicate;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {

    @Shadow
    private Collection<ItemStack> displayItems;
    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Unique
    private String recreative$getTabId() {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        return CreativeTabManager.getTabId(self);
    }

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void hideRemovedTabs(CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        if (CreativeTabManager.REMOVED_TABS.contains(id)) {
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

        CreativeTabManager.TabModifier modifier = CreativeTabManager.TAB_MODIFIERS.get(id);
        if (modifier == null) modifier = CreativeTabManager.CUSTOM_TABS_DEFS.get(id);

        if (modifier != null && modifier.name != null && !modifier.name.isEmpty()) {
            cir.setReturnValue(Component.translatable(modifier.name));
        }
    }

    @Inject(method = "getIconItem", at = @At("RETURN"), cancellable = true)
    private void modifyIcon(CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
        CreativeTabManager.TabModifier modifier = CreativeTabManager.TAB_MODIFIERS.get(id);
        if (modifier == null) modifier = CreativeTabManager.CUSTOM_TABS_DEFS.get(id);

        if (modifier != null && modifier.icon != null && !modifier.icon.isEmpty()) {
            if (modifier.icon.endsWith(".png")) {
                ((ICustomIconTab) this).recreative$setCustomIcon(ResourceLocation.parse(modifier.icon));
                cir.setReturnValue(ItemStack.EMPTY);
            } else {
                Item customIcon = BuiltInRegistries.ITEM.get(ResourceLocation.parse(modifier.icon));
                cir.setReturnValue(new ItemStack(customIcon));
                ((ICustomIconTab) this).recreative$setCustomIcon(null);
            }
        }
    }

    @Unique
    private List<ItemStack> recreative$resolveStacksToAdd(ItemEntry entry, CreativeModeTab.ItemDisplayParameters parameters) {
        List<ItemStack> stacksToAdd = new ArrayList<>();
        if (entry == null || entry.item == null) return stacksToAdd;

        if (entry.item.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.item.substring(1)));
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                stacksToAdd.add(new ItemStack(holder.value()));
            }
        } else {
            Item itemToAdd = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item));
            stacksToAdd.add(new ItemStack(itemToAdd));
        }

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacksToAdd) {
            if (stack.isEmpty() || stack.getCount() != 1) continue;

            if (entry.components != null) {
                try {
                    JsonElement componentJson = JsonParser.parseString(entry.components);
                    DataComponentPatch patch = DataComponentPatch.CODEC.parse(
                            RegistryOps.create(JsonOps.INSTANCE, parameters.holders()),
                            componentJson
                    ).result().orElseThrow();
                    stack.applyComponents(patch);
                } catch (Exception e) {
                    Constants.LOG.error("Failed to parse components for item {}", entry.item, e);
                }
            }

            result.add(stack);
        }
        return result;
    }

    @Unique
    private void recreative$applyPlacement(List<ItemStack> items, List<ItemStack> resolvedStacks, ItemEntry entry,
                                           HolderLookup.Provider holders) {
        int cursor = -1;

        for (ItemStack resolved : resolvedStacks) {
            int currentIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                if (ItemStack.isSameItemSameComponents(items.get(i), resolved)) {
                    currentIndex = i;
                    break;
                }
            }

            int targetIndex;
            if (cursor >= 0) {
                targetIndex = Math.min(cursor, items.size());
            } else {
                targetIndex = items.size();
                ItemRef anchor = entry.after != null ? entry.after : entry.before;
                if (anchor != null && anchor.item != null) {
                    int fallback = -1;
                    for (int i = 0; i < items.size(); i++) {
                        if (i == currentIndex) continue;
                        if (!BuiltInRegistries.ITEM.getKey(items.get(i).getItem()).toString().equals(anchor.item))
                            continue;
                        if (fallback < 0) fallback = i;
                        if (anchor.components == null || ComponentUtil.matches(items.get(i), anchor, holders)) {
                            fallback = i;
                            break;
                        }
                    }
                    if (fallback >= 0) {
                        targetIndex = entry.after != null ? fallback + 1 : fallback;
                    }
                }
            }

            if (currentIndex < 0) {
                if (targetIndex > items.size()) targetIndex = items.size();
                items.add(targetIndex, resolved);
                cursor = targetIndex + 1;
                continue;
            }

            if (targetIndex == currentIndex) {
                cursor = currentIndex + 1;
                continue;
            }

            ItemStack stack = items.remove(currentIndex);
            if (targetIndex > currentIndex) targetIndex--;
            items.add(targetIndex, stack);
            cursor = targetIndex + 1;
        }
    }

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void postBuildContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        String id = recreative$getTabId();
        if (!id.isEmpty()) {
            CreativeTabManager.PRISTINE_TAB_ITEMS.put(id, new ArrayList<>(this.displayItems));
        }

        if (!ModConfig.get().enabled) return;

        CreativeTabManager.TabModifier modifier = CreativeTabManager.TAB_MODIFIERS.get(id);
        if (modifier == null) return;

        List<ItemStack> tempDisplayItems = new ArrayList<>(this.displayItems);
        List<ItemStack> tempSearchItems = new ArrayList<>(this.displayItemsSearchTab);

        if (!modifier.removeItems.isEmpty()) {
            Predicate<ItemStack> shouldRemove = stack -> {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String idStr = itemId.toString();

                for (ItemEntry removal : modifier.removeItems) {
                    if (removal == null || removal.item == null) continue;
                    boolean match = false;

                    if (removal.item.startsWith("#")) {
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(removal.item.substring(1)));
                        if (stack.is(tagKey)) match = true;
                    } else if (idStr.equals(removal.item)) {
                        match = true;
                    }

                    if (match) {
                        if (removal.components != null) {
                            try {
                                JsonElement componentJson = JsonParser.parseString(removal.components);
                                DataComponentPatch patch = DataComponentPatch.CODEC.parse(
                                        RegistryOps.create(JsonOps.INSTANCE, parameters.holders()),
                                        componentJson
                                ).result().orElseThrow();

                                boolean componentsMatch = true;
                                for (Map.Entry<DataComponentType<?>, Optional<?>> patchEntry : patch.entrySet()) {
                                    if (patchEntry.getValue().isPresent()) {
                                        if (!Objects.equals(stack.get(patchEntry.getKey()), patchEntry.getValue().get())) {
                                            componentsMatch = false;
                                            break;
                                        }
                                    } else {
                                        if (stack.has(patchEntry.getKey())) {
                                            componentsMatch = false;
                                            break;
                                        }
                                    }
                                }
                                if (componentsMatch) return true;
                            } catch (Exception e) {
                                // Ignore unparseable components during remove iteration
                            }
                        } else {
                            return true;
                        }
                    }
                }
                return false;
            };

            tempDisplayItems.removeIf(shouldRemove);
            tempSearchItems.removeIf(shouldRemove);
        }

        if (!modifier.addItems.isEmpty()) {
            for (ItemEntry entry : modifier.addItems) {
                if (entry == null || entry.item == null) continue;

                List<ItemStack> resolvedStacks = recreative$resolveStacksToAdd(entry, parameters);
                recreative$applyPlacement(tempDisplayItems, resolvedStacks, entry, parameters.holders());
                recreative$applyPlacement(tempSearchItems, resolvedStacks, entry, parameters.holders());
            }
        }

        this.displayItems.clear();
        this.displayItems.addAll(tempDisplayItems);

        this.displayItemsSearchTab.clear();
        this.displayItemsSearchTab.addAll(tempSearchItems);
    }

    @Inject(method = "buildContents", at = @At("HEAD"), cancellable = true)
    private void recreative$bypassRegistryCheck(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;

        if (CreativeTabManager.RUNTIME_TABS.containsValue(self)) {
            Collection<ItemStack> displayItems = self.getDisplayItems();
            Collection<ItemStack> searchItems = self.getSearchTabDisplayItems();

            displayItems.clear();
            searchItems.clear();

            CreativeModeTab.Output output = (stack, visibility) -> {
                if (stack.getCount() != 1) return;

                if (stack.getItem().isEnabled(parameters.enabledFeatures())) {
                    if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
                        displayItems.add(stack);
                    }
                    if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY) {
                        searchItems.add(stack);
                    }
                }
            };

            ((CreativeModeTabAccessor) self).getDisplayItemsGenerator().accept(parameters, output);

            ci.cancel();
        }
    }
}
