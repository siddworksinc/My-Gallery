package com.siddworks.android.mygallery

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.helpers.*


class AboutActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        logEvent("ActivityAbout")

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val accentColor = baseConfig.customPrimaryColor

        // Author Heart Image
        val authorHeartImage = findViewById(R.id.about_author_image) as ImageView
        authorHeartImage.setColorFilter(ContextCompat.getColor(this, R.color.redbody))
        authorHeartImage.setImageResource(R.drawable.dr_heart_white)

        // Twitter row
        val authorTwitter = findViewById(R.id.about_author_root_twitter) as LinearLayout
        authorTwitter.setOnClickListener {
            logEvent("ButtonAboutAuthorTwitter")
            openUrl(this@AboutActivity, "https://twitter.com/sagarsiddhpura")
        }

        // Reddit row
        val authorReddit = findViewById(R.id.about_author_root_reddit) as LinearLayout
        authorReddit.setOnClickListener {
            logEvent("ButtonAboutAuthorReddit")
            openUrl(this@AboutActivity, "https://www.reddit.com/u/sagarsiddhpura/")
        }

        // App version textview
        val appVersion = findViewById(R.id.about_app_version) as TextView
        appVersion.setText("v"+ BuildConfig.VERSION_NAME)

        // App play store
        val appPlayStore = findViewById(R.id.about_app_root_playstore) as LinearLayout
        appPlayStore.setOnClickListener {
            logEvent("ButtonAboutAppPlayStore")
            openUrl(this@AboutActivity, "https://play.google.com/store/apps/details?id=com.siddworks.mygallery")
        }

        // App WhatsNew
        val appWhatsNew = findViewById(R.id.about_app_root_whatsnew) as LinearLayout
        appWhatsNew.setOnClickListener {
            logEvent("ButtonAboutAppWhatsNew")
            showWhatsNewPopup(this@AboutActivity)
        }

        // App PS Image
        val appPSImage = findViewById(R.id.about_app_ps) as ImageView
        appPSImage.setColorFilter(accentColor)
        appPSImage.setImageResource(R.drawable.ic_about_store)

        // App whatsnew Image
        val appWhatsNewImage = findViewById(R.id.about_app_whatsnew) as ImageView
        appWhatsNewImage.setColorFilter(accentColor)
        appWhatsNewImage.setImageResource(R.drawable.ic_about_new)

        // App curr ver Image
        val appCurrVerImage = findViewById(R.id.about_app_current_ver_image) as ImageView
        appCurrVerImage.setColorFilter(accentColor)
        appCurrVerImage.setImageResource(R.drawable.ic_about_current_ver)

        // Feedback

        // Feedback Connect
        val feedbackConnectRoot = findViewById(R.id.about_feedback_connect_root) as LinearLayout
        feedbackConnectRoot.setOnClickListener {
            logEvent("ButtonAboutFeedbackConnectWithDeveloper")
            showContactDeveloper(this@AboutActivity)
        }

        // Feedback Update
        val feedbackUpdateRoot = findViewById(R.id.about_feedback_update_root) as LinearLayout
        feedbackUpdateRoot.setOnClickListener {
            logEvent("ButtonAboutFeedbackLatestUpdate")
            openBetaFeedback(this@AboutActivity)
        }

//        // Feedback Upcoming
//        LinearLayout feedbackUpcomingRoot = (LinearLayout) findViewById(R.id.about_feedback_upcoming_root);
//        feedbackUpcomingRoot.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                logEvent("ButtonAboutFeedbackUpcomingFeatures");
//                sendGoogleFormsfeedback(AboutActivity.this);
//            }
//        });

        // sendGoogleFormsfeedback connect Image
        val feedbackConnect = findViewById(R.id.about_feedback_connect_image) as ImageView
        feedbackConnect.setColorFilter(accentColor)
        feedbackConnect.setImageResource(R.drawable.ic_about_mail)

        // sendGoogleFormsfeedback update Image
        val feedbackUpdate = findViewById(R.id.about_feedback_update_image) as ImageView
        feedbackUpdate.setColorFilter(accentColor)
        feedbackUpdate.setImageResource(R.drawable.ic_about_latest)

        // sendGoogleFormsfeedback upcoming Image
        val feedbackUpcoming = findViewById(R.id.about_feedback_upcoming_image) as ImageView
        feedbackUpcoming.setColorFilter(accentColor)
        feedbackUpcoming.setImageResource(R.drawable.ic_about_upcoming)

        // Credits Upcoming
        val creditsRedditRoot = findViewById(R.id.about_credits_simple_gallery_root) as LinearLayout
        creditsRedditRoot.setOnClickListener {
            logEvent("ButtonAboutCreditsSimpleGallery")
            openUrl(this@AboutActivity, "https://simplemobiletools.github.io/")
        }
    }
}
