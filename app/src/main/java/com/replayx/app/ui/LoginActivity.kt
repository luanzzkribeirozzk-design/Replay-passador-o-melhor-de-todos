package com.replayx.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.replayx.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db by lazy { Firebase.firestore }
    private val PREFS = "replayx_prefs"
    private val PREF_KEY = "saved_key"
    private val PREF_REMEMBER = "remember_key"
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val remember = prefs.getBoolean(PREF_REMEMBER, false)
        val savedKey = prefs.getString(PREF_KEY, "")

        binding.switchRemember.isChecked = remember
        if (remember && !savedKey.isNullOrEmpty()) {
            binding.etKey.setText(savedKey)
        }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.etKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { doLogin(); true } else false
        }
    }

    private fun doLogin() {
        val key = binding.etKey.text.toString().trim()
        if (key.isEmpty()) { showError("Insira sua key!"); return }

        setLoading(true)
        hideError()

        // Buscar key no Firestore
        db.collection("keys")
            .whereEqualTo("keyString", key)
            .get()
            .addOnSuccessListener { docs ->
                setLoading(false)
                if (docs.isEmpty) { showError("[ERR] Key invalida"); return@addOnSuccessListener }
                val doc = docs.documents[0]
                val data = doc.data ?: run { showError("[ERR] Dados corrompidos"); return@addOnSuccessListener }

                val status = data["status"] as? String ?: ""
                if (status == "paused") { showError("[ERR] Key pausada"); return@addOnSuccessListener }
                if (status != "active") { showError("[ERR] Key inativa"); return@addOnSuccessListener }

                val deviceId = data["deviceId"] as? String
                val myDeviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

                if (deviceId != null && deviceId != myDeviceId) {
                    showError("[ERR] Key ja usada em outro dispositivo")
                    return@addOnSuccessListener
                }

                // Primeiro uso - registrar device e firstUsed
                if (deviceId == null) {
                    val now = com.google.firebase.Timestamp.now()
                    doc.reference.update(
                        mapOf("deviceId" to myDeviceId, "firstUsed" to now)
                    ).addOnSuccessListener {
                        onLoginSuccess(doc.id, data, myDeviceId)
                    }
                } else {
                    onLoginSuccess(doc.id, data, myDeviceId)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError("[ERR] " + e.message)
            }
    }

    private fun onLoginSuccess(docId: String, data: Map<String, Any>, deviceId: String) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val remember = binding.switchRemember.isChecked
        val key = binding.etKey.text.toString().trim()
        prefs.edit()
            .putBoolean(PREF_REMEMBER, remember)
            .putString(PREF_KEY, if (remember) key else "")
            .putString("key_doc_id", docId)
            .putString("key_device_id", deviceId)
            .apply()

        // Verificar se expirou
        val days = (data["days"] as? Long)?.toInt() ?: 0
        val firstUsed = data["firstUsed"] as? com.google.firebase.Timestamp
        val pausedAt = data["pausedAt"] as? com.google.firebase.Timestamp
        val status = data["status"] as? String ?: "active"

        if (firstUsed != null) {
            val nowSec = System.currentTimeMillis() / 1000
            val usedSec = if (status == "paused" && pausedAt != null)
                pausedAt.seconds - firstUsed.seconds
            else
                nowSec - firstUsed.seconds
            val remainDays = days - (usedSec / 86400).toInt()
            if (remainDays <= 0) {
                showError("[ERR] Key expirada")
                return
            }
        }

        val user = data["user"] as? String ?: ""
        val firstUsedTs = data["firstUsed"] as? com.google.firebase.Timestamp

        // Mostrar timer e ir para MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("key_user", user)
        intent.putExtra("key_days", days)
        intent.putExtra("key_first_used_sec", firstUsedTs?.seconds ?: (System.currentTimeMillis() / 1000))
        intent.putExtra("key_status", status)
        intent.putExtra("key_paused_at_sec", (pausedAt?.seconds ?: 0L))
        startActivity(intent)
        finish()
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
