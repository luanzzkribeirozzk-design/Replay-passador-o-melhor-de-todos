package com.replayx.app.service

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
            val check = exec("ls " + src)
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
            exec("mkdir -p " + dst)
            var copied = 0
            for (i in files.indices) {
                val name = files[i]
                if (name.isBlank()) continue
                logCallback("[COPY] " + name)
                val r = exec("cp -rf " + src + "/" + name + " " + dst + "/" + name)
                if (!r.lowercase().contains("error")) {
                    copied++
                    logCallback("[OK] " + name)
                } else {
                    logCallback("[WARN] " + r)
                }
            }
            logCallback("[DONE] " + copied.toString() + "/" + files.size.toString())
            TransferResult(true, copied)
        } catch (ex: Exception) {
            logCallback("[ERRO] " + ex.message.orEmpty())
            TransferResult(false, 0, ex.message.orEmpty())
        }
    }

    private fun exec(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            p.waitFor()
            if (out.isNotBlank()) out else err
        } catch (ex: Exception) {
            "ERR " + ex.message.orEmpty()
        }
    }
}
