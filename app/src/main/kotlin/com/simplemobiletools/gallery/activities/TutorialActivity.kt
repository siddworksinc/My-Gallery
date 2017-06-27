package com.simplemobiletools.gallery.activities

import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.view.ViewPager
import com.google.gson.Gson
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.simplemobiletools.commons.extensions.toast
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

        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun gotDirectories(dirs: ArrayList<Directory>) {
        val directories = Gson().toJson(dirs)
        config.directories = directories
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun getDirectories() {
        val mCurrAsyncTask = GetDirectoriesAsynctask(applicationContext, false, false) {
            gotDirectories(it)
        }
        mCurrAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
