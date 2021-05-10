package com.example.wangyiyunmusic.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.wangyiyunmusic.R;
import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.model.AllSongSheetModel;
import com.example.wangyiyunmusic.model.SongSheetModel;
import com.example.wangyiyunmusic.view.SongLrcActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;


public class MusicService extends BaseService{

    private static final String TAG = "Service状况";

    private final IBinder binder = new MyMusicBinder();
    //音频媒体、音频焦点管理、音量管理初始化、WIFI（WeeFee）锁
    private MediaPlayer mMediaPlayer;
    private AudioAttributes mAudioAttributes;
    private AudioFocusRequest mFocusRequest;
    private AudioManager mAudioManager;
    private WifiManager.WifiLock mWifiLock;
    private OnAudioFocusChangeListener mAudioFocusChangeListener;
    //音乐播放
    private boolean isFirstPlay = true;
    private boolean isPlayerPrepared = false; //判断是否是音乐文件以准备好播放
    private boolean isStartForeground = false; //判断是否启动前台服务
    private boolean isOnCompleted = false;
    private int mCurrentPosition = 0; //当前音乐播放的进度
    private OnErrorListener mOnErrorListener;
    //保存当前播放音乐信息
    private SharedPreferences settings;
    private String mCurrentTitle,mCurrentArtist,mCurrentAlbum,mCurrentAlbumPath,mCurrentPath,mCurrentPlaySource;
    private long mCurrentDuration;
    private Bitmap mCurrentBitmap,mBitmap,InfoBitmap;
    //音乐播放控制（上、下一曲）
    private List<MusicBean> mMusicBeanList;
    private int mCurrentQueueIndex = -1;
    private String PLAYER_PLAY_MODE;
    private Timer mTimer;
    public int MainIndex = 0;
    //前台服务
    private static final int NOTIFICATION_ID = 1,NOTIFICATION_ID_DOWNLOAD =2;
    private String CHANNEL_DESCRIPTION = "demo - 音乐播放控制通知",CHANNEL_ID = "com.example.wangyiyunmusic.service.channel";
    private RemoteViews remoteViews,remoteViews_normal;
    private Notification mNotification,mNotification_download;
    private Notification.Builder mBuilder,mBuilder_download;
    private NotificationManager mNotificationManager;
    private SongSheetModel mSongSheet;
    private loadBitmapCallBack mBitmapCallBack;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("MusicService onBind!");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        startFG();

        System.out.println("MusicService OnCreate!");
        mMusicBeanList = new ArrayList<>();
        GetLastMusicPlay();
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isPlaying()){
                    if (mCurrentPosition != mMediaPlayer.getCurrentPosition()) {
                        //Log.d(TAG, "报告组织！MusicService还活着！position = "+getPosition());
                        mCurrentPosition = mMediaPlayer.getCurrentPosition();//正在播放时

                        if(settings != null){
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt("MusicPosition",mCurrentPosition);
                            String playMode = settings.getString("PlayMode",PLAYER_SORT_PLAY);
                            if (playMode != null && !TextUtils.isEmpty(playMode)
                                    && !playMode.equals(PLAYER_PLAY_MODE)) {
                                editor.putString("PlayMode",PLAYER_PLAY_MODE);
                            }
                            editor.apply();
                        }
                    }
                }
            }
        },2000,1000);//第一次任务在延迟2秒后执行，每隔1秒执行一次任务
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("MusicService onStartCommand!");
        if (mCurrentBitmap == null) {
            mSongSheet.loadAlbumBitmap(mBitmapCallBack,mCurrentAlbumPath);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        System.out.println("MusicService onTaskRemoved!");
        //保存信息
    }

    @Override
    public void onDestroy() {
        System.out.println("MusicService onDestroy!");
        super.onDestroy();
        if (mOnErrorListener != null) mOnErrorListener = null;
        //释放资源
        releaseMediaPlayer();
        releaseMusicInformation();
        releaseNotification();

        System.gc();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("MusicService onUnbind!");
        return super.onUnbind(intent);
    }
    public class MyMusicBinder extends Binder{
        public MusicService getMusicService() {
            return MusicService.this;
        }
    }

    @SuppressLint("RemoteViewLayout")
    @Override
    protected void init() {
        initMediaPlayer();
        remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification_big);
        remoteViews_normal = new RemoteViews(getPackageName(), R.layout.layout_notification_normal);
        mSongSheet = new AllSongSheetModel();//向上转型
        mBitmapCallBack = new loadBitmapCallBack();


    }
    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);//唤醒锁定模式，关闭屏幕时，CPU不休眠
        // 初始化wifi锁
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "dyqlLock");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //音频焦点
        mAudioFocusChangeListener = new OnAudioFocusChangeListener();
        mAudioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mAudioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(mAudioFocusChangeListener)
                .build();
        mMediaPlayer.setAudioAttributes(mAudioAttributes);
    }
    private class OnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener{
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN://获得长时间播放焦点,短暂失去焦点后触发此回调
                    Log.d(TAG, "onAudioFocusChange: 获得长时间播放焦点");
                    //暂停播放之后继续播放|获取到音频焦点焦点之后再播放
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://短暂失去焦点
                    Log.d(TAG, "onAudioFocusChange: 短暂失去焦点");
                    //暂停播放
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://失去焦点，但可以共同使用，需主动降低声量
                    Log.d(TAG, "onAudioFocusChange: 失去焦点，但可以共同使用");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS://长时间失去焦点
                    Log.d(TAG, "onAudioFocusChange: 长时间失去焦点");
                    //停止播放，立即释放音频焦点
                    break;
            }
        }
    }

    private void StopMediaPlayer(){
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
            if (!isFirstPlay()) {
                mCurrentPosition = 0;
                mMediaPlayer.seekTo(0);
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                if (mWifiLock.isHeld()) mWifiLock.release();//解除WIFI锁
                mAudioManager.abandonAudioFocusRequest(mFocusRequest);//释放音频焦点
            }
        }else mOnErrorListener.onError(null,0,0);
    }
    private void SetMediaPlayerResource(String Path,boolean isNetMusic){
        StopMediaPlayer();
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.setDataSource(Path);
                PlayMediaPlayer(isNetMusic);
            } catch (IOException e) {
                //e.printStackTrace();
                mOnErrorListener.onError(mMediaPlayer,1,2);
            }
        }else mOnErrorListener.onError(null,0,0);
    }
    private void PlayMediaPlayer(boolean isNetMusic){
        if (!isStartForeground) StartForeground(remoteViews);
        if (isPlayerPrepared) isPlayerPrepared = false;
        if (mMediaPlayer != null) {
            if (mCurrentPosition == 0) {
                try {
                    if (isNetMusic) mMediaPlayer.prepareAsync();
                    else mMediaPlayer.prepare();
                } catch (IOException e) {
                    mOnErrorListener.onError(mMediaPlayer,2,2);
                }
            }else {//1.接着上次播放，2.暂停后再播放
                if (isFirstPlay()) {//接着上次播放
                    try {
                        Log.d(TAG, "PlayMediaPlayer: 接着上次播放 "+mCurrentPosition);
                        mMediaPlayer.setDataSource(mCurrentPath);
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        mOnErrorListener.onError(mMediaPlayer,1,2);
                    }
                }else {//暂停后再播放
                    Log.d(TAG, "PlayMediaPlayer: 暂停后再播放 ");
                    mMediaPlayer.seekTo(mCurrentPosition);
                    OnRequestAudioFocus(mMediaPlayer,true);
                }
            }
            if (isFirstPlay()) {//只执行一次
                //Log.d(TAG, "PlayMediaPlayer: ");
                mOnErrorListener = new OnErrorListener();
                mMediaPlayer.setOnErrorListener(mOnErrorListener);
                mMediaPlayer.setOnCompletionListener(new OnCompletionListener());
                mMediaPlayer.setOnPreparedListener(new OnPreparedListener());
            }
        }else mOnErrorListener.onError(null,0,0);
    }
    public void OnPause(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()){
            //停止播放音乐释放焦点
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
            //释放wifi锁
            if (mWifiLock.isHeld()) mWifiLock.release();
            //记录当前音乐播放的进度
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            //暂停音乐
            mMediaPlayer.pause();
        }
    }
    public void OnContinueOrPausePlay(){
        PlayMediaPlayer(false);
    }
    public void OnPlay(String Path){
        Log.d(TAG, "OnPlay: "+Path);
        SetMediaPlayerResource(Path,false);
    }
    public void OnNextPlay(String PREVIOUS_OR_NEXT_PLAYER){
        if (getMusicListSize() > 0) {
            NextMediaPlayer(PREVIOUS_OR_NEXT_PLAYER);
            OnPlay(mCurrentPath);
        }else Toast.makeText(MusicService.this,"该歌单为空",Toast.LENGTH_SHORT).show();
    }
    /**
     * @param previous_or_next_player 上一曲还是下一曲
     * 确定 {@link MusicService#mCurrentQueueIndex} 的值
     * */
    private void NextMediaPlayer(String previous_or_next_player) {

        if (getMusicListSize() < 2) {
            mCurrentQueueIndex = getMusicListSize() -1;
        }else {
            switch (PLAYER_PLAY_MODE) {
                case PLAYER_SORT_PLAY://顺序播放
                    if (mMediaPlayer.isLooping()) mMediaPlayer.setLooping(false);
                    if (previous_or_next_player.equals("next"))
                        mCurrentQueueIndex = ++ mCurrentQueueIndex % getMusicListSize();//前缀运算符先于取余运算符执行【顺序播放】下一曲
                    else mCurrentQueueIndex = mCurrentQueueIndex > 0 ? mCurrentQueueIndex -1 : getMusicListSize() -1;//【顺序播放】上一曲
                    break;
                case PLAYER_RANDOM_PLAY://随机播放
                    if (mMediaPlayer.isLooping()) mMediaPlayer.setLooping(false);
                    if (getMusicListSize() <= 3) {
                        mCurrentQueueIndex = ++ mCurrentQueueIndex % getMusicListSize();//前缀运算符先于取余运算符执行【顺序播放】下一曲
                    }else {
                        int randomIndex = new Random().nextInt(getMusicListSize() - 1);
                        while (randomIndex == mCurrentQueueIndex) {//去重
                            randomIndex = new Random().nextInt(getMusicListSize() - 1);
                        }
                        mCurrentQueueIndex = randomIndex;
                        Log.d(TAG, "NextMediaPlayer: 【随机播放模式】下一曲位置: " +mCurrentQueueIndex);
                    }
                    break;
                case PLAYER_REPEAT_PLAY://重复播放
                    if (!mMediaPlayer.isLooping()) mMediaPlayer.setLooping(true);
                    break;
            }
        }
        Log.d(TAG, "NextMediaPlayer: 下一曲位置: " +mCurrentQueueIndex);
        MainIndex = mCurrentQueueIndex;
        if (!PLAYER_PLAY_MODE.equals(PLAYER_REPEAT_PLAY)) getMusicInfo(mCurrentQueueIndex);

    }

    private void getMusicInfo(int Queue) {
        MusicBean bean = null;
        if (Queue >= 0 && Queue < getMusicListSize()) {
            bean = mMusicBeanList.get(Queue);
        }
        else if (Queue < 0 || Queue >= getMusicListSize()){
            Log.e(TAG, "getMusicInfo: 【Queue 越界】: "+ Queue+", ListSize : "+getMusicListSize());
            if (getMusicListSize() <= 0) {
                if (mOnErrorListener == null) {
                    Log.d(TAG, "getMusicInfo: mOnErrorListener == null");
                }else mOnErrorListener.onError(mMediaPlayer,1,1);
                return;
            }else bean = mMusicBeanList.get(getMusicListSize() -1);
            mCurrentQueueIndex = 0;
        }
        if (bean != null) setNotification(bean);
    }

    private void OnRequestAudioFocus(MediaPlayer mp,boolean isOnlyClickPlay){
        if (mp == null) {
            mOnErrorListener.onError(null,0,0);
            return;
        }
        //申请长时间播放的焦点
        int audioFocusState = mAudioManager.requestAudioFocus(mFocusRequest);
        if (audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //启动WIFI锁
            mWifiLock.acquire();
            //所有都准备好了，开始播放
            mp.start();
            UpdateNotification(isOnlyClickPlay);
        }else if(audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_FAILED){
            mOnErrorListener.onError(mp,0,1);
        }else if(audioFocusState == AudioManager.AUDIOFOCUS_REQUEST_DELAYED){
            mOnErrorListener.onError(mp,0,2);
        }else mOnErrorListener.onError(mp,0,3);
    }
    private class OnErrorListener implements MediaPlayer.OnErrorListener{
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "onErrorListener: what:"+what+" , extra = "+extra);
            switch (what) {
                case 0:
                    if (extra == 0) Toast.makeText(MusicService.this,"播放错误，找不到媒体对象！",Toast.LENGTH_SHORT).show();
                    if (extra == 1) Toast.makeText(MusicService.this,"播放错误，申请播放焦点失败！",Toast.LENGTH_SHORT).show();
                    if (extra == 2) Toast.makeText(MusicService.this,"播放错误，延迟获取焦点！",Toast.LENGTH_SHORT).show();
                    if (extra == 3) Toast.makeText(MusicService.this,"播放错误，未曾设想的音频焦点获取结果！",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    if (extra == 1)
                        Toast.makeText(MusicService.this,"播放错误，歌曲地址为空,请播放其他歌曲",Toast.LENGTH_SHORT).show();
                    else if (extra == 2)
                        Toast.makeText(MusicService.this,"该音乐文件已损坏,请播放其他歌曲",Toast.LENGTH_SHORT).show();
                    else if (extra == -2147483648)
                        Toast.makeText(MusicService.this,"音乐文件解码失败,请删除该文件,尝试播放网络版本",Toast.LENGTH_SHORT).show();
                    else Toast.makeText(MusicService.this,"播放错误,请播放其他歌曲",Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    if (extra == 2)
                        Toast.makeText(MusicService.this,"音乐文件解码失败,请播放其他歌曲",Toast.LENGTH_SHORT).show();
                    else Toast.makeText(MusicService.this,"播放错误,请播放其他歌曲",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error Prepare");
                    break;
            }
            //onError返回值返回false会触发onCompletionListener，y
            //所以返回false，一般意味着会退出当前歌曲播放。
            //如果不想退出当前歌曲播放则应该返回true
            return true;
        }
    }
    private class OnCompletionListener implements MediaPlayer.OnCompletionListener{
        @Override
        public void onCompletion(MediaPlayer mp) {//自动下一曲播放
            Log.d(TAG, "onCompletion: ");
            isOnCompleted = true;
            if (!isFirstPlay()) {
                String action = "next";
                OnNextPlay(action);
                //当重复播放时不更新视图
                sendBroadcast(new Intent(action));
            }
        }
    }
    private class OnPreparedListener implements MediaPlayer.OnPreparedListener{
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (isFirstPlay()) {
                isFirstPlay = false;
            }
            if (!isPlayerPrepared) isPlayerPrepared = true;
            if (mCurrentPosition > 0) mp.seekTo(mCurrentPosition);
            OnRequestAudioFocus(mp,false);
        }
    }

    public boolean isFirstPlay() {
        return isFirstPlay;
    }
    public void setNotification(MusicBean bean){
        if (isFirstPlay()) mCurrentPosition = 0;
        this.mCurrentTitle = bean.getTitle();
        this.mCurrentArtist = bean.getArtist();
        this.mCurrentAlbum = bean.getAlbum();
        this.mCurrentAlbumPath = bean.getAlbumPath();
        this.mCurrentPath = bean.getPath();
        this.mCurrentDuration = bean.getDuration();
        SaveLastMusicPlay();//保存音乐信息
    }

    public void setmCurrentPosition(int position){
        mCurrentPosition = position;
        mCurrentQueueIndex = position;
        MainIndex = position;
    }

    public String getCurrentTitle() {
        return mCurrentTitle;
    }

    public String getCurrentArtist() {
        return mCurrentArtist;
    }

    public String getCurrentAlbum() {
        return mCurrentAlbum;
    }

    public String getCurrentAlbumPath() {
        return mCurrentAlbumPath;
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public long getDuration() {
        return mCurrentDuration;
    }

    public Bitmap getCurrentBitmap() {
        return mCurrentBitmap;
    }

    /**
     * 使用{@link AudioManager}获取、设置系统音量*/
    public int getMaxVolume(){
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }
    public void setVolume(int volume){
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
        }
    }
    public int getVolume(){
        if (mAudioManager != null) {
            Object object = getVolume(mAudioManager);
            if (object instanceof Integer) {
                return Integer.parseInt(object.toString());
            }
        }
        return 0;
    }
    private Object getVolume(AudioManager manager){
        return Optional.of(manager).map(new Function<AudioManager, Object>() {
            @Override
            public Object apply(AudioManager manager) {
                return manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        }).orElse("0");
    }

    public int getMusicListSize() {
        if (mMusicBeanList == null) return 0;
        return mMusicBeanList.size();
    }
    public void setMusicBeanList(List<MusicBean> musicBeanList) {
        mMusicBeanList = musicBeanList;
    }

    public String getPLAYER_PLAY_MODE() {
        return PLAYER_PLAY_MODE;
    }
    public void setPLAYER_PLAY_MODE(String PLAYER_PLAY_MODE) {
        this.PLAYER_PLAY_MODE = PLAYER_PLAY_MODE;
    }
    public String getPLAYER_SORT_PLAY() {
        return PLAYER_SORT_PLAY;
    }
    public String getPLAYER_RANDOM_PLAY() {
        return PLAYER_RANDOM_PLAY;
    }
    public String getPLAYER_REPEAT_PLAY() {
        return PLAYER_REPEAT_PLAY;
    }

    public boolean isPlaying(){
        if (mMediaPlayer != null) return mMediaPlayer.isPlaying();
        else return false;
    }

    public boolean isPlayerPrepared() {
        return isPlayerPrepared;
    }
    public boolean isOnCompleted() {
        return isOnCompleted;
    }
    public void setOnCompleted(boolean onCompeted) {
        this.isOnCompleted = onCompeted;
    }

    /**
     * 给音乐进度条提供设置音乐进度的方法*/
    public void setProgress(int currentPosition){
        this.mCurrentPosition = currentPosition;
        if (!isFirstPlay()) {
            if (isPlaying()) mMediaPlayer.pause();
            PlayMediaPlayer(false);
        }
    }

    /**
     * 设置/更新通知栏以及保存最后一曲歌曲信息
     */
    private void StartForeground(RemoteViews remoteViews){//开启前台服务
        Log.d("查看监听","开启前台服务");
        isStartForeground = true;
        /*点击通知栏跳转*/
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SongLrcActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT);
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //如果系统版本大于API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = createChannel(CHANNEL_DESCRIPTION, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }
        /*创建Notification*/
        remoteViews.setTextViewText(R.id.notification_top_song, mCurrentTitle);
        remoteViews.setTextViewText(R.id.notification_top_singer, mCurrentArtist);
        remoteViews_normal.setTextViewText(R.id.notification_top_song, mCurrentTitle);
        remoteViews_normal.setTextViewText(R.id.notification_top_singer, mCurrentArtist);
        //设置点击发送广播 大视图
        remoteViews.setOnClickPendingIntent(R.id.notification_iv_play, getPendingIntent(this,"play/pause"));
        remoteViews.setOnClickPendingIntent(R.id.notification_iv_right, getPendingIntent(this,"next"));
        remoteViews.setOnClickPendingIntent(R.id.notification_iv_left, getPendingIntent(this,"previous"));
        remoteViews.setOnClickPendingIntent(R.id.notification_iv_love, getPendingIntent(this,"love"));
        remoteViews.setOnClickPendingIntent(R.id.notification_iv_close, getPendingIntent(this,"close"));
        //remoteViews.setOnClickPendingIntent(R.id.notification_iv_lrc, getPendingIntent(this,"update"));
        //小视图
        remoteViews_normal.setOnClickPendingIntent(R.id.notification_iv_play, getPendingIntent(this,"play/pause"));
        remoteViews_normal.setOnClickPendingIntent(R.id.notification_iv_right, getPendingIntent(this,"next"));
        /*remoteViews.setOnClickPendingIntent(R.id.notification_iv_left,getPendingIntent(this,"previous"));*/
        remoteViews_normal.setOnClickPendingIntent(R.id.notification_iv_love, getPendingIntent(this,"love"));
        remoteViews_normal.setOnClickPendingIntent(R.id.notification_iv_close, getPendingIntent(this,"close"));
        mBuilder = new Notification.Builder(MusicService.this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ablum_title) //设置通知栏图标
                .setWhen(System.currentTimeMillis())
                //.setCustomHeadsUpContentView()浮动视图
                //.setStyle(new Notification.DecoratedCustomViewStyle())
                //.setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setCustomContentView(remoteViews_normal)//64dp
                .setCustomBigContentView(remoteViews);//最高256dp

        mNotification = mBuilder.build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;//设置常驻通知
        startForeground(NOTIFICATION_ID,mNotification);
    }

    private PendingIntent getPendingIntent(Context context,String action){
        Log.d("查看监听","getPendingIntent");
        return PendingIntent.getBroadcast(context,0,new Intent(action),0);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private NotificationChannel createChannel(String description,String id,int importance){
        Log.d("查看监听","设置监听信息");
        //频道用户可见名称
        CharSequence name = "音乐播放";
        //频道用户的可见描述。
        NotificationChannel channel = new NotificationChannel(id,name,importance);
        /*配置通知通道*/
        channel.setDescription(description);
        channel.enableLights(true);
        //是否显示在锁屏上
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        //关闭此通知发出时铃声
        channel.setSound(null,null);
        /*如果设备支持此功能，则设置发送到此频道通知的通知灯颜色。*/
        channel.setLightColor(Color.GREEN);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,400});
        /*设置发布到此频道的通知是否可以在启动程序中显示为应用程序图标徽章。*/
        channel.setShowBadge(false);
        return channel;
    }

    public void UpdateNotification(boolean isOnlyClickPlay){
        if (isPlaying()) {
            remoteViews.setImageViewBitmap(R.id.notification_iv_play,
                    BitmapFactory.decodeResource(getResources(),R.drawable.iv_pause_grey));
            remoteViews_normal.setImageViewBitmap(R.id.notification_iv_play,
                    BitmapFactory.decodeResource(getResources(),R.drawable.iv_pause_grey));
        } else {
            remoteViews.setImageViewBitmap(R.id.notification_iv_play,
                    BitmapFactory.decodeResource(getResources(),R.drawable.iv_play_grey));//播放或暂停
            remoteViews_normal.setImageViewBitmap(R.id.notification_iv_play,
                    BitmapFactory.decodeResource(getResources(),R.drawable.iv_play_grey));//播放或暂停
        }

        if (!isOnlyClickPlay){
            remoteViews.setTextViewText(R.id.notification_top_song, mCurrentTitle);//歌名
            remoteViews.setTextViewText(R.id.notification_top_singer, mCurrentArtist);//歌手
            remoteViews_normal.setTextViewText(R.id.notification_top_song, mCurrentTitle);//歌名
            remoteViews_normal.setTextViewText(R.id.notification_top_singer, mCurrentArtist);//歌手
            mSongSheet.loadAlbumBitmap(mBitmapCallBack,mCurrentAlbumPath);//回调方法加载专辑图片
        }else {
            mBuilder.setCustomBigContentView(remoteViews);
            mBuilder.setCustomContentView(remoteViews_normal);
            mNotification = mBuilder.build();
            mNotificationManager.notify(NOTIFICATION_ID,mNotification);
        }
    }

    private class loadBitmapCallBack implements SongSheetModel.OnLoadAlbumBitmapCallBack {

        @Override
        public void onCallBack(Bitmap bitmap) { ;
            mCurrentBitmap = bitmap == null ? mBitmap : bitmap;
            if (!isFirstPlay()) {
                //发送广播通知处于栈顶的Activity更新UI
                sendBroadcast(new Intent("update"));
                remoteViews.setImageViewBitmap(R.id.notification_iv_album, null);//专辑图片
                remoteViews_normal.setImageViewBitmap(R.id.notification_iv_album,null);//专辑图片

                remoteViews.setImageViewBitmap(R.id.notification_iv_album, mCurrentBitmap);//专辑图片
                remoteViews_normal.setImageViewBitmap(R.id.notification_iv_album, mCurrentBitmap);//专辑图片

                mBuilder.setCustomBigContentView(remoteViews);
                mBuilder.setCustomContentView(remoteViews_normal);

                mNotification = mBuilder.build();
                mNotificationManager.notify(NOTIFICATION_ID,mNotification);
            }
        }
    }
    @Override
    protected void receivePlay(String action) {
        if (action == null) return;
        switch (action) {
            case "play/pause":
                OnPlayPauseNotification();
                break;
            case "next":
            case "previous":
                OnPreviousNextNotification(action);
                break;
            case "love":
                //OnLovedNotification();
                break;
            case "close":
                //关闭通知栏。停止播放音乐，整个应用处于后台
                OnCloseNotification();
                break;
        }
    }

    private void OnPlayPauseNotification() {
        if (isPlaying()) OnPause();
        else OnContinueOrPausePlay();
        UpdateNotification(true);
    }

    private void OnPreviousNextNotification(String action) {
        if (!isOnCompleted()) OnNextPlay(action);
        //监听完成方法会发此广播，如果是它发的，则只更新UI，最后设置默认不是它发出的广播
        setOnCompleted(false);
    }
    public void OnCloseNotification(){
        stopForeground(true);
        isStartForeground = false;
        OnPause();
    }

    private void SaveLastMusicPlay(){
        if (mMediaPlayer.isPlaying()) {
            //播放中关闭本应用时调用，记录当前的播放进度
            //注：如此情况下记录的播放进度会比实际播放进度慢0-3秒左右
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
        }
        settings = getSharedPreferences("UserLastPlay",0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("MusicTitle",mCurrentTitle);
        editor.putString("MusicArtist",mCurrentArtist);
        editor.putString("MusicAlbum",mCurrentAlbum);
        editor.putString("MusicAlbumPath",mCurrentAlbumPath);
        editor.putString("MusicPath",mCurrentPath);
        editor.putInt("MusicPosition",mCurrentPosition);
        editor.putLong("MusicDuration",mCurrentDuration);
        editor.putString("MusicPlayMode",PLAYER_PLAY_MODE);//记录播放模式
        if (editor.commit()) Log.d(TAG, "SaveLastMusicPlay: 保存音乐信息成功");
        else Log.d(TAG, "SaveLastMusicPlay: 保存音乐信息失败");
    }
    private void GetLastMusicPlay(){
        settings = getSharedPreferences("UserLastPlay",0);

        mCurrentTitle = settings.getString("MusicTitle",null);
        mCurrentArtist = settings.getString("MusicArtist",null);
        mCurrentAlbum = settings.getString("MusicAlbum",null);
        mCurrentAlbumPath = settings.getString("MusicAlbumPath",null);
        mCurrentPath = settings.getString("MusicPath",null);
        mCurrentPosition = settings.getInt("MusicPosition",0);
        mCurrentDuration = settings.getLong("MusicDuration",0);
        PLAYER_PLAY_MODE = settings.getString("MusicPlayMode",PLAYER_SORT_PLAY);
        mBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.album);
    }

    private void releaseMediaPlayer() {
        //1.释放音频焦点
        mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        //2.释放WIFI锁
        if (mWifiLock.isHeld()) mWifiLock.release();
        if (mWifiLock != null) mWifiLock = null;
        //3.清空引用
        if (mAudioFocusChangeListener != null) mAudioFocusChangeListener = null;
        if (mFocusRequest != null) mFocusRequest = null;
        if (mAudioAttributes != null) mAudioAttributes = null;
        if (mAudioManager != null) mAudioManager = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void releaseMusicInformation(){
        if (settings != null) settings = null;
        if (mCurrentTitle != null) mCurrentTitle = null;
        if (mCurrentArtist != null) mCurrentArtist = null;
        if (mCurrentAlbum != null) mCurrentAlbum = null;
        if (mCurrentAlbumPath != null) mCurrentAlbumPath = null;
        if (mCurrentPath != null) mCurrentPath = null;
        if (mCurrentPlaySource != null) mCurrentPlaySource = null;
        if (PLAYER_PLAY_MODE != null) PLAYER_PLAY_MODE = null;
        if (mMusicBeanList != null) {
            if(mMusicBeanList.size() > 0) mMusicBeanList.clear();
            mMusicBeanList = null;
        }
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void releaseNotification(){
        //音乐播放控制通知
        if (remoteViews != null) {
            remoteViews.removeAllViews(R.layout.layout_notification_big);
            remoteViews = null;
        }
        if (remoteViews_normal != null) {
            remoteViews_normal.removeAllViews(R.layout.layout_notification_normal);
            remoteViews_normal = null;
        }
        if (mBuilder != null) mBuilder = null;
        if (mNotification != null) mNotification = null;
        if (CHANNEL_DESCRIPTION != null) CHANNEL_DESCRIPTION = null;
        if (CHANNEL_ID != null) CHANNEL_ID = null;
        //清空通知栏管理器对象
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager = null;
        }
        if (mSongSheet != null) mSongSheet = null;
        if (mBitmapCallBack != null) mBitmapCallBack = null;
        if (mCurrentBitmap != null) mCurrentBitmap = null;
        if (mBitmap != null) mBitmap = null;
    }

}
