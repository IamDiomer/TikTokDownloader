package com.udh.tiktokdownloader;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppearanceTest {
    @Test public void themeSwitchPersistsAndKeepsTheEnteredLink() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int original = DownloaderApplication.getThemeChoice(app);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.etUrl)).perform(replaceText("https://www.tiktok.com/@example/video/123"));
            choose(R.string.theme_light);
            onView(withId(R.id.etUrl)).check(matches(withText("https://www.tiktok.com/@example/video/123")));
            scenario.onActivity(activity -> {
                assertEquals(Configuration.UI_MODE_NIGHT_NO, activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
                assertEquals(1, DownloaderApplication.getThemeChoice(activity));
            });
            capture("light.png");
            choose(R.string.theme_dark);
            onView(withId(R.id.etUrl)).check(matches(withText("https://www.tiktok.com/@example/video/123")));
            scenario.onActivity(activity -> {
                assertEquals(Configuration.UI_MODE_NIGHT_YES, activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
                assertEquals(2, DownloaderApplication.getThemeChoice(activity));
            });
            capture("dark.png");
            scenario.recreate();
            onView(withId(R.id.btnTheme)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                assertEquals(Configuration.UI_MODE_NIGHT_YES, activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
                Drawable icon = activity.getDrawable(R.mipmap.ic_launcher);
                assertNotNull(icon);
                Bitmap bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                icon.setBounds(0, 0, 192, 192);
                icon.draw(new Canvas(bitmap));
                save(activity, bitmap, "icon.png");
                bitmap.recycle();
            });
            choose(R.string.theme_system);
            assertEquals(0, DownloaderApplication.getThemeChoice(app));
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> DownloaderApplication.setThemeChoice(app, original));
        }
    }

    private void choose(int label) {
        onView(withId(R.id.btnTheme)).perform(click());
        onView(withText(label)).perform(click());
        onView(withId(R.id.btnTheme)).check(matches(isDisplayed()));
    }

    private void capture(String name) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        save(InstrumentationRegistry.getInstrumentation().getTargetContext(), bitmap, name);
        bitmap.recycle();
    }

    private void save(Context context, Bitmap bitmap, String name) {
        File directory = new File(context.getExternalFilesDir(null), "appearance-previews");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } catch (Exception e) { throw new AssertionError(e); }
    }
}
