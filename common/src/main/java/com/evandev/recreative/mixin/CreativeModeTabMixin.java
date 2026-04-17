package com.evandev.recreative.mixin;

import com.evandev.recreative.Constants;
import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.data.ItemEntry;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
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
            Item customIcon = BuiltInRegistries.ITEM.get(ResourceLocation.parse(modifier.icon));
            cir.setReturnValue(new ItemStack(customIcon));
        }
    }

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void postBuildContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (!ModConfig.get().enabled) return;

        String id = recreative$getTabId();
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

                List<ItemStack> stacksToAdd = new ArrayList<>();

                if (entry.item.startsWith("#")) {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.item.substring(1)));
                    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                        stacksToAdd.add(new ItemStack(holder.value()));
                    }
                } else {
                    Item itemToAdd = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item));
                    stacksToAdd.add(new ItemStack(itemToAdd));
                }

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

                    int insertIndex = tempDisplayItems.size();
                    if (entry.after != null) {
                        for (int i = 0; i < tempDisplayItems.size(); i++) {
                            if (BuiltInRegistries.ITEM.getKey(tempDisplayItems.get(i).getItem()).toString().equals(entry.after)) {
                                insertIndex = i + 1;
                            }
                        }
                    } else if (entry.before != null) {
                        for (int i = 0; i < tempDisplayItems.size(); i++) {
                            if (BuiltInRegistries.ITEM.getKey(tempDisplayItems.get(i).getItem()).toString().equals(entry.before)) {
                                insertIndex = i;
                                break;
                            }
                        }
                    }

                    tempDisplayItems.add(insertIndex, stack.copy());
                    tempSearchItems.add(stack.copy());
                }
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