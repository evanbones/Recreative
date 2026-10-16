package com.evandev.recreative.client;

import com.evandev.recreative.client.gui.CreativeTabEditorScreen;
import com.evandev.recreative.client.gui.util.GuiCompat;
import net.minecraft.client.Minecraft;

public class ClientCommandHelper {
    public static void openEditorScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> GuiCompat.setScreen(new CreativeTabEditorScreen(null)));
    }
}
