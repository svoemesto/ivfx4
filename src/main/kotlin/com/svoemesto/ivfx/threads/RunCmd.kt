package com.svoemesto.ivfx.threads

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File as IOFile

class RunCmd(private val cmdText: String): Thread(), Runnable {
    override fun run() {
        val cmdFile = IOFile.createTempFile("ivfx", ".cmd")
        val writer = BufferedWriter(FileWriter(cmdFile))
        writer.write(cmdText)
        writer.flush()
        writer.close()
        val process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $cmdFile")
        process.waitFor()
        cmdFile.deleteOnExit()
    }
}