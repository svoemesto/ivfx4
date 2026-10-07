package com.svoemesto.ivfx.utils

import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import java.io.File
import java.io.InputStreamReader

/**
 * Путь к ресурсу, лежащему рядом с классом, в виде пути по файловой системе.
 *
 * Раньше здесь везде стояло `getResource(...)!!.file.substring(1)` — приём,
 * который работает только на Windows: там `URL.file` даёт `C:\...`, и первый
 * символ отбрасывается, чтобы убрать ведущую букву диска. На Linux
 * `URL.file` даёт `/home/...`, и тот же substring(1) срезает ведущий слэш,
 * превращая абсолютный путь в относительный: `home/nsa/...`. Такой файл не
 * находится, и `ImageIO.read` падает с «Can't read input file!».
 *
 * @param clazz класс, рядом с которым лежит ресурс
 * @param resourceName имя ресурса
 * @return абсолютный путь к файлу ресурса
 * @throws IllegalStateException если ресурса нет в classpath или он внутри банка
 */
fun getResourceFilePath(clazz: Class<*>, resourceName: String): String {
    val url = clazz.getResource(resourceName)
        ?: throw IllegalStateException("Ресурс не найден в classpath: $resourceName")
    if (url.protocol != "file") {
        throw IllegalStateException(
            "Ресурс $resourceName лежит внутри банка ($url), его нельзя открыть " +
                "по файловому пути. Приложение запускается из каталога target/classes."
        )
    }
    return File(url.toURI()).absolutePath
}

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
//    val exePath = IvfxFFmpegUtils.FFPROBE_PATH
//    val mediaFile = "E:/GOT/GOT.S01/GOT.S01E01.BDRip.1080p.mkv"
//
//    val ffprobe = FFprobe(exePath)
//    val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(mediaFile)
//
//    println(fFmpegProbeResult)

    val snils = "10091645013"
    println("${snils.substring(0,3)}-${snils.substring(3,6)}-${snils.substring(6,9)} ${snils.substring(9,11)}")
    println("${snils.subSequence(0,3)}-${snils.subSequence(3,6)}-${snils.subSequence(6,9)} ${snils.subSequence(9,11)}")

}