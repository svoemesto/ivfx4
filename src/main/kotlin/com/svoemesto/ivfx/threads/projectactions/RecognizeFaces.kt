package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Person
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
import java.util.concurrent.atomic.AtomicBoolean

class RecognizeFaces(var fileExt: FileExt,
                     val table: TableView<FileExt>,
                     val textLbl1: String,
                     val numCurrentThread: Int,
                     val countThreads: Int,
                     var lbl1: Label, var pb1: ProgressBar,
                     var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        // Длительности этапов. Без них на вопрос «сколько заняло
        // распознавание» отвечать нечем: в журнале у прогона не было ни
        // времени, ни единой отметки о ходе.
        val startedAt = System.currentTimeMillis()

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
        // Галерея собирается ОДНИМ запросом, а не обходом «серия × персона».
        //
        // Обход давал 700 запросов на проект из 10 серий и 70 персон, и он
        // повторялся заново для каждой серии подряд. Здесь три запроса:
        // файлы, персоны и сами лица галереи.
        //
        // Условия отбора те же, что были в коде, перенесены в SQL буквально
        // (см. FaceRepo). Расхождение с прежним отбором поменяло бы состав
        // галереи молча, и распознавание стало бы хуже без единой ошибки.
        //
        // У каждого лица отложенные связи file и person заменяются
        // настоящими объектами: идентификатор ленивого прокси Hibernate
        // отдаёт без запроса, а чтение shortName конструктором FaceExt без
        // подмены падает.
        val projectId = fileExt.projectExt.project.id
        val galleryFiles: Map<Long, File> = Main.fileRepo.findByProjectId(projectId).associateBy { it.id }
        val galleryPersons: Map<Long, Person> = Main.personRepo.findByProjectId(projectId).associateBy { it.id }
        val galleryFileExts: MutableMap<Long, FileExt> = mutableMapOf()
        val galleryPersonExts: MutableMap<Long, PersonExt> = mutableMapOf()
        val galleryFaces = Main.faceRepo
            .findGalleryByProjectId(projectId, PersonType.PERSON.order, undefinded.id)
            .map { face ->
                val galleryFile = galleryFiles.getValue(face.file.id)
                val galleryPerson = galleryPersons.getValue(face.person.id)
                val faceExt = galleryFileExts.getOrPut(galleryFile.id) { FileExt(galleryFile, fileExt.projectExt) }
                val personExt = galleryPersonExts.getOrPut(galleryPerson.id) { PersonExt(galleryPerson, fileExt.projectExt) }
                face.file = galleryFile
                face.person = galleryPerson
                FaceExt(face, faceExt, personExt)
            }
            .toMutableList()
        galleryFaces.forEach {
            it.personType = PersonType.PERSON.name
            it.personRecognizedName = it.face.personRecognizedName
        }
        val galleryReadyAt = System.currentTimeMillis()
        println("[RecognizeFaces] неопределённых лиц: ${arrFrameFaces.size}, " +
                "отмеченных для галереи: ${galleryFaces.size}")
        println("[RecognizeFaces] сбор галереи: ${galleryReadyAt - startedAt} мс")

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

        // Пока идёт скрипт, приложение читает ход его работы из отдельного
        // файла. Раньше здесь было чёрное окно на десятки секунд: полоса
        // стояла в неопределённом состоянии, и непонятно было, работает
        // приложение или зависло. Скрипт отдаёт ход файлом, потому что его
        // вывод на консоль приложение не читает — читает только код
        // возврата, и то лишь для того, чтобы напечатать его при ошибке.
        val progressFile = IOFile(fileExt.folderFramesFull + IOFile.separator + "recognize_faces_progress.txt")
        val stopWatching = AtomicBoolean(false)
        val progressWatcher = Runnable({ watchProgress(progressFile, stopWatching, pb2, lbl2) })
        val watcher = Thread(progressWatcher, "RecognizeFaces-progress")
        val runCmd = RunCmd(cmdText)
        val scriptStartedAt = System.currentTimeMillis()
        runCmd.run()
        val scriptFinishedAt = System.currentTimeMillis()
        stopWatching.set(true)
        progressFile.delete()

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
            // Применение идёт целиком в одной транзакции: по лицу на
            // транзакцию означало, что каждое ленивое чтение открывало свою
            // сессию. На прогоне E05 это дало 21,5 секунды на 400 лиц.
            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            FaceController.createOrUpdateAll(facesExtJsonArray.toList(), fileExt, undefindedPerson, nonPerson) { i, total ->
                val percentage2: Double = if (total == 0) 1.0 else i/total.toDouble()
                val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
                Platform.runLater {
                    lbl1.text = textLbl1
                    pb1.progress = percentage1
                    lbl2.text = "Recognize face [$i/$total]"
                    pb2.progress = percentage2
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        fileExt.hasRecognizedFaces = true

        val finishedAt = System.currentTimeMillis()
        val timings =
            "[RecognizeFaces] скрипт: ${scriptFinishedAt - scriptStartedAt} мс, " +
                "применение: ${finishedAt - scriptFinishedAt} мс, " +
                "прогон целиком: ${finishedAt - startedAt} мс"
        println(timings)

        Platform.runLater {
            table.refresh()
            lbl1.isVisible = false
            lbl2.text = "Done, ${finishedAt - startedAt} мс"
        }
    }

    /**
     * Следит за ходом скрипта, пока он работает.
     *
     * Отдельный метод, а не лямбда внутри Thread(...): лямбда в аргументе
     * заставляет ktlint переформатировать всё её тело, и файл начинает
     * отличаться от своего же стиля в двадцати местах.
     *
     * Ошибки чтения проглатываются намеренно: надпись и полоса — не повод
     * уронить прогон, следующая проверка прочитает файл целиком.
     */
    private fun watchProgress(
        progressFile: IOFile,
        stop: AtomicBoolean,
        pb: ProgressBar,
        lbl: Label,
    ) {
        while (!stop.get()) {
            try {
                if (progressFile.exists()) {
                    val raw = progressFile.readText()
                    val parts = raw.trim().split(" ", "\n", "\r").filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        val done = parts[0].toInt()
                        val total = parts[1].toInt()
                        if (total > 0) {
                            val percentage = done.toDouble() / total
                            Platform.runLater {
                                lbl.text = "Recognize face [$done/$total]"
                                pb.progress = percentage
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                return
            }
        }
    }
}
