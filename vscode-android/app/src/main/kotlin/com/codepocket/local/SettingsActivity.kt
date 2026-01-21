package com.codepocket.local

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codepocket.local.databinding.ActivitySettingsBinding

/**
 * Settings activity for configuring the server URL and other options.
 * 
 * For MVP, this provides:
 * - Server URL configuration (default: placeholder, later: localhost)
 * - Reset to default option
 * 
 * Future additions:
 * - Port configuration
 * - Workspace management
 * - Theme selection
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        loadCurrentSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupUI() {
        // Save URL when it changes (with debouncing via TextWatcher)
        binding.etServerUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveUrl(s?.toString() ?: PrefsConstants.DEFAULT_URL)
            }
        })

        // Reset to default button
        binding.btnResetDefault.setOnClickListener {
            binding.etServerUrl.setText(PrefsConstants.DEFAULT_URL)
            saveUrl(PrefsConstants.DEFAULT_URL)
            Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show()
        }

        // Preset buttons for common URLs
        binding.btnPresetLocalhost.setOnClickListener {
            val url = PrefsConstants.DEFAULT_URL
            binding.etServerUrl.setText(url)
            saveUrl(url)
        }

        binding.btnPresetPlaceholder.setOnClickListener {
            val url = PrefsConstants.PLACEHOLDER_URL
            binding.etServerUrl.setText(url)
            saveUrl(url)
        }
    }

    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences(PrefsConstants.PREF_NAME, MODE_PRIVATE)
        val url = prefs.getString(PrefsConstants.KEY_SERVER_URL, PrefsConstants.DEFAULT_URL)
        binding.etServerUrl.setText(url)
    }

    private fun saveUrl(url: String) {
        val prefs = getSharedPreferences(PrefsConstants.PREF_NAME, MODE_PRIVATE)
        prefs.edit().putString(PrefsConstants.KEY_SERVER_URL, url).apply()
    }
}
