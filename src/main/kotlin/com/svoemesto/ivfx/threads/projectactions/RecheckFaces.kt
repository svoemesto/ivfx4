package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.utils.FaceDetection
import com.svoemesto.ivfx.threads.RunCmd
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.util.Duration
import java.io.FileReader
import java.io.IOException
import java.io.File as IOFile

/**
 * Шаг перепроверки: ищет лица в кадрах, которые детектор пропустил при выборке.
 *
 * Зачем. Шаг отслеживания отбирает кадры, лежащие внутри окна присутствия
 * человека, но не попавшие в выборку детектора: между соседними проверенными
 * кадрами проходит до 0,83 секунды, и лицо, показанное на полсекунды, не
 * находилось никогда — и не могло быть найдено вручную, потому что его не
 * было в списке. На этой серии таких кадров 65 928 против 81 415 всех
 * пропущенных.
 *
 * Почему это отдельный шаг, а не повторный DF. Обычный DF перезаписывает
 * faces.json, а следующий за ним шаг создания лиц читает этот файл и
 * безусловно сбрасывает в неопределённые те лица, у которых пустое имя
 * распознавания. Свежий прогон детектора пишет именно такие лица, то есть
 * повторный DF вместе с CF вернул бы в базу все вручную размеченные лица как
 * неопределённые — молча, без ошибки. Поэтому здесь результат пишется в
 * отдельный файл, а в базу лица только добавляются: у уже существующих
 * ничего не меняется, включая персону.
 *
 * Пересечений с уже найденными лицами не бывает: кандидаты отбираются как раз
 * те кадры, которые детектор не смотрел, поэтому в них заведомо нет ни одного
 * лица и нумерация начинается с нуля без столкновений. Но проверка на
 * существование всё же делается — на случай ручной правки списка.
 */
class RecheckFaces(var fileExt: FileExt,
                   val textLbl1: String,
                   val numCurrentThread: Int,
                   val countThreads: Int,
                   var lbl1: Label, var pb1: ProgressBar,
                   var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    companion object {
        /** Список кадров, оставляемых шагом отслеживания. */
        const val CANDIDATES_FILE = "recheck_frames.txt"

        /** Куда пишет скрипт найденное. Отдельное имя — обязательное условие,
         *  а не украшение: перезапись faces.json снесла бы разметку. */
        const val RESULT_FILE = "faces_recheck.json"

        /** Список кадров в формате frames.json. Шаг отслеживания пишет
         *  номера построчно, а детектору нужны ещё проект, файл и путь к
         *  изображению, поэтому список пересобирается в тот же формат. */
        const val LIST_FILE = "recheck_frames.json"
    }

    override fun run() {

        Platform.runLater {
            lbl1.isVisible = true
            pb1.isVisible = true
            lbl2.isVisible = true
            pb2.isVisible = true
            lbl1.text = textLbl1
            pb1.progress = if (countThreads <= 1) 0.0 else (numCurrentThread - 1) / countThreads.toDouble()
            lbl2.text = "Preparing ..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val pathToCandidates = IOFile(fileExt.folderFramesFull, CANDIDATES_FILE)
        if (!pathToCandidates.exists()) {
            // Список появляется после шага отслеживания. Без него перепроверять
            // нечего, и молча прогонять детектор по всей серии нельзя: это
            // несколько часов работы вместо получаса.
            val text = "Нет списка $CANDIDATES_FILE — сначала выполните шаг [TR] Track faces"
            Platform.runLater { pb2.progress = 1.0; lbl2.text = "Failed"; lbl1.text = text }
            println("[RC] $text")
            return
        }
        val candidateNumbers = pathToCandidates.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }
        if (candidateNumbers.isEmpty()) {
            val text = "Список $CANDIDATES_FILE пуст — перепроверять нечего"
            Platform.runLater { pb2.progress = 1.0; lbl2.text = "Failed"; lbl1.text = text }
            println("[RC] $text")
            return
        }
        println("[RC] кадров на перепроверку: ${candidateNumbers.size}")

        // Пересобираем список в формате frames.json: детектору из записи
        // нужны проект, файл и путь к изображению, а не только номер кадра.
        val gson0 = GsonBuilder().create()
        val arrCandidates = FaceController.getArrayFramesToDetectFaces(fileExt, candidateNumbers)
        val pathToListJson = IOFile(fileExt.folderFramesFull, LIST_FILE)
        try {
            pathToListJson.writeText(gson0.toJson(arrCandidates))
        } catch (e: IOException) {
            val text = "Список не записан: ${e.javaClass.simpleName}"
            Platform.runLater { pb2.progress = 1.0; lbl2.text = "Failed"; lbl1.text = text }
            println("[RC] $text")
            return
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH
        val param: MutableList<String> = mutableListOf()
        param.add("cd \"${faceDetectorPath}\"\n")
        param.add(FaceDetection.PYTHON_PATH)
        param.add("${faceDetectorPath}/detect_faces_in_folder.py")
        param.add("-i")
        param.add("${fileExt.folderFramesFull}")
        param.add("-o")
        param.add("${fileExt.folderFacesFull}")
        param.add("-d")
        // Модели лежат в кеше на машине, а не в ресурсах — см. DetectFaces.
        param.add(FaceDetection.MODELS_DIR)
        param.add("-m")
        param.add(FaceDetection.modelPath("w600k_r50.onnx"))
        // Ключевое отличие от обычного DF: свой список кадров и свой файл
        // результата.
        param.add("-l")
        param.add(pathToListJson.absolutePath)
        param.add("-f")
        param.add(RESULT_FILE)
        param.add("-c")
        param.add(0.5.toString())

        val cmdText = param.joinToString(separator = " ")
        println(cmdText)

        val progressFile = IOFile(fileExt.folderFramesFull + IOFile.separator + "detect_faces_progress.txt")
        progressFile.delete()

        val poller = Timeline(KeyFrame(Duration.millis(500.0), EventHandler {
            try {
                if (progressFile.exists()) {
                    val parts = progressFile.readText().trim().split(" ")
                    if (parts.size >= 3) {
                        val done = parts[0].toDouble()
                        val total = parts[1].toDouble()
                        val faces = parts[2].toInt()
                        if (total > 0) {
                            val percent = (done / total * 100).toInt()
                            Platform.runLater {
                                pb2.progress = done / total
                                lbl2.text = "Recheck: ${done.toInt()} / ${total.toInt()} ($percent%), found: $faces"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // файла ещё нет в первые доли секунды — это нормально
            }
        }))
        poller.cycleCount = Timeline.INDEFINITE
        poller.play()
        RunCmd(cmdText).run()
        poller.stop()
        progressFile.delete()

        addNewFaces()

        Platform.runLater { pb2.progress = 1.0; lbl2.text = "Done" }
    }

    /**
     * Добавляет найденные лица в базу. Существующие не трогает: у них
     * персона, заданная вручную, и обновлять её нельзя ни при каких
     * обстоятельствах. Проверка идёт по паре «номер кадра + номер лица в
     * кадре» — это то, чем шаг создания лиц различает лицо в кадре.
     */
    private fun addNewFaces() {

        val gson = GsonBuilder().create()
        val pathToResult = IOFile(fileExt.folderFramesFull, RESULT_FILE)

        var added = 0
        var skipped = 0

        try {
            FileReader(pathToResult).use { fileReader ->
                val facesExtJsonArray: Array<FaceExtJson> = gson.fromJson(fileReader, Array<FaceExtJson>::class.java)

                // Существующие лица читаем один раз целиком: по одному запросу
                // на каждое найденное лицо при десятках тысяч лиц это десятки
                // тысяч обращений к базе.
                val existing = mutableSetOf<String>()
                for (f in Main.faceRepo.findByFileId(fileExt.file.id)) {
                    existing.add("${f.frameNumber}|${f.faceNumberInFrame}")
                }

                val undefindedPerson = PersonController.getUndefinded(fileExt.projectExt.project)

                for ((i, faceExtJson) in facesExtJsonArray.withIndex()) {

                    val key = "${faceExtJson.frameNumber}|${faceExtJson.faceNumberInFrame}"
                    if (existing.contains(key)) {
                        skipped++
                        continue
                    }

                    val total = facesExtJsonArray.size.toDouble()
                    val done = i.toDouble() / total
                    val text = "Add faces [${i + 1}/${facesExtJsonArray.size}]"
                    Platform.runLater { pb2.progress = done; lbl2.text = text }

                    val face = Face()
                    face.isExample = false
                    face.isManual = false
                    face.file = fileExt.file
                    // Новые лица всегда неопределённые: на этом шаре никакого
                    // распознавания ещё не было, а выдумывать имя нельзя.
                    face.person = undefindedPerson
                    face.frameNumber = faceExtJson.frameNumber
                    face.faceNumberInFrame = faceExtJson.faceNumberInFrame
                    face.personRecognizedName = ""
                    face.recognizeProbability = faceExtJson.recognizeProbability
                    face.startX = faceExtJson.startX
                    face.startY = faceExtJson.startY
                    face.endX = faceExtJson.endX
                    face.endY = faceExtJson.endY
                    face.vector = faceExtJson.vector

                    FaceController.save(face)
                    existing.add(key)
                    added++
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            val text = "Результат не прочитан: ${e.javaClass.simpleName}"
            Platform.runLater { pb2.progress = 1.0; lbl2.text = "Failed"; lbl1.text = text }
            println("[RC] $text")
            return
        }

        println("[RC] добавлено лиц: $added, пропущено уже существующих: $skipped")
        Platform.runLater { lbl1.text = "$textLbl1: добавлено $added лиц, пропущено $skipped" }
    }
}