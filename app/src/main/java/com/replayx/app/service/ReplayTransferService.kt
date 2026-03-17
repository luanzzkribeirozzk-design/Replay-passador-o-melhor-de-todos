package com.replayx.app.service

import rikka.shizuku.Shizuku

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

            logCallback("[SCAN] Verificando origem...")
            logCallback("[PATH] " + src)

            val check = runShell("ls " + src)
            logCallback("[LS] " + check.take(80))

            if (check.contains("No such file") || check.contains("cannot access")) {
                return TransferResult(false, 0, "Pasta nao encontrada em " + sourcePackage)
            }

            val files = check.trim().split("
").filter { it.isNotBlank() }
            logCallback("[INFO] " + files.size.toString() + " arquivo(s)")

            if (files.isEmpty()) {
                return TransferResult(false, 0, "Nenhum replay encontrado")
            }

            runShell("mkdir -p " + dst)
            logCallback("[MKDIR] OK")

            var copied = 0
            for (i in files.indices) {
                val name = files[i]
                if (name.isBlank()) continue
                logCallback("[COPY] " + (i + 1).toString() + "/" + files.size.toString() + " " + name)
                val r = runShell("cp -rf " + src + "/" + name + " " + dst + "/" + name)
                if (!r.lowercase().contains("error")) {
                    copied++
                    logCallback("[OK] " + name)
                } else {
                    logCallback("[WARN] " + name + " " + r)
                }
            }

            logCallback("[DONE] " + copied.toString() + "/" + files.size.toString())
            TransferResult(true, copied)

        } catch (ex: Exception) {
            logCallback("[ERRO] " + ex.message.orEmpty())
            TransferResult(false, 0, ex.message.orEmpty())
        }
    }

    private fun runShell(cmd: String): String {
        return try {
            val process: Process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (out.isNotBlank()) out else err
        } catch (ex: Exception) {
            "ERR " + ex.message.orEmpty()
        }
    }
}
