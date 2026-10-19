package com.evandev.recreative.mixin;

import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.client.gui.button.CreativeScreenEditorButton;
import com.evandev.recreative.client.gui.util.GuiCompat;
import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.mixin.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends Screen {

    protected CreativeModeInventoryScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract int getTabX(CreativeModeTab tab);

    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$addEditorButton(CallbackInfo ci) {
        if (!ModConfig.get().showEditorButton) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        this.addRenderableWidget(new CreativeScreenEditorButton(
                () -> accessor.getLeftPos() + accessor.getImageWidth() + 4,
                () -> accessor.getTopPos() + 6,
                Component.literal("Edit")
        ));
    }

    //? if <26.1 {
    @Inject(method = "renderTabButton", at = @At("TAIL"))
    private void recreative$renderCustomTabIcon(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab, CallbackInfo ci) {
        recreative$drawCustomTabIcon(guiGraphics, creativeModeTab);
    }
    //?} else {
    /*@Inject(method = "extractTabButton", at = @At("TAIL"))
    private void recreative$renderCustomTabIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, CreativeModeTab creativeModeTab, CallbackInfo ci) {
        recreative$drawCustomTabIcon(guiGraphics, creativeModeTab);
    }
    *///?}

    @Unique
    private void recreative$drawCustomTabIcon(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab) {
        if (creativeModeTab instanceof ICustomIconTab customTab) {
            ResourceLocation iconPath = customTab.recreative$getCustomIcon();

            if (iconPath != null) {
                boolean isTop = creativeModeTab.row() == CreativeModeTab.Row.TOP;

                AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
                int leftPos = accessor.getLeftPos();
                int topPos = accessor.getTopPos();
                int imageHeight = accessor.getImageHeight();

                int x = leftPos + this.getTabX(creativeModeTab);
                int y = topPos - (isTop ? 28 : -(imageHeight - 4));

                int drawX = x + 13 - 8;
                int drawY = y + 16 - 8 + (isTop ? 1 : -1);

                GuiCompat.blit(guiGraphics, iconPath, drawX, drawY, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}
