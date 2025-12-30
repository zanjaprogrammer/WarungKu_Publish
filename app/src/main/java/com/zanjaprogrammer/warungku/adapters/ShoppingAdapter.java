package com.zanjaprogrammer.warungku.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zanjaprogrammer.warungku.data.entity.Product;
import com.zanjaprogrammer.warungku.databinding.ItemShoppingBinding;

import java.util.ArrayList;
import java.util.List;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private List<Product> products = new ArrayList<>();

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShoppingBinding binding = ItemShoppingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.binding.tvName.setText(product.name);
        holder.binding.tvStockInfo.setText("Sisa stok: " + product.currentStock);
        holder.binding.tvSalesCount.setText(product.salesCount + " kali terjual");
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemShoppingBinding binding;

        ViewHolder(ItemShoppingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
