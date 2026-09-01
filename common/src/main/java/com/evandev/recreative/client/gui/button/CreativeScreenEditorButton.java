package com.evandev.recreative.client.gui.button;

import com.evandev.recreative.client.gui.CreativeTabEditorScreen;
import com.evandev.recreative.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CreativeScreenEditorButton extends AbstractButton {

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
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!ModConfig.get().showEditorButton) {
            this.visible = false;
            return;
        }
        this.visible = true;

        this.setX(calculateX(this.xSupplier.get()));
        this.setY(calculateY(this.ySupplier.get()));
        this.setWidth(20);
        this.setHeight(20);

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        var config = ModConfig.get();
        if (config.customEditorButtonTexture == null || config.customEditorButtonTexture.isEmpty()) {
            ResourceLocation sprite = this.isHovered ? ResourceLocation.withDefaultNamespace("widget/button_highlighted") : ResourceLocation.withDefaultNamespace("widget/button");
            guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        } else {
            String texToUse = (this.isHovered && config.customEditorButtonTextureHovered != null && !config.customEditorButtonTextureHovered.isEmpty())
                    ? config.customEditorButtonTextureHovered : config.customEditorButtonTexture;
            ResourceLocation tex = ResourceLocation.parse(texToUse);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            RenderSystem.disableBlend();
        }

        ItemStack icon = getIconStack();
        if (!icon.isEmpty()) {
            guiGraphics.renderFakeItem(icon, this.getX() + 2, this.getY() + 2);
        }

        if (this.isHovered && config.enableButtonTooltip) {
            guiGraphics.renderTooltip(mc.font, Component.translatable("gui.recreative.editor.button_tooltip"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        var config = ModConfig.get();
        if (config.customEditorButtonIcon == null || config.customEditorButtonIcon.isEmpty()) {
            return new ItemStack(Items.COMPASS);
        }
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(config.customEditorButtonIcon)));
        } catch (Exception e) {
            return new ItemStack(Items.COMPASS);
        }
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new CreativeTabEditorScreen(mc.screen));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
