package com.evandev.recreative.fabric.compat;

//? if fabric {
/*import com.evandev.recreative.client.integration.YaclConfigIntegration;
import com.evandev.recreative.platform.Platform;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Platform.isModLoaded(YaclConfigIntegration.YACL_MOD_ID)) {
            return YaclConfigIntegration::createScreen;
        }
        return parent -> null;
    }
}
*///?}
