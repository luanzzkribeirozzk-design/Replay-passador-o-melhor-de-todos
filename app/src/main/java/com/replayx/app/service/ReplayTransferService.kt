package com.replayx.app.service

import com.replayx.app.util.ShizukuHelper

data class TransferResult(
    val success: Boolean,
    val filesCopied: Int = 0,
    val errorMessage: String = ""
)

class ReplayTransferService {

    fun transferReplays(
        sourcePackage: String,
        destPackage: String,
        logCallback: (String) -> Unit
    ): TransferResult {
        return try {
            val src = "/sdcard/Android/data/" + sourcePackage + "/files/MReplays"
            val dst = "/sdcard/Android/data/" + destPackage + "/files/MReplays"
            logCallback("[SCAN] " + src)
            val check = sh("ls " + src)
            logCallback("[LS] " + check.take(120))
            if (check.contains("Permission denied")) {
                return TransferResult(false, 0, "Permissao negada - verifique Shizuku")
            }
            if (check.contains("No such file")) {
                return TransferResult(false, 0, "Pasta nao encontrada em " + sourcePackage)
            }
            val files = check.trim().lines().filter { it.isNotBlank() }
            logCallback("[INFO] " + files.size.toString() + " arquivo(s)")
            if (files.isEmpty()) {
                return TransferResult(false, 0, "Nenhum replay encontrado")
            }
            sh("mkdir -p " + dst)
            var copied = 0
            for (i in files.indices) {
                val name = files[i].trim()
                if (name.isBlank()) continue
                logCallback("[COPY] " + name)
                val cmd = "cp -rf " + src + "/" + name + " " + dst + "/" + name
                val r = sh(cmd)
                if (!r.lowercase().contains("error") && !r.lowercase().contains("permission")) {
                    copied++
                    logCallback("[OK] " + name)
                } else {
                    logCallback("[WARN] " + r.take(60))
                }
            }
            logCallback("[DONE] " + copied.toString() + "/" + files.size.toString())
            TransferResult(true, copied)
        } catch (ex: Exception) {
            logCallback("[ERRO] " + ex.message.orEmpty())
            TransferResult(false, 0, ex.message.orEmpty())
        }
    }

    private fun sh(cmd: String): String {
        return try {
            val p = ShizukuHelper.exec(cmd)
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            p.waitFor()
            if (out.isNotBlank()) out else err
        } catch (ex: Exception) {
            "ERR " + ex.message.orEmpty()
        }
    }
}
