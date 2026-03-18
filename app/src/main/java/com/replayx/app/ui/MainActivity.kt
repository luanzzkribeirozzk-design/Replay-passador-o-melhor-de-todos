package com.replayx.app.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.replayx.app.databinding.ActivityMainBinding
import com.replayx.app.service.ReplayTransferService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val service = ReplayTransferService()
    private val SHIZUKU_CODE = 1001
    private val PREFS_NAME = "replayx_prefs"
    private val PREF_HIDE = "hide_stream"
    private val binderReceived = Shizuku.OnBinderReceivedListener { updateStatus(true) }
    private val binderDead = Shizuku.OnBinderDeadListener { updateStatus(false) }
    private var applyingJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restaurar estado do Hide Stream ao abrir
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hideActive = prefs.getBoolean(PREF_HIDE, false)
        applyHideStream(hideActive)
        binding.switchHideStream.isChecked = hideActive
        updateHideStreamUI(hideActive)

        binding.switchHideStream.setOnCheckedChangeListener { _, isChecked ->
            applyHideStream(isChecked)
            updateHideStreamUI(isChecked)
            prefs.edit().putBoolean(PREF_HIDE, isChecked).apply()
            log(if (isChecked) "[SYS] >> HIDE_STREAM: ON" else "[SYS] >> HIDE_STREAM: OFF")
        }

        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        binding.btnBypassMaxToNormal.setOnClickListener {
            if (checkShizuku()) startTransfer("maxToNormal")
        }
        binding.btnBypassNormalToMax.setOnClickListener {
            if (checkShizuku()) startTransfer("normalToMax")
        }
        binding.btnClearLog.setOnClickListener { clearLog() }
    }

    private fun applyHideStream(active: Boolean) {
        if (active) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun updateHideStreamUI(active: Boolean) {
        if (active) {
            binding.tvHideStreamStatus.text = "HIDE STREAM: ON"
            binding.tvHideStreamStatus.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            binding.tvHideStreamStatus.text = "HIDE STREAM: OFF"
            binding.tvHideStreamStatus.setTextColor(0xFF444444.toInt())
        }
    }

    private fun checkShizuku(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) { log("[ERR] SHIZUKU_DEAD"); return false }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_CODE)
                log("[SYS] SHIZUKU_PERM_REQUEST")
                return false
            }
            true
        } catch (ex: Exception) { log("[ERR] " + ex.message.orEmpty()); false }
    }

    private fun startTransfer(direction: String) {
        binding.btnBypassMaxToNormal.isEnabled = false
        binding.btnBypassNormalToMax.isEnabled = false
        lifecycleScope.launch {
            log("--------------------------------")
            delay(100)
            val dots = listOf(".", "..", "...")
            applyingJob = lifecycleScope.launch {
                var i = 0
                while (true) {
                    binding.tvApplying.text = "Aplicando bypass" + dots[i % 3]
                    binding.tvApplying.visibility = View.VISIBLE
                    i++
                    delay(400)
                }
            }
            val result = withContext(Dispatchers.IO) {
                if (direction == "maxToNormal") {
                    service.transferMaxToNormal { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                } else {
                    service.transferNormalToMax { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                }
            }
            applyingJob?.cancel()
            if (result.success) {
                binding.tvApplying.text = "Bypass aplicado!"
                log("[SYS] >> BYPASS_COMPLETE 0x00")
            } else {
                binding.tvApplying.text = "Falha no bypass"
                log("[ERR] >> BYPASS_FAIL: " + result.errorMessage)
            }
            log("--------------------------------")
            delay(2000)
            binding.tvApplying.visibility = View.GONE
            binding.btnBypassMaxToNormal.isEnabled = true
            binding.btnBypassNormalToMax.isEnabled = true
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
        applyingJob?.cancel()
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }
}
