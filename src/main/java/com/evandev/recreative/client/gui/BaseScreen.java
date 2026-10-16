package com.evandev.recreative.client.gui;

import com.evandev.recreative.client.gui.util.ClickInput;
import com.evandev.recreative.client.gui.util.KeyInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    /**
     * Draws on top of the vanilla menu background, below the widgets
     */
    protected void renderBackgroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    /**
     * Draws on top of the widgets
     */
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    protected boolean onMouseClicked(ClickInput input) {
        return superMouseClicked(input);
    }

    protected boolean onMouseReleased(ClickInput input) {
        return superMouseReleased(input);
    }

    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) {
        return superMouseScrolled(mouseX, mouseY, scrollY);
    }

    protected boolean onKeyPressed(KeyInput input) {
        return superKeyPressed(input);
    }

    //? if <1.21 {
    /*@Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        renderBackgroundLayer(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    *///?} else if <26.1 {
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        renderBackgroundLayer(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    //?} else {
    /*@Override
    public void extractBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        renderBackgroundLayer(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    *///?}

    //? if <26.1 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return onMouseClicked(new ClickInput(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return onMouseReleased(new ClickInput(mouseX, mouseY, button));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return onKeyPressed(new KeyInput(keyCode, scanCode, modifiers));
    }

    protected final boolean superMouseClicked(ClickInput input) {
        return super.mouseClicked(input.x(), input.y(), input.button());
    }

    protected final boolean superMouseReleased(ClickInput input) {
        return super.mouseReleased(input.x(), input.y(), input.button());
    }

    protected final boolean superKeyPressed(KeyInput input) {
        return super.keyPressed(input.keyCode(), input.scanCode(), input.modifiers());
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        return onMouseClicked(new ClickInput(event, doubleClick));
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        return onMouseReleased(new ClickInput(event, false));
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        return onKeyPressed(new KeyInput(event));
    }

    protected final boolean superMouseClicked(ClickInput input) {
        return super.mouseClicked(input.event(), input.doubleClick());
    }

    protected final boolean superMouseReleased(ClickInput input) {
        return super.mouseReleased(input.event());
    }

    protected final boolean superKeyPressed(KeyInput input) {
        return super.keyPressed(input.event());
    }
    *///?}

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }

    protected final boolean superMouseScrolled(double mouseX, double mouseY, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, 0, scrollY);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }

    protected final boolean superMouseScrolled(double mouseX, double mouseY, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }
    *///?}
}
