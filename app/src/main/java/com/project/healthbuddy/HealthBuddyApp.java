package com.project.healthbuddy;

import android.app.Application;
import com.project.healthbuddy.ui.settings.ThemeManager;

public class HealthBuddyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.init(this);
    }
}
