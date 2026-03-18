package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(
    val success: Boolean,
    val filesCopied: Int = 0,
    val errorMessage: String = ""
)

class ReplayTransferService {

    fun transferMaxToNormal(logCallback: (String) -> Unit): TransferResult {
        logCallback("[EXEC] Copiando replay FFM -> FFN...")
        val result = ShizukuHelper.runMaxToNormal()
        logCallback("[OUT] " + result)
        return if (result.contains("sucesso") || result.contains("copiado")) {
            logCallback("[OK] Replay transferido!")
            TransferResult(true, 1)
        } else {
            logCallback("[ERRO] " + result)
            TransferResult(false, 0, result)
        }
    }

    fun transferNormalToMax(logCallback: (String) -> Unit): TransferResult {
        logCallback("[EXEC] Copiando replay FFN -> FFM...")
        val result = ShizukuHelper.runNormalToMax()
        logCallback("[OUT] " + result)
        return if (result.contains("sucesso") || result.contains("copiado")) {
            logCallback("[OK] Replay transferido!")
            TransferResult(true, 1)
        } else {
            logCallback("[ERRO] " + result)
            TransferResult(false, 0, result)
        }
    }
}
