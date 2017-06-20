package com.simplemobiletools.gallery.activities

import android.os.Bundle
import com.simplemobiletools.gallery.helpers.logEvent

class PhotoActivity : PhotoVideoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        PhotoVideoActivity.mIsVideo = false
        super.onCreate(savedInstanceState)
        logEvent("ActivityPhoto")
    }
}
