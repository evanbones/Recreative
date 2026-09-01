package com.evandev.recreative.client.gui.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiUtil {
    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/background.png");
    public static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/widgets.png");

    public static final ResourceLocation SCROLLBAR_BACKGROUND = ResourceLocation.fromNamespaceAndPath("recreative", "widget/scrollbar_background_vanilla");
    public static final ResourceLocation SCROLLBAR_TRACK = ResourceLocation.fromNamespaceAndPath("recreative", "widget/scrollbar_track_vanilla");
    public static final ResourceLocation SCROLLBAR_THUMB = ResourceLocation.fromNamespaceAndPath("recreative", "widget/scrollbar_thumb_vanilla");

    public static final ResourceLocation TEXT_FIELD = ResourceLocation.fromNamespaceAndPath("recreative", "widget/text_field");
    public static final ResourceLocation TEXT_FIELD_HIGHLIGHTED = ResourceLocation.fromNamespaceAndPath("recreative", "widget/text_field_highlighted");

    public static final int PANEL_PADDING = 9;
    public static final int SCROLLBAR_WIDTH = 16;
    public static final int SCROLLBAR_EXTRA_WIDTH = SCROLLBAR_WIDTH - PANEL_PADDING;

    public static final int SELECTED_SLOT = 0x7700BBFF;
    public static final int ADDED_SLOT = 0x3300FF00;
    public static final int INSERTION_MARKER = 0xFF00FFFF;

    public static final int SUBTEXT = 0xB5B5B5;
    public static final int BADGE_CUSTOM = 0x88CC88;
    public static final int BADGE_MODIFIED = 0xCCAA66;

    public static final int BADGE_HIDDEN = 0xCC7777;

    public static void drawNinePatch(GuiGraphics context, ResourceLocation texture, int x, int y, int w, int h, int u, int v, int cornerLength, int centerLength) {
        int corcen = cornerLength + centerLength;
        int innerWidth = w - cornerLength * 2;
        int innerHeight = h - cornerLength * 2;
        int coriw = cornerLength + innerWidth;
        int corih = cornerLength + innerHeight;

        context.blit(texture, x, y, cornerLength, cornerLength, u, v, cornerLength, cornerLength, 256, 256);
        context.blit(texture, x + cornerLength, y, innerWidth, cornerLength, u + cornerLength, v, centerLength, cornerLength, 256, 256);
        context.blit(texture, x + coriw, y, cornerLength, cornerLength, u + corcen, v, cornerLength, cornerLength, 256, 256);
        context.blit(texture, x, y + cornerLength, cornerLength, innerHeight, u, v + cornerLength, cornerLength, centerLength, 256, 256);
        context.blit(texture, x + cornerLength, y + cornerLength, innerWidth, innerHeight, u + cornerLength, v + cornerLength, centerLength, centerLength, 256, 256);
        context.blit(texture, x + coriw, y + cornerLength, cornerLength, innerHeight, u + corcen, v + cornerLength, cornerLength, centerLength, 256, 256);
        context.blit(texture, x, y + corih, cornerLength, cornerLength, u, v + corcen, cornerLength, cornerLength, 256, 256);
        context.blit(texture, x + cornerLength, y + corih, innerWidth, cornerLength, u + cornerLength, v + corcen, centerLength, cornerLength, 256, 256);
        context.blit(texture, x + coriw, y + corih, cornerLength, cornerLength, u + corcen, v + corcen, cornerLength, cornerLength, 256, 256);
    }

    public static void drawVanillaPanel(GuiGraphics context, int x, int y, int width, int height) {
        drawNinePatch(context, BACKGROUND, x, y, width, height, 0, 32, 8, 1);
    }

    public static void drawDialogPanel(GuiGraphics context, int x, int y, int width, int height) {
        drawNinePatch(context, BACKGROUND, x, y, width, height, 27, 0, 4, 1);
    }

    public static void drawContentPanel(GuiGraphics context, int contentX, int contentY, int contentWidth, int contentHeight) {
        drawVanillaPanel(context, contentX - PANEL_PADDING, contentY - PANEL_PADDING,
                contentWidth + PANEL_PADDING * 2, contentHeight + PANEL_PADDING * 2);
    }

    public static void drawVanillaScrollbar(GuiGraphics context, int x, int contentY, int contentHeight,
                                            double scrollAmount, int maxScroll) {
        int trackPadding = 2;
        int trackY = contentY - trackPadding;
        int trackHeight = contentHeight + trackPadding * 2;

        RenderSystem.enableBlend();
        context.blitSprite(SCROLLBAR_BACKGROUND, x, contentY - PANEL_PADDING, SCROLLBAR_WIDTH, contentHeight + PANEL_PADDING * 2);
        context.blitSprite(SCROLLBAR_TRACK, x, trackY, SCROLLBAR_WIDTH, trackHeight);

        int thumbHeight = trackHeight;
        int thumbY = trackY;
        if (maxScroll > 0) {
            float visibleFraction = (float) contentHeight / (float) (contentHeight + maxScroll);
            thumbHeight = Math.max(SCROLLBAR_WIDTH, (int) (trackHeight * visibleFraction));
            thumbY = trackY + (int) ((trackHeight - thumbHeight) * (scrollAmount / maxScroll));
        }
        context.blitSprite(SCROLLBAR_THUMB, x, thumbY, SCROLLBAR_WIDTH, thumbHeight);
        RenderSystem.disableBlend();
    }

    public static double scrollAmountFromMouse(double mouseY, int contentY, int contentHeight, int maxScroll) {
        if (maxScroll <= 0) return 0;
        int trackPadding = 2;
        int trackY = contentY - trackPadding;
        int trackHeight = contentHeight + trackPadding * 2;

        float visibleFraction = (float) contentHeight / (float) (contentHeight + maxScroll);
        int thumbHeight = Math.max(SCROLLBAR_WIDTH, (int) (trackHeight * visibleFraction));
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) return 0;

        double fraction = (mouseY - trackY - thumbHeight / 2.0) / travel;
        return Math.max(0, Math.min(1, fraction)) * maxScroll;
    }

    public static void drawSlot(GuiGraphics context, int x, int y) {
        context.blit(WIDGETS, x, y, 0, 0, 18, 18, 256, 256);
    }

    public static void drawSlotHighlight(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, 0x66FFFFFF);
    }
}
