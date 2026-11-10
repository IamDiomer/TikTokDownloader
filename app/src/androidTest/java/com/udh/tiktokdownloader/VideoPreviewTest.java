package com.udh.tiktokdownloader;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@RunWith(AndroidJUnit4.class)
public class VideoPreviewTest {
    @Test public void headerStaysFixedAndVideoPlaysAndRestoresPausedPosition() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File video = new File(app.getCacheDir(), "preview-test.mp4");
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("preview.mp4");
             FileOutputStream out = new FileOutputStream(video)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            bind(scenario, Uri.fromFile(video).toString(), "https://example.invalid/audio.mp3");
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View header = activity.findViewById(R.id.appHeader);
                int[] before = new int[2], after = new int[2];
                header.getLocationOnScreen(before);
                NestedScrollView scroll = activity.findViewById(R.id.mainScroll);
                scroll.scrollTo(0, activity.findViewById(R.id.cardPreview).getTop());
                assertTrue(scroll.getScrollY() > 0);
                header.getLocationOnScreen(after);
                assertArrayEquals(before, after);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.btnDownloadAudio).getVisibility());
            });
            onView(withId(R.id.btnTheme)).check(matches(isDisplayed()));
            onView(withId(R.id.btnPlayVideo)).perform(click());
            await(scenario, activity -> {
                Player player = ((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer();
                return player != null && player.isPlaying() && player.getCurrentPosition() > 100;
            });
            savePreview(app);
            scenario.onActivity(activity -> {
                Player player = ((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer();
                assertTrue(player.getDuration() >= 5900);
                player.pause();
                player.seekTo(1500);
            });
            scenario.recreate();
            await(scenario, activity -> {
                Player player = ((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer();
                return player != null && player.getPlaybackState() == Player.STATE_READY;
            });
            scenario.onActivity(activity -> {
                Player player = ((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer();
                assertFalse(player.getPlayWhenReady());
                assertTrue(player.getCurrentPosition() >= 1000);
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.onActivity(activity -> assertNull(((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer()));
            scenario.moveToState(Lifecycle.State.RESUMED);
            await(scenario, activity -> {
                Player player = ((PlayerView) activity.findViewById(R.id.videoPlayer)).getPlayer();
                return player != null && player.getPlaybackState() == Player.STATE_READY;
            });
        } finally { video.delete(); }
    }

    @Test public void playbackFailureShowsRetryAndMissingAudioHidesDownload() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            bind(scenario, "file:///missing-preview-video.mp4", "");
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ((NestedScrollView) activity.findViewById(R.id.mainScroll)).scrollTo(0, activity.findViewById(R.id.cardPreview).getTop());
                assertEquals(View.GONE, activity.findViewById(R.id.btnDownloadAudio).getVisibility());
            });
            onView(withId(R.id.btnPlayVideo)).perform(click());
            await(scenario, activity -> activity.findViewById(R.id.tvPlaybackStatus).getVisibility() == View.VISIBLE);
            onView(withId(R.id.btnPlayVideo)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertEquals(activity.getString(R.string.player_error),
                    ((TextView) activity.findViewById(R.id.tvPlaybackStatus)).getText().toString()));
        }
    }

    private void bind(ActivityScenario<MainActivity> scenario, String url, String audio) {
        TikTokVideoInfo info = new TikTokVideoInfo();
        info.setId("preview-test"); info.setTitle("Video de prueba"); info.setAuthorNickname("Vista previa");
        info.setAuthorUniqueId("preview"); info.setAuthorAvatar(""); info.setCoverUrl("");
        info.setVideoNoWmUrl(url); info.setVideoWmUrl(""); info.setAudioUrl(audio);
        scenario.onActivity(activity -> {
            try {
                java.lang.reflect.Method method = MainActivity.class.getDeclaredMethod("displayVideoInfo", TikTokVideoInfo.class);
                method.setAccessible(true); method.invoke(activity, info);
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }

    private interface Condition { boolean check(MainActivity activity); }

    private void savePreview(Context context) throws Exception {
        android.graphics.Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), "player-preview.png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }

    private void await(ActivityScenario<MainActivity> scenario, Condition condition) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 15000;
        AtomicBoolean passed = new AtomicBoolean();
        while (!passed.get() && android.os.SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> passed.set(condition.check(activity)));
            if (!passed.get()) Thread.sleep(100);
        }
        assertTrue("Preview should reach the expected state", passed.get());
    }
}
