package com.replayx.app.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.replayx.app.databinding.ActivityMainBinding
import com.replayx.app.service.ReplayTransferService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private val service = ReplayTransferService()
    private val SHIZUKU_CODE = 1001
    private val PREFS_NAME = "replayx_prefs"
    private val PREF_HIDE = "hide_stream"
    private val PREF_COUNT = "bypass_count"
    private val binderReceived = Shizuku.OnBinderReceivedListener { updateStatus(true) }
    private val binderDead = Shizuku.OnBinderDeadListener { updateStatus(false) }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var bypassCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tts = TextToSpeech(this, this)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        bypassCount = prefs.getInt(PREF_COUNT, 0)
        val hideActive = prefs.getBoolean(PREF_HIDE, false)
        applyHideStream(hideActive)
        binding.switchHideStream.isChecked = hideActive
        updateHideStreamUI(hideActive)
        binding.switchHideStream.setOnCheckedChangeListener { _, isChecked ->
            applyHideStream(isChecked)
            updateHideStreamUI(isChecked)
            prefs.edit().putBoolean(PREF_HIDE, isChecked).apply()
            log(if (isChecked) "[SYS] >> HIDE_STREAM: ENABLED" else "[SYS] >> HIDE_STREAM: DISABLED")
        }
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        binding.btnBypassMaxToNormal.setOnClickListener {
            if (checkShizuku()) { speak("Bypass activated"); startTransfer("maxToNormal") }
        }
        binding.btnBypassNormalToMax.setOnClickListener {
            if (checkShizuku()) { speak("Bypass activated"); startTransfer("normalToMax") }
        }
        binding.btnClearLog.setOnClickListener { clearLog() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.9f)
            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun applyHideStream(active: Boolean) {
        if (active) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun updateHideStreamUI(active: Boolean) {
        if (active) {
            binding.tvHideStreamStatus.text = "HIDE STREAM: ON"
            binding.tvHideStreamStatus.setTextColor(0xFF00FF41.toInt())
        } else {
            binding.tvHideStreamStatus.text = "HIDE STREAM: OFF"
            binding.tvHideStreamStatus.setTextColor(0xFF444444.toInt())
        }
    }

    private fun checkShizuku(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) { log("[ERR] SHIZUKU_DEAD"); false }
            else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_CODE); log("[SYS] SHIZUKU_PERM_REQUEST"); false
            } else true
        } catch (ex: Exception) { log("[ERR] " + ex.message.orEmpty()); false }
    }

    private fun startTransfer(direction: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        lifecycleScope.launch {
            log("--------------------------------")
            bypassCount++
            prefs.edit().putInt(PREF_COUNT, bypassCount).apply()
            val count = bypassCount
            val result = withContext(Dispatchers.IO) {
                if (direction == "maxToNormal") {
                    service.transferMaxToNormal(count) { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                } else {
                    service.transferNormalToMax(count) { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                }
            }
            if (!result.success) log("[ERR] >> BYPASS_FAIL")
            log("--------------------------------")
        }
    }

    private fun log(msg: String) {
        val t = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val cur = binding.tvLog.text.toString()
        val sep = System.lineSeparator()
        val next = if (cur.isEmpty()) "[" + t + "] " + msg else cur + sep + "[" + t + "] " + msg
        binding.tvLog.text = next
        binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun clearLog() { binding.tvLog.text = "" }

    private fun updateStatus(active: Boolean) {
        runOnUiThread {
            if (active) {
                binding.tvShizukuStatus.text = "● SHIZUKU ATIVO"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_green_light))
            } else {
                binding.tvShizukuStatus.text = "● SHIZUKU INATIVO"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop(); tts?.shutdown()
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }
}
