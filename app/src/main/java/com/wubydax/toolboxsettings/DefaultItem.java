package com.wubydax.toolboxsettings;

/**
 * Created by anna on 07/08/15.
 */
public class DefaultItem {
    private String itemName;
    private String itemDescription;
    private int imageId;

    public String getName() {
        return itemName;
    }

    public void setItemName(String text) {
        itemName = text;
    }

    public String getDescription() {
        return itemDescription;
    }

    public void setItemDescription (String text) {
        itemDescription= text;
    }

    public int getIcon() {
        return imageId;
    }

    public void setDrawable(int drawable) {
        imageId = drawable;
    }
}
