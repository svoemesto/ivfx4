package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.utils.FaceDetection
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile

class RecognizeFaces(var fileExt: FileExt,
                     val table: TableView<FileExt>,
                     val textLbl1: String,
                     val numCurrentThread: Int,
                     val countThreads: Int,
                     var lbl1: Label, var pb1: ProgressBar,
                     var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        Platform.runLater {
            lbl1.isVisible = true
            pb1.isVisible = true
            lbl2.isVisible = true
            pb2.isVisible = true
        }

        Platform.runLater {
            lbl1.text = textLbl1
            pb1.progress = (numCurrentThread-1) / countThreads.toDouble()
            lbl2.text = "Recognizing faces..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val builder = GsonBuilder()
        val gson = builder.create()

        val pathToFileJSON: String = fileExt.folderFramesFull + IOFile.separator + "faces.json"
        val arrFrameFaces: Array<FaceExt> = FaceController.getListFacesExtToRecognize(fileExt).toTypedArray()

        arrFrameFaces.forEach {
            it.personId = 0
            it.personType = PersonType.UNDEFINDED.name
            it.personRecognizedName = ""
        }

        // В json добавляются уже отмеченные лица — они и есть галерея.
        //
        // Раньше распознавание обучалось моделью, и обучающая выборка
        // собиралась отдельным шагом, поэтому сюда попадали только
        // неопределённые лица. Теперь сравнение идёт напрямую с отмеченными,
        // и без них у скрипта не с чем сравнивать: галерея выходит пустой,
        // скрипт пишет об этом и заканчивает работу, не найдя никого.
        //
        // Отмеченные кладём с типом PERSON и с именем — по ним скрипт и
        // строит галерею. При чтении результата они пропускаются, чтобы
        // уже сделанные назначения не перетирались (см. ниже).
        val undefinded = PersonController.getUndefinded(fileExt.projectExt.project)
        // Отбор по ТИПУ персоны, а не по «не тот неопределённый».
        //
        // Прежний отбор брал всё, что не является неопределённым, и в галерею
        // попадали лица типа NONPERSON — то есть вырезы, которые приложение
        // само отметило как «не персонаж». Сравнивать новые лица с ними
        // бессмысленно: это как раз тот мусор, который распознавание должно
        // отбрасывать, а не искать в нём похожих.
        //
        // Галерея берётся ПО ПРОЕКТУ, а не по файлу. Серии одного сериала
        // показывают одних и тех же людей, и отмеченные лица первой серии —
        // образцы для второй. По файлу у новой серии своих отмеченных лиц
        // нет: галерея выходит пустой, шаг останавливается, и все лица
        // остаются неопределёнными, хотя их сколько угодно много.
        //
        // Обход идёт парами «серия + персона», и это не удобство, а
        // необходимость: конструктор FaceExt читает file.shortName, а связь
        // file отложенная, и вне сессии её чтение падает. Отсюда две ошибки
        // подряд, обе в этом шаге: сперва personType, потом shortName.
        // Чтобы их больше не было, отложенные связи подменяются настоящими
        // объектами до создания FaceExt, а нужные значения (personType у
        // персоны, personRecognizedName у лица) — обычные колонки, читаются
        // без сессии.
        //
        // У каждой серии своя папка кадров, поэтому у лица галереи должен
        // быть И ЕГО FileExt, а не тот, на котором идёт распознавание: иначе
        // пути в json указывали бы на чужую серию.
        val projectId = fileExt.projectExt.project.id
        val galleryPersons = Main.personRepo.findByProjectId(projectId)
            .filter { it.personType == PersonType.PERSON && it.id != undefinded.id }
        val galleryFaces = mutableListOf<FaceExt>()
        for (galleryFile in Main.fileRepo.findByProjectId(projectId)) {
            val galleryFileExt = FileExt(galleryFile, fileExt.projectExt)
            for (person in galleryPersons) {
                for (face in Main.faceRepo.findAllByFileIdAndPersonId(galleryFile.id, person.id)) {
                    face.file = galleryFile
                    face.person = person
                    if (face.personRecognizedName == "") continue
                    galleryFaces.add(FaceExt(face, galleryFileExt, PersonExt(person, fileExt.projectExt)))
                }
            }
        }
        galleryFaces.forEach {
            it.personType = PersonType.PERSON.name
            it.personRecognizedName = it.face.personRecognizedName
        }
        println("[RecognizeFaces] неопределённых лиц: ${arrFrameFaces.size}, " +
                "отмеченных для галереи: ${galleryFaces.size}")

        // Галерея пуста — сравнивать не с чем. Действие не запускаем: иначе
        // приложение двадцать минут перебирает все неопределённые лица, ни
        // разу ничего не находя, и всё это время надпись сообщает, что идёт
        // распознавание. Похоже на работу, результата ноль.
        if (galleryFaces.isEmpty()) {
            println("[RecognizeFaces] ОСТАНОВ: нет ни одного отмеченного лица, " +
                    "галерея пуста. Распознавать нечего.")
            Platform.runLater {
                lbl1.isVisible = false
                lbl2.text = "Нечего распознавать: не отмечено ни одного лица"
                table.refresh()
            }
            return
        }

        // Неопределённых лиц нет — не с кем сравнивать. Зеркально к проверке
        // выше, и по той же причине: без этой проверки приложение писало json
        // на двадцать с лишним тысяч отмеченных лиц, запускало Python и
        // читало json обратно, чтобы не обновить ничего.
        // Проверено 2026-10-08 на E03: два прогона подряд после ручной
        // разметки дали «распознано: 0, отклонено: 0, отклонено: 0».
        if (arrFrameFaces.isEmpty()) {
            println("[RecognizeFaces] ОСТАНОВ: неопределённых лиц нет.")
            Platform.runLater {
                lbl1.isVisible = false
                lbl2.text = "Нечего распознавать: в серии нет неопределённых лиц"
                table.refresh()
            }
            return
        }

        try {
            FileWriter(pathToFileJSON).use { fileWriter -> gson.toJson(arrFrameFaces + galleryFaces, fileWriter) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("cd \"${faceDetectorPath}\"\n")
        param.add(FaceDetection.PYTHON_PATH)
        param.add("${faceDetectorPath}/recognize_faces.py")
        param.add("-i")
        param.add("${pathToFileJSON}")
        // Модели больше не передаются: распознавание идёт сравнением векторов
        // уже найденных лиц с отмеченными, обучать модель не нужно, поэтому
        // ни детектор, ни эмбеддер, ни pickle с SVM в команде не участвуют.
        param.add("-c")
        // Порог косинусного сходства. Прежние 0,3 относились к вероятности
        // SVM, здесь шкала другая: значение — насколько лицо похоже на
        // отмеченное. 0,45 — осторожная отметка, её нужно откалибровать на
        // реальных лицах серии.
        param.add(FaceDetection.RECOGNIZE_THRESHOLD.toString())
        param.add("-m")
        param.add(FaceDetection.RECOGNIZE_MARGIN.toString())

        val cmdText = param.joinToString(separator = " ")

        println(cmdText)

        val runCmd = RunCmd(cmdText)
        runCmd.run()

        // Приложение не читает вывод скрипта, поэтому результат
        // распознавания без этого остаётся невидимым: ни в журнале, ни в
        // форме. Скрипт пишет итог в файл, здесь он подхватывается и
        // показывается в надписи и в журнал.
        val resultFile = IOFile(fileExt.folderFramesFull + IOFile.separator + "recognize_faces_result.txt")
        val resultJsonFile = IOFile(fileExt.folderFramesFull + IOFile.separator + "recognize_faces_result.json")
        val scriptFailed = !resultFile.exists() || !resultJsonFile.exists()
        val resultText = if (scriptFailed) "РАСПОЗНАВАНИЕ НЕ СОСТОЯЛОСЬ: скрипт не дал результата" else resultFile.readText().trim()
        println("[RecognizeFaces] $resultText")
        Platform.runLater { lbl2.text = resultText }
        resultFile.delete()

        // Дальше читается json с результатом, и лица перезаписываются
        // ВСЕГДА, даже если скрипт упал. При провале в нём лежат те же
        // неопределённые лица, и приложение двадцать минут перезаписывает их
        // вхолостую, показывая растущий счётчик, — выглядит как работа, а
        // результата ноль. Поэтому при провале останавливаемся здесь, а не идём
        // по кругу.
        if (scriptFailed) {
            resultJsonFile.delete()
            Platform.runLater {
                lbl1.isVisible = false
                lbl2.text = resultText
            }
            return
        }

        // Читается не входной faces.json, а отдельный файл результата, куда
        // скрипт пишет ТОЛЬКО распознанные лица. Входной файл — это
        // разметка, 492 МБ, из которых 69 % галерея; читать его обратно
        // было второй разцией за один и тот же файл.
        //
        // Применяются только распознанные. Прежде сюда попадали все
        // неопределённые лица очереди, и каждое проходило поиск в базе и
        // сохранение без единого изменения: нераспознанное лицо и так
        // остаётся неопределённым, а перезапись стоила запроса на лицо.
        try {
            val facesExtJsonArray: Array<FaceExtJson> =
                gson.fromJson(resultJsonFile.readText(), Array<FaceExtJson>::class.java)
            resultJsonFile.delete()
            val nonPerson = PersonController.getNonpersonExt(fileExt.projectExt)
            val undefindedPerson = PersonController.getUndefindedExt(fileExt.projectExt)
            for ((i, faceExtJson) in facesExtJsonArray.withIndex()) {

                val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
                val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
                val percentage2: Double = if (facesExtJsonArray.isEmpty()) 1.0 else (i+1)/facesExtJsonArray.size.toDouble()
                val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
                Platform.runLater {
                    lbl1.text = textLbl1
                    pb1.progress = percentage1
                    lbl2.text = "Recognize face [$i/${facesExtJsonArray.size}]"
                    pb2.progress = percentage2
                }

                FaceController.createOrUpdate(faceExtJson, fileExt, undefindedPerson, nonPerson)

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        fileExt.hasRecognizedFaces = true

        Platform.runLater {
            table.refresh()
            lbl1.isVisible = false
            lbl2.text = "Done"
        }

    }
}