package com.zanjaprogrammer.warungku;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zanjaprogrammer.warungku.adapters.ShoppingAdapter;
import com.zanjaprogrammer.warungku.databinding.ActivityShoppingListBinding;
import com.zanjaprogrammer.warungku.viewmodel.AppViewModel;

public class ShoppingListActivity extends AppCompatActivity {

    private ActivityShoppingListBinding binding;
    private AppViewModel viewModel;
    private ShoppingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityShoppingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use singleton instance untuk persist cart across activities
        viewModel = AppViewModel.getInstance(getApplication());
        setupRecyclerView();

        viewModel.getShoppingList().observe(this, products -> {
            adapter.setProducts(products);
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ShoppingAdapter();
        binding.rvShopping.setLayoutManager(new LinearLayoutManager(this));
        binding.rvShopping.setAdapter(adapter);
    }
}
