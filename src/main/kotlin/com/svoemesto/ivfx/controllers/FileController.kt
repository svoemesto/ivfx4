package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
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
import com.svoemesto.ivfx.utils.getListIFrames
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import org.sikuli.basics.Settings
import org.sikuli.script.Finder
import org.sikuli.script.Match
import org.sikuli.script.Pattern
import org.springframework.stereotype.Controller
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
                     val trackRepo: TrackRepo,
                     val shotRepo: ShotRepo) {

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
        val propertyValue = getPropertyValue(file, folder.propertyCdfKey)
        val projectCdfFolder = ProjectController(projectRepo,propertyRepo,propertyCdfRepo,projectCdfRepo,fileRepo,fileCdfRepo,frameRepo,trackRepo, shotRepo).getCdfFolder(file.project, folder, createFolderIfNotExist)
        val fld = if (propertyValue == "") projectCdfFolder  + IOFile.separator + file.shortName else propertyValue
        try {
            if (createFolderIfNotExist && !IOFile(fld).exists()) IOFile(fld).mkdir()
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

    fun getLossless(file: File, createFolderIfNotExist: Boolean = false): String {
        val folder = getCdfFolder(file, Folders.LOSSLESS, createFolderIfNotExist)
        return if (folder == "") "" else folder + IOFile.separator + file.shortName + "_lossless.mkv"
    }

    fun getPreview(file: File, createFolderIfNotExist: Boolean = false): String {
        val folder = getCdfFolder(file, Folders.PREVIEW, createFolderIfNotExist)
        return if (folder == "") "" else folder + IOFile.separator + file.shortName + "_preview.mp4"
    }

    fun hasFramesSmall(file: File): Boolean {
        val countFrames = getFramesCount(file)
        val fld = getCdfFolder(file, Folders.FRAMES_SMALL)
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^\\b${fileNameRegexp}_frame_\\b\\d{6}\\.\\bjpg\\b\$")

        return if (!IOFile(fld).exists()) {
            false
        } else {
            countFrames == (IOFile(fld).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasFramesMedium(file: File): Boolean {
        val countFrames = getFramesCount(file)
        val fld = getCdfFolder(file, Folders.FRAMES_MEDIUM)
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^\\b${fileNameRegexp}_frame_\\b\\d{6}\\.\\bjpg\\b\$")

        return if (!IOFile(fld).exists()) {
            false
        } else {
            countFrames == (IOFile(fld).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasFramesFull(file: File): Boolean {
        val countFrames = getFramesCount(file)
        val fld = getCdfFolder(file, Folders.FRAMES_FULL)
        val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
        val frameFilenameRegex = Regex("^\\b${fileNameRegexp}_frame_\\b\\d{6}\\.\\bjpg\\b\$")

        return if (!IOFile(fld).exists()) {
            false
        } else {
            countFrames == (IOFile(fld).listFiles { _, name -> name.contains(frameFilenameRegex) }?.size ?: 0)
        }
    }

    fun hasAnalyzedFrames(file: File): Boolean {
        return ShotController(
            projectRepo,
            propertyRepo,
            propertyCdfRepo,
            projectCdfRepo,
            fileRepo,
            fileCdfRepo,
            frameRepo,
            trackRepo,
            shotRepo
        ).getListShots(file).isNotEmpty()
    }

    fun hasFaces(file: File): Boolean {
        if (getCdfFolder(file, Folders.FRAMES_FULL) != "") {
            val fld = IOFile(getCdfFolder(file, Folders.FRAMES_FULL)).parent
            if (IOFile(fld).exists()) {
                val fileNameRegexp = file.shortName.replace(".", "\\.").replace("-", "\\-")
                val faceFilenameRegex = Regex("^\\b${fileNameRegexp}_frame_\\b\\d{6}_face_\\d{2}\\.\\bjpg\\b\$")
                return (IOFile(fld).listFiles { _, name -> name.contains(faceFilenameRegex) }?.size ?: 0) > 0
            }
        }
        return false
    }

    fun getListFilesExt(project: Project): List<FileExt> {
        val list = fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toList()
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

    fun getFps(file: File): Double {
        return getFFmpegProbeResult(file).streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }
            .firstOrNull()?.r_frame_rate?.toDouble()?:0.0
    }

    fun getFramesCount(file: File): Int {
        return getFFmpegProbeResult(file).streams.filter { it.codec_type == FFmpegStream.CodecType.VIDEO }
            .firstOrNull()?.tags?.get("NUMBER_OF_FRAMES-eng")?.toInt()?:0
    }

    fun analizeFrames(file: File) {

        val frameController = FrameController(projectRepo,propertyRepo,propertyCdfRepo,projectCdfRepo,fileRepo,fileCdfRepo,frameRepo,trackRepo, shotRepo)
        val mediaFile: String = file.path
        val fps: Double = getFps(file)
        val framesCount: Int = getFramesCount(file)
        Settings.MinSimilarity = 0.0
        var simScore: Double

        // 1. получаем список IFrame-ов
        val listIFrames = getListIFrames(mediaFile, fps)

        // 2. создаем список кадров и заполяем его номером, файлом и признаком isIFrame
        val listFrames: MutableList<Frame> = mutableListOf()
        for (frameNumber in 1..framesCount) {
            val percent = (frameNumber.toDouble() / framesCount.toDouble() * 100).toInt()
            val frame: Frame = frameController.getOrCreate(file, frameNumber)
            frame.isIFrame = listIFrames.contains(frameNumber)
//            frameRepo.save(frame)
            listFrames.add(frame)
        }

        // 3. заполняем simScore's
        for (i in 0 until listFrames.size - 1) {
            println("Analize frame #" + i + "/" + (listFrames.size - 1))
            val percent = (i.toDouble() / (listFrames.size - 1).toDouble() * 100).toInt()
            val currentFrame: Frame = listFrames[i]
            val frameNext1: Frame? = if (i < listFrames.size - 1) listFrames[i + 1] else null
            val frameNext2: Frame? = if (i < listFrames.size - 2) listFrames[i + 2] else null
            val frameNext3: Frame? = if (i < listFrames.size - 3) listFrames[i + 3] else null
            simScore = 0.9999
            if (frameNext1 != null) {
                val fileName1: String = frameController.getFileNameFrameSmall(currentFrame)
                val fileName2: String = frameController.getFileNameFrameSmall(frameNext1)
                val f = Finder(fileName1)
                val targetImage = Pattern(fileName2)
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.getScore()
                frameNext1.simScorePrev1 = simScore
            }
            currentFrame.simScoreNext1 = simScore
            simScore = 0.9999
            if (frameNext2 != null) {
                val f = Finder(frameController.getFileNameFrameSmall(currentFrame))
                val targetImage = Pattern(frameController.getFileNameFrameSmall(frameNext2))
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.getScore()
                frameNext2.simScorePrev2 = simScore
            }
            currentFrame.simScoreNext2 = simScore
            simScore = 0.9999
            if (frameNext3 != null) {
                val f = Finder(frameController.getFileNameFrameSmall(currentFrame))
                val targetImage = Pattern(frameController.getFileNameFrameSmall(frameNext3))
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.getScore()
                frameNext3.simScorePrev3 = simScore
            }
            currentFrame.simScoreNext3 = simScore
        }

        // 4. заполняем diff's
        for (i in 0 until listFrames.size - 1) {
            val percent = (i.toDouble() / (listFrames.size - 1).toDouble() * 100).toInt()
            val currentFrame: Frame = listFrames[i]
            val framePrev1: Frame? = if (i > 0) listFrames[i - 1] else null
            val framePrev2: Frame? = if (i > 1) listFrames[i - 2] else null
            val frameNext1: Frame? = if (i < listFrames.size - 1) listFrames[i + 1] else null
            val frameNext2: Frame? = if (i < listFrames.size - 2) listFrames[i + 2] else null
            var diffNext: Double
            diffNext = 0.0
            if (frameNext1 != null) {
                diffNext = currentFrame.simScoreNext1 - frameNext1.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffNext1 = diffNext
            diffNext = 0.0
            if (frameNext1 != null && frameNext2 != null) {
                diffNext = frameNext1.simScoreNext1 - frameNext2.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffNext2 = diffNext
            diffNext = 0.0
            if (framePrev1 != null) {
                diffNext = framePrev1.simScoreNext1 - currentFrame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffPrev1 = diffNext
            diffNext = 0.0
            if (framePrev1 != null && framePrev2 != null) {
                diffNext = framePrev2.simScoreNext1 - framePrev1.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffPrev2 = diffNext
        }

        // 5. находим переходы
        val diff1 = 0.4 //Порог обнаружения перехода
        val diff2 = 0.42 //Вторичный порог
        for (frame in listFrames) {
            if (frame.simScorePrev1 < diff1) {
                if (frame.diffPrev1 > diff2 || frame.diffPrev2 > diff2) {
                    frame.isFind = true
                    frame.isManualAdd = false
                    frame.isManualCancel = false
                    frame.isFinalFind = false
                } else {
                    frame.isFind = false
                    frame.isManualAdd = false
                    frame.isManualCancel = false
                    frame.isFinalFind = false
                }
            } else if (frame.diffPrev1 > diff2 && frame.diffPrev2 > diff2 && frame.simScoreNext1 > diff1) {
                frame.isFind = true
                frame.isManualAdd = false
                frame.isManualCancel = false
                frame.isFinalFind = false
            } else {
                frame.isFind = false
                frame.isManualAdd = false
                frame.isManualCancel = false
                frame.isFinalFind = false
            }

        }

        // 6. сохраняем фреймы
        for (frame in listFrames) {
            frameRepo.save(frame)
        }
//        IVFXShots.createListShotsByFrames(file)
        
    }


}