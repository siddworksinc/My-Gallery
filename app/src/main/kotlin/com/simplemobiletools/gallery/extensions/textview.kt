package com.simplemobiletools.gallery.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.simplemobiletools.commons.views.MyTextView



fun MyTextView.setDrawableColor(color: Int) {
    for (drawable in this.compoundDrawables) {
        drawable?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}