package com.evandev.recreative.client.editor;

import com.evandev.recreative.Constants;
import com.evandev.recreative.data.*;
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

import java.io.Writer;
import java.nio.file.Files;
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
                if (src.after != null) obj.add("after", ItemRef.toJson(src.after));
                if (src.before != null) obj.add("before", ItemRef.toJson(src.before));
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
    private static final String SEP = String.valueOf((char) 1);
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

    public static HolderLookup.Provider editorHolders() {
        CreativeModeTab.ItemDisplayParameters cached = CreativeModeTabsAccessor.getCachedParameters();
        if (cached != null) return CreativeTabManager.freshHolders(cached.holders());
        return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : null;
    }

    private static String itemIdOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String stackKey(ItemStack stack, HolderLookup.Provider holders) {
        if (stack == null || stack.isEmpty()) return "";
        String id = itemIdOf(stack);
        String components = ComponentUtil.encode(stack, holders);
        return components == null ? id : id + SEP + components;
    }

    private static String entryKey(ItemEntry entry) {
        if (entry == null || entry.item == null) return "";
        return entry.components == null ? entry.item : entry.item + SEP + entry.components;
    }

    private static ItemEntry takeMatch(List<ItemEntry> pool, ItemStack stack, HolderLookup.Provider holders) {
        String id = itemIdOf(stack);

        int loose = -1;
        int idOnly = -1;
        for (int i = 0; i < pool.size(); i++) {
            ItemEntry candidate = pool.get(i);
            if (!Objects.equals(candidate.item, id)) continue;

            if (candidate.components != null) {
                if (ComponentUtil.matches(stack, candidate.item, candidate.components, holders)) {
                    return pool.remove(i);
                }
                if (idOnly < 0) idOnly = i;
            } else if (loose < 0) {
                loose = i;
            }
        }

        if (loose >= 0) return pool.remove(loose);
        if (idOnly >= 0) return pool.remove(idOnly);
        return null;
    }

    private static ItemEntry entryFor(ItemStack stack, HolderLookup.Provider holders) {
        return new ItemEntry(itemIdOf(stack), ComponentUtil.encode(stack, holders));
    }

    private static boolean sameEntry(ItemEntry a, ItemEntry b) {
        return a != null && b != null
                && Objects.equals(a.item, b.item)
                && Objects.equals(a.components, b.components);
    }

    private static List<Integer> descending(Collection<Integer> indices) {
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(indices));
        Collections.reverse(sorted);
        return sorted;
    }

    private static Path sourceFor(String tabId, Path fallback) {
        Path source = CreativeTabManager.TAB_RULE_SOURCES.get(tabId);
        return source != null ? source : fallback;
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
        List<ItemStack> pristine = CreativeTabManager.PRISTINE_TAB_ITEMS.get(editableTab.id);
        if (pristine != null && !pristine.isEmpty()) {
            editableTab.defaultItems.addAll(pristine);
        } else {
            editableTab.defaultItems.addAll(generateDefaultItems(tab, holders));
        }

        HolderLookup.Provider keyHolders = holders != null ? holders : editorHolders();

        editableTab.originalItemIds.clear();
        for (ItemStack s : editableTab.defaultItems) {
            editableTab.originalItemIds.add(itemIdOf(s));
            editableTab.originalItemIds.add(stackKey(s, keyHolders));
        }

        editableTab.displayItems.clear();
        try {
            Collection<ItemStack> items = tab.getDisplayItems();
            if (!items.isEmpty()) {
                editableTab.displayItems.addAll(items);
            } else {
                editableTab.displayItems.addAll(editableTab.defaultItems);
            }
        } catch (Throwable ignored) {
            editableTab.displayItems.addAll(editableTab.defaultItems);
        }

        for (ItemStack s : editableTab.displayItems) {
            String key = stackKey(s, keyHolders);
            if (editableTab.addedItems.stream().noneMatch(e -> Objects.equals(entryKey(e), key))) {
                editableTab.originalItemIds.add(itemIdOf(s));
                editableTab.originalItemIds.add(key);
            }
        }
        for (ItemEntry r : editableTab.removedItems) {
            if (r != null && r.item != null && !r.item.startsWith("#")) {
                editableTab.originalItemIds.add(r.item);
                editableTab.originalItemIds.add(entryKey(r));
            }
        }
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
        if (!id.contains(":")) id = Constants.MOD_ID + ":" + id;

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
            List<ItemStack> pristine = CreativeTabManager.PRISTINE_TAB_ITEMS.get(id);
            if (pristine != null && !pristine.isEmpty()) {
                tab.defaultItems.addAll(pristine);
            } else {
                CreativeModeTab vanillaTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation.parse(id));
                if (vanillaTab != null) {
                    CreativeModeTab.ItemDisplayParameters cachedParams = CreativeModeTabsAccessor.getCachedParameters();
                    HolderLookup.Provider holders = cachedParams != null ? CreativeTabManager.freshHolders(cachedParams.holders()) : null;
                    tab.defaultItems.addAll(generateDefaultItems(vanillaTab, holders));
                }
            }
            tab.originalItemIds.clear();
            for (ItemStack s : tab.defaultItems) {
                tab.originalItemIds.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
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
        tab.removedItems.removeIf(r -> sameEntry(r, entry));

        ItemStack stack = resolveItemStack(entry);
        if (!stack.isEmpty()) {
            if (targetIndex >= 0 && targetIndex <= tab.displayItems.size()) {
                tab.displayItems.add(targetIndex, stack);
            } else {
                tab.displayItems.add(stack);
            }
        }
        tab.addedItems.add(entry);
        updateItemPositions(tab);
        markDirty();
    }

    public int[] addItemsToTab(String tabId, List<ItemEntry> entries, int targetIndex) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || entries == null || entries.isEmpty()) return null;

        int insertAt = (targetIndex < 0 || targetIndex > tab.displayItems.size()) ? tab.displayItems.size() : targetIndex;
        int inserted = 0;

        for (ItemEntry entry : entries) {
            if (entry == null || entry.item == null) continue;
            ItemEntry copy = entry.copy();
            copy.after = null;
            copy.before = null;

            ItemStack stack = resolveItemStack(copy);
            if (stack.isEmpty()) continue;

            tab.removedItems.removeIf(r -> sameEntry(r, copy));
            tab.displayItems.add(insertAt + inserted, stack);
            tab.addedItems.add(copy);
            inserted++;
        }

        if (inserted == 0) return null;

        updateItemPositions(tab);
        markDirty();
        return new int[]{insertAt, inserted};
    }

    public void removeItemFromTab(String tabId, int itemIndex) {
        removeItemsFromTab(tabId, List.of(itemIndex));
    }

    public void removeItemsFromTab(String tabId, Collection<Integer> itemIndices) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || itemIndices == null || itemIndices.isEmpty()) return;

        HolderLookup.Provider holders = editorHolders();
        boolean changed = false;
        for (int itemIndex : descending(itemIndices)) {
            if (itemIndex < 0 || itemIndex >= tab.displayItems.size()) continue;

            ItemStack removedStack = tab.displayItems.remove(itemIndex);
            changed = true;
            if (removedStack.isEmpty()) continue;

            ItemEntry removedEntry = entryFor(removedStack, holders);

            takeMatch(tab.addedItems, removedStack, holders);

            if (!tab.isCustomTab && tab.originalItemIds.contains(stackKey(removedStack, holders))) {
                if (tab.removedItems.stream().noneMatch(e -> sameEntry(e, removedEntry))) {
                    tab.removedItems.add(removedEntry);
                }
            }
        }

        if (!changed) return;

        updateItemPositions(tab);
        markDirty();
    }

    public int reorderItemsInTab(String tabId, Collection<Integer> fromIndices, int toIndex) {
        EditableTab tab = tabsMap.get(tabId);
        if (tab == null || fromIndices == null || fromIndices.isEmpty()) return -1;

        List<Integer> sorted = new ArrayList<>(new TreeSet<>(fromIndices));
        for (int index : sorted) {
            if (index < 0 || index >= tab.displayItems.size()) return -1;
        }

        List<ItemStack> moving = new ArrayList<>(sorted.size());
        for (int index : sorted) {
            moving.add(tab.displayItems.get(index));
        }

        int insertAt = Math.max(0, Math.min(tab.displayItems.size(), toIndex));
        for (int i = sorted.size() - 1; i >= 0; i--) {
            int index = sorted.get(i);
            tab.displayItems.remove(index);
            if (index < insertAt) insertAt--;
        }

        insertAt = Math.max(0, Math.min(tab.displayItems.size(), insertAt));
        tab.displayItems.addAll(insertAt, moving);

        HolderLookup.Provider holders = editorHolders();
        for (ItemStack stack : moving) {
            ItemEntry entry = entryFor(stack, holders);
            if (tab.addedItems.stream().noneMatch(e -> sameEntry(e, entry))) {
                tab.addedItems.add(entry);
            }
        }

        updateItemPositions(tab);
        markDirty();
        return insertAt;
    }

    public void updateItemPositions(EditableTab tab) {
        if (tab == null) return;

        HolderLookup.Provider holders = editorHolders();

        if (tab.isCustomTab) {
            List<ItemEntry> newAdded = new ArrayList<>();
            List<ItemEntry> pool = new ArrayList<>(tab.addedItems);

            for (ItemStack stack : tab.displayItems) {
                ItemEntry matched = takeMatch(pool, stack, holders);
                if (matched == null) {
                    matched = entryFor(stack, holders);
                }
                matched.after = null;
                matched.before = null;
                newAdded.add(matched);
            }
            tab.addedItems.clear();
            tab.addedItems.addAll(newAdded);
            return;
        }

        List<ItemEntry> newAdded = new ArrayList<>();
        List<ItemEntry> pool = new ArrayList<>(tab.addedItems);

        for (int i = 0; i < tab.displayItems.size(); i++) {
            ItemStack stack = tab.displayItems.get(i);

            ItemEntry matched = takeMatch(pool, stack, holders);
            if (matched == null) continue;

            if (i > 0) {
                matched.after = ComponentUtil.anchorFor(tab.displayItems.get(i - 1), tab.displayItems, holders);
                matched.before = null;
            } else {
                matched.after = null;
                ItemStack nextVanilla = null;
                for (int j = 1; j < tab.displayItems.size(); j++) {
                    ItemStack nextStack = tab.displayItems.get(j);
                    if (tab.originalItemIds.contains(stackKey(nextStack, holders))) {
                        nextVanilla = nextStack;
                        break;
                    }
                }
                if (nextVanilla != null) {
                    matched.before = ComponentUtil.anchorFor(nextVanilla, tab.displayItems, holders);
                } else if (tab.displayItems.size() > 1) {
                    matched.before = ComponentUtil.anchorFor(tab.displayItems.get(1), tab.displayItems, holders);
                } else {
                    matched.before = null;
                }
            }
            newAdded.add(matched);
        }

        tab.addedItems.clear();
        tab.addedItems.addAll(newAdded);
    }

    public void saveAndApply() throws Exception {
        for (EditableTab tab : tabsMap.values()) {
            updateItemPositions(tab);
        }

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID);
        Files.createDirectories(configDir);
        Path defaultFile = configDir.resolve("tabs.json");

        Map<Path, List<TabRule>> byFile = new LinkedHashMap<>();

        Map<Path, List<String>> removedByFile = new LinkedHashMap<>();
        for (EditableTab tab : tabsMap.values()) {
            if (tab.isRemoved) {
                removedByFile.computeIfAbsent(sourceFor(tab.id, defaultFile), k -> new ArrayList<>()).add(tab.id);
            }
        }
        removedByFile.forEach((file, ids) -> {
            TabRule removeRule = new TabRule();
            removeRule.action = Action.REMOVE_TAB;
            removeRule.tabs = ids;
            byFile.computeIfAbsent(file, k -> new ArrayList<>()).add(removeRule);
        });

        if (!tabOrder.isEmpty()) {
            TabRule orderRule = new TabRule();
            orderRule.action = Action.TAB_ORDER;
            orderRule.order = new ArrayList<>(tabOrder);
            Path orderFile = CreativeTabManager.GLOBAL_RULE_SOURCES.getOrDefault("tab_order", defaultFile);
            byFile.computeIfAbsent(orderFile, k -> new ArrayList<>()).add(orderRule);
        }

        for (EditableTab tab : tabsMap.values()) {
            if (tab.isCustomTab) {
                TabRule customRule = new TabRule();
                customRule.action = Action.CUSTOM_TAB;
                customRule.tabs = List.of(tab.id);
                customRule.name = tab.customDisplayName != null ? tab.customDisplayName : tab.id;
                customRule.icon = tab.customIcon != null ? tab.customIcon : "minecraft:stone";
                customRule.addItems = new ArrayList<>(tab.addedItems);
                byFile.computeIfAbsent(sourceFor(tab.id, defaultFile), k -> new ArrayList<>()).add(customRule);
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
                byFile.computeIfAbsent(sourceFor(tab.id, defaultFile), k -> new ArrayList<>()).add(modRule);
            }
        }

        Set<Path> touched = new LinkedHashSet<>(byFile.keySet());
        touched.addAll(CreativeTabManager.TAB_RULE_SOURCES.values());
        touched.addAll(CreativeTabManager.GLOBAL_RULE_SOURCES.values());
        if (!byFile.isEmpty()) touched.add(defaultFile);

        for (Path file : touched) {
            List<TabRule> rules = byFile.getOrDefault(file, List.of());
            if (rules.isEmpty() && !Files.exists(file)) continue;
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(rules, writer);
            } catch (Exception e) {
                Constants.LOG.error("Failed to write Recreative rules to {}", file, e);
                throw e;
            }
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
