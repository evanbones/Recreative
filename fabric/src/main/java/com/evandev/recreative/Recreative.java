package com.evandev.recreative;

import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Recreative implements ModInitializer {

    @Override
    public void onInitialize() {
        CreativeTabManager.load();
        CommonClass.init();

        CreativeTabManager.getConfig().customTabs.forEach((id, def) -> {
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
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new ResourceLocation(id), tab);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RecreativeCommand.register(dispatcher);
        });
    }
}