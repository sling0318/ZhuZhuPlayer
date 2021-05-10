package com.example.wangyiyunmusic.model;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.wangyiyunmusic.bean.MusicBean;

import java.util.List;

public interface SongSheetModel {
    void showLocalMusic(OnLoadMusicListener onLoadMusicListener, Context context);
    void loadAlbumBitmap(OnLoadAlbumBitmapCallBack onAlbumCallBack, String Path);

    interface OnLoadMusicListener{
        void onComplete(List<MusicBean> beans);
    }
    interface OnLoadAlbumBitmapCallBack {
        void onCallBack(Bitmap bitmap);
    }
}
