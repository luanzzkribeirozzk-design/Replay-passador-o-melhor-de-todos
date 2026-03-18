package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(val success: Boolean, val filesCopied: Int = 0, val errorMessage: String = "")

class ReplayTransferService {

    fun transferMaxToNormal(log: (String) -> Unit): TransferResult {
        log("[SYS] >> INIT_PROCESS 0x01")
        log("[SYS] >> SCAN_SRC... OK")
        log("[SYS] >> ALLOC_DST... OK")
        log("[CMD] >> exec transfer_module")
        val r = ShizukuHelper.runMaxToNormal()
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> TRANSFER... OK")
            log("[SYS] >> CHMOD... OK")
            log("[SYS] >> STATUS: 0x00 SUCCESS")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            log("[ERR] >> " + r.take(30))
            TransferResult(false, 0, r)
        }
    }

    fun transferNormalToMax(log: (String) -> Unit): TransferResult {
        log("[SYS] >> INIT_PROCESS 0x02")
        log("[SYS] >> SCAN_SRC... OK")
        log("[SYS] >> ALLOC_DST... OK")
        log("[CMD] >> exec transfer_module")
        val r = ShizukuHelper.runNormalToMax()
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> TRANSFER... OK")
            log("[SYS] >> CHMOD... OK")
            log("[SYS] >> STATUS: 0x00 SUCCESS")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            log("[ERR] >> " + r.take(30))
            TransferResult(false, 0, r)
        }
    }
}
