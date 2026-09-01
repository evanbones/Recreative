package com.evandev.recreative.client.gui.modal;

import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.client.gui.util.ItemSearchHelper;
import com.evandev.recreative.client.gui.widget.ModEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IconPickerModal extends Screen {
    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int SLOT_SIZE = 18;
    private final Screen parent;
    private final Consumer<String> onSelect;
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private ModEditBox textureBox;
    private int scrollOffset = 0;

    public IconPickerModal(Screen parent, Consumer<String> onSelect) {
        super(Component.translatable("gui.recreative.icon_picker.title"));
        this.parent = parent;
        this.onSelect = onSelect;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(new ItemStack(item));
            }
        }
        filteredItems.addAll(allItems);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxW = 200;

        ModEditBox searchBox = new ModEditBox(this.font, centerX - boxW / 2, centerY - 80, boxW, 18, Component.literal("Search Items"));
        searchBox.setPlaceholder(Component.translatable("gui.recreative.search_icon_hint"));
        searchBox.setResponder(this::updateSearch);
        this.addRenderableWidget(searchBox);

        this.textureBox = new ModEditBox(this.font, centerX - boxW / 2, centerY + 52, boxW, 18, Component.translatable("gui.recreative.custom_texture"));
        this.textureBox.setPlaceholder(Component.literal("modid:textures/gui/icon.png"));
        this.textureBox.setMaxLength(256);
        this.addRenderableWidget(this.textureBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.apply_texture"), b -> {
            String tex = this.textureBox.getValue().trim();
            if (!tex.isEmpty()) {
                onSelect.accept(tex);
                this.minecraft.setScreen(parent);
            }
        }).bounds(centerX - boxW / 2, centerY + 72, 95, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 5, centerY + 72, 95, 20).build());
    }

    private void updateSearch(String query) {
        filteredItems.clear();
        filteredItems.addAll(ItemSearchHelper.filterItems(allItems, query, null));
        scrollOffset = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, (filteredItems.size() + COLS - 1) / COLS - ROWS);
        if (scrollY > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        } else if (scrollY < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int gridX = centerX - (COLS * SLOT_SIZE) / 2;
        int gridY = centerY - 58;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = (scrollOffset + r) * COLS + c;
                if (index >= filteredItems.size()) break;

                int slotX = gridX + c * SLOT_SIZE;
                int slotY = gridY + r * SLOT_SIZE;

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    ItemStack stack = filteredItems.get(index);
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    onSelect.accept(id.toString());
                    this.minecraft.setScreen(parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int modalW = 224;
        int modalH = 204;
        int modalX = centerX - modalW / 2;
        int modalY = centerY - modalH / 2;

        GuiUtil.drawDialogPanel(guiGraphics, modalX, modalY, modalW, modalH);

        guiGraphics.drawCenteredString(this.font, this.title, centerX, modalY + 8, 0xFFFFFF);

        int gridX = centerX - (COLS * SLOT_SIZE) / 2;
        int gridY = centerY - 58;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = (scrollOffset + r) * COLS + c;
                int slotX = gridX + c * SLOT_SIZE;
                int slotY = gridY + r * SLOT_SIZE;

                GuiUtil.drawSlot(guiGraphics, slotX, slotY);

                if (index < filteredItems.size()) {
                    ItemStack stack = filteredItems.get(index);
                    guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);

                    if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                        GuiUtil.drawSlotHighlight(guiGraphics, slotX, slotY);
                    }
                }
            }
        }

        guiGraphics.drawString(this.font, Component.translatable("gui.recreative.or_texture_path"), centerX - 100, centerY + 40, 0xFFFFFF);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int gridX = centerX - (COLS * SLOT_SIZE) / 2;
        int gridY = centerY - 58;
        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = (scrollOffset + r) * COLS + c;
                int slotX = gridX + c * SLOT_SIZE;
                int slotY = gridY + r * SLOT_SIZE;

                if (index < filteredItems.size()) {
                    if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                        hoveredStack = filteredItems.get(index);
                    }
                }
            }
        }

        if (!hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }
}
