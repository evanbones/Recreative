package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.gui.util.ClickInput;
import com.evandev.recreative.client.gui.util.GuiCompat;
import com.evandev.recreative.client.gui.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

public class ModEditBox extends EditBox {
    private static final int TEXT_INSET_X = 4;
    private static final int PLACEHOLDER_COLOR = 0xFF808080;

    private Component placeholder;

    public ModEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.setBordered(false);
    }

    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
    }

    private int textInsetY() {
        return (this.height - 8) / 2;
    }

    @Override
    public int getInnerWidth() {
        return this.width - TEXT_INSET_X * 2;
    }

    private boolean clearOnRightClick(ClickInput input) {
        if (input.isRight() && isMouseOver(input.x(), input.y())) {
            this.setValue("");
            this.setFocused(true);
            return true;
        }
        return false;
    }

    //? if <26.1 {
    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX - TEXT_INSET_X, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clearOnRightClick(new ClickInput(mouseX, mouseY, button))) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.isVisible()) return;
        renderBox(guiGraphics);
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        GuiCompat.pop(guiGraphics);
    }
    //?} else {
    /*@Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        super.onClick(new ClickInput(event, doubleClick).withX(event.x() - TEXT_INSET_X).event(), doubleClick);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (clearOnRightClick(new ClickInput(event, doubleClick))) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.isVisible()) return;
        renderBox(guiGraphics);
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        GuiCompat.pop(guiGraphics);
    }
    *///?}

    /**
     * Draws the field background and placeholder, leaving the pose translated to the text inset for the vanilla
     * text rendering. The caller pops the pose afterwards.
     */
    private void renderBox(GuiGraphics guiGraphics) {
        ResourceLocation sprite = this.isActive() && this.isFocused() ? GuiUtil.TEXT_FIELD_HIGHLIGHTED : GuiUtil.TEXT_FIELD;
        GuiCompat.enableBlend();
        GuiCompat.blitSprite(guiGraphics, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        GuiCompat.disableBlend();

        GuiCompat.push(guiGraphics);
        GuiCompat.translate(guiGraphics, TEXT_INSET_X, textInsetY());
        if (placeholder != null && this.getValue().isEmpty() && !this.isFocused()) {
            GuiCompat.drawString(guiGraphics, Minecraft.getInstance().font, placeholder, this.getX(), this.getY(), PLACEHOLDER_COLOR);
        }
    }
}
