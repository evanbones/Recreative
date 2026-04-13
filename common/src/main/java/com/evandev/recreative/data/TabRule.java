package com.evandev.recreative.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TabRule {
    public Action action;

    @SerializedName(value = "tab", alternate = {"tabs", "id", "ids"})
    public List<String> tabs = new ArrayList<>();

    public String name;
    public String icon;

    @SerializedName(value = "remove_items")
    public List<String> removeItems = new ArrayList<>();

    @SerializedName(value = "add_items", alternate = {"items"})
    public List<ItemEntry> addItems = new ArrayList<>();

    public List<String> order = new ArrayList<>();
}