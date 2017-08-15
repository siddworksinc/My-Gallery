package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.view.ViewPager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.DisplayMetrics
import android.view.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.MediaActivity.Companion.mMedia
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.adapters.MyPagerAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.asynctasks.GetMediaByDirsAsyncTask
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.dialogs.SlideshowDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_medium.*
import java.io.File
import java.io.OutputStream
import java.util.*

class ViewPagerActivity : SimpleActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener, MediaAdapter.MediaOperationsListener {

    lateinit var mOrientationEventListener: OrientationEventListener
    private var mPath = ""
    private var mDirectory = ""

    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false
    private var mIsSlideshowActive = false
    private var mRotationDegrees = 0f
    private var mLastHandledOrientation = 0
    private var mPrevHashcode = 0
    private lateinit var pagerAdapter: MyPagerAdapter
    var smoothScroller: RecyclerView.SmoothScroller? = null
    private var isViewIntent: Boolean = false

    private var mSlideshowHandler = Handler()
    private var mSlideshowInterval = SLIDESHOW_DEFAULT_INTERVAL
    private var mSlideshowMoveBackwards = false
    private var mSlideshowMedia = mutableListOf<Medium>()
    private var mAreSlideShowMediaVisible = false

    companion object {
        var screenWidth = 0
        var screenHeight = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)
        logEvent("ActivityViewPager")

        if (!hasWriteStoragePermission()) {
            finish()
            return
        }

        measureScreen()
        val uri = intent.data
        if (uri != null) {
            var cursor: Cursor? = null
            try {
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                cursor = contentResolver.query(uri, proj, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    mPath = cursor.getStringValue(MediaStore.Images.Media.DATA)
                }
            } finally {
                cursor?.close()
            }
        } else {
            mPath = intent.getStringExtra(MEDIUM)
            mShowAll = config.showAll
        }

        if (mPath.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }

        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            isViewIntent = true
        }

        if (!config.hideSystemUI)
            showExtraUI()

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullScreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            view_pager.adapter?.let {
                (it as MyPagerAdapter).toggleFullscreen(mIsFullScreen)
                checkSystemUI()
            }
        }

        mDirectory = File(mPath).parent
        title = mPath.getFilenameFromPath()

        view_pager.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view_pager.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
                    return

                if (mMedia.isNotEmpty()) {
                    gotMedia(mMedia)
                }
            }
        })

        reloadViewPager()
        scanPath(mPath) {}
        setupOrientationEventListener()

        if (config.darkBackground) {
            view_pager.background = ColorDrawable(Color.BLACK)
        }

        // can be init later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val tDesc = ActivityManager.TaskDescription(null, null, config.primaryColor)
            setTaskDescription(tDesc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            config.temporarilyShowHidden = false
        }
    }

    private fun setupOrientationEventListener() {
        mOrientationEventListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                val currOrient = if (orientation in 45..134) {
                    ORIENT_LANDSCAPE_RIGHT
                } else if (orientation in 225..314) {
                    ORIENT_LANDSCAPE_LEFT
                } else {
                    ORIENT_PORTRAIT
                }

                if (mLastHandledOrientation != currOrient) {
                    mLastHandledOrientation = currOrient

                    if (currOrient == ORIENT_LANDSCAPE_LEFT) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else if (currOrient == ORIENT_LANDSCAPE_RIGHT) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    } else {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasWriteStoragePermission()) {
            finish()
        }
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.actionbar_gradient_background))

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION && mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable()
        } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        updateStatusBarColor(R.color.black)
    }

    override fun onPause() {
        super.onPause()
        mOrientationEventListener.disable()
        stopSlideshow()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_viewpager, menu)
        val currentMedium = getCurrentMedium() ?: return true

        menu.apply {
            findItem(R.id.menu_set_as).isVisible = currentMedium.isImage()
            findItem(R.id.menu_edit).isVisible = currentMedium.isImage()
            findItem(R.id.menu_rotate).isVisible = currentMedium.isImage()
            findItem(R.id.menu_save_as).isVisible = mRotationDegrees != 0f
            findItem(R.id.menu_hide).isVisible = !currentMedium.name.startsWith('.')
            findItem(R.id.menu_unhide).isVisible = currentMedium.name.startsWith('.')
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getCurrentMedium() == null)
            return true

        when (item.itemId) {
            R.id.menu_set_as -> trySetAs(getCurrentFile())
            R.id.slideshow -> initSlideshow()
            R.id.menu_copy_to -> copyMoveTo(true)
            R.id.menu_move_to -> copyMoveTo(false)
            R.id.menu_open_with -> openWith(getCurrentFile())
            R.id.menu_hide -> toggleFileVisibility(true)
            R.id.menu_unhide -> toggleFileVisibility(false)
            R.id.menu_share -> shareMedium(getCurrentMedium()!!)
            R.id.menu_delete -> askConfirmDelete()
            R.id.menu_rename -> renameFile()
            R.id.menu_edit -> openFileEditor(getCurrentFile())
            R.id.menu_properties -> showProperties()
            R.id.show_on_map -> showOnMap()
            R.id.menu_rotate -> rotateImage()
            R.id.menu_save_as -> saveImageAs()
            R.id.settings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initSlideshow() {
        SlideshowDialog(this) {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        if (getMediaForSlideshow()) {
            view_pager.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view_pager.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
                        return

                    hideSystemUI()
                    mSlideshowInterval = config.slideshowInterval
                    mSlideshowMoveBackwards = config.slideshowMoveBackwards
                    mIsSlideshowActive = true
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    scheduleSwipe()
                }
            })
        }
    }

    private fun stopSlideshow() {
        if (mIsSlideshowActive) {
            showSystemUI()
            mIsSlideshowActive = false
            mSlideshowHandler.removeCallbacksAndMessages(null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateScrollPositionRV(mPos)to
        }
    }

    private fun scheduleSwipe() {
        mSlideshowHandler.removeCallbacksAndMessages(null)
        if (mIsSlideshowActive) {
            if (getCurrentMedium()!!.isImage() || getCurrentMedium()!!.isGif()) {
                mSlideshowHandler.postDelayed({
                    if (mIsSlideshowActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !isDestroyed) {
                        swipeToNextMedium()
                    }
                }, mSlideshowInterval * 1000L)
            } else {
                (getCurrentFragment() as? VideoFragment)!!.playVideo()
            }
        }
    }

    private fun swipeToNextMedium() {
        val before = view_pager.currentItem
        view_pager.currentItem = if (mSlideshowMoveBackwards) --view_pager.currentItem else ++view_pager.currentItem
        if (before == view_pager.currentItem) {
            stopSlideshow()
            toast(R.string.slideshow_ended)
        }
    }

    private fun updatePagerItems(media: MutableList<Medium>) {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, media)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed) {
            view_pager.apply {
                adapter = pagerAdapter
                adapter!!.notifyDataSetChanged()
                currentItem = mPos
                addOnPageChangeListener(this@ViewPagerActivity)
            }
        }
    }

    private fun getMediaForSlideshow(): Boolean {
        mSlideshowMedia = mMedia.toMutableList()
        if (!config.slideshowIncludePhotos) {
            mSlideshowMedia = mSlideshowMedia.filter { !it.isImage() && !it.isGif() } as MutableList
        }

        if (!config.slideshowIncludeVideos) {
            mSlideshowMedia = mSlideshowMedia.filter { it.isImage() || it.isGif() } as MutableList
        }

        if (config.slideshowRandomOrder) {
            Collections.shuffle(mSlideshowMedia)
            mPos = 0
        } else {
            mPath = getCurrentPath()
            mPos = getPositionInList(mSlideshowMedia)
        }

        return if (mSlideshowMedia.isEmpty()) {
            toast(R.string.no_media_for_slideshow)
            false
        } else {
            updatePagerItems(mSlideshowMedia)
            mAreSlideShowMediaVisible = true
            true
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>(1).apply { add(getCurrentFile()) }
        tryCopyMoveFilesTo(files, isCopyOperation) {
            if (!isCopyOperation) {
                reloadViewPager()
            }
        }
    }

    private fun toggleFileVisibility(hide: Boolean) {
        toggleFileVisibility(getCurrentFile(), hide) {
            val newFileName = it.absolutePath.getFilenameFromPath()
            title = newFileName

            getCurrentMedium()!!.apply {
                name = newFileName
                path = it.absolutePath
                getCurrentMedia()[mPos] = this
            }
            invalidateOptionsMenu()
        }
    }

    private fun rotateImage() {
        val currentMedium = getCurrentMedium() ?: return
        if (currentMedium.isJpg() && !isPathOnSD(currentMedium.path)) {
            rotateByExif()
        } else {
            rotateByDegrees()
        }
    }

    private fun rotateByExif() {
        val exif = ExifInterface(getCurrentPath())
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val newRotation = getNewRotation(rotation)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, newRotation)
        exif.saveAttributes()
        File(getCurrentPath()).setLastModified(System.currentTimeMillis())
        (getCurrentFragment() as? PhotoFragment)?.refreshBitmap()
    }

    private fun getNewRotation(rotation: Int): String {
        return when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_ROTATE_180
            ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_270
            ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_NORMAL
            else -> ExifInterface.ORIENTATION_ROTATE_90
        }.toString()
    }

    private fun rotateByDegrees() {
        mRotationDegrees = (mRotationDegrees + 90) % 360
        getCurrentFragment()?.let {
            (it as? PhotoFragment)?.rotateImageViewBy(mRotationDegrees)
        }
        supportInvalidateOptionsMenu()
    }

    private fun saveImageAs() {
        val currPath = getCurrentPath()
        SaveAsDialog(this, currPath) {
            Thread({
                toast(R.string.saving)
                val selectedFile = File(it)
                val tmpFile = File(selectedFile.parent, "tmp_${it.getFilenameFromPath()}")
                try {
                    val bitmap = BitmapFactory.decodeFile(currPath)
                    getFileOutputStream(tmpFile) {
                        saveFile(tmpFile, bitmap, it)
                        if (needsStupidWritePermissions(selectedFile.absolutePath)) {
                            deleteFile(selectedFile) {}
                        }

                        renameFile(tmpFile, selectedFile) {
                            deleteFile(tmpFile) {}
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    toast(R.string.out_of_memory_error)
                    deleteFile(tmpFile) {}
                } catch (e: Exception) {
                    toast(R.string.unknown_error_occurred)
                    deleteFile(tmpFile) {}
                }
            }).start()
        }
    }

    private fun saveFile(file: File, bitmap: Bitmap, out: OutputStream) {
        val matrix = Matrix()
        matrix.postRotate(mRotationDegrees)
        val bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bmp.compress(file.getCompressionFormat(), 90, out)
        out.flush()
        toast(R.string.file_saved)
        out.close()
    }

    private fun getCurrentFragment() = (view_pager.adapter as MyPagerAdapter).getCurrentFragment(view_pager.currentItem)

    private fun showProperties() {
        if (getCurrentMedium() != null)
            PropertiesDialog(this, getCurrentPath(), false)
    }

    private fun showOnMap() {
        val exif = ExifInterface(getCurrentPath())
        val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val lat_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val lon_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

        if (lat == null || lat_ref == null || lon == null || lon_ref == null) {
            toast(R.string.unknown_location)
        } else {
            val geoLat = if (lat_ref == "N") {
                convertToDegree(lat)
            } else {
                0 - convertToDegree(lat)
            }

            val geoLon = if (lon_ref == "E") {
                convertToDegree(lon)
            } else {
                0 - convertToDegree(lon)
            }

            val uriBegin = "geo:$geoLat,$geoLon"
            val query = "$geoLat, $geoLon"
            val encodedQuery = Uri.encode(query)
            val uriString = "$uriBegin?q=$encodedQuery&z=16"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            val packageManager = packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                toast(R.string.no_map_application)
            }
        }
    }

    private fun convertToDegree(stringDMS: String): Float {
        val dms = stringDMS.split(",".toRegex(), 3).toTypedArray()

        val stringD = dms[0].split("/".toRegex(), 2).toTypedArray()
        val d0 = stringD[0].toDouble()
        val d1 = stringD[1].toDouble()
        val floatD = d0 / d1

        val stringM = dms[1].split("/".toRegex(), 2).toTypedArray()
        val m0 = stringM[0].toDouble()
        val m1 = stringM[1].toDouble()
        val floatM = m0 / m1

        val stringS = dms[2].split("/".toRegex(), 2).toTypedArray()
        val s0 = stringS[0].toDouble()
        val s1 = stringS[1].toDouble()
        val floatS = s0 / s1

        return (floatD + floatM / 60 + floatS / 3600).toFloat()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mPos = -1
                reloadViewPager()
            }
        } else if (requestCode == REQUEST_SET_AS) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.wallpaper_set_successfully)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(this) {
            deleteFileBg(File(getCurrentMedia()[mPos].path)) {
                reloadViewPager()
            }
        }
    }

    private fun isDirEmpty(media: ArrayList<Medium>): Boolean {
        return if (media.isEmpty()) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else
            false
    }

    private fun renameFile() {
        RenameItemDialog(this, getCurrentPath()) {
            getCurrentMedia()[mPos].path = it
            updateActionbarTitle()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        measureScreen()
    }

    private fun measureScreen() {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        } else {
            windowManager.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    private fun reloadViewPager() {
        if(!mShowAll) {
            GetMediaAsynctask(applicationContext, mDirectory, false, false, false, isViewIntent && true) {
                gotMedia(it)
            }.execute()
        } else {
            val token = object : TypeToken<List<Directory>>() {}.type
            val dirs = Gson().fromJson<java.util.ArrayList<Directory>>(config.showAllDirectories, token) ?: java.util.ArrayList<Directory>(1)

            GetMediaByDirsAsyncTask(applicationContext, mPath, false, false, true, dirs) {
                gotMedia(it)
            }.execute()
        }
    }

    private fun gotMedia(media: ArrayList<Medium>) {
        if (isDirEmpty(media) || media.hashCode() == mPrevHashcode) {
            return
        }

        mPrevHashcode = media.hashCode()
        mMedia = media
        if (mPos == -1) {
            mPos = getPositionInList(media)
        } else {
            mPos = Math.min(mPos, mMedia.size - 1)
        }

        updatePagerItems(mMedia.toMutableList())
        setupRVAdapter()
        updateActionbarTitle()
        invalidateOptionsMenu()
        checkOrientation()
    }

    private fun getPositionInList(items: MutableList<Medium>): Int {
        mPos = 0
        var i = 0
        for (medium in items) {
            if (medium.path == mPath) {
                return i
            }
            i++
        }
        return mPos
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mDirectory)
        if (!file.isDownloadsFolder() && file.isDirectory && file.listFiles()?.isEmpty() == true) {
            file.delete()
        }

        scanPath(mDirectory) {}
    }

    private fun checkOrientation() {
        if (config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            val res = getCurrentFile().getResolution()
            if (res.x > res.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (res.x < res.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        checkSystemUI()
    }

    override fun videoEnded(): Boolean {
        if (mIsSlideshowActive)
            swipeToNextMedium()
        return mIsSlideshowActive
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideExtraUI()
        } else {
            stopSlideshow()
            showExtraUI()
        }
    }

    private fun showExtraUI() {
        showSystemUI()
        showRV()
    }

    private fun hideExtraUI() {
        hideSystemUI()
        hideRV()
    }

    private fun showRV() {
        media_grid.beVisible()
    }

    private fun hideRV() {
        media_grid.beGone()
    }

    private fun updateActionbarTitle() {
        runOnUiThread {
            if (mPos < getCurrentMedia().size) {
                title = getCurrentMedia()[mPos].path.getFilenameFromPath()
                supportActionBar?.subtitle = Html.fromHtml("<small>${mPos+1}/${mMedia.size}</small>")
                updateScrollPositionRV(mPos)
            }
        }
    }

    private fun getCurrentMedium(): Medium? {
        return if (getCurrentMedia().isEmpty() || mPos == -1)
            null
        else
            getCurrentMedia()[Math.min(mPos, getCurrentMedia().size - 1)]
    }

    private fun getCurrentMedia() = if (mAreSlideShowMediaVisible) mSlideshowMedia else mMedia

    private fun getCurrentPath() = getCurrentMedium()!!.path

    private fun getCurrentFile() = File(getCurrentPath())

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        if (view_pager.offscreenPageLimit == 1) {
            view_pager.offscreenPageLimit = 2
        }
        mPos = position
        updateActionbarTitle()
        mRotationDegrees = 0f
        supportInvalidateOptionsMenu()
        scheduleSwipe()
    }

    private fun updateScrollPositionRV(position: Int) {
        if(!mIsFullScreen) {
            if(smoothScroller == null) {
                smoothScroller = object : LinearSmoothScroller(this@ViewPagerActivity) {
                    override fun getVerticalSnapPreference(): Int {
                        return LinearSmoothScroller.SNAP_TO_START
                    }
                }
            }

            smoothScroller?.targetPosition = position
            media_grid.layoutManager.startSmoothScroll(smoothScroller)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            checkOrientation()
        }
    }

    private fun setupRVAdapter() {
        val currAdapter = media_grid.adapter
        if (currAdapter == null) {
            val mediaAdapter = MediaAdapter(this, mMedia, this, true) {
                itemClicked(it.path)
            }
            mediaAdapter.scrollVertically = false
            mediaAdapter.notifyDataSetChanged()
            media_grid.adapter = mediaAdapter

            val layoutManager = media_grid.layoutManager as GridLayoutManager
            layoutManager.orientation = GridLayoutManager.HORIZONTAL
            media_grid.layoutManager.scrollToPosition(mPos)
        } else {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
        }
    }

    private fun itemClicked(path: String) {
        mPath = path
        mPos = getPositionInList(mMedia)

        updateActionbarTitle()
        view_pager.currentItem = mPos
    }

    override fun refreshItems() {
        reloadViewPager()
    }

    override fun deleteFiles(files: ArrayList<File>) {
    }

    override fun itemLongClicked(position: Int) {
    }
}
