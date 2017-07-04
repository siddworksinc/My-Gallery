package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaByDirsAsyncTask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import com.simplemobiletools.gallery.views.MyScalableRecyclerView
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException

class ShowAllMediaActivity : SimpleActivity(), MediaAdapter.MediaOperationsListener {
    private val TAG = MediaActivity::class.java.simpleName
    private val SAVE_MEDIA_CNT = 40
    private val LAST_MEDIA_CHECK_PERIOD = 3000L

    private var mPath = ""
    private var mIsGetImageIntent = false
    private var mIsGetVideoIntent = false
    private var mIsGetAnyIntent = false
    private var mIsGettingMedia = false
    private var mShowAll = false
    private var mLoadedInitialPhotos = false
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mLastDrawnHashCode = 0
    private var mLastMediaModified = 0
    private var mLastMediaHandler = Handler()

    companion object {
        var mMedia = ArrayList<Medium>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        logEvent("ActivityShowAll")
        intent.apply {
            mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(GET_ANY_INTENT, false)
        }

        media_refresh_layout.setOnRefreshListener({ getMedia() })
        mPath = intent.getStringExtra(DIRECTORY)
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        mShowAll = true
        config.showAll = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        if (mShowAll && mStoredAnimateGifs != config.animateGifs) {
            mMedia.clear()
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            mMedia.clear()
        }
        tryloadGallery()
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        mIsGettingMedia = false
        media_refresh_layout.isRefreshing = false
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        MyScalableRecyclerView.mListener = null
        mLastMediaHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMedia.clear()
        config.showAll = false
    }

    private fun tryloadGallery() {
        if (hasWriteStoragePermission()) {
            val dirName = getHumanizedFilename(mPath)
            title = if (mShowAll) resources.getString(R.string.all_folders) else dirName
            getMedia()
            handleZooming()
            checkIfColorChanged()
        } else {
            finish()
        }
    }

    private fun checkIfColorChanged() {
        if (media_grid.adapter != null && getRecyclerAdapter().foregroundColor != config.primaryColor) {
            getRecyclerAdapter().updatePrimaryColor(config.primaryColor)
            media_fastscroller.updateHandleColor()
        }
    }

    private fun setupAdapter() {
        if (isDirEmpty())
            return

        val adapter = MediaAdapter(this, mMedia, this) {
            itemClicked(it.path)
        }

        val currAdapter = media_grid.adapter
        if (currAdapter != null) {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
        } else {
            media_grid.adapter = adapter
        }
        media_fastscroller.setViews(media_grid, media_refresh_layout)
    }

    private fun checkLastMediaChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
            return

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            Thread({
                val lastModified = getLastMediaModified()
                if (mLastMediaModified != lastModified) {
                    mLastMediaModified = lastModified
                    runOnUiThread {
                        getMedia()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }).start()
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.temporarily_show_hidden)?.isVisible = !config.temporarilyShowHidden && !config.showHiddenMedia
        menu?.findItem(R.id.hide_hidden)?.isVisible = config.temporarilyShowHidden && !config.showHiddenMedia
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_media, menu)

        val isFolderHidden = File(mPath).containsNoMedia()
        menu.apply {
            findItem(R.id.hide_folder).isVisible = !isFolderHidden && !mShowAll
            findItem(R.id.unhide_folder).isVisible = isFolderHidden && !mShowAll
            findItem(R.id.exclude_folder).isVisible = !mShowAll

            findItem(R.id.temporarily_show_hidden).isVisible = !config.showHiddenMedia

            findItem(R.id.increase_column_count).isVisible = config.mediaColumnCnt < 10
            findItem(R.id.reduce_column_count).isVisible = config.mediaColumnCnt > 1
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.hide_folder -> tryHideFolder()
            R.id.unhide_folder -> unhideFolder()
            R.id.exclude_folder -> tryExcludeFolder()
            R.id.temporarily_show_hidden -> temporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.hide_hidden -> hideHidden()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun hideHidden() {
        config.temporarilyShowHidden = false
        getMedia()
    }

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        if (media_grid.adapter != null)
            getRecyclerAdapter().updateDisplayFilenames(config.displayFileNames)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, false, !config.showAll, mPath) {
            getMedia()
        }
    }

    private fun tryHideFolder() {
        if (config.wasHideFolderTooltipShown) {
            hideFolder()
        } else {
            ConfirmationDialog(this, getString(R.string.hide_folder_description)) {
                config.wasHideFolderTooltipShown = true
                hideFolder()
            }
        }
    }

    private fun hideFolder() {
        addNoMedia(mPath) {
            runOnUiThread {
                if (!config.shouldShowHidden)
                    finish()
                else
                    invalidateOptionsMenu()
            }
        }
    }

    private fun unhideFolder() {
        removeNoMedia(mPath) {
            runOnUiThread {
                invalidateOptionsMenu()
            }
        }
    }

    private fun tryExcludeFolder() {
        val excludeText = getString(R.string.exclude_folder_description) + "\n\n" + arrayListOf(mPath).reduce { acc, s -> acc + "\n" + s }
        val view  = MyTextView(this)
        view.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        view.text = excludeText
        AlertDialog.Builder(this)
                .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                    run {
                        config.addExcludedFolders(setOf(mPath))
                        finish()
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().apply {
            setupDialogStuff(view, this, R.string.additional_info)
        }
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mPath)
        if (!file.isDownloadsFolder() && file.isDirectory && file.listFiles()?.isEmpty() == true) {
            file.delete()
        }
    }

    private fun getMedia() {
        if (mIsGettingMedia)
            return

        mIsGettingMedia = true
        media_refresh_layout.isRefreshing = true

        val tokenMedia = object : TypeToken<List<Medium>>() {}.type
        val media = Gson().fromJson<ArrayList<Medium>>(config.loadFolderMedia(mPath), tokenMedia) ?: ArrayList<Medium>(1)
        if (media.isNotEmpty() && !mLoadedInitialPhotos) {
            gotMedia(media)
        }

        val token = object : TypeToken<List<Directory>>() {}.type
        val dirs = Gson().fromJson<java.util.ArrayList<Directory>>(config.showAllDirectories, token) ?: java.util.ArrayList<Directory>(1)

        mLoadedInitialPhotos = true

        GetMediaByDirsAsyncTask(applicationContext, mPath, mIsGetVideoIntent, mIsGetImageIntent, true, dirs) {
            gotMedia(it)
        }.execute()
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else
            false
    }

    private fun temporarilyShowHidden() {
        config.temporarilyShowHidden = true
        getMedia()
    }

    private fun getRecyclerAdapter() = (media_grid.adapter as MediaAdapter)

    private fun handleZooming() {
        val layoutManager = media_grid.layoutManager as GridLayoutManager
        layoutManager.spanCount = config.mediaColumnCnt
        MyScalableRecyclerView.mListener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < 10) {
                    increaseColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun selectItem(position: Int) {
                getRecyclerAdapter().selectItem(position)
            }

            override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                getRecyclerAdapter().selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)
            }
        }
    }

    private fun increaseColumnCount() {
        config.mediaColumnCnt = ++(media_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
    }

    private fun reduceColumnCount() {
        config.mediaColumnCnt = --(media_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(SET_WALLPAPER_INTENT, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mMedia.clear()
                refreshItems()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun itemClicked(path: String) {
        if (isSetWallpaperIntent()) {
            toast(R.string.setting_wallpaper)

            val wantedWidth = wallpaperDesiredMinimumWidth
            val wantedHeight = wallpaperDesiredMinimumHeight
            val ratio = wantedWidth.toFloat() / wantedHeight
            Glide.with(this)
                    .load(File(path))
                    .asBitmap()
                    .override((wantedWidth * ratio).toInt(), wantedHeight)
                    .fitCenter()
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(bitmap: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                            try {
                                WallpaperManager.getInstance(applicationContext).setBitmap(bitmap)
                                setResult(Activity.RESULT_OK)
                            } catch (e: IOException) {
                                Log.e(TAG, "item click $e")
                            }

                            finish()
                        }
                    })
        } else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
            Intent().apply {
                data = Uri.parse(path)
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        } else {
            val file = File(path)
            val isVideo = file.isVideoFast()
            if (isVideo) {
                openWith(file, false)
            } else {
                Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(MEDIUM, path)
                    putExtra(SHOW_ALL, true)
                    startActivity(this)
                }
            }
        }
    }

    private fun gotMedia(media: ArrayList<Medium>) {
        supportActionBar?.subtitle = Html.fromHtml("<small>${media.size} Photos & Videos</small>")
        mLastMediaModified = getLastMediaModified()
        mIsGettingMedia = false
        media_refresh_layout.isRefreshing = false

        checkLastMediaChanged()
        if (mLastDrawnHashCode == 0)
            mLastDrawnHashCode = media.hashCode()

        if (media.hashCode() == mMedia.hashCode() && media.hashCode() == mLastDrawnHashCode)
            return

        mLastDrawnHashCode = media.hashCode()
        mMedia = media
        setupAdapter()
        storeFolder()
    }

    private fun storeFolder() {
        if (!config.temporarilyShowHidden) {
            val subList = mMedia.subList(0, Math.min(SAVE_MEDIA_CNT, mMedia.size))
            val json = Gson().toJson(subList)
            config.saveFolderMedia(mPath, json)
        }
    }

    override fun deleteFiles(files: ArrayList<File>) {
        val filtered = files.filter { it.isImageVideoGif() } as ArrayList
        deleteFiles(filtered) {
            if (!it) {
                toast(R.string.unknown_error_occurred)
            } else if (mMedia.isEmpty()) {
                finish()
            }
        }
    }

    override fun refreshItems() {
        getMedia()
        Handler().postDelayed({
            getMedia()
        }, 1000)
    }

    override fun itemLongClicked(position: Int) {
        media_grid.setDragSelectActive(position)
    }
}
