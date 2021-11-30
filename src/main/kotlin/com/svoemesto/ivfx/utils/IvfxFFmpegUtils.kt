package com.svoemesto.ivfx.utils

import net.bramp.ffmpeg.FFmpeg

class FFmpegUtils {
    companion object {
        val FFMPEG_PATH = FFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffmpeg.exe").path
        val FFPROBE_PATH = FFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffprobe.exe").path
        val FFPLAY_PATH = FFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffplay.exe").path
    }
}

fun main() {

    println(FFmpegUtils.FFMPEG_PATH)

}