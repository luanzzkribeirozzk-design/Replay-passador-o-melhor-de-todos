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
            val sourcePath = "/sdcard/Android/data/$sourcePackage/files/MReplays"
            val destPath = "/sdcard/Android/data/$destPackage/files/MReplays"

            logCallback("[SCAN] Verificando origem...")
            logCallback("[PATH] $sourcePath")

            val checkResult = execCmd("ls "$sourcePath" 2>&1")
            logCallback("[LS  ] ${checkResult.trim().take(80)}")

            if (checkResult.contains("No such file") || checkResult.contains("cannot access")) {
                return TransferResult(false, 0, "Pasta MReplays nao encontrada em $sourcePackage")
            }

            val files = checkResult.trim().split("
").filter { it.isNotBlank() }
            logCallback("[INFO] ${files.size} item(ns) encontrado(s)")

            if (files.isEmpty()) {
                return TransferResult(false, 0, "Nenhum replay encontrado")
            }

            logCallback("[MKDIR] Criando pasta destino...")
            execCmd("mkdir -p "$destPath"")

            var copied = 0
            files.forEachIndexed { i, name ->
                if (name.isBlank()) return@forEachIndexed
                logCallback("[COPY] [${i + 1}/${files.size}] $name")
                val r = execCmd("cp -rf "$sourcePath/$name" "$destPath/$name" 2>&1")
                if (r.isBlank() || !r.lowercase().contains("error")) {
                    copied++
                    logCallback("[OK  ] $name")
                } else {
                    logCallback("[WARN] $name -> $r")
                }
            }

            logCallback("[DONE] $copied/${files.size} copiado(s)")
            TransferResult(true, copied)

        } catch (e: Exception) {
            logCallback("[EXCEPTION] ${e.message}")
            TransferResult(false, 0, e.message ?: "Erro desconhecido")
        }
    }

    private fun execCmd(command: String): String {
        return try {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            val method = cls.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            process.waitFor()
            stdout.ifBlank { stderr }
        } catch (e: Exception) {
            "ERRO_CMD: ${e.message}"
        }
    }
}
