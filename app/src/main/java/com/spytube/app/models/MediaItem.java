package com.spytube.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class MediaItem implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("title")
    public String title;

    @SerializedName("name")
    public String name;

    @SerializedName("overview")
    public String overview;

    @SerializedName("poster_path")
    public String posterPath;

    @SerializedName("backdrop_path")
    public String backdropPath;

    @SerializedName("vote_average")
    public double voteAverage;

    @SerializedName("release_date")
    public String releaseDate;

    @SerializedName("first_air_date")
    public String firstAirDate;

    @SerializedName("media_type")
    public String mediaType;

    @SerializedName("genre_ids")
    public java.util.List<Integer> genreIds;

    public String getDisplayTitle() {
        if (title != null) return title;
        if (name != null) return name;
        return "Unknown";
    }

    public String getYear() {
        String date = releaseDate != null ? releaseDate : firstAirDate;
        if (date != null && date.length() >= 4) {
            return date.substring(0, 4);
        }
        return "N/A";
    }

    public String getRating() {
        return String.format("%.1f", voteAverage);
    }

    public String getMediaTypeLabel() {
        if ("tv".equals(mediaType)) return "TV";
        if ("movie".equals(mediaType)) return "Movie";
        return firstAirDate != null ? "TV" : "Movie";
    }

    public boolean isTv() {
        return "tv".equals(mediaType) || firstAirDate != null;
    }

    public boolean isAnime() {
        if (genreIds != null) {
            for (Integer genreId : genreIds) {
                // TMDB Animation Genre ID is 16
                if (genreId != null && genreId == 16) {
                    return true;
                }
            }
        }
        return false;
    }
}
