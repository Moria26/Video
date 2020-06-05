package com.example.video;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    Context context;
    ArrayList<Video> videos;
    public  VideoAdapter(Context context, ArrayList<Video> videos){
         this.context =context;
         this.videos =videos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Video item = videos.get(position);
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.error_img)
                .fallback(R.drawable.error_img)
                .error(R.drawable.error_img);
        Glide.with(context).load(item.getFeedurl()).apply(options).into(holder.icImage);
        holder.tvDes.setText(item.getDescription());
        holder.tvName.setText(item.getNickname());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener!=null){
                    onItemClickListener.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    class  ViewHolder extends RecyclerView.ViewHolder{
        ImageView icImage;
        TextView tvName;
        TextView tvDes;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icImage = itemView.findViewById(R.id.iv_img);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDes = itemView.findViewById(R.id.tv_des);
        }
    }
    interface OnItemClickListener{
        void onItemClick(int position);
    }
    OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
