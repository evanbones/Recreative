package com.evandev.recreative.command;

import com.evandev.recreative.Constants;
import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.client.ClientCommandHelper;
import com.evandev.recreative.data.*;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.evandev.recreative.platform.Services;
import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecreativeCommand {
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

    private static HolderLookup.Provider recreative$freshHolders(HolderLookup.Provider delegate) {
        return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return delegate.listRegistries();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                return delegate.lookup(registryKey);
            }
        };
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recreative")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CreativeTabManager.reloadTabs();
                            context.getSource().sendSuccess(() -> Component.translatable("command.recreative.reload.success"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("editor")
                        .executes(context -> {
                            if (Services.PLATFORM.isPhysicalClient()) {
                                ClientCommandHelper.openEditorScreen();
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.translatable("command.recreative.editor.client_only"));
                                return 0;
                            }
                        })
                )
                .then(Commands.literal("edit")
                        .executes(context -> {
                            if (Services.PLATFORM.isPhysicalClient()) {
                                ClientCommandHelper.openEditorScreen();
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.translatable("command.recreative.editor.client_only"));
                                return 0;
                            }
                        })
                )
                .then(Commands.literal("dump")
                        .then(Commands.literal("tabs").executes(c -> executeDump(c, "tabs")))
                        .then(Commands.literal("items").executes(c -> executeDump(c, "items")))
                        .then(Commands.literal("blocks").executes(c -> executeDump(c, "blocks")))
                        .then(Commands.literal("templates").executes(RecreativeCommand::executeDumpTemplates))
                        .then(Commands.literal("all").executes(c -> executeDump(c, "all")))
                )
        );
    }

    private static void dumpTemplatesCore(CommandContext<CommandSourceStack> context) throws Exception {
        CommandSourceStack source = context.getSource();
        CreativeModeTab.ItemDisplayParameters dumpParams = new CreativeModeTab.ItemDisplayParameters(
                source.enabledFeatures(),
                source.hasPermission(2),
                recreative$freshHolders(source.registryAccess())
        );

        Path baseDir = Services.PLATFORM.getConfigDirectory().resolve("recreative_exports").resolve("templates");
        Set<String> specialTabs = Set.of("minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks");

        Map<ResourceLocation, CreativeModeTab> allTabs = new LinkedHashMap<>();
        for (ResourceLocation id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            if (!specialTabs.contains(id.toString())) {
                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
                if (tab != null) {
                    allTabs.put(id, tab);
                }
            }
        }
        for (Map.Entry<String, CreativeModeTab> entry : CreativeTabManager.RUNTIME_TABS.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id != null && !specialTabs.contains(id.toString()) && !allTabs.containsKey(id)) {
                allTabs.put(id, entry.getValue());
            }
        }

        for (Map.Entry<ResourceLocation, CreativeModeTab> tabEntry : allTabs.entrySet()) {
            ResourceLocation id = tabEntry.getKey();
            CreativeModeTab tab = tabEntry.getValue();

            TabRule rule = new TabRule();
            rule.action = Action.MODIFY_TAB;
            rule.tabs.add(id.toString());

            ItemStack iconStack = tab.getIconItem();
            if (!iconStack.isEmpty()) {
                ResourceLocation iconId = BuiltInRegistries.ITEM.getKey(iconStack.getItem());
                if (!iconId.toString().equals("minecraft:air")) {
                    rule.icon = iconId.toString();
                }
            } else if (tab instanceof ICustomIconTab customIconTab && customIconTab.recreative$getCustomIcon() != null) {
                rule.icon = customIconTab.recreative$getCustomIcon().toString();
            }

            Collection<ItemStack> serverItems = null;
            try {
                tab.buildContents(dumpParams);
                serverItems = tab.getDisplayItems();
            } catch (Throwable t) {
                Constants.LOG.error("Failed to safely build contents for tab {}", id, t);
            }

            if (serverItems == null || serverItems.isEmpty()) {
                List<ItemStack> fallbackItems = new ArrayList<>();
                try {
                    ((CreativeModeTabAccessor) tab).getDisplayItemsGenerator().accept(dumpParams, (stack, visibility) -> {
                        if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY) {
                            fallbackItems.add(stack);
                        }
                    });
                } catch (Throwable t) {
                    Constants.LOG.error("Failed to safely generate fallback items for tab {}", id, t);
                }
                serverItems = fallbackItems;
            }

            for (ItemStack stack : serverItems) {
                if (stack.isEmpty() || stack.getCount() != 1) continue;
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

                if (!itemId.toString().equals("minecraft:air")) {
                    ItemEntry entry = new ItemEntry(itemId.toString());
                    DataComponentPatch patch = stack.getComponentsPatch();

                    if (!patch.isEmpty()) {
                        try {
                            JsonElement componentJson = DataComponentPatch.CODEC.encodeStart(
                                    RegistryOps.create(JsonOps.INSTANCE, dumpParams.holders()),
                                    patch
                            ).getOrThrow(IllegalStateException::new);
                            entry.components = componentJson.toString();
                        } catch (Exception e) {
                            Constants.LOG.error("Failed to serialize components for item {}", itemId, e);
                        }
                    }
                    rule.addItems.add(entry);
                }
            }

            File modDir = baseDir.resolve(id.getNamespace()).toFile();
            if (!modDir.exists() && !modDir.mkdirs()) {
                continue;
            }

            File file = new File(modDir, id.getPath() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(List.of(rule), writer);
            }
        }
    }

    private static int executeDumpTemplates(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            dumpTemplatesCore(context);

            Path baseDir = Services.PLATFORM.getConfigDirectory().resolve("recreative_exports").resolve("templates");
            Component link = Component.literal("recreative_exports/templates/")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, baseDir.toFile().getAbsolutePath()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy path to clipboard")))
                    );

            source.sendSuccess(() -> Component.translatable("command.recreative.dump.success", "templates").append(" ").append(link), false);
            return 1;
        } catch (Exception e) {
            Constants.LOG.error("Failed to dump tab templates", e);
            source.sendFailure(Component.translatable("command.recreative.dump.failure", "templates"));
            return 0;
        }
    }

    private static int executeDump(CommandContext<CommandSourceStack> context, String type) {
        CommandSourceStack source = context.getSource();
        try {
            if (type.equals("all")) {
                dumpData("tabs", getTabs());
                dumpData("items", getItems());
                dumpData("blocks", getBlocks());
                dumpTemplatesCore(context);
                sendSuccessMessage(source, "all");
            } else {
                List<String> data = switch (type) {
                    case "tabs" -> getTabs();
                    case "items" -> getItems();
                    case "blocks" -> getBlocks();
                    default -> new ArrayList<>();
                };
                dumpData(type, data);
                sendSuccessMessage(source, type);
            }
            return 1;
        } catch (Exception e) {
            Constants.LOG.error("Failed to dump data for: {}", type, e);
            source.sendFailure(Component.translatable("command.recreative.dump.failure", type));
            return 0;
        }
    }

    private static List<String> getTabs() {
        List<String> tabs = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());

        for (String customTab : CreativeTabManager.RUNTIME_TABS.keySet()) {
            if (!tabs.contains(customTab)) {
                tabs.add(customTab);
            }
        }
        return tabs;
    }

    private static List<String> getItems() {
        return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).toList();
    }

    private static List<String> getBlocks() {
        return BuiltInRegistries.BLOCK.keySet().stream().map(ResourceLocation::toString).toList();
    }

    private static void dumpData(String filename, List<String> data) throws Exception {
        File dir = Services.PLATFORM.getConfigDirectory().resolve("recreative_exports").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Failed to create exports directory.");
        }

        File file = new File(dir, filename + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        }
    }

    private static void sendSuccessMessage(CommandSourceStack source, String type) {
        File dir = Services.PLATFORM.getConfigDirectory().resolve("recreative_exports").toFile();

        Component link = Component.literal("recreative_exports/")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dir.getAbsolutePath()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy path to clipboard")))
                );

        source.sendSuccess(() -> Component.translatable("command.recreative.dump.success", type).append(" ").append(link), false);
    }
}