package com.evandev.recreative.mixin;

import com.evandev.recreative.api.ICustomIconTab;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabIconMixin implements ICustomIconTab {
    @Unique
    private Identifier recreative$customIcon;

    @Override
    public Identifier recreative$getCustomIcon() {
        return recreative$customIcon;
    }

    @Override
    public void recreative$setCustomIcon(Identifier identifier) {
        this.recreative$customIcon = identifier;
    }
}