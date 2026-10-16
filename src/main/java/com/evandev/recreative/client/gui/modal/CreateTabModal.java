package com.evandev.recreative.client.gui.modal;

import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.BaseScreen;
import com.evandev.recreative.client.gui.util.GuiCompat;
import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.client.gui.widget.ModEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CreateTabModal extends BaseScreen {
    private final Screen parent;
    private final EditorStateManager stateManager;
    private ModEditBox idBox;
    private ModEditBox nameBox;
    private String selectedIcon = "minecraft:chest";

    public CreateTabModal(Screen parent, EditorStateManager stateManager) {
        super(Component.translatable("gui.recreative.create_tab.title"));
        this.parent = parent;
        this.stateManager = stateManager;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int fieldW = 180;

        this.idBox = new ModEditBox(this.font, centerX - fieldW / 2, centerY - 45, fieldW, 18, Component.translatable("gui.recreative.tab_id"));
        this.idBox.setPlaceholder(Component.literal("recreative:custom_tab"));
        this.idBox.setMaxLength(64);
        this.addRenderableWidget(this.idBox);

        this.nameBox = new ModEditBox(this.font, centerX - fieldW / 2, centerY - 5, fieldW, 18, Component.translatable("gui.recreative.tab_name"));
        this.nameBox.setPlaceholder(Component.literal("My Custom Tab"));
        this.nameBox.setMaxLength(64);
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.choose_icon"), b -> {
            GuiCompat.setScreen(new IconPickerModal(this, icon -> {
                this.selectedIcon = icon;
            }));
        }).bounds(centerX - fieldW / 2 + 26, centerY + 30, fieldW - 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.create_tab.confirm"), b -> {
            String id = this.idBox.getValue().trim();
            String name = this.nameBox.getValue().trim();
            if (!id.isEmpty()) {
                if (!id.contains(":")) id = "recreative:" + id;
                if (name.isEmpty()) name = id;
                stateManager.createCustomTab(id, name, selectedIcon);
                GuiCompat.setScreen(parent);
            }
        }).bounds(centerX - fieldW / 2, centerY + 65, 85, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            GuiCompat.setScreen(parent);
        }).bounds(centerX + 10, centerY + 65, 85, 20).build());
    }

    @Override
    protected void renderBackgroundLayer(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int modalW = 224;
        int modalH = 176;
        int modalX = centerX - modalW / 2;
        int modalY = centerY - modalH / 2;

        GuiUtil.drawDialogPanel(guiGraphics, modalX, modalY, modalW, modalH);
        GuiCompat.drawString(guiGraphics, this.font, this.title, centerX - this.font.width(this.title) / 2, modalY + 8, 0x404040, false);

        GuiCompat.drawString(guiGraphics, this.font, Component.translatable("gui.recreative.tab_id"), centerX - 90, centerY - 57, 0x404040, false);
        GuiCompat.drawString(guiGraphics, this.font, Component.translatable("gui.recreative.tab_name"), centerX - 90, centerY - 17, 0x404040, false);
        GuiCompat.drawString(guiGraphics, this.font, Component.translatable("gui.recreative.tab_icon"), centerX - 90, centerY + 18, 0x404040, false);

        int iconSlotX = centerX - 90;
        int iconSlotY = centerY + 31;
        GuiUtil.drawSlot(guiGraphics, iconSlotX, iconSlotY);

        if (selectedIcon != null && (selectedIcon.contains("/") || selectedIcon.endsWith(".png"))) {
            try {
                ResourceLocation tex = ResourceLocation.parse(selectedIcon);
                GuiCompat.enableBlend();
                GuiCompat.blit(guiGraphics, tex, iconSlotX + 1, iconSlotY + 1, 0, 0, 16, 16, 16, 16);
                GuiCompat.disableBlend();
            } catch (Exception e) {
                ItemStack iconStack = EditorStateManager.resolveIconStack(selectedIcon);
                if (!iconStack.isEmpty()) {
                    GuiCompat.fakeItem(guiGraphics, iconStack, iconSlotX + 1, iconSlotY + 1);
                }
            }
        } else {
            ItemStack iconStack = EditorStateManager.resolveIconStack(selectedIcon);
            if (!iconStack.isEmpty()) {
                GuiCompat.fakeItem(guiGraphics, iconStack, iconSlotX + 1, iconSlotY + 1);
            }
        }
    }
}
