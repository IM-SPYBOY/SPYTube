package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class SeasonDetailResponse {
    @SerializedName("episodes")
    public List<Episode> episodes;

    public static class Episode {
        @SerializedName("episode_number")
        public int episodeNumber;

        @SerializedName("name")
        public String name;

        @SerializedName("overview")
        public String overview;

        @SerializedName("still_path")
        public String stillPath;

        @SerializedName("runtime")
        public Integer runtime;
    }
}
