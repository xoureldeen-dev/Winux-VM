package com.vectras.boxvidra;

import android.app.Application;

public class VectrasApp extends Application {
    public static Application instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
