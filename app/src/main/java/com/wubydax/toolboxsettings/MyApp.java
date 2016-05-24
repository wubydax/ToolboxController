package com.wubydax.toolboxsettings;

import android.app.Application;

/**
 * Created by Anna Berkovitch on 24/05/2016.
 */
public class MyApp extends Application {

    @Override
    public void onLowMemory() {
        Runtime.getRuntime().gc();
        super.onLowMemory();
    }
}
