package com.thelotradio.android.model;

import android.net.Uri;

/**
 * Provides the Uri for the music stream
 */
public final class MusicProvider {
    public final static Uri STREAM_64KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_a");
    public final static Uri STREAM_128KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_b");
    public final static Uri STREAM_192KBPS = Uri.parse("http://thelot.out.airtime.pro:8000/thelot_c");

    private MusicProvider() { }
}
