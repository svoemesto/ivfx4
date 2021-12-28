package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.MatrixPageFrames
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.application.HostServices
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
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.imageio.ImageReader
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
                mainStage?.initModality(Modality.WINDOW_MODAL)
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
                startX = it.x.toInt()
                startY = it.y.toInt()
            }
        }

        lblFrame!!.onMouseReleased = EventHandler {
            if (it.button == MouseButton.PRIMARY) {
                endX = it.x.toInt()
                endY = it.y.toInt()

                if (startX > endX) {
                    val tmp = endX
                    endX = startX
                    startX = tmp
                }

                if (startY > endY) {
                    val tmp = endY
                    endY = startY
                    startY = tmp
                }

                showFrame()
                onCreateNewFace(null)
            }
        }

    }

    fun showFrame(faceExt: FaceExt? = null) {

        val bi = FaceController.getOverlayedFrame(currentFrameExt!!, faceExt, true)

        if (startX !=0 && endX != 0 && startY !=0 && endY != 0) {
            val graphics2D = bi!!.graphics as Graphics2D
            graphics2D.drawImage(bi, 0, 0, null)
            val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F)
            graphics2D.composite = alphaChannel
            graphics2D.color = Color.BLUE
            graphics2D.drawRect(startX, startY, endX - startX, endY - startY)
            graphics2D.dispose()
        }

        lblFrame!!.graphic = ImageView(ConvertToFxImage.convertToFxImage(bi))

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

            showFrame()

        }

    }

    @FXML
    fun doOk(event: ActionEvent?) {

        mainStage!!.close()

    }

}
