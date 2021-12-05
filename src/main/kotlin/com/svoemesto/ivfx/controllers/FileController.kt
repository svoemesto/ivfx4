package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController
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
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import org.springframework.stereotype.Controller
import java.io.IOException
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class FileController() {

    class FileExt(val file: File) {
        var hasPreview: Boolean = false
        var hasPreviewString: String = ""
        var hasLossless: Boolean = false
        var hasLosslessString: String = ""
        var hasFramesSmall: Boolean = false
        var hasFramesSmallString: String = ""
        var hasFramesMedium: Boolean = false
        var hasFramesMediumString: String = ""
        var hasFramesFull: Boolean = false
        var hasFramesFullString: String = ""
        var hasAnalyzedFrames: Boolean = false
        var hasAnalyzedFramesString: String = ""
        var hasFaces: Boolean = false
        var hasFacesString: String = ""
        val order = file.order
        val name = file.name
    }

    fun getCdfFolder(file: File, folder: Folders, createFolderIfNotExist: Boolean = false): String {
        if (!isPropertyCdfPresent(file, folder.propertyCdfKey)) {
            Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, folder.propertyCdfKey)
        }
        val propertyValue = getPropertyCdfValue(file, folder.propertyCdfKey)
        val projectCdfFolder = Main.projectController.getCdfFolder(file.project, folder, createFolderIfNotExist)
        val fld = if (propertyValue == "") projectCdfFolder  + IOFile.separator + file.shortName else propertyValue
        try {
            if (createFolderIfNotExist && !IOFile(fld).exists()) IOFile(fld).mkdir()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fld
    }

    fun initializeTransientFields(file: File) {
        file.folderLossless = getFolderLossless(file)
        file.folderPreview = getFolderPreview(file)
        file.folderFavorites = getFolderFavorites(file)
        file.folderShots = getFolderShots(file)
        file.folderFramesSmall = getFolderFramesSmall(file)
        file.folderFramesMedium = getFolderFramesMedium(file)
        file.folderFramesFull = getFolderFramesFull(file)
        file.fps = getFps(file)
        file.framesCount = getFramesCount(file)
    }

    fun getFolderLossless(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.LOSSLESS.propertyCdfKey)
        return if (value == "") file.project.folderLossless + IOFile.separator + file.shortName else value
    }

    fun getFolderPreview(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.PREVIEW.propertyCdfKey)
        return if (value == "") file.project.folderPreview + IOFile.separator + file.shortName else value
    }

    fun getFolderFavorites(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.FAVORITES.propertyCdfKey)
        return if (value == "") file.project.folderFavorites + IOFile.separator + file.shortName else value
    }

    fun getFolderShots(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.SHOTS.propertyCdfKey)
        return if (value == "") file.project.folderShots + IOFile.separator + file.shortName else value
    }

    fun getFolderFramesSmall(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.FRAMES_SMALL.propertyCdfKey)
        return if (value == "") file.project.folderFramesSmall + IOFile.separator + file.shortName else value
    }

    fun getFolderFramesMedium(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.FRAMES_MEDIUM.propertyCdfKey)
        return if (value == "") file.project.folderFramesMedium + IOFile.separator + file.shortName else value
    }

    fun getFolderFramesFull(file: File): String{
        val value = Main.propertyCdfController.getOrCreate(file::class.java.simpleName, file.id, Folders.FRAMES_FULL.propertyCdfKey)
        return if (value == "") file.project.folderFramesFull + IOFile.separator + file.shortName else value
    }

    fun getFFmpegProbeResult(file: File): FFmpegProbeResult {
        return FFprobe(IvfxFFmpegUtils.FFPROBE_PATH).probe(file.path)
    }

    fun hasLossless(file: File): Boolean {
        return IOFile(file.folderLossless).exists()
    }

    fun hasPreview(file: File): Boolean {
        return IOFile(file.folderPreview).exists()
    }

    fun getLossless(file: File, createFolderIfNotExist: Boolean = false): String {
        return if (file.folderLossless == "") "" else file.folderLossless + IOFile.separator + file.shortName + "_lossless.mkv"
    }

    fun getPreview(file: File, createFolderIfNotExist: Boolean = false): String {
        return if (file.folderPreview == "") "" else file.folderPreview + IOFile.separator + file.shortName + "_preview.mp4"
    }

    fun hasFramesSmall(file: File): Boolean {
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

        return if (!IOFile(file.folderFramesSmall).exists()) {
            false
        } else {
            file.framesCount == (IOFile(file.folderFramesSmall).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasFramesMedium(file: File): Boolean {
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

        return if (!IOFile(file.folderFramesMedium).exists()) {
            false
        } else {
            file.framesCount == (IOFile(file.folderFramesMedium).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasFramesFull(file: File): Boolean {
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

        return if (!IOFile(file.folderFramesFull).exists()) {
            false
        } else {
            file.framesCount == (IOFile(file.folderFramesFull).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasAnalyzedFrames(file: File): Boolean {
        return Main.shotController.getListShots(file).isNotEmpty()
    }

    fun hasFaces(file: File): Boolean {
        if (file.folderFramesFull != "") {
            val fld = file.folderFramesFull + ".faces"
            if (IOFile(fld).exists()) {
                val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
                val faceFilenameRegex = Regex("^\\b${fileNameRegexp}_frame_\\b\\d{6}_face_\\d{2}\\.\\bjpg\\b\$")
                return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
            }
        }
        return false
    }

    fun getListFilesExt(project: Project): List<FileExt> {
        val list = getListFiles(project)
        val resultedList: MutableList<FileExt> = mutableListOf()
        list.forEach { file ->
            val fileExt = FileExt(file)
            fileExt.hasPreview = hasPreview(file)
            fileExt.hasPreviewString = if (fileExt.hasPreview) "✓" else "✗"
            fileExt.hasLossless = hasLossless(file)
            fileExt.hasLosslessString = if (fileExt.hasLossless) "✓" else "✗"
            fileExt.hasFramesSmall = hasFramesSmall(file)
            fileExt.hasFramesSmallString = if (fileExt.hasFramesSmall) "✓" else "✗"
            fileExt.hasFramesMedium = hasFramesMedium(file)
            fileExt.hasFramesMediumString = if (fileExt.hasFramesMedium) "✓" else "✗"
            fileExt.hasFramesFull = hasFramesFull(file)
            fileExt.hasFramesFullString = if (fileExt.hasFramesFull) "✓" else "✗"
            fileExt.hasAnalyzedFrames = hasAnalyzedFrames(file)
            fileExt.hasAnalyzedFramesString = if (fileExt.hasAnalyzedFrames) "✓" else "✗"
            fileExt.hasFaces = hasFaces(file)
            fileExt.hasFacesString = if (fileExt.hasFaces) "✓" else "✗"
            resultedList.add(fileExt)
        }
        return resultedList
    }

    fun getListFiles(project: Project): MutableList<File> {
        val result = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toMutableList()
        result.forEach { file ->
            file.project = project

            val cdf = Main.fileCdfController.getFileCdf(file)
            file.cdfs = mutableListOf()
            file.cdfs.add(cdf)

            initializeTransientFields(file)
//            file.frames = Main.frameController.getListFrames(file)
//            file.shots = Main.shotController.getListShots(file)
            file.tracks = Main.trackController.getListTracks(file)
        }
        return result
    }

    fun getProperties(file: File) : List<Property> {
        return Main.propertyRepo.findByParentClassAndParentId(file::class.simpleName!!, file.id).toList()
    }

    fun getPropertyValue(file: File, key: String) : String {
        val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(file::class.simpleName!!, file.id, key).firstOrNull()
        return property?.value ?: ""
    }

    fun isPropertyPresent(file: File, key: String) : Boolean {
        return Main.propertyRepo.findByParentClassAndParentIdAndKey(file::class.simpleName!!, file.id, key).any()
    }

    fun getPropertyCdfValue(file: File, key: String) : String {
        val propertyCdf = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(file::class.simpleName!!, file.id, Main.ccid, key).firstOrNull()
        return propertyCdf?.value ?: ""
    }

    fun isPropertyCdfPresent(file: File, key: String) : Boolean {
        return Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(file::class.simpleName!!, file.id, Main.ccid, key).any()
    }

    fun save(file: File) {
        Main.fileRepo.save(file)
    }

    fun saveAll(files: Iterable<File>) {
        files.forEach { save(it) }
    }

    fun create(project: Project, path: String): File {
        val entity = File()
        entity.project = project
        val lastEntity = Main.fileRepo.getEntityWithGreaterOrder(project.id).firstOrNull()
        entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
        entity.name = IOFile(path).nameWithoutExtension
        entity.shortName = entity.name
        save(entity)

        entity.cdfs = mutableListOf()
        val cdf = Main.fileCdfController.create(entity)
        cdf.path = path
        Main.fileCdfController.save(cdf)
        entity.cdfs.add(cdf)

        Main.fileCdfController.save(entity.cdfs.first())
        Folders.values().forEach {
            Main.propertyCdfController.editOrCreate(entity::class.java.simpleName, entity.id, it.propertyCdfKey)
        }
        Main.trackController.createTracksFromMediaInfo(entity)
        initializeTransientFields(entity)
        return entity
    }

    // удаление файла
    fun delete(file: File) {
        reOrder(ReorderTypes.MOVE_TO_LAST, file)
        Main.frameController.deleteAll(file)
        Main.trackController.deleteAll(file)
        Main.shotController.deleteAll(file)

        Main.propertyController.deleteAll(file::class.java.simpleName, file.id)
        Main.propertyCdfController.deleteAll(file::class.java.simpleName, file.id)
        Main.fileCdfController.deleteAll(file)
        Main.fileRepo.delete(file)
    }

    fun deleteAll(project: Project) {
        getListFiles(project).forEach { file ->
            delete(file)
        }
    }

    fun reOrder(reorderType: ReorderTypes, file: File) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(file.project.id, file.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    file.order += 1
                    save(file)
                    save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = Main.fileRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(file.project.id, file.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    file.order -= 1
                    save(file)
                    save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = Main.fileRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(file.project.id, file.order)
                previousEntities.forEach{it.order++}
                saveAll(previousEntities)
                file.order = 1
                save(file)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(file.project.id, file.order).toList()
                if (nextEntities.isNotEmpty()) {
                    nextEntities.forEach{it.order--}
                    saveAll(nextEntities)
                    file.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    save(file)
                }
            }
        }
    }

    fun getFps(file: File): Double {
        return getFFmpegProbeResult(file).streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.r_frame_rate?.toDouble()?:0.0
    }

    fun getFramesCount(file: File): Int {
        return getFFmpegProbeResult(file).streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()?:0
    }


}