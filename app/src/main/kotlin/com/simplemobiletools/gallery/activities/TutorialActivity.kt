package com.simplemobiletools.gallery.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.logEvent
import com.simplemobiletools.gallery.models.Directory
import java.util.*




class TutorialActivity : IntroActivity() {
    private val STORAGE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        isFullscreen = true
        super.onCreate(savedInstanceState)
        logEvent("ActivityTutorial")
        getDirectories()

        isButtonBackVisible = false;

        addSlide(FragmentSlide.Builder()
                .background(R.color.intro_bg)
                .fragment(R.layout.fragment_intro_1)
                .build())
        addSlide(FragmentSlide.Builder()
                .background(R.color.intro_bg)
                .fragment(R.layout.fragment_intro_2)
                .build())
        addSlide(FragmentSlide.Builder()
                .background(R.color.intro_bg)
                .fragment(R.layout.fragment_intro_3)
                .build())
        addSlide(FragmentSlide.Builder()
                .background(R.color.intro_bg)
                .fragment(R.layout.fragment_intro_4)
                .build())

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)

        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if(position == 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    }
                }
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories()
            } else {
                MaterialDialog.Builder(this@TutorialActivity)
                        .title("Permission Required")
                        .contentColor(R.color.black)
                        .titleColor(R.color.black)
                        .content("My Gallery is a Gallery app. To show Photos & Videos, " +
                                "it needs access to your device's storage. " +
                                "(${getString(R.string.no_storage_permissions)})")
                        .positiveText("Grant")
                        .onPositive { _, _ ->
                            run {
                                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
                            }
                        }
                        .negativeText("Close")
                        .positiveColor(ContextCompat.getColor(this@TutorialActivity, R.color.colorPrimary))
                        .show()
            }
        }
    }

    private fun getDirectories() {
        val mCurrAsyncTask = GetDirectoriesAsynctask(applicationContext, false, false) {
            gotDirectories(it)
        }
        mCurrAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun gotDirectories(dirs: ArrayList<Directory>) {
        val directories = Gson().toJson(dirs)
        config.directories = directories
    }

}
