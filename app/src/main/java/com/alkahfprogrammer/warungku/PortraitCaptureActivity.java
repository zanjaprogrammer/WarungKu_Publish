package com.alkahfprogrammer.warungku;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Custom CaptureActivity yang memaksa portrait mode
 */
public class PortraitCaptureActivity extends CaptureActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}

