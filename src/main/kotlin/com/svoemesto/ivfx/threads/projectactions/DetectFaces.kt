package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.utils.FaceDetection
import com.svoemesto.ivfx.utils.Trace
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import javafx.util.Duration
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile

class DetectFaces(
    var fileExt: FileExt,
    val table: TableView<FileExt>,
    val textLbl1: String,
    val numCurrentThread: Int,
    val countThreads: Int,
    var lbl1: Label,
    var pb1: ProgressBar,
    var lbl2: Label,
    var pb2: ProgressBar,
) : Thread(),
    Runnable {
    override fun run() {
        // Видимость элементов формы меняется только из потока интерфейса.
        // Этот класс работает в отдельном потоке, и прямое присваивание
        // isVisible из него JavaFX не применяет: форма остаётся пустой, а
        // поиск лиц при этом идёт. Поэтому всё, что касается формы, идёт
        // через Platform.runLater — и в начале, и в конце.
        Platform.runLater {
            lbl1.isVisible = true
            pb1.isVisible = true
            lbl2.isVisible = true
            pb2.isVisible = true
            lbl1.text = textLbl1
            pb1.progress = (numCurrentThread - 1) / countThreads.toDouble()
            lbl2.text = "Detecting faces: starting..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val builder = GsonBuilder()
        var gson = builder.create()
        val pathToFileJSON: String = fileExt.folderFramesFull + IOFile.separator + "frames.json"

        fileExt.file.shots = ShotController.getSetShots(fileExt.file)
        val arrFrameToDetectFaces: Array<FaceController.Companion.FrameToDetectFaces> = FaceController.getArrayFramesToDetectFaces(fileExt)

        try {
            FileWriter(pathToFileJSON).use { fileWriter -> gson.toJson(arrFrameToDetectFaces, fileWriter) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("cd \"${faceDetectorPath}\"\n")
        param.add(FaceDetection.PYTHON_PATH)
        param.add("$faceDetectorPath/detect_faces_in_folder.py")
        param.add("-i")
        param.add("${fileExt.folderFramesFull}")
        param.add("-o")
        param.add("${fileExt.folderFacesFull}")
        param.add("-d")
        // Детектор и распознаватель лежат не в ресурсах, а в кеше на
        // машине: 184 МБ моделей в git — несоразмерно. Каталог достаётся
        // через FaceDetection.MODELS_DIR, скачивание — tools/fetch-models.sh.
        param.add(FaceDetection.MODELS_DIR)
        param.add("-m")
        // Раньше здесь была модель openface_nn4.small2.v1.t7 в формате
        // TorchScript: она грузилась только через cv2.dnn.readNetFromTorch,
        // которого нет в OpenCV 5. Сейчас это ArcFace R50, и она работает
        // через тот же onnxruntime, что и детектор.
        param.add(FaceDetection.modelPath("w600k_r50.onnx"))
        param.add("-c")
        // Порог 0,5 — на нём проводилось сравнение с YuNet: при 0,5 медианная
        // достоверность находимых лиц 0,79, а ложные срабатывания на
        // текстурах уходили вниз.
        param.add(0.5.toString())

        val cmdText = param.joinToString(separator = " ")

        println(cmdText)

        val runCmd = RunCmd(cmdText)

        // На серии в 88643 кадра поиск лиц идёт около часа, и всё это время
        // форма показывала безликое «Detecting faces...» с крутящейся
        // полосой: отличить работу от зависания было нечем. Скрипт пишет
        // файл прогресса, здесь он опрашивается и показывает настоящие
        // числа.
        val progressFile = IOFile(fileExt.folderFramesFull + IOFile.separator + "detect_faces_progress.txt")
        progressFile.delete()

        val poller =
            Timeline(
                KeyFrame(
                    Duration.millis(500.0),
                    EventHandler {
                        try {
                            val parts = progressFile.readText().trim().split(" ")
                            if (parts.size >= 3) {
                                val done = parts[0].toDouble()
                                val total = parts[1].toDouble()
                                val faces = parts[2].toInt()
                                if (total > 0) {
                                    // В журнал пишем то же, что в подпись: детектор —
                                    // внешний процесс, и иначе нельзя отличить работу
                                    // от зависания по одному факту, что окно открыто.
                                    Trace.progress("DF", done.toLong(), total.toLong(), "найдено лиц: $faces")
                                    val percent = (done / total * 100).toInt()
                                    Platform.runLater {
                                        pb2.progress = done / total
                                        lbl2.text = "Detecting faces: ${done.toInt()} / ${total.toInt()} ($percent%), found: $faces"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // файла ещё нет в первые доли секунды — это нормально,
                            // показывать тогда нечего
                        }
                    },
                ),
            )
        poller.cycleCount = Timeline.INDEFINITE
        poller.play()

        runCmd.run()

        poller.stop()
        progressFile.delete()

        // Прерванная операция не отмечает файл выполненной.
        //
        // После отмены поток операции возвращается из runCmd.run() — он не
        // умирает, а просто доходит до следующей строки. Стоявший здесь
        // println ничего не значил, а вот `hasDetectedFaces = true` ставилось
        // **всегда**, то есть и после отмены: скрипт не дошёл до конца, а файл
        // уже считался обработанным. Дальше пайплайн по этому флагу решал бы,
        // что детекция выполнена.
        if (currentThread().isInterrupted) {
            Trace.fail("операция DetectFaces", "прервана, файл не отмечен обработанным")
            currentThread().interrupt()
            return
        }

        fileExt.hasDetectedFaces = true

        // таблица и форма обновляются тоже из потока интерфейса — по той же
        // причине, что и в начале
        Platform.runLater {
            table.refresh()
            // Прячем только текущий файл, полосы остаются на месте: иначе
            // после завершения их снова не видно и непонятно, отработало
            // действие или нет. Оставляем надпись с результатом.
            lbl1.isVisible = false
            lbl2.text = "Done"
        }
    }
}
