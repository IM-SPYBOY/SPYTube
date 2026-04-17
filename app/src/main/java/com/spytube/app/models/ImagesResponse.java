package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ImagesResponse {
    @SerializedName("id")
    public int id;

    @SerializedName("logos")
    public List<ImageItem> logos;
}
