package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ProjectExt
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

    companion object {

        fun getCdfFolder(file: File, folder: Folders, createFolderIfNotExist: Boolean = false): String {
            if (!isPropertyCdfPresent(file, folder.propertyCdfKey)) {
                PropertyCdfController.getOrCreate(file::class.java.simpleName, file.id, folder.propertyCdfKey)
            }
            val propertyValue = getPropertyCdfValue(file, folder.propertyCdfKey)
            val projectCdfFolder = ProjectController.getCdfFolder(file.project, folder, createFolderIfNotExist)
            val fld = if (propertyValue == "") projectCdfFolder  + IOFile.separator + file.shortName else propertyValue
            try {
                if (createFolderIfNotExist && !IOFile(fld).exists()) IOFile(fld).mkdir()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return fld
        }

        fun getFolderLossless(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.LOSSLESS.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderLossless + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderPreview(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.PREVIEW.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderPreview + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderFavorites(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FAVORITES.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFavorites + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderShots(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.SHOTS.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderShots + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderFramesSmall(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FRAMES_SMALL.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFramesSmall + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderFramesMedium(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FRAMES_MEDIUM.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFramesMedium + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderFramesFull(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FRAMES_FULL.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFramesFull + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFFmpegProbeResult(file: File): FFmpegProbeResult {
            return FFprobe(IvfxFFmpegUtils.FFPROBE_PATH).probe(file.path)
        }

        fun hasLossless(fileExt: FileExt): Boolean {
            return IOFile(getLossless(fileExt)).exists()
        }

        fun hasPreview(fileExt: FileExt): Boolean {
            return IOFile(getPreview(fileExt)).exists()
        }

        fun getLossless(fileExt: FileExt): String {
            return fileExt.folderLossless + IOFile.separator + fileExt.file.shortName + "_lossless.mkv"
        }

        fun getPreview(fileExt: FileExt): String {
            return fileExt.folderPreview + IOFile.separator + fileExt.file.shortName + "_preview.mp4"
        }

        fun hasFramesSmall(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            return if (!IOFile(fileExt.folderFramesSmall).exists()) {
                false
            } else {
                fileExt.framesCount == (IOFile(fileExt.folderFramesSmall).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
            }
        }

        fun hasFramesMedium(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            return if (!IOFile(fileExt.folderFramesMedium).exists()) {
                false
            } else {
                fileExt.framesCount == (IOFile(fileExt.folderFramesMedium).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
            }
        }

        fun hasFramesFull(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            return if (!IOFile(fileExt.folderFramesFull).exists()) {
                false
            } else {
                fileExt.framesCount == (IOFile(fileExt.folderFramesFull).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
            }
        }

        fun hasAnalyzedFrames(file: File): Boolean {
            return Main.frameRepo.getCountFrames(file.id) > 0
        }

        fun hasCreatedShots(file: File): Boolean {
            return ShotController.getListShots(file).isNotEmpty()
        }

        fun hasDetectedFaces(fileExt: FileExt): Boolean {
            if (fileExt.folderFramesFull != "") {
                val fld = fileExt.folderFramesFull + ".faces"
                if (IOFile(fld).exists()) {
                    val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
                    val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg\$")
                    return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
                }
            }
            return false
        }

        fun hasRecognizedFaces(file: File): Boolean {
            return FaceController.getListFaces(file).any { it.personRecognizedId != 0L }
        }

        fun hasCreatedFaces(file: File): Boolean {
            return FaceController.getListFaces(file).isNotEmpty()
        }

        fun getListFilesExt(project: Project): List<FileExt> {
            val list = getListFiles(project)
            val resultedList: MutableList<FileExt> = mutableListOf()
            val projectExt = ProjectExt(project)
            list.forEach { file ->
                val fileExt = FileExt(file, projectExt)
                resultedList.add(fileExt)
            }
            return resultedList
        }

        fun getListFiles(project: Project): MutableList<File> {
            val result = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toMutableList()
            result.forEach { file ->
                file.project = project

                val cdf = FileCdfController.getFileCdf(file)
                file.cdfs = mutableListOf()
                file.cdfs.add(cdf)

//                initializeTransientFields(file)
    //            file.frames = Main.frameController.getListFrames(file)
    //            file.shots = Main.shotController.getListShots(file)
                file.tracks = TrackController.getListTracks(file)
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
            val cdf = FileCdfController.create(entity)
            cdf.path = path
            FileCdfController.save(cdf)
            entity.cdfs.add(cdf)

            FileCdfController.save(entity.cdfs.first())
            Folders.values().forEach {
                PropertyCdfController.editOrCreate(entity::class.java.simpleName, entity.id, it.propertyCdfKey)
            }
            TrackController.createTracksFromMediaInfo(entity)
//            initializeTransientFields(entity)
            return entity
        }

        // удаление файла
        fun delete(file: File) {
            reOrder(ReorderTypes.MOVE_TO_LAST, file)
            FaceController.deleteAll(file)
            FrameController.deleteAll(file)
            TrackController.deleteAll(file)
            ShotController.deleteAll(file)

            PropertyController.deleteAll(file::class.java.simpleName, file.id)
            PropertyCdfController.deleteAll(file::class.java.simpleName, file.id)
            FileCdfController.deleteAll(file)
            TagController.deleteAll(file::class.java.simpleName, file.id)

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

        fun getFile(fileId: Long, project: Project): File {
            val file = Main.fileRepo.findById(fileId).get()
            file.project = project
            val cdf = FileCdfController.getFileCdf(file)
            file.cdfs = mutableListOf()
            file.cdfs.add(cdf)
            file.tracks = TrackController.getListTracks(file)
            return file
        }

        fun getFileExt(fileId: Long, project: Project): FileExt {
            val file = getFile(fileId, project)
            return FileExt(file, ProjectController.getProjectExt(file.project.id))
        }

    }
}