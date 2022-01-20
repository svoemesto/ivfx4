package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import java.io.File as IOFile


class FrameFacesEditFXController {
    @FXML
    private var tblFaces: TableView<FaceExt>? = null

    @FXML
    private var colFace: TableColumn<FaceExt, String>? = null

    @FXML
    private var colPerson: TableColumn<FaceExt, String>? = null

    @FXML
    private var colIsManual: TableColumn<FaceExt, String>? = null

    @FXML
    private var btnCreateNewFace: Button? = null

    @FXML
    private var lblFrame: Label? = null

    @FXML
    private var btnOk: Button? = null

    companion object {
        private var mainStage: Stage? = null
        private var currentFrameExt: FrameExt? = null

        fun editFrame(frameExt: FrameExt) {
            currentFrameExt = frameExt
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(FrameFacesEditFXController::class.java.getResource("frame-faces-edit-view.fxml"))
                mainStage?.scene = Scene(root)
                mainStage?.initModality(Modality.APPLICATION_MODAL)
//            onStart()
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы FrameFacesEditFXController.")
            mainStage = null

        }

    }

    private var listFacesExt: ObservableList<FaceExt> = FXCollections.observableArrayList()

    private var startX: Int = 0
    private var endX: Int = 0
    private var startY: Int = 0
    private var endY: Int = 0
    private var wasClicked: Boolean = false
    private var biOverlayed: BufferedImage? = null

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна FrameFacesEditFXController.")
        }

        println("Инициализация FrameFacesEditFXController.")

        lblFrame!!.alignment = Pos.CENTER

        listFacesExt = FXCollections.observableArrayList(currentFrameExt!!.facesExt())

        colFace?.cellValueFactory = PropertyValueFactory("labelSmall")
        colPerson?.cellValueFactory = PropertyValueFactory("labelPersonSmall")
        colIsManual?.cellValueFactory = PropertyValueFactory("isManualText")
        tblFaces!!.items = listFacesExt

        showFrame()

        tblFaces!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out FaceExt?>?, oldValue: FaceExt?, newValue: FaceExt? ->
                if (newValue != null) {
                    showFrame(newValue)
                }
            }

        lblFrame!!.onMousePressed = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                biOverlayed = FaceController.getOverlayedFrame(currentFrameExt!!, null, true)
                startX = it.x.toInt()
                startY = it.y.toInt()
                wasClicked = true
            }
        }

        lblFrame!!.onMouseReleased = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                wasClicked = false
                onCreateNewFace(null)
            }
        }

//        lblFrame!!.onMouseMoved = EventHandler {
//            if (wasClicked) {
//                endX = it.x.toInt()
//                endY = it.y.toInt()
//                showFrame()
//            }
//        }
//
        lblFrame!!.onMouseDragged = EventHandler {
            if (wasClicked) {
                endX = it.x.toInt()
                endY = it.y.toInt()
                showFrame()
            }
        }
    }

    fun deepCopy(bi: BufferedImage): BufferedImage {
        val cm = bi.colorModel
        val isAlphaPremultiplied = cm.isAlphaPremultiplied
        val raster = bi.copyData(null)
        return BufferedImage(cm, raster, isAlphaPremultiplied, null).getSubimage(0, 0, bi.width, bi.height)
    }

    fun showFrame(faceExt: FaceExt? = null) {

        if (biOverlayed == null) biOverlayed = FaceController.getOverlayedFrame(currentFrameExt!!, null, true)

        if (startX !=0 && endX != 0 && startY !=0 && endY != 0) {
            var biOverClone = deepCopy(biOverlayed!!)
            val graphics2D = biOverClone.graphics as Graphics2D
            graphics2D.drawImage(biOverClone, 0, 0, null)
            val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F)
            graphics2D.composite = alphaChannel
            graphics2D.color = Color.BLUE

            val x1 = if (startX < endX) startX else endX
            val x2 = if (startX < endX) endX else startX
            val y1 = if (startY < endY) startY else endY
            val y2 = if (startY < endY) endY else startY

            graphics2D.drawRect(x1, y1, x2 - x1, y2 - y1)
            graphics2D.dispose()
            lblFrame!!.graphic = ImageView(ConvertToFxImage.convertToFxImage(biOverClone))
        } else {
            lblFrame!!.graphic = ImageView(ConvertToFxImage.convertToFxImage(biOverlayed))
        }



    }

    @FXML
    fun onCreateNewFace(event: ActionEvent?) {

        val personExt = PersonSelectFXController().getPersonExt(currentFrameExt!!.fileExt.projectExt)
        if (personExt != null) {

            val face = Face()
            face.file = currentFrameExt!!.fileExt.file
            face.person = personExt.person
            face.faceNumberInFrame = listFacesExt.size+1
            face.frameNumber = currentFrameExt!!.frame.frameNumber
            face.startX = startX
            face.startY = startY
            face.endX = endX
            face.endY = endY
            face.isManual = true
            FaceController.save(face)

            val faceExt = FaceExt(face, currentFrameExt!!.fileExt, personExt)

            if (!IOFile(faceExt.pathToPreviewFile).parentFile.exists()) IOFile(faceExt.pathToPreviewFile).parentFile.mkdir()
            val biSource = ImageIO.read(IOFile(currentFrameExt!!.pathToFull))
            var bi = OverlayImage.extractRegion(biSource, startX, startY, endX, endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
            if (face.isExample) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.GREEN, 1.0F)
            if (face.isManual) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.RED, 1.0F)
            val outputfile = IOFile(faceExt.pathToPreviewFile)
            ImageIO.write(bi, "jpg", outputfile)

            startX = 0
            startY = 0
            endX = 0
            endY = 0

            listFacesExt = FXCollections.observableArrayList(currentFrameExt!!.facesExt())
            tblFaces!!.items = listFacesExt

            biOverlayed = FaceController.getOverlayedFrame(currentFrameExt!!, null, true)
            showFrame()

        }

    }

    @FXML
    fun doOk(event: ActionEvent?) {

        mainStage!!.close()

    }

}
