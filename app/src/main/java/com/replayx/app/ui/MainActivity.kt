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
    private val service = ReplayTransferService()
    private val SHIZUKU_CODE = 1001

    private val binderReceived = Shizuku.OnBinderReceivedListener { updateStatus(true) }
    private val binderDead = Shizuku.OnBinderDeadListener { updateStatus(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

    private fun checkShizuku(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                log("[ERRO] Shizuku nao esta ativo!")
                return false
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_CODE)
                log("[AGUARDANDO] Conceda permissao ao Shizuku...")
                return false
            }
            true
        } catch (ex: Exception) {
            log("[ERRO] " + ex.message.orEmpty())
            false
        }
    }

    private fun startTransfer(direction: String) {
        binding.btnBypassMaxToNormal.isEnabled = false
        binding.btnBypassNormalToMax.isEnabled = false
        lifecycleScope.launch {
            log("===========================")
            log("[INICIO] Bypass " + direction)
            log("===========================")
            delay(200)
            val result = withContext(Dispatchers.IO) {
                if (direction == "maxToNormal") {
                    service.transferMaxToNormal { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                } else {
                    service.transferNormalToMax { msg -> lifecycleScope.launch(Dispatchers.Main) { log(msg) } }
                }
            }
            log("===========================")
            if (result.success) {
                log("[OK] BYPASS CONCLUIDO!")
            } else {
                log("[ERRO] " + result.errorMessage)
            }
            log("===========================")
            binding.btnBypassMaxToNormal.isEnabled = true
            binding.btnBypassNormalToMax.isEnabled = true
        }
    }

    private fun log(msg: String) {
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val cur = binding.tvLog.text.toString()
        val sep = System.lineSeparator()
        val next = if (cur.isEmpty()) "[" + t + "] " + msg else cur + sep + "[" + t + "] " + msg
        binding.tvLog.text = next
        binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun clearLog() {
        binding.tvLog.text = ""
        log("[SISTEMA] Log limpo.")
    }

    private fun updateStatus(active: Boolean) {
        runOnUiThread {
            if (active) {
                binding.tvShizukuStatus.text = "SHIZUKU ATIVO"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_green_light))
            } else {
                binding.tvShizukuStatus.text = "SHIZUKU INATIVO"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }
}
