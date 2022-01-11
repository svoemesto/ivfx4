package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.enums.AudioCodecs
import com.svoemesto.ivfx.enums.LosslessContainers
import com.svoemesto.ivfx.enums.LosslessVideoCodecs
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListShotsExt
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

class CreateShotsLosslessWithAudio(var fileExt: FileExt,
                                   val table: TableView<FileExt>,
                                   val textLbl1: String,
                                   val numCurrentThread: Int,
                                   val countThreads: Int,
                                   var lbl1: Label, var pb1: ProgressBar,
                                   var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        this.name = "CreateShotsLosslessWithAudio"

        lbl1.isVisible = true
        lbl2.isVisible = true
        pb1.isVisible = true
        pb2.isVisible = true

        if (fileExt.framesExt.isEmpty()) LoadListFramesExt(fileExt.framesExt, fileExt, null, null).run()
        if (fileExt.shotsExt.isEmpty()) LoadListShotsExt(fileExt.shotsExt, fileExt, null, null).run()

        val fileInput = fileExt.pathToLosslessFile
        if (!File(fileExt.folderShotsLosslessWithAudio).exists()) File(fileExt.folderShotsLosslessWithAudio).mkdir()

        val frameRate: Double = fileExt.fps
        val ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
        val ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)
        val w = fileExt.file.project.width
        val h = fileExt.file.project.height

        val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)
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

        for ((i, shotExt) in fileExt.shotsExt.withIndex()) {

            val fileOutput = shotExt.pathToLosslessWithAudio

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((i+1)/fileExt.shotsExt.size.toDouble() )
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Creating shot video file [$i/${fileExt.shotsExt.size}]: $fileOutput"
                pb2.progress = percentage2
            }

            if (!File(fileOutput).exists()) {
                val firstFrame: Int = shotExt.shot.firstFrameNumber
                val lastFrame: Int = shotExt.shot.lastFrameNumber
                val framesToCode = lastFrame - firstFrame + 1
                val start = (IvfxFFmpegUtils.getDurationByFrameNumber(firstFrame - 1, frameRate).toDouble() / 1000).toString()

                val builderOutput = FFmpegOutputBuilder()

                val builder = FFmpegBuilder()
                if (firstFrame != 0) builder.addExtraArgs("-ss", start)
                builder.setInput(fileInput)
                builder.overrideOutputFiles(true)
                builder.addOutput(builderOutput)

                builderOutput.setFilename(fileOutput)
                builderOutput.addExtraArgs("-map", "0:v:0")

                fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track ->
                    var typeOrder = PropertyController.getOrCreate(track::class.java.simpleName, track.id, "@typeorder")
                    if (typeOrder == "") typeOrder = "1"
                    builderOutput.addExtraArgs("-map", "0:a:${(typeOrder.toInt())-1}")
                }

                builderOutput.addExtraArgs("-vframes", framesToCode.toString())
                builderOutput.setVideoResolution(w,h)
                builderOutput.setVideoCodec(LosslessVideoCodecs.DNX.codec)
                builderOutput.addExtraArgs("-b:v","36M")
                builderOutput.setAudioCodec(AudioCodecs.PMC.codec)
                builderOutput.setAudioSampleRate(48000)
//            builderOutput.setVideoFilter(filter)

                val executor = FFmpegExecutor(ffmpeg)

                val job = executor.createJob(builder)
                job.run()
            }


        }

        fileExt.hasShotsLosslessWithAudio = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}