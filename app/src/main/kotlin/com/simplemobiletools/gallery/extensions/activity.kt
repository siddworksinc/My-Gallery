package com.simplemobiletools.gallery.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.helpers.NOMEDIA
import com.simplemobiletools.gallery.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.gallery.helpers.REQUEST_SET_WALLPAPER
import com.simplemobiletools.gallery.helpers.dpToPx
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.views.MySquareImageView
import java.io.File
import java.util.*

fun Activity.shareUri(medium: Medium, uri: Uri) {
    val shareTitle = resources.getString(R.string.share_via)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = medium.getMimeType()
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedium(medium: Medium) {
    val shareTitle = resources.getString(R.string.share_via)
    val file = File(medium.path)
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = medium.getMimeType()
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.shareMedia(media: List<Medium>) {
    val shareTitle = resources.getString(R.string.share_via)
    val uris = ArrayList<Uri>(media.size)
    Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "image/* video/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        media.map { File(it.path) }
                .mapTo(uris) { Uri.fromFile(it) }

        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(this, shareTitle))
    }
}

fun Activity.trySetAsWallpaper(file: File) {
    try {
        var uri = Uri.fromFile(file)
        if (!setAsWallpaper(uri, file)) {
            uri = getFileContentUri(file)
            setAsWallpaper(uri, file, false)
        }
    } catch (e: Exception) {
        toast(R.string.unknown_error_occurred)
    }
}

fun Activity.setAsWallpaper(uri: Uri, file: File, showToast: Boolean = true): Boolean {
    var success = false
    Intent().apply {
        action = Intent.ACTION_ATTACH_DATA
        setDataAndType(uri, file.getMimeType("image/*"))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(this, getString(R.string.set_as_wallpaper_with))

        if (resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, REQUEST_SET_WALLPAPER)
            success = true
        } else {
            if (showToast) {
                toast(R.string.no_wallpaper_setter_found)
            }
            success = false
        }
    }

    return success
}

fun Activity.getFileContentUri(file: File): Uri? {
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = "${MediaStore.Images.Media.DATA} = ?"
    val selectionArgs = arrayOf(file.absolutePath)

    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val id = cursor.getIntValue(MediaStore.Images.Media._ID)
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "$id")
        }
    } finally {
        cursor?.close()
    }
    return null
}

fun Activity.openWith(file: File, forceChooser: Boolean = true) {
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, file.getMimeType())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(this, getString(R.string.open_with))

        if (resolveActivity(packageManager) != null) {
            startActivity(if (forceChooser) chooser else this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Activity.openEditor(file: File) {
    val uri = Uri.fromFile(file)
    Intent().apply {
        action = Intent.ACTION_EDIT
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (resolveActivity(packageManager) != null) {
            startActivityForResult(this, REQUEST_EDIT_IMAGE)
        } else {
            toast(R.string.no_editor_found)
        }
    }
}

fun Activity.hasNavBar(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val display = windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)

        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    } else {
        val hasMenuKey = ViewConfiguration.get(applicationContext).hasPermanentMenuKey()
        val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        !hasMenuKey && !hasBackKey
    }
}

fun Activity.launchCamera() {
    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        toast(R.string.no_camera_app_found)
    }
}

fun SimpleActivity.launchAbout() {
    startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_GLIDE or LICENSE_CROPPER or LICENSE_MULTISELECT or LICENSE_RTL
            or LICENSE_PHOTOVIEW or LICENSE_SUBSAMPLING, BuildConfig.VERSION_NAME)
}

fun AppCompatActivity.showSystemUI() {
    supportActionBar?.show()
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun AppCompatActivity.hideSystemUI() {
    supportActionBar?.hide()
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE
}

fun SimpleActivity.addNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    if (file.exists())
        return

    if (needsStupidWritePermissions(path)) {
        handleSAFDialog(file) {
            try {
                getFileDocument(path)?.createFile("", NOMEDIA)
            } catch (e: Exception) {
                toast(R.string.unknown_error_occurred)
            }
        }
    } else {
        try {
            file.createNewFile()
        } catch (e: Exception) {
            toast(R.string.unknown_error_occurred)
        }
    }
    scanFile(file) {
        callback()
    }
}

fun SimpleActivity.removeNoMedia(path: String, callback: () -> Unit) {
    val file = File(path, NOMEDIA)
    deleteFile(file) {
        callback()
    }
}

fun SimpleActivity.toggleFileVisibility(oldFile: File, hide: Boolean, callback: (newFile: File) -> Unit) {
    val path = oldFile.parent
    var filename = oldFile.name
    if (hide) {
        filename = ".${filename.trimStart('.')}"
    } else {
        filename = filename.substring(1, filename.length)
    }
    val newFile = File(path, filename)
    renameFile(oldFile, newFile) {
        newFile.setLastModified(System.currentTimeMillis())
        callback(newFile)
    }
}

fun Activity.getFileSignature(path: String) = StringSignature(File(path).lastModified().toString())

fun Activity.loadImage(path: String, target: MySquareImageView) {
    if (path.isImageFast() || path.isVideoFast()) {
        if (path.isPng()) {
            loadPng(path, target)
        } else {
            loadJpg(path, target)
        }
    } else if (path.isGif()) {
        if (config.animateGifs) {
            loadAnimatedGif(path, target)
        } else {
            loadStaticGif(path, target)
        }
    }
}

fun Activity.loadPng(path: String, target: MySquareImageView) {
    val builder = Glide.with(applicationContext)
            .load(path)
            .asBitmap()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .format(DecodeFormat.PREFER_ARGB_8888)

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadJpg(path: String, target: MySquareImageView) {
    val builder = Glide.with(applicationContext)
            .load(path)
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .crossFade()

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadImageFromResource(id: Int, target: MySquareImageView) {
    Glide.with(applicationContext)
            .load(id)
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .into(target)
}

fun Activity.loadAnimatedGif(path: String, target: MySquareImageView) {
    val builder = Glide.with(applicationContext)
            .load(path)
            .asGif()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .crossFade()

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.loadStaticGif(path: String, target: MySquareImageView) {
    val builder = Glide.with(applicationContext)
            .load(path)
            .asBitmap()
            .signature(getFileSignature(path))
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)

    if (config.cropThumbnails) builder.centerCrop() else builder.fitCenter()
    builder.into(target)
}

fun Activity.getCachedDirectories(): ArrayList<Directory> {
    val token = object : TypeToken<List<Directory>>() {}.type
    return Gson().fromJson<ArrayList<Directory>>(config.directories, token) ?: ArrayList<Directory>(1)
}

fun Activity.loadImageForShortcut(shortcut: Directory, target: MySquareImageView) {
    if(shortcut.isThumbnailHidden) {
        loadImageFromResource(R.drawable.ic_folder_gallery3, target)
    } else {
        if(shortcut.coverImage != null) {
            loadImage(shortcut.coverImage as String, target)
        } else {
            if(shortcut.tmb != "") {
                loadImage(shortcut.tmb, target)
            } else {
                loadImageFromResource(R.drawable.ic_folder_gallery, target)
            }
        }
    }
}

@SuppressLint("NewApi")
fun Activity.init() {
    baseConfig.appRunCount++
    if (baseConfig.appRunCount == 50 || baseConfig.appRunCount == 300 || baseConfig.appRunCount == 1000) {
//        DonateDialog(this)
    }

    Thread({
        baseConfig.internalStoragePath = getInternalStoragePath()
        baseConfig.sdCardPath = getSDCardPath().trimEnd('/')
    }).start()
}

fun Activity.alert(okText: String, message: String, function: () -> Unit?) {
    val view  = MyTextView(this)
    view.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
    view.text = message
    AlertDialog.Builder(this)
            .setPositiveButton(okText, null)
            .create().apply {
        setupDialogStuff(view, this, R.string.additional_info)
    }
}

