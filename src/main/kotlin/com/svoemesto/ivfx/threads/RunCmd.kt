package com.svoemesto.ivfx.threads

import com.google.common.io.ByteStreams.copy
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.io.File as IOFile

class RunCmd(
    private val cmdText: String,
) : Thread(),
    Runnable {
    override fun run() {
        this.name = "RunCmd"
        val cmdFile = IOFile.createTempFile("ivfx", ".cmd")
        val writer = BufferedWriter(FileWriter(cmdFile))
        writer.write(cmdText)
        writer.flush()
        writer.close()

        val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        val shell = if (isWindows) listOf("cmd", "/c", cmdFile.absolutePath)
        else listOf("/bin/sh", cmdFile.absolutePath)

        exec(shell)

//        val process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $cmdFile")
//        process.outputStream
//        process.waitFor()

//        val command = "cmd /c ${cmdFile.absolutePath}"
//        val p = Runtime.getRuntime().exec(command)
//        p.waitFor()

        cmdFile.deleteOnExit()
    }

    @Throws(InterruptedException::class, IOException::class)
    private fun exec(command: List<String>): Int {
        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(true)
        val p = pb.start()
        p.outputStream.close()
        val baos = ByteArrayOutputStream()
        // Вывод процесса читает отдельный поток.
        //
        // Раньше здесь стоял copy(p.inputStream, baos) прямо в потоке
        // операции. Чтение из канала блокируется и **не реагирует на
        // прерывание**: поток можно было прервать сколько угодно раз, а
        // скрипт продолжал работать. На операциях DetectFaces и
        // RecognizeFaces вызывается runCmd.run() — то есть синхронно, в
        // потоке самой операции, — и поток висел именно здесь.
        val reader =
            Thread {
                try {
                    copy(p.inputStream, baos)
                } catch (e: IOException) {
                    // Процесс убит, канал закрылся — читать больше нечего.
                }
            }
        reader.name = "RunCmd-output"
        reader.isDaemon = true
        reader.start()

        // Ожидание опросом, а не одним waitFor(): waitFor() бросает
        // InterruptedException, и его можно поймать, тогда как copy()
        // прерывание проглатывал молча.
        var interrupted = false
        var exitCode = -1
        while (true) {
            try {
                if (p.waitFor(200, TimeUnit.MILLISECONDS)) {
                    exitCode = p.exitValue()
                    break
                }
            } catch (e: InterruptedException) {
                interrupted = true
                break
            }
            if (Thread.currentThread().isInterrupted) {
                interrupted = true
                break
            }
        }
        if (interrupted) {
            // Убивать нужно дерево, а не только оболочку.
            //
            // RunCmd запускает `/bin/sh` с файлом команды, а python идёт его
            // потомком. destroy() убивает только оболочку, и скрипт
            // остаётся жить: его переподчиняет systemd, и он продолжает
            // писать на диск. Проверено на живом прогоне — после прерывания
            // процесс detect_faces_in_folder.py жил с ppid = systemd --user.
            //
            // Потомки собираются ДО убийства: после смерти оболочки дерево
            // рассыпается и найти его уже нельзя.
            val descendants = p.toHandle().descendants().toList()
            descendants.forEach { it.destroy() }
            p.destroy()
            if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly()
            descendants.forEach { if (it.isAlive) it.destroyForcibly() }
            reader.join(1000)
            // Флаг прерывания возвращается: иначе вызывающий не увидит, что
            // операция была прервана, и запишет её как успешную.
            Thread.currentThread().interrupt()
            return -1
        }
        reader.join(2000)
        if (exitCode != 0) println(command.joinToString(" ") + " cmd: output:\n" + baos)
        return exitCode
    }
}