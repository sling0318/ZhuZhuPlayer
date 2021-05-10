package com.example.wangyiyunmusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wangyiyunmusic.Adapter.MusicAdapter;
import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.dialog.SongOpDialog;
import com.example.wangyiyunmusic.present.MusicPresent;
import com.example.wangyiyunmusic.service.MusicService;
import com.example.wangyiyunmusic.util.HtmlStringUtil;
import com.example.wangyiyunmusic.view.base.BaseActivity;
import com.example.wangyiyunmusic.view.LocalMusicView;
import com.example.wangyiyunmusic.view.SongLrcActivity;

import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends BaseActivity<MusicPresent,LocalMusicView> implements LocalMusicView {

    private static final String TAG = "haha";

    private ImageView play_Iv;
    private ImageView albumImage_Iv;
    private TextView songTv,singerTv;
    private RecyclerView musicRv;
    private ConstraintLayout UILayout,PlayAllLayout;
    private MyViewClickListener mClickListener;
    private TableLayout tableLayout;

    private MusicAdapter adapter;
//    绑定音乐服务
    private MyConn mMyConn;
    private Intent myServiceIntent;
    private MusicService mMusicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.startForegroundService(myServiceIntent);
        this.bindService(myServiceIntent,mMyConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop");
        super.onStop();
            this.unbindService(mMyConn);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();

        this.stopService(myServiceIntent);
        releaseInterface();
        releaseUI();
    }

    @Override
    protected void receiveNextPlay(String Action) {
        Log.d("查看监听","receiveNextPlay");
        if (mMusicService == null) return;
        switch (Action){
            case "play/pause" :
                clickBottomPlay();
                if (!mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() >0){
                    mMusicService.UpdateNotification(true);
                }
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
                play_Iv.setImageResource(R.drawable.iv_main_play);
                break;
            case "update":
                if (mMusicService.isPlaying()) play_Iv.setImageResource(R.drawable.iv_main_pause);
                else play_Iv.setImageResource(R.drawable.iv_main_play);

                songTv.setText(HtmlStringUtil.SongSingerName(mMusicService.getCurrentTitle(),mMusicService.getCurrentArtist()));

                albumImage_Iv.setImageBitmap(null);
                albumImage_Iv.setImageBitmap(mMusicService.getCurrentBitmap());
                break;
        }
    }


    @Override
    protected MusicPresent createPresenter() {
        return new MusicPresent();
    }

    @Override
    public void showLocalMusic(List<MusicBean> beans) {
        if (beans != null) {
            if (beans.size() > 0) mMusicService.setMusicBeanList(beans);
            UpdateMusicListAdapter(beans);
        }

    }


    @Override
    public void showErrorMessage(String msg) {
        Log.d(TAG, "showErrorMessage: "+msg);
    }

    @Override
    protected void init() {
        // 申请读写权限
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{WRITE_EXTERNAL_STORAGE}, 1);


//        初始化绑定音乐服务
        myServiceIntent = new Intent(this,MusicService.class);
        mMyConn = new MyConn();

//        初始化点击监听
        mClickListener = new MyViewClickListener();


    }

    private void initUI(){
//        Typeface tf = Typeface.createFromAsset(getAssets(),"fonts/kunlun_bak.ttf");
        musicRv = findViewById(R.id.local_musci_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        musicRv.setLayoutManager(layoutManager);
        //绑定控件
        play_Iv = findViewById(R.id.bottom_play);
        songTv = findViewById(R.id.bottom_song);
//        songTv.setTypeface(tf);
        albumImage_Iv = findViewById(R.id.bottom_album);
        UILayout = findViewById(R.id.music_bottom_layout);
        PlayAllLayout = findViewById(R.id.layout_play_all);
//        设置监听事件
        play_Iv.setOnClickListener(mClickListener);
        UILayout.setOnClickListener(mClickListener);
        PlayAllLayout.setOnClickListener(mClickListener);
    }



    private void SyncMusicInformation() {
        Log.d(TAG,"同步信息");
        presenter.fetch();//显示音乐列表

        songTv.setText(HtmlStringUtil.SongSingerName(mMusicService.getCurrentTitle(),mMusicService.getCurrentArtist()));
        if(mMusicService.isPlaying()) play_Iv.setImageResource(R.drawable.iv_main_pause);
        else play_Iv.setImageResource(R.drawable.iv_main_play);
        albumImage_Iv.setImageBitmap(null);
        albumImage_Iv.setImageBitmap(mMusicService.getCurrentBitmap());
    }

    private void UpdateMusicListAdapter(List<MusicBean>beans){
        Log.d(TAG,"UpdateMusicListAdapter");
        if(adapter != null){
            adapter = null;
        }
        adapter = new MusicAdapter(this,beans);
        musicRv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        setEventListener(beans);
    }

    private void setEventListener(final List<MusicBean>beans){
//        MusicBean bean = beans.get(mMusicService.MainIndex);
//        presenter.fetch(bean.getPath());
        if (null == beans || beans.size() <= 0) return;
        adapter.setmItemClickListener(new MusicAdapter.ItemClickListener() {
            @Override
            public void ItemClick(View v, int position) {
//                列表点击事件
                if (mMusicService == null) return;
                MusicBean bean = beans.get(position);
                if((mMusicService.getCurrentTitle() + mMusicService.getCurrentArtist() + mMusicService.getCurrentAlbum())
                .equals(bean.getTitle() + bean.getArtist() + bean.getAlbum())){
                    sendBroadcast(new Intent("play/pause"));
                } else {
                    mMusicService.setNotification(bean);
                    mMusicService.OnPlay(bean.getPath());
                    Log.d(TAG,"列表点击");
                }

                mMusicService.setmCurrentPosition(position);
            }

            @Override
            public void ItemViewClick(View v, int position) {
//                一项列表的更多点击事件
                Log.d(TAG,"点击了更多");
                SongOpDialog dialog = new SongOpDialog(MainActivity.this,beans,position,mMusicService.getCurrentBitmap());
                dialog.show();
            }
        });
    }



    private class MyConn implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MyMusicBinder binder = (MusicService.MyMusicBinder)service;
            mMusicService = binder.getMusicService();

            SyncMusicInformation();//同步音乐信息
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected: ");
            if (mMusicService != null)  mMusicService = null;
        }
    }

    private class MyViewClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            if (mMusicService == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.bottom_play:
                    sendBroadcast(new Intent("play/pause"));
                    Log.d("查看监听","点击了播放/暂停");
                    break;
                case R.id.music_bottom_layout:
                    Intent intent = new Intent(MainActivity.this, SongLrcActivity.class);
                    startActivity(intent);
                    break;
                case R.id.layout_play_all:
                        Log.d(TAG,"播放全部");
                    mMusicService.setmCurrentPosition(-1);
                    mMusicService.OnNextPlay("next");
                    break;
            }
        }
    }
    private void clickBottomPlay(){
        if (mMusicService.isPlaying()) {
            mMusicService.OnPause();
            play_Iv.setImageResource(R.drawable.iv_main_play);
        }else {
            if (mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() > 0) {
//                            接着上一次播放
                mMusicService.OnContinueOrPausePlay();
                play_Iv.setImageResource(R.drawable.iv_pause);
            }
            else if(mMusicService.isFirstPlay() && mMusicService.getCurrentPosition() == 0){
                Log.d(TAG,"安装APP后第一次播放");
            }
            else{//暂停后播放
                Log.d(TAG,"点击播放");
                mMusicService.OnContinueOrPausePlay();
                play_Iv.setImageResource(R.drawable.iv_pause);
            }
        }
    }

    private void releaseInterface() {
        if (mMyConn != null) mMyConn = null;
        if (myServiceIntent != null) myServiceIntent = null;
        if (mMusicService != null) mMusicService = null;
        if (adapter != null) adapter = null;
        if (mClickListener != null) mClickListener = null;
    }

    private void releaseUI() {
        if(songTv != null) songTv = null;
        if(singerTv != null) singerTv = null;
        if(albumImage_Iv != null){
            albumImage_Iv.setImageBitmap(null);
            albumImage_Iv = null;
        }
        if (musicRv != null){
            musicRv.setAdapter(null);
            musicRv = null;
        }
        if(UILayout != null) UILayout = null;
    }

}