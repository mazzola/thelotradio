package com.thelotradio.android.model;

import android.net.Uri;

/**
 * Provides the Uri for the music stream
 */
public final class MusicProvider {
    public final static Uri STREAM_64KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_a");
    public final static Uri STREAM_128KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_b");
    public final static Uri STREAM_192KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_c");
    public final static Uri SAMPLE_MP3 = Uri.parse("https://audiocdn7.mixcloud.com/previews/9/f/b/a/c8df-aa59-484e-9c32-873a5bbad006.mp3");

    private MusicProvider() { }
}
