package com.evandev.recreative.mixin;

import com.evandev.recreative.api.ICustomIconTab;
import com.evandev.recreative.client.gui.button.CreativeScreenEditorButton;
import com.evandev.recreative.config.ModConfig;
import com.evandev.recreative.mixin.accessor.AbstractContainerScreenAccessor;
import com.evandev.recreative.mixin.accessor.ScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Shadow
    protected abstract int getTabX(CreativeModeTab tab);

    @Inject(method = "init", at = @At("RETURN"))
    private void recreative$addEditorButton(CallbackInfo ci) {
        if (!ModConfig.get().showEditorButton) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        ScreenAccessor screenAccessor = (ScreenAccessor) this;
        screenAccessor.recreative$invokeAddRenderableWidget(new CreativeScreenEditorButton(
                () -> accessor.getLeftPos() + accessor.getImageWidth() + 4,
                () -> accessor.getTopPos() + 6,
                Component.literal("Edit")
        ));
    }

    @Inject(method = "renderTabButton", at = @At("TAIL"))
    private void recreative$renderCustomTabIcon(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab, CallbackInfo ci) {
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

                guiGraphics.blit(iconPath, drawX, drawY, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}