package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TvDetails {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("number_of_seasons")
    public int numberOfSeasons;

    @SerializedName("seasons")
    public List<Season> seasons;

    public static class Season {
        @SerializedName("season_number")
        public int seasonNumber;

        @SerializedName("episode_count")
        public int episodeCount;

        @SerializedName("name")
        public String name;
    }
}
