package com.example.wangyiyunmusic.view;

import com.example.wangyiyunmusic.bean.MusicBean;
import com.example.wangyiyunmusic.view.base.BaseView;

import java.util.List;

public interface LocalMusicView extends BaseView {
    void showLocalMusic(List<MusicBean> beans);
//    void showAlbumBitMap(Bitmap bitmap);
}
