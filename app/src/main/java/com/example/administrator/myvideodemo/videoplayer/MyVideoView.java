package com.example.administrator.myvideodemo.videoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.administrator.myvideodemo.R;
import com.example.administrator.myvideodemo.util.KLog;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Administrator on 9/4/2016.
 */
public class MyVideoView extends SurfaceView implements IMyMediaControl {
    private static final String TAG = "MyVideoView";
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private MyMediaController mMyMediaController = null;

    private IVideoView mIVideoView;

    private final boolean mFitXY;
    private final boolean mAutoRotation;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;


    private int mVideoWidth;
    private int mVideoHeight;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private int mSeekWhenPrepared;  // 在播放器准备的时候记录播放的位置


    private Uri mUri;
    private Context mContext;
    private int mAudioSession;

    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private boolean mPreparedBeforeStart;
    private int mCurrentBufferPercentage;
    private int mVideoViewLayoutWidth = 0;
    private int mVideoViewLayoutHeight = 0;

    public MyVideoView(Context context) {
        this(context, null);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyVideoView, 0, 0);
        mFitXY = a.getBoolean(R.styleable.MyVideoView_fitXY, false);
        mAutoRotation = a.getBoolean(R.styleable.MyVideoView_autoRotation, false);
        a.recycle();
        initVideoView(context);
    }

    private void initVideoView(Context context) {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSurfaceHolderCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    public void setMediaController(MyMediaController controller) {
        mMyMediaController = controller;
        attachMediaController();
    }


    //附加MyMediaController
    private void attachMediaController() {

        if (mMediaPlayer != null && mMyMediaController != null) {
            mMyMediaController.setMediaPlayer(this);
//            mMyMediaController.setEnabled(isInPlaybackState());
            mMyMediaController.hideControlBar();
        }
    }


    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }


    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }


    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }


    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            KLog.i("------surfaceChanged");
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            KLog.i("------surfaceCreated");
            mSurfaceHolder = holder;
            openVideo();
//            enableOrientationDetect();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            KLog.i("------surfaceDestroyed");
            mSurfaceHolder = null;
            if (mMyMediaController != null) mMyMediaController.hideControlBar();
            release(true);
//            disableOrientationDetect();
        }
    };

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            if (mAudioSession != 0) {
                mMediaPlayer.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = mMediaPlayer.getAudioSessionId();
            }
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();


            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (IOException ex) {
            KLog.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
        KLog.i("------openVideo");
    }

    /*
 * release the media player in any state
 */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
        }
        KLog.i("------release");
    }


    public MyMediaController getMyMediaController() {
        return mMyMediaController;
    }


    public void setVideoViewCallback(IVideoView callback) {
        this.mIVideoView = callback;
    }

    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            KLog.i("-----onAudioFocusChange:" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:   //音频获得焦点时
                    // resume playback
                    if (mMediaPlayer == null) {
                        openVideo();
                    } else if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                        mMediaPlayer.setVolume(1.0f, 1.0f);
                    }
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:  //音频失去焦点时
                    // Lost focus for an unbounded amount of time: stop playback and release media player

                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: //暂时失去了音频的焦点，应该要马上回到焦点上.你一定要停止掉所有的音频的播放,但是你能持有你的资源因为你可能很快的再次获得聚焦.
                    // Lost focus for a short time, but we have to stop
                    // playback. We don't release the media player because playback
                    // is likely to resume
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        }
                    }
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: //暂时的失去了音频的焦点,但是你允许继续用小音量播放音乐而不是完全杀掉音频.
                    // Lost focus for a short time, but it's ok to keep playing
                    // at an attenuated level
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.setVolume(0.1f, 0.1f);
                        }
                    }
                    break;
            }
        }
    };

    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {

            mCurrentState = STATE_PREPARED;

            mCanPause = mCanSeekBack = mCanSeekForward = true;

            mPreparedBeforeStart = true;
            if (mMyMediaController != null) {
                mMyMediaController.hideLoading();
            }

            // TODO: 9/4/2016 这里不明白
//            if (mOnPreparedListener != null) {
//                mOnPreparedListener.onPrepared(mMediaPlayer);
//            }
            if (mMyMediaController != null) {
                mMyMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //KLog.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    // We didn't actually change the size (it was already at the size
                    // we need), so we won't get a "surface changed" callback, so
                    // start the video here instead of in the callback.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                        if (mMyMediaController != null) {
                            mMyMediaController.showControlBar();
                        }
                    } else if (!isPlaying() &&
                            (seekToPosition != 0 || getCurrentPosition() > 0)) {
                        if (mMyMediaController != null) {
                            // Show the media controls when we're paused into a video and make 'em stick.
                            mMyMediaController.showControlBar(0);
                        }
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };


    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            KLog.d(TAG, String.format("onVideoSizeChanged width=%d,height=%d", mVideoWidth, mVideoHeight));
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                requestLayout();
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMyMediaController != null) {
                boolean a = mMediaPlayer.isPlaying();
                int b = mCurrentState;
                mMyMediaController.showComplete();
                //FIXME 播放完成后,视频中央会显示一个播放按钮,点击播放按钮会调用start重播,
                // 但start后竟然又回调到这里,导致第一次点击按钮不会播放视频,需要点击第二次.
                KLog.d(TAG, String.format("a=%s,b=%d", a, b));
            }
//                    if (mOnCompletionListener != null) {
//                        mOnCompletionListener.onCompletion(mMediaPlayer);
//                    }
        }
    };

    private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            boolean handled = false;
            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    KLog.d(TAG, "onInfo MediaPlayer.MEDIA_INFO_BUFFERING_START");
                    if (mIVideoView != null) {
                        mIVideoView.onVideoBufferingStart(mMediaPlayer);
                    }
                    if (mMyMediaController != null) {
                        mMyMediaController.showLoading();
                    }
                    handled = true;
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    KLog.d(TAG, "onInfo MediaPlayer.MEDIA_INFO_BUFFERING_END");
                    if (mIVideoView != null) {
                        mIVideoView.onVideoBufferingEnd(mMediaPlayer);
                    }
                    if (mMyMediaController != null) {
                        mMyMediaController.hideLoading();
                    }
                    handled = true;
                    break;
            }
//                    if (mOnInfoListener != null) {
//                        return mOnInfoListener.onInfo(mp, what, extra) || handled;
//                    }
            return handled;
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            KLog.d(TAG, "Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            release(true);
            if (mMyMediaController != null) {
                mMyMediaController.showError();
            }

            /* If an error handler has been supplied, use it and finish. */
//                    if (mOnErrorListener != null) {
//                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
//                            return true;
//                        }
//                    }

            /* Otherwise, pop up an error diaKLog so the user knows that
             * something bad has happened. Only try and pop up the diaKLog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
//                    if (getWindowToken() != null) {
//                        Resources r = mContext.getResources();
//                        int messageId;
//
//                        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
//                            messageId = com.android.internal.R.string.VideoView_error_text_invalid_progressive_playback;
//                        } else {
//                            messageId = com.android.internal.R.string.VideoView_error_text_unknown;
//                        }
//
//                        new AlertDiaKLog.Builder(mContext)
//                                .setMessage(messageId)
//                                .setPositiveButton(com.android.internal.R.string.VideoView_error_button,
//                                        new DiaKLogInterface.OnClickListener() {
//                                            public void onClick(DiaKLogInterface diaKLog, int whichButton) {
//                                        /* If we get here, there is no onError listener, so
//                                         * at least inform them that the video is over.
//                                         */
//                                                if (mOnCompletionListener != null) {
//                                                    mOnCompletionListener.onCompletion(mMediaPlayer);
//                                                }
//                                            }
//                                        })
//                                .setCancelable(false)
//                                .show();
//                    }
            return true;
        }
    };


    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };


    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public void start() {
        if (!mPreparedBeforeStart && mMyMediaController != null) {
            mMyMediaController.showLoading();
        }

        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if (this.mIVideoView != null) {
                this.mIVideoView.onVideoStart(mMediaPlayer);
            }
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                if (this.mIVideoView != null) {
                    this.mIVideoView.onVideoPause(mMediaPlayer);
                }
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = pos;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public void closePlayer() {
        KLog.i("-----closePlayer");
        release(true);
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        int screenOrientation = fullscreen ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setFullscreen(fullscreen, screenOrientation);
    }

    @Override
    public void setFullscreen(boolean fullscreen, int screenOrientation) {
        // Activity需要设置为: android:configChanges="keyboardHidden|orientation|screenSize"
        Activity activity = (Activity) mContext;

        if (fullscreen) {
            if (mVideoViewLayoutWidth == 0 && mVideoViewLayoutHeight == 0) {
                ViewGroup.LayoutParams params = getLayoutParams();
                mVideoViewLayoutWidth = params.width;//保存全屏之前的参数
                mVideoViewLayoutHeight = params.height;
            }
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.setRequestedOrientation(screenOrientation);
        } else {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = mVideoViewLayoutWidth;//使用全屏之前的参数
            params.height = mVideoViewLayoutHeight;
            setLayoutParams(params);

            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.setRequestedOrientation(screenOrientation);
        }
        mMyMediaController.toggleButtons(fullscreen);
        if (mIVideoView != null) {
            mIVideoView.onVideoScaleChange(fullscreen);
        }
    }
}
