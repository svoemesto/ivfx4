package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.VideoContainers
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

        fun getFolderFacesFull(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FACES_FULL.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFacesFull + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderFacesPreview(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.FACES_PREVIEW.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderFacesPreview + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderShotsCompressedWithAudio(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.SHOTS_COMPRESSED_WITH_AUDIO.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderShotsCompressedWithAudio + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderShotsLosslessWithAudio(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.SHOTS_LOSSLESS_WITH_AUDIO.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderShotsLosslessWithAudio + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderShotsLosslessWithoutAudio(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.SHOTS_LOSSLESS_WITHOUT_AUDIO.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderShotsLosslessWithoutAudio + IOFile.separator + fileExt.file.shortName else value
        }

        fun getFolderConcat(fileExt: FileExt): String{
            val value = PropertyCdfController.getOrCreate(fileExt.file::class.java.simpleName, fileExt.file.id, Folders.CONCAT.propertyCdfKey)
            return if (value == "") fileExt.projectExt.folderConcat else value
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

        fun hasConcat(fileExt: FileExt): Boolean {
            return IOFile(getConcat(fileExt)).exists()
        }

        fun getLossless(fileExt: FileExt): String {
            return "${fileExt.folderLossless}${IOFile.separator}${fileExt.file.shortName}_lossless.mkv"
        }

        fun getPreview(fileExt: FileExt): String {
            return "${fileExt.folderPreview}${IOFile.separator}${fileExt.file.shortName}_preview.mp4"
        }

        fun getConcat(fileExt: FileExt): String {
            return "${fileExt.folderConcat}${IOFile.separator}${fileExt.file.shortName}_concat.${VideoContainers.valueOf(fileExt.projectExt.project.container).extention}"
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
            return ShotController.getSetShots(file).isNotEmpty()
        }

        fun hasDetectedFaces(fileExt: FileExt): Boolean {
            if (fileExt.folderFacesFull != "") {
                val fld = fileExt.folderFacesFull
                if (IOFile(fld).exists()) {
                    val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
                    val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg\$")
                    return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
                }
            }
            return false
        }

        fun hasRecognizedFaces(file: File): Boolean {
            val personUndefinded = PersonController.getUndefinded(file.project)
            return Main.faceRepo.findFirstByFileIdAndPersonIdNotEqual(file.id, personUndefinded.id).any()
//            return Main.faceRepo.findByFileIdAndPersonIdNotEqual(file.id, personUndefinded.id).any()
        }

        fun hasCreatedFaces(file: File): Boolean {
            return Main.faceRepo.getFirstByFileId(file.id).any()
//            return Main.faceRepo.findByFileId(file.id).any()
        }

        fun hasCreatedFacesPreview(fileExt: FileExt): Boolean {
            if (fileExt.folderFacesPreview != "") {
                val fld = fileExt.folderFacesPreview
                if (IOFile(fld).exists()) {
                    val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
                    val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg\$")
                    return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
                }
            }
            return false
        }

        fun hasShotsCompressedWithAudio(fileExt: FileExt): Boolean {
            return false // !fileExt.shotsExt.any { !it.hasCompressedWithAudio }
        }

        fun hasShotsLosslessWithAudio(fileExt: FileExt): Boolean {
            return false // !fileExt.shotsExt.any { !it.hasLosslessWithAudio }
        }

        fun hasShotsLosslessWithoutAudio(fileExt: FileExt): Boolean {
            return false // !fileExt.shotsExt.any { !it.hasLosslessWithoutAudio }
        }


        fun getListFilesExt(project: Project): List<FileExt> {
            val projectExt = ProjectExt(project)
            val result = getSetFiles(project).map { FileExt(it, projectExt) }.toMutableList()
            result.sort()
            return result
        }

//        fun getListFiles(project: Project): MutableList<File> {
//            val result = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toMutableList()
//            result.forEach { file ->
//                file.project = project
//
//                val cdf = FileCdfController.getFileCdf(file)
//                file.cdfs = mutableSetOf()
//                file.cdfs.add(cdf)
//                file.tracks = TrackController.getSetTracks(file)
//            }
//            return result
//        }

        fun getSetFiles(project: Project): MutableSet<File> {
            val files = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0)
            return files.map { file ->
                file.project = project
                val cdf = FileCdfController.getFileCdf(file)
                file.cdfs = mutableSetOf()
                file.cdfs.add(cdf)
                file.tracks = TrackController.getSetTracks(file)
                file
            }.toMutableSet()
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

            entity.cdfs = mutableSetOf()
            val cdf = FileCdfController.create(entity)
            cdf.path = path
            FileCdfController.save(cdf)
            entity.cdfs.add(cdf)

            FileCdfController.save(entity.cdfs.first())
            Folders.values().filter{ it.forFile }.forEach {
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
            project.files.forEach { delete(it) }
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

//            return file.tracks.filter{it.type == "General"}.firstOrNull()?.let { TrackController.getPropertyValue(it,"FrameRate").toDouble() } ?: 0.0
            return getFFmpegProbeResult(file).streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.r_frame_rate?.toDouble()?:0.0
        }

        fun getFramesCount(file: File): Int {
//            return file.tracks.filter{it.type == "General"}.firstOrNull()?.let { TrackController.getPropertyValue(it,"FrameCount").toInt() } ?: 0
            return getFFmpegProbeResult(file).streams.firstOrNull { it.codec_type == FFmpegStream.CodecType.VIDEO }?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()?:0
        }

        fun getFile(fileId: Long, project: Project): File {
            val file = Main.fileRepo.findById(fileId).get()
            file.project = project
            val cdf = FileCdfController.getFileCdf(file)
            file.cdfs = mutableSetOf()
            file.cdfs.add(cdf)
            file.tracks = TrackController.getSetTracks(file)
            file.shots = ShotController.getSetShots(file)
            return file
        }

        fun getFileExt(fileId: Long, project: Project): FileExt {
            val file = getFile(fileId, project)
            return FileExt(file, ProjectController.getProjectExt(file.project.id))
        }

        fun getFramesWithFaces(file: File): MutableSet<Int> {
            val result: MutableSet<Int> = mutableSetOf()
            val sqlFaces = "select distinct tf.frame_number from tbl_faces as tf where tf.file_id = ?"
            val stFaces = Main.connection.prepareStatement(sqlFaces)
            stFaces.setLong(1, file.id)
            val rsFaces = stFaces.executeQuery()
            while (rsFaces.next()) {
                result.add(rsFaces.getInt("frame_number"))
            }
            return result
        }

        fun getFileForShotId(shotId: Long): File {
            val file = Main.fileRepo.getFileForShotId(shotId).first()
            file.project = ProjectController.getProjectForFileId(file.id)
            val cdf = FileCdfController.getFileCdf(file)
            file.cdfs = mutableSetOf()
            file.cdfs.add(cdf)
            file.tracks = TrackController.getSetTracks(file)
            file.shots = ShotController.getSetShots(file)
            return file
        }

    }
}