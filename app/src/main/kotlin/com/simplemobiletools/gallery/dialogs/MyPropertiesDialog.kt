package com.simplemobiletools.gallery.dialogs

/**
 * Created by sidd on 24/6/17.
 */

import android.app.Activity
import android.content.res.Resources
import android.media.ExifInterface
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.dialog_properties.view.*
import kotlinx.android.synthetic.main.property_item.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MyPropertiesDialog() {
    lateinit var mInflater: LayoutInflater
    lateinit var mPropertyView: ViewGroup
    lateinit var mResources: Resources

    private var mCountHiddenItems = false
    private var mFilesCnt = 0

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: Activity, path: String, countHiddenItems: Boolean = false) : this() {
        mCountHiddenItems = countHiddenItems
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = mInflater.inflate(R.layout.dialog_properties, null)
        mPropertyView = view.properties_holder

        val file = File(path)
        addProperty(R.string.name, file.name)
        addProperty(R.string.path, file.parent)
        addProperty(R.string.size, "...", R.id.properties_size)

        Thread({
            val size = getItemSize(file).formatSize()
            activity.runOnUiThread {
                (view.findViewById(R.id.properties_size).property_value as TextView).text = size

                if (file.isDirectory) {
                    (view.findViewById(R.id.properties_file_count).property_value as TextView).text = mFilesCnt.toString()
                }
            }
        }).start()

        if (file.isDirectory) {
            addProperty(R.string.direct_children_count, getDirectChildrenCount(file, countHiddenItems))
            addProperty(R.string.files_count, "...", R.id.properties_file_count)
        } else if (file.isImageSlow()) {
            addProperty(R.string.resolution, file.getResolution().formatAsResolution())
        } else if (file.isAudioSlow()) {
            file.getDuration()?.let { addProperty(R.string.duration, it) }
            file.getArtist()?.let { addProperty(R.string.artist, it) }
            file.getAlbum()?.let { addProperty(R.string.album, it) }
        } else if (file.isVideoSlow()) {
            file.getDuration()?.let { addProperty(R.string.duration, it) }
            addProperty(R.string.resolution, file.getResolution().formatAsResolution())
            file.getArtist()?.let { addProperty(R.string.artist, it) }
            file.getAlbum()?.let { addProperty(R.string.album, it) }
        }

        addProperty(R.string.last_modified, file.lastModified().formatLastModified())

        if (!file.isDirectory) {
            addExifProperties(path)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.properties)
        }
    }

    /**
     * A File Properties dialog constructor with an optional parameter, usable at multiple items selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes
     */
    constructor(activity: Activity, paths: List<String>, countHiddenItems: Boolean = false) : this() {
        mCountHiddenItems = countHiddenItems
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = mInflater.inflate(R.layout.dialog_properties, null)
        mPropertyView = view.properties_holder

        val files = ArrayList<File>(paths.size)
        paths.forEach { files.add(File(it)) }
        addProperty(R.string.items_selected, paths.size.toString())

        val passwordsString = activity.config.passwords
        val listType = object : TypeToken<HashMap<String, String>>() {}.type
        val pass =  Gson().fromJson<HashMap<String, String>>(passwordsString, listType) ?: HashMap(1)

        if(!paths.any { pass.contains(it) }) {
            if (isSameParent(files))
                addProperty(R.string.path, files[0].parent)
        }
        addProperty(R.string.size, "...", R.id.properties_size)
        addProperty(R.string.files_count, "...", R.id.properties_file_count)

        Thread({
            val size = getItemsSize(files).formatSize()
            activity.runOnUiThread {
                (view.findViewById(R.id.properties_size).property_value as TextView).text = size
                (view.findViewById(R.id.properties_file_count).property_value as TextView).text = mFilesCnt.toString()
            }
        }).start()

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.properties)
        }
    }

    private fun addExifProperties(path: String) {
        val exif = ExifInterface(path)
        exif.getAttribute(ExifInterface.TAG_DATETIME).let {
            if (it?.isNotEmpty() == true) {
                try {
                    val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH)
                    val dateTaken = simpleDateFormat.parse(it).time.formatLastModified()
                    addProperty(R.string.date_taken, dateTaken)
                } catch (ignored: Exception) {
                }
            }
        }

        exif.getAttribute(ExifInterface.TAG_MAKE).let {
            if (it?.isNotEmpty() == true) {
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                addProperty(R.string.camera, "$it $model")
            }
        }

        var exifString = ""
        exif.getAttribute(ExifInterface.TAG_F_NUMBER).let {
            if (it?.isNotEmpty() == true) {
                val number = it.trimEnd('0').trimEnd('.')
                exifString += "F/$number  "
            }
        }

        exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH).let {
            if (it?.isNotEmpty() == true) {
                val values = it.split('/')
                val focalLength = "${Math.round(values[0].toDouble() / values[1].toDouble())}mm"
                exifString += "$focalLength  "
            }
        }

        exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME).let {
            if (it?.isNotEmpty() == true) {
                val exposureSec = (1 / it.toFloat()).toInt()
                exifString += "1/${exposureSec}s  "
            }
        }

        exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS).let {
            if (it?.isNotEmpty() == true) {
                exifString += "ISO-$it"
            }
        }

        if (exifString.trim().isNotEmpty())
            addProperty(R.string.exif, exifString.trim())
    }

    private fun isSameParent(files: List<File>): Boolean {
        var parent = files[0].parent
        for (file in files) {
            val curParent = file.parent
            if (curParent != parent)
                return false

            parent = curParent
        }
        return true
    }

    private fun getDirectChildrenCount(file: File, countHiddenItems: Boolean): String {
        return if (file.listFiles() == null)
            "0"
        else
            file.listFiles().filter { it != null && (!it.isHidden || (it.isHidden && countHiddenItems)) }.size.toString()
    }

    private fun addProperty(labelId: Int, value: String?, viewId: Int = 0) {
        if (value == null)
            return

        mInflater.inflate(R.layout.property_item, mPropertyView, false).apply {
            property_label.text = mResources.getString(labelId)
            property_value.text = value
            mPropertyView.properties_holder.addView(this)

            if (viewId != 0)
                id = viewId
        }
    }

    private fun getItemsSize(files: ArrayList<File>): Long {
        var size = 0L
        files.forEach { size += getItemSize(it) }
        return size
    }

    private fun getItemSize(file: File): Long {
        if (file.isDirectory) {
            return getDirectorySize(File(file.path))
        }

        mFilesCnt++
        return file.length()
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    if (files[i].isDirectory) {
                        size += getDirectorySize(files[i])
                    } else if (!files[i].isHidden && !dir.isHidden || mCountHiddenItems) {
                        mFilesCnt++
                        size += files[i].length()
                    }
                }
            }
        }
        return size
    }
}

