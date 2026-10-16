package com.evandev.recreative.command;

import com.evandev.recreative.Constants;
import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.client.ClientCommandHelper;
import com.evandev.recreative.data.*;
import com.evandev.recreative.mixin.accessor.CreativeModeTabAccessor;
import com.evandev.recreative.platform.Platform;
import com.evandev.recreative.util.RegistryCompat;
import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

//? if >=26.1 {
/*import net.minecraft.server.permissions.Permissions;
*///?}

public class RecreativeCommand {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Action.class, (JsonSerializer<Action>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.name().toLowerCase()))
            .registerTypeAdapter(ItemEntry.class, (JsonSerializer<ItemEntry>) (src, typeOfSrc, context) -> ItemEntry.toJson(src))
            .create();

    private static boolean hasGamemasterPermission(CommandSourceStack source) {
        //? if <26.1 {
        return source.hasPermission(2);
        //?} else {
        /*return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        *///?}
    }

    private static Component copyPathLink(String label, String path) {
        //? if <1.21.5 {
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, path);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy path to clipboard"));
        //?} else {
        /*ClickEvent clickEvent = new ClickEvent.CopyToClipboard(path);
        HoverEvent hoverEvent = new HoverEvent.ShowText(Component.literal("Click to copy path to clipboard"));
        *///?}
        return Component.literal(label)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(clickEvent)
                        .withHoverEvent(hoverEvent)
                );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recreative")
                .requires(RecreativeCommand::hasGamemasterPermission)
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CreativeTabManager.reloadTabs();
                            context.getSource().sendSuccess(() -> Component.translatable("command.recreative.reload.success"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("editor")
                        .executes(context -> {
                            if (Platform.isPhysicalClient()) {
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
                            if (Platform.isPhysicalClient()) {
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
                hasGamemasterPermission(source),
                RegistryCompat.freshHolders(source.registryAccess())
        );

        Path baseDir = Platform.getConfigDirectory().resolve("recreative_exports").resolve("templates");
        Set<String> specialTabs = Set.of("minecraft:search", "minecraft:inventory", "minecraft:hotbar", "minecraft:op_blocks");

        Map<ResourceLocation, CreativeModeTab> allTabs = new LinkedHashMap<>();
        for (ResourceLocation id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            if (!specialTabs.contains(id.toString())) {
                CreativeModeTab tab = RegistryCompat.tab(id);
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
                    entry.components = ComponentUtil.encode(stack, dumpParams.holders());
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

            Path baseDir = Platform.getConfigDirectory().resolve("recreative_exports").resolve("templates");
            Component link = copyPathLink("recreative_exports/templates/", baseDir.toFile().getAbsolutePath());

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
        File dir = Platform.getConfigDirectory().resolve("recreative_exports").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Failed to create exports directory.");
        }

        File file = new File(dir, filename + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        }
    }

    private static void sendSuccessMessage(CommandSourceStack source, String type) {
        File dir = Platform.getConfigDirectory().resolve("recreative_exports").toFile();

        Component link = copyPathLink("recreative_exports/", dir.getAbsolutePath());

        source.sendSuccess(() -> Component.translatable("command.recreative.dump.success", type).append(" ").append(link), false);
    }
}