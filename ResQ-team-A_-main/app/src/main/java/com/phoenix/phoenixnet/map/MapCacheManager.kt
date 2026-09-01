package com.phoenix.phoenixnet.map

import android.content.Context
import android.util.Log
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to manage pre-downloading map tiles for offline emergency use.
 */
@Singleton
class MapCacheManager @Inject constructor() {

    companion object {
        private const val TAG = "MapCacheManager"
    }

    /**
     * Downloads tiles for a 5km radius around a point.
     */
    fun preCacheArea(mapView: MapView, center: GeoPoint, zoomMin: Int = 10, zoomMax: Int = 18) {
        val cacheManager = CacheManager(mapView)
        
        // Approx 0.05 degrees for 5km
        val boundingBox = BoundingBox(
            center.latitude + 0.05,
            center.longitude + 0.05,
            center.latitude - 0.05,
            center.longitude - 0.05
        )

        Log.i(TAG, "Starting map tile download for tactical area: $boundingBox")
        
        cacheManager.downloadAreaAsync(mapView.context, boundingBox, zoomMin, zoomMax)
    }
}
