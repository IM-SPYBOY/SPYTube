package com.spytube.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.spytube.app.R;
import com.spytube.app.api.ApiClient;
import com.spytube.app.models.MediaItem;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    private final List<MediaItem> items;
    private final Context context;
    private final OnItemClickListener listener;
    private final boolean isGrid;

    public interface OnItemClickListener {
        void onItemClick(MediaItem item);
    }

    public MediaAdapter(Context context, List<MediaItem> items, OnItemClickListener listener) {
        this(context, items, listener, false);
    }

    public MediaAdapter(Context context, List<MediaItem> items, OnItemClickListener listener, boolean isGrid) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.isGrid = isGrid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isGrid ? R.layout.item_media_grid : R.layout.item_media;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView rating;

        ViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.poster);
            rating = itemView.findViewById(R.id.rating);
        }

        void bind(MediaItem item) {
            // Load poster with Glide
            String posterUrl = ApiClient.getPosterUrl(item.posterPath);
            if (posterUrl != null) {
                Glide.with(context)
                        .load(posterUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(poster);
            }

            // Show rating badge for high-rated content
            if (rating != null && item.voteAverage >= 7.5) {
                rating.setVisibility(View.VISIBLE);
                rating.setText(item.getRating());
            } else if (rating != null) {
                rating.setVisibility(View.GONE);
            }

            // Click handler
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
