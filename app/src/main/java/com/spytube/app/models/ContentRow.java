package com.spytube.app.models;

import java.util.List;

public class ContentRow {
    public String title;
    public List<MediaItem> items;

    public ContentRow(String title, List<MediaItem> items) {
        this.title = title;
        this.items = items;
    }
}
