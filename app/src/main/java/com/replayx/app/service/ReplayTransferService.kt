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
        return when {
            result.contains("COPIADO_OK") || result.contains("sucesso") -> {
                logCallback("[OK] Replay transferido com sucesso!")
                TransferResult(true, 1)
            }
            result.contains("NAO_ENCONTRADO") -> {
                logCallback("[AVISO] Nenhum replay encontrado no FF Max")
                TransferResult(false, 0, "Nenhum replay no FF Max")
            }
            result.startsWith("ERR") -> {
                logCallback("[ERRO] " + result)
                TransferResult(false, 0, result)
            }
            else -> {
                logCallback("[OK] Operacao concluida: " + result)
                TransferResult(true, 1)
            }
        }
    }

    fun transferNormalToMax(logCallback: (String) -> Unit): TransferResult {
        logCallback("[EXEC] Copiando replay FFN -> FFM...")
        val result = ShizukuHelper.runNormalToMax()
        logCallback("[OUT] " + result)
        return when {
            result.contains("COPIADO_OK") || result.contains("sucesso") -> {
                logCallback("[OK] Replay transferido com sucesso!")
                TransferResult(true, 1)
            }
            result.contains("NAO_ENCONTRADO") -> {
                logCallback("[AVISO] Nenhum replay encontrado no FF Normal")
                TransferResult(false, 0, "Nenhum replay no FF Normal")
            }
            result.startsWith("ERR") -> {
                logCallback("[ERRO] " + result)
                TransferResult(false, 0, result)
            }
            else -> {
                logCallback("[OK] Operacao concluida: " + result)
                TransferResult(true, 1)
            }
        }
    }
}
