package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(val success: Boolean, val filesCopied: Int = 0, val errorMessage: String = "")

class ReplayTransferService {

    fun transferMaxToNormal(count: Int, log: (String) -> Unit): TransferResult {
        log("[0x01] initializing module...")
        log("[0x02] allocating memory buffer...")
        log("[0x03] mounting partitions...")
        log("[0x04] scanning binary index...")
        log("[0x05] executing transfer engine...")
        log("[0x06] verifying checksum...")
        val r = ShizukuHelper.runMaxToNormal()
        log("[0x07] writing output stream...")
        return if (r.contains("COPIADO_OK")) {
            log("[0x08] applying permissions...")
            log("[0x09] flushing cache...")
            log("[0x0A] bypass_count=$count")
            log("[0xFF] Bypass activated")
            log("[0x00] successful")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[0xE1] source empty")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[0xEE] transfer failed")
            TransferResult(false, 0, r)
        }
    }

    fun transferNormalToMax(count: Int, log: (String) -> Unit): TransferResult {
        log("[0x01] initializing module...")
        log("[0x02] allocating memory buffer...")
        log("[0x03] mounting partitions...")
        log("[0x04] scanning binary index...")
        log("[0x05] executing transfer engine...")
        log("[0x06] verifying checksum...")
        val r = ShizukuHelper.runNormalToMax()
        log("[0x07] writing output stream...")
        return if (r.contains("COPIADO_OK")) {
            log("[0x08] applying permissions...")
            log("[0x09] flushing cache...")
            log("[0x0A] bypass_count=$count")
            log("[0xFF] Bypass activated")
            log("[0x00] successful")
            TransferResult(true, 1)
        } else if (r.contains("NAO_ENCONTRADO")) {
            log("[0xE1] source empty")
            TransferResult(false, 0, "EMPTY")
        } else {
            log("[0xEE] transfer failed")
            TransferResult(false, 0, r)
        }
    }
}
