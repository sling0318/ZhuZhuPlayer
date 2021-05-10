package com.example.wangyiyunmusic.present;

import android.graphics.Bitmap;

import com.example.wangyiyunmusic.view.LocalMusicView;
import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.model.LocalMusicModel;
import com.example.wangyiyunmusic.model.MusicModel;

import java.util.List;

public class MusicPresent<T extends LocalMusicView> extends BasePresent{


//    持有model的引用
    MusicModel mMusicModel = new LocalMusicModel();



    public void fetch(){
        if(mLocalMusicView != null && mMusicModel != null){
            mMusicModel.showLocalMusic(new MusicModel.OnLoadMusicListener() {
                @Override
                public void onComplete(List<MusicBean> beans) {
                    ((T)mLocalMusicView.get()).showLocalMusic(beans);
                }
            },context);
        }
    }

//    public void fetch(String Path){
//        if(mLocalMusicView != null && mMusicModel != null){
//            mMusicModel.loadAlbumBitMap(new MusicModel.OnLoadAlbumBitMapCallBack() {
//                @Override
//                public void onCallBack(Bitmap bitmap) {
//                    ((T)mLocalMusicView.get()).showAlbumBitMap(bitmap);
//                }
//            },Path);
//        }
//    }


}
