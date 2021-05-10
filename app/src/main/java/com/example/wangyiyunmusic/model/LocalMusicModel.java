package com.example.wangyiyunmusic.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.util.HtmlStringUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocalMusicModel implements MusicModel{
    @Override
    public void showLocalMusic(OnLoadMusicListener onLoadMusicListener, Context context) {
        onLoadMusicListener.onComplete(getLocalMusic(context));
    }

//    @Override
//    public void loadAlbumBitMap(OnLoadAlbumBitMapCallBack onAlbumCallBack, String Path) {
//        onAlbumCallBack.onCallBack(getAlbumBitmap(Path));
//    }

    private List<MusicBean> getLocalMusic(Context context){
        if (context == null) return null;

        context = context.getApplicationContext();
        List<MusicBean> beans = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri,null,null,null);

        int id = 0;
        while (cursor != null && cursor.moveToNext()) {
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            if(duration < 90000) continue;

            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            id++;
            String sid = String.valueOf(id);
            MusicBean bean = new MusicBean(sid,title,artist,album,path,path,duration);
            beans.add(bean);
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        return beans;
    }


//        private Bitmap getAlbumBitmap(String Path){
//            if (Path == null || TextUtils.isEmpty(Path)) return  null;
//            if (!HtmlStringUtil.FileExists(Path)) return null;
//
//            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//            mediaMetadataRetriever.setDataSource(Path);
//            byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();
//            mediaMetadataRetriever.release();
//            if (picture == null) return null;
//            return BitmapFactory.decodeByteArray(picture,0,picture.length);
//        }
}
