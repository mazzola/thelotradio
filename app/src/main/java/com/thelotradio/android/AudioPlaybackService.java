package com.thelotradio.android;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.thelotradio.android.playback.AudioPlayback;
import com.thelotradio.android.playback.Playback;

/**
 * Background service for playing audio
 */
public class AudioPlaybackService extends Service implements Playback.Callback {
    private MediaSessionCompat session;
    private AudioPlayback audioPlayback;
    private NotificationManager notificationManager;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MediaSessionCompat.Token getSessionToken() {
            return session.getSessionToken();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioPlayback = new AudioPlayback(this, this);
        audioPlayback.setCallback(this);
        // Start a new MediaSession
        session = new MediaSessionCompat(this, "TheLotRadio");
        session.setCallback(new MediaSessionCallback());
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        session.setSessionActivity(pi);
        if (!session.isActive()) {
            session.setActive(true);
        }

        audioPlayback.play();
        session.setMetadata(
                new MediaMetadataCompat.Builder()
                        .putText(MediaMetadataCompat.METADATA_KEY_TITLE,
                                getApplicationContext().getString(R.string.app_name))
                        .putText(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                getString(R.string.subtitle))
                        .build());
        notificationManager = new NotificationManager(this, session);
        notificationManager.startNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(session, intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        audioPlayback.stop(true);
        session.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onPlaybackStatusChanged(PlaybackStateCompat state) {
        session.setPlaybackState(state);
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void setCurrentMediaId(String mediaId) {

    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (!audioPlayback.isPlaying()) {
                audioPlayback.play();
                notificationManager.updateNotification();
            }
        }

        @Override
        public void onPause() {
            if (audioPlayback.isPlaying()) {
                audioPlayback.pause();
                notificationManager.updateNotification();
            }
        }

        @Override
        public void onStop() {
            audioPlayback.stop(true);
            session.setActive(false);
            notificationManager.stopNotification();
        }
    }
}
