package com.svoemesto.ivfx.utils

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import java.util.concurrent.TimeUnit


class IvfxFFmpegUtils {
    companion object {
        val FFMPEG_PATH = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffmpeg.exe").path
        val FFPROBE_PATH = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffprobe.exe").path
        val FFPLAY_PATH = IvfxFFmpegUtils::class.java.getResource("ffmpeg-shared/bin/ffplay.exe").path
    }
}

fun main() {

    val fileInput = "E:/GOT/GOT.S01/GOT.S01E01.BDRip.1080p.mkv"
//    val fileOutput = "F:/ivfxGOT/TestOutput/GOT.S1.E1.%06d.jpg"
    val fileOutput = "F:/ivfxGOT/TestOutput/GOT.S1.E1.mp4"



    var ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
    var ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)

    val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)
    val ffmpegFormat = fFmpegProbeResult.getFormat()
    val ffmpegStream = fFmpegProbeResult.getStreams().get(0)


//    var builder = FFmpegBuilder()
//        .setInput(fileInput)
//        .overrideOutputFiles(true)
//        .addOutput(fileOutput)
////        .setFrames(88643)
//        .setFrames(10000)
//            .setVideoResolution(135,75)
//        .done()
    val w = 720
    val h = 400

    val fileWidth: Int = 1920
    val fileHeight: Int = 1080
    val fileAspect = fileWidth.toDouble() / fileHeight.toDouble()
    val frameAspect = w.toDouble() / h.toDouble()
    var filter = ""

    filter = if (fileAspect > frameAspect) {
        val frameHeight = (w.toDouble() / fileAspect).toInt()
        "\"scale=" + w + ":" + frameHeight + ",pad=" + w + ":" + h + ":0:" + ((h - frameHeight) / 2.0).toInt() + ":black\""
    } else {
        val frameWidth = (h.toDouble() * fileAspect).toInt()
        "\"scale=" + frameWidth + ":" + h + ",pad=" + w + ":" + h + ":" + ((w - frameWidth) / 2.0).toInt() + ":0:black\""
    }

    var builder = FFmpegBuilder()
        .setInput(fileInput)
        .overrideOutputFiles(true)
        .addOutput(fileOutput)
        .setVideoResolution(720,400)
        .setVideoBitRate(500000)
        .setVideoCodec("libx264")
        .setAudioCodec("aac")
        .setAudioBitRate(128492)
        .setAudioSampleRate(48000)
        .setAudioChannels(2)
        .setVideoFilter(filter)
        .done()

    var executor = FFmpegExecutor(ffmpeg, ffprobe)
//    executor.createJob(builder).run()


    val job = executor.createJob(builder, object : ProgressListener {
        // Using the FFmpegProbeResult determine the duration of the input
        val duration_ns: Double = fFmpegProbeResult.getFormat().duration * TimeUnit.SECONDS.toNanos(1)
        override fun progress(progress: Progress) {
            val percentage: Double = progress.out_time_ns / duration_ns
            // Print out interesting information about the progress
            println(
                java.lang.String.format(
                    "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
                    percentage * 100,
                    progress.status,
                    progress.frame,
                    FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                    progress.fps.toDouble(),
                    progress.speed
                )
            )
        }
    })

    job.run()

}