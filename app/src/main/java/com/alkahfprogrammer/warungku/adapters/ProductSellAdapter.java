package com.alkahfprogrammer.warungku.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.alkahfprogrammer.warungku.R;
import com.alkahfprogrammer.warungku.data.entity.Product;
import com.alkahfprogrammer.warungku.databinding.ItemProductSellBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSellAdapter extends RecyclerView.Adapter<ProductSellAdapter.ViewHolder> {

    private List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));

    public interface OnProductClickListener {
        void onProductClick(Product product);

        void onProductLongClick(Product product);
    }

    public ProductSellAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductSellBinding binding = ItemProductSellBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.binding.tvProductName.setText(product.name);
        holder.binding.tvProductPrice.setText(formatter.format(product.sellPrice));
        holder.binding.tvProductStock.setText("Stok: " + product.currentStock);
        
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
        
        // Show/hide favorite icon
        if (holder.binding.ivFavorite != null) {
            holder.binding.ivFavorite.setVisibility(product.isFavorite ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onProductLongClick(product);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemProductSellBinding binding;

        ViewHolder(ItemProductSellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
