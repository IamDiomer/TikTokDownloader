package com.udh.tiktokdownloader;

import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class QuickDownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

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
        }

        if (sharedText != null && !sharedText.trim().isEmpty()) {
            Toast.makeText(this, R.string.msg_quick_download_start, Toast.LENGTH_SHORT).show();

            TikTokParser parser = new TikTokParser();
            parser.fetchVideoInfo(sharedText, new TikTokParser.OnVideoInfoListener() {
                @Override
                public void onSuccess(TikTokVideoInfo videoInfo) {
                    DownloadHelper.downloadFile(
                            getApplicationContext(),
                            videoInfo.getVideoNoWmUrl(),
                            videoInfo.getId(),
                            videoInfo.getTitle(),
                            DownloadHelper.TYPE_VIDEO_NO_WM
                    );
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(getApplicationContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            Toast.makeText(this, R.string.err_empty_url, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
