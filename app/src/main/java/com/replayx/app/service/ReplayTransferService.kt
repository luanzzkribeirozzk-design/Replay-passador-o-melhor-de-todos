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

            val checkResult = runCmd("ls $sourcePath 2>&1")
            logCallback("[LS  ] " + checkResult.trim().take(80))

            if (checkResult.contains("No such file") || checkResult.contains("cannot access")) {
                return TransferResult(false, 0, "Pasta MReplays nao encontrada em $sourcePackage")
            }

            val files = checkResult.trim().split("
").filter { line -> line.isNotBlank() }
            logCallback("[INFO] " + files.size + " arquivo(s) encontrado(s)")

            if (files.isEmpty()) {
                return TransferResult(false, 0, "Nenhum replay encontrado")
            }

            logCallback("[MKDIR] Criando pasta destino...")
            runCmd("mkdir -p $destPath")

            var copied = 0
            var index = 0
            for (name in files) {
                if (name.isBlank()) continue
                index++
                logCallback("[COPY] [$index/${files.size}] $name")
                val result = runCmd("cp -rf $sourcePath/$name $destPath/$name 2>&1")
                if (result.isBlank() || !result.lowercase().contains("error")) {
                    copied++
                    logCallback("[OK  ] $name")
                } else {
                    logCallback("[WARN] $name -> $result")
                }
            }

            logCallback("[DONE] $copied/${files.size} copiado(s)")
            TransferResult(true, copied)

        } catch (e: Exception) {
            logCallback("[EXCEPTION] " + e.message)
            TransferResult(false, 0, e.message ?: "Erro desconhecido")
        }
    }

    private fun runCmd(command: String): String {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcess = shizukuClass.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val args = arrayOf("sh", "-c", command)
            val process = newProcess.invoke(null, args, null, null) as Process
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (out.isNotBlank()) out else err
        } catch (e: Exception) {
            "ERRO: " + e.message
        }
    }
}
