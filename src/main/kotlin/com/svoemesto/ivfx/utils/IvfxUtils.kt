package com.svoemesto.ivfx.utils

import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.InputStreamReader




//@Throws(IOException::class, InterruptedException::class)
fun executeExe(exePath: String, parameters: List<String>): String {

    val param: MutableList<String> = ArrayList()
    param.add(exePath)
    param.addAll(parameters)

    val builder = ProcessBuilder(param)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val buffer = StringBuilder()
    InputStreamReader(process.inputStream).use { reader ->
        var i: Int
        while (reader.read().also { i = it } != -1) {
            buffer.append(i.toChar())
        }
    }
    process.waitFor()
    val out = buffer.toString()
    return out.substring(0, out.length - 2)

}

fun main() {
    val exePath = IvfxFFmpegUtils.FFPROBE_PATH
    val mediaFile = "E:/GOT/GOT.S01/GOT.S01E01.BDRip.1080p.mkv"

    val ffprobe = FFprobe(exePath)
    val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(mediaFile)

    println(fFmpegProbeResult)
}