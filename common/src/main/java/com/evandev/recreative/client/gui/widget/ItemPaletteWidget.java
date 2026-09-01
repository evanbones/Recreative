package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.client.gui.util.ItemSearchHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class ItemPaletteWidget extends AbstractWidget {
    private static final int SLOT_SIZE = 18;

    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private final Set<String> modIds = new TreeSet<>();
    private final Consumer<ItemStack> onAddItem;
    private String selectedMod = "all";
    private String searchQuery = "";
    private double scrollAmount = 0;
    private boolean isScrolling = false;
    private Consumer<ItemStack> onStartDrag;

    private double pressedMouseX = -1;
    private double pressedMouseY = -1;
    private int pressedIndex = -1;
    private boolean dragInitiated = false;

    public ItemPaletteWidget(int x, int y, int width, int height, Consumer<ItemStack> onAddItem) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.onAddItem = onAddItem;

        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(key);
            if (item != Items.AIR) {
                allItems.add(new ItemStack(item));
                modIds.add(key.getNamespace());
            }
        }
        filteredItems.addAll(allItems);
    }

    public void setOnStartDrag(Consumer<ItemStack> onStartDrag) {
        this.onStartDrag = onStartDrag;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
        updateFilter();
    }

    public Set<String> getModIds() {
        return modIds;
    }

    public String getSelectedMod() {
        return selectedMod;
    }

    public void setSelectedMod(String modId) {
        this.selectedMod = modId != null ? modId : "all";
        updateFilter();
    }

    private void updateFilter() {
        filteredItems.clear();
        filteredItems.addAll(ItemSearchHelper.filterItems(allItems, searchQuery, selectedMod));
        scrollAmount = 0;
    }

    public int getColumns() {
        return Math.max(1, (this.width - GuiUtil.PANEL_PADDING * 2 - GuiUtil.SCROLLBAR_EXTRA_WIDTH) / SLOT_SIZE);
    }

    public int getVisibleRows() {
        return Math.max(1, (this.height - GuiUtil.PANEL_PADDING * 2) / SLOT_SIZE);
    }

    public int getPanelWidth() {
        return getColumns() * SLOT_SIZE + GuiUtil.PANEL_PADDING * 2 + GuiUtil.SCROLLBAR_EXTRA_WIDTH;
    }

    public int getPanelHeight() {
        return getVisibleRows() * SLOT_SIZE + GuiUtil.PANEL_PADDING * 2;
    }

    public int getPanelX() {
        return this.getX() + Math.max(0, (this.width - getPanelWidth()) / 2);
    }

    public int getPanelY() {
        return this.getY() + Math.max(0, (this.height - getPanelHeight()) / 2);
    }

    public int getGridX() {
        return getPanelX() + GuiUtil.PANEL_PADDING;
    }

    public int getGridY() {
        return getPanelY() + GuiUtil.PANEL_PADDING;
    }

    public int getGridWidth() {
        return getColumns() * SLOT_SIZE;
    }

    public int getGridHeight() {
        return getVisibleRows() * SLOT_SIZE;
    }

    public int getScrollbarX() {
        return getGridX() + getGridWidth();
    }

    public int getTotalRows() {
        if (filteredItems.isEmpty()) return 1;
        return (filteredItems.size() + getColumns() - 1) / getColumns();
    }

    public int getMaxScroll() {
        return Math.max(0, getTotalRows() * SLOT_SIZE - getGridHeight());
    }

    private boolean isOverScrollbar(double mouseX) {
        int scrollbarX = getScrollbarX();
        return mouseX >= scrollbarX && mouseX < scrollbarX + GuiUtil.SCROLLBAR_WIDTH;
    }

    private int slotIndexAt(double mouseX, double mouseY) {
        int gridX = getGridX();
        int gridY = getGridY();
        if (mouseX < gridX || mouseX >= gridX + getGridWidth()) return -1;
        if (mouseY < gridY || mouseY >= gridY + getGridHeight()) return -1;

        int col = (int) ((mouseX - gridX) / SLOT_SIZE);
        int row = (int) ((mouseY - gridY + scrollAmount) / SLOT_SIZE);
        int index = row * getColumns() + col;
        return index >= 0 && index < filteredItems.size() ? index : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isHovered) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * SLOT_SIZE, 0, getMaxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isHovered) return false;

        if (isOverScrollbar(mouseX)) {
            this.isScrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        int index = slotIndexAt(mouseX, mouseY);
        if (index >= 0) {
            if (button == 0) {
                this.pressedMouseX = mouseX;
                this.pressedMouseY = mouseY;
                this.pressedIndex = index;
                this.dragInitiated = false;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling && getMaxScroll() > 0) {
            updateScrollFromMouse(mouseY);
            return true;
        }

        if (button == 0 && pressedIndex >= 0 && pressedIndex < filteredItems.size() && !dragInitiated) {
            double distSq = (mouseX - pressedMouseX) * (mouseX - pressedMouseX) + (mouseY - pressedMouseY) * (mouseY - pressedMouseY);
            if (distSq > 9) {
                dragInitiated = true;
                if (onStartDrag != null) {
                    ItemStack clicked = filteredItems.get(pressedIndex);
                    onStartDrag.accept(clicked.copy());
                }
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY) {
        this.scrollAmount = GuiUtil.scrollAmountFromMouse(mouseY, getGridY(), getGridHeight(), getMaxScroll());
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isScrolling = false;

        if (button == 0 && pressedIndex >= 0 && pressedIndex < filteredItems.size() && !dragInitiated) {
            ItemStack clicked = filteredItems.get(pressedIndex);
            if (onAddItem != null) {
                onAddItem.accept(clicked.copy());
            }
        }

        this.pressedIndex = -1;
        this.dragInitiated = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int gridX = getGridX();
        int gridY = getGridY();
        int gridW = getGridWidth();
        int gridH = getGridHeight();

        GuiUtil.drawContentPanel(guiGraphics, gridX, gridY, gridW, gridH);

        if (filteredItems.isEmpty()) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.recreative.no_items_found"),
                    gridX + gridW / 2, gridY + gridH / 2 - 4, 0x888888);
            GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarX(), gridY, gridH, 0, 0);
            return;
        }

        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());

        int cols = getColumns();
        int startRow = (int) (scrollAmount / SLOT_SIZE);
        int yOffset = (int) (scrollAmount % SLOT_SIZE);

        ItemStack hoveredStack = ItemStack.EMPTY;
        int hoveredIndex = this.isHovered ? slotIndexAt(mouseX, mouseY) : -1;

        guiGraphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);

        for (int r = 0; r <= getVisibleRows(); r++) {
            int actualRow = startRow + r;
            for (int c = 0; c < cols; c++) {
                int index = actualRow * cols + c;
                if (index >= filteredItems.size()) break;

                int slotX = gridX + c * SLOT_SIZE;
                int slotY = gridY + r * SLOT_SIZE - yOffset;

                ItemStack stack = filteredItems.get(index);
                if (!stack.isEmpty()) {
                    guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);
                }

                if (index == hoveredIndex) {
                    GuiUtil.drawSlotHighlight(guiGraphics, slotX, slotY);
                    hoveredStack = stack;
                }
            }
        }

        guiGraphics.disableScissor();

        GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarX(), gridY, gridH, scrollAmount, getMaxScroll());

        if (!hoveredStack.isEmpty()) {
            List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), hoveredStack));
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(hoveredStack.getItem());
            tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("gui.recreative.click_to_add").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("gui.recreative.drag_tip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, hoveredStack.getTooltipImage(), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
