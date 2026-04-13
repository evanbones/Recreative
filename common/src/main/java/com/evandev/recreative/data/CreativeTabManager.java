package com.evandev.recreative.data;

import com.evandev.recreative.Constants;
import com.evandev.recreative.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreativeTabManager {
    public static final Map<String, CreativeModeTab> RUNTIME_TABS = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File TAB_FILE = Services.PLATFORM.getConfigDirectory().resolve("recreative_tabs.json").toFile();
    private static TabConfig currentConfig = new TabConfig();

    public static void load() {
        if (TAB_FILE.exists()) {
            try (FileReader reader = new FileReader(TAB_FILE)) {
                currentConfig = GSON.fromJson(reader, TabConfig.class);
                Constants.LOG.info("Loaded Recreative tabs configuration!");
            } catch (Exception e) {
                Constants.LOG.error("Failed to load recreative_tabs.json", e);
                currentConfig = new TabConfig();
            }
        } else {
            currentConfig = new TabConfig();

            currentConfig.removedTabs.add("minecraft:example_tab");
            TabConfig.ModifyTabDef modifyTemplate = new TabConfig.ModifyTabDef();
            modifyTemplate.name = "Custom Combat Name";
            modifyTemplate.icon = "minecraft:netherite_sword";
            modifyTemplate.removeItems.add("minecraft:wooden_sword");
            currentConfig.modifyTabs.put("minecraft:combat", modifyTemplate);

            TabConfig.CustomTabDef customTemplate = new TabConfig.CustomTabDef();
            customTemplate.name = "My Custom Tab";
            customTemplate.icon = "minecraft:emerald";
            customTemplate.items.add("minecraft:diamond");
            customTemplate.items.add("minecraft:gold_ingot");
            currentConfig.customTabs.put("recreative:my_custom_tab", customTemplate);

            save();
        }

        RUNTIME_TABS.clear();
        currentConfig.customTabs.forEach((id, def) -> {
            CreativeModeTab tab = Services.PLATFORM.buildCreativeTab(
                    Component.translatable(def.name),
                    () -> {
                        Item iconItem = BuiltInRegistries.ITEM.get(new ResourceLocation(def.icon));
                        return new ItemStack(iconItem);
                    },
                    (parameters, output) -> {
                        for (String itemId : def.items) {
                            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                            output.accept(new ItemStack(item));
                        }
                    }
            );
            RUNTIME_TABS.put(id, tab);
        });
    }

    public static String getTabId(CreativeModeTab tab) {
        ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (key != null) return key.toString();

        for (Map.Entry<String, CreativeModeTab> entry : RUNTIME_TABS.entrySet()) {
            if (entry.getValue() == tab) return entry.getKey();
        }
        return "";
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(TAB_FILE)) {
            GSON.toJson(currentConfig, writer);
        } catch (Exception e) {
            Constants.LOG.error("Failed to save recreative_tabs.json", e);
        }
    }

    public static TabConfig getConfig() {
        return currentConfig;
    }
}