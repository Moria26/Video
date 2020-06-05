package com.tencent.liteav.demo.play.controller;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.liteav.demo.play.R;
import com.tencent.liteav.demo.play.SuperPlayerConst;
import com.tencent.liteav.demo.play.bean.TCPlayImageSpriteInfo;
import com.tencent.liteav.demo.play.bean.TCPlayKeyFrameDescInfo;
import com.tencent.liteav.demo.play.bean.TCVideoQuality;
import com.tencent.liteav.demo.play.utils.TCTimeUtil;
import com.tencent.liteav.demo.play.utils.TCVideoGestureUtil;
import com.tencent.liteav.demo.play.view.TCPointSeekBar;
import com.tencent.liteav.demo.play.view.TCVideoProgressLayout;
import com.tencent.liteav.demo.play.view.TCVolumeBrightnessProgressLayout;

import java.util.List;


public class TCControllerBanner extends RelativeLayout implements IController, View.OnClickListener,
        TCPointSeekBar.OnSeekBarChangeListener {

    // UI控件
    private LinearLayout                        mLayoutTop;                             // 顶部标题栏布局
    private ImageView                           mBackground;                            // 背景
    private ProgressBar                         mPbLiveLoading;                         // 加载圈

    private IControllerCallback                 mControllerCallback;                    // 播放控制回调
    private HideViewControllerViewRunnable      mHideViewRunnable;                      // 隐藏控件子线程

    private boolean                             isShowing;                              // 自身是否可见
    private int                                 mPlayType;                              // 当前播放视频类型
    private int                                 mCurrentPlayState = -1;                 // 当前播放状态
    private long                                mDuration;                              // 视频总时长
    private long                                mLivePushDuration;                      // 直播推流总时长
    private long                                mProgress;                              // 当前播放进度
    private Bitmap                              mBackgroundBmp;                         // 背景图
    private long                                mLastClickTime;                         // 上次点击事件的时间
    private View controller;
    private ImageView ivBigPlay;
    private ImageView ivVol;

    public TCControllerBanner(Context context) {
        super(context);
        init(context);
    }

    public TCControllerBanner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TCControllerBanner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    /**
     * 初始化控件、手势检测监听器、亮度/音量/播放进度的回调
     */
    private void init(Context context) {
        initView(context);
        mHideViewRunnable = new HideViewControllerViewRunnable(this);


    }

    /**
     * 初始化view
     */
    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.vod_controller_banner, this);

        mLayoutTop = (LinearLayout) findViewById(R.id.layout_top);
        ivBigPlay = (ImageView) findViewById(R.id.iv_big_play);
        ivVol = (ImageView) findViewById(R.id.iv_vol);
        controller =  findViewById(R.id.controller);
        mPbLiveLoading = (ProgressBar) findViewById(R.id.pb_live);

        controller.setOnClickListener(this);
        ivBigPlay.setOnClickListener(this);
        ivVol.setOnClickListener(this);
        mBackground = (ImageView)findViewById(R.id.small_iv_background);
        setBackground(mBackgroundBmp);

    }

    /**
     * 切换播放状态
     *
     * 双击和点击播放/暂停按钮会触发此方法
     */
    private void togglePlayState() {
        switch (mCurrentPlayState) {
            case SuperPlayerConst.PLAYSTATE_PAUSE:
                if (mControllerCallback != null) {
                    mControllerCallback.onResume();
                }
                show();
                break;
            case SuperPlayerConst.PLAYSTATE_END:
                if (mControllerCallback != null) {
                    mControllerCallback.onResume();
                }
                break;
            case SuperPlayerConst.PLAYSTATE_PLAYING:
            case SuperPlayerConst.PLAYSTATE_LOADING:
                if (mControllerCallback != null) {
                    mControllerCallback.onPause();
                }
                hide();
                break;
        }
    }

    /**
     * 切换自身的可见性
     */
    private void toggle() {
        if (isShowing) {
            hide();
        } else {
            show();
            if (mHideViewRunnable != null) {
                getHandler().removeCallbacks(mHideViewRunnable);
                getHandler().postDelayed(mHideViewRunnable, 7000);
            }
        }
    }

    /**
     * 设置回调
     *
     * @param callback 回调接口实现对象
     */
    @Override
    public void setCallback(IControllerCallback callback) {
        mControllerCallback = callback;
    }

    /**
     * 设置水印
     *
     * @param bmp 水印图
     * @param x   水印的x坐标
     * @param y   水印的y坐标
     */
    @Override
    public void setWatermark(final Bitmap bmp, float x, float y) {

    }

    /**
     * 显示控件
     */
    @Override
    public void show() {
        isShowing = true;
        mLayoutTop.setVisibility(View.VISIBLE);
        ivBigPlay.setVisibility(View.GONE);
        if (mPlayType == SuperPlayerConst.PLAYTYPE_LIVE_SHIFT) {
        }
    }

    /**
     * 隐藏控件
     */
    @Override
    public void hide() {
        isShowing = false;
        mLayoutTop.setVisibility(View.GONE);
        ivBigPlay.setVisibility(VISIBLE);
        if (mPlayType == SuperPlayerConst.PLAYTYPE_LIVE_SHIFT) {
        }
    }

    /**
     * 释放控件的内存
     */
    @Override
    public void release() {
    }

    /**
     * 更新播放状态
     *
     * @param playState 正在播放{@link SuperPlayerConst#PLAYSTATE_PLAYING}
     *                  正在加载{@link SuperPlayerConst#PLAYSTATE_LOADING}
     *                  暂停   {@link SuperPlayerConst#PLAYSTATE_PAUSE}
     *                  播放结束{@link SuperPlayerConst#PLAYSTATE_END}
     */
    @Override
    public void updatePlayState(int playState) {
        switch (playState) {
            case SuperPlayerConst.PLAYSTATE_PLAYING:
                toggleView(mPbLiveLoading, false);
                break;
            case SuperPlayerConst.PLAYSTATE_LOADING:
                toggleView(mPbLiveLoading, true);
                break;
            case SuperPlayerConst.PLAYSTATE_PAUSE:
                toggleView(mPbLiveLoading, false);
                break;
            case SuperPlayerConst.PLAYSTATE_END:
                toggleView(mPbLiveLoading, false);
                break;
        }
        mCurrentPlayState = playState;
    }

    /**
     * 设置视频画质信息
     *
     * @param list 画质列表
     */
    @Override
    public void setVideoQualityList(List<TCVideoQuality> list) {
    }

    /**
     * 更新视频名称
     *
     * @param title 视频名称
     */
    @Override
    public void updateTitle(String title) {
    }

    /**
     * 更新视频播放进度
     *
     * @param current  当前进度(秒)
     * @param duration 视频总时长(秒)
     */
    @Override
    public void updateVideoProgress(long current, long duration) {
        mProgress = current < 0 ? 0 : current;
        mDuration = duration < 0 ? 0 : duration;

    }

    /**
     * 更新播放类型
     *
     * @param type 点播     {@link SuperPlayerConst#PLAYTYPE_VOD}
     *             点播     {@link SuperPlayerConst#PLAYTYPE_LIVE}
     *             直播回看  {@link SuperPlayerConst#PLAYTYPE_LIVE_SHIFT}
     */
    @Override
    public void updatePlayType(int type) {
        mPlayType = type;
    }

    /**
     * 设置背景
     *
     * @param bitmap 背景图
     */
    @Override
    public void setBackground(final Bitmap bitmap) {
        this.post(new Runnable() {
            @Override
            public void run() {
                if (bitmap == null) return;
                if (mBackground == null) {
                    mBackgroundBmp = bitmap;
                } else {
                    setBitmap(mBackground, mBackgroundBmp);
                }
            }
        });
    }

    /**
     * 设置目标ImageView显示的图片
     */
    private void setBitmap(ImageView view, Bitmap bitmap) {
        if (view == null || bitmap == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(new BitmapDrawable(getContext().getResources(), bitmap));
        } else {
            view.setBackgroundDrawable(new BitmapDrawable(getContext().getResources(), bitmap));
        }
    }

    /**
     * 显示背景
     */
    @Override
    public void showBackground() {
        this.post(new Runnable() {
            @Override
            public void run() {
                ValueAnimator alpha = ValueAnimator.ofFloat(0.0f, 1);
                alpha.setDuration(500);
                alpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (Float) animation.getAnimatedValue();
                        mBackground.setAlpha(value);
                        if (value == 1) {
                            mBackground.setVisibility(VISIBLE);
                        }
                    }
                });
                alpha.start();
            }
        });
    }

    /**
     * 隐藏背景
     */
    @Override
    public void hideBackground() {
        this.post(new Runnable() {
            @Override
            public void run() {
                if (mBackground.getVisibility() != View.VISIBLE) return;
                ValueAnimator alpha = ValueAnimator.ofFloat(1.0f, 0.0f);
                alpha.setDuration(500);
                alpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (Float) animation.getAnimatedValue();
                        mBackground.setAlpha(value);
                        if (value == 0) {
                            mBackground.setVisibility(GONE);
                        }
                    }
                });
                alpha.start();
            }
        });
    }

    /**
     * 更新视频播放画质
     *
     * @param videoQuality 画质
     */
    @Override
    public void updateVideoQuality(TCVideoQuality videoQuality) {

    }

    /**
     * 更新雪碧图信息
     *
     * @param info 雪碧图信息
     */
    @Override
    public void updateImageSpriteInfo(TCPlayImageSpriteInfo info) {

    }

    /**
     * 更新关键帧信息
     *
     * @param list 关键帧信息列表
     */
    @Override
    public void updateKeyFrameDescInfo(List<TCPlayKeyFrameDescInfo> list) {

    }

    /**
     * 重写触摸事件监听，实现手势调节亮度、音量以及播放进度
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return true;
    }

    /**
     * 设置点击事件监听
     */
    @Override
    public void onClick(View view) {
        if (System.currentTimeMillis() - mLastClickTime < 300) { //限制点击频率
            return;
        }
        mLastClickTime = System.currentTimeMillis();
        int id = view.getId();
        if (id == R.id.controller) { //暂停\播放按钮
            togglePlayState();
        } else if (id == R.id.iv_big_play) { //暂停\播放按钮
            togglePlayState();
        }else if (id == R.id.iv_vol) { //
        }
    }

    @Override
    public void onProgressChanged(TCPointSeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(TCPointSeekBar seekBar) {
        this.getHandler().removeCallbacks(mHideViewRunnable);
    }

    @Override
    public void onStopTrackingTouch(TCPointSeekBar seekBar) {
        int curProgress = seekBar.getProgress();
        int maxProgress = seekBar.getMax();

        switch (mPlayType) {
            case SuperPlayerConst.PLAYTYPE_VOD:
                if (curProgress >= 0 && curProgress <= maxProgress) {
                    // 关闭重播按钮
                    float percentage = ((float) curProgress) / maxProgress;
                    int position = (int) (mDuration * percentage);
                    if (mControllerCallback != null) {
                        mControllerCallback.onSeekTo(position);
                        mControllerCallback.onResume();
                    }
                }
                break;
            case SuperPlayerConst.PLAYTYPE_LIVE:
            case SuperPlayerConst.PLAYTYPE_LIVE_SHIFT:
                toggleView(mPbLiveLoading, true);
                int seekTime = (int) (mLivePushDuration * curProgress * 1.0f / maxProgress);
                if (mLivePushDuration > SuperPlayerConst.MAX_SHIFT_TIME) {
                    seekTime = (int) (mLivePushDuration - SuperPlayerConst.MAX_SHIFT_TIME *  (maxProgress - curProgress) * 1.0f / maxProgress);
                }
                if (mControllerCallback != null) {
                    mControllerCallback.onSeekTo(seekTime);
                }
                break;
        }
        this.getHandler().postDelayed(mHideViewRunnable, 7000);
    }

    /**
     * 设置控件的可见性
     *
     * @param view      目标控件
     * @param isVisible 显示：true 隐藏：false
     */
    private void toggleView(final View view, final boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
