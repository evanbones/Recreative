package com.evandev.recreative.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.function.Supplier;

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
*///?} else if neoforge {
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
//?} else {
/*import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}

public final class Platform {
    public static boolean isModLoaded(String modId) {
        //? if fabric {
        /*return FabricLoader.getInstance().isModLoaded(modId);
         *///?} else {
        return ModList.get().isLoaded(modId);
        //?}
    }

    public static Path getConfigDirectory() {
        //? if fabric {
        /*return FabricLoader.getInstance().getConfigDir();
         *///?} else {
        return FMLPaths.CONFIGDIR.get();
        //?}
    }

    public static boolean isPhysicalClient() {
        //? if fabric {
        /*return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
         *///?} else if neoforge && >=26.1 {
        /*return FMLEnvironment.getDist().isClient();
         *///?} else {
        return FMLEnvironment.dist.isClient();
        //?}
    }

    public static CreativeModeTab buildCreativeTab(Component title, Supplier<ItemStack> icon,
                                                   CreativeModeTab.DisplayItemsGenerator displayItems) {
        //? if fabric && <26.1 {
        /*return net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
         *///?} else if fabric {
        /*return net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
         *///?} else {
        return CreativeModeTab.builder()
        //?}
                .title(title)
                .icon(icon)
                .displayItems(displayItems)
                .build();
    }
}
