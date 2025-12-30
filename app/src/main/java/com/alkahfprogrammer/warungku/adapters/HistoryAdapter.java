package com.alkahfprogrammer.warungku.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.alkahfprogrammer.warungku.R;
import com.alkahfprogrammer.warungku.data.entity.CashFlow;
import com.alkahfprogrammer.warungku.databinding.ItemHistoryBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<CashFlow> items = new ArrayList<>();
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public void setItems(List<CashFlow> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CashFlow item = items.get(position);
        holder.binding.tvDescription.setText(item.description);
        holder.binding.tvDate.setText(dateFormat.format(new Date(item.timestamp)));

        if ("IN".equals(item.type)) {
            holder.binding.tvAmount.setText("+ " + formatter.format(item.amount));
            holder.binding.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        } else {
            holder.binding.tvAmount.setText("- " + formatter.format(item.amount));
            holder.binding.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemHistoryBinding binding;

        ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
