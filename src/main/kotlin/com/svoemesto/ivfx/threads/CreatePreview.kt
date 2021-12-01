package com.svoemesto.ivfx.threads

import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import java.util.concurrent.TimeUnit

class CreatePreview(var fileExt: FileExt,
                    val fileController: FileController,
                    val table: TableView<FileExt>,
                    val textLbl1: String,
                    val numCurrentThread: Int,
                    val countThreads: Int,
                    var lbl1: Label, var pb1: ProgressBar,
                    var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {
    override fun run() {

        lbl1.isVisible = true
        lbl2.isVisible = true
        pb1.isVisible = true
        pb2.isVisible = true

        val fileInput = fileExt.file.path
        val fileOutput = fileController.getPreview(fileExt.file, true)

        var ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
        var ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)

        val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)

        val countFrames = fFmpegProbeResult.streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }
            .firstOrNull()?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()

        val w = 720
        val h = 400

        val fileWidth: Int = fFmpegProbeResult.streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }.firstOrNull()?.width!!
        val fileHeight: Int = fFmpegProbeResult.streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }.firstOrNull()?.height!!
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
            .setVideoResolution(w,h)
            .setVideoBitRate(500000)
            .setVideoCodec("libx264")
            .setAudioCodec("aac")
            .setAudioBitRate(196608)
            .setAudioSampleRate(48000)
            .setAudioChannels(2)
            .setVideoFilter(filter)
            .done()

        var executor = FFmpegExecutor(ffmpeg, ffprobe)

        val job = executor.createJob(builder, object : ProgressListener {
            // Using the FFmpegProbeResult determine the duration of the input
            val duration_ns: Double = fFmpegProbeResult.getFormat().duration * TimeUnit.SECONDS.toNanos(1)

            override fun progress(progress: Progress) {
                val percentage2: Double = progress.out_time_ns / duration_ns
                val textLbl2 = java.lang.String.format(
                    "[%.0f%%] status: %s, frame: %d, time: %s ms, fps: %.0f, speed: %.2fx",
                    percentage2 * 100,
                    progress.status,
                    progress.frame,
                    FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                    progress.fps.toDouble(),
                    progress.speed
                )
                val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
                val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
                val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
                Platform.runLater {
                    lbl1.text = textLbl1
                    pb1.progress = percentage1
                    lbl2.text = textLbl2
                    pb2.progress = percentage2
                }

            }
        })

        job.run()

        fileExt.hasPreview = true
        fileExt.hasPreviewString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}