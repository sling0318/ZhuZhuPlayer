package com.example.wangyiyunmusic.dialog;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.wangyiyunmusic.R;

import org.jetbrains.annotations.NotNull;

import fun.inaction.dialog.ViewAdapter;

public class DialogSongHeader implements ViewAdapter {

    private ViewGroup parent;
    private View view;
    private ImageView albumPic;
    private TextView songName;
    private TextView artistName;

    public DialogSongHeader(ViewGroup parent) {
        this.parent = parent;
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_view_song_header,parent,false);
        albumPic = view.findViewById(R.id.albumPic);
        songName = view.findViewById(R.id.songName);
        artistName = view.findViewById(R.id.artistName);
    }

    @NotNull
    @Override
    public View getView() {
        return view;
    }

    public void setAlbumPic(Bitmap bitmap){
        albumPic.setImageBitmap(bitmap);
    }

    public void setSongName(String text){
        songName.setText(text);
    }

    public void setArtistName(String text){
        artistName.setText(text);
    }

}
