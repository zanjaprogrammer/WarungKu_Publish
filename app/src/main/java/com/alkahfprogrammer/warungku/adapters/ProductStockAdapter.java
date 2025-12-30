package com.alkahfprogrammer.warungku.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.alkahfprogrammer.warungku.R;
import com.alkahfprogrammer.warungku.data.entity.Product;
import com.alkahfprogrammer.warungku.databinding.ItemProductStockBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductStockAdapter extends RecyclerView.Adapter<ProductStockAdapter.ViewHolder> {

    private List<Product> products = new ArrayList<>();
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductStockAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductStockBinding binding = ItemProductStockBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.binding.tvName.setText(product.name);
        holder.binding.tvPrice.setText(formatter.format(product.sellPrice));
        holder.binding.tvStock.setText(String.valueOf(product.currentStock));

        // Load product image
        if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .transform(new RoundedCorners(16)))
                    .into(holder.binding.ivProductImage);
        } else {
            holder.binding.ivProductImage.setImageResource(R.drawable.ic_image_placeholder);
        }

        if (product.currentStock == 0) {
            // Stok habis - MERAH
            holder.binding.tvStatus.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvStatus.setText("Habis");
            holder.binding.tvStatus.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
        } else if (product.currentStock <= product.minStock) {
            // Stok hampir habis - ORANYE
            holder.binding.tvStatus.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvStatus.setText("Hampir habis");
            holder.binding.tvStatus.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning));
        } else {
            holder.binding.tvStatus.setVisibility(android.view.View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemProductStockBinding binding;

        ViewHolder(ItemProductStockBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
