package com.pythonide.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PythonIDEApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
