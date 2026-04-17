package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbResponse {
    @SerializedName("page")
    public int page;

    @SerializedName("results")
    public List<MediaItem> results;

    @SerializedName("total_pages")
    public int totalPages;

    @SerializedName("total_results")
    public int totalResults;
}
