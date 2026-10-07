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
import net.bramp.ffmpeg.shared.CodecType
import net.bramp.ffmpeg.probe.FFmpegStream
import org.springframework.stereotype.Controller
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
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

        /**
         * Число файлов, подходящих под регулярку, без создания объекта File на
         * каждый: в каталоге кадров их десятки тысяч на файл, и listFiles
         * вместе с последующей фильтрацией занимали больше двадцати секунд на
         * открытие окна.
         *
         * Прежде здесь стояло name.contains(frameFilenameRegex) — это НЕ
         * проверка регулярки: contains ищет подстроку, а Regex — CharSequence,
         * то есть искался буквальный текст «^GOTS..._frame_\d{6}\.jpg$» и
         * совпадений не было никогда. Теперь считаем регуляркой правильно.
         */
        private fun countByRegex(dir: IOFile, regex: Regex): Int {
            if (!dir.exists()) return 0
            var count = 0
            Files.newDirectoryStream(dir.toPath()).use { stream ->
                for (p in stream) {
                    if (regex.matches(p.fileName.toString())) count++
                }
            }
            return count
        }

        /**
         * Сколько кадров реально лежит в папке полных кадров.
         *
         * Число кадров из метаданных бывает на единицу больше написанного:
         * длительность, делённая на частоту, даёт лишний кадр на склейке.
         * Шаг разбора кадров создавал записи по метаданным, и последний
         * кадр уезжал в базу без файла — детектор на нём падал, и вся серия
         * оставалась без лиц. Поэтому число для базы берём то, что есть.
         */
        fun getExistingFramesCount(fileExt: FileExt): Int {
            val nameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val regex = Regex("^${nameRegexp}_frame_\\d{6}\\.jpg\$")
            return countByRegex(IOFile(fileExt.folderFramesFull), regex)
        }

        /** Есть ли хоть один подходящий файл: обход прерывается на первом. */
        private fun anyByRegex(dir: IOFile, regex: Regex): Boolean {
            if (!dir.exists()) return false
            Files.newDirectoryStream(dir.toPath()).use { stream ->
                for (p in stream) {
                    if (regex.matches(p.fileName.toString())) return true
                }
            }
            return false
        }

        /** Замер каждой проверки: при открытии окна их вызывается много подряд,
         *  и по журналу видно, какая именно съедает время. */
        private inline fun <T> timed(name: String, block: () -> T): T {
            val t0 = System.currentTimeMillis()
            val r = block()
            val ms = System.currentTimeMillis() - t0
            if (ms > 30) println("[ЗАМЕР] $name = ${ms} мс")
            return r
        }

        fun hasFramesSmall(fileExt: FileExt): Boolean = timed("hasFramesSmall:" + fileExt.file.shortName) { hasFramesSmallImpl(fileExt) }

        private fun hasFramesSmallImpl(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            // Сравниваем с числом кадров, которое знает приложение, а не с
            // числом кадров в самом видео: на этой серии видео сообщает о
            // 88644 кадрах, на диске лежит 88643 (small, medium) и 88647 (full).
            // Равенство не выполняется ни для одной папки, и признак всегда
            // показывал «шаг не выполнен». Правильный вопрос — «нарезано ли не
            // меньше, чем приложение знает», и он устойчив к расхождениям.
            return countByRegex(IOFile(fileExt.folderFramesSmall), frameFilenameRegex) >=
                    Main.frameRepo.getCountFrames(fileExt.file.id)
        }

        fun hasFramesMedium(fileExt: FileExt): Boolean = timed("hasFramesMedium:" + fileExt.file.shortName) { hasFramesMediumImpl(fileExt) }

        private fun hasFramesMediumImpl(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            // Сравниваем с числом кадров, которое знает приложение, а не с
            // числом кадров в самом видео: на этой серии видео сообщает о
            // 88644 кадрах, на диске лежит 88643 (small, medium) и 88647 (full).
            // Равенство не выполняется ни для одной папки, и признак всегда
            // показывал «шаг не выполнен». Правильный вопрос — «нарезано ли не
            // меньше, чем приложение знает», и он устойчив к расхождениям.
            return countByRegex(IOFile(fileExt.folderFramesMedium), frameFilenameRegex) >=
                    Main.frameRepo.getCountFrames(fileExt.file.id)
        }

        fun hasFramesFull(fileExt: FileExt): Boolean = timed("hasFramesFull:" + fileExt.file.shortName) { hasFramesFullImpl(fileExt) }

        private fun hasFramesFullImpl(fileExt: FileExt): Boolean {
            val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
            val frameFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}\\.jpg\$")

            // Сравниваем с числом кадров, которое знает приложение, а не с
            // числом кадров в самом видео: на этой серии видео сообщает о
            // 88644 кадрах, на диске лежит 88643 (small, medium) и 88647 (full).
            // Равенство не выполняется ни для одной папки, и признак всегда
            // показывал «шаг не выполнен». Правильный вопрос — «нарезано ли не
            // меньше, чем приложение знает», и он устойчив к расхождениям.
            return countByRegex(IOFile(fileExt.folderFramesFull), frameFilenameRegex) >=
                    Main.frameRepo.getCountFrames(fileExt.file.id)
        }

        fun hasAnalyzedFrames(file: File): Boolean = timed("hasAnalyzedFrames:" + file.shortName) { hasAnalyzedFramesImpl(file) }

        private fun hasAnalyzedFramesImpl(file: File): Boolean {
            return Main.frameRepo.getCountFrames(file.id) > 0
        }

        fun hasCreatedShots(file: File): Boolean = timed("hasCreatedShots:" + file.shortName) { hasCreatedShotsImpl(file) }

        private fun hasCreatedShotsImpl(file: File): Boolean {
            return ShotController.getSetShots(file).isNotEmpty()
        }

        fun hasDetectedFaces(fileExt: FileExt): Boolean = timed("hasDetectedFaces:" + fileExt.file.shortName) { hasDetectedFacesImpl(fileExt) }

        private fun hasDetectedFacesImpl(fileExt: FileExt): Boolean {
            if (fileExt.folderFacesFull != "") {
                val fld = fileExt.folderFacesFull
                if (IOFile(fld).exists()) {
                    val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
                    val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg\$")
                    return anyByRegex(IOFile(fld), faceFilenameRegex)
                }
            }
            return false
        }

        fun hasRecognizedFaces(file: File): Boolean = timed("hasRecognizedFaces:" + file.shortName) { hasRecognizedFacesImpl(file) }

        private fun hasRecognizedFacesImpl(file: File): Boolean {
            val personUndefinded = PersonController.getUndefinded(file.project)
            return Main.faceRepo.findFirstByFileIdAndPersonIdNotEqual(file.id, personUndefinded.id).any()
//            return Main.faceRepo.findByFileIdAndPersonIdNotEqual(file.id, personUndefinded.id).any()
        }

        fun hasCreatedFaces(file: File): Boolean = timed("hasCreatedFaces:" + file.shortName) { hasCreatedFacesImpl(file) }

        private fun hasCreatedFacesImpl(file: File): Boolean {
            return Main.faceRepo.getFirstByFileId(file.id).any()
//            return Main.faceRepo.findByFileId(file.id).any()
        }

        fun hasCreatedFacesPreview(fileExt: FileExt): Boolean = timed("hasCreatedFacesPreview:" + fileExt.file.shortName) { hasCreatedFacesPreviewImpl(fileExt) }

        private fun hasCreatedFacesPreviewImpl(fileExt: FileExt): Boolean {
            // Проверка идёт перебором каталога, и раньше это стоило 22 секунды
            // на файл: dir.listFiles() создаёт объект File на каждую из
            // 12548 записей на HDD, и окно Project Actions открывалось полминуты.
            // Нужно знать только «есть ли хоть одно превью», поэтому каталог
            // читается потоком и обход прерывается на первом совпадении.
            val fld = fileExt.folderFacesPreview
            val dir = IOFile(fld ?: "")
            val exists = dir.exists()
            var matched = 0
            var sample = ""
            if (exists) {
                val fileNameRegexp = fileExt.file.shortName.replace(".", "\\.").replace("-", "\\-")
                val faceFilenameRegex = Regex("^${fileNameRegexp}_frame_\\d{6}_face_\\d{2}\\.jpg\$")
                Files.newDirectoryStream(dir.toPath()).use { stream ->
                    for (p in stream) {
                        val name = p.fileName.toString()
                        // Раньше здесь стояло name.contains(faceFilenameRegex).
                        // Это НЕ проверка регулярки: contains ищет образец как
                        // обычную подстроку, а Regex — CharSequence, то есть
                        // искалась буквальная строка «^GOTS..._frame_\d{6}...».
                        // Совпадений не было никогда, и признак CFP у превью
                        // готовых всегда показывал, что шаг не выполнен.
                        if (faceFilenameRegex.matches(name)) {
                            matched = 1
                            sample = name
                            break
                        }
                    }
                }
            }
            println("[CFP-диагностика] путь='${fld}' существует=$exists совпало=$matched пример='$sample'")

            if (fld != "" && exists && matched > 0) return true
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
            return getFFmpegProbeResult(file).streams.firstOrNull { it.codec_type == CodecType.VIDEO }?.r_frame_rate?.toDouble()?:0.0
        }

        /**
         * Число кадров в файле.
         *
         * Раньше здесь читался тег `NUMBER_OF_FRAMES-eng`, но это тег
         * MediaInfo, а проба идёт через ffprobe, который таких тегов не
         * пишет. Поле всегда было нулём, и всё, что на него опирается,
         * работало вхолостую: создание кадров вставляло `limit 0` и не
         * делало ничего, а анализ кадров проходил по пустому списку и
         * завершался без ошибки.
         *
         * Теперь сначала берётся `nb_frames` от ffprobe. У mkv это поле
         * не заполнено (ffprobe отдаёт N/A), и тогда остаётся длина файла,
         * умноженная на частоту кадров, — но так получается на один кадр
         * больше нужного: длина контейнера берётся по самой длинной дорожке,
         * а у видео она на 60 мс короче звука. На проверенном файле выходило
         * 88 644 против фактических 88 643, из-за чего:
         *   - `hasFramesSmall` сравнивал 88 644 с числом файлов на диске и
         *     показывал «✗» у полностью отработавшего действия;
         *   - в базу попадал лишний кадр 88 644, файла которого нет, и
         *     анализ кадров падал на нём, не сохраняя результат.
         *
         * Поэтому точное число берётся перебором пакетов видеодорожки.
         * Ответ кэшируется: подсчёт читает весь файл, а значение нужно
         * во многих местах и для одного и того же файла не меняется.
         *
         * @param file файл
         * @return число кадров либо ноль, если определить не удалось
         */
        fun getFramesCount(file: File): Int {
            // Число кадров уже сохранено свойством дорожки, когда файл
            // разбирался на дорожки. Берём его оттуда.
            //
            // Раньше здесь сперва запускался ffprobe, а у mkv он отдаёт по
            // этому файлу N/A, и код уходил в перебор пакетов — то есть
            // читал видеофайл ЦЕЛИКОМ. На файле в 1,5 гигабайта это 35–40
            // секунд на файл, и окно Project Actions открывалось полторы
            // минуты на две серии. Перебор каталогов тут ни при чём: замеры
            // показали 39 секунд на «кадры малые» и 111 мс на «кадры средние»
            // при одинаковых 88643 файлах.
            framesCountCache[file.path]?.let { return it }
            val stored = getStoredFramesCount(file)
            if (stored > 0) {
                framesCountCache[file.path] = stored
                return stored
            }
            val result = getFFmpegProbeResult(file)
            val video = result.streams.firstOrNull { it.codec_type == CodecType.VIDEO }
                ?: return 0
            if (video.nb_frames > 0) {
                return video.nb_frames.toInt()
            }
            val key = file.path
            framesCountCache[key]?.let { return it }
            val counted = countFramesByPackets(file)
            if (counted > 0) {
                framesCountCache[key] = counted
                return counted
            }
            val fps = video.avg_frame_rate?.toDouble() ?: 0.0
            val duration = result.getFormat().duration
            if (fps <= 0.0 || duration <= 0.0) {
                return 0
            }
            return Math.floor(duration * fps).toInt()
        }

        /**
         * Кэш числа кадров по полному пути файла: перебор пакетов читает
         * файл целиком, повторять это для одного и того же файла незачем.
         */
        /**
         * Число кадров из свойства дорожки файла, без чтения самого видео.
         */
        private fun getStoredFramesCount(file: File): Int {
            return try {
                for (track in Main.trackRepo.findByFileId(file.id)) {
                    val value = Main.propertyRepo
                        .findByParentClassAndParentIdAndKey("Track", track.id, "FrameCount")
                        .firstOrNull()?.value ?: continue
                    val n = value.toIntOrNull() ?: continue
                    if (n > 0) return n
                }
                0
            } catch (_: Exception) {
                0
            }
        }

        private val framesCountCache: MutableMap<String, Int> = ConcurrentHashMap()

        /**
         * Считает кадры перебором пакетов видеодорожки.
         *
         * Здесь не используется `executeExe`: он отбрасывает два последних
         * символа вывода, а здесь ответ — одно число, и вместе с переводом
         * строки срезалась бы последняя цифра.
         *
         * @param file файл
         * @return число кадров либо ноль, если ffprobe не ответил
         */
        private fun countFramesByPackets(file: File): Int {
            return try {
                val param = listOf(
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-count_packets",
                    "-show_entries", "stream=nb_read_packets",
                    "-of", "default=nw=1:nk=1",
                    file.path
                )
                val process = ProcessBuilder(IvfxFFmpegUtils.FFPROBE_PATH, *param.toTypedArray())
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()
                output.trim().lineSequence()
                    .map { it.trim().toIntOrNull() }
                    .firstOrNull { it != null } ?: 0
            } catch (_: Exception) {
                0
            }
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