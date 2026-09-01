package com.evandev.recreative.client.integration;

import com.evandev.recreative.client.gui.button.CreativeScreenEditorButton;
import com.evandev.recreative.config.ModConfig;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

@EmiEntrypoint
public class RecreativeEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(CreativeModeInventoryScreen.class, (screen, consumer) -> {
            if (!ModConfig.get().showEditorButton) return;

            for (GuiEventListener child : screen.children()) {
                if (child instanceof CreativeScreenEditorButton button && button.visible) {
                    consumer.accept(new Bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
                }
            }
        });
    }
}
