package com.evandev.recreative.client.integration;

import com.evandev.recreative.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class YaclConfigIntegration {
    public static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigCategory general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.recreative.category.general"))
                .option(toggle("enabled", true, () -> config.enabled, v -> config.enabled = v))
                .option(toggle("show_internal_ids", false, () -> config.showInternalTabIds, v -> config.showInternalTabIds = v))
                .build();

        ConfigCategory editing = ConfigCategory.createBuilder()
                .name(Component.translatable("config.recreative.category.editing"))
                .option(toggle("show_editor_button", true, () -> config.showEditorButton, v -> config.showEditorButton = v))
                .option(toggle("enable_button_tooltip", true, () -> config.enableButtonTooltip, v -> config.enableButtonTooltip = v))
                .option(integer("editor_button_offset_x", 0, () -> config.editorButtonOffsetX, v -> config.editorButtonOffsetX = v))
                .option(integer("editor_button_offset_y", 0, () -> config.editorButtonOffsetY, v -> config.editorButtonOffsetY = v))
                .option(string("custom_editor_button_icon", "minecraft:compass", () -> config.customEditorButtonIcon, v -> config.customEditorButtonIcon = v))
                .option(string("custom_editor_button_texture", "", () -> config.customEditorButtonTexture, v -> config.customEditorButtonTexture = v))
                .option(string("custom_editor_button_texture_hovered", "", () -> config.customEditorButtonTextureHovered, v -> config.customEditorButtonTextureHovered = v))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.recreative.title"))
                .category(general)
                .category(editing)
                .save(ModConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> toggle(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.recreative.option." + key))
                .description(OptionDescription.of(Component.translatable("config.recreative.option." + key + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> integer(String key, int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("config.recreative.option." + key))
                .description(OptionDescription.of(Component.translatable("config.recreative.option." + key + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(IntegerFieldControllerBuilder::create)
                .build();
    }

    private static Option<String> string(String key, String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        return Option.<String>createBuilder()
                .name(Component.translatable("config.recreative.option." + key))
                .description(OptionDescription.of(Component.translatable("config.recreative.option." + key + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(StringControllerBuilder::create)
                .build();
    }
}
