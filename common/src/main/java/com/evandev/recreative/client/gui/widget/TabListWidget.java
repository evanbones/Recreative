package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.util.GuiUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TabListWidget extends AbstractSelectionList<TabListWidget.TabEntry> {
    private static final ResourceLocation ICON_VISIBLE = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/icons/visible.png");
    private static final ResourceLocation ICON_HIDDEN = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/icons/hidden.png");

    private static final ResourceLocation TAB_SPRITE = ResourceLocation.fromNamespaceAndPath("recreative", "widget/tabs/vertical_vanilla");
    private static final ResourceLocation TAB_SELECTED_SPRITE = ResourceLocation.fromNamespaceAndPath("recreative", "widget/tabs/vertical_vanilla_selected");

    private final EditorStateManager stateManager;
    private final Consumer<EditorStateManager.EditableTab> onSelect;
    private String searchQuery = "";
    private boolean draggingScrollbar = false;
    private boolean suppressSelectCallback = false;

    public TabListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight,
                         EditorStateManager stateManager, Consumer<EditorStateManager.EditableTab> onSelect) {
        super(minecraft, width, height, y, itemHeight);
        this.stateManager = stateManager;
        this.onSelect = onSelect;
        this.refreshList();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        this.refreshList();
    }

    public void refreshList() {
        TabEntry prevSelected = this.getSelected();
        String selectedId = prevSelected != null ? prevSelected.tab.id : null;

        this.suppressSelectCallback = true;
        try {
            rebuildEntries(selectedId);
        } finally {
            this.suppressSelectCallback = false;
        }

        TabEntry nowSelected = this.getSelected();
        boolean selectionChanged = nowSelected != null
                && (selectedId == null || !selectedId.equals(nowSelected.tab.id));
        if (selectionChanged && onSelect != null) {
            onSelect.accept(nowSelected.tab);
        }
    }

    private void rebuildEntries(String selectedId) {
        this.clearEntries();
        List<EditorStateManager.EditableTab> allTabs = stateManager.getTabs();

        for (EditorStateManager.EditableTab tab : allTabs) {
            if (!searchQuery.isEmpty()) {
                String idLower = tab.id.toLowerCase(Locale.ROOT);
                String nameLower = tab.getEffectiveDisplayName().getString().toLowerCase(Locale.ROOT);
                if (!idLower.contains(searchQuery) && !nameLower.contains(searchQuery)) {
                    continue;
                }
            }

            TabEntry entry = new TabEntry(tab);
            this.addEntry(entry);

            if (tab.id.equals(selectedId)) {
                this.setSelected(entry);
            }
        }

        if (this.getSelected() == null && !this.children().isEmpty()) {
            this.setSelected(this.children().getFirst());
        }
    }

    public void selectTab(String tabId) {
        for (TabEntry entry : this.children()) {
            if (entry.tab.id.equals(tabId)) {
                this.setSelected(entry);
                break;
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    public int getRowLeft() {
        return this.getX();
    }

    private int contentTop() {
        return this.getY() + 4;
    }

    private int contentHeight() {
        return this.height - 4;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int x = getScrollbarPosition();
        return mouseX >= x && mouseX < x + GuiUtil.SCROLLBAR_WIDTH
                && mouseY >= contentTop() && mouseY < this.getBottom();
    }

    @Override
    protected void renderListBackground(@NotNull GuiGraphics guiGraphics) {
        GuiUtil.drawContentPanel(guiGraphics, this.getX(), contentTop(), this.width, contentHeight());
    }

    @Override
    protected void renderListSeparators(@NotNull GuiGraphics guiGraphics) {
    }

    @Override
    protected void enableScissor(@NotNull GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(this.getX(), contentTop(), this.getRight(), this.getBottom());
    }

    @Override
    protected boolean scrollbarVisible() {
        return false;
    }

    @Override
    protected void renderDecorations(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarPosition(), contentTop(), contentHeight(),
                this.getScrollAmount(), this.getMaxScroll());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || isOverScrollbar(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.getMaxScroll() <= 0) return false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void updateScrollingState(double mouseX, double mouseY, int button) {
        super.updateScrollingState(isOverScrollbar(mouseX, mouseY) ? getScrollbarPosition() : mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            this.draggingScrollbar = true;
            this.setScrollAmount(GuiUtil.scrollAmountFromMouse(mouseY, contentTop(), contentHeight(), this.getMaxScroll()));
            return true;
        }
        this.draggingScrollbar = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar && button == 0) {
            this.setScrollAmount(GuiUtil.scrollAmountFromMouse(mouseY, contentTop(), contentHeight(), this.getMaxScroll()));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderListItems(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getRowLeft();
        int rowWidth = this.getRowWidth();
        int count = this.getItemCount();

        for (int i = 0; i < count; i++) {
            int top = this.getRowTop(i);
            if (top + this.itemHeight >= this.getY() && top <= this.getBottom()) {
                this.renderItem(guiGraphics, mouseX, mouseY, partialTick, i, left, top, rowWidth, this.itemHeight);
            }
        }
    }

    @Override
    protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
    }

    @Override
    public void setSelected(@Nullable TabEntry selected) {
        TabEntry prev = this.getSelected();
        super.setSelected(selected);
        if (selected != null && onSelect != null && !suppressSelectCallback) {
            if (prev == null || !prev.tab.id.equals(selected.tab.id)) {
                onSelect.accept(selected.tab);
            }
        }
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    public class TabEntry extends AbstractSelectionList.Entry<TabEntry> {
        public final EditorStateManager.EditableTab tab;

        public TabEntry(EditorStateManager.EditableTab tab) {
            this.tab = tab;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean isHovered, float partialTicks) {
            boolean isSelected = TabListWidget.this.getSelected() == this;

            RenderSystem.enableBlend();
            if (isSelected) {
                guiGraphics.blitSprite(TAB_SELECTED_SPRITE, left, top, width, height);
            } else {
                guiGraphics.blitSprite(TAB_SPRITE, left, top, width, height);
                if (isHovered) {
                    guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0x18FFFFFF);
                }
            }
            RenderSystem.disableBlend();

            int iconX = left + 4;
            int iconY = top + (height - 16) / 2;

            if (tab.customIcon != null && (tab.customIcon.contains("/") || tab.customIcon.endsWith(".png"))) {
                try {
                    ResourceLocation tex = ResourceLocation.parse(tab.customIcon);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.blit(tex, iconX, iconY, 0, 0, 16, 16, 16, 16);
                    RenderSystem.disableBlend();
                } catch (Exception e) {
                    ItemStack icon = tab.getEffectiveIconStack();
                    if (!icon.isEmpty()) {
                        guiGraphics.renderFakeItem(icon, iconX, iconY);
                    }
                }
            } else {
                ItemStack icon = tab.getEffectiveIconStack();
                if (!icon.isEmpty()) {
                    guiGraphics.renderFakeItem(icon, iconX, iconY);
                }
            }

            int textX = left + 24;
            int textY = top + 4;
            int subTextY = top + 15;

            Component name = tab.getEffectiveDisplayName();
            String nameStr = name.getString();
            int maxTextW = width - 44;
            if (TabListWidget.this.minecraft.font.width(nameStr) > maxTextW && maxTextW > 10) {
                nameStr = TabListWidget.this.minecraft.font.plainSubstrByWidth(nameStr, maxTextW - TabListWidget.this.minecraft.font.width("..")) + "..";
            }

            int nameColor = tab.isRemoved ? 0x888888 : 0xFFFFFF;
            guiGraphics.drawString(TabListWidget.this.minecraft.font, nameStr, textX, textY, nameColor);

            Component subComponent;
            int subColor = GuiUtil.SUBTEXT;
            if (tab.isRemoved) {
                subComponent = Component.translatable("gui.recreative.badge_hidden");
                subColor = GuiUtil.BADGE_HIDDEN;
            } else if (tab.isCustomTab) {
                subComponent = Component.translatable("gui.recreative.badge_custom");
                subColor = GuiUtil.BADGE_CUSTOM;
            } else if (tab.isModified()) {
                subComponent = Component.translatable("gui.recreative.badge_modified");
                subColor = GuiUtil.BADGE_MODIFIED;
            } else {
                String subText = tab.id.substring(tab.id.indexOf(':') + 1);
                if (TabListWidget.this.minecraft.font.width(subText) > maxTextW && maxTextW > 10) {
                    subText = TabListWidget.this.minecraft.font.plainSubstrByWidth(subText, maxTextW - TabListWidget.this.minecraft.font.width("..")) + "..";
                }
                subComponent = Component.literal(subText);
            }
            guiGraphics.drawString(TabListWidget.this.minecraft.font, subComponent, textX, subTextY, subColor);

            int eyeX = left + width - 18;
            int eyeY = top + (height - 16) / 2;
            ResourceLocation eyeTex = tab.isRemoved ? ICON_HIDDEN : ICON_VISIBLE;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(eyeTex, eyeX, eyeY, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int entryLeft = TabListWidget.this.getRowLeft();
            int entryWidth = TabListWidget.this.getRowWidth();
            int eyeX = entryLeft + entryWidth - 18;

            if (mouseX >= eyeX && mouseX <= eyeX + 18) {
                stateManager.setTabVisibility(tab.id, tab.isRemoved);
                TabListWidget.this.refreshList();
                return true;
            }

            TabListWidget.this.setSelected(this);
            return true;
        }
    }
}
