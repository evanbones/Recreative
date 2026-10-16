package com.evandev.recreative.forge.client;

//? if forge {
/*import com.evandev.recreative.client.integration.YaclConfigIntegration;
import com.evandev.recreative.platform.Platform;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public class ForgeConfigScreen {
    public static void register() {
        if (Platform.isModLoaded(YaclConfigIntegration.YACL_MOD_ID)) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> YaclConfigIntegration.createScreen(parent)));
        }
    }
}
*///?}
