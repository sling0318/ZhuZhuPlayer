package com.example.wangyiyunmusic.model;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.wangyiyunmusic.bean.MusicBean;

import java.util.List;

public interface MusicModel {
    void showLocalMusic(OnLoadMusicListener onLoadMusicListener, Context context);
//    void loadAlbumBitMap(OnLoadAlbumBitMapCallBack onAlbumCallBack, String Path);



    interface OnLoadMusicListener{
        void onComplete(List<MusicBean> beans);
    }

//    interface OnLoadAlbumBitMapCallBack{
//        void onCallBack(Bitmap bitmap);
//    }
}
