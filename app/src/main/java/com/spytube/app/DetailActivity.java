package com.spytube.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.spytube.app.api.ApiClient;
import com.spytube.app.api.TmdbService;
import com.spytube.app.models.MediaItem;
import com.spytube.app.models.TvDetails;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    private MediaItem media;
    private int selectedSeason = 1;
    private int selectedEpisode = 1;
    private TmdbService tmdbService;
    private LinearLayout episodesContainer;
    private android.widget.RadioGroup serverRadioGroup;
    private android.webkit.WebView trailerWebView;
    private boolean isMuted = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);

        // Inject the native XML layout into Jetpack Compose for real glass interception
        android.view.View nativeRoot = getLayoutInflater().inflate(R.layout.activity_detail, null);
        com.spytube.app.ui.components.DetailNavBar.setupOpticsGlassRoot(this, nativeRoot, () -> {
            startAppLogic();
            return kotlin.Unit.INSTANCE;
        });

        // Modern fullscreen immersive edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // Optional: Ensure light/dark icon contrast depending on your theme needs
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false); // White icons on dark imagery
        windowInsetsController.setAppearanceLightNavigationBars(false);
    }

    public void startAppLogic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                media = getIntent().getSerializableExtra("media", MediaItem.class);
            } else {
                media = (MediaItem) getIntent().getSerializableExtra("media");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading content", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (media == null) {
            Toast.makeText(this, "Content not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tmdbService = ApiClient.getTmdbService();
        initViews();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView backdrop = findViewById(R.id.backdrop);
        TextView title = findViewById(R.id.title);
        TextView meta = findViewById(R.id.meta);
        TextView overview = findViewById(R.id.overview);
        Button btnPlay = findViewById(R.id.btn_play);
        LinearLayout seasonContainer = findViewById(R.id.season_container);
        episodesContainer = findViewById(R.id.episodes_container);

        title.setText(media.getDisplayTitle());

        // Build premium meta line with colored rating
        String rating = media.getRating();
        String year = media.getYear();
        String type = media.getMediaTypeLabel();
        String metaStr = "";
        if (rating != null && !rating.isEmpty()) metaStr += "★ " + rating;
        if (year != null && !year.isEmpty()) metaStr += "   " + year;
        if (type != null && !type.isEmpty()) metaStr += "   " + type;

        android.text.SpannableString metaSpan = new android.text.SpannableString(metaStr);
        // Color the rating part green
        int ratingEnd = metaStr.indexOf("   ");
        if (ratingEnd > 0) {
            double ratingVal = 0;
            try { ratingVal = Double.parseDouble(rating); } catch (Exception ignored) {}
            int ratingColor = ratingVal >= 7.0 ? 0xFF46D369 : ratingVal >= 5.0 ? 0xFFFFB84D : 0xFFFF6B6B;
            metaSpan.setSpan(new android.text.style.ForegroundColorSpan(ratingColor),
                    0, ratingEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        meta.setText(metaSpan);
        overview.setText(media.overview != null ? media.overview : "No description available.");

        String backdropUrl = media.backdropPath != null
                ? "https://image.tmdb.org/t/p/original" + media.backdropPath : null;
        if (backdropUrl != null) {
            Glide.with(this).load(backdropUrl).into(backdrop);
        }
        LinearLayout genreChipsContainer = findViewById(R.id.genre_chips_container);
        if (media.genreIds != null && !media.genreIds.isEmpty()) {
            java.util.Map<Integer, String> genreMap = new java.util.HashMap<>();
            genreMap.put(28, "Action"); genreMap.put(12, "Adventure"); genreMap.put(16, "Animation");
            genreMap.put(35, "Comedy"); genreMap.put(80, "Crime"); genreMap.put(99, "Documentary");
            genreMap.put(18, "Drama"); genreMap.put(10751, "Family"); genreMap.put(14, "Fantasy");
            genreMap.put(36, "History"); genreMap.put(27, "Horror"); genreMap.put(10402, "Music");
            genreMap.put(9648, "Mystery"); genreMap.put(10749, "Romance"); genreMap.put(878, "Sci-Fi");
            genreMap.put(10770, "TV Movie"); genreMap.put(53, "Thriller"); genreMap.put(10752, "War");
            genreMap.put(37, "Western"); genreMap.put(10759, "Action"); genreMap.put(10762, "Kids");
            genreMap.put(10763, "News"); genreMap.put(10764, "Reality"); genreMap.put(10765, "Sci-Fi");
            genreMap.put(10766, "Soap"); genreMap.put(10767, "Talk"); genreMap.put(10768, "Politics");

            for (Integer genreId : media.genreIds) {
                String name = genreMap.get(genreId);
                if (name != null) {
                    TextView chip = new TextView(this);
                    chip.setText(name);
                    chip.setTextColor(getResources().getColor(R.color.text_secondary));
                    chip.setTextSize(12);
                    chip.setBackgroundResource(R.drawable.genre_chip_bg);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMarginEnd(8);
                    chip.setLayoutParams(lp);
                    genreChipsContainer.addView(chip);
                }
            }
        }

        serverRadioGroup = findViewById(R.id.server_radio_group);

        btnPlay.setOnClickListener(v -> playContent());
        ImageView btnDownload = findViewById(R.id.btn_download);
        btnDownload.setOnClickListener(v -> {
            if (media.isTv()) {
                Toast.makeText(this, "Please scroll down to download individual episodes.", Toast.LENGTH_LONG).show();
                return;
            }
            String searchTitle = media.title != null ? media.title : media.name;
            if (searchTitle == null || searchTitle.isEmpty()) {
                Toast.makeText(this, "No title to search", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if already downloaded
            java.util.Map<String, String> downloadMeta = com.spytube.app.models.HiCineDownloadManager.INSTANCE.getCompletedDownloadMeta(this, searchTitle);
            if (downloadMeta != null && downloadMeta.containsKey("downloadId")) {
                new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                    .setTitle("Delete Download")
                    .setMessage("Remove " + searchTitle + " from device?")
                    .setPositiveButton("Delete", (d, w) -> {
                        com.spytube.app.models.HiCineDownloadManager.INSTANCE.removeDownloadAndFile(this, Long.parseLong(downloadMeta.get("downloadId")));
                        Toast.makeText(this, "Download removed", Toast.LENGTH_SHORT).show();
                        refreshDownloadStateUI();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return;
            }

            Toast.makeText(this, "Searching downloads...", Toast.LENGTH_SHORT).show();
            String posterUrl = (media.posterPath != null) ? "https://image.tmdb.org/t/p/w500" + media.posterPath : null;
            searchAndDownload(searchTitle, String.valueOf(media.id), false, null, null, posterUrl);
        });

        loadCredits();
        loadTitleLogo();
        loadSimilar();
        loadTrailer();

        if (media.isTv()) {
            seasonContainer.setVisibility(View.VISIBLE);
            loadSeasons();
        } else {
            seasonContainer.setVisibility(View.GONE);
        }
    }

    private void refreshDownloadStateUI() {
        ImageView btnDownload = findViewById(R.id.btn_download);
        if (btnDownload == null || media == null) return;
        
        String searchTitle = media.title != null ? media.title : media.name;
        if (!media.isTv() && searchTitle != null) {
            java.util.Map<String, String> downloadMeta = com.spytube.app.models.HiCineDownloadManager.INSTANCE.getCompletedDownloadMeta(this, searchTitle);
            if (downloadMeta != null) {
                btnDownload.setImageResource(R.drawable.ic_delete);
                btnDownload.setColorFilter(0xFFFFFFFF); // white delete icon
                btnDownload.setBackground(getResources().getDrawable(R.drawable.btn_circle_glass, getTheme()));
            } else {
                btnDownload.setImageResource(R.drawable.ic_download_arrow);
                btnDownload.setColorFilter(null);
                btnDownload.setBackground(getResources().getDrawable(R.drawable.btn_circle_glass, getTheme()));
            }
        }
    }

    private void loadTitleLogo() {
        String type = media.isTv() ? "tv" : "movie";
        tmdbService.getMediaImages(type, media.id, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.ImagesResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.ImagesResponse> call,
                                           Response<com.spytube.app.models.ImagesResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().logos != null && !response.body().logos.isEmpty()) {
                            // Prefer English logo
                            String logoPath = null;
                            for (com.spytube.app.models.ImageItem logo : response.body().logos) {
                                if ("en".equals(logo.language)) {
                                    logoPath = logo.filePath;
                                    break;
                                }
                            }
                            if (logoPath == null) {
                                logoPath = response.body().logos.get(0).filePath;
                            }
                            ImageView titleLogo = findViewById(R.id.title_logo);
                            Glide.with(DetailActivity.this)
                                    .load("https://image.tmdb.org/t/p/w500" + logoPath)
                                    .into(titleLogo);
                            titleLogo.setVisibility(View.VISIBLE);
                            titleLogo.setAlpha(0f);
                            titleLogo.animate().alpha(1f).setDuration(400).start();
                            // Hide text title when logo is shown
                            TextView titleText = findViewById(R.id.title);
                            if (titleText != null) titleText.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<com.spytube.app.models.ImagesResponse> call, Throwable t) {}
                });
    }

    private void loadSimilar() {
        String type = media.isTv() ? "tv" : "movie";
        tmdbService.getSimilar(type, media.id, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.TmdbResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.TmdbResponse> call,
                                           Response<com.spytube.app.models.TmdbResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().results != null && !response.body().results.isEmpty()) {
                            LinearLayout container = findViewById(R.id.similar_container);
                            container.setVisibility(View.VISIBLE);

                            androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.similar_recycler);
                            rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                                    DetailActivity.this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

                            java.util.List<com.spytube.app.models.MediaItem> items = new java.util.ArrayList<>();
                            for (com.spytube.app.models.MediaItem item : response.body().results) {
                                if (item.posterPath != null) {
                                    items.add(item);
                                }
                            }
                            rv.setAdapter(new SimilarAdapter(items));
                        }
                    }

                    @Override
                    public void onFailure(Call<com.spytube.app.models.TmdbResponse> call, Throwable t) {}
                });
    }

    private void loadTrailer() {
        String type = media.isTv() ? "tv" : "movie";
        android.util.Log.d("DetailTrailer", "Loading videos for " + type + "/" + media.id);
        tmdbService.getVideos(type, media.id, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.VideosResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.VideosResponse> call,
                                           Response<com.spytube.app.models.VideosResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().results == null || response.body().results.isEmpty()) {
                            android.util.Log.d("DetailTrailer", "Videos API failed or empty. Trying search by title fallback.");
                            searchAndLoadTrailer(type);
                            return;
                        }
                        android.util.Log.d("DetailTrailer", "Got " + response.body().results.size() + " videos");
                        parseTrailerAndPlay(response.body().results);
                    }

                    @Override
                    public void onFailure(Call<com.spytube.app.models.VideosResponse> call, Throwable t) {
                        android.util.Log.e("DetailTrailer", "Videos API error", t);
                        searchAndLoadTrailer(type);
                    }
                });
    }

    private void searchAndLoadTrailer(String originalType) {
        String query = media.title != null ? media.title : media.name;
        if (query == null || query.isEmpty()) return;

        android.util.Log.d("DetailTrailer", "Searching TMDB for title: " + query);
        tmdbService.searchMulti(query, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.TmdbResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.TmdbResponse> call, Response<com.spytube.app.models.TmdbResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                            int realId = response.body().results.get(0).id;
                            String realType = response.body().results.get(0).mediaType;
                            if (realType == null) realType = originalType;

                            android.util.Log.d("DetailTrailer", "Found real TMDB ID: " + realType + "/" + realId);
                            // Now fetch videos for the real TMDB ID
                            tmdbService.getVideos(realType, realId, ApiClient.getApiKey(DetailActivity.this))
                                    .enqueue(new Callback<com.spytube.app.models.VideosResponse>() {
                                        @Override
                                        public void onResponse(Call<com.spytube.app.models.VideosResponse> call, Response<com.spytube.app.models.VideosResponse> response) {
                                            if (response.isSuccessful() && response.body() != null && response.body().results != null) {
                                                parseTrailerAndPlay(response.body().results);
                                            }
                                        }
                                        @Override public void onFailure(Call<com.spytube.app.models.VideosResponse> call, Throwable t) {}
                                    });
                        } else {
                            android.util.Log.d("DetailTrailer", "Search fallback found no results for title: " + query);
                        }
                    }
                    @Override public void onFailure(Call<com.spytube.app.models.TmdbResponse> call, Throwable t) {}
                });
    }

    private void parseTrailerAndPlay(java.util.List<com.spytube.app.models.VideosResponse.Video> results) {
        String trailerKey = null;
        for (com.spytube.app.models.VideosResponse.Video v : results) {
            android.util.Log.d("DetailTrailer", "Video: " + v.name + " type=" + v.type + " site=" + v.site + " key=" + v.key);
            if ("YouTube".equals(v.site) && "Trailer".equals(v.type)) {
                if (v.official) { trailerKey = v.key; break; }
                if (trailerKey == null) trailerKey = v.key;
            }
        }
        // If no trailer, try Teaser
        if (trailerKey == null) {
            for (com.spytube.app.models.VideosResponse.Video v : results) {
                if ("YouTube".equals(v.site) && "Teaser".equals(v.type)) {
                    trailerKey = v.key;
                    break;
                }
            }
        }
        if (trailerKey == null) {
            android.util.Log.d("DetailTrailer", "No trailer found");
            return;
        }
        android.util.Log.d("DetailTrailer", "Playing trailer: " + trailerKey);
        playTrailerInWebView(trailerKey);
    }

    @android.annotation.SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void playTrailerInWebView(String youtubeKey) {
        trailerWebView = findViewById(R.id.trailer_webview);
        ImageView btnMute = findViewById(R.id.btn_mute);
        ImageView backdrop = findViewById(R.id.backdrop);
        if (trailerWebView == null) return;

        android.util.Log.d("DetailTrailer", "Setting up WebView for key: " + youtubeKey);

        // Critical: Desktop user agent allows autoplay (YouTube blocks mobile UA autoplay)
        android.webkit.WebSettings ws = trailerWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setDomStorageEnabled(true);
        ws.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        trailerWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // JavaScript interface for callbacks from the player
        trailerWebView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onPlayerReady() {
                android.util.Log.d("DetailTrailer", "YouTube player ready!");
                runOnUiThread(() -> {
                    trailerWebView.setAlpha(0f);
                    trailerWebView.setVisibility(View.VISIBLE);
                    trailerWebView.animate().alpha(1f).setDuration(1500).start();
                    if (backdrop != null) {
                        backdrop.animate().alpha(0f).setDuration(1500).start();
                    }

                    btnMute.setVisibility(View.VISIBLE);
                    btnMute.setAlpha(0f);
                    btnMute.animate().alpha(1f).setStartDelay(500).setDuration(500).start();
                });
            }

            @android.webkit.JavascriptInterface
            public void onPlayerError(int code) {
                android.util.Log.e("DetailTrailer", "YouTube player error: " + code);
                // Show backdrop as fallback on error
                runOnUiThread(() -> {
                    if (backdrop != null) backdrop.setAlpha(1f);
                    trailerWebView.setVisibility(View.GONE);
                });
            }
        }, "Android");

        trailerWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage msg) {
                android.util.Log.d("DetailTrailer", "JS: " + msg.message());
                return true;
            }
        });

        // Load HTML from assets, then inject via loadDataWithBaseURL so origin matches youtube.com
        try {
            java.io.InputStream is = getAssets().open("trailer_player.html");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String html = new String(buffer);
            // Inject the video ID directly into the HTML before loading
            html = html.replace("var videoId = '';", "var videoId = '" + youtubeKey + "';");
            trailerWebView.loadDataWithBaseURL("https://www.example.com", html, "text/html", "UTF-8", null);
            android.util.Log.d("DetailTrailer", "Loaded trailer HTML with baseURL youtube.com");
        } catch (Exception e) {
            android.util.Log.e("DetailTrailer", "Failed to load asset", e);
        }

        // Mute toggle
        btnMute.setOnClickListener(v -> {
            trailerWebView.evaluateJavascript("toggleMute()", result -> {
                if (result != null) {
                    isMuted = "true".equals(result.replace("\"", ""));
                    btnMute.setImageResource(isMuted
                            ? android.R.drawable.ic_lock_silent_mode
                            : android.R.drawable.ic_lock_silent_mode_off);
                }
            });
        });
    }

    private void loadCredits() {
        String type = media.isTv() ? "tv" : "movie";
        tmdbService.getCredits(type, media.id, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.CreditsResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.CreditsResponse> call, Response<com.spytube.app.models.CreditsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            displayCast(response.body().cast);
                        }
                    }

                    @Override
                    public void onFailure(Call<com.spytube.app.models.CreditsResponse> call, Throwable t) {
                        // Ignore error
                    }
                });
    }

    private void displayCast(java.util.List<com.spytube.app.models.CreditsResponse.CastMember> cast) {
        androidx.recyclerview.widget.RecyclerView castRecycler = findViewById(R.id.cast_recycler);
        if (castRecycler == null || cast == null || cast.isEmpty()) return;

        castRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        castRecycler.setAdapter(new com.spytube.app.adapters.CastAdapter(this, cast));
    }

    private void loadSeasons() {
        // Fix: correctly passing tvId first then apiKey
        tmdbService.getTvDetails(media.id, ApiClient.getApiKey(this)).enqueue(new Callback<TvDetails>() {
            @Override
            public void onResponse(Call<TvDetails> call, Response<TvDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displaySeasons(response.body());
                }
            }

            @Override
            public void onFailure(Call<TvDetails> call, Throwable t) {
                Toast.makeText(DetailActivity.this, "Failed to load seasons", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displaySeasons(TvDetails details) {
        if (details.seasons == null || details.seasons.isEmpty()) return;

        // Filter out specials (season 0)
        java.util.List<TvDetails.Season> validSeasons = new java.util.ArrayList<>();
        for (TvDetails.Season s : details.seasons) {
            if (s.seasonNumber > 0) validSeasons.add(s);
        }
        if (validSeasons.isEmpty()) return;

        TextView dropdownText = findViewById(R.id.season_dropdown_text);
        View dropdown = findViewById(R.id.season_dropdown);

        // Set initial season
        selectedSeason = validSeasons.get(0).seasonNumber;
        dropdownText.setText("Season " + selectedSeason);
        loadSeasonEpisodes(selectedSeason);

        // Season dropdown popup
        dropdown.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(
                    this, v, android.view.Gravity.END);
            for (TvDetails.Season s : validSeasons) {
                popup.getMenu().add(0, s.seasonNumber, s.seasonNumber,
                        s.name != null ? s.name : "Season " + s.seasonNumber);
            }
            popup.setOnMenuItemClickListener(item -> {
                int sn = item.getItemId();
                selectedSeason = sn;
                dropdownText.setText(item.getTitle());
                loadSeasonEpisodes(sn);
                return true;
            });

            // Style the popup with dark theme
            try {
                java.lang.reflect.Field mPopup = popup.getClass().getDeclaredField("mPopup");
                mPopup.setAccessible(true);
                Object menuPopupHelper = mPopup.get(popup);
                menuPopupHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class)
                        .invoke(menuPopupHelper, false);
            } catch (Exception ignored) {}

            popup.show();
        });
    }

    private void loadSeasonEpisodes(int seasonNumber) {
        if (episodesContainer == null) return;
        episodesContainer.removeAllViews();

        // Show loading
        TextView loading = new TextView(this);
        loading.setText("Loading episodes...");
        loading.setTextColor(getResources().getColor(R.color.text_secondary, null));
        loading.setTextSize(14);
        loading.setPadding(0, 24, 0, 24);
        episodesContainer.addView(loading);

        tmdbService.getSeasonDetail(media.id, seasonNumber, ApiClient.getApiKey(this))
                .enqueue(new Callback<com.spytube.app.models.SeasonDetailResponse>() {
                    @Override
                    public void onResponse(Call<com.spytube.app.models.SeasonDetailResponse> call,
                                           Response<com.spytube.app.models.SeasonDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().episodes != null) {
                            displayEpisodesList(response.body().episodes);
                        } else {
                            // Fallback to simple numbered episodes
                            displayEpisodesFallback(10);
                        }
                    }

                    @Override
                    public void onFailure(Call<com.spytube.app.models.SeasonDetailResponse> call, Throwable t) {
                        displayEpisodesFallback(10);
                    }
                });
    }

    private void displayEpisodesList(java.util.List<com.spytube.app.models.SeasonDetailResponse.Episode> episodes) {
        if (episodesContainer == null) return;
        episodesContainer.removeAllViews();

        int dp4 = dpToPx(4);
        int dp8 = dpToPx(8);
        int dp10 = dpToPx(10);
        int dp12 = dpToPx(12);
        int dp14 = dpToPx(14);

        for (com.spytube.app.models.SeasonDetailResponse.Episode ep : episodes) {
            // Episode row container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp10, 0, dp10);
            row.setClickable(true);
            row.setFocusable(true);

            // Ripple feedback on the entire row
            android.content.res.TypedArray ta = obtainStyledAttributes(
                    new int[]{android.R.attr.selectableItemBackground});
            row.setForeground(ta.getDrawable(0));
            ta.recycle();

            // Episode thumbnail
            ImageView thumb = new ImageView(this);
            LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(dpToPx(130), dpToPx(73));
            thumbLp.setMarginEnd(dp14);
            thumb.setLayoutParams(thumbLp);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setClipToOutline(true);
            thumb.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(8));
                }
            });

            if (ep.stillPath != null) {
                Glide.with(this)
                        .load("https://image.tmdb.org/t/p/w300" + ep.stillPath)
                        .placeholder(android.R.color.darker_gray)
                        .into(thumb);
            } else {
                thumb.setBackgroundColor(0xFF1E1E2E);
                // Show episode number on blank thumbnail
                thumb.setImageDrawable(null);
            }

            // Play icon overlay on thumbnail
            android.widget.FrameLayout thumbFrame = new android.widget.FrameLayout(this);
            thumbFrame.setLayoutParams(thumbLp);
            thumbFrame.addView(thumb, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            // Small play indicator
            ImageView playIcon = new ImageView(this);
            playIcon.setImageResource(android.R.drawable.ic_media_play);
            playIcon.setAlpha(0.7f);
            android.widget.FrameLayout.LayoutParams playLp = new android.widget.FrameLayout.LayoutParams(
                    dpToPx(24), dpToPx(24));
            playLp.gravity = android.view.Gravity.CENTER;
            thumbFrame.addView(playIcon, playLp);

            row.addView(thumbFrame);

            // Text column
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLp.weight = 1;
            textCol.setLayoutParams(textLp);

            // Episode label "E1 · title"
            TextView titleTv = new TextView(this);
            String epTitle = "E" + ep.episodeNumber;
            if (ep.name != null && !ep.name.isEmpty()) {
                epTitle += "  ·  " + ep.name;
            }
            titleTv.setText(epTitle);
            titleTv.setTextColor(getResources().getColor(R.color.text_primary, null));
            titleTv.setTextSize(14);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleTv.setMaxLines(1);
            titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(titleTv);

            // Runtime
            if (ep.runtime != null && ep.runtime > 0) {
                TextView runtime = new TextView(this);
                runtime.setText(ep.runtime + " min");
                runtime.setTextColor(getResources().getColor(R.color.text_secondary, null));
                runtime.setTextSize(12);
                LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rtLp.topMargin = dp4;
                runtime.setLayoutParams(rtLp);
                textCol.addView(runtime);
            }

            // Overview (2 lines max)
            if (ep.overview != null && !ep.overview.isEmpty()) {
                TextView desc = new TextView(this);
                desc.setText(ep.overview);
                desc.setTextColor(0xFF888899);
                desc.setTextSize(12);
                desc.setMaxLines(2);
                desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
                desc.setLineSpacing(0, 1.3f);
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                descLp.topMargin = dp4;
                desc.setLayoutParams(descLp);
                textCol.addView(desc);
            }

            row.addView(textCol);
            ImageView dlBtn = new ImageView(this);
            String searchTitle = media.title != null ? media.title : media.name;
            final int dlEpNum = ep.episodeNumber;
            final String targetEpTitle = "S" + selectedSeason + " E" + dlEpNum + " • " + searchTitle;
            
            java.util.Map<String, String> epMeta = com.spytube.app.models.HiCineDownloadManager.INSTANCE.getCompletedDownloadMeta(this, targetEpTitle);
            boolean isDownloaded = (epMeta != null);
            
            if (isDownloaded) {
                dlBtn.setImageResource(R.drawable.ic_delete);
                dlBtn.setColorFilter(0xFFFFFFFF); // white trash icon
                dlBtn.setAlpha(0.6f);
            } else {
                dlBtn.setImageResource(R.drawable.ic_download_arrow);
                dlBtn.setColorFilter(0xFFE50914); // red download icon
                dlBtn.setAlpha(0.85f);
            }
            
            dlBtn.setPadding(dp8, dp8, dp8, dp8);
            dlBtn.setBackground(getResources().getDrawable(R.drawable.btn_circle_glass, getTheme()));
            LinearLayout.LayoutParams dlLp = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
            dlLp.setMarginStart(dp8);
            dlBtn.setLayoutParams(dlLp);
            
            final String epStillPath = ep.stillPath;
            dlBtn.setOnClickListener(v -> {
                if (searchTitle == null) return;
                
                if (isDownloaded) {
                    new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                        .setTitle("Delete Download")
                        .setMessage("Remove Episode " + dlEpNum + " from device?")
                        .setPositiveButton("Delete", (d, w) -> {
                            com.spytube.app.models.HiCineDownloadManager.INSTANCE.removeDownloadAndFile(this, Long.parseLong(epMeta.get("downloadId")));
                            Toast.makeText(this, "Episode removed", Toast.LENGTH_SHORT).show();
                            loadSeasons(); // Refresh lists
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                    return;
                }

                Toast.makeText(this, "Finding download for S" + selectedSeason + "E" + dlEpNum + "...", Toast.LENGTH_SHORT).show();
                String epPoster = (epStillPath != null) ? "https://image.tmdb.org/t/p/w500" + epStillPath : 
                                  (media.posterPath != null) ? "https://image.tmdb.org/t/p/w500" + media.posterPath : null;
                searchAndDownload(searchTitle, String.valueOf(media.id), true, selectedSeason, dlEpNum, epPoster);
            });
            row.addView(dlBtn);

            // Click to play
            final int epNum = ep.episodeNumber;
            row.setOnClickListener(v -> {
                if (isDownloaded) {
                    String localUri = epMeta.get("localUri");
                    if (localUri != null) {
                        Intent intent = new Intent(this, CinefyPlayerActivity.class);
                        intent.putExtra("localUri", localUri);
                        intent.putExtra("mediaId", String.valueOf(media.id));
                        intent.putExtra("showTitle", searchTitle);
                        intent.putExtra("episodeTitle", "S" + selectedSeason + "E" + epNum);
                        intent.putExtra("currentSeason", selectedSeason);
                        intent.putExtra("currentEpisode", epNum);
                        intent.putExtra("isTv", true);
                        startActivity(intent);
                        return;
                    }
                }
                selectedEpisode = epNum;
                playContent();
            });

            episodesContainer.addView(row);

            // Divider
            View divider = new View(this);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
            divLp.topMargin = 0;
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(0x15FFFFFF);
            episodesContainer.addView(divider);
        }
    }

    /** Fallback when TMDB season detail fails — simple numbered list */
    private void displayEpisodesFallback(int episodeCount) {
        if (episodesContainer == null) return;
        episodesContainer.removeAllViews();

        for (int i = 1; i <= episodeCount; i++) {
            TextView item = new TextView(this);
            item.setText("Episode " + i);
            item.setTextColor(getResources().getColor(R.color.text_primary, null));
            item.setTextSize(15);
            item.setPadding(0, dpToPx(14), 0, dpToPx(14));

            final int ep = i;
            item.setOnClickListener(v -> {
                selectedEpisode = ep;
                playContent();
            });

            episodesContainer.addView(item);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(0x15FFFFFF);
            episodesContainer.addView(divider);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void playContent() {
        String searchTitle = media.title != null ? media.title : media.name;
        
        // 1. Check for local download override!
        if (searchTitle != null) {
            String checkTitle = searchTitle;
            if (media.isTv()) {
                checkTitle = "S" + selectedSeason + " E" + selectedEpisode + " • " + searchTitle;
            }
            java.util.Map<String, String> meta = com.spytube.app.models.HiCineDownloadManager.INSTANCE.getCompletedDownloadMeta(this, checkTitle);
            if (meta != null && meta.containsKey("localUri")) {
                Intent intent = new Intent(this, CinefyPlayerActivity.class);
                intent.putExtra("localUri", meta.get("localUri"));
                intent.putExtra("mediaId", String.valueOf(media.id));
                intent.putExtra("showTitle", searchTitle);
                intent.putExtra("title", searchTitle);
                if (media.isTv()) {
                    intent.putExtra("episodeTitle", "S" + selectedSeason + "E" + selectedEpisode);
                    intent.putExtra("currentSeason", selectedSeason);
                    intent.putExtra("currentEpisode", selectedEpisode);
                } else {
                    intent.putExtra("episodeTitle", "");
                }
                intent.putExtra("isTv", media.isTv());
                intent.putExtra("resumePosition", com.spytube.app.models.CinefyCache.getPosition(this, searchTitle));
                startActivity(intent);
                return;
            }
        }

        // 2. Fall back to stream playback
        com.spytube.app.models.WatchHistoryManager.INSTANCE.addToHistory(this, media);

        int serverIndex = 3; // Default to Cinefy (first pill)
        if (serverRadioGroup != null) {
            int checkedId = serverRadioGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.server_zxcstream) serverIndex = 0;
            else if (checkedId == R.id.server_vidrock) serverIndex = 1;
            else if (checkedId == R.id.server_vidlink) serverIndex = 2;
            else if (checkedId == R.id.server_cinefy) serverIndex = 3;
        }

        if (serverIndex == 3) {
            String title = media.title != null ? media.title : media.name;
            Intent intent = new Intent(this, CinefyPlayerActivity.class);
            intent.putExtra("searchTitle", title);
            intent.putExtra("isTv", media.isTv());
            intent.putExtra("season", selectedSeason);
            intent.putExtra("episode", selectedEpisode);
            intent.putExtra("resumePosition", com.spytube.app.models.CinefyCache.getPosition(this, title));
            intent.putExtra("media", media);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("media", media);
            intent.putExtra("season", selectedSeason);
            intent.putExtra("episode", selectedEpisode);
            intent.putExtra("server", serverIndex);
            startActivity(intent);
        }
    }

    private void searchCinefyAndPlay(String title) {
        String[] providers = {"netflix", "prime", "hotstar"};
        boolean isTv = media.isTv();
        int season = selectedSeason;
        int episode = selectedEpisode;
        String titleLower = title.toLowerCase().trim();

        new Thread(() -> {
            for (String provider : providers) {
                try {
                    String encodedTitle = java.net.URLEncoder.encode(title, "UTF-8");
                    String searchJson = httpGet("https://cinefy.lol/api/" + provider + "/search?q=" + encodedTitle);
                    if (searchJson == null) continue;

                    org.json.JSONObject searchObj = new org.json.JSONObject(searchJson);
                    if (!searchObj.optBoolean("success")) continue;
                    org.json.JSONArray data = searchObj.optJSONArray("data");
                    if (data == null || data.length() == 0) continue;

                    java.util.List<String> matchedIds = new java.util.ArrayList<>();
                    for (int r = 0; r < data.length(); r++) {
                        org.json.JSONObject item = data.getJSONObject(r);
                        String resultTitle = item.optString("title", "").toLowerCase().trim();
                        if (resultTitle.equals(titleLower)) {
                            matchedIds.add(item.getString("id"));
                        }
                    }
                    if (matchedIds.isEmpty()) continue;

                    for (String cinefyId : matchedIds) {
                        String loadJson = httpGet("https://cinefy.lol/api/" + provider + "/load/" + cinefyId);
                        if (loadJson == null) continue;

                        org.json.JSONObject loadObj = new org.json.JSONObject(loadJson);
                        if (!loadObj.optBoolean("success")) continue;
                        org.json.JSONObject detail = loadObj.getJSONObject("data");

                        boolean cinefyIsMovie = detail.optBoolean("isMovie", true);

                        if (isTv && cinefyIsMovie) continue;
                        if (!isTv && !cinefyIsMovie) continue;

                        if (!isTv) {
                            String fp = provider;
                            runOnUiThread(() -> launchCinefyPlayer(fp, cinefyId, title));
                            return;
                        }

                        org.json.JSONArray seasons = detail.optJSONArray("seasons");
                        String targetSeasonId = null;
                        if (seasons != null) {
                            for (int i = 0; i < seasons.length(); i++) {
                                org.json.JSONObject s = seasons.getJSONObject(i);
                                if (String.valueOf(season).equals(s.optString("label"))) {
                                    targetSeasonId = s.getString("id");
                                    break;
                                }
                            }
                        }

                        org.json.JSONArray episodes;
                        String defaultSeasonId = detail.optString("defaultSeasonId", "");

                        if (targetSeasonId != null && !targetSeasonId.equals(defaultSeasonId)) {
                            String epJson = httpGet("https://cinefy.lol/api/" + provider + "/episodes/" + cinefyId + "/" + targetSeasonId);
                            if (epJson == null) continue;
                            org.json.JSONObject epObj = new org.json.JSONObject(epJson);
                            episodes = epObj.optJSONArray("data");
                        } else {
                            episodes = detail.optJSONArray("episodes");
                        }

                        if (episodes == null || episodes.length() == 0) continue;

                        String episodeId = null;
                        for (int i = 0; i < episodes.length(); i++) {
                            org.json.JSONObject ep = episodes.getJSONObject(i);
                            String epNum = ep.optString("episode", "");
                            if (String.valueOf(episode).equals(epNum)) {
                                episodeId = ep.getString("id");
                                break;
                            }
                        }
                        if (episodeId == null && episode - 1 < episodes.length()) {
                            episodeId = episodes.getJSONObject(episode - 1).getString("id");
                        }
                        if (episodeId == null) continue;

                        String finalEpId = episodeId;
                        String fp = provider;
                        String fShowId = cinefyId;
                        String fSeasonId = targetSeasonId != null ? targetSeasonId : defaultSeasonId;
                        int fSeason = season;
                        int fEpisode = episode;
                        runOnUiThread(() -> launchCinefyPlayer(fp, finalEpId, title, fShowId, fSeasonId, fSeason, fEpisode));
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void launchCinefyPlayer(String provider, String mediaId, String title,
                                     String showId, String seasonId, int season, int episode) {
        Intent intent = new Intent(this, CinefyPlayerActivity.class);
        intent.putExtra("provider", provider);
        intent.putExtra("mediaId", mediaId);
        intent.putExtra("title", title);
        intent.putExtra("showId", showId != null ? showId : "");
        intent.putExtra("seasonId", seasonId != null ? seasonId : "");
        intent.putExtra("season", season);
        intent.putExtra("episode", episode);
        intent.putExtra("isTv", media.isTv());
        // Resume from last position
        float resumePos = com.spytube.app.models.CinefyCache.getPosition(this, title);
        intent.putExtra("resumePosition", resumePos);
        startActivity(intent);
    }

    private void launchCinefyPlayer(String provider, String mediaId, String title) {
        launchCinefyPlayer(provider, mediaId, title, "", "", 0, 0);
    }

    private String httpGet(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() == 200) {
                java.io.InputStream is = conn.getInputStream();
                String json = new java.util.Scanner(is).useDelimiter("\\A").next();
                is.close();
                conn.disconnect();
                return json;
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return null;
    }

    
    private void searchAndDownload(String searchTitle, String tmdbId, boolean isTv, Integer season, Integer episode, String posterUrl) {
        new Thread(() -> {
            try {
                java.util.List<com.spytube.app.models.HiCineDownloadLink> links =
                        com.spytube.app.models.HiCineDownloadManager.searchDownloadsBlocking(
                                searchTitle, tmdbId, isTv, season, episode
                        );

                if (links.isEmpty()) {
                    String errorStr = com.spytube.app.api.VidVaultClient.INSTANCE.getLastError();
                    runOnUiThread(() -> {
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Download Failed")
                            .setMessage("No links available for TMDB " + tmdbId + ".\nError: " + errorStr)
                            .setPositiveButton("OK", null)
                            .show();
                    });
                    return;
                }

                String finalName = searchTitle;
                if (isTv && season != null && episode != null) {
                    finalName = "S" + season + " E" + episode + " • " + searchTitle;
                }
                final String dialogTitle = finalName; // must be effectively final

                runOnUiThread(() -> showDownloadQualityPicker(dialogTitle, links, posterUrl));

            } catch (Exception e) {
                android.util.Log.e("Download", "Download search failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Download search failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    
    private void showDownloadQualityPicker(
            String title,
            java.util.List<com.spytube.app.models.HiCineDownloadLink> links,
            String posterUrl
    ) {
        String[] items = new String[links.size()];
        for (int i = 0; i < links.size(); i++) {
            com.spytube.app.models.HiCineDownloadLink link = links.get(i);
            items[i] = link.getQuality() + "  •  " + link.getSize();
        }

        new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
                .setTitle("⬇ Download: " + title)
                .setItems(items, (dialog, which) -> {
                    com.spytube.app.models.HiCineDownloadLink selected = links.get(which);
                    Toast.makeText(this, "Starting download: " + selected.getQuality(), Toast.LENGTH_SHORT).show();

                    // Run the 3-step download pipeline in background
                    new Thread(() -> {
                        try {
                            com.spytube.app.models.HiCineDownloadManager.INSTANCE
                                    .startDownloadBlocking(this, selected, title, posterUrl);
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    
    private void searchHicineEpisodeAndDownload(String searchTitle, int seasonNum, int episodeNum) {
        new Thread(() -> {
            try {
                java.util.List<com.spytube.app.models.HiCineItem> results =
                        com.spytube.app.api.HiCineClient.INSTANCE.getService()
                                .searchBlocking(java.net.URLEncoder.encode(searchTitle, "UTF-8"))
                                .execute().body();

                if (results == null || results.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No downloads found", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Find best match
                String titleLower = searchTitle.toLowerCase().trim();
                com.spytube.app.models.HiCineItem match = null;
                for (com.spytube.app.models.HiCineItem item : results) {
                    if (item.getTitle().toLowerCase().trim().contains(titleLower) && item.isSeries()) {
                        match = item;
                        break;
                    }
                }
                if (match == null) match = results.get(0);

                // Get season data and parse episode links
                String seasonData = match.getSeasonData(seasonNum);
                java.util.List<com.spytube.app.models.HiCineDownloadLink> allLinks =
                        com.spytube.app.models.HiCineDownloadManager.INSTANCE.parseSeriesSeasonLinks(seasonData);

                // Filter to requested episode
                java.util.List<com.spytube.app.models.HiCineDownloadLink> epLinks = new java.util.ArrayList<>();
                for (com.spytube.app.models.HiCineDownloadLink link : allLinks) {
                    if (link.getEpisodeNumber() != null && link.getEpisodeNumber() == episodeNum) {
                        epLinks.add(link);
                    }
                }

                if (epLinks.isEmpty()) {
                    // Fallback: try movie-style links
                    epLinks = com.spytube.app.models.HiCineDownloadManager.INSTANCE.parseMovieLinks(match.getLinks());
                }

                if (epLinks.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No download for S" + seasonNum + "E" + episodeNum, Toast.LENGTH_SHORT).show());
                    return;
                }

                java.util.List<com.spytube.app.models.HiCineDownloadLink> finalLinks = epLinks;
                String posterUrl = (media != null) ? "https://image.tmdb.org/t/p/w500" + media.posterPath : null;
                runOnUiThread(() -> showDownloadQualityPicker(
                        searchTitle + " S" + seasonNum + "E" + episodeNum,
                        finalLinks,
                        posterUrl
                ));

            } catch (Exception e) {
                android.util.Log.e("Download", "Episode search failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Download search failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private class SimilarAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<SimilarAdapter.VH> {
        private final java.util.List<com.spytube.app.models.MediaItem> items;

        SimilarAdapter(java.util.List<com.spytube.app.models.MediaItem> items) {
            this.items = items;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            int w = (int) (110 * parent.getContext().getResources().getDisplayMetrics().density);
            int h = (int) (165 * parent.getContext().getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new android.view.ViewGroup.MarginLayoutParams(w, h));
            ((android.view.ViewGroup.MarginLayoutParams) iv.getLayoutParams()).setMarginEnd(
                    (int) (8 * parent.getContext().getResources().getDisplayMetrics().density));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setClipToOutline(true);
            iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                            12 * view.getContext().getResources().getDisplayMetrics().density);
                }
            });
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            com.spytube.app.models.MediaItem item = items.get(position);
            Glide.with(holder.iv.getContext())
                    .load("https://image.tmdb.org/t/p/w342" + item.posterPath)
                    .into(holder.iv);
            holder.iv.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(DetailActivity.this, DetailActivity.class);
                intent.putExtra("media", item);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView iv;
            VH(ImageView iv) { super(iv); this.iv = iv; }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (trailerWebView != null) {
            trailerWebView.evaluateJavascript("pauseVideo()", null);
            trailerWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (media != null) {
            refreshDownloadStateUI();
        }
        if (trailerWebView != null) {
            trailerWebView.onResume();
            trailerWebView.evaluateJavascript("playVideo()", null);
        }
    }

    @Override
    protected void onDestroy() {
        if (trailerWebView != null) {
            trailerWebView.evaluateJavascript("pauseVideo()", null);
            trailerWebView.stopLoading();
            trailerWebView.loadUrl("about:blank");
            trailerWebView.destroy();
            trailerWebView = null;
        }
        super.onDestroy();
    }
}
