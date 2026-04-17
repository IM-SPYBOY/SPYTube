package com.spytube.app.api;

import com.spytube.app.models.CreditsResponse;
import com.spytube.app.models.ImagesResponse;
import com.spytube.app.models.TmdbResponse;
import com.spytube.app.models.TvDetails;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbService {

    @GET("trending/all/day")
    Call<TmdbResponse> getTrending(@Query("api_key") String apiKey);

    @GET("trending/all/week")
    Call<TmdbResponse> getTrendingWeek(@Query("api_key") String apiKey);

    @GET("movie/popular")
    Call<TmdbResponse> getPopularMovies(@Query("api_key") String apiKey);

    @GET("movie/popular")
    Call<TmdbResponse> getPopularMoviesPage(@Query("api_key") String apiKey, @Query("page") int page);

    @GET("movie/top_rated")
    Call<TmdbResponse> getTopRatedMovies(@Query("api_key") String apiKey);

    @GET("movie/now_playing")
    Call<TmdbResponse> getNowPlayingMovies(@Query("api_key") String apiKey);

    @GET("movie/upcoming")
    Call<TmdbResponse> getUpcomingMovies(@Query("api_key") String apiKey);

    @GET("tv/popular")
    Call<TmdbResponse> getPopularTv(@Query("api_key") String apiKey);

    @GET("tv/popular")
    Call<TmdbResponse> getPopularTvPage(@Query("api_key") String apiKey, @Query("page") int page);

    @GET("tv/top_rated")
    Call<TmdbResponse> getTopRatedTv(@Query("api_key") String apiKey);

    @GET("tv/on_the_air")
    Call<TmdbResponse> getOnTheAirTv(@Query("api_key") String apiKey);

    @GET("tv/airing_today")
    Call<TmdbResponse> getAiringTodayTv(@Query("api_key") String apiKey);

    @GET("search/multi")
    Call<TmdbResponse> searchMulti(
            @Query("api_key") String apiKey,
            @Query("query") String query
    );

    @GET("discover/movie")
    Call<TmdbResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genres,
            @Query("sort_by") String sortBy
    );

    @GET("discover/tv")
    Call<TmdbResponse> discoverTv(
            @Query("api_key") String apiKey,
            @Query("with_genres") String genres,
            @Query("sort_by") String sortBy
    );

    @GET("tv/{tv_id}")
    Call<TvDetails> getTvDetails(
            @Path("tv_id") int tvId,
            @Query("api_key") String apiKey
    );

    @GET("tv/{tv_id}/season/{season_number}")
    Call<com.spytube.app.models.SeasonDetailResponse> getSeasonDetail(
            @Path("tv_id") int tvId,
            @Path("season_number") int seasonNumber,
            @Query("api_key") String apiKey
    );

    @GET("{type}/{id}/credits")
    Call<CreditsResponse> getCredits(
            @Path("type") String type,
            @Path("id") int id,
            @Query("api_key") String apiKey
    );

    @GET("{type}/{id}/images")
    Call<ImagesResponse> getMediaImages(
            @Path("type") String type,
            @Path("id") int id,
            @Query("api_key") String apiKey
    );

    @GET("{type}/{id}/similar")
    Call<TmdbResponse> getSimilar(
            @Path("type") String type,
            @Path("id") int id,
            @Query("api_key") String apiKey
    );

    @GET("{type}/{id}/videos")
    Call<com.spytube.app.models.VideosResponse> getVideos(
            @Path("type") String type,
            @Path("id") int id,
            @Query("api_key") String apiKey
    );
}
