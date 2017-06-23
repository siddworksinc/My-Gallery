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
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R





/**
 * Created by sidd on 3/6/17.
 */

fun showWhatsNewPopup(activity: Activity) {
    val whatsNewContent = getWhatsNewContent(BuildConfig.VERSION_CODE)
    if (whatsNewContent != null) {
        showAlertOneButton("", activity, "What's New", whatsNewContent)
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

fun getWhatsNewContent(versionCode: Int): String? {
    if (versionCode >= 31) {
        val retVal = " - Added Shortcuts feature. Create shortcuts to multiple folders\n\n"
        return retVal
    }
    else if (versionCode > 27) {
        val retVal = " - Brand New UI\n\n" +
                " - Added features like Share, delete, rename, rotate etc\n\n" +
                " - Added Copy/Move\n\n" +
                " - Added tip to show folders are being ignored in copy/move/share\n\n"
        return retVal
    }
    else if (versionCode > 25) {
        val retVal = " - Brand New UI\n\n" +
                " - Added functionalities like Share, delete, rename, rotate etc\n\n" +
                " - Added Whats new section in Settings. Never miss out in newly added features.\n\n" +
                " - More faster and more quick\n\n" +
                " - Added About Section\n\n" +
                " - New Icons\n\n" +
                " - Folders are also sorted as per sorting preferences"
        return retVal
    }
    return null
}

fun openUrl(activity: Activity, yattoUrl: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(yattoUrl))
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
