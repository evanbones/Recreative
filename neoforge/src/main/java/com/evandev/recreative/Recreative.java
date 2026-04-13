package com.evandev.recreative;

import com.evandev.recreative.client.ClientConfigSetup;
import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class Recreative {

    public Recreative(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        CreativeTabManager.load();

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(modContainer);
        }

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CommonClass::init);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RecreativeCommand.register(event.getDispatcher());
    }
}