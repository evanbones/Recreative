package com.evandev.recreative.client.gui.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ItemSearchHelper {

    public static List<ItemStack> filterItems(List<ItemStack> allItems, String searchQuery, String selectedMod) {
        List<ItemStack> results = new ArrayList<>();
        String query = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";
        boolean filterMod = selectedMod != null && !selectedMod.isEmpty() && !selectedMod.equalsIgnoreCase("all");

        if (query.startsWith("#")) {
            String tagQuery = query.substring(1).trim();
            if (tagQuery.isEmpty()) {
                for (ItemStack stack : allItems) {
                    if (!filterMod || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equalsIgnoreCase(selectedMod)) {
                        results.add(stack);
                    }
                }
                return results;
            }

            Set<Item> matchedItems = new HashSet<>();
            ResourceLocation tagLoc = ResourceLocation.tryParse(tagQuery);
            if (tagLoc != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                    matchedItems.add(holder.value());
                }
            }

            BuiltInRegistries.ITEM.getTags().forEach(tagPair -> {
                TagKey<Item> tagKey = tagPair.getFirst();
                String tagId = tagKey.location().toString().toLowerCase(Locale.ROOT);
                String tagPath = tagKey.location().getPath().toLowerCase(Locale.ROOT);
                if (tagId.contains(tagQuery) || tagPath.contains(tagQuery)) {
                    for (Holder<Item> holder : tagPair.getSecond()) {
                        matchedItems.add(holder.value());
                    }
                }
            });

            for (ItemStack stack : allItems) {
                if (matchedItems.contains(stack.getItem())) {
                    if (!filterMod || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equalsIgnoreCase(selectedMod)) {
                        results.add(stack);
                    }
                }
            }
            return results;
        }

        String explicitMod = null;
        if (query.startsWith("@")) {
            int spaceIdx = query.indexOf(' ');
            if (spaceIdx > 0) {
                explicitMod = query.substring(1, spaceIdx).trim();
                query = query.substring(spaceIdx + 1).trim();
            } else {
                explicitMod = query.substring(1).trim();
                query = "";
            }
        }

        for (ItemStack stack : allItems) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idNamespace = id.getNamespace().toLowerCase(Locale.ROOT);
            String idPath = id.getPath().toLowerCase(Locale.ROOT);
            String idFull = id.toString().toLowerCase(Locale.ROOT);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);

            if (filterMod && !idNamespace.equalsIgnoreCase(selectedMod)) continue;
            if (explicitMod != null && !explicitMod.isEmpty() && !idNamespace.contains(explicitMod)) continue;

            if (query.isEmpty() || idPath.contains(query) || idFull.contains(query) || name.contains(query)) {
                results.add(stack);
            }
        }

        return results;
    }
}
