package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.gui.util.GuiUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropdownWidget extends AbstractWidget {
    private static final int ROW_H = 12;
    private static final int MAX_VISIBLE_ROWS = 12;

    private final List<String> values = new ArrayList<>();
    private final Function<String, Component> labeller;
    private final Consumer<String> onSelect;
    private final Component title;

    private int selectedIndex = 0;
    private boolean open = false;
    private double scrollAmount = 0;

    public DropdownWidget(int x, int y, int width, int height, Component title,
                          List<String> values, Function<String, Component> labeller, Consumer<String> onSelect) {
        super(x, y, width, height, title);
        this.title = title;
        this.labeller = labeller;
        this.onSelect = onSelect;
        this.values.addAll(values);
    }

    public void setValues(List<String> newValues) {
        String previous = getSelectedValue();
        this.values.clear();
        this.values.addAll(newValues);
        int idx = this.values.indexOf(previous);
        this.selectedIndex = Math.max(idx, 0);
        this.scrollAmount = 0;
    }

    public String getSelectedValue() {
        return selectedIndex >= 0 && selectedIndex < values.size() ? values.get(selectedIndex) : null;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

    private int visibleRows() {
        return Math.min(MAX_VISIBLE_ROWS, Math.max(1, values.size()));
    }

    private int listContentHeight() {
        return visibleRows() * ROW_H;
    }

    private int listContentY() {
        return this.getY() + this.height + 2 + GuiUtil.PANEL_PADDING;
    }

    private int listContentX() {
        return this.getX() + GuiUtil.PANEL_PADDING;
    }

    private int listContentWidth() {
        return this.width - GuiUtil.PANEL_PADDING * 2 - GuiUtil.SCROLLBAR_EXTRA_WIDTH;
    }

    private int maxScroll() {
        return Math.max(0, values.size() * ROW_H - listContentHeight());
    }

    public boolean isOverList(double mouseX, double mouseY) {
        if (!open) return false;
        int x0 = this.getX();
        int x1 = listContentX() + listContentWidth() + GuiUtil.SCROLLBAR_WIDTH;
        int y0 = listContentY() - GuiUtil.PANEL_PADDING;
        int y1 = listContentY() + listContentHeight() + GuiUtil.PANEL_PADDING;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int rowIndexAt(double mouseX, double mouseY) {
        int cx = listContentX();
        int cy = listContentY();
        if (mouseX < cx || mouseX >= cx + listContentWidth()) return -1;
        if (mouseY < cy || mouseY >= cy + listContentHeight()) return -1;
        int idx = (int) ((mouseY - cy + scrollAmount) / ROW_H);
        return idx >= 0 && idx < values.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (open) {
            int idx = rowIndexAt(mouseX, mouseY);
            if (idx >= 0) {
                this.selectedIndex = idx;
                this.open = false;
                if (onSelect != null) onSelect.accept(values.get(idx));
                return true;
            }
            if (isOverList(mouseX, mouseY)) return true;
            this.open = false;
            return clicked(mouseX, mouseY);
        }

        if (clicked(mouseX, mouseY)) {
            this.open = true;
            scrollToSelected();
            return true;
        }
        return false;
    }

    private void scrollToSelected() {
        int target = selectedIndex * ROW_H - listContentHeight() / 2;
        this.scrollAmount = Mth.clamp(target, 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (open && isOverList(mouseX, mouseY)) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * ROW_H, 0, maxScroll());
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(this.isHovered || open ? GuiUtil.TEXT_FIELD_HIGHLIGHTED : GuiUtil.TEXT_FIELD,
                this.getX(), this.getY(), this.width, this.height);
        RenderSystem.disableBlend();

        var font = Minecraft.getInstance().font;
        Component label = Component.empty().append(title).append(labelFor(getSelectedValue()));
        int textY = this.getY() + (this.height - 8) / 2;
        int maxLabelW = this.width - 8;
        String text = label.getString();
        if (font.width(text) > maxLabelW && maxLabelW > 8) {
            text = font.plainSubstrByWidth(text, maxLabelW - font.width("..")) + "..";
        }
        guiGraphics.drawString(font, text, this.getX() + 4, textY, 0xFFFFFF);
    }

    private Component labelFor(String value) {
        return labeller != null ? labeller.apply(value) : Component.literal(String.valueOf(value));
    }

    public void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!open) return;

        int cx = listContentX();
        int cy = listContentY();
        int cw = listContentWidth();
        int ch = listContentHeight();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);

        GuiUtil.drawContentPanel(guiGraphics, cx, cy, cw, ch);

        var font = Minecraft.getInstance().font;
        int hovered = rowIndexAt(mouseX, mouseY);

        guiGraphics.enableScissor(cx, cy, cx + cw, cy + ch);
        int first = (int) (scrollAmount / ROW_H);
        int offset = (int) (scrollAmount % ROW_H);
        for (int r = 0; r <= visibleRows(); r++) {
            int idx = first + r;
            if (idx < 0 || idx >= values.size()) continue;

            int rowY = cy + r * ROW_H - offset;
            if (idx == selectedIndex) {
                guiGraphics.fill(cx, rowY, cx + cw, rowY + ROW_H, GuiUtil.SELECTED_SLOT);
            } else if (idx == hovered) {
                guiGraphics.fill(cx, rowY, cx + cw, rowY + ROW_H, 0x33FFFFFF);
            }

            String text = labelFor(values.get(idx)).getString();
            if (font.width(text) > cw - 6) {
                text = font.plainSubstrByWidth(text, cw - 6 - font.width("..")) + "..";
            }
            guiGraphics.drawString(font, text, cx + 3, rowY + 2, 0xFFFFFF);
        }
        guiGraphics.disableScissor();

        GuiUtil.drawVanillaScrollbar(guiGraphics, cx + cw, cy, ch, scrollAmount, maxScroll());

        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
