package com.thelotradio.android.playback;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.thelotradio.android.R;
import com.thelotradio.android.media.AudioPlayer;
import com.thelotradio.android.media.EventLogger;
import com.thelotradio.android.model.MusicProvider;

/**
 * Plays an audio track using an AudioPlayer
 */
public class AudioPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        AudioPlayer.Listener {
    // The volume we set the AudioPlayer to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the AudioPlayer when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;
    private final AudioManager audioManager;

    private Context context;
    private AudioPlayer audioPlayer;
    private Callback callback;
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private int state = PlaybackStateCompat.STATE_NONE;
    private boolean playOnAudioFocusGain;
    private EventLogger eventLogger;
    private WifiManager.WifiLock wifiLock;

    public AudioPlayback(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "thelotradio_lock");
        createPlayer();
    }

    @Override
    public void start() { }

    @Override
    public void play() {
        playOnAudioFocusGain = true;
        tryToGetAudioFocus();
        AudioNoisyManager.registerAudioNoisyReceiver(context, this);

        if (state == PlaybackStateCompat.STATE_PAUSED) {
            configAudioPlayerState();
        } else {
            state = PlaybackStateCompat.STATE_BUFFERING;
            audioPlayer.prepare();
            wifiLock.acquire();
            if (callback != null) {
                callback.onPlaybackStatusChanged(createPlaybackState(state));
            }
        }
    }

    @Override
    public void pause() {
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            if (audioPlayer != null && audioPlayer.getPlayWhenReady()) {
                audioPlayer.setPlayWhenReady(false);
            }
            releaseResources(false);
            giveUpAudioFocus();
        }
        state = PlaybackStateCompat.STATE_PAUSED;
        if (callback != null) {
            callback.onPlaybackStatusChanged(createPlaybackState(state));
        }
        AudioNoisyManager.unregisterAudioNoisyReceiver(context);
    }

    @Override
    public void stop(boolean notifyListeners) {
        state = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(createPlaybackState(state));
        }
        giveUpAudioFocus();
        AudioNoisyManager.unregisterAudioNoisyReceiver(context);
        releaseResources(true);
    }

    @Override
    public void setState(int state) {
        this.state = state;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public boolean isPlaying() {
        return playOnAudioFocusGain || (audioPlayer != null && audioPlayer.getPlayWhenReady());
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            audioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset AudioPlayer by calling configAudioPlayerState
            // with audioFocus properly set.
            if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                playOnAudioFocusGain = true;
            }
        }

        configAudioPlayerState();
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == AudioPlayer.STATE_READY) {
            // The AudioPlayer is done preparing. That means we can start playing if we
            // have audio focus.
            configAudioPlayerState();
        } else if (playbackState == AudioPlayer.STATE_ENDED) {
            if (callback != null) {
                callback.onCompletion();
            }
        }

    }

    @Override
    public void onError(Exception e) {
        if (callback == null) {
            return;
        }
        String errorString = null;
        if (e instanceof ExoPlaybackException
                && e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
            // Special case for decoder initialization failures.
            MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
                    (MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.getCause() instanceof
                        MediaCodecUtil.DecoderQueryException) {
                    errorString = context.getString(R.string.error_querying_decoders);
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = context.getString(R.string.error_no_secure_decoder,
                            decoderInitializationException.mimeType);
                } else {
                    errorString = context.getString(R.string.error_no_decoder,
                            decoderInitializationException.mimeType);
                }
            } else {
                errorString = context.getString(R.string.error_instantiating_decoder,
                        decoderInitializationException.decoderName);
            }
        }
        callback.onError("AudioPlayer error: " + errorString);
    }

    private void createPlayer() {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(context, MusicProvider.STREAM_128KBPS);
            eventLogger = new EventLogger();
            eventLogger.startSession();
            audioPlayer.addListener(eventLogger);
            audioPlayer.addListener(this);
            audioPlayer.setInternalErrorListener(eventLogger);
        }
    }

    /**
     * Reconfigures AudioPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the AudioPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * AudioPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes audioPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configAudioPlayerState() {
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                audioPlayer.setVolume(VOLUME_DUCK); // we'll be relatively quiet
            } else {
                audioPlayer.setVolume(VOLUME_NORMAL); // we can be loud again
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (playOnAudioFocusGain) {
                if (!audioPlayer.getPlayWhenReady()) {
                    audioPlayer.setPlayWhenReady(true);
                    state = PlaybackStateCompat.STATE_PLAYING;
                }
                playOnAudioFocusGain = false;
            }
        }
        if (callback != null) {
            callback.onPlaybackStatusChanged(createPlaybackState(state));
        }
    }

    private PlaybackStateCompat createPlaybackState(int state) {
        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());
        stateBuilder.setState(state, 0, 1.0f);
        return stateBuilder.build();
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY;
        if (isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    private void giveUpAudioFocus() {
        if (audioFocus == AUDIO_FOCUSED) {
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    private void tryToGetAudioFocus() {
        if (audioFocus != AUDIO_FOCUSED) {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void releaseResources(boolean releaseAudioPlayer) {
        if (releaseAudioPlayer && audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
            eventLogger.endSession();
            eventLogger = null;
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }
}
