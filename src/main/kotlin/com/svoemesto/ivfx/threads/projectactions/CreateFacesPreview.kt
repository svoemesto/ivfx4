package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import java.io.FileReader
import java.io.IOException
import java.io.File as IOFile

class CreateFacesPreview(var fileExt: FileExt,
                         val table: TableView<FileExt>,
                         val textLbl1: String,
                         val numCurrentThread: Int,
                         val countThreads: Int,
                         var lbl1: Label, var pb1: ProgressBar,
                         var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        lbl1.isVisible = true
        pb1.isVisible = true
        lbl2.isVisible = true
        pb2.isVisible = true

        val builder = GsonBuilder()
        val gson = builder.create()

        val pathToJsonFaces = fileExt.folderFramesFull + IOFile.separator + "faces.json"

        var countBlocks = 1
        var currentBlock = 1

        Platform.runLater {
            lbl1.text = textLbl1
            lbl2.text = "Loading faces ..."
            pb2.progress = -1.0
        }

        val facesExt = FaceController.getListFacesExt(fileExt)
        for ((i, faceExt) in facesExt.withIndex()) {
            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/facesExt.size.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Create face preview [$i/${facesExt.size}]"
                pb2.progress = percentage2
            }

            faceExt.previewSmall

        }

        fileExt.hasCreatedFacesPreview = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}