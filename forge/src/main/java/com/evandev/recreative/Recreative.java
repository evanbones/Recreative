package com.evandev.recreative;

import com.evandev.recreative.client.ClientConfigSetup;
import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class Recreative {

    public Recreative() {
        CreativeTabManager.load();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(this::onRegisterTabs);

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CommonClass::init);
    }

    private void onRegisterTabs(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
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
                event.register(Registries.CREATIVE_MODE_TAB, new ResourceLocation(id), () -> tab);
            });
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RecreativeCommand.register(event.getDispatcher());
    }
}