package com.evandev.recreative.compat;

import com.evandev.recreative.client.integration.YaclConfigIntegration;
import com.evandev.recreative.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            return YaclConfigIntegration::createScreen;
        }
        return null;
    }
}