package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.MasterPassDialog
import com.simplemobiletools.gallery.extensions.alert
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.*
import kotlinx.android.synthetic.main.activity_settings_new.*

class SettingsNewActivity : SimpleActivity() {
    lateinit var res: Resources
    var accentColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_new)
        logEvent("ActivitySetingsNew")
        res = resources
    }


    override fun onResume() {
        super.onResume()

        accentColor = baseConfig.customPrimaryColor
        setupCustomizeColors()
        setupExcludedAlbums()
        setupCustomAlbumsFolders()
        setupAppLock()
        setupShowHiddenMedia()
        setupPassProtectedAlbums()
        setupAutoplayVideos()
        setupLoopVideos()
        setupAnimateGifs()
        setupMaxBrightness()
        setupCropThumbnails()
        setupDarkBackground()
        setupScreenRotation()
        setupShowMedia()
        setupMasterPass()
        setupPrivacyAndSecurity()
        updateTextColors(main_content)
    }

    private fun setupPassProtectedAlbums() {
        hide_pass_albums_image.setColorFilter(accentColor)
        hide_pass_albums_image.setImageResource(R.drawable.settings_pass_pro_album)

        hide_pass_albums.isChecked = config.passProtectedAlbumsHidden
        hide_pass_albums_root.setOnClickListener {
            hide_pass_albums.toggle()
            config.passProtectedAlbumsHidden = hide_pass_albums.isChecked

            if(hide_pass_albums.isChecked) {
                hide_pass_albums_hint.text = "Password locked albums are hidden"
            } else {
                hide_pass_albums_hint.text = "Password locked albums are not hidden"
            }
        }

        if(config.passProtectedAlbumsHidden) {
            hide_pass_albums_hint.text = "Password locked albums are hidden"
        } else {
            hide_pass_albums_hint.text = "Password locked albums are not hidden"
        }
    }

    private fun setupAppLock() {
        app_lock_image.setColorFilter(accentColor)
        app_lock_image.setImageResource(R.drawable.security_app_lock)

        app_lock.isChecked = config.appLocked != null
        app_lock_root.setOnClickListener {
            if(config.masterPass == null) {
                toast("Setup Master Password to enable App Lock")
            } else {
                app_lock.toggle()
                if(app_lock.isChecked) {
                    config.appLocked = config.masterPass
                    app_lock_hint.text = "Password required to open Gallery"
                } else {
                    app_lock_hint.text = "Password not required to open Gallery"
                    config.appLocked = null
                }
            }
        }

        if(config.appLocked != null) {
            app_lock_hint.text = "Password required to open Gallery"
        } else {
            app_lock_hint.text = "Password not required to open Gallery"
        }
    }

    private fun setupPrivacyAndSecurity() {
        password_done.setBackgroundColor(baseConfig.customPrimaryColor)
        password_pass.background?.mutate()?.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)

        if(config.masterPass == null) {
            master_pass_hint.text = "Master Password is Disabled"
            privacy_settings_password_root.visibility = View.GONE
            privacy_settings_root.visibility = View.VISIBLE
        } else {
            master_pass_hint.text = "Master Password is Enabled"
            privacy_settings_password_root.visibility = View.VISIBLE
            privacy_settings_root.visibility = View.GONE
            password_done.setOnClickListener {
                hideKeyboard()
                if(password_pass.text?.toString() == config.masterPass) {
                    privacy_settings_password_root.visibility = View.GONE
                    privacy_settings_root.visibility = View.VISIBLE
                } else {
                    password_warning.text = "Incorrect Password"
                }
            }
        }
    }

    private fun setupMasterPass() {
        master_pass_image.setColorFilter(accentColor)
        master_pass_image.setImageResource(R.drawable.security_password)
        master_pass_help.setColorFilter(accentColor)
        master_pass_help.setImageResource(R.drawable.settings_help)

        master_pass_parent.setOnClickListener {
            MasterPassDialog(this) {
                if(config.masterPass == null) {
                    master_pass_hint.text = "Master Password is Disabled"
                    app_lock.isChecked = false
                    app_lock_hint.text = "Password not required to open Gallery"
                } else {
                    master_pass_hint.text = "Master Password is Enabled"
                }
            }
        }

        master_pass_help.setOnClickListener {
            alert("Ok", "Setting up Master Password will require this password on:" +
                    "\n\n - Accessing and changing Privacy & Security Settings" +
                    "\n - Temporarily Showing Hidden Media (On Main Screen)" +
                    "\n\nSetting up master password will ensure that Hidden/Excluded Media cannot be seen without entering this password" +
                    "\n\nTo Change Master Password, disable it and set new password by enabling it again") {}
        }
    }

    private fun setupCustomizeColors() {
        // Look Feel Image
        customize_colors_image.setColorFilter(accentColor)
        customize_colors_image.setImageResource(R.drawable.settings_look_feel)

        customize_colors_parent.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomAlbumsFolders() {
        custom_albums_image.setColorFilter(accentColor)
        custom_albums_image.setImageResource(R.drawable.settings_custom_albums)
        custom_albums_help.setColorFilter(accentColor)
        custom_albums_help.setImageResource(R.drawable.settings_help)

        custom_albums_root.setOnClickListener {
            startActivity(Intent(this, IncludedFoldersActivity::class.java))
        }

        if(config.includedFolders.isEmpty()) {
            custom_albums_hint.text = "No Custom Albums"
        } else {
            custom_albums_hint.text = config.includedFolders.size.toString() + " Custom Album(s)"
        }

        custom_albums_help.setOnClickListener {
            alert("Ok", getString(R.string.included_activity_placeholder)) {}
        }
    }

    private fun setupExcludedAlbums() {
        excluded_albums_image.setColorFilter(accentColor)
        excluded_albums_image.setImageResource(R.drawable.settings_excluded_albums)
        excluded_albums_help.setColorFilter(accentColor)
        excluded_albums_help.setImageResource(R.drawable.settings_help)

        excluded_albums_parent.setOnClickListener {
            startActivity(Intent(this, ExcludedFoldersActivity::class.java))
        }

        if(config.excludedFolders.isEmpty()) {
            excluded_albums_hint.text = "No Excluded Albums"
        } else {
            excluded_albums_hint.text = config.excludedFolders.size.toString() + " Excluded Album(s)"
        }

        excluded_albums_help.setOnClickListener {
            alert("Ok", getString(R.string.excluded_activity_placeholder)) {}
        }
    }

    private fun setupShowHiddenMedia() {
        // hidden media Image
        show_hidden_media_image.setColorFilter(accentColor)
        show_hidden_media_image.setImageResource(R.drawable.ic_hide)

        show_hidden_media.isChecked = config.showHiddenMedia
        show_hidden_media_root.setOnClickListener {
            show_hidden_media.toggle()
            config.showHiddenMedia = show_hidden_media.isChecked

            if(show_hidden_media.isChecked) {
                show_hidden_media_hint.text = "Hidden Media is shown"
            } else {
                show_hidden_media_hint.text = "Hidden Media is not shown"
            }
        }

        if(show_hidden_media.isChecked) {
            show_hidden_media_hint.text = "Hidden Media is shown"
        } else {
            show_hidden_media_hint.text = "Hidden Media is not shown"
        }
    }

    private fun setupAutoplayVideos() {
        autoplay_videos_image.setColorFilter(accentColor)
        autoplay_videos_image.setImageResource(R.drawable.settings_autoplay_videos)

        autoplay_videos.isChecked = config.autoplayVideos
        autoplay_videos_root.setOnClickListener {
            autoplay_videos.toggle()
            config.autoplayVideos = autoplay_videos.isChecked
        }
    }

    private fun setupLoopVideos() {
        loop_videos_image.setColorFilter(accentColor)
        loop_videos_image.setImageResource(R.drawable.settings_loop_videos)

        loop_videos.isChecked = config.loopVideos
        loop_videos_root.setOnClickListener {
            loop_videos.toggle()
            config.loopVideos = loop_videos.isChecked
        }
    }

    private fun setupAnimateGifs() {
        animate_gifs_image.setColorFilter(accentColor)
        animate_gifs_image.setImageResource(R.drawable.settings_gif)

        animate_gifs.isChecked = config.animateGifs
        animate_gifs_root.setOnClickListener {
            animate_gifs.toggle()
            config.animateGifs = animate_gifs.isChecked
        }
    }

    private fun setupMaxBrightness() {
        max_brightness_image.setColorFilter(accentColor)
        max_brightness_image.setImageResource(R.drawable.settings_brightness)

        max_brightness.isChecked = config.maxBrightness
        max_brightness_root.setOnClickListener {
            max_brightness.toggle()
            config.maxBrightness = max_brightness.isChecked
        }
    }

    private fun setupCropThumbnails() {
        crop_thumbnails_image.setColorFilter(accentColor)
        crop_thumbnails_image.setImageResource(R.drawable.settings_crop_thumbnails)

        crop_thumbnails.isChecked = config.cropThumbnails
        crop_thumbnails_root.setOnClickListener {
            crop_thumbnails.toggle()
            config.cropThumbnails = crop_thumbnails.isChecked
        }
    }

    private fun setupDarkBackground() {
        dark_background_image.setColorFilter(accentColor)
        dark_background_image.setImageResource(R.drawable.settings_dark_background)

        dark_background.isChecked = config.darkBackground
        dark_background_root.setOnClickListener {
            dark_background.toggle()
            config.darkBackground = dark_background.isChecked
        }
    }

    private fun setupScreenRotation() {
        screen_rotation_image.setColorFilter(accentColor)
        screen_rotation_image.setImageResource(R.drawable.settings_rotate_media)

        screen_rotation_value.text = getScreenRotationText()
        screen_rotation_root.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(ROTATE_BY_SYSTEM_SETTING, res.getString(R.string.screen_rotation_system_setting)),
                    RadioItem(ROTATE_BY_DEVICE_ROTATION, res.getString(R.string.screen_rotation_device_rotation)),
                    RadioItem(ROTATE_BY_ASPECT_RATIO, res.getString(R.string.screen_rotation_aspect_ratio)))

            RadioGroupDialog(this@SettingsNewActivity, items, config.screenRotation) {
                config.screenRotation = it as Int
                screen_rotation_value.text = getScreenRotationText()
            }
        }
    }

    private fun getScreenRotationText() = getString(when (config.screenRotation) {
        ROTATE_BY_SYSTEM_SETTING -> R.string.screen_rotation_system_setting
        ROTATE_BY_DEVICE_ROTATION -> R.string.screen_rotation_device_rotation
        else -> R.string.screen_rotation_aspect_ratio
    })

    private fun setupShowMedia() {
        show_media_image.setColorFilter(accentColor)
        show_media_image.setImageResource(R.drawable.settings_media)

        show_media_value.text = getShowMediaText()
        show_media_root.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(IMAGES_AND_VIDEOS, res.getString(R.string.images_and_videos)),
                    RadioItem(IMAGES, res.getString(R.string.images)),
                    RadioItem(VIDEOS, res.getString(R.string.videos)))

            RadioGroupDialog(this@SettingsNewActivity, items, config.showMedia) {
                config.showMedia = it as Int
                show_media_value.text = getShowMediaText()
            }
        }
    }

    private fun getShowMediaText() = getString(when (config.showMedia) {
        IMAGES_AND_VIDEOS -> R.string.images_and_videos
        IMAGES -> R.string.images
        else -> R.string.videos
    })
}
