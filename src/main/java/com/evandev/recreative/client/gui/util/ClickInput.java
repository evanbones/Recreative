package com.evandev.recreative.client.gui.util;

//? if >=26.1 {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

/**
 * A mouse button press or release
 */
public record ClickInput(double x, double y, int button, int modifiers, boolean doubleClick) {
    //? if >=26.1 {
    /*private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
    private static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;
    *///?} else {
    private static final int LEFT = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    private static final int RIGHT = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    //?}

    public ClickInput(double x, double y, int button) {
        this(x, y, button, 0, false);
    }

    //? if >=26.1 {
    /*public ClickInput(MouseButtonEvent event, boolean doubleClick) {
        this(event.x(), event.y(), event.button(), event.modifiers(), doubleClick);
    }

    public MouseButtonEvent event() {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
    }
    *///?}

    public boolean isLeft() {
        return button == LEFT;
    }

    public boolean isRight() {
        return button == RIGHT;
    }

    public ClickInput withX(double newX) {
        return new ClickInput(newX, y, button, modifiers, doubleClick);
    }
}
