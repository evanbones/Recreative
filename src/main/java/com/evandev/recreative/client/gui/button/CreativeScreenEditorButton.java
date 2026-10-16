package com.evandev.recreative.client.gui.button;

import com.evandev.recreative.client.gui.CreativeTabEditorScreen;
import com.evandev.recreative.client.gui.util.ClickInput;
import com.evandev.recreative.client.gui.util.GuiCompat;
import com.evandev.recreative.client.gui.widget.BaseWidget;
import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.util.RegistryCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public class CreativeScreenEditorButton extends BaseWidget {

    private final Supplier<Integer> xSupplier;
    private final Supplier<Integer> ySupplier;

    public CreativeScreenEditorButton(Supplier<Integer> xSupplier, Supplier<Integer> ySupplier, Component buttonText) {
        super(
                calculateX(xSupplier.get()),
                calculateY(ySupplier.get()),
                20,
                20,
                buttonText
        );
        this.xSupplier = xSupplier;
        this.ySupplier = ySupplier;
    }

    private static int calculateX(int x) {
        var config = ModConfig.get();
        return x + config.editorButtonOffsetX;
    }

    private static int calculateY(int y) {
        var config = ModConfig.get();
        return y + config.editorButtonOffsetY;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!ModConfig.get().showEditorButton) {
            this.visible = false;
            return;
        }
        this.visible = true;

        this.setX(calculateX(this.xSupplier.get()));
        this.setY(calculateY(this.ySupplier.get()));

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        var config = ModConfig.get();
        if (config.customEditorButtonTexture == null || config.customEditorButtonTexture.isEmpty()) {
            GuiCompat.enableBlend();
            GuiCompat.drawButton(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isHovered);
            GuiCompat.disableBlend();
        } else {
            String texToUse = (this.isHovered && config.customEditorButtonTextureHovered != null && !config.customEditorButtonTextureHovered.isEmpty())
                    ? config.customEditorButtonTextureHovered : config.customEditorButtonTexture;
            ResourceLocation tex = ResourceLocation.parse(texToUse);
            GuiCompat.enableBlend();
            GuiCompat.blit(guiGraphics, tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            GuiCompat.disableBlend();
        }

        ItemStack icon = getIconStack();
        if (!icon.isEmpty()) {
            GuiCompat.fakeItem(guiGraphics, icon, this.getX() + 2, this.getY() + 2);
        }

        if (this.isHovered && config.enableButtonTooltip) {
            GuiCompat.tooltip(guiGraphics, mc.font, Component.translatable("gui.recreative.editor.button_tooltip"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        var config = ModConfig.get();
        if (config.customEditorButtonIcon == null || config.customEditorButtonIcon.isEmpty()) {
            return new ItemStack(Items.COMPASS);
        }
        try {
            return new ItemStack(RegistryCompat.item(ResourceLocation.parse(config.customEditorButtonIcon)));
        } catch (Exception e) {
            return new ItemStack(Items.COMPASS);
        }
    }

    @Override
    protected boolean onMouseClicked(ClickInput input) {
        if (this.active && this.visible && input.isLeft() && this.isMouseOver(input.x(), input.y())) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            GuiCompat.setScreen(new CreativeTabEditorScreen(GuiCompat.getScreen()));
            return true;
        }
        return false;
    }
}
