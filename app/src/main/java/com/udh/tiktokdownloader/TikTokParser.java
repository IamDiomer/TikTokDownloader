package com.udh.tiktokdownloader;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TikTokParser {

    public interface OnVideoInfoListener {
        void onSuccess(TikTokVideoInfo videoInfo);
        void onError(String errorMessage);
    }

    private static final String TAG = "TikTokParser";
    private static final String TIKWM_API_URL = "https://www.tikwm.com/api/?url=";
    private static final String TIKWM_BASE_DOMAIN = "https://www.tikwm.com";

    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public TikTokParser() {
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static String extractUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public void fetchVideoInfo(String rawUrl, OnVideoInfoListener listener) {
        String cleanUrl = extractUrl(rawUrl);
        if (cleanUrl == null) {
            listener.onError("URL de TikTok inválida");
            return;
        }

        executor.execute(() -> {
            try {
                TikTokVideoInfo videoInfo = parseWithTikWm(cleanUrl);
                if (videoInfo != null) {
                    mainHandler.post(() -> listener.onSuccess(videoInfo));
                } else {
                    mainHandler.post(() -> listener.onError("No se pudo obtener información del video"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching video info", e);
                mainHandler.post(() -> listener.onError("Error al conectar: " + e.getLocalizedMessage()));
            }
        });
    }

    private TikTokVideoInfo parseWithTikWm(String targetUrl) throws IOException {
        String requestUrl = TIKWM_API_URL + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8.name());

        Request request = new Request.Builder()
                .url(requestUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            String jsonString = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonString);

            int code = jsonObject.optInt("code", -1);
            if (code != 0 || !jsonObject.has("data")) {
                return null;
            }

            JSONObject data = jsonObject.getJSONObject("data");
            TikTokVideoInfo info = new TikTokVideoInfo();

            info.setId(data.optString("id", String.valueOf(System.currentTimeMillis())));
            info.setTitle(data.optString("title", "TikTok Video"));
            info.setCoverUrl(fixUrl(data.optString("cover", "")));
            info.setVideoNoWmUrl(fixUrl(data.optString("play", "")));
            info.setVideoWmUrl(fixUrl(data.optString("wmplay", "")));
            info.setAudioUrl(fixUrl(data.optString("music", "")));

            info.setLikesCount(data.optLong("digg_count", 0));
            info.setCommentsCount(data.optLong("comment_count", 0));
            info.setSharesCount(data.optLong("share_count", 0));
            info.setViewsCount(data.optLong("play_count", 0));

            if (data.has("author") && !data.isNull("author")) {
                JSONObject author = data.getJSONObject("author");
                info.setAuthorNickname(author.optString("nickname", "Usuario TikTok"));
                info.setAuthorUniqueId(author.optString("unique_id", ""));
                info.setAuthorAvatar(fixUrl(author.optString("avatar", "")));
            } else {
                info.setAuthorNickname("Usuario TikTok");
                info.setAuthorUniqueId("");
                info.setAuthorAvatar("");
            }

            if (info.getVideoNoWmUrl().isEmpty() && info.getVideoWmUrl().isEmpty()) {
                return null;
            }
            return info;
        } catch (Exception e) {
            Log.e(TAG, "Parse exception", e);
            return null;
        }
    }

    private String fixUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("/")) {
            return TIKWM_BASE_DOMAIN + url;
        }
        return url;
    }

    public void close() {
        client.dispatcher().cancelAll();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
