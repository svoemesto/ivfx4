package com.svoemesto.ivfx.threads

import com.google.common.io.ByteStreams.copy
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile


class RunCmd(private val cmdText: String): Thread(), Runnable {
    override fun run() {
        this.name = "RunCmd"
        val cmdFile = IOFile.createTempFile("ivfx", ".cmd")
        val writer = BufferedWriter(FileWriter(cmdFile))
        writer.write(cmdText)
        writer.flush()
        writer.close()

        exec(cmdFile.absolutePath)

//        val process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $cmdFile")
//        process.outputStream
//        process.waitFor()

//        val command = "cmd /c ${cmdFile.absolutePath}"
//        val p = Runtime.getRuntime().exec(command)
//        p.waitFor()

        cmdFile.deleteOnExit()
    }

    @Throws(InterruptedException::class, IOException::class)
    private fun exec(cmd: String): Int {
        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        val p = pb.start()
        p.outputStream.close()
        val baos = ByteArrayOutputStream()
        copy(p.inputStream, baos)
        val r = p.waitFor()
        if (r != 0) println(cmd + " cmd: output:\n" + baos)
        return r
    }
}