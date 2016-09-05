package com.example.administrator.myvideodemo.videoplayer;

import android.media.MediaPlayer;

/**
 * Created by Administrator on 9/4/2016.
 */
public interface IVideoView {
    void onVideoScaleChange(boolean isFullscreen);

    void onVideoPause(final MediaPlayer mediaPlayer);

    void onVideoStart(final MediaPlayer mediaPlayer);

    void onVideoBufferingStart(final MediaPlayer mediaPlayer);

    void onVideoBufferingEnd(final MediaPlayer mediaPlayer);
}
