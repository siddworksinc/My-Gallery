package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import com.siddworks.android.mygallery.ShortcutsActivity
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.logEvent
import kotlinx.android.synthetic.main.activity_password_locked.*

class PasswordLockedActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_locked)
        logEvent("ActivityPasswordLocked")

        updateTextColors(root)
        var accentColor = baseConfig.customPrimaryColor
        pass_button.setBackgroundColor(baseConfig.customPrimaryColor)
        pass_button.background?.mutate()?.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)

        pass_button.setOnClickListener {
            hideKeyboard()
            if(pass.text?.toString() == config.masterPass) {
                startActivity(Intent(this, ShortcutsActivity::class.java))
                finish()
            } else {
                toast("Incorrect Password")
            }
        }
    }
}
