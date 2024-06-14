package com.remotetechs.kun510.ui.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.remotetechs.kun510.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SettingsActivity", "onCreate called")
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // load settings fragment
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, MainPreferenceFragment())
            .commit()
    }

    class MainPreferenceFragment : PreferenceFragmentCompat(),
        Preference.OnPreferenceChangeListener,
        androidx.preference.Preference.OnPreferenceChangeListener {
        private lateinit var keyVideoResolution: ListPreference
        private lateinit var keyAudioSource: ListPreference
        private lateinit var keyVideoEncoder: ListPreference
        private lateinit var keyVideoFps: ListPreference
        private lateinit var keyVideoBitrate: ListPreference
        private lateinit var keyOutputFormat: ListPreference
        private lateinit var keyRecordAudio: SwitchPreferenceCompat

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_main)

            keyRecordAudio = findPreference<SwitchPreferenceCompat>(getString(R.string.key_record_audio))!!
            keyAudioSource = findPreference(getString(R.string.key_audio_source))!!
            keyVideoEncoder = findPreference(getString(R.string.key_video_encoder))!!
            keyVideoResolution = findPreference(getString(R.string.key_video_resolution))!!
            keyVideoFps = findPreference(getString(R.string.key_video_fps))!!
            keyVideoBitrate = findPreference(getString(R.string.key_video_bitrate))!!
            keyOutputFormat = findPreference(getString(R.string.key_output_format))!!

            setPreferenceListeners()
            setPreviousSelectedAsSummary()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        }

        private fun setPreferenceListeners() {
            listOf(keyAudioSource, keyVideoEncoder, keyVideoResolution, keyVideoFps, keyVideoBitrate, keyOutputFormat)
                .forEach { it.onPreferenceChangeListener = this }
        }

        private fun updatePreferenceSummary(preference: ListPreference, newValue: String) {
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(newValue)
                preference.setSummary(preference.entries[index])
                preference.value = newValue
            }
        }
        private fun setPreviousSelectedAsSummary() {
            activity?.let {
                val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(it)

                keyRecordAudio.isChecked = prefs.getBoolean("key_record_audio", true)

                updatePreferenceSummary(keyAudioSource, prefs.getString("key_audio_source", "") ?: "")
                updatePreferenceSummary(keyVideoEncoder, prefs.getString("key_video_encoder", "") ?: "")
                updatePreferenceSummary(keyVideoResolution, prefs.getString("key_video_resolution", "") ?: "")
                updatePreferenceSummary(keyVideoFps, prefs.getString("key_video_fps", "") ?: "")
                updatePreferenceSummary(keyVideoBitrate, prefs.getString("key_video_bitrate", "") ?: "")
                updatePreferenceSummary(keyOutputFormat, prefs.getString("key_output_format", "") ?: "")
            }
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            if (preference is ListPreference) {
                updatePreferenceSummary(preference, newValue.toString())
            }
            return true
        }

        override fun onPreferenceChange(
            preference: androidx.preference.Preference,
            newValue: Any?
        ): Boolean {
            if (preference is ListPreference) {
                updatePreferenceSummary(preference, newValue.toString())
            }
            return true
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
