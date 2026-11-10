package com.udh.tiktokdownloader;

import android.app.Application;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public class DownloaderApplication extends Application {
    private static final String PREFERENCES = "appearance";
    private static final String THEME = "theme";
    private static final int[] MODES = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};

    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(MODES[getThemeChoice(this)]);
    }

    public static int getThemeChoice(Context context) {
        int choice = context.getSharedPreferences(PREFERENCES, MODE_PRIVATE).getInt(THEME, 0);
        return choice >= 0 && choice < MODES.length ? choice : 0;
    }

    public static void setThemeChoice(Context context, int choice) {
        if (choice < 0 || choice >= MODES.length) throw new IllegalArgumentException("Unknown appearance");
        context.getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putInt(THEME, choice).apply();
        AppCompatDelegate.setDefaultNightMode(MODES[choice]);
    }
}
