package com.evandev.recreative.client.editor;

import com.evandev.recreative.data.ItemEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EditorClipboard {
    private static final List<ItemEntry> ENTRIES = new ArrayList<>();

    private EditorClipboard() {
    }

    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void set(Collection<ItemEntry> entries) {
        ENTRIES.clear();
        if (entries == null) return;
        for (ItemEntry entry : entries) {
            if (entry != null && entry.item != null) {
                ItemEntry copy = entry.copy();
                copy.after = null;
                copy.before = null;
                ENTRIES.add(copy);
            }
        }
    }

    public static List<ItemEntry> get() {
        List<ItemEntry> out = new ArrayList<>(ENTRIES.size());
        for (ItemEntry entry : ENTRIES) {
            out.add(entry.copy());
        }
        return out;
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
