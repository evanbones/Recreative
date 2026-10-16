package com.evandev.recreative.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.Set;

//? if >=1.21 {
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.RegistryOps;

import java.util.Objects;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
*///?}

/**
 * Handles the extra data attached to item entries: a data component patch (as JSON) on 1.21+, or an NBT
 * compound (as SNBT) on 1.20.1.
 */
public final class ComponentUtil {
    //? if >=1.21 {
    public static final String JSON_KEY = "components";
    public static final String LEGACY_JSON_KEY = "nbt";
    //?} else {
    /*public static final String JSON_KEY = "nbt";
    public static final String LEGACY_JSON_KEY = "components";
    *///?}

    public static String encode(ItemStack stack, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty() || holders == null) return null;

        //? if >=1.21 {
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
        //?} else {
        /*CompoundTag tag = stack.getTag();
        return tag == null || tag.isEmpty() ? null : tag.toString();
        *///?}
    }

    public static void apply(ItemStack stack, String data, HolderLookup.Provider holders) throws Exception {
        if (data == null) return;
        //? if >=1.21 {
        DataComponentPatch patch = DataComponentPatch.CODEC.parse(
                RegistryOps.create(JsonOps.INSTANCE, holders),
                JsonParser.parseString(data)
        ).result().orElseThrow();
        stack.applyComponents(patch);
        //?} else {
        /*stack.getOrCreateTag().merge(TagParser.parseTag(data));
        *///?}
    }

    public static boolean tryApply(ItemStack stack, String data, HolderLookup.Provider holders) {
        try {
            apply(stack, data, holders);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean matchesData(ItemStack stack, String data, HolderLookup.Provider holders) {
        if (data == null) return true;
        try {
            //? if >=1.21 {
            DataComponentPatch patch = DataComponentPatch.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, holders),
                    JsonParser.parseString(data)
            ).result().orElse(null);
            if (patch == null) return false;
            DataComponentPatch.SplitResult split = patch.split();
            for (TypedDataComponent<?> component : split.added()) {
                if (!Objects.equals(stack.get(component.type()), component.value())) return false;
            }
            for (DataComponentType<?> removed : split.removed()) {
                if (stack.has(removed)) return false;
            }
            return true;
            //?} else {
            /*CompoundTag tag = TagParser.parseTag(data);
            return NbtUtils.compareNbt(tag, stack.getTag(), true);
            *///?}
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean matches(ItemStack stack, String itemId, String data, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty()) return false;
        if (itemId != null && !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) return false;
        return matchesData(stack, data, holders);
    }

    public static boolean matches(ItemStack stack, ItemRef ref, HolderLookup.Provider holders) {
        if (ref == null) return false;
        return matches(stack, ref.item, ref.components, holders);
    }

    public static boolean isSameItemSameData(ItemStack a, ItemStack b) {
        //? if >=1.21 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?}
    }

    public static Set<ItemStack> createLinkedSet() {
        //? if >=1.21 {
        return ItemStackLinkedSet.createTypeAndComponentsSet();
        //?} else {
        /*return ItemStackLinkedSet.createTypeAndTagSet();
        *///?}
    }

    /**
     * Reads the encoded data from an entry JSON object, accepting both the current and the legacy key.
     */
    public static String readJson(JsonObject obj) {
        JsonElement element = obj.has(JSON_KEY) ? obj.get(JSON_KEY) : obj.get(LEGACY_JSON_KEY);
        if (element == null || element.isJsonNull()) return null;
        return element.isJsonObject() ? element.toString() : element.getAsString();
    }

    public static void writeJson(JsonObject obj, String data) {
        if (data == null) return;
        //? if >=1.21 {
        try {
            obj.add(JSON_KEY, JsonParser.parseString(data));
        } catch (Exception e) {
            obj.addProperty(JSON_KEY, data);
        }
        //?} else {
        /*obj.add(JSON_KEY, new JsonPrimitive(data));
        *///?}
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
