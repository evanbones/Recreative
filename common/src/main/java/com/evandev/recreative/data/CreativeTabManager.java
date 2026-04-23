package com.evandev.recreative.data;

import com.evandev.recreative.Constants;
import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.mixin.accessor.MappedRegistryAccessor;
import com.evandev.recreative.platform.Services;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class CreativeTabManager {
    public static final Map<String, CreativeModeTab> RUNTIME_TABS = new LinkedHashMap<>();
    public static final Set<String> REMOVED_TABS = new HashSet<>();
    public static final List<String> TAB_ORDER = new ArrayList<>();
    public static final Map<String, TabModifier> TAB_MODIFIERS = new HashMap<>();
    public static final Map<String, TabModifier> CUSTOM_TABS_DEFS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(ItemEntry.class, new ItemEntryDeserializer())
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrListDeserializer())
            .registerTypeAdapter(new TypeToken<List<ItemEntry>>() {
            }.getType(), new ItemEntryListDeserializer())
            .create();

    public static void load() {
        REMOVED_TABS.clear();
        TAB_ORDER.clear();
        TAB_MODIFIERS.clear();
        CUSTOM_TABS_DEFS.clear();
        RUNTIME_TABS.clear();

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("recreative");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                generateDefaultConfig(configDir);
            } catch (Exception ignored) {
            }
        }

        try (Stream<Path> paths = Files.walk(configDir)) {
            List<Path> files = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
            if (!files.isEmpty()) {
                files.forEach(CreativeTabManager::parseFile);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load recreative tab rules", e);
        }

        CUSTOM_TABS_DEFS.forEach((id, def) -> {
            Identifier tabId = Identifier.parse(id);
            CreativeModeTab existingTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(tabId);

            CreativeModeTab tabToUse;

            if (existingTab != null) {
                tabToUse = existingTab;
            } else {
                tabToUse = Services.PLATFORM.buildCreativeTab(
                        Component.translatable(def.name),
                        () -> {
                            TabModifier currentDef = CUSTOM_TABS_DEFS.get(id);
                            String iconId = (currentDef != null && currentDef.icon != null) ? currentDef.icon : "minecraft:stone";

                            if (iconId.endsWith(".png")) {
                                return ItemStack.EMPTY;
                            }

                            Item iconItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(iconId));
                            return new ItemStack(iconItem);
                        },
                        (parameters, output) -> {
                            TabModifier currentDef = CUSTOM_TABS_DEFS.get(id);
                            if (currentDef == null) return;

                            for (ItemEntry entry : currentDef.addItems) {
                                if (entry == null || entry.item == null) continue;

                                List<ItemStack> stacksToAdd = new ArrayList<>();

                                if (entry.item.startsWith("#")) {
                                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(entry.item.substring(1)));
                                    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                                        stacksToAdd.add(new ItemStack(holder.value()));
                                    }
                                } else {
                                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.item));
                                    stacksToAdd.add(new ItemStack(item));
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
                                    output.accept(stack);
                                }
                            }
                        }
                );

                boolean wasFrozen = false;
                MappedRegistryAccessor registryAccessor = null;

                if (BuiltInRegistries.CREATIVE_MODE_TAB instanceof MappedRegistryAccessor accessor) {
                    registryAccessor = accessor;
                    wasFrozen = registryAccessor.isFrozen();
                    if (wasFrozen) {
                        registryAccessor.setFrozen(false);
                    }
                }

                Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabId, tabToUse);

                if (wasFrozen) {
                    registryAccessor.setFrozen(true);
                }
            }
            if (def != null && def.icon != null && def.icon.endsWith(".png")) {
                ((ICustomIconTab) tabToUse).recreative$setCustomIcon(Identifier.parse(def.icon));
            }
            RUNTIME_TABS.put(id, tabToUse);
        });

        Constants.LOG.info("Loaded Recreative tabs configuration!");
    }

    private static void parseFile(Path path) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonReader reader = new JsonReader(fileReader);
            reader.setStrictness(Strictness.LENIENT);

            while (reader.peek() != JsonToken.END_DOCUMENT) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonArray()) {
                    for (JsonElement e : json.getAsJsonArray()) {
                        processRule(GSON.fromJson(e, TabRule.class));
                    }
                } else if (json.isJsonObject()) {
                    processRule(GSON.fromJson(json, TabRule.class));
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing Recreative file: {}", path, e);
        }
    }

    private static void processRule(TabRule rule) {
        if (rule.action == null) return;

        switch (rule.action) {
            case REMOVE_TAB -> REMOVED_TABS.addAll(rule.tabs);
            case TAB_ORDER -> TAB_ORDER.addAll(rule.order);
            case MODIFY_TAB -> {
                for (String tabId : rule.tabs) {
                    TabModifier modifyDef = TAB_MODIFIERS.computeIfAbsent(tabId, _ -> new TabModifier());
                    if (rule.name != null) modifyDef.name = rule.name;
                    if (rule.icon != null) modifyDef.icon = rule.icon;
                    if (rule.removeItems != null) modifyDef.removeItems.addAll(rule.removeItems);
                    if (rule.addItems != null) modifyDef.addItems.addAll(rule.addItems);
                }
            }
            case CUSTOM_TAB -> {
                for (String tabId : rule.tabs) {
                    TabModifier customDef = new TabModifier();
                    customDef.name = rule.name != null ? rule.name : "Custom Tab";
                    customDef.icon = rule.icon != null ? rule.icon : "minecraft:stone";
                    if (rule.addItems != null) customDef.addItems.addAll(rule.addItems);
                    CUSTOM_TABS_DEFS.put(tabId, customDef);
                }
            }
        }
    }

    private static void generateDefaultConfig(Path configDir) {
        String defaultJson = """
                [
                    {
                        "action": "remove_tab",
                        "tabs": ["minecraft:example_tab"]
                    },
                    {
                        "action": "modify_tab",
                        "tabs": ["minecraft:combat"],
                        "name": "Custom Combat Name",
                        "icon": "minecraft:netherite_sword",
                        "remove_items": ["minecraft:wooden_sword"]
                    },
                    {
                        "action": "custom_tab",
                        "tabs": ["recreative:my_custom_tab"],
                        "name": "My Custom Tab",
                        "icon": "minecraft:emerald",
                        "items": [
                            "minecraft:diamond",
                            "minecraft:gold_ingot"
                        ]
                    }
                ]""";
        try {
            Files.writeString(configDir.resolve("example_tabs.json.disabled"), defaultJson);
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate default Recreative rules", e);
        }
    }

    public static String getTabId(CreativeModeTab tab) {
        Identifier key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (key != null) return key.toString();

        for (Map.Entry<String, CreativeModeTab> entry : RUNTIME_TABS.entrySet()) {
            if (entry.getValue() == tab) return entry.getKey();
        }
        return "";
    }

    public static class TabModifier {
        public final List<ItemEntry> removeItems = new ArrayList<>();
        public final List<ItemEntry> addItems = new ArrayList<>();
        public String name;
        public String icon;
    }

    public static class ItemEntryDeserializer implements JsonDeserializer<ItemEntry> {
        @Override
        public ItemEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return new ItemEntry(json.getAsString());
            } else if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (!obj.has("item")) return null;
                ItemEntry entry = new ItemEntry(obj.get("item").getAsString());
                if (obj.has("after")) entry.after = obj.get("after").getAsString();
                if (obj.has("before")) entry.before = obj.get("before").getAsString();
                if (obj.has("components")) {
                    JsonElement comp = obj.get("components");
                    entry.components = comp.isJsonObject() ? comp.toString() : comp.getAsString();
                }
                return entry;
            }
            return null;
        }
    }

    private static class StringOrListDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<String> list = new ArrayList<>();
            if (json.isJsonArray()) json.getAsJsonArray().forEach(e -> list.add(e.getAsString()));
            else if (json.isJsonPrimitive()) list.add(json.getAsString());
            return list;
        }
    }

    private static class ItemEntryListDeserializer implements JsonDeserializer<List<ItemEntry>> {
        @Override
        public List<ItemEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<ItemEntry> list = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    ItemEntry entry = context.deserialize(e, ItemEntry.class);
                    if (entry != null) list.add(entry);
                }
            } else {
                ItemEntry entry = context.deserialize(json, ItemEntry.class);
                if (entry != null) list.add(entry);
            }
            return list;
        }
    }
}