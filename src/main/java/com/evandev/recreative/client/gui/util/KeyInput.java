package com.evandev.recreative.client.gui.util;

//? if <26.1 {
import net.minecraft.client.gui.screens.Screen;
//?} else {
/*import net.minecraft.client.input.KeyEvent;
*///?}

/**
 * A key press, independent of the version-specific event types. Shortcut checks go through the vanilla helpers
 * because key codes are not stable across versions.
 */
public final class KeyInput {
    //? if <26.1 {
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;

    public KeyInput(int keyCode, int scanCode, int modifiers) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
    }

    public int keyCode() {
        return keyCode;
    }

    public int scanCode() {
        return scanCode;
    }

    public int modifiers() {
        return modifiers;
    }

    public boolean isCopy() {
        return Screen.isCopy(keyCode);
    }

    public boolean isCut() {
        return Screen.isCut(keyCode);
    }

    public boolean isPaste() {
        return Screen.isPaste(keyCode);
    }

    public boolean isSelectAll() {
        return Screen.isSelectAll(keyCode);
    }
    //?} else {
    /*private final KeyEvent event;

    public KeyInput(KeyEvent event) {
        this.event = event;
    }

    public KeyEvent event() {
        return event;
    }

    public int keyCode() {
        return event.key();
    }

    public boolean isCopy() {
        return event.isCopy();
    }

    public boolean isCut() {
        return event.isCut();
    }

    public boolean isPaste() {
        return event.isPaste();
    }

    public boolean isSelectAll() {
        return event.isSelectAll();
    }
    *///?}
}
