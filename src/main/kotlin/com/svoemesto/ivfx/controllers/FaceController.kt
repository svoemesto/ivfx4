package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.utils.FaceDetection
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
import javafx.geometry.Pos
import org.springframework.stereotype.Controller
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File as IOFile

@Controller
class FaceController {


    companion object {

        /**
         * Применяет результат распознавания или создания лиц **целиком, в одной
         * транзакции**.
         *
         * Почему так. Применение шло по одному лицу, и каждое лицо обращалось
         * к базе отдельно. Hibernate в проекте настроен на
         * `enable_lazy_load_no_trans`, а это значит: **каждое ленивое чтение
         * вне транзакции открывает новую сессию**. На каждое лицо приходилось
         * несколько таких чтений — отложенная коллекция персон проекта,
         * связи лица, — и на прогоне E05 это дало 21,5 секунды на 400 лиц,
         * то есть 54 миллисекунды на лицо.
         *
         * В одной транзакции сессия одна: ленивые чтения бесплатны, а
         * сохранения идут пачкой, а не по одному.
         *
         * **Лица серии читаются одним запросом и раскладываются в карту.**
         * Починка границы транзакции дала ×5,4 — с 54 до 9,9 мс на лицо на
         * прогоне E06, — но эти 9,9 мс складывались уже из двух обращений к
         * базе на каждое лицо: найти и сохранить. Поиск лица сам по себе
         * занимает 2,3 мс, и десять таких поисков на лицо и дают десять
         * миллисекунд.
         *
         * Карта строится по ключу «номер кадра + номер лица в кадре» — тому
         * самому, по которому лицо искалось запросом.
         *
         * [personByName] — кеш персон по имени в распознавателе. Без него на
         * прогоне E05 выполнялось 400 одинаковых поисков по 71 персоне.
         */
        fun createOrUpdateAll(
            facesExtJson: List<FaceExtJson>,
            fileExt: FileExt,
            undefindedPerson: PersonExt,
            nonPerson: PersonExt,
            onFaceApplied: (Int, Int) -> Unit,
        ): Int {
            val personByName: MutableMap<String, Person> = mutableMapOf()
            val changed: MutableList<Face> = mutableListOf()
            var applied = 0
            val total = facesExtJson.size
            Main.transactionTemplate.executeWithoutResult {
                // Лица группируются по файлу, потому что json может нести
                // лица нескольких серий, а читать надо лица того файла, к
                // которому относится запись. На прогоне серии файл один.
                facesExtJson.groupBy { it.fileId }.forEach { (_, itemsOfFile) ->
                    val existingByKey: Map<Pair<Int, Int>, Face> =
                        Main.faceRepo.findByFileId(itemsOfFile.first().fileId)
                            .associateBy { it.frameNumber to it.faceNumberInFrame }
                    val framesOfFile = Main.frameRepo.findByFileId(itemsOfFile.first().fileId)
                    val frameByNumber: Map<Int, Frame> = framesOfFile.associateBy { it.frameNumber }
                    itemsOfFile.forEach { faceExtJson ->
                        createOrUpdate(
                            faceExtJson, fileExt, undefindedPerson, nonPerson, personByName,
                            existingByKey,
                            changed,
                            frameByNumber[faceExtJson.frameNumber],
                        )
                        applied += 1
                        onFaceApplied(applied, total)
                    }
                }
                // Одно сохранение пачкой вместо сохранения на каждое лицо.
                if (changed.isNotEmpty()) Main.faceRepo.saveAll(changed)
            }
            return applied
        }

        fun createOrUpdate(
            faceExtJson: FaceExtJson,
            fileExt: FileExt,
            undefindedPerson: PersonExt,
            nonPerson: PersonExt,
            personByName: MutableMap<String, Person> = mutableMapOf(),
            existingByKey: Map<Pair<Int, Int>, Face>? = null,
            changed: MutableList<Face>? = null,
            preloadedFrame: Frame? = null,
        ): FaceExt {

            // Проект берём один раз: обращение fileExt.projectExt.project
            // внутри цикла — это ленивое чтение, а вне транзакции каждое
            // такое чтение открывает свою сессию.
            val project = fileExt.projectExt.project
            val w = faceExtJson.endX - faceExtJson.startX
            val h = faceExtJson.endY - faceExtJson.startY
            val d = if(w>h) w/h.toDouble() else h/w.toDouble()

            // Лицо ищется ТОЛЬКО через карту, если она передана, и промах в
            // карте означает «такого лица нет».
            //
            // Раньше здесь было `preloadedFace ?: поиск по базе`, и на новой
            // серии это давало катастрофу: карта пуста по построению — лиц
            // ещё нет, — поэтому КАЖДОЕ лицо уходило в базу. А каждый запрос
            // Hibernate перед собой сбрасывает накопленное, и сброс каскадит по
            // всем новым лицам серии. Итог — 120 мс на лицо вместо 0,7, то
            // есть рост в семьдесят пять раз и двадцать семь минут вместо
            // десяти секунд. На седьмой серии этого не было видно: там лица уже
            // существовали, карта попадала, и запросов в цикле не было.
            var face = if (existingByKey != null) {
                existingByKey[faceExtJson.frameNumber to faceExtJson.faceNumberInFrame]
            } else if (faceExtJson.frameId == 0L) {
                Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(
                    faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame,
                ).firstOrNull()
            } else {
                if (faceExtJson.faceId == 0L) {
                    null
                } else {
                    Main.faceRepo.findById(faceExtJson.frameId).orElse(null)
                }
            }

            if (face != null) {
                face.file = fileExt.file
                // Кто вправе решить судьбу лица.
                //
                // Имя в json приносит только распознавание. Создание лиц
                // имён не приносит вовсе, и прежнее правило «имя не пришло —
                // значит неопределённый» означало, что перезапуск создания лиц
                // на уже обработанной серии тихо отправлял размеченные лица
                // обратно в неопределённые. Работа оператора исчезала без
                // единой ошибки и без единой отметки.
                //
                // Правило теперь: **чужое решение не отменяется**. Персону
                // меняет только тот, кто принёс имя, то есть распознавание.
                // Геометрия — вытянутая рамка означает «не персонаж» —
                // применяется только к лицам, для которых ещё никто ничего
                // не решил.
                if (faceExtJson.personRecognizedName != "") {
                    if (faceExtJson.recognizeProbability > FaceDetection.RECOGNIZE_THRESHOLD) {
                        face.person = personByName.getOrPut(faceExtJson.personRecognizedName) {
                            PersonController.getPersonByProjectIdAndNameInRecognizer(project,
                                faceExtJson.personRecognizedName, faceExtJson.fileId,
                                faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                        }
                    } else {
                        face.person = undefindedPerson.person
                    }
                } else if (face.person.id == undefindedPerson.person.id && d > 4) {
                    face.person = nonPerson.person
                }

                val personExt = PersonExt(face.person, fileExt.projectExt)
                val faceExt = FaceExt(face, fileExt, personExt)

                var needToSave = false
                if (face.personRecognizedName != faceExtJson.personRecognizedName) {

                    if (faceExtJson.personRecognizedName != "") {
                        face.personRecognizedName = faceExtJson.personRecognizedName
                        if (faceExtJson.recognizeProbability > FaceDetection.RECOGNIZE_THRESHOLD) {
                            face.person = personByName.getOrPut(faceExtJson.personRecognizedName) {
                                PersonController.getPersonByProjectIdAndNameInRecognizer(project,
                                    faceExtJson.personRecognizedName, faceExtJson.fileId,
                                    faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                            }
                        } else {
                            face.person = undefindedPerson.person
                        }
                        needToSave = true
                    }
                    // Пустой ветке здесь больше нечего делать: снять персону
                    // пришло бы значить ровно то, от чего мы ушли выше, —
                    // стереть чужое решение из-за отсутствия имени в json.
                }
                if (face.recognizeProbability != faceExtJson.recognizeProbability) {
                    face.recognizeProbability = faceExtJson.recognizeProbability
                    needToSave = true
                }
                if (face.startX != faceExtJson.startX) {
                    face.startX = faceExtJson.startX
                    needToSave = true
                }
                if (face.startY != faceExtJson.startY) {
                    face.startY = faceExtJson.startY
                    needToSave = true
                }
                if (face.endX != faceExtJson.endX) {
                    face.endX = faceExtJson.endX
                    needToSave = true
                }
                if (face.endY != faceExtJson.endY) {
                    face.endY = faceExtJson.endY
                    needToSave = true
                }
                // Пустой вектор в json — это «не прислали», а не «обнули».
                // Скрипт возвращает те же записи, что получил, поэтому вместе
                // с результатом распознавания приходит и пустой вектор, если
                // приложение отдало json без них.
                if (faceExtJson.vector.isNotEmpty() && !faceExt.vector.contentEquals(faceExtJson.vector)) {
                    faceExt.vector = faceExtJson.vector
                    needToSave = true
                }
                if (needToSave) {
                    if (changed != null) changed.add(face) else save(face)
                }

                return faceExt

            } else {
                face = Face()
                face.isExample = false
                face.isManual = false
                face.file = fileExt.file
            }

            // Ссылка на кадр ставится ВСЕГДА, и для найденного тоже. Иначе
            // она накапливалась бы только у новых лиц, а у существующих
            // осталась бы незаполненной — то есть ровно на тех, где связь
            // нужна для разбора. Заполнение делается одним запросом на файл:
            // пачкой из карты дешевле, чем поиск на каждое лицо.
            if (face.frame == null && preloadedFrame != null) face.frame = preloadedFrame

            if (d > 4) {
                face.person = nonPerson.person
            } else {
                if (faceExtJson.personRecognizedName != "") {
                    if (faceExtJson.recognizeProbability > FaceDetection.RECOGNIZE_THRESHOLD) {
                        face.person = personByName.getOrPut(faceExtJson.personRecognizedName) {
                            PersonController.getPersonByProjectIdAndNameInRecognizer(project,
                                faceExtJson.personRecognizedName, faceExtJson.fileId,
                                faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                        }
                    } else {
                        face.person = undefindedPerson.person
                    }
                } else {
                    face.person = undefindedPerson.person
                }
            }

            val personExt = PersonExt(face.person, fileExt.projectExt)
            val faceExt = FaceExt(face, fileExt, personExt)

            face.frameNumber = faceExtJson.frameNumber
            face.faceNumberInFrame = faceExtJson.faceNumberInFrame
            face.personRecognizedName = faceExtJson.personRecognizedName
//            if (faceExtJson.personRecognizedName != "") {
//                face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
//                    faceExtJson.personRecognizedName, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
//            } else {
//                face.person = undefindedPerson.person
//            }
            face.recognizeProbability = faceExtJson.recognizeProbability
            face.startX = faceExtJson.startX
            face.startY = faceExtJson.startY
            face.endX = faceExtJson.endX
            face.endY = faceExtJson.endY
            face.vectorText = faceExtJson.vector.joinToString(separator = "|", prefix = "", postfix = "")

            // Новое лицо тоже уходит в общую очередь сохранения, иначе
            // создание лиц на новой серии по-прежнему писало бы базу по
            // одному лицу — то есть ровно то, что починка убирала.
            if (changed != null) changed.add(face) else save(face)

            return faceExt

        }

        fun save(face: Face) {
            Main.faceRepo.save(face)
        }

        fun getListFaces(file: File): MutableList<Face> {
            val result = Main.faceRepo.findByFileId(file.id).toMutableList()
            result.forEach { face->
                face.file = file
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(file.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(file.project)
                }
            }
            return result
        }

        fun getListFacesExt(fileExt: FileExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileId(fileExt.file.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            val personExtMap: MutableMap<String, PersonExt> = mutableMapOf()
            result.forEach { face->
                face.file = fileExt.file
                var person = if (personExtMap.containsKey(face.personRecognizedName)) personExtMap[face.personRecognizedName]?.person else {
                    if (face.personRecognizedName != "") {
                        PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                            face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                    } else {
                        PersonController.getUndefinded(fileExt.projectExt.project)
                    }
                }
                val personExt = PersonExt(person!!, fileExt.projectExt)
                personExtMap[face.personRecognizedName] = personExt
                face.person = person

                listFacesExt.add(FaceExt(face, fileExt , personExt))
            }
            return listFacesExt
        }

        /**
         * Лица серии, которые имеет смысл отправлять на распознавание.
         *
         * Лица с вытянутой рамкой **сюда не попадают**: их всё равно
         * отправят в «не персонаж», и сделано это будет после сравнения —
         * то есть сравнение оплачивается, а толку ноль. Раньше эта проверка
         * жила в применении результата, то есть в конце, и на прогоне E05
         * таких лиц был десятки тысяч. Теперь они отсекаются до постановки
         * в очередь и сразу получают персону «не персонаж», как и раньше,
         * только без сравнения.
         *
         * Проверка вытянутости — та же самая, что была в применении
         * результата: отношение большей стороны к меньшей больше 4.
         */
        fun getListFacesExtToRecognize(fileExt: FileExt): MutableList<FaceExt> {
            val undefindedPerson = PersonController.getUndefinded(fileExt.projectExt.project)
            val nonPersonPerson = PersonController.getNonperson(fileExt.projectExt.project)
            val result = Main.faceRepo.findFacesToRecognize(fileExt.file.id, undefindedPerson.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            val personExtMap: MutableMap<String, PersonExt> = mutableMapOf()
            val tooThin = mutableListOf<Face>()
            result.forEach { face->
                val width = face.endX - face.startX
                val height = face.endY - face.startY
                val ratio = if (width > height) width / height.toDouble() else height / width.toDouble()
                if (ratio > 4) {
                    face.person = nonPersonPerson
                    tooThin.add(face)
                    return@forEach
                }
                face.file = fileExt.file
                var person = if (personExtMap.containsKey(face.personRecognizedName)) personExtMap[face.personRecognizedName]?.person else {
                    if (face.personRecognizedName != "") {
                        PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                            face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                    } else {
                        undefindedPerson
                    }
                }
                val personExt = PersonExt(person!!, fileExt.projectExt)
                personExtMap[face.personRecognizedName] = personExt
                face.person = person

                listFacesExt.add(FaceExt(face, fileExt , personExt))
            }
            if (tooThin.isNotEmpty()) {
                Main.faceRepo.saveAll(tooThin)
                println("[FaceController] вытянутых рамок отправлено в «не персонаж» до распознавания: ${tooThin.size}")
            }
            return listFacesExt
        }

        fun getListFacesToTrain(project: Project): MutableList<Face> {
            return Main.faceRepo.getListFacesToTrain(project.id).toMutableList()
        }

        fun getListFacesExt(frameExt: FrameExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileIdAndFrameNumber(frameExt.fileExt.file.id, frameExt.frame.frameNumber).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            result.forEach { face->
                face.file = frameExt.fileExt.file
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(frameExt.fileExt.projectExt.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(frameExt.fileExt.projectExt.project)
                }
                listFacesExt.add(FaceExt(face, frameExt.fileExt, PersonExt(face.person, frameExt.fileExt.projectExt)))
            }
            return listFacesExt
        }

        /**
         * Карта «трек → кадр, с которого он начинается» по всему проекту.
         *
         * Нужна, чтобы лицо узнало свой трек и сортировка списка персоны
         * группировала лица по трекам. Забирается **один раз на загрузку
         * списка**, а не на каждое лицо: идентификатор ленивой связи
         * `Face.track` Hibernate отдаёт из прокси, без запроса к базе.
         *
         * Для списка одного файла берётся перегрузка ниже: треки остальных
         * серий этому списку не нужны, а их в проекте тысячи.
         */
        fun getTrackFirstFrameNumbers(projectExt: ProjectExt): Map<Long, Int> =
            Main.faceTrackRepo
                .findByProjectId(projectExt.project.id)
                .map { it.id to it.firstFrameNumber }
                .toMap()

        /** Карта «трек → кадр начала» по одному файлу. */
        fun getTrackFirstFrameNumbers(fileExt: FileExt): Map<Long, Int> =
            Main.faceTrackRepo
                .findByFileId(fileExt.file.id)
                .map { it.id to it.firstFrameNumber }
                .toMap()

        /**
         * Проставляет лицу ключ группировки по треку. Лицо без трека остаётся
         * на своём месте по времени.
         */
        private fun applyTrackOrder(
            faceExt: FaceExt,
            trackFirstFrameNumbers: Map<Long, Int>,
        ): FaceExt {
            val trackId = faceExt.face.track?.id
            faceExt.trackFirstFrameNumber = trackId?.let { trackFirstFrameNumbers[it] } ?: faceExt.frameNumber
            return faceExt
        }

        fun getListFacesExt(fileExt: FileExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByFileIdAndPersonId(fileExt.file.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            val trackFirstFrameNumbers = getTrackFirstFrameNumbers(fileExt)
            return result.map {
                it.file = fileExt.file
                it.person = personExt.person
                applyTrackOrder(FaceExt(it, fileExt, personExt), trackFirstFrameNumbers)
            }.toMutableList()

        }

        fun getListFacesExt(shotExt: ShotExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByShotIdAndPersonId(shotExt.shot.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            val trackFirstFrameNumbers = getTrackFirstFrameNumbers(shotExt.fileExt)
            val setFilesExt: MutableSet<FileExt> = mutableSetOf()
            return result.mapNotNull { face ->

                var fileId = 0L
                var fileExt: FileExt? = null

                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
                val stFaces = Main.connection.prepareStatement(sqlFaces)
                stFaces.setLong(1, face.id)
                val rsFaces = stFaces.executeQuery()
                while (rsFaces.next()) {
                    fileId = rsFaces.getLong("file_id")
                    break
                }
                if (fileId != 0L) {
                    fileExt = setFilesExt.firstOrNull { it.file.id == fileId }
                    if (fileExt == null) {
                        fileExt = FileController.getFileExt(fileId, shotExt.fileExt.projectExt.project)
                        setFilesExt.add(fileExt)
                    }
                    face.file = fileExt.file
                    face.person = personExt.person
                    applyTrackOrder(FaceExt(face, fileExt, personExt), trackFirstFrameNumbers)
                } else {
                    null
                }
            }.toMutableList()

        }

        fun getListFacesExt(projectExt: ProjectExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByProjectIdAndPersonId(projectExt.project.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            val trackFirstFrameNumbers = getTrackFirstFrameNumbers(projectExt)
            val setFilesExt: MutableSet<FileExt> = mutableSetOf()
            return result.mapNotNull { face ->

                var fileId = 0L
                var fileExt: FileExt? = null

                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
                val stFaces = Main.connection.prepareStatement(sqlFaces)
                stFaces.setLong(1, face.id)
                val rsFaces = stFaces.executeQuery()
                while (rsFaces.next()) {
                    fileId = rsFaces.getLong("file_id")
                    break
                }
                if (fileId != 0L) {
                    fileExt = setFilesExt.firstOrNull { it.file.id == fileId }
                    if (fileExt == null) {
                        fileExt = FileController.getFileExt(fileId, projectExt.project)
                        setFilesExt.add(fileExt)
                    }
                    face.file = fileExt.file
                    face.person = personExt.person
                    applyTrackOrder(FaceExt(face, fileExt, personExt), trackFirstFrameNumbers)
                } else {
                    null
                }
            }.toMutableList()

        }

        data class FrameToDetectFaces(val projectId: Long,
                                      val fileId: Long,
                                      val frameNumber: Int,
                                      val pathToFrameFile: String) {
        }

        fun getArrayFramesToDetectFaces(fileExt: FileExt): Array<FrameToDetectFaces> =
            getArrayFramesToDetectFaces(fileExt, getFramesToRecognize(fileExt))

        /**
         * Записи кадров для произвольного списка номеров — того же формата,
         * что и у frames.json. Нужен шагу перепроверки: детектор читает из
         * записи не только номер кадра, но и проект, файл и путь к
         * изображению, поэтому списка из одних номеров ему не хватает.
         */
        fun getArrayFramesToDetectFaces(fileExt: FileExt, listFrameNumbers: List<Int>): Array<FrameToDetectFaces> {
            val list: MutableList<FrameToDetectFaces> = mutableListOf() //<Frame>(listFrameNumbers.size)
            for (i in listFrameNumbers.indices) {
                list.add(FrameToDetectFaces(fileExt.projectExt.project.id,
                    fileExt.file.id,
                    listFrameNumbers[i],
                    "${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", listFrameNumbers[i])}.jpg"))
            }
            return list.toTypedArray()
        }

        fun getFramesToRecognize(fileExt: FileExt): List<Int> =
            getFramesToRecognize(fileExt.file.shots, fileExt.framesCount)

        /**
         * Правило шага выборки отдельно от файла.
         *
         * Отдельная версия нужна шагу отслеживания: он работает в фоновом
         * потоке, где сессии Hibernate нет, а file.shots подгружается
         * отложенно, и обращение к нему падает с LazyInitializationException.
         * Правило при этом должно быть одно и то же у обоих шагов, иначе
         * трекинг будет считать кандидатами кадры, которые детектор уже смотрел.
         */
        fun getFramesToRecognize(shotsIn: Collection<Shot>, countFrames: Int): List<Int> {
            var curr = 0
            val listFrames: MutableList<Int> = mutableListOf()
            val listShots: MutableList<Shot> = shotsIn.toMutableList()
            // По номеру кадра, а не штатным compareTo: те сравнивают по
            // file.order, а file подгружается отложенно. В вызывающем фоне
            // сессии Hibernate нет, и обращение к отложенному полю падает с
            // LazyInitializationException. Порядок по номеру кадра вдобавок
            // совпадает с прежним: сортировка сцен штатным compareTo тоже
            // идёт по file.order, а он задаёт порядок файла, а внутри файла
            // сцены идут по возрастанию номера кадра.
            listShots.sortBy { it.firstFrameNumber }

            for (shot in listShots) {
                val stepFrames = if (shot.lastFrameNumber - shot.firstFrameNumber < 15) 3
                                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 30) 5
                                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 60) 10 else 20
                var i: Int = shot.firstFrameNumber
                while (i < shot.lastFrameNumber) {
                    curr = i
                    if (curr <= countFrames) listFrames.add(curr)
                    i += stepFrames
                }
                if (curr < shot.lastFrameNumber) curr = shot.lastFrameNumber
                if (curr <= countFrames) listFrames.add(curr)
            }
            return listFrames
        }

        fun deleteAll(file: File) {
            Main.faceRepo.deleteAll(file.id)
        }

        fun getFace(fileId: Long, frameNumber: Int, faceNumber: Int): Face? {
            return Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(fileId, frameNumber, faceNumber).firstOrNull()
        }

        fun getFaceExt(fileId: Long, frameNumber: Int, faceNumber: Int, project: Project): FaceExt? {
            val face = getFace(fileId, frameNumber, faceNumber)
            if (face != null) {
                val file = project.files.first { it.id == fileId }
                face.file = file
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(file.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(file.project)
                }
                val projectExt = ProjectExt(project)
                val fileExt = FileExt(file, projectExt)
                return FaceExt(face, fileExt, PersonExt(face.person, fileExt.projectExt))
            }
            return null
        }

        fun getOverlayedFrame(frameExt: FrameExt, faceExt: FaceExt? = null, fullFrame: Boolean = false): BufferedImage? {

            var bi: BufferedImage? = null
            var fileId: Long = 0L
            var personId: Long = 0L
            var frameId: Long = 0L

            val fileExt = frameExt.fileExt
            val projectExt = frameExt.fileExt.projectExt
//            val personExt = faceExt!!.personExt

//            val sqlFrames = "select * from tbl_frames where file_id = ? and frame_number = ?"
//            val stFrames = Main.connection.prepareStatement(sqlFrames)
//            stFrames.setLong(1, fileExt.file.id)
//            stFrames.setInt(2, faceExt.face.frameNumber)
//            val rsFrames = stFrames.executeQuery()
//            while (rsFrames.next()) {
//                frameId = rsFrames.getLong("id")
//                break
//            }


//            val listFacesInCurrentFrame = Main.faceRepo.getListFacesInFrame(fileExt.file.id, frameExt.frame.frameNumber).toMutableList()
//            val listFacesExt: MutableList<FaceExt> = mutableListOf()
//            listFacesInCurrentFrame.forEach { face ->
//
//                face.file = fileExt.file
//
//                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
//                val stFaces = Main.connection.prepareStatement(sqlFaces)
//                stFaces.setLong(1, face.id)
//                val rsFaces = stFaces.executeQuery()
//                while (rsFaces.next()) {
//                    personId = rsFaces.getLong("person_id")
//                    break
//                }
//
//                if (personId != 0L) {
//
//                    val person = Main.personRepo.findById(personId).orElse(null)
//                    if (person != null) {
//                        val currentPersonExt = PersonExt(person, projectExt)
//                        val currentFaceExt = FaceExt(face,fileExt, currentPersonExt)
//                        listFacesExt.add(currentFaceExt)
//                    }
//                }
//            }

            val listFacesExt = frameExt.facesExt()

            bi = if (fullFrame) frameExt.biFull else frameExt.biMedium

            if (bi != null) {

                val frameWidth: Int = bi.width
                val frameHeight: Int = bi.height
                //TODO Брать ширину картинки из свойств файла
                val faceSourceFrameWidth = Main.FULL_FRAME_W
                val scaleFactor = frameWidth / faceSourceFrameWidth

                listFacesExt.forEach { faceExtInFrame ->


                    val startX = (scaleFactor * faceExtInFrame.startX).toInt()
                    val startY = (scaleFactor * faceExtInFrame.startY).toInt()
                    val endX = (scaleFactor * faceExtInFrame.endX).toInt()
                    val endY = (scaleFactor * faceExtInFrame.endY).toInt()

                    val opaque = 1.0f

                    var textColor = Color.YELLOW
                    if (faceExtInFrame.face.isManual) textColor = Color.RED
                    if (faceExt!=null && faceExt.face.id == faceExtInFrame.face.id && faceExt.face.isManual) {
                        textColor = Color.ORANGE
                    } else if (faceExt!=null && faceExt.face.id == faceExtInFrame.face.id && !faceExt.face.isManual) {
                        textColor = Color.GREEN
                    }

                    val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
                    val imageType = BufferedImage.TYPE_INT_ARGB
                    val textPosition = Pos.BOTTOM_CENTER
                    val textToOverlay = faceExtInFrame.personExt.person.name

                    val resultImage = BufferedImage(frameWidth, frameHeight, imageType)
                    val graphics2D = resultImage.graphics as Graphics2D
                    graphics2D.drawImage(bi, 0, 0, null)
                    val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opaque)
                    graphics2D.composite = alphaChannel

                    graphics2D.font = textFont
                    val fontMetrics = graphics2D.fontMetrics
                    val rect = fontMetrics.getStringBounds(textToOverlay, graphics2D)
                    val rectW = rect.width.toInt()
                    val rectH = rect.height.toInt()

                    var centerY = startY
                    centerY = if (centerY < 20) {
                        endY - startY + 25
                    } else {
                        centerY - 3
                    }
                    if (centerY > frameHeight) centerY = frameHeight - 5

                    graphics2D.color = Color.BLACK
                    graphics2D.fillRect(startX - 3, centerY - rectH, rectW + 6, rectH + 6)
                    graphics2D.color = textColor

                    graphics2D.drawString(textToOverlay, startX, centerY)
                    graphics2D.drawRect(startX, startY, endX - startX, endY - startY)
                    if (faceExt != null && faceExt.face.id == faceExtInFrame.face.id) {
                        graphics2D.drawRect(startX - 1, startY - 1, endX - startX + 2, endY - startY + 2)
                    }
                    graphics2D.dispose()
                    bi = resultImage
                }
            }


            return bi
        }

    }


}