package com.example.administrator.myvideodemo.videoplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.administrator.myvideodemo.R;
import com.example.administrator.myvideodemo.util.KLog;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Administrator on 9/4/2016.
 */
public class MyMediaController extends FrameLayout implements View.OnClickListener {
    private static final String TAG = "MyMediaController";

    private RelativeLayout rl_control_root;
    private LinearLayout ll_loading;
    private LinearLayout ll_error;
    private LinearLayout ll_title_bar;
    private ImageButton bt_back;
    private TextView tv_title;
    private ImageView iv_center_play;
    private LinearLayout ll_control_bar;
    private ImageButton bt_switch;
    private ImageButton bt_scale;
    private TextView tv_has_played;
    private SeekBar sb_progress;
    private TextView tv_duration;

    private static final int CONTROL_BAR_DISMISS_INTERVAL = 5000;  //controlbar无操作消失时间

    private MyHandler mHandler;
    private IMyMediaControl mIMyMediaControl;

    private static final int ACTION_SHOW_CONTROL_BAR = 9;
    private static final int ACTION_HIDE_CONTROL_BAR = 10;
    private static final int ACTION_SHOW_LOADDING = 11;
    private static final int ACTION_HIDE_LOADDING = 12;
    private static final int ACTION_SHOW_COMPLETE = 14;
    private static final int ACTION_SHOW_ERROR = 15;
    private static final int ACTION_HIDE_CENTER = 16;
    private static final int ACTION_SHOW_PROGRESS = 17;

    private boolean mScalable = true;
    private boolean mIsFullScreen = true;
    private boolean mIsControlBarShow = false;
    private boolean mDragging;
    private Formatter mFormatter;
    private StringBuilder mFormatBuilder;


    public MyMediaController(Context context) {
        this(context, null);
    }

    public MyMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyMediaController);
        mScalable = a.getBoolean(R.styleable.MyMediaController_scalable, false);
        a.recycle();
        mHandler = new MyHandler();
        initController(context);
    }

    private void initController(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewRoot = inflater.inflate(R.layout.layout_media_controller, this);

        rl_control_root = (RelativeLayout) viewRoot.findViewById(R.id.rl_root);
        ll_loading = (LinearLayout) viewRoot.findViewById(R.id.ll_loading);
        ll_error = (LinearLayout) viewRoot.findViewById(R.id.ll_error);
        ll_title_bar = (LinearLayout) viewRoot.findViewById(R.id.ll_title_bar);
        bt_back = (ImageButton) viewRoot.findViewById(R.id.bt_back);
        tv_title = (TextView) viewRoot.findViewById(R.id.tv_title);
        iv_center_play = (ImageView) viewRoot.findViewById(R.id.iv_center_play);
        ll_control_bar = (LinearLayout) viewRoot.findViewById(R.id.ll_control_bar);
        bt_switch = (ImageButton) viewRoot.findViewById(R.id.bt_switch);
        bt_scale = (ImageButton) viewRoot.findViewById(R.id.bt_scale);
        tv_has_played = (TextView) viewRoot.findViewById(R.id.tv_has_played);
        sb_progress = (SeekBar) viewRoot.findViewById(R.id.sb_progress);
        tv_duration = (TextView) viewRoot.findViewById(R.id.tv_duration);
        bt_scale = (ImageButton) viewRoot.findViewById(R.id.bt_scale);

        rl_control_root.setOnClickListener(this);

        //重新开始播放
        iv_center_play.setOnClickListener(this);
        bt_switch.setOnClickListener(this);
        bt_scale.setOnClickListener(this);
        bt_back.setOnClickListener(this);

        sb_progress.setMax(1000);
        sb_progress.setOnSeekBarChangeListener(mSeekListener);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    }

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        int newPosition = 0;

        boolean change = false;

        public void onStartTrackingTouch(SeekBar bar) {
            if (mIMyMediaControl == null) {
                return;
            }
            showControlBar(3600000);

            mDragging = true;
            mHandler.removeMessages(ACTION_SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mIMyMediaControl == null || !fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mIMyMediaControl.getDuration();
            long newposition = (duration * progress) / 1000L;
            newPosition = (int) newposition;
            change = true;
        }

        public void onStopTrackingTouch(SeekBar bar) {
            if (mIMyMediaControl == null) {
                return;
            }
            if (change) {
                mIMyMediaControl.seekTo(newPosition);
//                if (mCurrentTime != null) {
//                    mCurrentTime.setText(stringForTime(newPosition));
//                }
            }
            mDragging = false;
            setProgress();
            updateSwitch();
            showControlBar(-1);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mIsControlBarShow = true;
            mHandler.sendEmptyMessage(ACTION_SHOW_PROGRESS);
        }
    };

    // TODO: 9/4/2016   暂时先不处理内存泄漏
    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int pos;
            switch (msg.what) {
                case ACTION_SHOW_CONTROL_BAR:
                    showControlBar();
                    break;
                case ACTION_HIDE_CONTROL_BAR:
                    hideControlBar();
                    break;
                case ACTION_HIDE_LOADDING:
                    showCenterView(ACTION_HIDE_LOADDING);
                    break;
                case ACTION_SHOW_LOADDING:
                    showCenterView(ACTION_SHOW_LOADDING);
                    break;
                case ACTION_SHOW_COMPLETE:
                    showCenterView(ACTION_SHOW_COMPLETE);
                    updateSwitch();
                    sb_progress.setProgress(0);
                    tv_has_played.setText("00:00");
                    break;
                case ACTION_SHOW_ERROR:
                    updateSwitch();
                    showCenterView(ACTION_SHOW_ERROR);
                    break;
                case ACTION_HIDE_CENTER:
                    showCenterView(ACTION_HIDE_CENTER);
                    break;

                case ACTION_SHOW_PROGRESS: //2
                    pos = setProgress();
                    if (!mDragging && mIsControlBarShow && mIMyMediaControl != null && mIMyMediaControl.isPlaying()) {
                        msg = obtainMessage(ACTION_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }

    }

    private void showCenterView(int action) {
        switch (action) {
            case ACTION_HIDE_LOADDING:
                ll_loading.setVisibility(GONE);
                ll_error.setVisibility(GONE);
                iv_center_play.setVisibility(GONE);
                break;
            case ACTION_SHOW_LOADDING:
                ll_loading.setVisibility(VISIBLE);
                ll_error.setVisibility(GONE);
                iv_center_play.setVisibility(GONE);
                break;
            case ACTION_SHOW_COMPLETE:
                ll_loading.setVisibility(GONE);
                ll_error.setVisibility(GONE);
                iv_center_play.setVisibility(VISIBLE);
                break;
            case ACTION_SHOW_ERROR:
                ll_loading.setVisibility(GONE);
                ll_error.setVisibility(VISIBLE);
                iv_center_play.setVisibility(GONE);
                break;
            case ACTION_HIDE_CENTER:
                ll_loading.setVisibility(GONE);
                ll_error.setVisibility(GONE);
                iv_center_play.setVisibility(GONE);
                break;
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_root:
                if (mIsControlBarShow) {
                    mHandler.removeCallbacksAndMessages(ACTION_SHOW_CONTROL_BAR);
                    mHandler.sendEmptyMessage(ACTION_HIDE_CONTROL_BAR);
                } else {
                    mHandler.sendEmptyMessage(ACTION_SHOW_CONTROL_BAR);
                }
                break;
            case R.id.iv_center_play:
                mHandler.sendEmptyMessage(ACTION_HIDE_CENTER);
                mIMyMediaControl.start();
                updateSwitch();
                break;
            case R.id.bt_back:
                if (mIsFullScreen) {
                    mIsFullScreen = false;
                    updateScaleButton();
                    updateBackButton();
                    mIMyMediaControl.setFullscreen(false);
                }
            case R.id.bt_switch:
                if (mIMyMediaControl.isPlaying()) {
                    if (mIMyMediaControl.canPause()) {
                        mIMyMediaControl.pause();
                    }
                } else {
                    mIMyMediaControl.start();
                }
                updateSwitch();
                break;
        }
    }


    public void setMediaPlayer(IMyMediaControl control) {
        mIMyMediaControl = control;
        updateSwitch();
    }


    //更新播放按钮
    private void updateSwitch() {
        if (mIMyMediaControl != null && mIMyMediaControl.isPlaying()) {
            bt_switch.setImageResource(R.drawable.uvv_stop_btn);
        } else {
            bt_switch.setImageResource(R.drawable.uvv_player_player_btn);
        }
    }

    //只负责上下两条bar的隐藏,不负责中央loading,error,playBtn的隐藏
    public void hideControlBar() {
        KLog.i(TAG, "------hideControlBar");
        ll_title_bar.setVisibility(GONE);
        ll_control_bar.setVisibility(GONE);
        mIsControlBarShow = false;

    }

    public void showControlBar() {
        showControlBar(-1);
    }

    public void showControlBar(int interval) {
        KLog.i(TAG, "------showControlBar");


        if (!mIsControlBarShow) {
            setProgress();
            if (bt_switch != null) {
                bt_switch.requestFocus();
            }
//            disableUnsupportedButtons();
            mIsControlBarShow = true;
        }
        updateSwitch();
        updateBackButton();
        ll_title_bar.setVisibility(VISIBLE);
        ll_control_bar.setVisibility(VISIBLE);
        mIsControlBarShow = true;
        mHandler.removeMessages(ACTION_HIDE_CONTROL_BAR);
        mHandler.sendEmptyMessageDelayed(ACTION_HIDE_CONTROL_BAR, interval >= 0 ? interval : CONTROL_BAR_DISMISS_INTERVAL);
    }

    private int setProgress() {
        if (mIMyMediaControl == null || mDragging) {
            return 0;
        }
        int position = mIMyMediaControl.getCurrentPosition();
        int duration = mIMyMediaControl.getDuration();
        if (sb_progress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                sb_progress.setProgress((int) pos);
            }
            int percent = mIMyMediaControl.getBufferPercentage();
            sb_progress.setSecondaryProgress(percent * 10);
        }

        if (tv_has_played != null)
            tv_has_played.setText(stringForTime(position));
        if (tv_duration != null)
            tv_duration.setText(stringForTime(duration));

        return position;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public void showLoading() {
        mHandler.sendEmptyMessage(ACTION_SHOW_LOADDING);
    }

    public void hideLoading() {
        mHandler.sendEmptyMessage(ACTION_HIDE_LOADDING);
    }

    public void showComplete() {
        mHandler.sendEmptyMessage(ACTION_HIDE_LOADDING);
    }

    public void showError() {
        mHandler.sendEmptyMessage(ACTION_SHOW_ERROR);
    }

    public void toggleButtons(boolean isFullScreen) {
        mIsFullScreen = isFullScreen;
        updateScaleButton();
        updateBackButton();
    }

    public void updateScaleButton() {
        if (mIsFullScreen) {
            bt_scale.setImageResource(R.drawable.uvv_star_zoom_in);
        } else {
            bt_scale.setImageResource(R.drawable.ic_scale);
        }
    }

    public void updateBackButton() {
        bt_back.setVisibility(mIsFullScreen ? View.VISIBLE : View.INVISIBLE);
    }
}
