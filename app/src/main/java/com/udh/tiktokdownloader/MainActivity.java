package com.udh.tiktokdownloader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

@android.annotation.SuppressLint("UnsafeOptInUsageError")
public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUrl;
    private MaterialButton btnPaste;
    private MaterialButton btnFetch;
    private LinearProgressIndicator progressIndicator;

    private MaterialCardView cardPreview;
    private ImageView ivCover;
    private ShapeableImageView ivAvatar;
    private TextView tvAuthorName;
    private TextView tvAuthorHandle;
    private TextView tvVideoTitle;

    private TextView tvLikes;
    private TextView tvComments;
    private TextView tvShares;
    private TextView tvViews;

    private MaterialButton btnDownloadNoWm;
    private MaterialButton btnDownloadWm;
    private MaterialButton btnDownloadAudio;
    private MaterialButton btnShare;
    private MaterialButton btnPlayVideo;
    private PlayerView videoPlayer;
    private TextView tvPlaybackStatus;
    private ExoPlayer player;
    private long playbackPosition;
    private boolean playbackRequested;
    private boolean resumePlayback;
    private boolean triedWatermarkedVideo;

    private TikTokParser tiktokParser;
    private TikTokVideoInfo currentVideoInfo;
    private boolean fetching;
    private int searchGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.mainRoot);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        tiktokParser = new TikTokParser();

        initViews();
        setupListeners();
        setupDynamicShareShortcuts();

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        } else {
            etUrl.setText(savedInstanceState.getString("url_input", ""));
            playbackPosition = savedInstanceState.getLong("playback_position");
            playbackRequested = savedInstanceState.getBoolean("playback_requested");
            resumePlayback = savedInstanceState.getBoolean("resume_playback");
            TikTokVideoInfo savedVideo = Build.VERSION.SDK_INT >= 33
                    ? savedInstanceState.getSerializable("video_info", TikTokVideoInfo.class)
                    : (TikTokVideoInfo) savedInstanceState.getSerializable("video_info");
            if (savedVideo != null) displayVideoInfo(savedVideo);
            else if (savedInstanceState.getBoolean("fetching")) fetchVideo(savedInstanceState.getString("url_input", ""));
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void initViews() {
        etUrl = findViewById(R.id.etUrl);
        btnPaste = findViewById(R.id.btnPaste);
        btnFetch = findViewById(R.id.btnFetch);
        progressIndicator = findViewById(R.id.progressIndicator);

        cardPreview = findViewById(R.id.cardPreview);
        ivCover = findViewById(R.id.ivCover);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvAuthorHandle = findViewById(R.id.tvAuthorHandle);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);

        tvLikes = findViewById(R.id.tvLikes);
        tvComments = findViewById(R.id.tvComments);
        tvShares = findViewById(R.id.tvShares);
        tvViews = findViewById(R.id.tvViews);

        btnDownloadNoWm = findViewById(R.id.btnDownloadNoWm);
        btnDownloadWm = findViewById(R.id.btnDownloadWm);
        btnDownloadAudio = findViewById(R.id.btnDownloadAudio);
        btnShare = findViewById(R.id.btnShare);
        btnPlayVideo = findViewById(R.id.btnPlayVideo);
        videoPlayer = findViewById(R.id.videoPlayer);
        tvPlaybackStatus = findViewById(R.id.tvPlaybackStatus);
    }

    private void setupListeners() {
        btnPlayVideo.setOnClickListener(v -> {
            playbackRequested = true;
            resumePlayback = true;
            triedWatermarkedVideo = false;
            startPlayback();
        });
        btnDownloadAudio.setOnClickListener(v -> {
            if (currentVideoInfo != null) DownloadHelper.downloadFile(this, currentVideoInfo.getAudioUrl(),
                    currentVideoInfo.getId(), currentVideoInfo.getTitle(), DownloadHelper.TYPE_AUDIO);
        });
        findViewById(R.id.btnTheme).setOnClickListener(v -> {
            String[] choices = {getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark)};
            new MaterialAlertDialogBuilder(this).setTitle(R.string.theme_title)
                    .setSingleChoiceItems(choices, DownloaderApplication.getThemeChoice(this), (dialog, which) -> {
                        dialog.dismiss();
                        DownloaderApplication.setThemeChoice(this, which);
                    })
                    .setNegativeButton(R.string.theme_cancel, null).show();
        });
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                btnFetch.performClick();
                return true;
            }
            return false;
        });
        btnPaste.setOnClickListener(v -> pasteFromClipboard());

        btnFetch.setOnClickListener(v -> {
            String text = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            if (text.isEmpty()) {
                Toast.makeText(this, R.string.err_empty_url, Toast.LENGTH_SHORT).show();
            } else {
                fetchVideo(text);
            }
        });

        btnDownloadNoWm.setOnClickListener(v -> {
            if (currentVideoInfo != null) {
                DownloadHelper.downloadFile(
                        this,
                        currentVideoInfo.getVideoNoWmUrl(),
                        currentVideoInfo.getId(),
                        currentVideoInfo.getTitle(),
                        DownloadHelper.TYPE_VIDEO_NO_WM
                );
            }
        });

        btnDownloadWm.setOnClickListener(v -> {
            if (currentVideoInfo != null) {
                DownloadHelper.downloadFile(
                        this,
                        currentVideoInfo.getVideoWmUrl(),
                        currentVideoInfo.getId(),
                        currentVideoInfo.getTitle(),
                        DownloadHelper.TYPE_VIDEO_WM
                );
            }
        });

        btnShare.setOnClickListener(v -> {
            if (currentVideoInfo != null && currentVideoInfo.getVideoNoWmUrl() != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, currentVideoInfo.getVideoNoWmUrl());
                startActivity(Intent.createChooser(shareIntent, "Compartir video de TikTok"));
            }
        });
    }

    private void setupDynamicShareShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                if (shortcutManager != null) {
                    Intent appShareIntent = new Intent(this, MainActivity.class);
                    appShareIntent.setAction(Intent.ACTION_SEND);
                    appShareIntent.setType("text/plain");

                    ShortcutInfo shortcutApp = new ShortcutInfo.Builder(this, "tiktok_app_share")
                            .setShortLabel(getString(R.string.app_name))
                            .setLongLabel(getString(R.string.app_subtitle))
                            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                            .setIntent(appShareIntent)
                            .setCategories(Collections.singleton("com.udh.tiktokdownloader.category.SHARE_TARGET"))
                            .build();

                    Intent quickDownloadIntent = new Intent(this, QuickDownloadActivity.class);
                    quickDownloadIntent.setAction(Intent.ACTION_SEND);
                    quickDownloadIntent.setType("text/plain");

                    ShortcutInfo shortcutQuick = new ShortcutInfo.Builder(this, "tiktok_quick_download")
                            .setShortLabel(getString(R.string.label_quick_download))
                            .setLongLabel(getString(R.string.label_quick_download))
                            .setIcon(Icon.createWithResource(this, R.drawable.ic_download))
                            .setIntent(quickDownloadIntent)
                            .setCategories(Collections.singleton("com.udh.tiktokdownloader.category.QUICK_DOWNLOAD_TARGET"))
                            .build();

                    shortcutManager.addDynamicShortcuts(Arrays.asList(shortcutApp, shortcutQuick));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData.getItemCount() > 0) {
                CharSequence pastedText = clipData.getItemAt(0).getText();
                if (pastedText != null && !pastedText.toString().trim().isEmpty()) {
                    etUrl.setText(pastedText.toString());
                    fetchVideo(pastedText.toString());
                    return;
                }
            }
        }
        Toast.makeText(this, R.string.err_clipboard_empty, Toast.LENGTH_SHORT).show();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String sharedText = null;
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                CharSequence textChar = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (textChar != null) {
                    sharedText = textChar.toString();
                }
            }
            if (sharedText == null && intent.getClipData() != null) {
                ClipData clip = intent.getClipData();
                for (int i = 0; i < clip.getItemCount(); i++) {
                    CharSequence text = clip.getItemAt(i).getText();
                    if (text != null) {
                        String extracted = TikTokParser.extractUrl(text.toString());
                        if (extracted != null) {
                            sharedText = extracted;
                            break;
                        }
                    }
                }
            }
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            sharedText = intent.getData().toString();
        } else if (Intent.ACTION_PROCESS_TEXT.equals(action)) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            if (text != null) {
                sharedText = text.toString();
            }
        }

        if (sharedText != null && !sharedText.trim().isEmpty()) {
            etUrl.setText(sharedText);
            fetchVideo(sharedText);
        }
    }

    private void fetchVideo(String input) {
        int generation = ++searchGeneration;
        etUrl.clearFocus();
        androidx.core.view.WindowInsetsControllerCompat insetsController = ViewCompat.getWindowInsetsController(etUrl);
        if (insetsController != null) insetsController.hide(WindowInsetsCompat.Type.ime());
        releasePlayer();
        playbackPosition = 0;
        playbackRequested = false;
        resumePlayback = false;
        triedWatermarkedVideo = false;
        fetching = true;
        progressIndicator.setVisibility(View.VISIBLE);
        cardPreview.setVisibility(View.GONE);
        currentVideoInfo = null;

        tiktokParser.fetchVideoInfo(input, new TikTokParser.OnVideoInfoListener() {
            @Override
            public void onSuccess(TikTokVideoInfo videoInfo) {
                if (isDestroyed() || generation != searchGeneration) return;
                fetching = false;
                progressIndicator.setVisibility(View.GONE);
                displayVideoInfo(videoInfo);
                cardPreview.post(() -> ((androidx.core.widget.NestedScrollView) findViewById(R.id.mainScroll))
                        .smoothScrollTo(0, cardPreview.getTop()));
            }

            @Override
            public void onError(String errorMessage) {
                if (isDestroyed() || generation != searchGeneration) return;
                fetching = false;
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        rememberPlayback();
        super.onSaveInstanceState(state);
        state.putString("url_input", etUrl.getText() == null ? "" : etUrl.getText().toString());
        state.putBoolean("fetching", fetching);
        state.putLong("playback_position", playbackPosition);
        state.putBoolean("playback_requested", playbackRequested);
        state.putBoolean("resume_playback", resumePlayback);
        if (currentVideoInfo != null) state.putSerializable("video_info", currentVideoInfo);
    }

    @Override protected void onDestroy() {
        releasePlayer();
        if (tiktokParser != null) tiktokParser.close();
        super.onDestroy();
    }

    private void displayVideoInfo(TikTokVideoInfo videoInfo) {
        this.currentVideoInfo = videoInfo;
        cardPreview.setVisibility(View.VISIBLE);

        tvAuthorName.setText(videoInfo.getAuthorNickname());
        String handleText = videoInfo.getAuthorUniqueId().isEmpty() ? "" : "@" + videoInfo.getAuthorUniqueId();
        tvAuthorHandle.setText(handleText);
        tvVideoTitle.setText(videoInfo.getTitle());

        tvLikes.setText(String.format(Locale.getDefault(), "❤️ %s", DownloadHelper.formatNumber(videoInfo.getLikesCount())));
        tvComments.setText(String.format(Locale.getDefault(), "💬 %s", DownloadHelper.formatNumber(videoInfo.getCommentsCount())));
        tvShares.setText(String.format(Locale.getDefault(), "🔁 %s", DownloadHelper.formatNumber(videoInfo.getSharesCount())));
        tvViews.setText(String.format(Locale.getDefault(), "👁️ %s", DownloadHelper.formatNumber(videoInfo.getViewsCount())));

        if (!videoInfo.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .load(videoInfo.getCoverUrl())
                    .centerCrop()
                    .into(ivCover);
        }

        if (!videoInfo.getAuthorAvatar().isEmpty()) {
            Glide.with(this)
                    .load(videoInfo.getAuthorAvatar())
                    .circleCrop()
                    .into(ivAvatar);
        }

        btnDownloadNoWm.setVisibility(videoInfo.getVideoNoWmUrl().isEmpty() ? View.GONE : View.VISIBLE);
        btnDownloadWm.setVisibility(videoInfo.getVideoWmUrl().isEmpty() ? View.GONE : View.VISIBLE);
        btnDownloadAudio.setVisibility(videoInfo.getAudioUrl().isEmpty() ? View.GONE : View.VISIBLE);
        btnPlayVideo.setVisibility(View.VISIBLE);
        btnPlayVideo.setContentDescription(getString(R.string.player_play));
        ivCover.setVisibility(View.VISIBLE);
        videoPlayer.setVisibility(View.INVISIBLE);
        tvPlaybackStatus.setVisibility(View.GONE);
        if (playbackRequested && getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) startPlayback();
    }

    @Override protected void onStart() {
        super.onStart();
        if (playbackRequested && currentVideoInfo != null && player == null) startPlayback();
    }

    @Override protected void onStop() {
        releasePlayer();
        super.onStop();
    }

    private void rememberPlayback() {
        if (player == null) return;
        playbackPosition = Math.max(0, player.getCurrentPosition());
        resumePlayback = player.getPlayWhenReady();
    }

    private void releasePlayer() {
        if (player == null) return;
        rememberPlayback();
        videoPlayer.setKeepScreenOn(false);
        videoPlayer.setPlayer(null);
        ExoPlayer old = player;
        player = null;
        old.release();
    }

    private String playbackUrl() {
        if (currentVideoInfo == null) return "";
        String noWm = currentVideoInfo.getVideoNoWmUrl();
        String wm = currentVideoInfo.getVideoWmUrl();
        return !triedWatermarkedVideo && noWm != null && !noWm.isEmpty() ? noWm : (wm == null ? "" : wm);
    }

    private void startPlayback() {
        if (player != null) {
            player.setPlayWhenReady(resumePlayback);
            return;
        }
        String url = playbackUrl();
        if (url.isEmpty()) {
            tvPlaybackStatus.setText(R.string.player_unavailable);
            tvPlaybackStatus.setVisibility(View.VISIBLE);
            return;
        }
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("TikTokDownloader/1.0")
                .setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(15000);
        ExoPlayer current = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this, http)))
                .build();
        player = current;
        current.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        current.setHandleAudioBecomingNoisy(true);
        current.addListener(new Player.Listener() {
            @Override public void onRenderedFirstFrame() {
                if (player != current) return;
                ivCover.setVisibility(View.GONE);
            }

            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (player == current) videoPlayer.setKeepScreenOn(isPlaying);
            }

            @Override public void onPlayerError(@NonNull PlaybackException error) {
                if (player != current) return;
                releasePlayer();
                String fallback = currentVideoInfo == null ? "" : currentVideoInfo.getVideoWmUrl();
                if (!triedWatermarkedVideo && fallback != null && !fallback.isEmpty() && !fallback.equals(url)) {
                    triedWatermarkedVideo = true;
                    startPlayback();
                    return;
                }
                videoPlayer.setVisibility(View.INVISIBLE);
                ivCover.setVisibility(View.VISIBLE);
                btnPlayVideo.setVisibility(View.VISIBLE);
                btnPlayVideo.setContentDescription(getString(R.string.player_retry));
                tvPlaybackStatus.setText(R.string.player_error);
                tvPlaybackStatus.setVisibility(View.VISIBLE);
            }
        });
        videoPlayer.setPlayer(current);
        videoPlayer.setVisibility(View.VISIBLE);
        ivCover.setVisibility(View.GONE);
        btnPlayVideo.setVisibility(View.GONE);
        tvPlaybackStatus.setVisibility(View.GONE);
        current.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        current.seekTo(playbackPosition);
        current.setPlayWhenReady(resumePlayback);
        current.prepare();
    }
}
