package com.replayx.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.replayx.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
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
        val savedKey = prefs.getString(PREF_KEY, "") ?: ""

        binding.switchRemember.isChecked = remember
        if (remember && savedKey.isNotEmpty()) {
            binding.etKey.setText(savedKey)
        }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.etKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { doLogin(); true } else false
        }
    }

    private fun doLogin() {
        val key = binding.etKey.text.toString().trim()
        if (key.isEmpty()) { showError("[ERR] Insira sua key!"); return }

        setLoading(true)
        hideError()

        db.collection("keys")
            .whereEqualTo("keyString", key)
            .get()
            .addOnSuccessListener { docs ->
                setLoading(false)
                if (docs.isEmpty) {
                    showError("[ERR] Key invalida")
                    return@addOnSuccessListener
                }
                val doc = docs.documents[0]
                val data = doc.data
                if (data == null) {
                    showError("[ERR] Dados invalidos")
                    return@addOnSuccessListener
                }

                val status = data["status"] as? String ?: ""
                if (status == "paused") { showError("[ERR] Key pausada"); return@addOnSuccessListener }
                if (status != "active") { showError("[ERR] Key inativa"); return@addOnSuccessListener }

                val deviceId = data["deviceId"] as? String
                val myDevice = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

                if (deviceId != null && deviceId != myDevice) {
                    showError("[ERR] Key em uso em outro dispositivo")
                    return@addOnSuccessListener
                }

                if (deviceId == null) {
                    val now = Timestamp.now()
                    doc.reference.update(mapOf("deviceId" to myDevice, "firstUsed" to now))
                        .addOnSuccessListener { proceedLogin(doc.id, data, myDevice) }
                        .addOnFailureListener { e -> showError("[ERR] " + e.message) }
                } else {
                    proceedLogin(doc.id, data, myDevice)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError("[ERR] " + (e.message ?: "Falha de conexao"))
            }
    }

    private fun proceedLogin(docId: String, data: Map<String, Any>, myDevice: String) {
        val days = (data["days"] as? Long)?.toInt() ?: 0
        val firstUsedTs = data["firstUsed"] as? Timestamp
        val pausedAtTs = data["pausedAt"] as? Timestamp
        val status = data["status"] as? String ?: "active"
        val user = data["user"] as? String ?: ""

        // Verificar expiracao
        if (firstUsedTs != null) {
            val nowSec = System.currentTimeMillis() / 1000L
            val usedSec = if (status == "paused" && pausedAtTs != null)
                pausedAtTs.seconds - firstUsedTs.seconds
            else
                nowSec - firstUsedTs.seconds
            val remainDays = days - (usedSec / 86400L).toInt()
            if (remainDays <= 0) {
                showError("[ERR] Key expirada")
                setLoading(false)
                return
            }
        }

        val key = binding.etKey.text.toString().trim()
        val remember = binding.switchRemember.isChecked
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(PREF_REMEMBER, remember)
            .putString(PREF_KEY, if (remember) key else "")
            .apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("key_user", user)
        intent.putExtra("key_days", days)
        intent.putExtra("key_first_used_sec", firstUsedTs?.seconds ?: (System.currentTimeMillis() / 1000L))
        intent.putExtra("key_status", status)
        intent.putExtra("key_paused_at_sec", pausedAtTs?.seconds ?: 0L)
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
