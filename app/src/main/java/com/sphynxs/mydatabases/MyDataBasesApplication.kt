package com.sphynxs.mydatabases

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyDataBasesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Inicialización de la aplicación
    }
}
