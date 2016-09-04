package com.example.administrator.myvideodemo.videoplayer;

import android.media.MediaPlayer;

/**
 * Created by Administrator on 9/4/2016.
 */
public interface IVideoView {
    void onScaleChange(boolean isFullscreen);

    void onPause(final MediaPlayer mediaPlayer);

    void onStart(final MediaPlayer mediaPlayer);

    void onBufferingStart(final MediaPlayer mediaPlayer);

    void onBufferingEnd(final MediaPlayer mediaPlayer);
}
