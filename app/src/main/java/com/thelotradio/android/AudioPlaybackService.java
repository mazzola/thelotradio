package com.thelotradio.android;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.thelotradio.android.media.AudioPlayer;
import com.thelotradio.android.media.EventLogger;
import com.thelotradio.android.model.MusicProvider;

import java.util.List;

/**
 * Background service for playing audio
 */
public class AudioPlaybackService extends MediaBrowserServiceCompat implements
        AudioPlayer.Listener {
    private EventLogger eventLogger;
    private AudioPlayer audioPlayer;
    private boolean playerNeedsPrepare;
    private MediaSessionCompat session;
    private MediaNotificationManager mediaNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        preparePlayer();

        // Start a new MediaSession
        session = new MediaSessionCompat(this, "TheLotRadio");
        setSessionToken(session.getSessionToken());
        session.setCallback(new MediaSessionCallback());
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        session.setSessionActivity(pi);

        audioPlayer.setPlayWhenReady(true);

        try {
            mediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
    }

    @Override
    public void onDestroy() {
        audioPlayer.setPlayWhenReady(false);
        mediaNotificationManager.stopNotification();
        session.release();
    }

    private void preparePlayer() {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(this, MusicProvider.STREAM_128KBPS);
            playerNeedsPrepare = true;
            eventLogger = new EventLogger();
            eventLogger.startSession();
            audioPlayer.addListener(eventLogger);
            audioPlayer.addListener(this);
            audioPlayer.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            audioPlayer.prepare();
            playerNeedsPrepare = false;
        }
    }

    private void releasePlayer() {
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == AudioPlayer.STATE_READY) {
            session.setActive(true);
        }
    }

    @Override
    public void onError(Exception e) {
        String errorString = null;
        if (e instanceof ExoPlaybackException
                && e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
            // Special case for decoder initialization failures.
            MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
                    (MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    errorString = getString(R.string.error_querying_decoders);
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = getString(R.string.error_no_secure_decoder,
                            decoderInitializationException.mimeType);
                } else {
                    errorString = getString(R.string.error_no_decoder,
                            decoderInitializationException.mimeType);
                }
            } else {
                errorString = getString(R.string.error_instantiating_decoder,
                        decoderInitializationException.decoderName);
            }
        }
        if (errorString != null) {
            Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            audioPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            audioPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onStop() {
            audioPlayer.setPlayWhenReady(false);
            releasePlayer();
        }
    }
}
