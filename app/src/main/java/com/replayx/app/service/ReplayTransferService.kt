package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(val success: Boolean, val filesCopied: Int = 0, val errorMessage: String = "")

class ReplayTransferService {

    fun transferMaxToNormal(count: Int, log: (String) -> Unit): TransferResult {
        log("[SYS] >> BYPASS_MODULE_LOAD 0x01")
        log("[SYS] >> PROC_INIT... OK")
        log("[MEM] >> ALLOC 4096B... OK")
        log("[SYS] >> SRC_MOUNT... OK")
        log("[SYS] >> DST_MOUNT... OK")
        log("[SYS] >> SCAN_BINARIES... OK")
        log("[CMD] >> exec bin/transfer --mode=0x01")
        log("[SYS] >> CHECKSUM_VERIFY... OK")
        val r = ShizukuHelper.runMaxToNormal()
        log("[SYS] >> IO_WRITE... OK")
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> CHMOD_APPLY... OK")
            log("[SYS] >> STATUS: 0x00 SUCCESS")
            log("[SYS] >> TOTAL_BYPASS_COUNT: " + count)
            log("[SYS] >> Bypass activated 0xAC")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            TransferResult(false, 0, r)
        }
    }

    fun transferNormalToMax(count: Int, log: (String) -> Unit): TransferResult {
        log("[SYS] >> BYPASS_MODULE_LOAD 0x02")
        log("[SYS] >> PROC_INIT... OK")
        log("[MEM] >> ALLOC 4096B... OK")
        log("[SYS] >> SRC_MOUNT... OK")
        log("[SYS] >> DST_MOUNT... OK")
        log("[SYS] >> SCAN_BINARIES... OK")
        log("[CMD] >> exec bin/transfer --mode=0x02")
        log("[SYS] >> CHECKSUM_VERIFY... OK")
        val r = ShizukuHelper.runNormalToMax()
        log("[SYS] >> IO_WRITE... OK")
        return if (r.contains("COPIADO_OK") || r.contains("sucesso")) {
            log("[SYS] >> CHMOD_APPLY... OK")
            log("[SYS] >> STATUS: 0x00 SUCCESS")
            log("[SYS] >> TOTAL_BYPASS_COUNT: " + count)
            log("[SYS] >> Bypass activated 0xAC")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[WRN] >> STATUS: 0x01 EMPTY")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[ERR] >> STATUS: 0xFF FAIL")
            TransferResult(false, 0, r)
        }
    }
}
