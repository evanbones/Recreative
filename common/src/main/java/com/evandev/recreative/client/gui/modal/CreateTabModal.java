package com.evandev.recreative.client.gui.modal;

import com.evandev.recreative.client.editor.EditorStateManager;
import com.evandev.recreative.client.gui.util.GuiUtil;
import com.evandev.recreative.client.gui.widget.ModEditBox;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CreateTabModal extends Screen {
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

        // Icon picker button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.choose_icon"), b -> {
            this.minecraft.setScreen(new IconPickerModal(this, icon -> {
                this.selectedIcon = icon;
            }));
        }).bounds(centerX - fieldW / 2 + 26, centerY + 30, fieldW - 26, 20).build());

        // Create button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.create_tab.confirm"), b -> {
            String id = this.idBox.getValue().trim();
            String name = this.nameBox.getValue().trim();
            if (!id.isEmpty()) {
                if (!id.contains(":")) id = "recreative:" + id;
                if (name.isEmpty()) name = id;
                stateManager.createCustomTab(id, name, selectedIcon);
                this.minecraft.setScreen(parent);
            }
        }).bounds(centerX - fieldW / 2, centerY + 65, 85, 20).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 10, centerY + 65, 85, 20).build());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int modalW = 224;
        int modalH = 176;
        int modalX = centerX - modalW / 2;
        int modalY = centerY - modalH / 2;

        GuiUtil.drawDialogPanel(guiGraphics, modalX, modalY, modalW, modalH);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, modalY + 8, 0xFFFFFF);

        guiGraphics.drawString(this.font, Component.translatable("gui.recreative.tab_id"), centerX - 90, centerY - 57, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.recreative.tab_name"), centerX - 90, centerY - 17, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.recreative.tab_icon"), centerX - 90, centerY + 18, 0xAAAAAA);

        int iconSlotX = centerX - 90;
        int iconSlotY = centerY + 31;
        GuiUtil.drawSlot(guiGraphics, iconSlotX, iconSlotY);

        if (selectedIcon != null && (selectedIcon.contains("/") || selectedIcon.endsWith(".png"))) {
            try {
                ResourceLocation tex = ResourceLocation.parse(selectedIcon);
                RenderSystem.enableBlend();
                guiGraphics.blit(tex, iconSlotX + 1, iconSlotY + 1, 0, 0, 16, 16, 16, 16);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                ItemStack iconStack = EditorStateManager.resolveIconStack(selectedIcon);
                if (!iconStack.isEmpty()) {
                    guiGraphics.renderFakeItem(iconStack, iconSlotX + 1, iconSlotY + 1);
                }
            }
        } else {
            ItemStack iconStack = EditorStateManager.resolveIconStack(selectedIcon);
            if (!iconStack.isEmpty()) {
                guiGraphics.renderFakeItem(iconStack, iconSlotX + 1, iconSlotY + 1);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}
