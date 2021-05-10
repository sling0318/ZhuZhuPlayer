package com.example.wangyiyunmusic.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseService extends Service {

    private static final String TAG = "BaseService状况";
    protected static final  String PLAYER_SORT_PLAY = "SORT_PLAY";
    protected static final  String PLAYER_RANDOM_PLAY = "RANDOM_PLAY";
    protected static final  String PLAYER_REPEAT_PLAY = "REPEAT_PLAY";

    private serviceBroadcastReceiver mServiceBroadcastReceiver;

    private boolean isServiceDestroy = false;
    protected boolean isFirstPlayBase = true;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        isServiceDestroy = true;
        //如果注册了广播接收器，则清除它
        if (!isRunningForeground() && mServiceBroadcastReceiver != null) {
            unregisterReceiver(mServiceBroadcastReceiver);
            mServiceBroadcastReceiver = null;
        }
    }

    protected void init(){}
    protected void receivePlay(String action){} //空实现

    //接收广播后要进行的动作
    private  class serviceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d(TAG, "onReceive: "+action);
            receivePlay(action);
        }
    }//class myBroadcastReceiver end
    //用来控制应用前后台切换的逻辑
    protected boolean isRunningForeground() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = get(activityManager);
        // 枚举进程
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfos) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(this.getApplicationInfo().processName)) {
                    //Log.d(TAG,"EntryActivity isRunningForeGround");
                    return true;
                }
            }
        }
        //Log.d(TAG, "EntryActivity isRunningBackGround");
        return false;
    }
    private List<ActivityManager.RunningAppProcessInfo> get(ActivityManager activityManager){
        List<ActivityManager.RunningAppProcessInfo> a = new ArrayList<>();
        return Optional.of(activityManager).map(new Function<ActivityManager, List<ActivityManager.RunningAppProcessInfo>>() {
            @Override
            public List<ActivityManager.RunningAppProcessInfo> apply(ActivityManager activityManager) {
                return activityManager.getRunningAppProcesses();
            }
        }).orElse(a);
    }
    //监听Activity的生命周期
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void activityStart(LifecycleOwner owner){
        if (!isRunningForeground())
            Log.d(TAG, owner.getClass().getSimpleName()+" start");
        else {
            if(mServiceBroadcastReceiver != null && !isServiceDestroy){
                Log.d(TAG, "注销广播");
                unregisterReceiver(mServiceBroadcastReceiver);
                mServiceBroadcastReceiver = null;
            }
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void activityResume(LifecycleOwner owner){
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void activityStop(LifecycleOwner owner){
        if (!isRunningForeground())
            Log.d(TAG, owner.getClass().getSimpleName()+" Stop");
        //启动定时任务，本应用退至后台500ms后注册广播接收器
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRunningForeground() && mServiceBroadcastReceiver == null && !isServiceDestroy && !isFirstPlayBase) {
                    Log.d(TAG, "接收广播");
                    //接收广播
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("play/pause");
                    filter.addAction("next");
                    filter.addAction("previous");
                    filter.addAction("love");
                    filter.addAction("close");
                    mServiceBroadcastReceiver = new serviceBroadcastReceiver();
                    registerReceiver(mServiceBroadcastReceiver,filter);
                }
            }
        },500);
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void activityDestroy(LifecycleOwner owner){
    }
}
