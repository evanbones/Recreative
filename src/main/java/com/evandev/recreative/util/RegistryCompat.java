package com.evandev.recreative.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public final class RegistryCompat {
    public static Item item(ResourceLocation id) {
        //? if <26.1 {
        return BuiltInRegistries.ITEM.get(id);
        //?} else {
        /*return BuiltInRegistries.ITEM.getValue(id);
        *///?}
    }

    public static CreativeModeTab tab(ResourceLocation id) {
        //? if <26.1 {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        //?} else {
        /*return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id);
        *///?}
    }

    public static void forEachItemTag(BiConsumer<TagKey<Item>, Iterable<Holder<Item>>> consumer) {
        //? if <26.1 {
        BuiltInRegistries.ITEM.getTags().forEach(pair -> consumer.accept(pair.getFirst(), pair.getSecond()));
        //?} else {
        /*BuiltInRegistries.ITEM.getTags().forEach(named -> consumer.accept(named.key(), named));
        *///?}
    }

    /**
     * Wraps a lookup provider in a new instance, so creative tab parameters built from it are never considered
     * equal to the cached ones and the tab contents get rebuilt
     */
    public static HolderLookup.Provider freshHolders(HolderLookup.Provider delegate) {
        return new HolderLookup.Provider() {
            //? if >=1.21 && <26.1 {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return delegate.listRegistries();
            }
            //?} else if >=26.1 {
            /*@Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return delegate.listRegistryKeys();
            }
            *///?}

            //? if <26.1 {
            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                return delegate.lookup(registryKey);
            }
            //?} else {
            /*@Override
            public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                return delegate.lookup(registryKey);
            }
            *///?}
        };
    }
}
