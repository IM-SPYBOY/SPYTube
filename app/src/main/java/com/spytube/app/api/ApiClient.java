package com.spytube.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class ApiClient {
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3/";
    private static final String PREFS_NAME = "SPYTubePrefs";
    private static final String KEY_API_KEY = "tmdb_api_key";
    
    private static final String DEFAULT_API_KEY = "5a4235e50596bd715288dd2ccecc5082";
    private static Retrofit tmdbRetrofit = null;
    private static String cachedApiKey = null;
    
    public static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/";

    public static void saveApiKey(Context context, String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
        cachedApiKey = apiKey;
        tmdbRetrofit = null;
    }

    public static String getApiKey(Context context) {
        if (cachedApiKey != null && !cachedApiKey.isEmpty()) {
            return cachedApiKey;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cachedApiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY);
        return cachedApiKey;
    }

    public static boolean hasApiKey(Context context) {
        return true;
    }

    
    private static Dns buildDoh() {
        try {
            OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            return new DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(HttpUrl.get("https://1.1.1.1/dns-query"))
                    .bootstrapDnsHosts(
                            InetAddress.getByName("1.1.1.1"),
                            InetAddress.getByName("1.0.0.1"),
                            InetAddress.getByName("8.8.8.8")
                    )
                    .build();
        } catch (UnknownHostException e) {
            return Dns.SYSTEM;
        }
    }

    public static Retrofit getTmdbClient() {
        if (tmdbRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = DohTunnel.INSTANCE.getClient().newBuilder()
                    .addInterceptor(new RetryInterceptor(3))
                    .addInterceptor(logging)
                    .build();

            tmdbRetrofit = new Retrofit.Builder()
                    .baseUrl(TMDB_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return tmdbRetrofit;
    }

    public static TmdbService getTmdbService() {
        return getTmdbClient().create(TmdbService.class);
    }

    public static String getImageUrl(String path, String size) {
        if (path == null || path.isEmpty()) return null;
        return TMDB_IMAGE_BASE + size + path;
    }

    public static String getPosterUrl(String path) {
        return getImageUrl(path, "w500");
    }

    public static String getBackdropUrl(String path) {
        return getImageUrl(path, "w1280");
    }

    public static String getProfileUrl(String path) {
        return getImageUrl(path, "w185");
    }

    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    if (response.isSuccessful() || attempt >= maxRetries) {
                        return response;
                    }
                    if (response.code() >= 500) {
                        response.close();
                        try { Thread.sleep(500L * (attempt + 1)); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    return response;
                } catch (IOException e) {
                    lastException = e;
                    if (attempt < maxRetries) {
                        try { Thread.sleep(500L * (attempt + 1)); } catch (InterruptedException ignored) {}
                    }
                }
            }
            throw lastException != null ? lastException : new IOException("Request failed after retries");
        }
    }
}
