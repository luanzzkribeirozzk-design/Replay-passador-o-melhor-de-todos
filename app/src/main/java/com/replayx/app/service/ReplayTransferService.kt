package com.replayx.app.service

import rikka.shizuku.Shizuku

data class TransferResult(val success: Boolean, val filesCopied: Int = 0, val errorMessage: String = "")

class ReplayTransferService {
    fun transferReplays(sourcePackage: String, destPackage: String, logCallback: (String) -> Unit): TransferResult {
        return try {
            val sourcePath = "/sdcard/Android/data/$sourcePackage/files/MReplays"
            val destPath = "/sdcard/Android/data/$destPackage/files/MReplays"
            logCallback("[SCAN] Verificando pasta origem...")
            logCallback("[PATH] $sourcePath")
            val checkResult = runShizukuCommand("ls \"\$sourcePath\" 2>&1")
            logCallback("[CMD] -> ${checkResult.trim()}")
            if (checkResult.contains("No such file")) {
                return TransferResult(false, 0, "Pasta MReplays nao encontrada em $sourcePackage")
            }
            val listResult = runShizukuCommand("ls \"\$sourcePath\"")
            val files = listResult.trim().split("\n").filter { it.isNotBlank() }
            logCallback("[SCAN] ${files.size} arquivo(s) encontrado(s)")
            if (files.isEmpty()) return TransferResult(false, 0, "Nenhum replay encontrado")
            logCallback("[MKDIR] Criando pasta destino...")
            runShizukuCommand("mkdir -p \"\$destPath\"")
            var copied = 0
            files.forEachIndexed { i, fileName ->
                if (fileName.isBlank()) return@forEachIndexed
                logCallback("[COPY] [${i+1}/${files.size}] $fileName")
                val r = runShizukuCommand("cp -r \"\$sourcePath/\$fileName\" \"\$destPath/\$fileName\"")
                if (!r.contains("error", true)) { copied++; logCallback("[OK] $fileName") }
                else logCallback("[WARN] $fileName: $r")
            }
            logCallback("[DONE] $copied/${files.size} copiado(s)")
            TransferResult(true, copied)
        } catch (e: Exception) {
            logCallback("[EXCEPTION] ${e.message}")
            TransferResult(false, 0, e.message ?: "Erro desconhecido")
        }
    }

    private fun runShizukuCommand(command: String): String {
        return try {
            val p = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            p.waitFor()
            if (out.isNotBlank()) out else err
        } catch (e: Exception) { "ERRO: ${e.message}" }
    }
}
