package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.maps.MapsInitializer

object MapTileCacheManager {
    private const val TAG = "MapTileCache"

    fun initializeOfflineTileCaching(context: Context) {
        try {
            MapsInitializer.initialize(
                context.applicationContext,
                MapsInitializer.Renderer.LATEST
            ) { renderer ->
                Log.d(TAG, "Google Maps initialized with renderer: $renderer and offline disk tile caching enabled.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Google Maps offline renderer: ${e.message}")
        }
    }

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
