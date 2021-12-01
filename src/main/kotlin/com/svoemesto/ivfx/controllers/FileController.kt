package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import org.springframework.stereotype.Controller
import java.io.FilenameFilter
import java.io.IOException
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class FileController(val projectRepo: ProjectRepo,
                     val propertyRepo: PropertyRepo,
                     val propertyCdfRepo: PropertyCdfRepo,
                     val projectCdfRepo: ProjectCdfRepo,
                     val fileRepo: FileRepo,
                     val fileCdfRepo: FileCdfRepo,
                     val frameRepo: FrameRepo,
                     val trackRepo: TrackRepo) {

    class FileExt(val file: File) {
        var hasPreview: Boolean = false
        var hasPreviewString: String = ""
        var hasLossless: Boolean = false
        var hasLosslessString: String = ""
        var hasFramesSmall: Boolean = false
        var hasFramesSmallString: String = ""
        val order = file.order
        val name = file.name
    }

    fun getCdfFolder(file: File, folder: Folders, createIfNotExist: Boolean = false): String {
        val propertyValue = getPropertyValue(file, folder.propertyCdfKey)
        val projectCdfFolder = ProjectController(projectRepo,propertyRepo,propertyCdfRepo,projectCdfRepo,fileRepo,fileCdfRepo,frameRepo,trackRepo).getCdfFolder(file.project, folder, createIfNotExist)
        val fld = if (propertyValue == "") projectCdfFolder  + IOFile.separator + file.shortName else propertyValue
        try {
            if (createIfNotExist && !IOFile(fld).exists()) IOFile(fld).mkdir()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fld
    }

    fun getFFmpegProbeResult(file: File): FFmpegProbeResult {
        return FFprobe(IvfxFFmpegUtils.FFPROBE_PATH).probe(file.path)
    }

    fun hasLossless(file: File): Boolean {
        return IOFile(getLossless(file)).exists()
    }

    fun hasPreview(file: File): Boolean {
        return IOFile(getPreview(file)).exists()
    }

    fun getLossless(file: File): String {
        val folder = getCdfFolder(file, Folders.LOSSLESS)
        return if (folder == "") "" else folder + IOFile.separator + file.shortName + "_lossless.mkv"
    }

    fun getPreview(file: File): String {
        val folder = getCdfFolder(file, Folders.PREVIEW)
        return if (folder == "") "" else folder + IOFile.separator + file.shortName + "_preview.mp4"
    }

    fun hasFramesSmall(file: File): Boolean {
        val countFrames = getFFmpegProbeResult(file).streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }
            .firstOrNull()?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()
        val fld = getCdfFolder(file, Folders.FRAMES_SMALL)
        if (!IOFile(fld).exists()) {
            return false
        } else {
            return countFrames == IOFile(fld).listFiles(FilenameFilter { dir, name -> name.startsWith(file.shortName) && name.endsWith(".jpg") }).size
        }
    }

    fun getListFilesExt(project: Project): List<FileExt> {
        val list = fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toList()
        var resultedList: MutableList<FileExt> = mutableListOf()
        list.forEach { file ->
            var fileExt = FileExt(file)
            fileExt.hasPreview = hasPreview(file)
            fileExt.hasPreviewString = if (fileExt.hasPreview) "✓" else "✗"
            fileExt.hasLossless = hasLossless(file)
            fileExt.hasLosslessString = if (fileExt.hasLossless) "✓" else "✗"
            fileExt.hasFramesSmall = hasFramesSmall(file)
            fileExt.hasFramesSmallString = if (fileExt.hasFramesSmall) "✓" else "✗"
            resultedList.add(fileExt)
        }
        return resultedList
    }

    fun getListFiles(project: Project): List<File> {
        val list = fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toList()
        list.forEach { FileCdfController(fileCdfRepo).getFileCdf(it) }
        return fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toList()
    }

    fun getProperties(file: File) : List<Property> {
        return propertyRepo.findByParentClassAndParentId(file::class.simpleName!!, file.id).toList()
    }

    fun getPropertyValue(file: File, key: String) : String {
        val property = propertyRepo.findByParentClassAndParentIdAndKey(file::class.simpleName!!, file.id, key).firstOrNull()
        return if (property != null) property.value else ""
    }

    fun isPropertyPresent(file: File, key: String) : Boolean {
        return propertyRepo.findByParentClassAndParentIdAndKey(file::class.simpleName!!, file.id, key).any()
    }

    fun create(project: Project): File {
        val entity = File()
        entity.project = project
        val lastEntity = fileRepo.getEntityWithGreaterOrder(project.id).firstOrNull()
        entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
        entity.name = "New file ${entity.order} to project ${project.id}"
        fileRepo.save(entity)
        return entity
    }

    // удаление файла
    fun delete(file: File) {
        reOrder(ReorderTypes.MOVE_TO_LAST, file)
        propertyRepo.deleteAll(file::class.java.simpleName, file.id)
        propertyCdfRepo.deleteAll(file::class.java.simpleName, file.id)
        fileCdfRepo.deleteAll(file.id)
        file.frames.forEach{ frame ->
            propertyRepo.deleteAll(frame::class.java.simpleName, frame.id)
            propertyCdfRepo.deleteAll(frame::class.java.simpleName, frame.id)
        }
        frameRepo.deleteAll(file.id)
        file.tracks.forEach{ track ->
            propertyRepo.deleteAll(track::class.java.simpleName, track.id)
            propertyCdfRepo.deleteAll(track::class.java.simpleName, track.id)
        }
        trackRepo.deleteAll(file.id)

        fileRepo.delete(file.id)
    }


    fun reOrder(reorderType: ReorderTypes, file: File) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(file.project.id, file.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    file.order += 1
                    fileRepo.save(file)
                    fileRepo.save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = fileRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(file.project.id, file.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    file.order -= 1
                    fileRepo.save(file)
                    fileRepo.save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = fileRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(file.project.id, file.order)
                previousEntities.forEach{it.order++}
                fileRepo.saveAll(previousEntities)
                file.order = 1
                fileRepo.save(file)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(file.project.id, file.order)
                if (nextEntities.count() > 0) {
                    nextEntities.forEach{it.order--}
                    fileRepo.saveAll(nextEntities)
                    file.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    fileRepo.save(file)
                }
            }
        }
    }


}