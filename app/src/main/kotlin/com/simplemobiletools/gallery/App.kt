package com.simplemobiletools.gallery

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.simplemobiletools.gallery.helpers.logEvent
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric

class App : Application() {
    val USE_LEAK_CANARY = false
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Fabric.with(this, Answers())
        logEvent("VersionCode"+BuildConfig.VERSION_CODE)
        if (USE_LEAK_CANARY) {
            if (!LeakCanary.isInAnalyzerProcess(this)) {
                LeakCanary.install(this)
            }
        }
    }
}
