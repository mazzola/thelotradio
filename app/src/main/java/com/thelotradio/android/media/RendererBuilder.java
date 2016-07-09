package com.thelotradio.android.media;

/**
 * Builds renderers for the player.
 */
public interface RendererBuilder {
    /**
     * Builds renderers for playback.
     *
     * @param audioPlayer The audioPlayer for which renderers are being built. {@link AudioPlayer#onRenderers}
     *                    should be invoked once the renderers have been built. If building fails,
     *                    {@link AudioPlayer#onRenderersError} should be invoked.
     */
    void buildRenderers(AudioPlayer audioPlayer);

    /**
     * Cancels the current build operation, if there is one. Else does nothing.
     * <p/>
     * A canceled build operation must not invoke {@link AudioPlayer#onRenderers} or
     * {@link AudioPlayer#onRenderersError} on the player, which may have been released.
     */
    void cancel();
}