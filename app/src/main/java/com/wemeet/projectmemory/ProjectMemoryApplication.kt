package com.wemeet.projectmemory

import android.app.Application
import com.wemeet.projectmemory.di.AppContainer

class ProjectMemoryApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
