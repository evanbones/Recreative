package com.evandev.recreative.data;

import com.google.gson.annotations.SerializedName;

public enum Action {
    @SerializedName(value = "REMOVE_TAB", alternate = {"remove_tab", "remove_tabs", "Remove_Tabs"})
    REMOVE_TAB,

    @SerializedName(value = "MODIFY_TAB", alternate = {"modify_tab", "modify_tabs", "Modify_Tabs"})
    MODIFY_TAB,

    @SerializedName(value = "CUSTOM_TAB", alternate = {"custom_tab", "custom_tabs", "Custom_Tabs"})
    CUSTOM_TAB,

    @SerializedName(value = "TAB_ORDER", alternate = {"tab_order", "Tab_Order"})
    TAB_ORDER
}