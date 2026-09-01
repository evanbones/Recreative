package com.evandev.recreative.client;

import com.evandev.recreative.client.gui.CreativeTabEditorScreen;
import net.minecraft.client.Minecraft;

public class ClientCommandHelper {
    public static void openEditorScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> mc.setScreen(new CreativeTabEditorScreen(null)));
    }
}
