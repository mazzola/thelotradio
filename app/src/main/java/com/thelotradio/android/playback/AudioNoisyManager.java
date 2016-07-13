package com.thelotradio.android.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

/**
 * Manages the AudioManager.ACTION_AUDIO_BECOMING_NOISY for the application
 */
public final class AudioNoisyManager {
    private static boolean mAudioNoisyReceiverRegistered;
    private static Playback playback;
    private static AudioNoisyReceiver audioNoisyReceiver = new AudioNoisyReceiver();
    private static final IntentFilter audioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private AudioNoisyManager() { }

    public static void registerAudioNoisyReceiver(Context context, Playback playback) {
        if (!mAudioNoisyReceiverRegistered) {
            AudioNoisyManager.playback = playback;
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    public static void unregisterAudioNoisyReceiver(Context context) {
        if (mAudioNoisyReceiverRegistered) {
            playback = null;
            context.unregisterReceiver(audioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private static class AudioNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (playback.isPlaying()) {
                    playback.pause();
                }
            }
        }
    }
}
