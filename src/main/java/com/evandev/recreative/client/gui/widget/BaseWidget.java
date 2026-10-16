package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.gui.util.ClickInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

public abstract class BaseWidget extends AbstractWidget {
    protected BaseWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

    protected boolean onMouseClicked(ClickInput input) {
        return superMouseClicked(input);
    }

    protected boolean onMouseReleased(ClickInput input) {
        return superMouseReleased(input);
    }

    protected boolean onMouseDragged(ClickInput input, double dragX, double dragY) {
        return superMouseDragged(input, dragX, dragY);
    }

    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) {
        return false;
    }

    //? if <26.1 {
    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderContent(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return onMouseClicked(new ClickInput(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return onMouseReleased(new ClickInput(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return onMouseDragged(new ClickInput(mouseX, mouseY, button), dragX, dragY);
    }

    protected final boolean superMouseClicked(ClickInput input) {
        return super.mouseClicked(input.x(), input.y(), input.button());
    }

    protected final boolean superMouseReleased(ClickInput input) {
        return super.mouseReleased(input.x(), input.y(), input.button());
    }

    protected final boolean superMouseDragged(ClickInput input, double dragX, double dragY) {
        return super.mouseDragged(input.x(), input.y(), input.button(), dragX, dragY);
    }
    //?} else {
    /*@Override
    protected void extractWidgetRenderState(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderContent(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        return onMouseClicked(new ClickInput(event, doubleClick));
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        return onMouseReleased(new ClickInput(event, false));
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        return onMouseDragged(new ClickInput(event, false), dragX, dragY);
    }

    protected final boolean superMouseClicked(ClickInput input) {
        return super.mouseClicked(input.event(), input.doubleClick());
    }

    protected final boolean superMouseReleased(ClickInput input) {
        return super.mouseReleased(input.event());
    }

    protected final boolean superMouseDragged(ClickInput input, double dragX, double dragY) {
        return super.mouseDragged(input.event(), dragX, dragY);
    }
    *///?}

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }
    *///?}

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
