package com.evandev.recreative.client.integration;

import com.evandev.recreative.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.recreative.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.recreative.category.general"));

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.recreative.option.enabled"))
                .description(OptionDescription.of(Component.translatable("config.recreative.option.enabled.tooltip")))
                .binding(true, () -> config.enabled, val -> config.enabled = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.recreative.option.show_internal_ids"))
                .description(OptionDescription.of(Component.translatable("config.recreative.option.show_internal_ids.tooltip")))
                .binding(false, () -> config.showInternalTabIds, val -> config.showInternalTabIds = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        return builder
                .category(generalCategory.build())
                .build()
                .generateScreen(parent);
    }
}
