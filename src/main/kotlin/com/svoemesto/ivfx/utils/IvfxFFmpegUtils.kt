package com.svoemesto.ivfx.utils

import java.io.File
import java.util.regex.Pattern
import kotlin.math.floor
import kotlin.math.roundToInt


class IvfxFFmpegUtils {
    companion object {
        /**
         * Пути к утилитам ffmpeg.
         *
         * Раньше здесь жёстко были прописаны бинарники с расширением .exe в папке ffmpeg-shared из
         * ресурсов, взятые через `getResource().path`. Это ломалось дважды:
         * `.exe` — исполняемый файл для Windows, на Linux его не запустить,
         * а `getResource().path` при запуске из jar-а отдаёт строку
         * `file:/...jar!/...`, которую ProcessBuilder принять не может.
         * Из-за этого любое действие с ffmpeg, в том числе «Create preview»,
         * падало с IOException.
         *
         * Теперь сначала ищется системная утилита, затем файл рядом с
         * приложением, и только потом встроенный ресурс — уже с корректным
         * разбором пути.
         */
        val FFMPEG_PATH: String = resolveBinary("ffmpeg", "ffmpeg-shared/bin/ffmpeg.exe")
        val FFPROBE_PATH: String = resolveBinary("ffprobe", "ffprobe-shared/bin/ffprobe.exe")
        val FFPLAY_PATH: String = resolveBinary("ffplay", "ffplay-shared/bin/ffplay.exe")

        /**
         * Ищет исполняемый файл: системный, затем рядом с приложением,
         * затем встроенный ресурс.
         *
         * @param name имя утилиты в PATH
         * @param resource путь к встроенной утилите в ресурсах
         * @return путь к утилите либо её имя, если ничего не найдено
         */
        private fun resolveBinary(name: String, resource: String): String {
            val fromPath = System.getenv("PATH").orEmpty()
                .split(File.pathSeparator)
                .map { File(it, name) }
                .firstOrNull { it.canExecute() }
            if (fromPath != null) {
                return fromPath.absolutePath
            }
            val nearby = listOf(File(name), File("ffmpeg-shared/bin/$name"))
                .firstOrNull { it.canExecute() }
            if (nearby != null) {
                return nearby.absolutePath
            }
            val bundled = IvfxFFmpegUtils::class.java.getResource(resource) ?: return name
            val file = File(bundled.toURI().path)
            return if (file.canExecute()) file.absolutePath else name
        }

        //@Throws(IOException::class, InterruptedException::class)
        fun getListIFrames(mediaFile: String, fps: Double): List<Int> {
            val exePath = IvfxFFmpegUtils.FFPROBE_PATH
            val param: MutableList<String> = mutableListOf()
            param.add("-skip_frame")
            param.add("nokey")
            param.add("-select_streams")
            param.add("v")
            param.add("-show_frames")
            param.add(mediaFile)
            val executeResult: String = executeExe(exePath, param)
            val list: MutableList<Int> = ArrayList()
            val regExp = "(?<=\\[FRAME\\]\r\n)[\\w|\\W]+?(?=\\[/FRAME\\]\r\n)"
            val pattern = Pattern.compile(regExp)
            val matcher = pattern.matcher(executeResult)
            while (matcher.find()) {
                val startPosition = matcher.start()
                val endPosition = matcher.end()
                val result = executeResult.substring(startPosition, endPosition)
                val resultLines = result.split("\\r\\n".toRegex()).toTypedArray()
                for (i in resultLines.indices) {
                    val line = resultLines[i].split("=".toRegex()).toTypedArray()
                    if (line.size > 0) {
                        if (line[0] == "pkt_pts") {
                            try {
                                val findedResult = line[1].toInt()
                                list.add(getFrameNumberByDuration(findedResult, fps))
                            } catch (_: NumberFormatException) {
                            }
                        }
                    }
                }
            }
            return list
        }

        fun getFrameNumberByDuration(duration: Int, fps: Double): Int {
            val dur1fr = 1000 / fps
            val doubleFrames = duration / dur1fr + 1
            return doubleFrames.roundToInt()
        }

        fun convertDurationToString(duration: Int): String {
            val hours = duration / 3600000
            val minutes = (duration - hours * 3600000) / 60000
            val seconds = (duration - hours * 3600000 - minutes * 60000) / 1000
            val milliseconds = duration - hours * 3600000 - minutes * 60000 - seconds * 1000
            return hours.toString() + ":" + String.format("%02d", minutes) + ":" + String.format(
                "%02d",
                seconds
            ) + "." + String.format("%03d", milliseconds)
        }

        fun getDurationByFrameNumber(frameNumber: Int, fps: Double): Int {
            val dur1fr = 1000 / fps
            val countFramesBefore = frameNumber - 0
            var durDouble = countFramesBefore * dur1fr
            if (durDouble < 0) durDouble = 0.0
            return floor(durDouble).toInt()
        }


    }
}



fun main() {

}