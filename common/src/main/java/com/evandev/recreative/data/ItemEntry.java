package com.evandev.recreative.data;

public class ItemEntry {
    public String item;
    public String after;
    public String before;
    public String components;

    public ItemEntry(String item) {
        this.item = item;
    }

    public ItemEntry copy() {
        ItemEntry copy = new ItemEntry(this.item);
        copy.after = this.after;
        copy.before = this.before;
        copy.components = this.components;
        return copy;
    }
}
