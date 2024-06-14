package com.remotetechs.kun510.ui.activity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hbisoft.hbrecorder.Constants
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderCodecInfo
import com.hbisoft.hbrecorder.HBRecorderListener
import com.remotetechs.kun510.R
import com.remotetechs.kun510.base.BaseActivity
import com.remotetechs.kun510.databinding.ActivityMainBinding
import com.remotetechs.kun510.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class MainActivity : BaseActivity(), HBRecorderListener {
    private lateinit var binding: ActivityMainBinding
    private var hasPermissions = false
    private var hbRecorder: HBRecorder? = null
    private var wasHDSelected: Boolean = true
    private var isAudioEnabled: Boolean = true
    private var resolver: ContentResolver? = null
    private var contentValues: ContentValues? = null
    private var mUri: Uri? = null
    override fun setUpBinding() {
        super.setUpBinding()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setupListen() {
        super.setupListen()
        setOnClickListeners()
        setRadioGroupCheckListener()
        setRecordAudioCheckBoxListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hbRecorder = HBRecorder(this, this)
            if (hbRecorder!!.isBusyRecording()) {
                binding.buttonStart.setText(R.string.stop_recording)
            }
        }
        val hbRecorderCodecInfo = HBRecorderCodecInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mWidth = hbRecorder!!.defaultWidth
            val mHeight = hbRecorder!!.defaultHeight
            val mMimeType = "video/avc"
            val mFPS = 30
            if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {
                val defaultVideoEncoder = hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType)
                val isSizeAndFramerSupported = hbRecorderCodecInfo.isSizeAndFramerateSupported(
                    mWidth,
                    mHeight,
                    mFPS,
                    mMimeType,
                    Configuration.ORIENTATION_PORTRAIT
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "Default Video Encoder: $defaultVideoEncoder, isSizeAndFramerSupported: $isSizeAndFramerSupported"
                )
            } else {
                Log.e("HBRecorderCodecInfo", "MimeType not supported")
            }
        }
    }

    private fun setOnClickListeners() {
        binding.buttonStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (checkAndRequestPermissions()) {
                    toggleRecording()
                }
            } else {
                showLongToast("This library requires API 21>")
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(
                POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(POST_NOTIFICATIONS)
        }

        if (checkSelfPermission(RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && checkSelfPermission(
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(WRITE_EXTERNAL_STORAGE)
        }

        return if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), Constant.PERMISSION_REQ_ID_RECORD_AUDIO)
            false
        } else {
            true
        }
    }

    private fun toggleRecording() {
        if (hbRecorder!!.isBusyRecording()) {
            hbRecorder!!.stopScreenRecording()
            binding.buttonStart.setText(R.string.start_recording)
        } else {
            startRecordingScreen()
        }
    }

    private fun createFolder() {
        val f1 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "HBRecorder"
        )
        if (!f1.exists() && !f1.mkdirs()) {
            Log.i("Folder ", "created")
        }
    }

    private fun customSettings() {
        val prefs = this.getSharedPreferences("default", Context.MODE_PRIVATE)

        //Is audio enabled
        handleAudioEnabledSetting(prefs)

        //Audio Source
        handleAudioSourceSetting(prefs)

        //Video Encoder
        handleVideoEncoderSetting(prefs)

        //Video Dimensions
        handleVideoResolutionSetting(prefs)

        //Video Frame Rate
        handleVideoFrameRateSetting(prefs)

        //Video Bitrate
        handleVideoBitRateSetting(prefs)

        //Output Format
        handleOutputFormatSetting(prefs)
    }

    private fun handleAudioEnabledSetting(prefs: SharedPreferences) {
        val audioEnabled = prefs.getBoolean("key_record_audio", true)
        hbRecorder!!.isAudioEnabled(audioEnabled)
    }

    private fun handleAudioSourceSetting(prefs: SharedPreferences) {
        val audioSource = prefs.getString("key_audio_source", null)
        if (audioSource != null) {
            when (audioSource) {
                "0" -> hbRecorder!!.setAudioSource("DEFAULT")
                "1" -> hbRecorder!!.setAudioSource("CAMCODER")
                "2" -> hbRecorder!!.setAudioSource("MIC")
            }
        }
    }

    private fun handleVideoEncoderSetting(prefs: SharedPreferences) {
        val videoEncoder = prefs.getString("key_video_encoder", null)
        if (videoEncoder != null) {
            when (videoEncoder) {
                "0" -> hbRecorder!!.setVideoEncoder("DEFAULT")
                "1" -> hbRecorder!!.setVideoEncoder("H264")
                "2" -> hbRecorder!!.setVideoEncoder("H263")
                "3" -> hbRecorder!!.setVideoEncoder("HEVC")
                "4" -> hbRecorder!!.setVideoEncoder("MPEG_4_SP")
                "5" -> hbRecorder!!.setVideoEncoder("VP8")
            }
        }
    }

    private fun handleVideoResolutionSetting(prefs: SharedPreferences) {
        val videoResolution = prefs.getString("key_video_resolution", null)
        if (videoResolution != null) {
            when (videoResolution) {
                "0" -> hbRecorder!!.setScreenDimensions(426, 240)
                "1" -> hbRecorder!!.setScreenDimensions(640, 360)
                "2" -> hbRecorder!!.setScreenDimensions(854, 480)
                "3" -> hbRecorder!!.setScreenDimensions(1280, 720)
                "4" -> hbRecorder!!.setScreenDimensions(1920, 1080)
            }
        }
    }

    private fun handleVideoFrameRateSetting(prefs: SharedPreferences) {
        val videoFrameRate = prefs.getString("key_video_fps", null)
        if (videoFrameRate != null) {
            when (videoFrameRate) {
                "0" -> hbRecorder!!.setVideoFrameRate(60)
                "1" -> hbRecorder!!.setVideoFrameRate(50)
                "2" -> hbRecorder!!.setVideoFrameRate(48)
                "3" -> hbRecorder!!.setVideoFrameRate(30)
                "4" -> hbRecorder!!.setVideoFrameRate(25)
                "5" -> hbRecorder!!.setVideoFrameRate(24)
            }
        }
    }

    private fun handleVideoBitRateSetting(prefs: SharedPreferences) {
        val videoBitRate = prefs.getString("key_video_bitrate", null)
        if (videoBitRate != null) {
            when (videoBitRate) {
                "1" -> hbRecorder!!.setVideoBitrate(12000000)
                "2" -> hbRecorder!!.setVideoBitrate(8000000)
                "3" -> hbRecorder!!.setVideoBitrate(7500000)
                "4" -> hbRecorder!!.setVideoBitrate(5000000)
                "5" -> hbRecorder!!.setVideoBitrate(4000000)
                "6" -> hbRecorder!!.setVideoBitrate(2500000)
                "7" -> hbRecorder!!.setVideoBitrate(1500000)
                "8" -> hbRecorder!!.setVideoBitrate(1000000)
            }
        }
    }

    private fun handleOutputFormatSetting(prefs: SharedPreferences) {
        val outputFormat = prefs.getString("key_output_format", null)
        if (outputFormat != null) {
            when (outputFormat) {
                "0" -> hbRecorder!!.setOutputFormat("DEFAULT")
                "1" -> hbRecorder!!.setOutputFormat("MPEG_4")
                "2" -> hbRecorder!!.setOutputFormat("THREE_GPP")
                "3" -> hbRecorder!!.setOutputFormat("WEBM")
            }
        }
    }


    private fun startRecordingScreen() {
        if (binding.customSettingsSwitch.isChecked) {
            //WHEN SETTING CUSTOM SETTINGS YOU MUST SET THIS!!!
            hbRecorder!!.enableCustomSettings()
            customSettings()
            val mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
            startForResult.launch(permissionIntent)
        } else {
            quickSettings()
            val mediaProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
            startForResult.launch(permissionIntent)
        }
        binding.buttonStart.setText(R.string.stop_recording)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // The Intent that was used to launch the activity is available
                val intent = result.data
                // You can now process the intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //Set file path or Uri depending on SDK version
                    setOutputPath()
                    //Start screen recording
                    hbRecorder!!.startScreenRecording(intent, result.resultCode)
                } else {
                    showLongToast("Screen Cast Permission Denied")
                }
            }
        }

    private fun quickSettings() {
        hbRecorder!!.setAudioBitrate(128000)
        hbRecorder!!.setAudioSamplingRate(44100)
        hbRecorder!!.recordHDVideo(wasHDSelected)
        hbRecorder!!.isAudioEnabled(isAudioEnabled)
        //Customise Notification
        hbRecorder!!.setNotificationSmallIcon(R.drawable.icon)
        hbRecorder!!.setNotificationTitle(getString(R.string.stop_recording_notification_title))
        hbRecorder!!.setNotificationDescription(getString(R.string.stop_recording_notification_message))
    }

    private fun setRadioGroupCheckListener() {
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.hd_button) {
                //Ser HBRecorder to HD
                wasHDSelected = true
            } else if (checkedId == R.id.sd_button) {
                //Ser HBRecorder to SD
                wasHDSelected = false
            }
        }
    }

    private fun setRecordAudioCheckBoxListener() {
        binding.audioCheckBox.setOnCheckedChangeListener { _, isChecked ->
            isAudioEnabled = isChecked
        }
    }

    override fun HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called")
    }

    override fun HBRecorderOnComplete() {
        binding.buttonStart.setText(R.string.start_recording)
        showLongToast("Saved Successfully")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Update gallery depending on SDK Level
            if (hbRecorder!!.wasUriSet()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    updateGalleryUri()
                } else {
                    refreshGalleryFile()
                }
            } else {
                refreshGalleryFile()
            }
        }
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        when (errorCode) {
            Constants.SETTINGS_ERROR -> {
                showLongToast(getString(R.string.settings_not_supported_message))
            }

            Constants.MAX_FILE_SIZE_REACHED_ERROR -> {
                showLongToast(getString(R.string.max_file_size_reached_message))
            }

            else -> {
                showLongToast(getString(R.string.general_recording_error_message))
                Log.e("HBRecorderOnError", reason!!)
            }
        }

        binding.buttonStart.setText(R.string.start_recording)
    }

    override fun HBRecorderOnPause() {
        Log.d("HBRecorder", "HBRecorderOnPause called")
    }

    override fun HBRecorderOnResume() {
        Log.d("HBRecorder", "HBRecorderOnResume called")
    }

    private fun refreshGalleryFile() {
        MediaScannerConnection.scanFile(
            this,
            arrayOf(hbRecorder!!.filePath), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }

    private fun updateGalleryUri() {
        contentValues?.clear()
        contentValues?.put(MediaStore.Video.Media.IS_PENDING, 0)
        mUri?.let { contentResolver.update(it, contentValues, null, null) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item = menu?.findItem(R.id.action_settings)
        item?.isVisible = true
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constant.PERMISSION_REQ_POST_NOTIFICATIONS -> handlePostNotificationsPermission(grantResults)
            Constant.PERMISSION_REQ_ID_RECORD_AUDIO -> handleRecordAudioPermission(grantResults)
            Constant.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE -> handleWriteExternalStoragePermission(
                grantResults
            )

            else -> handleUnknownPermission()
        }
    }

    private fun handlePostNotificationsPermission(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkSelfPermission(RECORD_AUDIO, Constant.PERMISSION_REQ_ID_RECORD_AUDIO)
        } else {
            hasPermissions = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                showLongToast("No permission for $POST_NOTIFICATIONS")
            }
        }
    }

    private fun handleRecordAudioPermission(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkSelfPermission(
                WRITE_EXTERNAL_STORAGE,
                Constant.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
            )
        } else {
            hasPermissions = false
            showLongToast("No permission for $RECORD_AUDIO")
        }
    }

    private fun handleWriteExternalStoragePermission(grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermissions = true
            startRecordingScreen()
        } else {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasPermissions = true
            } else {
                hasPermissions = false
                showLongToast("No permission for $WRITE_EXTERNAL_STORAGE")
            }
        }
    }

    private fun handleUnknownPermission() {
        hasPermissions = false
        showLongToast("No permission for $WRITE_EXTERNAL_STORAGE")
    }

    @Deprecated("Deprecated in Java. Do not forget to remove this deprecated code someday.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestCode == Constant.SCREEN_RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            // Set file path or Uri depending on SDK version
            setOutputPath()
            // Start screen recording
            hbRecorder!!.startScreenRecording(data, resultCode)
        } else {
            showLongToast("Screen Cast Permission Denied")
        }
    }


    private fun setOutputPath() {
        val filename: String = generateFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = contentResolver
            contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HBRecorder")
                put(MediaStore.Video.Media.TITLE, filename)
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            }
            resolver?.let {
                mUri = it.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                // FILE NAME SHOULD BE THE SAME
                hbRecorder!!.fileName = filename
                hbRecorder!!.setOutputUri(mUri)
            }
        } else {
            createFolder()
            hbRecorder!!.setOutputPath(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    .toString() + "/HBRecorder"
            )
        }
    }


    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate).replace(" ", "")
    }

    private fun showLongToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

}
