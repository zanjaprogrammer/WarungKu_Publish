package com.alkahfprogrammer.warungku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.alkahfprogrammer.warungku.R;
import com.alkahfprogrammer.warungku.data.entity.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductReportAdapter extends RecyclerView.Adapter<ProductReportAdapter.ViewHolder> {

    private List<Product> products = new ArrayList<>();
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    private final boolean isTopSelling; // true untuk top selling, false untuk unsold

    public ProductReportAdapter(boolean isTopSelling) {
        this.isTopSelling = isTopSelling;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.tvName.setText(product.name);
        holder.tvPrice.setText(formatter.format(product.sellPrice));

        if (isTopSelling) {
            // Untuk produk terlaris: tampilkan jumlah penjualan
            holder.tvInfo.setText("Terjual: " + product.salesCount + " unit");
            holder.tvInfo.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            // Untuk produk tidak laku: tampilkan stok
            holder.tvInfo.setText("Stok: " + product.currentStock);
            holder.tvInfo.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvName, tvPrice, tvInfo;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvInfo = itemView.findViewById(R.id.tvInfo);
        }
    }
}

