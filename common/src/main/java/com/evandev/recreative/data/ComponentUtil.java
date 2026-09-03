package com.evandev.recreative.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ComponentUtil {
    private ComponentUtil() {
    }

    public static String encode(ItemStack stack, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty() || holders == null) return null;

        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return null;

        try {
            JsonElement json = DataComponentPatch.CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, holders),
                    patch
            ).getOrThrow(IllegalStateException::new);
            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static DataComponentPatch parse(String componentsJson, HolderLookup.Provider holders) {
        if (componentsJson == null || holders == null) return null;
        try {
            JsonElement json = JsonParser.parseString(componentsJson);
            return DataComponentPatch.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, holders),
                    json
            ).result().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean matchesPatch(ItemStack stack, DataComponentPatch patch) {
        if (patch == null) return false;
        for (Map.Entry<DataComponentType<?>, Optional<?>> patchEntry : patch.entrySet()) {
            if (patchEntry.getValue().isPresent()) {
                if (!Objects.equals(stack.get(patchEntry.getKey()), patchEntry.getValue().get())) return false;
            } else if (stack.has(patchEntry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public static boolean matches(ItemStack stack, String itemId, String componentsJson, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty()) return false;
        if (itemId != null && !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) return false;
        if (componentsJson == null) return true;

        DataComponentPatch patch = parse(componentsJson, holders);
        if (patch == null) return false;
        return matchesPatch(stack, patch);
    }

    public static boolean matches(ItemStack stack, ItemRef ref, HolderLookup.Provider holders) {
        if (ref == null) return false;
        return matches(stack, ref.item, ref.components, holders);
    }

    public static ItemRef anchorFor(ItemStack stack, Iterable<ItemStack> siblings, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty()) return null;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int sameId = 0;
        for (ItemStack sibling : siblings) {
            if (sibling != null && !sibling.isEmpty()
                    && BuiltInRegistries.ITEM.getKey(sibling.getItem()).toString().equals(id)) {
                sameId++;
                if (sameId > 1) break;
            }
        }

        return sameId > 1 ? new ItemRef(id, encode(stack, holders)) : new ItemRef(id);
    }
}
