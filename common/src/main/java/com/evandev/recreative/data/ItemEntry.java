package com.evandev.recreative.data;

public class ItemEntry {
    public String item;
    public ItemRef after;
    public ItemRef before;
    public String components;

    public ItemEntry(String item) {
        this.item = item;
    }

    public ItemEntry(String item, String components) {
        this.item = item;
        this.components = components;
    }

    public ItemEntry copy() {
        ItemEntry copy = new ItemEntry(this.item);
        copy.after = this.after != null ? this.after.copy() : null;
        copy.before = this.before != null ? this.before.copy() : null;
        copy.components = this.components;
        return copy;
    }
}
