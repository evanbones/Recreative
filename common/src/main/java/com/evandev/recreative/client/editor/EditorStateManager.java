package com.evandev.recreative.client.editor;

import com.evandev.recreative.Constants;
import com.evandev.recreative.data.Action;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.data.ItemEntry;
import com.evandev.recreative.data.TabRule;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.evandev.recreative.mixin.accessor.CreativeModeTabsAccessor;
import com.evandev.recreative.platform.Services;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;

public class EditorStateManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Action.class, (JsonSerializer<Action>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.name().toLowerCase()))
            .registerTypeAdapter(ItemEntry.class, (JsonSerializer<ItemEntry>) (src, typeOfSrc, context) -> {
                if (src.after == null && src.before == null && src.components == null) {
                    return new JsonPrimitive(src.item);
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("item", src.item);
                if (src.after != null) obj.addProperty("after", src.after);
                if (src.before != null) obj.addProperty("before", src.before);
                if (src.components != null) {
                    try {
                        obj.add("components", JsonParser.parseString(src.components));
                    } catch (Exception e) {
                        obj.addProperty("components", src.components);
                    }
                }
                return obj;
            })
            .create();

    private final Map<String, EditableTab> tabsMap = new LinkedHashMap<>();
    private final List<String> tabOrder = new ArrayList<>();
    private boolean isDirty = false;

    private static Component rawDisplayName(CreativeModeTab tab) {
        try {
            Component name = ((CreativeModeTabAccessor) tab).getRawDisplayName();
            if (name != null) return name;
        } catch (Throwable ignored) {
        }
        return tab.getDisplayName();
    }

    private static ItemStack rawIcon(CreativeModeTab tab) {
        try {
            var generator = ((CreativeModeTabAccessor) tab).getIconGenerator();
            if (generator != null) {
                ItemStack icon = generator.get();
                if (icon != null) return icon;
            }
        } catch (Throwable ignored) {
        }
        return tab.getIconItem();
    }

    public static ItemStack resolveIconStack(String iconId) {
        if (iconId == null || iconId.isEmpty() || iconId.endsWith(".png")) {
            return new ItemStack(Items.ITEM_FRAME);
        }
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(iconId));
            return item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.ITEM_FRAME);
        } catch (Exception e) {
            return new ItemStack(Items.ITEM_FRAME);
        }
    }

    public static ItemStack resolveItemStack(ItemEntry entry) {
        if (entry == null || entry.item == null) return ItemStack.EMPTY;
        if (entry.item.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.item.substring(1)));
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                return new ItemStack(holder.value());
            }
            return ItemStack.EMPTY;
        }

        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item));
            if (item == Items.AIR) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(item);
            if (entry.components != null) {
                CreativeModeTab.ItemDisplayParameters cachedParams = CreativeModeTabsAccessor.getCachedParameters();
                if (cachedParams != null) {
                    JsonElement componentJson = JsonParser.parseString(entry.components);
                    DataComponentPatch.CODEC.parse(
                            RegistryOps.create(JsonOps.INSTANCE, CreativeTabManager.freshHolders(cachedParams.holders())),
                            componentJson
                    ).result().ifPresent(stack::applyComponents);
                }
            }
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public void loadState() {
        tabsMap.clear();
        tabOrder.clear();
        isDirty = false;

        CreativeTabManager.load();

        CreativeModeTab.ItemDisplayParameters cachedParams = CreativeModeTabsAccessor.getCachedParameters();
        HolderLookup.Provider holders = null;
        if (cachedParams != null) {
            holders = CreativeTabManager.freshHolders(cachedParams.holders());
        } else if (Minecraft.getInstance().level != null) {
            holders = Minecraft.getInstance().level.registryAccess();
        }

        for (ResourceLocation key : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            String tabId = key.toString();
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
            if (tab == null) continue;

            EditableTab editableTab = new EditableTab(tabId, false);
            editableTab.defaultDisplayName = rawDisplayName(tab);
            editableTab.defaultIcon = rawIcon(tab);

            CreativeTabManager.TabModifier modifier = CreativeTabManager.TAB_MODIFIERS.get(tabId);
            if (modifier != null) {
                editableTab.customDisplayName = modifier.name;
                editableTab.customIcon = modifier.icon;
                editableTab.addedItems.addAll(modifier.addItems);
                editableTab.removedItems.addAll(modifier.removeItems);
            }

            editableTab.isRemoved = CreativeTabManager.REMOVED_TABS.contains(tabId);

            populateTabDisplayItems(editableTab, tab, holders);
            tabsMap.put(tabId, editableTab);
        }

        for (Map.Entry<String, CreativeTabManager.TabModifier> entry : CreativeTabManager.CUSTOM_TABS_DEFS.entrySet()) {
            String tabId = entry.getKey();
            CreativeTabManager.TabModifier def = entry.getValue();
            CreativeModeTab tab = CreativeTabManager.RUNTIME_TABS.get(tabId);

            EditableTab editableTab = new EditableTab(tabId, true);
            editableTab.defaultDisplayName = Component.literal(def.name != null ? def.name : "Custom Tab");
            editableTab.customDisplayName = def.name;
            editableTab.customIcon = def.icon;
            editableTab.addedItems.addAll(def.addItems);
            editableTab.isRemoved = CreativeTabManager.REMOVED_TABS.contains(tabId);

            if (tab != null) {
                editableTab.defaultIcon = tab.getIconItem();
                populateTabDisplayItems(editableTab, tab, holders);
            } else {
                editableTab.defaultIcon = resolveIconStack(def.icon);
                populateCustomTabItems(editableTab, holders);
            }

            tabsMap.put(tabId, editableTab);
        }

        if (!CreativeTabManager.TAB_ORDER.isEmpty()) {
            for (String id : CreativeTabManager.TAB_ORDER) {
                if (tabsMap.containsKey(id) && !tabOrder.contains(id)) {
                    tabOrder.add(id);
                }
            }
        }
        for (String id : tabsMap.keySet()) {
            if (!tabOrder.contains(id)) {
                tabOrder.add(id);
            }
        }
    }

    private List<ItemStack> generateDefaultItems(CreativeModeTab tab, HolderLookup.Provider holders) {
        List<ItemStack> out = new ArrayList<>();
        if (holders == null) return out;
        try {
            CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                    Minecraft.getInstance().player != null ? Minecraft.getInstance().player.connection.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS,
                    Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2),
                    holders
            );
            ((CreativeModeTabAccessor) tab).getDisplayItemsGenerator().accept(params, (stack, visibility) -> {
                if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY && !stack.isEmpty()) {
                    out.add(stack);
                }
            });
        } catch (Throwable t) {
            Constants.LOG.error("Failed to generate default items for tab {}", CreativeTabManager.getTabId(tab), t);
        }
        return out;
    }

    private void populateTabDisplayItems(EditableTab editableTab, CreativeModeTab tab, HolderLookup.Provider holders) {
        editableTab.defaultItems.clear();
        editableTab.defaultItems.addAll(generateDefaultItems(tab, holders));

        editableTab.originalItemIds.clear();
        for (ItemStack s : editableTab.defaultItems) {
            editableTab.originalItemIds.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
        }

        editableTab.displayItems.clear();
        try {
            Collection<ItemStack> items = tab.getDisplayItems();
            if (!items.isEmpty()) {
                editableTab.displayItems.addAll(items);
                return;
            }
        } catch (Throwable ignored) {
        }
        editableTab.displayItems.addAll(editableTab.defaultItems);
    }

    private void populateCustomTabItems(EditableTab editableTab, HolderLookup.Provider holders) {
        editableTab.displayItems.clear();
        for (ItemEntry entry : editableTab.addedItems) {
            if (entry == null || entry.item == null) continue;
            if (entry.item.startsWith("#")) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.item.substring(1)));
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                    editableTab.displayItems.add(new ItemStack(holder.value()));
                }
            } else {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.item));
                if (item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    if (entry.components != null && holders != null) {
                        try {
                            JsonElement componentJson = JsonParser.parseString(entry.components);
                            DataComponentPatch patch = DataComponentPatch.CODEC.parse(
                                    RegistryOps.create(JsonOps.INSTANCE, holders),
                                    componentJson
                            ).result().orElseThrow();
                            stack.applyComponents(patch);
                        } catch (Exception ignored) {
                        }
                    }
                    editableTab.displayItems.add(stack);
                }
            }
        }
    }

    public List<EditableTab> getTabs() {
        List<EditableTab> list = new ArrayList<>();
        for (String id : tabOrder) {
            EditableTab tab = tabsMap.get(id);
            if (tab != null) list.add(tab);
        }
        for (EditableTab tab : tabsMap.values()) {
            if (!list.contains(tab)) list.add(tab);
        }
        return list;
    }

    public EditableTab getTab(String id) {
        return tabsMap.get(id);
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public void createCustomTab(String id, String name, String icon) {
        if (id == null || id.isEmpty()) return;
        if (!id.contains(":")) id = "recreative:" + id;

        if (tabsMap.containsKey(id)) {
            EditableTab existing = tabsMap.get(id);
            existing.isCustomTab = true;
            existing.customDisplayName = name;
            existing.customIcon = icon;
            existing.isRemoved = false;
        } else {
            EditableTab tab = new EditableTab(id, true);
            tab.defaultDisplayName = Component.literal(name != null ? name : id);
            tab.customDisplayName = name;
            tab.customIcon = icon != null && !icon.isEmpty() ? icon : "minecraft:stone";
            tab.defaultIcon = resolveIconStack(tab.customIcon);
            tabsMap.put(id, tab);
            tabOrder.add(id);
        }
        markDirty();
    }

    public void deleteCustomTab(String id) {
        EditableTab tab = tabsMap.get(id);
        if (tab != null && tab.isCustomTab) {
            tabsMap.remove(id);
            tabOrder.remove(id);
            markDirty();
        }
    }

    public void resetTab(String id) {
        EditableTab tab = tabsMap.get(id);
        if (tab == null) return;

        if (tab.isCustomTab) {
            deleteCustomTab(id);
            return;
        }

        tab.customDisplayName = null;
        tab.customIcon = null;
        tab.isRemoved = false;
        tab.addedItems.clear();
        tab.removedItems.clear();

        if (tab.defaultItems.isEmpty()) {
            CreativeModeTab vanillaTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation.parse(id));
            if (vanillaTab != null) {
                CreativeModeTab.ItemDisplayParameters cachedParams = CreativeModeTabsAccessor.getCachedParameters();
                HolderLookup.Provider holders = cachedParams != null ? CreativeTabManager.freshHolders(cachedParams.holders()) : null;
                tab.defaultItems.addAll(generateDefaultItems(vanillaTab, holders));
                tab.originalItemIds.clear();
                for (ItemStack s : tab.defaultItems) {
                    tab.originalItemIds.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                }
            }
        }
        tab.displayItems.clear();
        tab.displayItems.addAll(tab.defaultItems);

        markDirty();
    }

    public void setTabName(String tabId, String name) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null) return;

        String trimmed = (name == null || name.trim().isEmpty()) ? null : name.trim();
        if (!tab.isCustomTab && trimmed != null && trimmed.equals(tab.defaultDisplayName.getString())) {
            trimmed = null;
        }
        if (Objects.equals(trimmed, tab.customDisplayName)) return;

        tab.customDisplayName = trimmed;
        markDirty();
    }

    public void setTabIcon(String tabId, String icon) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null) return;

        String trimmed = (icon == null || icon.trim().isEmpty()) ? null : icon.trim();
        if (!tab.isCustomTab && trimmed != null && !trimmed.endsWith(".png") && !tab.defaultIcon.isEmpty()) {
            ResourceLocation defaultKey = BuiltInRegistries.ITEM.getKey(tab.defaultIcon.getItem());
            if (defaultKey.toString().equals(trimmed)) {
                trimmed = null;
            }
        }
        if (Objects.equals(trimmed, tab.customIcon)) return;

        tab.customIcon = trimmed;
        markDirty();
    }

    public void setTabVisibility(String tabId, boolean visible) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab != null) {
            tab.isRemoved = !visible;
            markDirty();
        }
    }

    public void moveTab(String tabId, int delta) {
        int index = tabOrder.indexOf(tabId);
        if (index < 0) return;
        int target = index + delta;
        if (target >= 0 && target < tabOrder.size()) {
            tabOrder.remove(index);
            tabOrder.add(target, tabId);
            markDirty();
        }
    }

    public void addItemToTab(String tabId, ItemEntry entry, int targetIndex) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || entry == null || entry.item == null) return;
        tab.removedItems.removeIf(r -> Objects.equals(r.item, entry.item));
        tab.addedItems.add(entry);

        ItemStack stack = resolveItemStack(entry);
        if (!stack.isEmpty()) {
            if (targetIndex >= 0 && targetIndex <= tab.displayItems.size()) {
                tab.displayItems.add(targetIndex, stack);
            } else {
                tab.displayItems.add(stack);
            }
        }
        markDirty();
    }

    public void removeItemFromTab(String tabId, int itemIndex) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || itemIndex < 0 || itemIndex >= tab.displayItems.size()) return;

        ItemStack removedStack = tab.displayItems.remove(itemIndex);
        if (removedStack.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(removedStack.getItem()).toString();

        tab.addedItems.removeIf(e -> Objects.equals(e.item, itemId));

        if (!tab.isCustomTab && tab.originalItemIds.contains(itemId)) {
            if (tab.removedItems.stream().noneMatch(e -> Objects.equals(e.item, itemId))) {
                tab.removedItems.add(new ItemEntry(itemId));
            }
        }

        markDirty();
    }

    public void reorderItemInTab(String tabId, int fromIndex, int toIndex) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || fromIndex < 0 || fromIndex >= tab.displayItems.size() || toIndex < 0 || toIndex >= tab.displayItems.size())
            return;
        if (fromIndex == toIndex) return;

        ItemStack stack = tab.displayItems.remove(fromIndex);
        tab.displayItems.add(toIndex, stack);

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ItemEntry entry = null;
        for (ItemEntry e : tab.addedItems) {
            if (Objects.equals(e.item, itemId)) {
                entry = e;
                break;
            }
        }
        if (entry == null) {
            entry = new ItemEntry(itemId);
            tab.addedItems.add(entry);
        }

        if (toIndex > 0) {
            ItemStack beforeStack = tab.displayItems.get(toIndex - 1);
            entry.after = BuiltInRegistries.ITEM.getKey(beforeStack.getItem()).toString();
            entry.before = null;
        } else if (toIndex + 1 < tab.displayItems.size()) {
            ItemStack afterStack = tab.displayItems.get(toIndex + 1);
            entry.before = BuiltInRegistries.ITEM.getKey(afterStack.getItem()).toString();
            entry.after = null;
        }

        markDirty();
    }

    public void saveAndApply() throws Exception {
        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("recreative");
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<TabRule> rules = new ArrayList<>();

        List<String> removedTabsList = new ArrayList<>();
        for (EditableTab tab : tabsMap.values()) {
            if (tab.isRemoved) {
                removedTabsList.add(tab.id);
            }
        }
        if (!removedTabsList.isEmpty()) {
            TabRule removeRule = new TabRule();
            removeRule.action = Action.REMOVE_TAB;
            removeRule.tabs = removedTabsList;
            rules.add(removeRule);
        }

        if (!tabOrder.isEmpty()) {
            TabRule orderRule = new TabRule();
            orderRule.action = Action.TAB_ORDER;
            orderRule.order = new ArrayList<>(tabOrder);
            rules.add(orderRule);
        }

        for (EditableTab tab : tabsMap.values()) {
            if (tab.isCustomTab) {
                TabRule customRule = new TabRule();
                customRule.action = Action.CUSTOM_TAB;
                customRule.tabs = List.of(tab.id);
                customRule.name = tab.customDisplayName != null ? tab.customDisplayName : tab.id;
                customRule.icon = tab.customIcon != null ? tab.customIcon : "minecraft:stone";
                customRule.addItems = new ArrayList<>(tab.addedItems);
                rules.add(customRule);
            }
        }

        for (EditableTab tab : tabsMap.values()) {
            if (!tab.isCustomTab && (tab.customDisplayName != null || tab.customIcon != null || !tab.addedItems.isEmpty() || !tab.removedItems.isEmpty())) {
                TabRule modRule = new TabRule();
                modRule.action = Action.MODIFY_TAB;
                modRule.tabs = List.of(tab.id);
                if (tab.customDisplayName != null) modRule.name = tab.customDisplayName;
                if (tab.customIcon != null) modRule.icon = tab.customIcon;
                if (!tab.addedItems.isEmpty()) modRule.addItems = new ArrayList<>(tab.addedItems);
                if (!tab.removedItems.isEmpty()) modRule.removeItems = new ArrayList<>(tab.removedItems);
                rules.add(modRule);
            }
        }

        File outFile = new File(dir, "tabs.json");
        try (FileWriter writer = new FileWriter(outFile)) {
            GSON.toJson(rules, writer);
        }

        CreativeTabManager.reloadTabs();
        this.isDirty = false;
    }

    public static class EditableTab {
        public final String id;
        public final Set<String> originalItemIds = new HashSet<>();
        public final List<ItemEntry> addedItems = new ArrayList<>();
        public final List<ItemEntry> removedItems = new ArrayList<>();
        public final List<ItemStack> defaultItems = new ArrayList<>();
        public final List<ItemStack> displayItems = new ArrayList<>();
        public boolean isCustomTab;
        public boolean isRemoved = false;
        public Component defaultDisplayName = Component.empty();
        public String customDisplayName = null;
        public ItemStack defaultIcon = ItemStack.EMPTY;
        public String customIcon = null;

        public EditableTab(String id, boolean isCustomTab) {
            this.id = id;
            this.isCustomTab = isCustomTab;
        }

        public Component getEffectiveDisplayName() {
            if (customDisplayName != null && !customDisplayName.isEmpty()) {
                return Component.literal(customDisplayName);
            }
            return defaultDisplayName != null ? defaultDisplayName : Component.literal(id);
        }

        public ItemStack getEffectiveIconStack() {
            if (customIcon != null && !customIcon.isEmpty()) {
                return resolveIconStack(customIcon);
            }
            return defaultIcon != null && !defaultIcon.isEmpty() ? defaultIcon : new ItemStack(Items.CHEST);
        }

        public boolean isModified() {
            return customDisplayName != null || customIcon != null || !addedItems.isEmpty() || !removedItems.isEmpty() || isRemoved;
        }
    }
}
