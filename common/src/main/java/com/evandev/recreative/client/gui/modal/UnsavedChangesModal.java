package com.evandev.recreative.client.gui.modal;

import com.evandev.recreative.client.gui.util.GuiUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnsavedChangesModal extends Screen {
    private static final int MODAL_W = 260;
    private static final int MODAL_H = 110;

    private final Screen parent;
    private final Runnable onSave;
    private final Runnable onDiscard;

    public UnsavedChangesModal(Screen parent, Runnable onSave, Runnable onDiscard) {
        super(Component.translatable("gui.recreative.unsaved.title"));
        this.parent = parent;
        this.onSave = onSave;
        this.onDiscard = onDiscard;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int modalY = this.height / 2 - MODAL_H / 2;
        int btnY = modalY + MODAL_H - 28;
        int btnW = 78;
        int gap = 4;
        int totalW = btnW * 3 + gap * 2;
        int x = centerX - totalW / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.unsaved.save"), b -> {
            onSave.run();
        }).bounds(x, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.recreative.unsaved.discard"), b -> {
            onDiscard.run();
        }).bounds(x + btnW + gap, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
            this.minecraft.setScreen(parent);
        }).bounds(x + (btnW + gap) * 2, btnY, btnW, 20).build());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int modalY = this.height / 2 - MODAL_H / 2;
        GuiUtil.drawDialogPanel(guiGraphics, centerX - MODAL_W / 2, modalY, MODAL_W, MODAL_H);

        guiGraphics.drawCenteredString(this.font, this.title, centerX, modalY + 12, 0xFFFFFF);

        List<FormattedCharSequence> lines =
                this.font.split(Component.translatable("gui.recreative.unsaved.message"), MODAL_W - 24);
        int lineY = modalY + 34;
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawCenteredString(this.font, line, centerX, lineY, 0xCCCCCC);
            lineY += 11;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
