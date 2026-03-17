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

            logCallback("[SCAN] Verificando origem...")
            logCallback("[PATH] " + src)

            val check = shell("ls " + src)
            logCallback("[LS  ] " + check.take(80))

            if (check.contains("No such file") || check.contains("cannot access")) {
                return TransferResult(false, 0, "Pasta nao encontrada em " + sourcePackage)
            }

            val files = check.trim().split("
").filter { it.isNotBlank() }
            logCallback("[INFO] " + files.size + " arquivo(s)")

            if (files.isEmpty()) {
                return TransferResult(false, 0, "Nenhum replay encontrado")
            }

            shell("mkdir -p " + dst)
            logCallback("[MKDIR] Destino criado")

            var copied = 0
            for (i in files.indices) {
                val name = files[i]
                if (name.isBlank()) continue
                logCallback("[COPY] [" + (i + 1) + "/" + files.size + "] " + name)
                val r = shell("cp -rf " + src + "/" + name + " " + dst + "/" + name)
                if (!r.lowercase().contains("error")) {
                    copied++
                    logCallback("[OK] " + name)
                } else {
                    logCallback("[WARN] " + name + " -> " + r)
                }
            }

            logCallback("[DONE] " + copied + "/" + files.size + " copiado(s)")
            TransferResult(true, copied)

        } catch (ex: Exception) {
            logCallback("[ERRO] " + ex.message)
            TransferResult(false, 0, ex.message ?: "Erro")
        }
    }

    private fun shell(cmd: String): String {
        return try {
            val c = Class.forName("rikka.shizuku.Shizuku")
            val m = c.getMethod("newProcess",
                Class.forName("[Ljava.lang.String;"),
                Class.forName("[Ljava.lang.String;"),
                String::class.java)
            val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val o = p.inputStream.bufferedReader().readText()
            val e = p.errorStream.bufferedReader().readText()
            p.waitFor()
            if (o.isNotBlank()) o else e
        } catch (ex: Exception) {
            "ERR:" + ex.message
        }
    }
}
