package com.svoemesto.ivfx.threads.projectactions

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
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import java.util.concurrent.TimeUnit
import java.io.File as IOFile

class CreateFramesSmall(var fileExt: FileExt,
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
        if (!IOFile(fileExt.folderFramesSmall).exists()) IOFile(fileExt.folderFramesSmall).mkdir()
        val fileOutput = fileExt.folderFramesSmall  + IOFile.separator + fileExt.file.shortName + "_frame_%06d.jpg"
//        val fileOutput = Main.fileController.getCdfFolder(fileExt.file, Folders.FRAMES_SMALL,  true) + IOFile.separator +
//                fileExt.file.shortName + "_frame_%06d.jpg"

        val ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
        val ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)

        val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)

        val countFrames = fFmpegProbeResult.streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()

        val w = 135
        val h = 75

        val builder = FFmpegBuilder()
            .setInput(fileInput)
            .overrideOutputFiles(true)
            .addOutput(fileOutput)
            .setFrames(countFrames?:1)
            .addExtraArgs("-qscale:v","1")
            .setVideoResolution(w,h)
            .done()

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

        fileExt.hasFramesSmall = true
//        fileExt.hasFramesSmallString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}