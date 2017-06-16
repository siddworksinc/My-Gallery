package com.simplemobiletools.gallery

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.simplemobiletools.gallery.helpers.logEvent
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
        Fabric.with(this, Crashlytics())
        Fabric.with(this, Answers())
        logEvent("VersionCode"+BuildConfig.VERSION_CODE)
    }
}
