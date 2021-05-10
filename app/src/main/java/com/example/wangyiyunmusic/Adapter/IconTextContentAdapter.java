package com.example.wangyiyunmusic.Adapter;



import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.wangyiyunmusic.R;

import org.jetbrains.annotations.NotNull;

import kotlin.Pair;

public class IconTextContentAdapter extends BaseQuickAdapter<Pair<Integer,String>, BaseViewHolder> {


    public IconTextContentAdapter() {
        super(R.layout.item_song_options);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, Pair<Integer, String> integerStringPair) {
        baseViewHolder.setImageResource(R.id.iconImageView,integerStringPair.getFirst());
        baseViewHolder.setText(R.id.text,integerStringPair.getSecond());
    }
}
