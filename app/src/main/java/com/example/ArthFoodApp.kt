package com.example

import android.app.Application
import com.example.data.local.ArthFoodDatabase
import com.example.data.local.PreferencesStore
import com.example.data.repository.ArthFoodRepository
import com.example.licensing.LicenseManager

class AppContainer(application: Application) {
    val database: ArthFoodDatabase by lazy { ArthFoodDatabase.getInstance(application) }
    val preferencesStore: PreferencesStore by lazy { PreferencesStore(application) }
    val repository: ArthFoodRepository by lazy {
        ArthFoodRepository(application, database, preferencesStore)
    }
    val licenseManager: LicenseManager by lazy {
        LicenseManager(application, preferencesStore)
    }
}

class ArthFoodApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
