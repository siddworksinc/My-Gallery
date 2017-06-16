package com.simplemobiletools.gallery.helpers

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent

/**
 * Created by sidd on 1/6/17.
 */

fun logEvent(event: String) {
    Answers.getInstance().logCustom(CustomEvent(event))
}

fun logException(ex: Exception) {
    ex.printStackTrace()
    Crashlytics.logException(ex)
}

