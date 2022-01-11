package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.enums.AudioCodecs
import com.svoemesto.ivfx.enums.LosslessContainers
import com.svoemesto.ivfx.enums.LosslessVideoCodecs
import com.svoemesto.ivfx.modelsext.FileExt
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
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import java.io.File
import java.util.concurrent.TimeUnit

class CreateLossless(var fileExt: FileExt,
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
        if (!File(fileExt.folderLossless).exists()) File(fileExt.folderLossless).mkdir()
        val fileOutput = fileExt.pathToLosslessFile

        val ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
        val ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)

        val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)

        val w = fileExt.file.project.width
        val h = fileExt.file.project.height

        val fileWidth: Int = fFmpegProbeResult.streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.width!!
        val fileHeight: Int = fFmpegProbeResult.streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.height!!
        val fileAspect = fileWidth.toDouble() / fileHeight.toDouble()
        val frameAspect = w.toDouble() / h.toDouble()

        val filter: String = if (fileAspect > frameAspect) {
            val frameHeight = (w.toDouble() / fileAspect).toInt()
            "\"scale=" + w + ":" + frameHeight + ",pad=" + w + ":" + h + ":0:" + ((h - frameHeight) / 2.0).toInt() + ":black\""
        } else {
            val frameWidth = (h.toDouble() * fileAspect).toInt()
            "\"scale=" + frameWidth + ":" + h + ",pad=" + w + ":" + h + ":" + ((w - frameWidth) / 2.0).toInt() + ":0:black\""
        }

        val builderOutput = FFmpegOutputBuilder()

        val builder = FFmpegBuilder()
            .setInput(fileInput)
            .overrideOutputFiles(true)
            .addOutput(builderOutput)

        builderOutput.setFilename(fileOutput)
        builderOutput.addExtraArgs("-map", "0:v:0")

        fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track ->
            var typeOrder = PropertyController.getOrCreate(track::class.java.simpleName, track.id, "@typeorder")
            if (typeOrder == "") typeOrder = "1"
            builderOutput.addExtraArgs("-map", "0:a:${(typeOrder.toInt())-1}")
        }

        builderOutput.setVideoResolution(w,h)

        if (fileExt.file.project.lossLessContainer == LosslessContainers.MXF.name) {
            builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)
                .addExtraArgs("-b:v","36M")
                .setAudioCodec(AudioCodecs.PMC.codec)
                .setAudioSampleRate(48000)
        } else {
            if (fileExt.file.project.lossLessCodec == LosslessVideoCodecs.RAW.name) {
                builderOutput.setVideoCodec(LosslessVideoCodecs.RAW.codec)
                    .addExtraArgs("-pix_fmt","yuv420p")
            } else if (fileExt.file.project.lossLessCodec == LosslessVideoCodecs.DNX.name) {
                builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)
                    .addExtraArgs("-b:v","36M")
            }
            builderOutput.setAudioCodec(AudioCodecs.PMC.codec)
//                .setAudioBitRate(320000)
                .setAudioSampleRate(48000)
        }

        builderOutput.setVideoFilter(filter)


        val executor = FFmpegExecutor(ffmpeg, ffprobe)

        val job = executor.createJob(builder, object : ProgressListener {
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

        fileExt.hasLossless = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}