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
         * Раньше здесь жёстко были прописаны бинарники с расширением .exe в
         * папке ffmpeg-shared из ресурсов, взятые через `getResource().path`.
         * Это ломалось дважды: `.exe` на Linux не запустить, а
         * `getResource().path` при запуске из jar-а отдаёт строку
         * `file:/...jar!/...`, которую ProcessBuilder принять не может.
         *
         * Папка ffmpeg-shared удалена из проекта 2026-10-07: 72 МБ мёртвых
         * Windows-бинарников. Осталась система: сначала утилита из PATH,
         * затем исполняемый файл рядом с приложением.
         */
        val FFMPEG_PATH: String = resolveBinary("ffmpeg")
        val FFPROBE_PATH: String = resolveBinary("ffprobe")
        val FFPLAY_PATH: String = resolveBinary("ffplay")

        /**
         * Ищет исполняемый файл: системный, затем рядом с приложением.
         *
         * Если не найдено ни того, ни другого, возвращается само имя —
         * тогда запуском займётся система, и ошибку сообщит ProcessBuilder
         * с понятным текстом, а не молчаливый отказ здесь.
         *
         * @param name имя утилиты
         * @return путь к утилите либо её имя
         */
        private fun resolveBinary(name: String): String {
            val fromPath = System.getenv("PATH").orEmpty()
                .split(File.pathSeparator)
                .map { File(it, name) }
                .firstOrNull { it.canExecute() }
            if (fromPath != null) {
                return fromPath.absolutePath
            }
            val nearby = File(name).takeIf { it.canExecute() }
            return nearby?.absolutePath ?: name
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