package com.example.wangyiyunmusic.dialog;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;


import com.example.wangyiyunmusic.R;
import com.example.wangyiyunmusic.bean.MusicBean;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;


public class SongOpDialog {

    private MusicBean bean;

    private BottomSheetDialog bottomDialog;
    private long playListId = 0;
    private Bitmap bitmap;



    public SongOpDialog(Context context, List<MusicBean>beans, int position, Bitmap bitmap){
        this.bitmap = bitmap;
        this.bean = beans.get(position);
        initDialog(context);
    }

    private void initDialog(Context context) {
        bottomDialog = new BottomSheetDialog(context);

        List<Pair<Integer, String>> items = new ArrayList<>();
        items.add(new Pair(R.drawable.ic_next_play_song, "下一首播放"));
        items.add(new Pair(R.drawable.ic_collect, "收藏到歌单"));
        items.add(new Pair(R.drawable.ic_info, "歌曲信息"));
        if(playListId != 0){
            items.add(new Pair(R.drawable.ic_delete, "删除"));
        }

        bottomDialog.setHeader(bitmap
                , bean.getTitle()
                , bean.getArtist())
                .setListData(items);

        bottomDialog.setOnItemClickListener((adapter, view, position) -> {
            switch (position) {
                // 下一首播放
                case 0:

                    bottomDialog.dismiss();
                    break;

                // 收藏到歌单
                case 1:
                    bottomDialog.dismiss();
                    collectToPlaylist(context);
                    break;

                // 歌曲信息
                case 2:

                    bottomDialog.dismiss();
                    break;

                // 删除
                case 4:
                    deleteFromPlaylist();
                    bottomDialog.dismiss();
                    break;

            }
        });
    }


    // 收藏到歌单
    private void collectToPlaylist(Context context) {

    }

    // 从歌单中删除
    private void deleteFromPlaylist(){

    }

    // 通知歌单变化

    public void show() {
        bottomDialog.show();

    }

}
