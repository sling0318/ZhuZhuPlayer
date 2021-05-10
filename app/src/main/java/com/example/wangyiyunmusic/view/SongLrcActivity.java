package com.example.wangyiyunmusic.view;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.wangyiyunmusic.R;
import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.customeView.MarqueeTextView;
import com.example.wangyiyunmusic.present.MusicPresent;
import com.example.wangyiyunmusic.service.MusicService;
import com.example.wangyiyunmusic.view.base.BaseActivity;

import java.security.acl.Group;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class SongLrcActivity extends BaseActivity<MusicPresent,LocalMusicView> implements LocalMusicView{

    private static final String TAG = "lrc000";

    private MarqueeTextView tv_song;
    private TextView tv_singer,tv_seekBar_start,tv_seekBar_end,tv_get_search;
    private ImageView iv_play_toggle,iv_center_album,iv_center_album_2,iv_loading,iv_comment,iv_more,iv_share,
            iv_blur_bg,iv_play_mode,iv_love,iv_yest,iv_download,sb_sound;
    private SeekBar sb_songProgress,sb_volume;
    private Group mLrcGroup;
    private ConstraintLayout csl_center_layout,mLayoutUIRoot;
    //    绑定音乐服务
    private MyConn mMyConn;
    private Intent myServiceIntent;
    private MusicService mMusicService;
    //
    private MyViewClickListener mClickListener;
    private SongProgressListener mProgressListener;
    private MusicVolumeListener mVolumeListener;
    private SimpleDateFormat mDateFormat;
    private boolean isSeekBarChanging = false;
    private Timer mTimerProgressAndVolume;
    private static final int UPDATE_UI = 0;

    //hander更新ui
    private Handler handler=new Handler(){

        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d("刷新", "::handleMessage 刷新");
            switch (msg.what){
                case UPDATE_UI:
                    SyncMusicInformation();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_lrc);
        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.bindService(myServiceIntent,mMyConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.sendEmptyMessage(UPDATE_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unbindService(mMyConn);
        if (mTimerProgressAndVolume != null) {
            mTimerProgressAndVolume.purge();
            mTimerProgressAndVolume.cancel();
            mTimerProgressAndVolume = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseInterface();
        releaseUI();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        DisplayMetrics dm = new DisplayMetrics();
        Objects.requireNonNull(getWindowManager()).getDefaultDisplay().getMetrics(dm);
        if(iv_center_album.getWidth() < dm.widthPixels * 0.45){
            ViewGroup.LayoutParams params = iv_center_album.getLayoutParams();
            params.width = (int)(dm.widthPixels * 0.45 + 1);
            params.height = (int)(dm.widthPixels * 0.45 + 1);
            iv_center_album.setLayoutParams(params);
        }if(iv_center_album_2.getWidth() < dm.widthPixels * 0.7){
            ViewGroup.LayoutParams params = iv_center_album_2.getLayoutParams();
            params.width = (int)(dm.widthPixels * 0.7 + 1);
            params.height = (int)(dm.widthPixels * 0.7 + 1);
            iv_center_album_2.setLayoutParams(params);
        }
    }

    @Override
    protected void receiveNextPlay(String Action) {
        if (mMusicService == null) return;
        switch (Action) {
            case "play/pause":
                clickBottomPlay();
                if(!mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() > 0){
                    Log.d(TAG, "receiveNextPlay: 更新通知栏");
                    mMusicService.UpdateNotification(true); }
                break;
            case "next":
            case "previous":
                if (!mMusicService.isOnCompleted())
                    mMusicService.OnNextPlay(Action);
                else mMusicService.setOnCompleted(false);
                break;
            case "love":
                break;
            case "close":
                mMusicService.OnCloseNotification();
                iv_play_toggle.setImageResource(R.drawable.iv_play);
                break;
            case "update":
                if (mMusicService.isPlaying()) iv_play_toggle.setImageResource(R.drawable.iv_pause);
                else iv_play_toggle.setImageResource(R.drawable.iv_play);

                tv_song.setText(mMusicService.getCurrentTitle(),getWindowManager());
                tv_singer.setText(mMusicService.getCurrentArtist());
                iv_center_album.setImageBitmap(null);
                iv_center_album.setImageBitmap(mMusicService.getCurrentBitmap());;
                if (mMusicService.getDuration() > 0) {
                    sb_songProgress.setMax((int) mMusicService.getDuration());
                    tv_seekBar_end.setText(mDateFormat.format(mMusicService.getDuration()));
                }
                break;
        }
    }

    @Override
    protected MusicPresent createPresenter() {
        return new MusicPresent();
    }

    @Override
    public void showLocalMusic(List<MusicBean> beans) {

    }

//    @Override
//    public void showAlbumBitMap(Bitmap bitmap) {
//
//    }

    @Override
    public void showErrorMessage(String msg) {

    }

    private class MyConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MyMusicBinder binder = (MusicService.MyMusicBinder)service;
            mMusicService = binder.getMusicService();
            mDateFormat = new SimpleDateFormat("mm:ss", Locale.CHINA);
            if(mMusicService != null) SyncMusicInformation();//同步音乐信息
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected: ");
            if (mMusicService != null)  mMusicService = null;
        }
    }

    /**
     * 同步当期音乐信息
     */

    private void SyncMusicInformation(){
        iv_center_album.setImageBitmap(null);
        iv_center_album.setImageBitmap(mMusicService.getCurrentBitmap());
        tv_song.setText(mMusicService.getCurrentTitle(),getWindowManager());
        tv_singer.setText(mMusicService.getCurrentArtist());
        if(mMusicService.isPlaying()) iv_play_toggle.setImageResource(R.drawable.iv_pause);
        else iv_play_toggle.setImageResource(R.drawable.iv_play);
        if (mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_SORT_PLAY())) {
            iv_play_mode.setImageResource(R.drawable.ic_sort);
        }else if(mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_RANDOM_PLAY())){
            iv_play_mode.setImageResource(R.drawable.ic_turn_random);
        }else if(mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_REPEAT_PLAY())){
            iv_play_mode.setImageResource(R.drawable.ic_turn_repeat);
        }
//        同步音乐播放速度
        UpdateMusicProgressAndVolume();
        if(mMusicService.getCurrentPosition() > 0){
            tv_seekBar_start.setText(mDateFormat.format(mMusicService.getCurrentPosition()));
            sb_songProgress.setProgress(mMusicService.getCurrentPosition());
            sb_songProgress.setOnSeekBarChangeListener(mProgressListener);
        }
        UpdateSeekBarProgress();//启动 Timer和TimerTask子线程 根据条件 实时更新 当前音乐进度
        UpdateSeekBarVolume();//启动 TimerTask子线程 根据条件 实时更新 当前系统音量值

        handler.sendEmptyMessageDelayed(UPDATE_UI, 500);

    }

    private class MyViewClickListener implements View.OnClickListener{
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            Log.d(TAG,"发生点击");
            if (mMusicService == null) {
                Log.d(TAG,"mMusicService = null");
                return;
            }
            switch (v.getId()) {
                case R.id.song_lrc_top_return:
                    finish();
                    break;
                case R.id.song_lrc_center_album:
                    if(iv_center_album.getVisibility() == View.VISIBLE) iv_center_album.setVisibility(View.INVISIBLE);
                    if(iv_center_album_2.getVisibility() == View.VISIBLE) iv_center_album_2.setVisibility(View.INVISIBLE);
                    if(sb_sound.getVisibility() == View.INVISIBLE) sb_sound.setVisibility(View.VISIBLE);
                    if(sb_volume.getVisibility() == View.INVISIBLE) sb_volume.setVisibility(View.VISIBLE);
                    break;
                case R.id.song_lrc_top_sounds:
                    if(iv_center_album.getVisibility() == View.INVISIBLE) iv_center_album.setVisibility(View.VISIBLE);
                    if(iv_center_album_2.getVisibility() == View.INVISIBLE) iv_center_album_2.setVisibility(View.VISIBLE);
                    if(sb_sound.getVisibility() == View.VISIBLE) sb_sound.setVisibility(View.INVISIBLE);
                    if(sb_volume.getVisibility() == View.VISIBLE) sb_volume.setVisibility(View.INVISIBLE);
                    break;
                case R.id.song_lrc_bottom_play:
                    sendBroadcast(new Intent("play/pause"));
                    break;
                case R.id.song_lrc_bottom_play_mode:
//                    切换播放模式
                    if (mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_SORT_PLAY())) {
                        mMusicService.setPLAYER_PLAY_MODE(mMusicService.getPLAYER_RANDOM_PLAY());
                        iv_play_mode.setImageResource(R.drawable.ic_turn_random);
                        Log.d(TAG,"切换后播放模式 ： " + mMusicService.getPLAYER_PLAY_MODE());
                    }else if(mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_RANDOM_PLAY())){
                        mMusicService.setPLAYER_PLAY_MODE(mMusicService.getPLAYER_REPEAT_PLAY());
                        iv_play_mode.setImageResource(R.drawable.ic_turn_repeat);
                        Log.d(TAG,"切换后播放模式 ： " + mMusicService.getPLAYER_PLAY_MODE());
                    }else if(mMusicService.getPLAYER_PLAY_MODE().equals(mMusicService.getPLAYER_REPEAT_PLAY())){
                        mMusicService.setPLAYER_PLAY_MODE(mMusicService.getPLAYER_SORT_PLAY());
                        iv_play_mode.setImageResource(R.drawable.ic_sort);
                        Log.d(TAG,"切换后播放模式 ： " + mMusicService.getPLAYER_PLAY_MODE());
                    }
                    break;
                case R.id.song_lrc_bottom_right:
                    Log.d("haha","点击下一曲");
                    mMusicService.OnNextPlay("next");
                    break;
                case R.id.song_lrc_bottom_left:
                    Log.d("haha","点击上一曲");
                    mMusicService.OnNextPlay("previous");
                    break;
                case R.id.song_lir_top_more:

                    break;
            }
        }
    }

    private class SongProgressListener implements OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mMusicService == null) return;
            if(isSeekBarChanging && seekBar.getMax() > 0 || progress >= 0)
//                在播放中滑动，TextView开始时间现实滑动的时间
                tv_seekBar_start.setText(mDateFormat.format(progress));
            else if(!mMusicService.isFirstPlay() && !isSeekBarChanging)
                tv_seekBar_start.setText(mDateFormat.format(mMusicService.getCurrentPosition()));
            if(sb_songProgress.getMax() < 1 || sb_songProgress.getMax() != mMusicService.getDuration())
                UpdateMusicProgressAndVolume();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            if(mMusicService == null) return;

            if(seekBar.getProgress() < 1) mMusicService.setProgress(1);
            else mMusicService.setProgress(seekBar.getProgress());

            if (!mMusicService.isFirstPlay()) {
                iv_play_toggle.setImageResource(R.drawable.iv_pause);
                tv_seekBar_start.setText(mDateFormat.format(mMusicService.getCurrentPosition()));
            }else{
                if (mMusicService.getCurrentPosition() > 0) {
                    mMusicService.OnContinueOrPausePlay();
                    iv_play_toggle.setImageResource(R.drawable.iv_pause);
                }else Toast.makeText(SongLrcActivity.this,"请先播放歌曲",Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void UpdateMusicProgressAndVolume() {
        if(!mMusicService.isPlaying() || mMusicService.getCurrentPosition() > 0 ){
//            在MediaPlayer已经播放过的情况下可以获取
//            1.点击Item装在音乐文件开始播放
//            2.重新打开APP，接着上次的播放器位置继续播放的时候
            if(!(tv_song.getText()+tv_singer.getText().toString())
                    .equals(mMusicService.getCurrentTitle()+mMusicService.getCurrentArtist()))
                sb_songProgress.setSecondaryProgress(0);

            if (mMusicService.getDuration() > 0) {
                sb_songProgress.setMax((int) mMusicService.getDuration());
                tv_seekBar_end.setText(mDateFormat.format(mMusicService.getDuration()));
            }
//            音量进度初始化
            if (mMusicService.getMaxVolume() >0) {
                sb_volume.setMax(mMusicService.getMaxVolume());
                sb_volume.setProgress(mMusicService.getVolume());
                sb_volume.setOnSeekBarChangeListener(mVolumeListener);
            }

        }
    }

    private void UpdateSeekBarProgress() {
        if (mTimerProgressAndVolume == null) {
            mTimerProgressAndVolume = new Timer();
            mTimerProgressAndVolume.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(sb_songProgress == null) return;
                    if(mMusicService == null)return;

                    if(sb_songProgress.getProgress() >= sb_songProgress.getMax()){
                        if (sb_songProgress.getMax() > 0){
                            sb_songProgress.setProgress(0);
                        }
                        return;
                    }
                    if (!isSeekBarChanging && mMusicService.isFirstPlay()){
                        if(!mMusicService.isPlayerPrepared()){
                            sb_songProgress.setProgress(mMusicService.getCurrentPosition());
                        }else sb_songProgress.setProgress(mMusicService.getCurrentPosition());
//                        更新第二层进度
                        if (sb_songProgress.getSecondaryProgress() > 0 &&
                                sb_songProgress.getMax() >=0 &&
                                sb_songProgress.getSecondaryProgress() < sb_songProgress.getMax()) {
                            int secondary = new Random().nextInt(sb_songProgress.getMax()/5);
                            int current = sb_songProgress.getSecondaryProgress();
                            if (mMusicService.getCurrentPosition() > 0 &&
                                    current < mMusicService.getCurrentPosition() &&
                                    mMusicService.getCurrentPosition() < sb_songProgress.getMax()) {
                                current = mMusicService.getCurrentPosition();
                            }
                            if ((current += secondary) > sb_songProgress.getMax())
                                current = sb_songProgress.getMax();
                            sb_songProgress.setSecondaryProgress(current);
                        }
                    }
                }
            },0,330);
        }
    }

    private class MusicVolumeListener implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(mMusicService != null && progress <= seekBar.getMax()) {
                Log.d(TAG,"onProgressChanged: 总音量" + mMusicService.getMaxVolume() + "当前音量：" + progress);
                mMusicService.setVolume(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mMusicService != null && seekBar.getProgress() <= seekBar.getMax())
                mMusicService.setVolume(seekBar.getProgress());
        }
    }
    private void UpdateSeekBarVolume(){
        if (mTimerProgressAndVolume != null) {
            mTimerProgressAndVolume.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mMusicService != null && sb_sound.getVisibility() == View.VISIBLE && sb_sound.getVisibility() == View.VISIBLE
                            && sb_volume.getProgress() != mMusicService.getVolume()) {
                        sb_volume.setProgress(mMusicService.getVolume());
                    }
                }
            },100,100);
        }
    }

    private void clickBottomPlay(){
        if (mMusicService.isPlaying()) {
            Log.d(TAG,"点击暂停");
            mMusicService.OnPause();
            iv_play_toggle.setImageResource(R.drawable.iv_play);
        }else {
            UpdateMusicProgressAndVolume();
            if (mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() > 0) {
//                            接着上一次播放
                mMusicService.OnContinueOrPausePlay();
                iv_play_toggle.setImageResource(R.drawable.iv_pause);
            }
            else if(mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() == 0){
                Log.d(TAG,"安装APP后第一次播放");
            }
            else{//暂停后播放
                Log.d(TAG,"点击播放");
                mMusicService.OnContinueOrPausePlay();
                iv_play_toggle.setImageResource(R.drawable.iv_pause);
            }
        }
    }





    @Override
    protected void init() {
        Log.d(TAG,"init初始化");
//初始化绑定音乐服务
        myServiceIntent = new Intent(this,MusicService.class);
        mMyConn = new MyConn();
        //初始化点击事件监听
        mClickListener = new MyViewClickListener();
        mProgressListener = new SongProgressListener();
        mVolumeListener = new MusicVolumeListener();
    }


    private void initUI(){
        Log.d(TAG,"initUi初始化");
        tv_song = findViewById(R.id.song_lrc_top_song);
        tv_singer = findViewById(R.id.song_lrc_top_singer);

        iv_center_album = findViewById(R.id.song_lrc_center_album);
        iv_center_album.setOnClickListener(mClickListener);
        iv_center_album_2 = findViewById(R.id.circleImageView);
        iv_center_album_2.setOnClickListener(mClickListener);


        iv_play_toggle = findViewById(R.id.song_lrc_bottom_play);
        iv_play_toggle.setOnClickListener(mClickListener);
        iv_play_mode = findViewById(R.id.song_lrc_bottom_play_mode);
        iv_play_mode.setOnClickListener(mClickListener);

        findViewById(R.id.song_lrc_bottom_left).setOnClickListener(mClickListener);
        findViewById(R.id.song_lrc_bottom_right).setOnClickListener(mClickListener);
        findViewById(R.id.song_lrc_top_return).setOnClickListener(mClickListener);

//      音乐进度控制与显示
        sb_songProgress = findViewById(R.id.song_lrc_bar);

        tv_seekBar_start  = findViewById(R.id.song_lrc_time_start);
        tv_seekBar_end = findViewById(R.id.song_lrc_time_end);
//        歌词显示
        sb_volume = findViewById(R.id.song_lrc_top_bar_volume);

        sb_sound = findViewById(R.id.song_lrc_top_sounds);
        findViewById(R.id.song_lrc_top_sounds).setOnClickListener(mClickListener);
    }

    private void releaseUI(){
        if (tv_song != null) tv_song = null;
        if (tv_singer != null) tv_singer = null;
        if (iv_play_toggle != null) iv_play_toggle = null;
        if (iv_play_mode != null) iv_play_mode = null;
        if(iv_center_album != null){
            iv_center_album.setImageBitmap(null);
            iv_center_album = null;
        }
        if(sb_songProgress != null) sb_songProgress = null;
        if(tv_seekBar_start != null) tv_seekBar_start = null;
        if(tv_seekBar_end != null) tv_seekBar_end = null;

        if(mLrcGroup != null) mLrcGroup = null;
        if(sb_volume != null) sb_volume = null;
    }

    private void releaseInterface(){
        if (mMyConn != null) mMyConn = null;
        if (myServiceIntent != null) myServiceIntent = null;
        if (mMusicService != null) mMusicService = null;
        if (mClickListener != null) mClickListener = null;
        if (mProgressListener != null) mProgressListener = null;
        if (mVolumeListener != null) mVolumeListener = null;
        if (mDateFormat != null) mDateFormat = null;
        if (mTimerProgressAndVolume != null) {
            mTimerProgressAndVolume.purge();
            mTimerProgressAndVolume.cancel();
            mTimerProgressAndVolume = null;
        }

    }
}
