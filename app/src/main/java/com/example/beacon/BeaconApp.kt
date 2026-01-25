package com.example.beacon

import android.app.Application
import org.maplibre.android.MapLibre

class BeaconApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre; no API key needed for demo tiles
        MapLibre.getInstance(this)
    }
}
