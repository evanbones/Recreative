package com.evandev.recreative;

import com.evandev.recreative.command.RecreativeCommand;
import com.evandev.recreative.data.CreativeTabManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Recreative implements ModInitializer {

    @Override
    public void onInitialize() {
        CreativeTabManager.load();
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RecreativeCommand.register(dispatcher);
        });
    }
}