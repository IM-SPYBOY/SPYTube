package com.spytube.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spytube.app.R;
import com.spytube.app.models.ContentRow;
import com.spytube.app.models.MediaItem;

import java.util.List;

public class RowAdapter extends RecyclerView.Adapter<RowAdapter.ViewHolder> {

    private final List<ContentRow> rows;
    private final Context context;
    private final MediaAdapter.OnItemClickListener listener;
    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    public RowAdapter(Context context, List<ContentRow> rows, MediaAdapter.OnItemClickListener listener) {
        this.context = context;
        this.rows = rows;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentRow row = rows.get(position);
        holder.bind(row);
    }

    @Override
    public int getItemCount() {
        return rows != null ? rows.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView rowTitle;
        RecyclerView rowRecycler;

        ViewHolder(View itemView) {
            super(itemView);
            rowTitle = itemView.findViewById(R.id.row_title);
            rowRecycler = itemView.findViewById(R.id.recycler_view);
        }

        void bind(ContentRow row) {
            rowTitle.setText(row.title);

            MediaAdapter adapter = new MediaAdapter(context, row.items, listener);
            rowRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            rowRecycler.setAdapter(adapter);
            rowRecycler.setHasFixedSize(true);
            rowRecycler.setRecycledViewPool(sharedPool);
        }
    }
}
