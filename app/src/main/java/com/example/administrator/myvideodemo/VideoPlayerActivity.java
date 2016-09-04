package com.example.administrator.myvideodemo;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.administrator.myvideodemo.videoplayer.IVideoView;
import com.example.administrator.myvideodemo.videoplayer.MyMediaController;
import com.example.administrator.myvideodemo.videoplayer.MyVideoView;

public class VideoPlayerActivity extends AppCompatActivity implements IVideoView {
    private MyVideoView vv_video_view;
    private FrameLayout fl_video_view;
    private MyMediaController media_controller;
    private static final String VIDEO_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private int cachedHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        vv_video_view = (MyVideoView) findViewById(R.id.vv_video_view);
        fl_video_view = (FrameLayout) findViewById(R.id.fl_video_view);
        media_controller = (MyMediaController) findViewById(R.id.media_controller);
        vv_video_view.setMediaController(media_controller);
        setVideoAreaSize();
        vv_video_view.setVideoViewCallback(this);

    }


    /**
     * 置视频区域大小
     */
    private void setVideoAreaSize() {
        fl_video_view.post(new Runnable() {
            @Override
            public void run() {
                int width = fl_video_view.getWidth();
                cachedHeight = (int) (width * 405f / 720f);
//                cachedHeight = (int) (width * 3f / 4f);
//                cachedHeight = (int) (width * 9f / 16f);
                ViewGroup.LayoutParams videoLayoutParams = fl_video_view.getLayoutParams();
                videoLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                videoLayoutParams.height = cachedHeight;
                fl_video_view.setLayoutParams(videoLayoutParams);
                vv_video_view.setVideoPath(VIDEO_URL);
                vv_video_view.requestFocus();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onScaleChange(boolean isFullscreen) {

    }

    @Override
    public void onPause(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onStart(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onBufferingStart(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onBufferingEnd(MediaPlayer mediaPlayer) {

    }
}
