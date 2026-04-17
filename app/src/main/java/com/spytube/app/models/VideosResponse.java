package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class VideosResponse {
    @SerializedName("results")
    public List<Video> results;

    public static class Video {
        @SerializedName("key")
        public String key;

        @SerializedName("site")
        public String site;

        @SerializedName("type")
        public String type;

        @SerializedName("name")
        public String name;

        @SerializedName("official")
        public boolean official;
    }
}
