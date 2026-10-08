package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.utils.Trace
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import java.io.FileReader
import java.io.IOException
import java.io.File as IOFile

class CreateFaces(
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
        // Длительности фаз. Без них на вопрос «сколько заняло создание лиц»
        // ответа нет, а операция самая крупная по объёму: она применяет все
        // найденные лица серии, а не только распознанные.
        val startedAt = System.currentTimeMillis()

        // Всё, что трогает интерфейс, — только через Platform.runLater.
        // Прямое обращение из рабочего потока даёт IllegalStateException:
        // создание лиц всегда обрывалось на этом в самом конце, когда надписи
        // уже на экране и смена видимости идёт в сцену.
        Platform.runLater {
            lbl1.isVisible = true
            pb1.isVisible = true
            lbl2.isVisible = true
            pb2.isVisible = true
        }

        val builder = GsonBuilder()
        val gson = builder.create()

        val pathToJsonFaces = fileExt.folderFramesFull + IOFile.separator + "faces.json"

        var countBlocks = 1
        var currentBlock = 1
        // Отсчёт для применения и для числа лиц — их нет вне блока чтения.
        var applyStartedAt = startedAt
        var facesCount = 0

        try {
            FileReader(pathToJsonFaces).use { fileReader ->
                val facesExtJsonArray: Array<FaceExtJson> = gson.fromJson(fileReader, Array<FaceExtJson>::class.java)
                val jsonReadAt = System.currentTimeMillis()
                println("[CreateFaces] прочитано лиц из json: ${facesExtJsonArray.size}, чтение: ${jsonReadAt - startedAt} мс")
                val nonPerson = PersonController.getNonpersonExt(fileExt.projectExt)
                val undefindedPerson = PersonController.getUndefindedExt(fileExt.projectExt)
                val initProgress1: Double = (numCurrentThread - 1) / (countThreads.toDouble())
                val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
                // Применение идёт целиком в одной транзакции: по лицу на
                // транзакцию означало, что каждое ленивое чтение открывало свою
                // сессию, и на больших сериях это стоило минут.
                facesCount = facesExtJsonArray.size
                applyStartedAt = System.currentTimeMillis()
                FaceController.createOrUpdateAll(facesExtJsonArray.toList(), fileExt, undefindedPerson, nonPerson) { i, total ->
                    Trace.progress("CF", i.toLong(), total.toLong())
                    val percentage2: Double =
                        ((currentBlock - 1) + i / total.toDouble()) / countBlocks.toDouble()
                    val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
                    Platform.runLater {
                        lbl1.text = textLbl1
                        pb1.progress = percentage1
                        lbl2.text = "Create face [$i/$total]"
                        pb2.progress = percentage2
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val finishedAt = System.currentTimeMillis()
        val applyMillis = finishedAt - applyStartedAt
        println(
            "[CreateFaces] применение: $applyMillis мс, " +
                "на лицо: ${if (facesCount > 0) applyMillis / facesCount else 0} мс, " +
                "прогон целиком: ${finishedAt - startedAt} мс",
        )

        fileExt.hasCreatedFaces = true
//        fileExt.hasCreatedFacesString = "✓"
        table.refresh()

        val doneText = "Done, ${finishedAt - startedAt} мс"
        Platform.runLater {
            lbl2.text = doneText
            lbl1.isVisible = false
            lbl2.isVisible = false
            pb1.isVisible = false
            pb2.isVisible = false
        }
    }
}
