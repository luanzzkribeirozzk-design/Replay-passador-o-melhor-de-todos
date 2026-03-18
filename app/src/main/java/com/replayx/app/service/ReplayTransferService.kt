package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(
    val success: Boolean,
    val filesCopied: Int = 0,
    val errorMessage: String = ""
)

class ReplayTransferService {

    fun transferMaxToNormal(logCallback: (String) -> Unit): TransferResult {
        return runScript(ShizukuHelper.SCRIPT_MAX_TO_NORMAL, "FFM->FFN", logCallback)
    }

    fun transferNormalToMax(logCallback: (String) -> Unit): TransferResult {
        return runScript(ShizukuHelper.SCRIPT_NORMAL_TO_MAX, "FFN->FFM", logCallback)
    }

    private fun runScript(script: String, label: String, logCallback: (String) -> Unit): TransferResult {
        return try {
            logCallback("[EXEC] Executando bypass " + label + "...")
            val result = ShizukuHelper.run(script)
            logCallback("[OUT] " + result)
            if (result.contains("sucesso") || result.contains("copiado")) {
                logCallback("[OK] Replay transferido!")
                TransferResult(true, 1)
            } else if (result.startsWith("ERR")) {
                logCallback("[ERRO] " + result)
                TransferResult(false, 0, result)
            } else {
                logCallback("[OK] Operacao concluida")
                TransferResult(true, 1)
            }
        } catch (ex: Exception) {
            logCallback("[ERRO] " + ex.message.orEmpty())
            TransferResult(false, 0, ex.message.orEmpty())
        }
    }
}
