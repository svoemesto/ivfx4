package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.enums.VideoContainers
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit

class CreateFilterResult(var filterExt: FilterExt, var projectExt: ProjectExt, var shotsExt: MutableList<ShotExt>, var fileExt: FileExt, var filename: String = ""): Thread(), Runnable {
    override fun run() {

        if (!File(projectExt.folderFilters).exists()) File(projectExt.folderFilters).mkdir()

        var concatFiles = ""
        shotsExt.forEach { concatFiles += "file '${it.pathToCompressedWithAudio}'\n" }

        if (filename == "") filename = "${projectExt.folderFilters}${File.separator}${filterExt.filter.name} [${shotsExt.first().fileExt.file.shortName}-${shotsExt.last().fileExt.file.shortName}].${VideoContainers.valueOf(projectExt.project.container).extention}"
        val fileInput = "${filename}.txt"
        if (File(fileInput).exists()) File(fileInput).delete()
        try {
            val listFilesToConcat = File(fileInput)
            val writer = BufferedWriter(FileWriter(listFilesToConcat))
            writer.write(concatFiles)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val fileOutput = filename

        val ffmpeg = FFmpeg(IvfxFFmpegUtils.FFMPEG_PATH)

        val builderOutput = FFmpegOutputBuilder()
        builderOutput.setFilename(fileOutput)

        val builder = FFmpegBuilder()
            .setInput(fileInput)
            .addExtraArgs("-f", "concat")
            .addExtraArgs("-safe", "0")
            .overrideOutputFiles(true)
            .addOutput(builderOutput)

        builderOutput.addExtraArgs("-map", "0:v:0")
        fileExt.file.tracks.filter { it.type == "Audio" && it.use }.forEach { track ->
            var typeOrder = PropertyController.getOrCreate(track::class.java.simpleName, track.id, "@typeorder")
            if (typeOrder == "") typeOrder = "1"
            builderOutput.addExtraArgs("-map", "0:a:${(typeOrder.toInt())-1}")
        }
        builderOutput.addExtraArgs("-c", "copy")

        val executor = FFmpegExecutor(ffmpeg)

        val job = executor.createJob(builder)
        job.run()
        if (File(fileInput).exists()) File(fileInput).delete()

    }
}