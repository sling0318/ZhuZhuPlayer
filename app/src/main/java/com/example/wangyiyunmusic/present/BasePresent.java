package com.example.wangyiyunmusic.present;

import android.content.Context;

import com.example.wangyiyunmusic.view.base.BaseView;

import java.lang.ref.WeakReference;

public class BasePresent<T extends BaseView>{
    Context context;

    //    持有View层的利用
//    LocalMusicView mLocalMusicView;
    protected WeakReference<T> mLocalMusicView;//弱引用


    /**
     * 绑定
     */
    public void attachView(T view,Context context){
        mLocalMusicView = new WeakReference<>(view);
        this.context = context.getApplicationContext();
    }

    /**
     * 解绑
     */
    public void detachView(){
        if (mLocalMusicView != null) {
            mLocalMusicView.clear();
            mLocalMusicView = null;
        }
    }
}
