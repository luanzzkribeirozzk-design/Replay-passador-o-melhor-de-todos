package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(val success: Boolean, val filesCopied: Int = 0, val errorMessage: String = "")

class ReplayTransferService {

    fun transferMaxToNormal(log: (String) -> Unit): TransferResult {
        log("[SYS] >> BYPASS_INIT FFM->FFN")
        log("[SRC] /Android/data/com.dts.freefiremax/files/MReplays")
        log("[DST] /Android/data/com.dts.freefireth/files/MReplays")
        log("[CMD] ls -t *.bin | head -n1")
        val r = ShizukuHelper.runMaxToNormal()
        log("[OUT] " + r)
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> STATUS: 0x00 OK")
            log("[SYS] >> cp -f OK | chmod 666 OK")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY_SRC")
            TransferResult(false, 0, "NAO_ENCONTRADO")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            TransferResult(false, 0, r)
        }
    }

    fun transferNormalToMax(log: (String) -> Unit): TransferResult {
        log("[SYS] >> BYPASS_INIT FFN->FFM")
        log("[SRC] /Android/data/com.dts.freefireth/files/MReplays")
        log("[DST] /Android/data/com.dts.freefiremax/files/MReplays")
        log("[CMD] ls -t *.bin | head -n1")
        val r = ShizukuHelper.runNormalToMax()
        log("[OUT] " + r)
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> STATUS: 0x00 OK")
            log("[SYS] >> cp -f OK | chmod 666 OK")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY_SRC")
            TransferResult(false, 0, "NAO_ENCONTRADO")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            TransferResult(false, 0, r)
        }
    }
}
