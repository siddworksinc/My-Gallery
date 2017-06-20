package com.simplemobiletools.gallery.activities

import android.os.Bundle
import com.simplemobiletools.gallery.helpers.logEvent

class VideoActivity : PhotoVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        PhotoVideoActivity.mIsVideo = true
        super.onCreate(savedInstanceState)
        logEvent("ActivityVideo")
    }
}
