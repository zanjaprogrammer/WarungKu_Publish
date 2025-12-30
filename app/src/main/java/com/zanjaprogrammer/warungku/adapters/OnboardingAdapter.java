package com.zanjaprogrammer.warungku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zanjaprogrammer.warungku.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private static final int[] TITLES = {
        R.string.onboarding_welcome_title,
        R.string.onboarding_features_title,
        R.string.onboarding_tips_title,
        R.string.onboarding_about_title
    };

    private static final int[] DESCRIPTIONS = {
        R.string.onboarding_welcome_desc,
        R.string.onboarding_features_desc,
        R.string.onboarding_tips_desc,
        R.string.onboarding_about_desc
    };

    private static final int[] IMAGES = {
        R.drawable.ic_home, // Placeholder, bisa diganti dengan icon khusus
        R.drawable.ic_stock,
        R.drawable.ic_sell,
        R.drawable.ic_summary
    };

    private final android.content.Context context;

    public OnboardingAdapter(android.content.Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        try {
            holder.imageView.setImageResource(IMAGES[position]);
            holder.titleView.setText(TITLES[position]);
            holder.descriptionView.setText(DESCRIPTIONS[position]);
            android.util.Log.d("OnboardingAdapter", "Binding position " + position + 
                ", title: " + context.getString(TITLES[position]));
        } catch (Exception e) {
            android.util.Log.e("OnboardingAdapter", "Error binding view at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return TITLES.length;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView descriptionView;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivOnboarding);
            titleView = itemView.findViewById(R.id.tvOnboardingTitle);
            descriptionView = itemView.findViewById(R.id.tvOnboardingDesc);
        }
    }
}

