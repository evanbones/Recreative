package com.evandev.recreative.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
//?} else {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

//? if <1.21 {
/*import com.evandev.recreative.client.gui.util.legacy.LegacySprites;
import net.minecraft.client.gui.components.AbstractWidget;
*///?}

public final class GuiCompat {
    public static void push(GuiGraphics g) {
        //? if <26.1 {
        g.pose().pushPose();
        //?} else {
        /*g.pose().pushMatrix();
        *///?}
    }

    public static void pop(GuiGraphics g) {
        //? if <26.1 {
        g.pose().popPose();
        //?} else {
        /*g.pose().popMatrix();
        *///?}
    }

    public static void translate(GuiGraphics g, double x, double y) {
        //? if <26.1 {
        g.pose().translate(x, y, 0);
        //?} else {
        /*g.pose().translate((float) x, (float) y);
        *///?}
    }

    /**
     * Starts drawing above everything rendered so far this frame. Must be paired with {@link #endOverlay}
     */
    public static void beginOverlay(GuiGraphics g, float z) {
        //? if <26.1 {
        g.pose().pushPose();
        g.pose().translate(0, 0, z);
        //?} else {
        /*g.nextStratum();
        g.pose().pushMatrix();
        *///?}
    }

    public static void endOverlay(GuiGraphics g) {
        pop(g);
    }

    public static void enableBlend() {
        //? if <26.1 {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void disableBlend() {
        //? if <26.1 {
        RenderSystem.disableBlend();
        //?}
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        //? if <26.1 {
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?}
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        //? if <26.1 {
        g.blit(texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight);
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight);
        *///?}
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        //? if <1.21 {
        /*LegacySprites.blit(g, sprite, x, y, width, height);
        *///?} else if <26.1 {
        g.blitSprite(sprite, x, y, width, height);
        //?} else {
        /*g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        *///?}
    }

    public static void drawButton(GuiGraphics g, int x, int y, int width, int height, boolean hovered) {
        //? if <1.21 {
        /*g.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, width, height, 20, 4, 200, 20, 0, hovered ? 86 : 66);
        *///?} else {
        blitSprite(g, ResourceLocation.withDefaultNamespace(hovered ? "widget/button_highlighted" : "widget/button"), x, y, width, height);
        //?}
    }

    public static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        //? if <26.1 {
        g.renderOutline(x, y, width, height, color);
        //?} else {
        /*g.outline(x, y, width, height, color);
        *///?}
    }

    /**
     * Treats text colors without an alpha channel as opaque.
     */
    private static int opaque(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color) {
        drawString(g, font, text, x, y, color, true);
    }

    public static void drawString(GuiGraphics g, Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        //? if <26.1 {
        g.drawString(font, text, x, y, opaque(color), shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        *///?}
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        //? if <26.1 {
        g.drawString(font, text, x, y, opaque(color));
        //?} else {
        /*g.text(font, text, x, y, opaque(color));
        *///?}
    }

    public static void drawString(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if <26.1 {
        g.drawString(font, text, x, y, opaque(color), shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        *///?}
    }

    public static void drawString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if <26.1 {
        g.drawString(font, text, x, y, opaque(color), shadow);
        //?} else {
        /*g.text(font, text, x, y, opaque(color), shadow);
        *///?}
    }

    public static void drawCenteredString(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        //? if <26.1 {
        g.drawCenteredString(font, text, x, y, opaque(color));
        //?} else {
        /*g.centeredText(font, text, x, y, opaque(color));
        *///?}
    }

    public static void drawCenteredString(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if <26.1 {
        g.drawCenteredString(font, text, x, y, opaque(color));
        //?} else {
        /*g.centeredText(font, text, x, y, opaque(color));
        *///?}
    }

    public static void fakeItem(GuiGraphics g, ItemStack stack, int x, int y) {
        //? if <26.1 {
        g.renderFakeItem(stack, x, y);
        //?} else {
        /*g.fakeItem(stack, x, y);
        *///?}
    }

    public static void itemDecorations(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        //? if <26.1 {
        g.renderItemDecorations(font, stack, x, y);
        //?} else {
        /*g.itemDecorations(font, stack, x, y);
        *///?}
    }

    public static void tooltip(GuiGraphics g, Font font, Component text, int mouseX, int mouseY) {
        //? if <26.1 {
        g.renderTooltip(font, text, mouseX, mouseY);
        //?} else {
        /*g.setTooltipForNextFrame(font, text, mouseX, mouseY);
        *///?}
    }

    public static void tooltip(GuiGraphics g, Font font, List<Component> lines, Optional<TooltipComponent> image, int mouseX, int mouseY) {
        //? if <26.1 {
        g.renderTooltip(font, lines, image, mouseX, mouseY);
        //?} else {
        /*g.setTooltipForNextFrame(font, lines, image, mouseX, mouseY);
        *///?}
    }

    public static void tooltip(GuiGraphics g, Font font, ItemStack stack, int mouseX, int mouseY) {
        //? if <26.1 {
        g.renderTooltip(font, stack, mouseX, mouseY);
        //?} else {
        /*g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        *///?}
    }

    public static boolean hasControlDown() {
        //? if <26.1 {
        return Screen.hasControlDown();
        //?} else {
        /*return Minecraft.getInstance().hasControlDown();
        *///?}
    }

    public static boolean hasShiftDown() {
        //? if <26.1 {
        return Screen.hasShiftDown();
        //?} else {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?}
    }

    public static boolean hasAltDown() {
        //? if <26.1 {
        return Screen.hasAltDown();
        //?} else {
        /*return Minecraft.getInstance().hasAltDown();
        *///?}
    }

    public static void setScreen(@Nullable Screen screen) {
        //? if <26.2 {
        Minecraft.getInstance().setScreen(screen);
        //?} else {
        /*Minecraft.getInstance().gui.setScreen(screen);
        *///?}
    }

    public static @Nullable Screen getScreen() {
        //? if <26.2 {
        return Minecraft.getInstance().screen;
        //?} else {
        /*return Minecraft.getInstance().gui.screen();
        *///?}
    }
}
