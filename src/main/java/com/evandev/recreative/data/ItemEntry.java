package com.evandev.recreative.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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

    public static JsonElement toJson(ItemEntry entry) {
        if (entry.after == null && entry.before == null && entry.components == null) {
            return new JsonPrimitive(entry.item);
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("item", entry.item);
        if (entry.after != null) obj.add("after", ItemRef.toJson(entry.after));
        if (entry.before != null) obj.add("before", ItemRef.toJson(entry.before));
        ComponentUtil.writeJson(obj, entry.components);
        return obj;
    }

    public ItemEntry copy() {
        ItemEntry copy = new ItemEntry(this.item);
        copy.after = this.after != null ? this.after.copy() : null;
        copy.before = this.before != null ? this.before.copy() : null;
        copy.components = this.components;
        return copy;
    }
}
