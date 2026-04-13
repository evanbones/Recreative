package com.evandev.recreative.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabConfig {
    @SerializedName("removed_tabs")
    public List<String> removedTabs = new ArrayList<>();

    @SerializedName("tab_order")
    public List<String> tabOrder = new ArrayList<>();

    @SerializedName("modify_tabs")
    public Map<String, ModifyTabDef> modifyTabs = new HashMap<>();

    @SerializedName("custom_tabs")
    public Map<String, CustomTabDef> customTabs = new HashMap<>();

    public static class ModifyTabDef {
        public String name;
        public String icon;
        @SerializedName("remove_items")
        public List<String> removeItems = new ArrayList<>();
        @SerializedName("add_items")
        public List<String> addItems = new ArrayList<>();
    }

    public static class CustomTabDef {
        public String name;
        public String icon;
        public List<String> items = new ArrayList<>();
    }
}