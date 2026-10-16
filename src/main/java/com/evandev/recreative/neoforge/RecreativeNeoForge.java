package com.evandev.recreative.neoforge;

//? if neoforge {
import com.evandev.recreative.CommonClass;
import com.evandev.recreative.Constants;
import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.neoforge.client.NeoForgeConfigScreen;
import com.evandev.recreative.platform.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class RecreativeNeoForge {

    public RecreativeNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        CreativeTabManager.load();

        if (Platform.isPhysicalClient()) {
            NeoForgeConfigScreen.register(modContainer);
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
//?}
