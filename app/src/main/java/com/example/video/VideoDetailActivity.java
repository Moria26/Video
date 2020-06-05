package com.example.video;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.liteav.demo.play.SuperPlayerConst;
import com.tencent.liteav.demo.play.SuperPlayerGlobalConfig;
import com.tencent.liteav.demo.play.SuperPlayerModel;
import com.tencent.liteav.demo.play.SuperPlayerView;
import com.tencent.rtmp.TXLiveBase;
import com.tencent.rtmp.TXLiveConstants;



public class VideoDetailActivity extends AppCompatActivity implements SuperPlayerView.OnSuperPlayerViewCallback {

    SuperPlayerView mSuperPlayerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSuperPlayerView = findViewById(R.id.main_super_player_view);

        initData();
    }

    protected void initData() {
        initSuperPlayer();
        String url =   getIntent().getStringExtra("url");
        playVideo(url);
    }
    private void playVideo( String url ) {
        TXLiveBase.setConsoleEnabled(true);
        TXLiveBase.setLogLevel(TXLiveConstants.LOG_LEVEL_DEBUG);
        SuperPlayerModel model = new SuperPlayerModel();
        model.url =url;
        mSuperPlayerView.playWithModel(model);

    }


    private void initSuperPlayer() {
        mSuperPlayerView.setPlayerViewCallback(this);
        initSuperVodGlobalSetting();
    }
    private void initSuperVodGlobalSetting() {
        SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
        // 开启悬浮窗播放
        prefs.enableFloatWindow = false;
        // 设置悬浮窗的初始位置和宽高
        SuperPlayerGlobalConfig.TXRect rect = new SuperPlayerGlobalConfig.TXRect();
        rect.x = 0;
        rect.y = 0;
        rect.width = 810;
        rect.height = 540;
        prefs.floatViewRect = rect;
        // 播放器默认缓存个数
        prefs.maxCacheItem = 5;
        // 设置播放器渲染模式
        prefs.enableHWAcceleration = true;
        prefs.renderMode = TXLiveConstants.RENDER_MODE_FULL_FILL_SCREEN;
        //需要修改为自己的时移域名
        prefs.playShiftDomain = "vcloudtimeshift.qcloud.com";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSuperPlayerView.getPlayState() == SuperPlayerConst.PLAYSTATE_PLAYING) {
            mSuperPlayerView.onResume();
            if (mSuperPlayerView.getPlayMode() == SuperPlayerConst.PLAYMODE_FLOAT) {
                mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {

        mSuperPlayerView.release();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.resetPlayer();
        }
        super.onDestroy();
    }
    @Override
    public void onStartFullScreenPlay() {
        Log.e("tag","onStartFullScreenPlay");
    }

    @Override
    public void onStopFullScreenPlay() {
    }

    @Override
    public void onClickFloatCloseBtn() {

    }

    @Override
    public void onClickSmallReturnBtn() {
        finish();
    }

    @Override
    public void onStartFloatWindowPlay() {

    }
}
