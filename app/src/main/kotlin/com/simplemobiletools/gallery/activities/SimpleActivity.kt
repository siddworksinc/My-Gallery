package com.simplemobiletools.gallery.activities

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.PickAlbumDialog
import java.io.File
import java.util.*

open class SimpleActivity : BaseSimpleActivity() {
    fun tryCopyMoveFilesTo(files: ArrayList<File>, isCopyOperation: Boolean, callback: () -> Unit) {
        if (files.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            return
        }

        val source = if (files[0].isFile) files[0].parent else files[0].absolutePath
        PickAlbumDialog(this, source, null) {
            copyMoveFilesTo(files, source.trimEnd('/'), it.path, isCopyOperation, true, callback)
        }
    }
}
