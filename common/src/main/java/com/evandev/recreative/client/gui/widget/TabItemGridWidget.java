package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.data.ItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TabItemGridWidget extends AbstractWidget {
    private static final int SLOT_SIZE = 18;

    private final EditorStateManager stateManager;
    private final Consumer<Integer> onSelectItem;
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();
    private EditorStateManager.EditableTab currentTab;
    private int selectedItemIndex = -1;
    private int selectionAnchor = -1;
    private double scrollAmount = 0;
    private boolean isScrolling = false;
    private BiConsumer<Integer, ItemStack> onStartDrag;

    private double pressedMouseX = -1;
    private double pressedMouseY = -1;
    private int pressedIndex = -1;
    private boolean dragInitiated = false;
    private int collapseSelectionOnRelease = -1;

    public TabItemGridWidget(int x, int y, int width, int height,
                             EditorStateManager stateManager, Consumer<Integer> onSelectItem) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.stateManager = stateManager;
        this.onSelectItem = onSelectItem;
    }

    public void setOnStartDrag(BiConsumer<Integer, ItemStack> onStartDrag) {
        this.onStartDrag = onStartDrag;
    }

    public EditorStateManager.EditableTab getTab() {
        return currentTab;
    }

    public void setTab(EditorStateManager.EditableTab tab) {
        boolean sameTab = this.currentTab != null && tab != null && this.currentTab.id.equals(tab.id);
        this.currentTab = tab;
        if (!sameTab) {
            this.selectedIndices.clear();
            this.selectedItemIndex = -1;
            this.selectionAnchor = -1;
            this.scrollAmount = 0;
        }
    }

    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    public void setSelectedItemIndex(int index) {
        this.selectedIndices.clear();
        if (index >= 0) {
            this.selectedIndices.add(index);
        }
        this.selectionAnchor = index;
        this.selectedItemIndex = index;
        if (onSelectItem != null) {
            onSelectItem.accept(index);
        }
    }

    public List<Integer> getSelectedIndices() {
        List<Integer> sorted = new ArrayList<>(selectedIndices);
        Collections.sort(sorted);
        return sorted;
    }

    public int getSelectionSize() {
        return selectedIndices.size();
    }

    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    }

    public void setSelection(Collection<Integer> indices) {
        this.selectedIndices.clear();
        int primary = -1;
        if (indices != null && currentTab != null) {
            for (int index : indices) {
                if (index >= 0 && index < currentTab.displayItems.size()) {
                    this.selectedIndices.add(index);
                    primary = index;
                }
            }
        }
        this.selectedItemIndex = primary;
        this.selectionAnchor = primary;
        if (onSelectItem != null) {
            onSelectItem.accept(primary);
        }
    }

    public void selectRange(int startIndex, int endIndex) {
        if (currentTab == null) return;
        int from = Math.min(startIndex, endIndex);
        int to = Math.max(startIndex, endIndex);
        List<Integer> range = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            range.add(i);
        }
        setSelection(range);
        this.selectionAnchor = startIndex;
        this.selectedItemIndex = endIndex;
    }

    public void selectAll() {
        if (currentTab == null || currentTab.displayItems.isEmpty()) return;
        selectRange(0, currentTab.displayItems.size() - 1);
    }

    public void clearSelection() {
        setSelectedItemIndex(-1);
    }

    public void shiftSelectionAfterRemoval(int removedIndex) {
        List<Integer> shifted = new ArrayList<>(selectedIndices.size());
        for (int index : getSelectedIndices()) {
            if (index == removedIndex) continue;
            shifted.add(index > removedIndex ? index - 1 : index);
        }
        setSelection(shifted);
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
        if (currentTab == null || currentTab.displayItems.isEmpty()) return 1;
        return (currentTab.displayItems.size() + getColumns() - 1) / getColumns();
    }

    public int getMaxScroll() {
        return Math.max(0, getTotalRows() * SLOT_SIZE - getGridHeight());
    }

    private boolean isOverScrollbar(double mouseX) {
        int scrollbarX = getScrollbarX();
        return mouseX >= scrollbarX && mouseX < scrollbarX + GuiUtil.SCROLLBAR_WIDTH;
    }

    private int slotIndexAt(double mouseX, double mouseY) {
        if (currentTab == null) return -1;
        int gridX = getGridX();
        int gridY = getGridY();
        if (mouseX < gridX || mouseX >= gridX + getGridWidth()) return -1;
        if (mouseY < gridY || mouseY >= gridY + getGridHeight()) return -1;

        int col = (int) ((mouseX - gridX) / SLOT_SIZE);
        int row = (int) ((mouseY - gridY + scrollAmount) / SLOT_SIZE);
        int index = row * getColumns() + col;
        return index >= 0 && index < currentTab.displayItems.size() ? index : -1;
    }

    public boolean isOverItem(double mouseX, double mouseY) {
        return slotIndexAt(mouseX, mouseY) >= 0;
    }

    public int getInsertionIndex(double mouseX, double mouseY) {
        if (currentTab == null) return 0;
        if (currentTab.displayItems.isEmpty()) return 0;

        int cols = getColumns();
        int gridX = getGridX();
        int gridY = getGridY();

        int relX = (int) (mouseX - gridX);
        int relY = (int) (mouseY - gridY + scrollAmount);

        int col = Math.max(0, Math.min(cols - 1, relX / SLOT_SIZE));
        int row = Math.max(0, relY / SLOT_SIZE);

        int index = row * cols + col;

        int slotRelX = relX % SLOT_SIZE;
        if (slotRelX > SLOT_SIZE / 2 && index < currentTab.displayItems.size()) {
            index++;
        }

        return Math.max(0, Math.min(currentTab.displayItems.size(), index));
    }

    public void renderInsertionMarker(@NotNull GuiGraphics guiGraphics, int insertionIndex) {
        if (currentTab == null) return;
        int cols = getColumns();
        int gridX = getGridX();
        int gridY = getGridY();

        int markerX = gridX + (insertionIndex % cols) * SLOT_SIZE;
        int markerY = gridY + (insertionIndex / cols) * SLOT_SIZE - (int) scrollAmount;

        if (markerY + SLOT_SIZE >= gridY && markerY <= gridY + getGridHeight()) {
            guiGraphics.fill(markerX - 1, markerY, markerX + 1, markerY + SLOT_SIZE, GuiUtil.INSERTION_MARKER);
        }
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
        if (!this.isHovered || currentTab == null) return false;

        if (isOverScrollbar(mouseX)) {
            this.isScrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        int index = slotIndexAt(mouseX, mouseY);
        if (index >= 0) {
            if (button == 1) {
                if (isSelected(index) && selectedIndices.size() > 1) {
                    stateManager.removeItemsFromTab(currentTab.id, getSelectedIndices());
                    clearSelection();
                } else {
                    stateManager.removeItemFromTab(currentTab.id, index);
                    shiftSelectionAfterRemoval(index);
                }
            } else if (button == 0) {
                this.pressedMouseX = mouseX;
                this.pressedMouseY = mouseY;
                this.pressedIndex = index;
                this.dragInitiated = false;
                this.collapseSelectionOnRelease = -1;

                if (Screen.hasControlDown()) {
                    if (!selectedIndices.remove(index)) {
                        selectedIndices.add(index);
                        this.selectedItemIndex = index;
                    } else if (selectedItemIndex == index) {
                        this.selectedItemIndex = selectedIndices.isEmpty() ? -1 : Collections.max(selectedIndices);
                    }
                    this.selectionAnchor = index;
                    if (onSelectItem != null) {
                        onSelectItem.accept(this.selectedItemIndex);
                    }
                } else if (Screen.hasShiftDown() && selectionAnchor >= 0 && selectionAnchor < currentTab.displayItems.size()) {
                    selectRange(selectionAnchor, index);
                    if (onSelectItem != null) {
                        onSelectItem.accept(index);
                    }
                } else if (isSelected(index) && selectedIndices.size() > 1) {
                    this.collapseSelectionOnRelease = index;
                    this.selectedItemIndex = index;
                    this.selectionAnchor = index;
                    if (onSelectItem != null) {
                        onSelectItem.accept(index);
                    }
                } else {
                    setSelectedItemIndex(index);
                }
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

        if (button == 0 && pressedIndex >= 0 && currentTab != null && pressedIndex < currentTab.displayItems.size() && !dragInitiated) {
            double distSq = (mouseX - pressedMouseX) * (mouseX - pressedMouseX) + (mouseY - pressedMouseY) * (mouseY - pressedMouseY);
            if (distSq > 9) {
                dragInitiated = true;
                if (onStartDrag != null) {
                    ItemStack stack = currentTab.displayItems.get(pressedIndex);
                    onStartDrag.accept(pressedIndex, stack);
                }
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public void endDragInteraction() {
        this.isScrolling = false;
        this.pressedIndex = -1;
        this.dragInitiated = false;
        this.collapseSelectionOnRelease = -1;
    }

    private void updateScrollFromMouse(double mouseY) {
        this.scrollAmount = GuiUtil.scrollAmountFromMouse(mouseY, getGridY(), getGridHeight(), getMaxScroll());
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isScrolling = false;
        this.pressedIndex = -1;
        if (this.collapseSelectionOnRelease >= 0 && !this.dragInitiated) {
            setSelectedItemIndex(this.collapseSelectionOnRelease);
        }
        this.collapseSelectionOnRelease = -1;
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

        if (currentTab == null) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.recreative.no_tab_selected"),
                    gridX + gridW / 2, gridY + gridH / 2 - 4, 0x888888);
            GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarX(), gridY, gridH, 0, 0);
            return;
        }

        if (currentTab.displayItems.isEmpty()) {
            Component emptyMsg = Component.translatable("gui.recreative.tab_empty");
            List<FormattedCharSequence> lines = Minecraft.getInstance().font.split(emptyMsg, Math.max(50, gridW - 6));
            int lineY = gridY + gridH / 2 - (lines.size() * 10) / 2;
            for (FormattedCharSequence line : lines) {
                int lineW = Minecraft.getInstance().font.width(line);
                guiGraphics.drawString(Minecraft.getInstance().font, line, gridX + (gridW - lineW) / 2, lineY, 0x888888, false);
                lineY += 10;
            }
            GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarX(), gridY, gridH, 0, 0);
            return;
        }

        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());

        int cols = getColumns();
        int startRow = (int) (scrollAmount / SLOT_SIZE);
        int yOffset = (int) (scrollAmount % SLOT_SIZE);

        ItemStack hoveredStack = ItemStack.EMPTY;
        boolean isHoveredCustom = false;
        int hoveredIndex = this.isHovered ? slotIndexAt(mouseX, mouseY) : -1;

        guiGraphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);

        for (int r = 0; r <= getVisibleRows(); r++) {
            int actualRow = startRow + r;
            for (int c = 0; c < cols; c++) {
                int index = actualRow * cols + c;
                if (index >= currentTab.displayItems.size()) break;

                int slotX = gridX + c * SLOT_SIZE;
                int slotY = gridY + r * SLOT_SIZE - yOffset;

                ItemStack stack = currentTab.displayItems.get(index);
                boolean isAdded = isItemAddedByRule(stack);

                GuiUtil.drawSlot(guiGraphics, slotX, slotY);

                if (isAdded) {
                    guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, GuiUtil.ADDED_SLOT);
                }

                if (selectedIndices.contains(index)) {
                    guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, GuiUtil.SELECTED_SLOT);
                    if (index == selectedItemIndex && selectedIndices.size() > 1) {
                        guiGraphics.renderOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE, GuiUtil.PRIMARY_SELECTION_OUTLINE);
                    }
                }

                if (!stack.isEmpty()) {
                    guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);
                }

                if (index == hoveredIndex) {
                    GuiUtil.drawSlotHighlight(guiGraphics, slotX, slotY);
                    hoveredStack = stack;
                    isHoveredCustom = isAdded;
                }
            }
        }

        guiGraphics.disableScissor();

        GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarX(), gridY, gridH, scrollAmount, getMaxScroll());

        if (!hoveredStack.isEmpty()) {
            List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), hoveredStack));
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(hoveredStack.getItem());
            tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
            if (isHoveredCustom) {
                tooltip.add(Component.translatable("gui.recreative.item_added_badge").withStyle(ChatFormatting.GREEN));
            }
            tooltip.add(Component.translatable("gui.recreative.remove_item_tip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("gui.recreative.drag_tip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("gui.recreative.multi_select_tip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("gui.recreative.clipboard_tip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, hoveredStack.getTooltipImage(), mouseX, mouseY);
        }
    }

    private boolean isItemAddedByRule(ItemStack stack) {
        if (currentTab == null || stack == null || stack.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (ItemEntry entry : currentTab.addedItems) {
            if (entry == null || entry.item == null) continue;
            if (Objects.equals(entry.item, itemId)) return true;
            if (entry.item.startsWith("#")) {
                try {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.item.substring(1)));
                    if (stack.is(tagKey)) return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
