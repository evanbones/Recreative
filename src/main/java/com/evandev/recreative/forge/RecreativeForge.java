package com.evandev.recreative.forge;

//? if forge {
/*import com.evandev.recreative.CommonClass;
import com.evandev.recreative.Constants;
import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.forge.client.ForgeConfigScreen;
import com.evandev.recreative.platform.Platform;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class RecreativeForge {

    public RecreativeForge() {
        CreativeTabManager.load();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        if (Platform.isPhysicalClient()) {
            ForgeConfigScreen.register();
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CommonClass::init);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RecreativeCommand.register(event.getDispatcher());
    }
}
*///?}
