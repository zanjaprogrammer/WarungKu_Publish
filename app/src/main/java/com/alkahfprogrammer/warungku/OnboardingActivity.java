package com.alkahfprogrammer.warungku;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.alkahfprogrammer.warungku.adapters.OnboardingAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button btnNext;
    private Button btnSkip;
    private OnboardingAdapter adapter;
    private static final String PREF_NAME = "WarungKuPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        
        // Debug: Log adapter item count
        android.util.Log.d("OnboardingActivity", "Adapter item count: " + adapter.getItemCount());
        android.util.Log.d("OnboardingActivity", "ViewPager width: " + viewPager.getWidth() + ", height: " + viewPager.getHeight());
        android.util.Log.d("OnboardingActivity", "ViewPager setup complete");
        
        // Force layout to ensure ViewPager gets proper dimensions
        viewPager.post(() -> {
            android.util.Log.d("OnboardingActivity", "ViewPager post - width: " + viewPager.getWidth() + ", height: " + viewPager.getHeight());
        });

        // Setup TabLayout dengan ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            // Tab indicator dots
        }).attach();

        // Update button text berdasarkan posisi
        updateButtonText();

        // Listener untuk ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateButtonText();
            }
        });

        // Button listeners
        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(currentItem + 1, true);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void updateButtonText() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem == adapter.getItemCount() - 1) {
            btnNext.setText("Mulai");
        } else {
            btnNext.setText("Lanjut");
        }
    }

    private void finishOnboarding() {
        // Mark onboarding as completed
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).commit(); // Use commit() instead of apply() to ensure it's saved immediately
        
        android.util.Log.d("OnboardingActivity", "Onboarding completed! first_launch set to false");

        // Go to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public static boolean isFirstLaunch(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * Reset onboarding untuk testing (force show onboarding)
     */
    public static void resetOnboarding(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply();
    }
}

