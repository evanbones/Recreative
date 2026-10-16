package com.evandev.recreative.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

public class ItemRef {
    public String item;
    public String components;

    public ItemRef(String item) {
        this.item = item;
    }

    public ItemRef(String item, String components) {
        this.item = item;
        this.components = components;
    }

    public static ItemRef fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) return null;
        if (json.isJsonPrimitive()) return new ItemRef(json.getAsString());
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            if (!obj.has("item")) return null;
            ItemRef ref = new ItemRef(obj.get("item").getAsString());
            ref.components = ComponentUtil.readJson(obj);
            return ref;
        }
        return null;
    }

    public static JsonElement toJson(ItemRef ref) {
        if (ref == null || ref.item == null) return null;
        if (ref.components == null) return new JsonPrimitive(ref.item);

        JsonObject obj = new JsonObject();
        obj.addProperty("item", ref.item);
        ComponentUtil.writeJson(obj, ref.components);
        return obj;
    }

    public ItemRef copy() {
        return new ItemRef(this.item, this.components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemRef other)) return false;
        return Objects.equals(item, other.item) && Objects.equals(components, other.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, components);
    }
}
