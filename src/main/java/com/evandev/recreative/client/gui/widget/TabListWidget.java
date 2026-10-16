package com.evandev.recreative.client.gui.widget;

import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.util.ClickInput;
import com.evandev.recreative.client.gui.util.GuiCompat;
import com.evandev.recreative.client.gui.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TabListWidget extends BaseWidget {
    private static final ResourceLocation ICON_VISIBLE = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/icons/visible.png");
    private static final ResourceLocation ICON_HIDDEN = ResourceLocation.fromNamespaceAndPath("recreative", "textures/gui/icons/hidden.png");

    private static final ResourceLocation TAB_SPRITE = ResourceLocation.fromNamespaceAndPath("recreative", "widget/tabs/vertical_vanilla");
    private static final ResourceLocation TAB_SELECTED_SPRITE = ResourceLocation.fromNamespaceAndPath("recreative", "widget/tabs/vertical_vanilla_selected");

    private final EditorStateManager stateManager;
    private final Consumer<EditorStateManager.EditableTab> onSelect;
    private final List<TabEntry> entries = new ArrayList<>();
    private final int itemHeight;
    @Nullable
    private TabEntry selected;
    private String searchQuery = "";
    private double scrollAmount = 0;
    private boolean draggingScrollbar = false;
    private boolean suppressSelectCallback = false;

    public TabListWidget(int x, int y, int width, int height, int itemHeight,
                         EditorStateManager stateManager, Consumer<EditorStateManager.EditableTab> onSelect) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.itemHeight = itemHeight;
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
        this.entries.clear();
        this.selected = null;
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
            this.entries.add(entry);

            if (tab.id.equals(selectedId)) {
                this.setSelected(entry);
            }
        }

        if (this.getSelected() == null && !this.entries.isEmpty()) {
            this.setSelected(this.entries.get(0));
        }
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());
    }

    public void selectTab(String tabId) {
        for (TabEntry entry : this.entries) {
            if (entry.tab.id.equals(tabId)) {
                this.setSelected(entry);
                break;
            }
        }
    }

    @Nullable
    public TabEntry getSelected() {
        return this.selected;
    }

    public void setSelected(@Nullable TabEntry selected) {
        TabEntry prev = this.selected;
        this.selected = selected;
        if (selected != null && onSelect != null && !suppressSelectCallback) {
            if (prev == null || !prev.tab.id.equals(selected.tab.id)) {
                onSelect.accept(selected.tab);
            }
        }
    }

    private int contentTop() {
        return this.getY() + 4;
    }

    private int contentHeight() {
        return this.height - 4;
    }

    private int getScrollbarPosition() {
        return this.getX() + this.width;
    }

    private int getMaxScroll() {
        return Math.max(0, this.entries.size() * this.itemHeight - contentHeight());
    }

    private int getRowTop(int index) {
        return contentTop() - (int) this.scrollAmount + index * this.itemHeight;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int x = getScrollbarPosition();
        return mouseX >= x && mouseX < x + GuiUtil.SCROLLBAR_WIDTH
                && mouseY >= contentTop() && mouseY < (this.getY() + this.height);
    }

    private boolean isOverContent(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= contentTop() && mouseY < (this.getY() + this.height);
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        if (!isOverContent(mouseX, mouseY)) return -1;
        int index = (int) Math.floor((mouseY - contentTop() + this.scrollAmount) / this.itemHeight);
        return index >= 0 && index < this.entries.size() ? index : -1;
    }

    private void updateScrollFromMouse(double mouseY) {
        this.scrollAmount = GuiUtil.scrollAmountFromMouse(mouseY, contentTop(), contentHeight(), getMaxScroll());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || isOverScrollbar(mouseX, mouseY);
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (getMaxScroll() <= 0) return false;
        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * this.itemHeight / 2.0, 0, getMaxScroll());
        return true;
    }

    @Override
    protected boolean onMouseClicked(ClickInput input) {
        if (!input.isLeft()) return false;

        if (isOverScrollbar(input.x(), input.y())) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(input.y());
            return true;
        }
        this.draggingScrollbar = false;

        int index = entryIndexAt(input.x(), input.y());
        if (index >= 0) {
            return this.entries.get(index).mouseClicked(input.x());
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(ClickInput input, double dragX, double dragY) {
        if (this.draggingScrollbar && input.isLeft()) {
            updateScrollFromMouse(input.y());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseReleased(ClickInput input) {
        this.draggingScrollbar = false;
        return false;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiUtil.drawContentPanel(guiGraphics, this.getX(), contentTop(), this.width, contentHeight());

        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());
        int hoveredIndex = entryIndexAt(mouseX, mouseY);

        guiGraphics.enableScissor(this.getX(), contentTop(), (this.getX() + this.width), (this.getY() + this.height));
        for (int i = 0; i < this.entries.size(); i++) {
            int top = getRowTop(i);
            if (top + this.itemHeight >= this.getY() && top <= (this.getY() + this.height)) {
                this.entries.get(i).render(guiGraphics, top, this.getX(), this.width, this.itemHeight, i == hoveredIndex);
            }
        }
        guiGraphics.disableScissor();

        GuiUtil.drawVanillaScrollbar(guiGraphics, getScrollbarPosition(), contentTop(), contentHeight(),
                this.scrollAmount, getMaxScroll());
    }

    public class TabEntry {
        public final EditorStateManager.EditableTab tab;

        public TabEntry(EditorStateManager.EditableTab tab) {
            this.tab = tab;
        }

        public void render(GuiGraphics guiGraphics, int top, int left, int width, int height, boolean isHovered) {
            Font font = Minecraft.getInstance().font;
            boolean isSelected = TabListWidget.this.getSelected() == this;

            GuiCompat.enableBlend();
            if (isSelected) {
                GuiCompat.blitSprite(guiGraphics, TAB_SELECTED_SPRITE, left, top, width, height);
            } else {
                GuiCompat.blitSprite(guiGraphics, TAB_SPRITE, left, top, width, height);
                if (isHovered) {
                    guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0x18FFFFFF);
                }
            }
            GuiCompat.disableBlend();

            int iconX = left + 4;
            int iconY = top + (height - 16) / 2;

            if (tab.customIcon != null && (tab.customIcon.contains("/") || tab.customIcon.endsWith(".png"))) {
                try {
                    ResourceLocation tex = ResourceLocation.parse(tab.customIcon);
                    GuiCompat.enableBlend();
                    GuiCompat.blit(guiGraphics, tex, iconX, iconY, 0, 0, 16, 16, 16, 16);
                    GuiCompat.disableBlend();
                } catch (Exception e) {
                    ItemStack icon = tab.getEffectiveIconStack();
                    if (!icon.isEmpty()) {
                        GuiCompat.fakeItem(guiGraphics, icon, iconX, iconY);
                    }
                }
            } else {
                ItemStack icon = tab.getEffectiveIconStack();
                if (!icon.isEmpty()) {
                    GuiCompat.fakeItem(guiGraphics, icon, iconX, iconY);
                }
            }

            int textX = left + 24;
            int textY = top + 4;
            int subTextY = top + 15;

            Component name = tab.getEffectiveDisplayName();
            String nameStr = name.getString();
            int maxTextW = width - 44;
            if (font.width(nameStr) > maxTextW && maxTextW > 10) {
                nameStr = font.plainSubstrByWidth(nameStr, maxTextW - font.width("..")) + "..";
            }

            int nameColor = tab.isRemoved ? 0x888888 : 0xFFFFFF;
            GuiCompat.drawString(guiGraphics, font, nameStr, textX, textY, nameColor);

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
                if (font.width(subText) > maxTextW && maxTextW > 10) {
                    subText = font.plainSubstrByWidth(subText, maxTextW - font.width("..")) + "..";
                }
                subComponent = Component.literal(subText);
            }
            GuiCompat.drawString(guiGraphics, font, subComponent, textX, subTextY, subColor);

            int eyeX = left + width - 18;
            int eyeY = top + (height - 16) / 2;
            ResourceLocation eyeTex = tab.isRemoved ? ICON_HIDDEN : ICON_VISIBLE;
            GuiCompat.enableBlend();
            GuiCompat.blit(guiGraphics, eyeTex, eyeX, eyeY, 0, 0, 16, 16, 16, 16);
            GuiCompat.disableBlend();
        }

        public boolean mouseClicked(double mouseX) {
            int eyeX = TabListWidget.this.getX() + TabListWidget.this.width - 18;

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
