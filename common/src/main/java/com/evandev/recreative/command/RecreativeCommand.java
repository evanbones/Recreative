package com.evandev.recreative.command;

import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.data.CreativeTabManager;
import com.evandev.recreative.mixin.accessor.CreativeModeTabsAccessor;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public class RecreativeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recreative")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            ModConfig.load();
                            CreativeTabManager.load();

                            CreativeModeTab.ItemDisplayParameters params = CreativeModeTabsAccessor.getCachedParameters();
                            if (params != null) {
                                for (CreativeModeTab tab : CreativeTabManager.RUNTIME_TABS.values()) {
                                    tab.buildContents(params);
                                }
                            }

                            context.getSource().sendSuccess(() -> Component.literal("Recreative configs and tabs reloaded!"), true);
                            return 1;
                        })
                )
        );
    }
}