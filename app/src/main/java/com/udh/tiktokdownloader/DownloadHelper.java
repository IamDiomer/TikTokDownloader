package com.udh.tiktokdownloader;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.util.Locale;

public class DownloadHelper {

    public static final int TYPE_VIDEO_NO_WM = 1;
    public static final int TYPE_VIDEO_WM = 2;
    public static final int TYPE_AUDIO = 3;

    public static long downloadFile(Context context, String url, String videoId, String title, int downloadType) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(context, "URL de descarga no disponible", Toast.LENGTH_SHORT).show();
            return -1;
        }

        try {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(context, "Servicio de descarga no disponible", Toast.LENGTH_SHORT).show();
                return -1;
            }

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            String safeId = (videoId != null && !videoId.isEmpty()) ? videoId : String.valueOf(System.currentTimeMillis());
            String fileName;
            String mimeType;
            String downloadTitle;

            if (downloadType == TYPE_AUDIO) {
                String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
                String extension = ".mp3";
                mimeType = "audio/mpeg";
                if (path.endsWith(".m4a") || path.endsWith(".mp4")) { extension = ".m4a"; mimeType = "audio/mp4"; }
                else if (path.endsWith(".aac")) { extension = ".aac"; mimeType = "audio/aac"; }
                else if (path.endsWith(".ogg")) { extension = ".ogg"; mimeType = "audio/ogg"; }
                else if (path.endsWith(".wav")) { extension = ".wav"; mimeType = "audio/wav"; }
                fileName = "TikTok_Audio_" + safeId + extension;
                downloadTitle = "TikTok Audio - " + (title != null ? title : safeId);
            } else if (downloadType == TYPE_VIDEO_WM) {
                fileName = "TikTok_WM_" + safeId + ".mp4";
                mimeType = "video/mp4";
                downloadTitle = "TikTok Video (WM) - " + (title != null ? title : safeId);
            } else {
                fileName = "TikTok_NoWM_" + safeId + ".mp4";
                mimeType = "video/mp4";
                downloadTitle = "TikTok Video - " + (title != null ? title : safeId);
            }

            request.setTitle(downloadTitle);
            request.setDescription("Descargando archivo...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setMimeType(mimeType);

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TikTok_Downloads/" + fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

            long downloadId = downloadManager.enqueue(request);
            Toast.makeText(context, "Descarga iniciada: " + fileName, Toast.LENGTH_LONG).show();
            return downloadId;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al iniciar descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    public static String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format(Locale.getDefault(), "%.1fK", number / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", number / 1000000.0);
    }
}
