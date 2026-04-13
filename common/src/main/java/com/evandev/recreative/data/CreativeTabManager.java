package com.evandev.recreative.data;

import com.evandev.recreative.Constants;
import com.evandev.recreative.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.item.CreativeModeTab;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class CreativeTabManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File TAB_FILE = Services.PLATFORM.getConfigDirectory().resolve("recreative_tabs.json").toFile();
    public static List<CreativeModeTab> CACHED_SORTED_TABS = null;
    private static TabConfig currentConfig = new TabConfig();

    public static void load() {
        CACHED_SORTED_TABS = null;

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