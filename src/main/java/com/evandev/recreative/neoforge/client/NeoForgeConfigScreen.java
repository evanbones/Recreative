package com.evandev.recreative.neoforge.client;

//? if neoforge {
import com.evandev.recreative.client.integration.YaclConfigIntegration;
import com.evandev.recreative.platform.Platform;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class NeoForgeConfigScreen {
    public static void register(ModContainer container) {
        if (Platform.isModLoaded(YaclConfigIntegration.YACL_MOD_ID)) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> YaclConfigIntegration.createScreen(parent));
        }
    }
}
//?}
