package com.replayx.app.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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
    private val transferService = ReplayTransferService()
    private val SHIZUKU_CODE = 1001

    private val shizukuBinderReceiver = Shizuku.OnBinderReceivedListener {
        updateShizukuStatus(true)
    }

    private val shizukuDeadReceiver = Shizuku.OnBinderDeadListener {
        updateShizukuStatus(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        setupShizuku()
    }

    private fun setupUI() {
        binding.btnBypassMaxToNormal.setOnClickListener {
            if (!checkShizukuReady()) return@setOnClickListener
            startTransfer("com.dts.freefiremax", "com.dts.freefireth", "FFM -> FFN")
        }
        binding.btnBypassNormalToMax.setOnClickListener {
            if (!checkShizukuReady()) return@setOnClickListener
            startTransfer("com.dts.freefireth", "com.dts.freefiremax", "FFN -> FFM")
        }
        binding.btnClearLog.setOnClickListener {
            clearLog()
        }
    }

    private fun setupShizuku() {
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceiver)
        Shizuku.addBinderDeadListener(shizukuDeadReceiver)
    }

    private fun checkShizukuReady(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                appendLog("[ERRO] Shizuku nao esta ativo!")
                return false
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_CODE)
                appendLog("[AGUARDANDO] Conceda permissao ao Shizuku...")
                return false
            }
            true
        } catch (e: Exception) {
            appendLog("[ERRO] Shizuku: ${e.message}")
            false
        }
    }

    private fun startTransfer(from: String, to: String, label: String) {
        binding.btnBypassMaxToNormal.isEnabled = false
        binding.btnBypassNormalToMax.isEnabled = false

        lifecycleScope.launch {
            appendLog("===========================")
            appendLog("[INICIANDO] Bypass $label")
            appendLog("[FROM] $from")
            appendLog("[TO  ] $to")
            appendLog("===========================")
            delay(200)

            val result = withContext(Dispatchers.IO) {
                transferService.transferReplays(from, to) { msg ->
                    lifecycleScope.launch(Dispatchers.Main) { appendLog(msg) }
                }
            }

            appendLog("===========================")
            if (result.success) {
                appendLog("[OK] CONCLUIDO! ${result.filesCopied} replay(s)")
            } else {
                appendLog("[ERRO] ${result.errorMessage}")
            }
            appendLog("===========================")

            binding.btnBypassMaxToNormal.isEnabled = true
            binding.btnBypassNormalToMax.isEnabled = true
        }
    }

    private fun appendLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val current = binding.tvLog.text.toString()
        val newText = if (current.isEmpty()) "[$time] $message"
                      else "$current\n[$time] $message"
        binding.tvLog.text = newText
        binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun clearLog() {
        binding.tvLog.text = ""
        appendLog("[SISTEMA] Log limpo.")
    }

    private fun updateShizukuStatus(active: Boolean) {
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
        Shizuku.removeBinderReceivedListener(shizukuBinderReceiver)
        Shizuku.removeBinderDeadListener(shizukuDeadReceiver)
    }
}
