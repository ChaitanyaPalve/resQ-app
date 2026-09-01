package com.phoenix.phoenixnet

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.phoenix.phoenixnet.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration as OsmConfig
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class PhoenixNetApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid configuration
        OsmConfig.getInstance().userAgentValue = packageName
        val osmdroidDir = File(filesDir, "osmdroid")
        if (!osmdroidDir.exists()) osmdroidDir.mkdirs()
        OsmConfig.getInstance().osmdroidBasePath = osmdroidDir
        OsmConfig.getInstance().osmdroidTileCache = File(osmdroidDir, "tiles")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize periodic cloud synchronization
        syncManager.schedulePeriodicSync()
    }
}
