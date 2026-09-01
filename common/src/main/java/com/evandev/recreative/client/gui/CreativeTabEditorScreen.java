package com.evandev.recreative.client.gui;

import com.evandev.recreative.Constants;
import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.modal.CreateTabModal;
import com.evandev.recreative.client.gui.modal.IconPickerModal;
import com.evandev.recreative.client.gui.modal.UnsavedChangesModal;
import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.client.gui.widget.*;
import com.evandev.recreative.data.ItemEntry;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreativeTabEditorScreen extends Screen {
    private static final int PROP_ROW_H = 18;
    private static final int PROP_ROW_GAP = 6;
    private static final int ICON_TEXT_GAP = 6;

    private final Screen parent;
    private final EditorStateManager stateManager;

    private TabListWidget tabListWidget;
    private TabItemGridWidget tabItemGridWidget;
    private ItemPaletteWidget itemPaletteWidget;

    private ModEditBox tabNameBox;

    private Button saveButton;
    private Button discardButton;
    private DropdownWidget modFilterDropdown;

    private Button resetTabButton;

    private Button moveItemLeftButton;
    private Button moveItemRightButton;
    private Button removeItemButton;

    private String statusMessage = "";
    private int statusColor = 0x888888;
    private long statusClearTime = 0;

    private int[] iconRect = new int[]{0, 0, 0, 0};

    private ItemStack draggedStack = ItemStack.EMPTY;
    private String dragSource = null;
    private int dragSourceIndex = -1;
    private boolean isUpdatingTab = false;

    public CreativeTabEditorScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.recreative.editor.title"));
        this.parent = parent;
        this.stateManager = new EditorStateManager();
        this.stateManager.loadState();
    }

    private static boolean isOver(@Nullable Button button, double mouseX, double mouseY) {
        return button != null && button.visible
                && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    @Override
    protected void init() {
        int leftX = 6;
        int topBarH = 28;
        int bottomBarH = 24;
        int mainAreaH = this.height - topBarH - bottomBarH - leftX * 2;

        int leftW = 145;
        int rightW = 150;
        int centerW = this.width - leftW - rightW - leftX * 4;

        int centerX = leftX + leftW + leftX;
        int rightX = centerX + centerW + leftX;
        int contentY = topBarH + leftX;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width - 65 - leftX, 4, 65, 20).build());

        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.save_and_apply"), b -> {
            try {
                this.stateManager.saveAndApply();
                setStatus(Component.translatable("gui.recreative.save_success").getString(), 0x55FF55);
                if (this.tabListWidget != null) {
                    this.tabListWidget.refreshList();
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to save Recreative tabs", e);
                setStatus(Component.translatable("gui.recreative.save_failure").getString(), 0xFF5555);
            }
        }).bounds(this.width - 170 - leftX, 4, 100, 20).build());

        this.discardButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.discard"), b -> {
            this.stateManager.loadState();
            if (this.tabListWidget != null) {
                this.tabListWidget.refreshList();
            }
            setStatus(Component.translatable("gui.recreative.discard_success").getString(), 0xFFAA00);
        }).bounds(this.width - 245 - leftX, 4, 70, 20).build());

        int propH = GuiUtil.PANEL_PADDING * 2 + PROP_ROW_H + PROP_ROW_GAP + 9;
        int gridY = contentY + propH + 4;
        int gridH = mainAreaH - propH - 26;

        this.tabItemGridWidget = new TabItemGridWidget(centerX, gridY, centerW, gridH, this.stateManager, this::onSelectItem);
        this.tabItemGridWidget.setOnStartDrag((idx, stack) -> {
            this.draggedStack = stack;
            this.dragSource = "grid";
            this.dragSourceIndex = idx;
        });
        int moveLeftX = this.tabItemGridWidget.getPanelX();
        int propPanelW = this.tabItemGridWidget.getPanelWidth();

        int propInnerX = moveLeftX + GuiUtil.PANEL_PADDING;
        int propInnerRight = moveLeftX + propPanelW - GuiUtil.PANEL_PADDING;
        int propInnerY = contentY + GuiUtil.PANEL_PADDING;

        this.iconRect = new int[]{propInnerX, propInnerY + (PROP_ROW_H - 16) / 2, 16, 16};

        int nameLabelW = this.font.width(Component.translatable("gui.recreative.tab_name_label"));
        int nameBoxX = propInnerX + 16 + ICON_TEXT_GAP + nameLabelW + 4;
        int nameBoxW = Math.max(30, propInnerRight - nameBoxX);

        this.tabNameBox = new ModEditBox(this.font, nameBoxX, propInnerY, nameBoxW, PROP_ROW_H, Component.translatable("gui.recreative.tab_name_label"));
        this.tabNameBox.setResponder(name -> {
            if (this.isUpdatingTab) return;
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    this.stateManager.setTabName(tab.id, name);
                    if (this.tabListWidget != null) {
                        this.tabListWidget.refreshList();
                    }
                }
            }
        });
        this.addRenderableWidget(this.tabNameBox);

        this.addRenderableWidget(this.tabItemGridWidget);

        int itemCtrlY = contentY + mainAreaH - 18;
        int btnGap = 4;
        int arrowW = 20;
        int rowRight = moveLeftX + propPanelW;
        int moveRightX = moveLeftX + arrowW + btnGap;
        int removeX = moveRightX + arrowW + btnGap;

        int actionSpace = rowRight - removeX - btnGap;
        int actionW = Math.min(80, Math.max(12, actionSpace / 2));
        int clearX = rowRight - actionW;

        this.moveItemLeftButton = this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                int idx = this.tabItemGridWidget.getSelectedItemIndex();
                if (tab != null && idx > 0) {
                    this.stateManager.reorderItemInTab(tab.id, idx, idx - 1);
                    this.tabItemGridWidget.setSelectedItemIndex(idx - 1);
                }
            }
        }).bounds(moveLeftX, itemCtrlY, arrowW, 18).tooltip(Tooltip.create(Component.translatable("gui.recreative.move_left"))).build());

        this.moveItemRightButton = this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                int idx = this.tabItemGridWidget.getSelectedItemIndex();
                if (tab != null && idx >= 0 && idx < tab.displayItems.size() - 1) {
                    this.stateManager.reorderItemInTab(tab.id, idx, idx + 1);
                    this.tabItemGridWidget.setSelectedItemIndex(idx + 1);
                }
            }
        }).bounds(moveRightX, itemCtrlY, arrowW, 18).tooltip(Tooltip.create(Component.translatable("gui.recreative.move_right"))).build());

        this.removeItemButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.remove_item"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                int idx = this.tabItemGridWidget.getSelectedItemIndex();
                if (tab != null && idx >= 0 && idx < tab.displayItems.size()) {
                    this.stateManager.removeItemFromTab(tab.id, idx);
                    if (idx >= tab.displayItems.size()) {
                        this.tabItemGridWidget.setSelectedItemIndex(tab.displayItems.size() - 1);
                    }
                }
            }
        }).bounds(removeX, itemCtrlY, actionW, 18).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.clear_all"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    while (!tab.displayItems.isEmpty()) {
                        this.stateManager.removeItemFromTab(tab.id, 0);
                    }
                    this.tabItemGridWidget.setSelectedItemIndex(-1);
                }
            }
        }).bounds(clearX, itemCtrlY, actionW, 18).build());

        ModEditBox tabSearchBox = new ModEditBox(this.font, leftX, contentY, leftW, 18, Component.translatable("gui.recreative.search_tabs_hint"));
        tabSearchBox.setPlaceholder(Component.translatable("gui.recreative.search_tabs_hint"));
        tabSearchBox.setResponder(q -> {
            if (this.tabListWidget != null) {
                this.tabListWidget.setSearchQuery(q);
            }
        });
        this.addRenderableWidget(tabSearchBox);

        int listPanelTop = contentY + 22;
        int listPanelH = mainAreaH - 44;
        int listContentTop = listPanelTop + GuiUtil.PANEL_PADDING;
        int listContentH = Math.max(GuiUtil.SCROLLBAR_WIDTH, listPanelH - GuiUtil.PANEL_PADDING * 2);
        int listContentW = Math.max(40, leftW - GuiUtil.PANEL_PADDING * 2 - GuiUtil.SCROLLBAR_EXTRA_WIDTH);

        this.tabListWidget = new TabListWidget(this.minecraft, listContentW, listContentH + 4, listContentTop - 4, 26, this.stateManager, this::onSelectTab);
        this.tabListWidget.setX(leftX + GuiUtil.PANEL_PADDING);
        this.addRenderableWidget(this.tabListWidget);

        int btnY = contentY + mainAreaH - 18;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.btn_new"), b -> {
            this.minecraft.setScreen(new CreateTabModal(this, this.stateManager));
        }).bounds(leftX, btnY, 45, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    this.stateManager.moveTab(tab.id, -1);
                    this.tabListWidget.refreshList();
                    this.tabListWidget.selectTab(tab.id);
                }
            }
        }).bounds(leftX + 48, btnY, 22, 18).tooltip(Tooltip.create(Component.translatable("gui.recreative.move_up"))).build());

        this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    this.stateManager.moveTab(tab.id, 1);
                    this.tabListWidget.refreshList();
                    this.tabListWidget.selectTab(tab.id);
                }
            }
        }).bounds(leftX + 73, btnY, 22, 18).tooltip(Tooltip.create(Component.translatable("gui.recreative.move_down"))).build());

        this.resetTabButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.btn_reset"), b -> {
            if (this.tabItemGridWidget != null) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    this.stateManager.resetTab(tab.id);
                    this.tabListWidget.refreshList();
                    this.tabItemGridWidget.setSelectedItemIndex(-1);
                    syncNameBox(this.stateManager.getTab(tab.id));
                    updateButtonStates();
                }
            }
        }).bounds(leftX + 98, btnY, 47, 18).build());

        List<String> modFilterList = new ArrayList<>();
        modFilterList.add("all");

        int searchBoxY = this.height - 21;

        int paletteY = contentY + 22;
        int paletteH = Math.max(GuiUtil.PANEL_PADDING * 2 + 18, searchBoxY - 4 - paletteY);

        this.itemPaletteWidget = new ItemPaletteWidget(rightX, paletteY, rightW, paletteH, this::onAddItemFromPalette);
        this.itemPaletteWidget.setOnStartDrag(stack -> {
            this.draggedStack = stack;
            this.dragSource = "palette";
            this.dragSourceIndex = -1;
        });
        modFilterList.addAll(this.itemPaletteWidget.getModIds());

        this.modFilterDropdown = new DropdownWidget(rightX, contentY, rightW, 18,
                Component.translatable("gui.recreative.mod_filter_label"),
                modFilterList,
                mod -> mod == null || mod.equals("all")
                        ? Component.translatable("gui.recreative.mod_all")
                        : Component.literal(mod),
                mod -> this.itemPaletteWidget.setSelectedMod(mod));
        this.addRenderableWidget(this.modFilterDropdown);

        this.addRenderableWidget(this.itemPaletteWidget);

        ModEditBox paletteSearchBox = new ModEditBox(this.font, moveLeftX, searchBoxY, propPanelW, 18, Component.translatable("gui.recreative.search_palette_hint"));
        paletteSearchBox.setPlaceholder(Component.translatable("gui.recreative.search_palette_hint"));
        paletteSearchBox.setResponder(q -> {
            if (this.itemPaletteWidget != null) {
                this.itemPaletteWidget.setSearchQuery(q);
            }
        });
        this.addRenderableWidget(paletteSearchBox);

        List<EditorStateManager.EditableTab> tabs = this.stateManager.getTabs();
        if (!tabs.isEmpty()) {
            this.tabListWidget.selectTab(tabs.getFirst().id);
        }
    }

    private void onSelectTab(EditorStateManager.EditableTab tab) {
        if (tab == null) return;

        EditorStateManager.EditableTab previous = this.tabItemGridWidget != null ? this.tabItemGridWidget.getTab() : null;
        boolean sameTab = previous != null && previous.id.equals(tab.id);

        if (this.tabItemGridWidget != null) {
            this.tabItemGridWidget.setTab(tab);
        }
        if (!sameTab) {
            syncNameBox(tab);
        }
        updateButtonStates();
    }

    private void syncNameBox(EditorStateManager.EditableTab tab) {
        if (this.tabNameBox == null || tab == null) return;
        this.isUpdatingTab = true;
        try {
            String custom = tab.customDisplayName;
            String targetValue = custom != null ? custom : tab.defaultDisplayName.getString();
            if (!this.tabNameBox.getValue().equals(targetValue)) {
                this.tabNameBox.setValue(targetValue);
            }
            this.tabNameBox.setCursorPosition(0);
            this.tabNameBox.setHighlightPos(0);
        } finally {
            this.isUpdatingTab = false;
        }
    }

    private void onSelectItem(int index) {
        updateButtonStates();
    }

    private void onAddItemFromPalette(ItemStack stack) {
        EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
        if (tab == null || stack.isEmpty()) return;

        ItemEntry entry = createItemEntry(stack);

        int targetIdx = this.tabItemGridWidget.getSelectedItemIndex();
        if (targetIdx >= 0 && targetIdx < tab.displayItems.size()) {
            this.stateManager.addItemToTab(tab.id, entry, targetIdx + 1);
            this.tabItemGridWidget.setSelectedItemIndex(targetIdx + 1);
        } else {
            this.stateManager.addItemToTab(tab.id, entry, tab.displayItems.size());
            this.tabItemGridWidget.setSelectedItemIndex(tab.displayItems.size() - 1);
        }

        this.tabListWidget.refreshList();
        setStatus(Component.translatable("gui.recreative.item_added_notification", stack.getHoverName().getString()).getString(), 0x55FF55);
    }

    private ItemEntry createItemEntry(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ItemEntry entry = new ItemEntry(key.toString());

        DataComponentPatch patch = stack.getComponentsPatch();
        if (!patch.isEmpty()) {
            try {
                var holders = this.minecraft.player != null ? this.minecraft.player.connection.registryAccess() : null;
                if (holders != null) {
                    var componentJson = DataComponentPatch.CODEC.encodeStart(
                            RegistryOps.create(JsonOps.INSTANCE, holders),
                            patch
                    ).getOrThrow(IllegalStateException::new);
                    entry.components = componentJson.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return entry;
    }

    private void updateButtonStates() {
        if (this.tabItemGridWidget == null) return;
        EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
        int idx = this.tabItemGridWidget.getSelectedItemIndex();
        boolean hasTab = tab != null;
        boolean hasItem = hasTab && idx >= 0 && idx < tab.displayItems.size();

        if (this.moveItemLeftButton != null) this.moveItemLeftButton.active = hasItem && idx > 0;
        if (this.moveItemRightButton != null)
            this.moveItemRightButton.active = hasItem && idx < tab.displayItems.size() - 1;
        if (this.removeItemButton != null) this.removeItemButton.active = hasItem;
        if (this.resetTabButton != null) this.resetTabButton.active = hasTab && (tab.isModified() || tab.isCustomTab);
    }

    public void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusClearTime = System.currentTimeMillis() + 4000;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusClearTime > 0 && System.currentTimeMillis() > statusClearTime) {
            statusMessage = "";
            statusClearTime = 0;
        }
        if (this.saveButton != null) {
            this.saveButton.active = this.stateManager.isDirty();
        }
        if (this.discardButton != null) {
            this.discardButton.active = this.stateManager.isDirty();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!draggedStack.isEmpty()) {
            if (this.tabItemGridWidget != null && this.tabItemGridWidget.isMouseOver(mouseX, mouseY)) {
                EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
                if (tab != null) {
                    int targetIdx = this.tabItemGridWidget.getInsertionIndex(mouseX, mouseY);
                    if ("palette".equals(dragSource)) {
                        ItemEntry entry = createItemEntry(draggedStack);
                        this.stateManager.addItemToTab(tab.id, entry, targetIdx);
                        this.tabItemGridWidget.setSelectedItemIndex(targetIdx);
                        this.tabListWidget.refreshList();
                        setStatus(Component.translatable("gui.recreative.item_added_notification", draggedStack.getHoverName().getString()).getString(), 0x55FF55);
                    } else if ("grid".equals(dragSource)) {
                        if (dragSourceIndex >= 0 && dragSourceIndex < tab.displayItems.size()) {
                            int finalIdx = targetIdx > dragSourceIndex ? targetIdx - 1 : targetIdx;
                            this.stateManager.reorderItemInTab(tab.id, dragSourceIndex, finalIdx);
                            this.tabItemGridWidget.setSelectedItemIndex(finalIdx);
                        }
                    }
                }
            }
            this.draggedStack = ItemStack.EMPTY;
            this.dragSource = null;
            this.dragSourceIndex = -1;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.fill(0, 0, this.width, 28, 0xDD181818);
        guiGraphics.renderOutline(0, 0, this.width, 28, 0xFF333333);

        guiGraphics.drawString(this.font, this.title, 8, 10, 0xFFFFFF);

        int statusX = 8 + this.font.width(this.title) + 12;
        int statusMaxW = (this.width - 245 - 6) - 8 - statusX;
        if (!statusMessage.isEmpty()) {
            drawClipped(guiGraphics, statusMessage, statusX, 10, statusMaxW, statusColor);
        } else if (this.stateManager.isDirty()) {
            drawClipped(guiGraphics, Component.translatable("gui.recreative.unsaved_changes").getString(),
                    statusX, 10, statusMaxW, 0xFFAA00);
        }

        EditorStateManager.EditableTab currentTab = this.tabItemGridWidget != null ? this.tabItemGridWidget.getTab() : null;
        int pad = 6;
        int leftW = 145;
        int centerX = pad + leftW + pad;
        int centerW = this.width - leftW - 150 - pad * 4;
        int contentY = 28 + pad;
        int propH = GuiUtil.PANEL_PADDING * 2 + PROP_ROW_H + PROP_ROW_GAP + 9;

        GuiUtil.drawVanillaPanel(guiGraphics, centerX, contentY, centerW, propH);

        if (currentTab != null) {
            int innerX = centerX + GuiUtil.PANEL_PADDING;
            int innerRight = centerX + centerW - GuiUtil.PANEL_PADDING;
            int innerY = contentY + GuiUtil.PANEL_PADDING;

            int iconX = this.iconRect[0];
            int iconY = this.iconRect[1];
            if (isOverIcon(mouseX, mouseY)) {
                guiGraphics.fill(iconX - 2, iconY - 2, iconX + 18, iconY + 18, 0x33FFFFFF);
            }

            if (currentTab.customIcon != null && (currentTab.customIcon.contains("/") || currentTab.customIcon.endsWith(".png"))) {
                try {
                    ResourceLocation tex = ResourceLocation.parse(currentTab.customIcon);
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    guiGraphics.blit(tex, iconX, iconY, 0, 0, 16, 16, 16, 16);
                    com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                } catch (Exception e) {
                    ItemStack icon = currentTab.getEffectiveIconStack();
                    if (!icon.isEmpty()) {
                        guiGraphics.renderFakeItem(icon, iconX, iconY);
                    }
                }
            } else {
                ItemStack icon = currentTab.getEffectiveIconStack();
                if (!icon.isEmpty()) {
                    guiGraphics.renderFakeItem(icon, iconX, iconY);
                }
            }

            int labelY = innerY + (PROP_ROW_H - 8) / 2;
            guiGraphics.drawString(this.font, Component.translatable("gui.recreative.tab_name_label"),
                    innerX + 16 + ICON_TEXT_GAP, labelY, 0xAAAAAA);

            int detailY = innerY + PROP_ROW_H + PROP_ROW_GAP;
            int rightAlignX = innerRight;
            if (currentTab.isRemoved) {
                Component hiddenText = Component.translatable("gui.recreative.badge_hidden");
                int hiddenW = this.font.width(hiddenText);
                int hiddenX = rightAlignX - hiddenW;
                guiGraphics.drawString(this.font, hiddenText, hiddenX, detailY, 0xFF5555);
                rightAlignX = hiddenX - 10;
            }

            Component itemsText = Component.translatable("gui.recreative.tab_items_prefix", currentTab.displayItems.size());
            int itemsW = this.font.width(itemsText);
            int itemsX = rightAlignX - itemsW;

            int maxIdW = itemsX - innerX - 6;
            String rawIdText = Component.translatable("gui.recreative.tab_id_prefix", currentTab.id).getString();
            String displayIdText = rawIdText;
            if (this.font.width(rawIdText) > maxIdW && maxIdW > 20) {
                displayIdText = this.font.plainSubstrByWidth(rawIdText, maxIdW - this.font.width("...")) + "...";
            }

            guiGraphics.drawString(this.font, displayIdText, innerX, detailY, 0x777777);
            guiGraphics.drawString(this.font, itemsText, itemsX, detailY, 0x888888);
        }
    }

    private void drawClipped(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        if (this.font.width(text) > maxWidth) {
            int ellipsisW = this.font.width("...");
            if (maxWidth <= ellipsisW) return;
            text = this.font.plainSubstrByWidth(text, maxWidth - ellipsisW) + "...";
        }
        guiGraphics.drawString(this.font, text, x, y, color);
    }

    private boolean isOverIcon(double mouseX, double mouseY) {
        if (this.tabItemGridWidget == null || this.tabItemGridWidget.getTab() == null) return false;
        return mouseX >= iconRect[0] && mouseX < iconRect[0] + iconRect[2]
                && mouseY >= iconRect[1] && mouseY < iconRect[1] + iconRect[3];
    }

    private void openIconPicker() {
        if (this.tabItemGridWidget == null) return;
        EditorStateManager.EditableTab tab = this.tabItemGridWidget.getTab();
        if (tab == null) return;
        this.minecraft.setScreen(new IconPickerModal(this, icon -> {
            this.stateManager.setTabIcon(tab.id, icon);
            if (this.tabListWidget != null) {
                this.tabListWidget.refreshList();
            }
        }));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modFilterDropdown != null && this.modFilterDropdown.isOpen()) {
            if (this.modFilterDropdown.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0 && isOverIcon(mouseX, mouseY)) {
            openIconPicker();
            return true;
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 0 && this.tabItemGridWidget != null
                && this.tabItemGridWidget.getSelectedItemIndex() >= 0
                && !this.tabItemGridWidget.isOverItem(mouseX, mouseY)
                && !isOverItemControls(mouseX, mouseY)) {
            this.tabItemGridWidget.setSelectedItemIndex(-1);
        }
        return handled;
    }

    private boolean isOverItemControls(double mouseX, double mouseY) {
        return isOver(moveItemLeftButton, mouseX, mouseY)
                || isOver(moveItemRightButton, mouseX, mouseY)
                || isOver(removeItemButton, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.modFilterDropdown != null && this.modFilterDropdown.isOpen()
                && this.modFilterDropdown.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.modFilterDropdown != null) {
            this.modFilterDropdown.renderOverlay(guiGraphics, mouseX, mouseY);
        }

        if (isOverIcon(mouseX, mouseY) && draggedStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.recreative.change_icon_tip"), mouseX, mouseY);
        }

        if (!draggedStack.isEmpty()) {
            if (this.tabItemGridWidget != null && this.tabItemGridWidget.isMouseOver(mouseX, mouseY)) {
                int insertIdx = this.tabItemGridWidget.getInsertionIndex(mouseX, mouseY);
                this.tabItemGridWidget.renderInsertionMarker(guiGraphics, insertIdx);
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, 0.0f, 400.0f);
            guiGraphics.renderFakeItem(draggedStack, mouseX - 8, mouseY - 8);
            guiGraphics.renderItemDecorations(this.font, draggedStack, mouseX - 8, mouseY - 8);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;

        if (this.stateManager.isDirty()) {
            this.minecraft.setScreen(new UnsavedChangesModal(this,
                    () -> {
                        try {
                            this.stateManager.saveAndApply();
                            this.minecraft.setScreen(this.parent);
                        } catch (Exception e) {
                            Constants.LOG.error("Failed to save Recreative tabs", e);
                            this.minecraft.setScreen(this);
                            setStatus(Component.translatable("gui.recreative.save_failure").getString(), 0xFF5555);
                        }
                    },
                    () -> this.minecraft.setScreen(this.parent)));
            return;
        }
        this.minecraft.setScreen(this.parent);
    }
}
