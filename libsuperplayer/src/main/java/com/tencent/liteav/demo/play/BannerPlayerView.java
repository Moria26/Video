package com.tencent.liteav.demo.play;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.liteav.demo.play.bean.TCResolutionName;
import com.tencent.liteav.demo.play.bean.TCVideoQuality;
import com.tencent.liteav.demo.play.net.TCLogReport;
import com.tencent.liteav.demo.play.protocol.IPlayInfoProtocol;
import com.tencent.liteav.demo.play.protocol.IPlayInfoRequestCallback;
import com.tencent.liteav.demo.play.protocol.TCPlayInfoParams;
import com.tencent.liteav.demo.play.protocol.TCPlayInfoProtocolV2;
import com.tencent.liteav.demo.play.protocol.TCPlayInfoProtocolV4;
import com.tencent.liteav.demo.play.utils.TCNetWatcher;
import com.tencent.liteav.demo.play.utils.TCUrlUtil;
import com.tencent.liteav.demo.play.utils.TCVideoQualityUtil;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.ITXVodPlayListener;
import com.tencent.rtmp.TXBitrateItem;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXVodPlayConfig;
import com.tencent.rtmp.TXVodPlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;


public class BannerPlayerView extends ConstraintLayout implements ITXVodPlayListener, ITXLivePlayListener, View.OnClickListener {
    private static final String TAG = "SuperPlayerView";

    OnSuperPlayerViewCallback onSuperPlayerViewCallback;
    private Context mContext;
    // UI
    private ViewGroup mRootView;                      // SuperPlayerView的根view
    private TXCloudVideoView mTXCloudVideoView;              // 腾讯云视频播放view


    private SuperPlayerModel mCurrentModel;                  // 当前播放的model
    private IPlayInfoProtocol mCurrentProtocol;               // 当前视频信息协议类

    private TXVodPlayer mVodPlayer;                     // 点播播放器
    private TXVodPlayConfig mVodPlayConfig;                 // 点播播放器配置

    private TCNetWatcher mWatcher;                       // 网络质量监视器
    private String mCurrentPlayVideoURL;           // 当前播放的url
    private int mCurrentPlayType;               // 当前播放类型
    private int mCurrentPlayMode = SuperPlayerConst.PLAYMODE_WINDOW;    // 当前播放模式
    private int mCurrentPlayState = -1; // 当前播放状态
    private boolean mIsMultiBitrateStream;          // 是否是多码流url播放
    private boolean mIsPlayWithFileId;              // 是否是腾讯云fileId播放
    private long mReportLiveStartTime = -1;      // 直播开始时间，用于上报使用时长
    private long mReportVodStartTime = -1;       // 点播开始时间，用于上报使用时长
    private boolean mDefaultQualitySet;             // 标记播放多码流url时是否设置过默认画质
    private boolean mChangeHWAcceleration;          // 切换硬解后接收到第一个关键帧前的标记位
    private int mSeekPos;                       // 记录切换硬解时的播放时间
    private long mMaxLiveProgressTime;           // 观看直播的最大时长
    private ViewGroup.LayoutParams mLayoutParamWindowMode;
    private ViewGroup.LayoutParams mLayoutParamFullScreenMode;

    private ImageView playView;
    private ImageView ivVol;
    private ImageView smallIvBackground;

    public BannerPlayerView(Context context) {
        super(context);
        initView(context);
    }

    public BannerPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public BannerPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    /**
     * 初始化view
     *
     * @param context
     */
    private void initView(Context context) {
        mContext = context;
        mRootView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.super_vod_player_banner, null);
        mTXCloudVideoView =  mRootView.findViewById(R.id.cloud_video_view);
        playView =  mRootView.findViewById(R.id.iv_play);
        smallIvBackground =  mRootView.findViewById(R.id.small_iv_background);
        ivVol =  mRootView.findViewById(R.id.iv_vol);

        ivVol.setOnClickListener(this);
        playView.setOnClickListener(this);
        mTXCloudVideoView.setOnClickListener(this);
        addView(mRootView);
        mTXCloudVideoView.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
        post(new Runnable() {

            @Override
            public void run() {
                if (mCurrentPlayMode == SuperPlayerConst.PLAYMODE_WINDOW) {
                    mLayoutParamWindowMode = getLayoutParams();
                }
                try {
                    // 依据上层Parent的LayoutParam类型来实例化一个新的fullscreen模式下的LayoutParam
                    Class parentLayoutParamClazz = getLayoutParams().getClass();
                    Constructor constructor = parentLayoutParamClazz.getDeclaredConstructor(int.class, int.class);
                    mLayoutParamFullScreenMode = (ViewGroup.LayoutParams) constructor.newInstance(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        TCLogReport.getInstance().setAppName(context);
        TCLogReport.getInstance().setPackageName(context);
    }

    /**
     * 初始化点播播放器
     *
     * @param context
     */
    private void initVodPlayer(Context context) {
        if (mVodPlayer != null)
            return;
        mVodPlayer = new TXVodPlayer(context);
        SuperPlayerGlobalConfig config = SuperPlayerGlobalConfig.getInstance();
        mVodPlayConfig = new TXVodPlayConfig();

        File sdcardDir = context.getExternalFilesDir(null);
        if (sdcardDir != null) {
            mVodPlayConfig.setCacheFolderPath(sdcardDir.getPath() + "/txcache");
        }
        mVodPlayConfig.setMaxCacheItems(config.maxCacheItem);
        mVodPlayer.setConfig(mVodPlayConfig);
        mVodPlayer.setRenderMode(config.renderMode);
        mVodPlayer.setVodListener(this);
        mVodPlayer.enableHardwareDecode(config.enableHWAcceleration);
    }



    public void setModel(SuperPlayerModel model){
        mCurrentModel = model;
    }
    /**
     * 播放视频
     */
    public void play() {
        if (mCurrentModel==null){
            return;
        }
        stopPlay();
        initVodPlayer(getContext());
        TCPlayInfoParams params = new TCPlayInfoParams();
        params.appId = mCurrentModel.appId;
        if (mCurrentModel.videoId != null) {
            params.fileId = mCurrentModel.videoId.fileId;
            params.videoId = mCurrentModel.videoId;
            mCurrentProtocol = new TCPlayInfoProtocolV4(params);
        } else if (mCurrentModel.videoIdV2 != null) {
            params.fileId = mCurrentModel.videoIdV2.fileId;
            params.videoIdV2 = mCurrentModel.videoIdV2;
            mCurrentProtocol = new TCPlayInfoProtocolV2(params);
        }
//        if (mCurrentModel.videoId != null || mCurrentModel.videoIdV2 != null) { // 根据FileId播放
//            mCurrentProtocol.sendRequest(new IPlayInfoRequestCallback() {
//                @Override
//                public void onSuccess(IPlayInfoProtocol protocol, TCPlayInfoParams param) {
//                    TXCLog.i(TAG, "onSuccess: protocol params = " + param.toString());
//                    mReportVodStartTime = System.currentTimeMillis();
//                    mVodPlayer.setPlayerView(mTXCloudVideoView);
//                    playModeVideo(mCurrentProtocol);
//                    updatePlayType(SuperPlayerConst.PLAYTYPE_VOD);
//                }
//
//                @Override
//                public void onError(int errCode, String message) {
//                    playView.setVisibility(VISIBLE);
//                    TXCLog.i(TAG, "onFail: errorCode = " + errCode + " message = " + message);
//                }
//            });
//        } else { // 根据URL播放
            String videoURL = null;
            List<TCVideoQuality> videoQualities = new ArrayList<>();
            if (mCurrentModel.multiURLs != null && !mCurrentModel.multiURLs.isEmpty()) {// 多码率URL播放
                int i = 0;
                for (SuperPlayerModel.SuperPlayerURL superPlayerURL : mCurrentModel.multiURLs) {
                    if (i == mCurrentModel.playDefaultIndex) {
                        videoURL = superPlayerURL.url;
                    }
                    videoQualities.add(new TCVideoQuality(i++, superPlayerURL.qualityName, superPlayerURL.url));
                }
                TCVideoQuality     defaultVideoQuality = videoQualities.get(mCurrentModel.playDefaultIndex);
            } else if (!TextUtils.isEmpty(mCurrentModel.url)) { // 传统URL模式播放
//                videoQualities.add(new TCVideoQuality(0, "", model.url));
//                defaultVideoQuality = videoQualities.get(0);
                videoURL = mCurrentModel.url;
            }

            if (TextUtils.isEmpty(videoURL)) {
                return;
            }
            mReportVodStartTime = System.currentTimeMillis();
            mVodPlayer.setPlayerView(mTXCloudVideoView);
            playVodURL(videoURL);
            updatePlayType(SuperPlayerConst.PLAYTYPE_VOD);
      //  }
    }

    /**
     * 播放FileId视频
     *
     * @param protocol
     */
    private void playModeVideo(IPlayInfoProtocol protocol) {
        playVodURL(protocol.getUrl());
        List<TCVideoQuality> videoQualityArrayList = protocol.getVideoQualityList();
        if (videoQualityArrayList != null) {
            mIsMultiBitrateStream = false;
        } else {
            mIsMultiBitrateStream = true;
        }

    }



    /**
     * 播放点播url
     */
    private void playVodURL(String url) {
        if (url == null || "".equals(url)) return;
        mCurrentPlayVideoURL = url;
        if (url.contains(".m3u8")) {
            mIsMultiBitrateStream = true;
        }
        if (mVodPlayer != null) {
            mDefaultQualitySet = false;
            mVodPlayer.setStartTime(0);
            mVodPlayer.setAutoPlay(true);
            mVodPlayer.setVodListener(this);
            if(mCurrentProtocol!=null && mCurrentProtocol.getToken()!=null){
                TXCLog.d(TAG,"TOKEN: "+mCurrentProtocol.getToken());
                mVodPlayer.setToken(mCurrentProtocol.getToken());
            }
            else {
                mVodPlayer.setToken(null);
            }
            int ret = mVodPlayer.startPlay(url);
            if (ret == 0) {
                mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PLAYING;
            }
        }
        mIsPlayWithFileId = false;
    }









    /**
     * 更新播放类型
     *
     * @param playType
     */
    private void updatePlayType(int playType) {
        mCurrentPlayType = playType;
    }

    /**
     * 更新播放状态
     *
     * @param playState
     */
    private void updatePlayState(int playState) {
        mCurrentPlayState = playState;

    }
    /**
     * resume生命周期回调
     */
    public void onResume() {
        resume();
    }
    private void resume() {
        if (mCurrentPlayType == SuperPlayerConst.PLAYTYPE_VOD && mVodPlayer != null) {
            mVodPlayer.resume();
        }
    }
    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.cloud_video_view){
            if (getPlayState()==SuperPlayerConst.PLAYSTATE_PLAYING){
                mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PLAYING;
                onSuperPlayerViewCallback.onPlayPause();
                playView.setVisibility(VISIBLE);
                ivVol.setVisibility(GONE);
            }else{
                mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PAUSE;
                onSuperPlayerViewCallback.onPlayResume();
                playView.setVisibility(GONE);
                ivVol.setVisibility(VISIBLE);
            }

        } else if (view.getId()==R.id.iv_play){
            if (getPlayState()==-1){
                onSuperPlayerViewCallback.onPlay();
                playView.setVisibility(GONE);
                ivVol.setVisibility(VISIBLE);
            }else if (getPlayState()==SuperPlayerConst.PLAYSTATE_PLAYING){
                mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PAUSE;
                onSuperPlayerViewCallback.onPlayPause();
                playView.setVisibility(VISIBLE);
                ivVol.setVisibility(GONE);
            }else{
                mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PLAYING;
                onSuperPlayerViewCallback.onPlayResume();
                playView.setVisibility(GONE);
                ivVol.setVisibility(VISIBLE);
            }

        }else if (view.getId()==R.id.iv_vol){
            onSuperPlayerViewCallback.onVol();
        }
    }

    public void onVolChange(boolean mute) {
        if (mute){
            ivVol.setImageResource(R.drawable.jingyin_no);
        }else{
            ivVol.setImageResource(R.drawable.jingyin);
        }
    }
    public void setImage(String img) {
        smallIvBackground.setVisibility(VISIBLE);
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.default_img)
                .fallback(R.drawable.default_img)
                .error(R.drawable.default_img);
        Glide.with(mContext).load(img).apply(options).into(smallIvBackground);
    }

    /**
     * SuperPlayerView的回调接口
     */
    public interface OnSuperPlayerViewCallback {
        /**
         * 开始按钮
         */
        void onPlay();
        void onPlayResume();
        /**
         * 点击暂停
         */
        void onPlayPause();


        void onVol();
    }
    /**
     * pause生命周期回调
     */
    public void onPause() {
        mCurrentPlayState = SuperPlayerConst.PLAYSTATE_PAUSE;
        playView.setVisibility(VISIBLE);
        ivVol.setVisibility(GONE);
        pause();
    }


    private void pause() {
        if (mCurrentPlayType == SuperPlayerConst.PLAYTYPE_VOD && mVodPlayer != null) {
            mVodPlayer.pause();
        }
    }

    /**
     * 重置播放器
     */
    public void resetPlayer() {

        stopPlay();
    }

    /**
     * 停止播放
     */
    private void stopPlay() {
        if (mVodPlayer != null) {
            mVodPlayer.setVodListener(null);
            mVodPlayer.stopPlay(false);
        }

        if (mWatcher != null) {
            mWatcher.stop();
        }
        reportPlayTime();
    }

    /**
     * 上报播放时长
     */
    private void reportPlayTime() {
        if (mReportLiveStartTime != -1) {
            long reportEndTime = System.currentTimeMillis();
            long diff = (reportEndTime - mReportLiveStartTime) / 1000;
            TCLogReport.getInstance().uploadLogs(TCLogReport.ELK_ACTION_LIVE_TIME, diff, 0);
            mReportLiveStartTime = -1;
        }
        if (mReportVodStartTime != -1) {
            long reportEndTime = System.currentTimeMillis();
            long diff = (reportEndTime - mReportVodStartTime) / 1000;
            TCLogReport.getInstance().uploadLogs(TCLogReport.ELK_ACTION_VOD_TIME, diff, mIsPlayWithFileId ? 1 : 0);
            mReportVodStartTime = -1;
        }
    }



    /**
     * 点播播放器回调
     *
     * 具体可参考官网文档：https://cloud.tencent.com/document/product/881/20216
     *
     * @param player
     * @param event  事件id.id类型请参考 {@linkplain TXLiveConstants#PLAY_EVT_CONNECT_SUCC 播放事件列表}.
     * @param param
     */
    /**
     * 点播播放器回调
     * <p>
     * 具体可参考官网文档：https://cloud.tencent.com/document/product/881/20216
     *
     * @param player
     * @param event  事件id.id类型请参考 {@linkplain TXLiveConstants#PLAY_EVT_CONNECT_SUCC 播放事件列表}.
     * @param param
     */
    @Override
    public void onPlayEvent(TXVodPlayer player, int event, Bundle param) {
        if (event != TXLiveConstants.PLAY_EVT_PLAY_PROGRESS) {
            String playEventLog = "TXVodPlayer onPlayEvent event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
            TXCLog.d(TAG, playEventLog);
        }
        switch (event) {
            case TXLiveConstants.PLAY_EVT_VOD_PLAY_PREPARED://视频播放开始
                hideBackground();
                updatePlayState(SuperPlayerConst.PLAYSTATE_PLAYING);
                playView.setVisibility(GONE);
                if (mIsMultiBitrateStream) {
                    List<TXBitrateItem> bitrateItems = mVodPlayer.getSupportedBitrates();
                    if (bitrateItems == null || bitrateItems.size() == 0)
                        return;
                    Collections.sort(bitrateItems); //masterPlaylist多清晰度，按照码率排序，从低到高
                    List<TCVideoQuality> videoQualities = new ArrayList<>();
                    int size = bitrateItems.size();

                    List<TCResolutionName> resolutionNames = (mCurrentProtocol!=null) ? mCurrentProtocol.getResolutionNameList() : null;
                    for (int i = 0; i < size; i++) {
                        TXBitrateItem bitrateItem = bitrateItems.get(i);
                        TCVideoQuality quality;
                        if (resolutionNames != null) {
                            quality = TCVideoQualityUtil.convertToVideoQuality(bitrateItem, mCurrentProtocol.getResolutionNameList());
                        } else {
                            quality = TCVideoQualityUtil.convertToVideoQuality(bitrateItem, i);
                        }
                        videoQualities.add(quality);
                    }
                    if (!mDefaultQualitySet) {
                        mVodPlayer.setBitrateIndex(bitrateItems.get(bitrateItems.size() - 1).index); //默认播放码率最高的
                        mDefaultQualitySet = true;
                    }
                }
                break;
            case TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME:
                if (mChangeHWAcceleration) { //切换软硬解码器后，重新seek位置
                    TXCLog.i(TAG, "seek pos:" + mSeekPos);
                    mChangeHWAcceleration = false;
                }
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_END:
                updatePlayState(SuperPlayerConst.PLAYSTATE_END);
                onSuperPlayerViewCallback.onPlay();
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_PROGRESS:
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_BEGIN: {
                updatePlayState(SuperPlayerConst.PLAYSTATE_PLAYING);
                break;
            }
            default:
                break;
        }
        if (event < 0) {// 播放点播文件失败
            mVodPlayer.stopPlay(true);
            playView.setVisibility(VISIBLE);
            mCurrentPlayState = -1;
        }

    }

    private void hideBackground() {
        smallIvBackground.setVisibility(GONE);
    }


    @Override
    public void onNetStatus(TXVodPlayer player, Bundle status) {

    }

    /**
     * 直播播放器回调
     * <p>
     * 具体可参考官网文档：https://cloud.tencent.com/document/product/881/20217
     *
     * @param event 事件id.id类型请参考 {@linkplain TXLiveConstants#PUSH_EVT_CONNECT_SUCC 播放事件列表}.
     * @param param
     */
    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (event != TXLiveConstants.PLAY_EVT_PLAY_PROGRESS) {
            String playEventLog = "TXLivePlayer onPlayEvent event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
            TXCLog.d(TAG, playEventLog);
        }
        switch (event) {
            case TXLiveConstants.PLAY_EVT_VOD_PLAY_PREPARED: //视频播放开始
                updatePlayState(SuperPlayerConst.PLAYSTATE_PLAYING);

                break;
            case TXLiveConstants.PLAY_EVT_PLAY_BEGIN:
                updatePlayState(SuperPlayerConst.PLAYSTATE_PLAYING);
                if (mWatcher != null) mWatcher.exitLoading();
                break;
            case TXLiveConstants.PLAY_ERR_NET_DISCONNECT:
            case TXLiveConstants.PLAY_EVT_PLAY_END:
                if (mCurrentPlayType == SuperPlayerConst.PLAYTYPE_LIVE_SHIFT) {  // 直播时移失败，返回直播
                    Toast.makeText(mContext, "时移失败,返回直播", Toast.LENGTH_SHORT).show();
                    updatePlayState(SuperPlayerConst.PLAYSTATE_PLAYING);
                } else {
                    stopPlay();
                    updatePlayState(SuperPlayerConst.PLAYSTATE_END);
                    if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT) {
                        Toast.makeText(mContext, "网络不给力,点击重试", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_LOADING:
            case TXLiveConstants.PLAY_WARNING_RECONNECT:
                updatePlayState(SuperPlayerConst.PLAYSTATE_LOADING);
                if (mWatcher != null) mWatcher.enterLoading();
                break;
            case TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME:
                break;
            case TXLiveConstants.PLAY_EVT_STREAM_SWITCH_SUCC:
                Toast.makeText(mContext, "清晰度切换成功", Toast.LENGTH_SHORT).show();
                break;
            case TXLiveConstants.PLAY_ERR_STREAM_SWITCH_FAIL:
                Toast.makeText(mContext, "清晰度切换失败", Toast.LENGTH_SHORT).show();
                break;
            case TXLiveConstants.PLAY_EVT_PLAY_PROGRESS:
                int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS_MS);
                mMaxLiveProgressTime = progress > mMaxLiveProgressTime ? progress : mMaxLiveProgressTime;
                break;
            default:
                break;
        }
    }


    @Override
    public void onNetStatus(Bundle status) {

    }




    /**
     * 获取当前播放模式
     *
     * @return
     */
    public int getPlayMode() {
        return mCurrentPlayMode;
    }

    /**
     * 获取当前播放状态
     *
     * @return
     */
    public int getPlayState() {
        return mCurrentPlayState;
    }

    public void setPlayerViewCallback(OnSuperPlayerViewCallback onSuperPlayerViewCallback) {
        this. onSuperPlayerViewCallback =  onSuperPlayerViewCallback;
    }



    public void release() {

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            release();
        } catch (Exception e) {
            TXCLog.e(TAG, Log.getStackTraceString(e));
        } catch (Error e) {
            TXCLog.e(TAG, Log.getStackTraceString(e));
        }
    }
}
