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

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.recreative.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.recreative.option.show_editor_button"), config.showEditorButton)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.recreative.option.show_editor_button.tooltip"))
                .setSaveConsumer(newValue -> config.showEditorButton = newValue)
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.recreative.option.enable_button_tooltip"), config.enableButtonTooltip)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.recreative.option.enable_button_tooltip.tooltip"))
                .setSaveConsumer(newValue -> config.enableButtonTooltip = newValue)
                .build());

        editing.addEntry(entryBuilder.startIntField(Component.translatable("config.recreative.option.editor_button_offset_x"), config.editorButtonOffsetX)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("config.recreative.option.editor_button_offset_x.tooltip"))
                .setSaveConsumer(newValue -> config.editorButtonOffsetX = newValue)
                .build());

        editing.addEntry(entryBuilder.startIntField(Component.translatable("config.recreative.option.editor_button_offset_y"), config.editorButtonOffsetY)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("config.recreative.option.editor_button_offset_y.tooltip"))
                .setSaveConsumer(newValue -> config.editorButtonOffsetY = newValue)
                .build());

        editing.addEntry(entryBuilder.startStrField(Component.translatable("config.recreative.option.custom_editor_button_icon"), config.customEditorButtonIcon)
                .setDefaultValue("minecraft:compass")
                .setTooltip(Component.translatable("config.recreative.option.custom_editor_button_icon.tooltip"))
                .setSaveConsumer(newValue -> config.customEditorButtonIcon = newValue)
                .build());

        editing.addEntry(entryBuilder.startStrField(Component.translatable("config.recreative.option.custom_editor_button_texture"), config.customEditorButtonTexture)
                .setDefaultValue("")
                .setTooltip(Component.translatable("config.recreative.option.custom_editor_button_texture.tooltip"))
                .setSaveConsumer(newValue -> config.customEditorButtonTexture = newValue)
                .build());

        editing.addEntry(entryBuilder.startStrField(Component.translatable("config.recreative.option.custom_editor_button_texture_hovered"), config.customEditorButtonTextureHovered)
                .setDefaultValue("")
                .setTooltip(Component.translatable("config.recreative.option.custom_editor_button_texture_hovered.tooltip"))
                .setSaveConsumer(newValue -> config.customEditorButtonTextureHovered = newValue)
                .build());

        return builder.build();
    }
}