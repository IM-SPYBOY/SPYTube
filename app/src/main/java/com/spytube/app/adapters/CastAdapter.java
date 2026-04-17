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
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.spytube.app.R;
import com.spytube.app.api.ApiClient;
import com.spytube.app.models.CreditsResponse;

import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.ViewHolder> {

    private final List<CreditsResponse.CastMember> cast;
    private final Context context;

    public CastAdapter(Context context, List<CreditsResponse.CastMember> cast) {
        this.context = context;
        this.cast = cast;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CreditsResponse.CastMember member = cast.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return cast != null ? cast.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView actorName;
        TextView characterName;

        ViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            actorName = itemView.findViewById(R.id.actor_name);
            characterName = itemView.findViewById(R.id.character_name);
        }

        void bind(CreditsResponse.CastMember member) {
            actorName.setText(member.name);
            characterName.setText(member.character);

            String profileUrl = ApiClient.getProfileUrl(member.profile_path);
            if (profileUrl != null) {
                profileImage.clearColorFilter();
                Glide.with(context)
                        .load(profileUrl)
                        .transform(new CircleCrop())
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_home);
                profileImage.setColorFilter(context.getResources().getColor(R.color.text_muted, null));
            }
        }
    }
}
