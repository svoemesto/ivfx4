package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.fxcontrollers.ShotsEditFXController
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.FileReader
import java.io.IOException
import javax.imageio.ImageIO
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
            lbl2.text = "Loading frames ..."
            pb2.progress = -1.0
        }
        val listFramesExt: ObservableList<FrameExt> = FXCollections.observableArrayList()
        LoadListFramesExt(listFramesExt, fileExt, pb2, lbl2).run()

        Platform.runLater {
            lbl1.text = textLbl1
            lbl2.text = "Loading faces ..."
            pb2.progress = -1.0
            lbl2.isVisible = true
            pb2.isVisible = true
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
                lbl2.isVisible = true
                pb2.isVisible = true
            }

            if (!IOFile(faceExt.pathToPreviewFile).exists()) {
                if (!IOFile(faceExt.pathToPreviewFile).parentFile.exists()) IOFile(faceExt.pathToPreviewFile).parentFile.mkdir()
                var frameExt = listFramesExt.firstOrNull { it.fileExt.file.id == faceExt.fileId &&
                                                           it.frame.frameNumber == faceExt.frameNumber }
                if (frameExt == null) frameExt = FrameController.getFrameExt(faceExt.fileId, faceExt.frameNumber, fileExt.projectExt.project)
                val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
                var bi = OverlayImage.extractRegion(biSource, faceExt.startX, faceExt.startY, faceExt.endX, faceExt.endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                if (faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.GREEN, 1.0F)
                if (faceExt.face.isManual) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.RED, 1.0F)
                val outputfile = IOFile(faceExt.pathToPreviewFile)
                ImageIO.write(bi, "jpg", outputfile)
            }

//            faceExt.previewSmall

        }

        fileExt.hasCreatedFacesPreview = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}