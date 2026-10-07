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
import net.bramp.ffmpeg.shared.CodecType
import net.bramp.ffmpeg.probe.FFmpegStream
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener
import java.util.concurrent.TimeUnit
import java.io.File as IOFile

class CreateFramesFull(var fileExt: FileExt,
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
        if (!IOFile(fileExt.folderFramesFull).exists()) IOFile(fileExt.folderFramesFull).mkdir()
        val fileOutput = fileExt.folderFramesFull + IOFile.separator + fileExt.file.shortName + "_frame_%06d.jpg"
//        val fileOutput = Main.fileController.getCdfFolder(fileExt.file, Folders.FRAMES_FULL,  true) + IOFile.separator +
//                fileExt.file.shortName + "_frame_%06d.jpg"

        val ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)
        val ffprobe = FFprobe(IvfxFFmpegUtils.FFPROBE_PATH)

        val fFmpegProbeResult: FFmpegProbeResult = ffprobe.probe(fileInput)

        val countFrames = fFmpegProbeResult.streams.firstOrNull { it.codec_type == CodecType.VIDEO }?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()

        val w = 1920
        val h = 1080

        val fileWidth: Int = fFmpegProbeResult.streams.firstOrNull { it.codec_type == CodecType.VIDEO }?.width!!
        val fileHeight: Int = fFmpegProbeResult.streams.firstOrNull { it.codec_type == CodecType.VIDEO }?.height!!
        val fileAspect = fileWidth.toDouble() / fileHeight.toDouble()
        val frameAspect = w.toDouble() / h.toDouble()

        val filter: String = if (fileAspect > frameAspect) {
            val frameHeight = ((w.toDouble() / fileAspect).toInt() / 2) * 2
            "scale=" + w + ":" + frameHeight + ",pad=" + w + ":" + h + ":0:" + ((h - frameHeight) / 2.0).toInt() + ":black"
        } else {
            val frameWidth = ((h.toDouble() * fileAspect).toInt() / 2) * 2
            "scale=" + frameWidth + ":" + h + ",pad=" + w + ":" + h + ":" + ((w - frameWidth) / 2.0).toInt() + ":0:black"
        }


        val builder = FFmpegBuilder()
        builder.setInput(fileInput)
        builder.overrideOutputFiles(true)
        val builderOutput = builder.addOutput(fileOutput)
        builderOutput.setFrames(countFrames?:1)
        builderOutput.addExtraArgs("-qscale:v","2")
        builderOutput.setVideoResolution(w,h)
                    // Обёртка добавляет -vframes 1, увидев расширение .jpg на
                    // выходе, и последовательность %06d схлопывается в один файл.
                    // Последний -vframes в команде выигрывает, поэтому свой
                    // добавляем после обёртки, с числом кадров файла.
                    .addExtraArgs("-vframes", MAX_FRAMES_TO_WRITE)
            .setVideoFilter(filter)
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

        fileExt.hasFramesFull = true
//        fileExt.hasFramesFullString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }

    /**
     * Число кадров для записи.
     *
     * Поле `framesCount` у файла на этом шаге ещё не заполнено, оно
     * равно нулю, а ноль в -vframes означает «не писать ничего». Число
     * берётся из результата ffprobe: длительность в микросекундах,
     * делённая на частоту кадров, с запасом в пять процентов — чтобы
     * обрезка не отсекла последний кадр из-за округления.
     *
     * @param durationMicros длительность файла в микросекундах
     * @param fps частота кадров
     * @return сколько кадров записывать
     */


    companion object {
        /**
         * Потолок числа записываемых кадров.
         *
         * Обёртка bramp добавляет `-vframes 1`, увидев расширение .jpg на
         * выходе, и последовательность с шаблоном %06d схлопывается в один
         * файл. Свой `-vframes` добавляется после обёртки, и последний
         * выигрывает. Точное число кадров считать не от чего: у файла поле
         * framesCount на этом шаге ещё не заполнено, а единицы длительности
         * у ffprobe и у обёртки разные. Поэтому ограничение просто снимается.
         * Сто миллионов кадров с запасом перекрывают любой реальный файл,
         * а при обрыве по диску или по кнопке «Стоп» ffmpeg остановится сам.
         */
        const val MAX_FRAMES_TO_WRITE = "100000000"
    }

}