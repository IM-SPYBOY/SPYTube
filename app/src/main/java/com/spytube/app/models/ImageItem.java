package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;

public class ImageItem {
    @SerializedName("file_path")
    public String filePath;

    @SerializedName("iso_639_1")
    public String language;
}
