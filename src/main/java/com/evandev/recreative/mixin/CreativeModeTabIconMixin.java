package com.evandev.recreative.mixin;

import com.evandev.recreative.api.ICustomIconTab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabIconMixin implements ICustomIconTab {
    @Unique
    private ResourceLocation recreative$customIcon;

    @Override
    public ResourceLocation recreative$getCustomIcon() {
        return recreative$customIcon;
    }

    @Override
    public void recreative$setCustomIcon(ResourceLocation identifier) {
        this.recreative$customIcon = identifier;
    }
}