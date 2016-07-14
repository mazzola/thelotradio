package com.thelotradio.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

/**
 * Creates and controls a notification for a MediaSession
 */
public class NotificationManager {
    private static final int NOTIFICATION_ID = 0;
    private final Service service;
    private final MediaSessionCompat mediaSession;
    private final NotificationManagerCompat notificationManager;

    public NotificationManager(Service service, MediaSessionCompat mediaSession) {
        this.service = service;
        this.mediaSession = mediaSession;
        notificationManager = NotificationManagerCompat.from(service);
    }

    public void startNotification() {
        service.startForeground(NOTIFICATION_ID, createNotification());
        updateNotification();
    }

    public void stopNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
        service.stopForeground(true);
    }

    public void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @return A pre-built notification with information from the given media session.
     */
    private Notification createNotification() {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();
        PendingIntent stopPendingIntent = getActionIntent(service, KeyEvent.KEYCODE_MEDIA_STOP);
        Bitmap largeIcon = ((BitmapDrawable)
                ContextCompat.getDrawable(service, R.drawable.ic_logo)).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service);
        builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(largeIcon)
                .setShowWhen(false)
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(ContextCompat.getColor(service, R.color.colorPrimaryDark))
                .setDeleteIntent(stopPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopPendingIntent)
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSession.getSessionToken()));

        addAction(builder);
        return builder.build();
    }

    private void addAction(NotificationCompat.Builder builder) {
        if (mediaSession.getController().getPlaybackState()
                .getState() == PlaybackStateCompat.STATE_PAUSED) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_arrow_white_24dp,
                    service.getString(R.string.label_play),
                    getActionIntent(service, KeyEvent.KEYCODE_MEDIA_PLAY)));
        } else {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_white_24dp,
                    service.getString(R.string.label_pause),
                    getActionIntent(service, KeyEvent.KEYCODE_MEDIA_PAUSE)));
        }
    }

    /**
     * Create a {@link PendingIntent} appropriate for a MediaStyle notification's action. Assumes
     * you are using a media button receiver.
     * @param context Context used to contruct the pending intent.
     * @param mediaKeyEvent KeyEvent code to send to your media button receiver.
     * @return An appropriate pending intent for sending a media button to your media button
     *      receiver.
     */
    private PendingIntent getActionIntent(
            Context context, int mediaKeyEvent) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyEvent));
        return PendingIntent.getBroadcast(context, mediaKeyEvent, intent, 0);
    }
}
