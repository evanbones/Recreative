package com.evandev.recreative.client.integration;

import com.evandev.recreative.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.recreative.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.recreative.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.recreative.option.enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.recreative.option.enabled.tooltip"))
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.recreative.option.show_internal_ids"), config.showInternalTabIds)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.recreative.option.show_internal_ids.tooltip"))
                .setSaveConsumer(newValue -> config.showInternalTabIds = newValue)
                .build());

        return builder.build();
    }
}