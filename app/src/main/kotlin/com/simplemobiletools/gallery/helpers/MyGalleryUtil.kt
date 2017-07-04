package com.simplemobiletools.gallery.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.siddworks.android.mygallery.ShortcutsActivity
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.Release


/**
 * Created by sidd on 3/6/17.
 */

fun showWhatsNewPopup(activity: Activity) {
    val whatsNewContent = getWhatsNewContent(BuildConfig.VERSION_CODE)
    if (whatsNewContent != null) {
        showAlertOneButton("", activity, "What's New", " * $whatsNewContent")
    }
}

@UiThread
fun showAlertOneButton(requestTag: String, mContext: Context?, heading: String, message: String) {
    //		System.out.println("showAlertOneButton heading:"+heading+", message:"+message);
    if (mContext != null && !(mContext as Activity).isFinishing) {
        MaterialDialog.Builder(mContext)
                .title(heading)
                .content(message)
                .positiveText("Close")
                .positiveColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                .show()
    }
}

fun showContactDeveloper(activity: Activity) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "message/rfc822"
    intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on Folder Gallery")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("siddworks.inc+folgal@gmail.com"))
    var emailContent = "Your Feedback:\n"

    intent.putExtra(Intent.EXTRA_TEXT, emailContent)
    activity.startActivity(Intent.createChooser(intent, "Send Email"))
}

fun openBetaFeedback(activity: Activity) {
    var whatsNewContent = getWhatsNewContent(BuildConfig.VERSION_CODE)
    if (whatsNewContent != null) {
        whatsNewContent = whatsNewContent!!.replace("\n\n".toRegex(), "\nFeedback:\n\n\n")

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("siddworks.inc+folgal@gmail.com"))
        intent.putExtra(Intent.EXTRA_TEXT, whatsNewContent)
        activity.startActivity(Intent.createChooser(intent, "Send Email"))
    }
}

fun openUrl(activity: Activity, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        showAlertOneButton("", activity, "No Browser Installed", "Browser is required to open this URL. Please install browser app and try again")
    }
}

fun shareApp(activity: Activity) {
    val intent = Intent();
    intent.setAction(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, "I found this awesome Gallery App. Check it out " +
            "https://play.google.com/store/apps/details?id=com.siddworks.mygallery" );
    intent.setType("text/plain");
    try {
        activity.startActivity(Intent.createChooser(intent, "Sharing Helper For Tap Titans:"));
    } catch (anfe: ActivityNotFoundException) {
        logException(anfe)
    }
}

fun dpToPx(dp: Int): Int {
    return ((dp * Resources.getSystem().getDisplayMetrics().density).toInt());
}

fun pxToDp(px: Int): Int {
    return ((px / Resources.getSystem().getDisplayMetrics().density).toInt());
}

fun checkWhatsNew(activity: ShortcutsActivity): ArrayList<Release>? {
    if (activity.baseConfig.appRunCount == 0) {
        activity.baseConfig.lastVersion = BuildConfig.VERSION_CODE
        return null
    }

    val releases = arrayListOf<Release>()
    val newReleases = arrayListOf<Release>()
    for (i in BuildConfig.VERSION_CODE downTo 1) {
        val whatsNewContent = getWhatsNewContent(i)
        if(whatsNewContent != null) {
            releases.add(Release(i, whatsNewContent))
        }
    }
    releases.filterTo(newReleases) { it.id > activity.baseConfig.lastVersion }

    if (newReleases.isNotEmpty()) {
        return newReleases
    }

    return null
}

fun showWhatsNewDialog(activity: ShortcutsActivity, releases: ArrayList<Release>) {
    val newReleases = getNewReleases(releases)
    showAlertOneButton("", activity, "What's New", newReleases)
    activity.baseConfig.lastVersion = BuildConfig.VERSION_CODE
}

fun getNewReleases(releases: ArrayList<Release>): String {
    val sb = StringBuilder()

    releases.forEach {
        sb.append(" * ${it.text}\n\n")
    }

    return sb.toString()
}

fun getWhatsNewContent(versionCode: Int): String? {
    when (versionCode) {
        2 -> return  "New: Hide password protected folders on startup\n\n" +
                " - New: Show Everything as Album (Thanks @cuteriz22)\n\n" +
                " - New: \"Whats New\" will show in drawer after update\n\n" +
                " - BugFix: Exclude Albums from Android/Data folder (Thanks @sagarraythatha)\n\n" +
                " - BugFix: Change all password fields to password type (Thanks @3sanket3)\n\n" +
                " - BugFix: Change Actionbar color to black while selecting things.(For now) Pink looked out of place. (Thanks @3sanket3, @strngrINknowns)\n\n" +
                " - Bug Fixes & Performance Improvements\n\n"
        1 -> return "First Release"
    }
    return null
}
