package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.gallery.extensions.config

class SplashActivity : SimpleActivity() {

    private val  REQUEST_CODE_INTRO: Int = 1337

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(config.tutorialCompleted) {
            navigate()
        } else {
            Intent(this, TutorialActivity::class.java).apply {
                startActivityForResult(this, REQUEST_CODE_INTRO)
            }
        }
    }

    private fun navigate() {
        if(config.appLocked != null) {
            startActivity(Intent(this, PasswordLockedActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, ShortcutsActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_INTRO) {
            if (resultCode == Activity.RESULT_OK) {
                config.tutorialCompleted = true
                navigate()
            } else {
                navigate()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
