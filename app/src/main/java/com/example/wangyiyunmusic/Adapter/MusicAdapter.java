package com.example.wangyiyunmusic.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wangyiyunmusic.R;
import com.example.wangyiyunmusic.bean.MusicBean;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    Context context;
    List<MusicBean> beans;
//    点击事件接口
    private ItemClickListener mItemClickListener;


    public void setmItemClickListener(ItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public interface ItemClickListener{
        void ItemClick(View v,int position);
        void ItemViewClick(View v,int position);
    }

    public MusicAdapter(Context context, List<MusicBean> mData) {
        this.context = context;
        this.beans = mData;
    }

    @NonNull
    @Override
    public MusicAdapter.MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_music,parent,false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder,int position) {
        MusicBean bean = beans.get(position);
        holder.id_Tv.setText(bean.getId());
        holder.song_Tv.setText(bean.getTitle());
        holder.singer_Tv.setText(bean.getArtist());
        holder.album_Tv.setText(bean.getAlbum());

        holder.mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.ItemClick(v,position);
            }
        });

        holder.more_Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.ItemViewClick(v,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        if(beans != null && beans.size() > 0) return beans.size();
        return 0;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if(beans != null){
            if(beans.size() > 0) beans.clear();
            beans = null;
        }
        if(context != null) context = null;
    }

    public class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView id_Tv,song_Tv,singer_Tv,album_Tv,time_Tv;
        ConstraintLayout mLayout;
        ImageView more_Tv;
        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            id_Tv = itemView.findViewById(R.id.item_list_id);
            song_Tv = itemView.findViewById(R.id.item_list_song);
            singer_Tv = itemView.findViewById(R.id.item_list_singert);
            album_Tv = itemView.findViewById(R.id.item_list_album);
            mLayout = itemView.findViewById(R.id.item_list);
            more_Tv = itemView.findViewById(R.id.item_list_more);
        }
    }
}
