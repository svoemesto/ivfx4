package com.svoemesto.ivfx.fxcontrollers

import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.MatrixFace
import com.svoemesto.ivfx.modelsext.MatrixFrame
import com.svoemesto.ivfx.modelsext.MatrixPageFaces
import com.svoemesto.ivfx.modelsext.MatrixPageFrames
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ShotExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonFacesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExt
import com.svoemesto.ivfx.threads.loadlists.LoadListShotsExt
import com.svoemesto.ivfx.threads.updatelists.UpdateListFramesExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.application.HostServices
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Bounds
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Skin
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.transaction.annotation.Transactional
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import java.io.File as IOFile

@Transactional
class ShotsEditFXController {
    @FXML
    private var tblPagesFrames: TableView<MatrixPageFrames>? = null

    @FXML
    private var colDurationStart: TableColumn<MatrixPageFrames, String>? = null

    @FXML
    private var colDurationEnd: TableColumn<MatrixPageFrames, String>? = null

    @FXML
    private var colFrameStart: TableColumn<MatrixPageFrames, String>? = null

    @FXML
    private var colFrameEnd: TableColumn<MatrixPageFrames, String>? = null

    @FXML
    private var tblShots: TableView<ShotExt>? = null

    @FXML
    private var colShotFrom: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotTo: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotType: TableColumn<ShotExt, String>? = null

    @FXML
    private var colButtonGetType: TableColumn<ShotExt, String>? = null

    @FXML
    private var tblPersonsAll: TableView<PersonExt>? = null

    @FXML
    private var colTblPersonsAllName: TableColumn<PersonExt, String>? = null

    @FXML
    private var tblPagesFaces: TableView<MatrixPageFaces>? = null

    @FXML
    private var colTblPagesFacesNumber: TableColumn<MatrixPageFaces, String>? = null

    @FXML
    private var lblFrameFull: Label? = null

    @FXML
    private var contextMenuFrameFull: ContextMenu? = null

    @FXML
    private var btnOK: Button? = null

    @FXML
    private var paneFrames: Pane? = null

    @FXML
    private var paneFaces: Pane? = null

    @FXML
    private var pb: ProgressBar? = null

    @FXML
    private var lblPb: Label? = null

    companion object {
        private const val fxBorderDefault = "-fx-border-color:#0f0f0f;-fx-border-width:1" // стиль бордюра лейбла по-умолчанию
        private const val fxBorderFocused = "-fx-border-color:YELLOW;-fx-border-width:1" // стиль бордюра лейбла в фокусе
        private const val fxBorderSelected = "-fx-border-color:RED;-fx-border-width:1" // стиль бордюра лейбла выбранного
        private const val framePictW = 135.0 // ширина картинки
        private const val framePictH = 75.0 // высота картинки
        private const val facePictW = 75.0 // ширина картинки
        private const val facePictH = 75.0 // высота картинки

        private val runListThreadsFramesFlagIsDone = SimpleBooleanProperty(false)
        private val runListThreadsFacesFlagIsDone = SimpleBooleanProperty(false)
        private val sbpCurrentMatrixPageWasChanged = SimpleBooleanProperty(false)
        private val sbpCurrentMatrixFrameWasChanged = SimpleBooleanProperty(false)
        private val sbpCurrentShotExtWasChanged = SimpleBooleanProperty(false)
        private val sbpNeedCreatePagesWasChanged = SimpleBooleanProperty(false)
        private var isPressedPlayForward = SimpleBooleanProperty(false)
        private var isPressedPlayBackward = SimpleBooleanProperty(false)

        private var hostServices: HostServices? = null
        private var currentFileExt: FileExt? = null
        private var mainStage: Stage? = null

        private var listMatrixPageFrames: ObservableList<MatrixPageFrames> = FXCollections.observableArrayList()
        private var listMatrixPageFaces: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
        private var listPersonsExtAll: ObservableList<PersonExt> = FXCollections.observableArrayList()
        private var listFacesExt: ObservableList<FaceExt> = FXCollections.observableArrayList()
        private var countColumnsInPage = 0
        private var countRowsInPage = 0

        private var isWorking = false
        private var isPressedControl = false
        private var isPlayingForward = false

        private var currentMatrixPageFrames: MatrixPageFrames? = null
        private var currentMatrixPageFaces: MatrixPageFaces? = null
        private var currentMatrixFrame: MatrixFrame? = null
        private var currentMatrixFace: MatrixFace? = null
        private var currentShotExt: ShotExt? = null
        private var currentPersonExt: PersonExt? = null
        private var currentNumPage = 0
        private var flowTblPagesFrames: VirtualFlow<*>? = null
        private var flowTblPagesFaces: VirtualFlow<*>? = null
        private var flowTblShots: VirtualFlow<*>? = null
        private var flowTblPersonsAll: VirtualFlow<*>? = null

        private var wasClickTablePagesFrames = false
        private var wasClickTablePagesFaces = false
        private var wasClickTableShots = false
        private var wasClickTablePersonsAll = false
        private var wasClickFrameLabel = false


        fun editShots(fileExt: FileExt, hostServices: HostServices? = null) {
            currentFileExt = fileExt
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("shots-edit-view.fxml"))
                mainStage?.scene = Scene(root)
                this.hostServices = hostServices
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                onStart()
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы ShotsEditFXController.")
            mainStage = null

        }

        fun onStart() {

            mainStage?.scene!!.onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.CONTROL) isPressedControl = true
                if (event.code == KeyCode.Z) isPressedPlayBackward.set(true)
                if (event.code == KeyCode.X) isPressedPlayForward.set(true)
            }

            mainStage?.scene!!.onKeyReleased = EventHandler { event ->
                if (event.code == KeyCode.CONTROL) isPressedControl = false
                if (event.code == KeyCode.Z) isPressedPlayBackward.set(false)
                if (event.code == KeyCode.X) isPressedPlayForward.set(false)
            }

        }
    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна ShotsEditFXController.")
        }

        println("Инициализация ShotsEditFXController.")

        /**
         * Первичная инициализация переменных. Нужна для правильно работы при повторном открытии формы
         */
        listMatrixPageFrames = FXCollections.observableArrayList()
        listPersonsExtAll = FXCollections.observableArrayList()
        listFacesExt = FXCollections.observableArrayList()
        countColumnsInPage = 0
        countRowsInPage = 0
        isWorking = false
        isPressedControl = false
        isPlayingForward = false
        currentMatrixPageFrames = null
        currentMatrixFrame = null
        currentShotExt = null
        currentPersonExt = null
        currentNumPage = 0
        flowTblPagesFrames = null
        flowTblShots = null
        wasClickTablePagesFrames = false
        wasClickTableShots = false
        wasClickFrameLabel = false
        runListThreadsFramesFlagIsDone.value = false
        runListThreadsFacesFlagIsDone.value = false
        sbpCurrentMatrixPageWasChanged.value = false
        sbpCurrentMatrixFrameWasChanged.value = false
        sbpCurrentShotExtWasChanged.value = false
        sbpNeedCreatePagesWasChanged.value = false

        tblPagesFrames?.placeholder = ProgressIndicator(-1.0)
        tblShots?.placeholder = ProgressIndicator(-1.0)

        mainStage?.title = "Редактор планов. Файл: ${currentFileExt!!.file.name}"

        var listThreads: MutableList<Thread> = mutableListOf()
        listThreads.add(LoadListPersonsExt(listPersonsExtAll, currentFileExt!!.projectExt, pb, lblPb))
        listThreads.add(LoadListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb))
        listThreads.add(LoadListShotsExt(currentFileExt!!.shotsExt, currentFileExt!!, pb, lblPb))
        var runListThreadsFrames = RunListThreads(listThreads, runListThreadsFramesFlagIsDone)
        runListThreadsFrames.start()

        runListThreadsFramesFlagIsDone.addListener { observable, oldValue, newValue ->
            if (newValue == true) {

                currentFileExt!!.shotsExt.forEach { shotExt ->
                    shotExt.buttonGetType.setOnAction { onActionButtonGetShotType(shotExt) }
                }
                tblPersonsAll!!.items = listPersonsExtAll

                listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.getWidth(), paneFrames!!.getHeight(), framePictW, framePictH)
                tblPagesFaces!!.items = listMatrixPageFaces

                listThreads = mutableListOf()
                listThreads.add(UpdateListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb))
                runListThreadsFrames = RunListThreads(listThreads)
                runListThreadsFrames.start()
            }
        }

        runListThreadsFacesFlagIsDone.addListener { observable, oldValue, newValue ->
            if (newValue == true) {

                listMatrixPageFaces = MatrixPageFaces.createPages(listFacesExt, paneFaces!!.getWidth(), paneFaces!!.getHeight(), facePictW, facePictH)
                tblPagesFrames!!.items = listMatrixPageFrames

                currentMatrixFace = listMatrixPageFaces.first().matrixFaces.first()
                currentMatrixPageFaces = getMatrixPageFacesByMatrixFace(currentMatrixFace!!)

                if (currentMatrixPageFaces != null) {
                    Platform.runLater{
                        showMatrixPageFaces(currentMatrixPageFaces!!)
                        tblPagesFaces!!.items = listMatrixPageFaces
                        tblPagesFaces!!.selectionModel.select(currentMatrixPageFaces)
                    }

                }

            }
        }

        isWorking = true

        // устанавливаем соответствия для столбцов и таблицы
        colDurationStart?.cellValueFactory = PropertyValueFactory("start")
        colDurationEnd?.cellValueFactory = PropertyValueFactory("end")
        colFrameStart?.cellValueFactory = PropertyValueFactory("firstFrameNumber")
        colFrameEnd?.cellValueFactory = PropertyValueFactory("lastFrameNumber")
        tblPagesFrames!!.items = listMatrixPageFrames

        colShotFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colShotTo?.cellValueFactory = PropertyValueFactory("labelLast1")
        colShotType?.cellValueFactory = PropertyValueFactory("labelType")
        colButtonGetType?.cellValueFactory = PropertyValueFactory("buttonGetType")

        tblShots!!.items = currentFileExt!!.shotsExt

        colTblPersonsAllName?.cellValueFactory = PropertyValueFactory("labelSmall")
        tblPersonsAll!!.items = listPersonsExtAll

        colTblPagesFacesNumber?.cellValueFactory = PropertyValueFactory("pageNumber")
        tblPagesFaces!!.items = listMatrixPageFaces

        // выбор записи в таблице tblPages
        tblPagesFrames!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out MatrixPageFrames?>?, oldValue: MatrixPageFrames?, newValue: MatrixPageFrames? ->
                if (newValue != null) {
                    if (wasClickTablePagesFrames) {
                        wasClickTablePagesFrames = false
                        goToFrame(newValue.matrixFrames.first())
                    } else {
                        tblPagesFramesSmartScroll(newValue)
                    }
                }
            }

        tblPagesFaces!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out MatrixPageFaces?>?, oldValue: MatrixPageFaces?, newValue: MatrixPageFaces? ->
                if (newValue != null) {
                    if (wasClickTablePagesFaces) {
                        wasClickTablePagesFaces = false
                        goToFace(newValue.matrixFaces.first())
                    } else {
                        tblPagesFacesSmartScroll(newValue)
                    }
                }
            }


        tblPagesFrames!!.onMouseEntered = EventHandler {
            wasClickTablePagesFrames = true
        }
        tblPagesFrames!!.onMouseExited = EventHandler {
            wasClickTablePagesFrames = false
        }

        tblPagesFaces!!.onMouseEntered = EventHandler {
            wasClickTablePagesFaces = true
        }
        tblPagesFaces!!.onMouseExited = EventHandler {
            wasClickTablePagesFaces = false
        }

        tblShots!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out ShotExt?>?, oldValue: ShotExt?, newValue: ShotExt? ->
                if (newValue != null) {
                    if (wasClickTableShots) {
                        wasClickTableShots = false
                        goToFrame(getMatrixFrameByFrameExt(newValue.firstFrameExt))
                    } else {
                        tblShotsSmartScroll(newValue)
                    }

                }
            }

        tblShots!!.onMouseEntered = EventHandler {
            wasClickTableShots = true
        }

        //событие "уход мыши"
        tblShots!!.onMouseExited = EventHandler {
            wasClickTableShots = false
        }

        tblPersonsAll!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out PersonExt?>?, oldValue: PersonExt?, newValue: PersonExt? ->
                if (newValue != null) {

                    currentPersonExt = newValue

                    runListThreadsFacesFlagIsDone.value = false
                    val listThreadsFaces: MutableList<Thread> = mutableListOf()
                    listThreadsFaces.add(LoadListPersonFacesExt(listFacesExt, currentFileExt!!, currentPersonExt!!, pb, lblPb))
                    val runListThreadsFaces = RunListThreads(listThreadsFaces, runListThreadsFacesFlagIsDone)
                    runListThreadsFaces.start()

                }
            }

        tblPersonsAll!!.onMouseEntered = EventHandler {
            wasClickTablePersonsAll = true
        }
        tblPersonsAll!!.onMouseExited = EventHandler {
            wasClickTablePersonsAll = false
        }


        // событие отслеживани видимости на экране текущего персонажа в таблице tblPages
        tblPagesFrames!!.skinProperty()
            .addListener(ChangeListener label@{ ov: ObservableValue<out Skin<*>?>?, t: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) {
                    return@label
                }
                val tvs =
                    t1 as TableViewSkin<*>
                val kids = tvs.children //    getChildrenUnmodifiable();
                if (kids == null || kids.isEmpty()) {
                    return@label
                }
                flowTblPagesFrames = kids[1] as VirtualFlow<*>
            })

        tblPagesFaces!!.skinProperty()
            .addListener(ChangeListener label@{ ov: ObservableValue<out Skin<*>?>?, t: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) {
                    return@label
                }
                val tvs =
                    t1 as TableViewSkin<*>
                val kids = tvs.children //    getChildrenUnmodifiable();
                if (kids == null || kids.isEmpty()) {
                    return@label
                }
                flowTblPagesFaces = kids[1] as VirtualFlow<*>
            })


        tblShots!!.skinProperty()
            .addListener(ChangeListener label@{ ov: ObservableValue<out Skin<*>?>?, t: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) {
                    return@label
                }
                val tvs =
                    t1 as TableViewSkin<*>
                val kids = tvs.children //    getChildrenUnmodifiable();
                if (kids == null || kids.isEmpty()) {
                    return@label
                }
                flowTblShots = kids[1] as VirtualFlow<*>
            })

        paneFrames!!.widthProperty().addListener { _, _, _ -> listenToChangePaneSize() }
        paneFrames!!.heightProperty().addListener { _, _, _ -> listenToChangePaneSize() }

        // прокрутка колеса мыши над CenterPane
        paneFrames!!.setOnScroll { e: ScrollEvent ->
            wasClickFrameLabel = false
            wasClickTablePagesFrames = false
            wasClickTableShots = false
            val delta = if (e.deltaY > 0) -1 else 1
            if (isPressedControl) {
                if (currentShotExt != null) {
                    if (delta > 0) {
                        goToFrame(currentShotExt!!.lastFrameExt.frame.frameNumber + 1)
                    } else {
                        goToFrame(currentShotExt!!.firstFrameExt.frame.frameNumber - 1)
                    }
                }
            } else {
                if (currentMatrixPageFrames == null) {
                    goToFrame(listMatrixPageFrames.first().matrixFrames.first())
                } else {
                    val frameToGo = if (delta < 0) getPrevMatrixFrame(currentMatrixPageFrames!!.matrixFrames.first()) else getNextMatrixFrame(currentMatrixPageFrames!!.matrixFrames.last())
                    goToFrame(frameToGo)
                }
            }
        }

        lblFrameFull!!.setOnScroll { e: ScrollEvent ->
            val delta = if (e.deltaY > 0) -1 else 1
            if (isPressedControl) {
                if (currentShotExt != null) {
                    if (delta > 0) {
                        goToFrame(currentShotExt!!.lastFrameExt.frame.frameNumber + 1)
                    } else {
                        goToFrame(currentShotExt!!.firstFrameExt.frame.frameNumber - 1)
                    }
                }
            } else {
                if (currentMatrixFrame != null) {
                    if (delta > 0) {
                        goToFrame(currentMatrixFrame?.frameNumber!!+1)
                    } else {
                        goToFrame(currentMatrixFrame?.frameNumber!!-1)
                    }
                }
            }
        }

        paneFaces!!.setOnScroll { e: ScrollEvent ->
            val delta = if (e.deltaY > 0) -1 else 1
            if (currentMatrixPageFaces == null) {
                goToFace(listMatrixPageFaces.first().matrixFaces.first())
            } else {
                val faceToGo = if (delta < 0) getPrevMatrixFace(currentMatrixPageFaces!!.matrixFaces.first()) else getNextMatrixFace(
                    currentMatrixPageFaces!!.matrixFaces.last())
                goToFace(faceToGo)
            }
        }
        
        sbpNeedCreatePagesWasChanged.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                sbpNeedCreatePagesWasChanged.value = false
                listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.width, paneFrames!!.height, framePictW, framePictH)
                tblPagesFrames!!.items = listMatrixPageFrames
            }
        }

        sbpCurrentMatrixPageWasChanged.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                sbpCurrentMatrixPageWasChanged.value = false
                if (currentMatrixPageFrames != null) {
                    showMatrixPageFrames(currentMatrixPageFrames!!)
                    tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames!!)
                }
            }
        }

        sbpCurrentMatrixFrameWasChanged.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                sbpCurrentMatrixFrameWasChanged.value = false
                goToFrame(currentMatrixFrame)
            }
        }

        sbpCurrentShotExtWasChanged.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                sbpCurrentShotExtWasChanged.value = false
                tblShots!!.selectionModel.select(currentShotExt!!)
            }
        }

        tblPagesFrames!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 1) {
                    wasClickTablePagesFrames = true
                    wasClickFrameLabel = false
                    wasClickTableShots = false
                }
            }
        }

        tblShots!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 1) {
                    wasClickTableShots = true
                    wasClickFrameLabel = false
                    wasClickTablePagesFrames = false
                }
            }
        }

    }



    @FXML
    fun doOK(event: ActionEvent?) {
        isWorking = false
        mainStage?.close()
    }

    fun getNextMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.last()) {
            if (matrixFrame.matrixPageFrames == listMatrixPageFrames.last()) return matrixFrame
        }
        return listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames)+1].matrixFrames.first()
    }

    fun getPrevMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.first()) {
            if (matrixFrame.matrixPageFrames == listMatrixPageFrames.first()) return matrixFrame
        }
        return listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames)-1].matrixFrames.last()
    }

    fun getNextMatrixFace(matrixFace: MatrixFace): MatrixFace {
        if (matrixFace == matrixFace.matrixPageFaces!!.matrixFaces.last()) {
            if (matrixFace.matrixPageFaces == listMatrixPageFaces.last()) return matrixFace
        }
        return listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces)+1].matrixFaces.first()
    }

    fun getPrevMatrixFace(matrixFace: MatrixFace): MatrixFace {
        if (matrixFace == matrixFace.matrixPageFaces!!.matrixFaces.first()) {
            if (matrixFace.matrixPageFaces == listMatrixPageFaces.first()) return matrixFace
        }
        return listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces)-1].matrixFaces.last()
    }
    
    fun loadPictureToFullFrameLabel(matrixFrame: MatrixFrame?) {

        if (matrixFrame != null) {

            Thread {
                try {
                    val ioFile = IOFile(matrixFrame.frameExt!!.pathToMedium)
                    if (ioFile.exists()) {
                        val bufferedImage = ImageIO.read(ioFile)
                        val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                        Platform.runLater {
                            lblFrameFull?.graphic = imageView
                        }

                    }
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }
            }.start()

        }
    }

    fun splitOrUnionShots(matrixFrame: MatrixFrame) {
        val shotExt = getShotExtByFrameNumber(matrixFrame.frameNumber!!)
        if (shotExt != null) {
            val frameNumber = matrixFrame.frameNumber
            if (matrixFrame.frameExt!!.frame.isFinalFind && shotExt.firstFrameExt.frame.frameNumber != matrixFrame.frameNumber) {
                // split
                val lastFrameNumber = shotExt.lastFrameExt.frame.frameNumber
                val lastFrameExt = currentFileExt!!.framesExt.first{it.frame.frameNumber == lastFrameNumber}
                shotExt.shot.lastFrameNumber = matrixFrame.frameNumber!! - 1
                shotExt.lastFrameExt = currentFileExt!!.framesExt.first{it.frame.frameNumber == matrixFrame.frameNumber!! - 1}
                shotExt.previewsLast = null
                shotExt.labelsLast = null
                ShotController.save(shotExt.shot)
                val shot = ShotController.getOrCreate(currentFileExt!!.file, matrixFrame.frameNumber!!, lastFrameNumber)
                val addedShotExt = ShotExt(shot, currentFileExt!!, matrixFrame.frameExt!!, lastFrameExt)
                addedShotExt.buttonGetType.setOnAction { onActionButtonGetShotType(addedShotExt) }
                currentFileExt!!.shotsExt.add(addedShotExt)
                currentFileExt!!.shotsExt.sort()

            } else if (!matrixFrame.frameExt!!.frame.isFinalFind && shotExt.firstFrameExt.frame.frameNumber == matrixFrame.frameNumber) {
                // union
                val unionShot = getShotExtByFrameNumber(matrixFrame.frameNumber!!-1)
                if (unionShot != null) {
                    unionShot.shot.lastFrameNumber = shotExt.shot.lastFrameNumber
                    unionShot.lastFrameExt = shotExt.lastFrameExt
                    unionShot.previewsLast = null
                    unionShot.labelsLast = null
                    ShotController.save(unionShot.shot)
                    currentFileExt!!.shotsExt.remove(shotExt)
                    currentFileExt!!.shotsExt.sort()
                    ShotController.delete(shotExt.shot)
                } else {
                    println("Проблема: не найден план для объединения!")
                }
            } else {
                //do nothing
                return
            }

            listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.getWidth(), paneFrames!!.getHeight(), framePictW, framePictH)
            tblPagesFrames!!.items = listMatrixPageFrames
            currentMatrixFrame = getMatrixFrameByFrameNumber(frameNumber!!)
            currentMatrixPageFrames = getMatrixPageFramesByFrame(frameNumber)
            currentShotExt = getShotExtByFrameNumber(frameNumber)
            showMatrixPageFrames(currentMatrixPageFrames!!)
            tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
            goToFrame(currentMatrixFrame)

        } else {
            println("Проблема: не найден план!")
        }

    }

    fun getMatrixFrameByFrameExt(frameExt: FrameExt): MatrixFrame? {
        val lst: MutableList<MatrixFrame> = mutableListOf()
        listMatrixPageFrames.forEach { matrixPage ->
            lst.addAll(matrixPage.matrixFrames.filter { matrixFrame -> matrixFrame.frameExt == frameExt })
        }
        return if (lst.size > 0) lst[0] else null
    }

    fun getMatrixFrameByFrameNumber(frameNumber: Int): MatrixFrame? {
        val lst: MutableList<MatrixFrame> = mutableListOf()
        listMatrixPageFrames.forEach { matrixPage ->
            lst.addAll(matrixPage.matrixFrames.filter { matrixFrame -> matrixFrame.frameExt!!.frame.frameNumber == frameNumber })
        }
        return if (lst.size > 0) lst[0] else null
    }

    fun goToFrame(frameNumber: Int) {
        goToFrame(getMatrixFrameByFrameNumber(frameNumber))
    }

    fun goToFrame(frameExt: FrameExt) {
        goToFrame(getMatrixFrameByFrameExt(frameExt))
    }

    fun goToFrame(matrixFrame: MatrixFrame?) {
        Platform.runLater {
            if (matrixFrame != null) {
                currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderDefault
                currentMatrixFrame = matrixFrame
                if (currentMatrixFrame!!.matrixPageFrames != currentMatrixPageFrames) {
                    currentMatrixPageFrames = currentMatrixFrame!!.matrixPageFrames
                    showMatrixPageFrames(currentMatrixPageFrames!!)
                    tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
                }
                currentShotExt = getShotExtByFrameNumber(currentMatrixFrame!!.frameNumber!!)
                tblShots!!.selectionModel.select(currentShotExt)

                currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderSelected
                loadPictureToFullFrameLabel(currentMatrixFrame)
            }
        }
    }

    fun goToFace(matrixFace: MatrixFace?) {
        Platform.runLater {
            if (matrixFace != null) {
                currentMatrixFace?.faceExt?.labelSmall?.style = fxBorderDefault
                currentMatrixFace = matrixFace
                if (currentMatrixFace!!.matrixPageFaces != currentMatrixPageFaces) {
                    currentMatrixPageFaces = currentMatrixFace!!.matrixPageFaces
                    showMatrixPageFaces(currentMatrixPageFaces!!)
                    tblPagesFaces!!.selectionModel.select(currentMatrixPageFaces)
                }

                currentMatrixFace?.faceExt?.labelSmall?.style = fxBorderSelected
//                loadPictureToFullFrameLabel(currentMatrixFrame)
            }
        }
    }


    fun tblPagesFramesSmartScroll(matrixPageFrames: MatrixPageFrames?) {
        if (flowTblPagesFrames != null && flowTblPagesFrames!!.cellCount > 0) {
            val first: Int = flowTblPagesFrames!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPagesFrames!!.lastVisibleCell.getIndex()
            val selected = tblPagesFrames!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblPagesFrames?.scrollTo(matrixPageFrames)
            }
        }
    }

    fun tblPagesFacesSmartScroll(matrixPageFaces: MatrixPageFaces?) {
        if (flowTblPagesFaces != null && flowTblPagesFaces!!.cellCount > 0) {
            val first: Int = flowTblPagesFaces!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPagesFaces!!.lastVisibleCell.getIndex()
            val selected = tblPagesFaces!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblPagesFaces?.scrollTo(matrixPageFaces)
            }
        }
    }

    fun tblShotsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShots != null && flowTblShots!!.cellCount > 0) {
            val first: Int = flowTblShots!!.firstVisibleCell.getIndex()
            val last: Int = flowTblShots!!.lastVisibleCell.getIndex()
            val selected = tblShots!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblShots?.scrollTo(shotExt)
            }
        }
    }

    fun tblPersonsAllSmartScroll(personExt: PersonExt?) {
        if (flowTblPersonsAll != null && flowTblPersonsAll!!.cellCount > 0) {
            val first: Int = flowTblPersonsAll!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPersonsAll!!.lastVisibleCell.getIndex()
            val selected = tblPersonsAll!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblPersonsAll?.scrollTo(personExt)
            }
        }
    }

    fun showMatrixPageFrames(matrixPageFrames: MatrixPageFrames) {
        val heightPadding = 10 // по высоте двойной отступ
        val widthPadding = 10 // по ширине двойной отступ
        val pane: Pane = paneFrames!!
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFrame in matrixPageFrames.matrixFrames) {
            val lbl: Label = matrixFrame.frameExt?.labelSmall!!
            val x: Double = widthPadding + matrixFrame.column * (framePictW + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFrame.row * (framePictH + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(framePictW, framePictH) //устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault //устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER //устанавливаем позиционирование по центру
            var resultImage: BufferedImage? = null

            var flagPrevFrameIsFind = false
            var flagPrevFrameIsManualAdd = false
            var flagPrevFrameIsManualCancel = false
            var flagNextFrameIsFind = false
            var flagNextFrameIsManualAdd = false
            var flagNextFrameIsManualCancel = false
            val flagCurrFrameIsIFrame = matrixFrame.frameExt!!.frame.isIFrame
            val flagCurrFrameIsFind = matrixFrame.frameExt!!.frame.isFind
            val flagCurrFrameIsManualAdd = matrixFrame.frameExt!!.frame.isManualAdd
            val flagCurrFrameIsManualCancel = matrixFrame.frameExt!!.frame.isManualCancel

            if (matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1 < matrixPageFrames.matrixFrames.size) {
                flagNextFrameIsFind = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isFind
                flagNextFrameIsManualAdd = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualAdd
                flagNextFrameIsManualCancel = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualCancel
            }
            if (matrixPageFrames.matrixFrames.indexOf(matrixFrame)>0) {
                flagPrevFrameIsFind = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isFind
                flagPrevFrameIsManualAdd = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isManualAdd
                flagPrevFrameIsManualCancel = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isManualCancel
            }

            if (flagPrevFrameIsFind || flagPrevFrameIsManualAdd || flagPrevFrameIsManualCancel ||
                flagNextFrameIsFind || flagNextFrameIsManualAdd || flagNextFrameIsManualCancel ||
                flagCurrFrameIsIFrame || flagCurrFrameIsFind || flagCurrFrameIsManualAdd || flagCurrFrameIsManualCancel) {
                resultImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
            }

            if (flagCurrFrameIsIFrame) resultImage = OverlayImage.setOverlayIFrame(resultImage!!)
            if (flagCurrFrameIsFind) resultImage = if (flagCurrFrameIsManualCancel) OverlayImage.cancelOverlayFirstFrameManual(resultImage!!) else OverlayImage.setOverlayFirstFrameFound(resultImage!!)
            if (flagNextFrameIsFind) resultImage = if (flagNextFrameIsManualCancel) OverlayImage.cancelOverlayLastFrameManual(resultImage!!) else OverlayImage.setOverlayLastFrameFound(resultImage!!)
            if (flagCurrFrameIsManualAdd) resultImage = OverlayImage.setOverlayFirstFrameManual(resultImage!!)
            if (flagNextFrameIsManualAdd) resultImage = OverlayImage.setOverlayLastFrameManual(resultImage!!)

            if (resultImage != null) {
                val screenImageView = ImageView(ConvertToFxImage.convertToFxImage(resultImage)) // загружаем ресайзный буфер в новый вьювер
                screenImageView.fitWidth = framePictW // устанавливаем ширину вьювера
                screenImageView.fitHeight = framePictH // устанавливаем высоту вьювера
                lbl.graphic = null //сбрасываем графику лейбла
                lbl.graphic = screenImageView // устанавливаем вьювер источником графики для лейбла
            }

            pane.children.add(lbl)

            //событие "наведение мыши"
            lbl.onMouseEntered = EventHandler {
                lbl.style = fxBorderFocused
                lbl.toFront()
            }

            //событие "уход мыши"
            lbl.onMouseExited = EventHandler {
                lbl.style = if (matrixFrame == currentMatrixFrame) fxBorderSelected else fxBorderDefault
            }

            lbl.onMouseClicked = EventHandler { mouseEvent ->

                //событие двойного клика
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 1) {
                        wasClickTablePagesFrames = false
                        wasClickTableShots = false
                        wasClickFrameLabel = true
                        currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderDefault
                        currentMatrixFrame = matrixFrame
                        currentShotExt = getShotExtByFrameNumber(matrixFrame.frameNumber!!)
                        tblShots!!.selectionModel.select(currentShotExt)
                        loadPictureToFullFrameLabel(currentMatrixFrame)
                    }
                    if (mouseEvent.clickCount == 2) {
                        if (matrixFrame.frameExt!!.frame.isFind) { //фрейм найден
                            if (!matrixFrame.frameExt!!.frame.isManualCancel) { // и не отменен вручную
                                matrixFrame.frameExt!!.frame.isManualCancel = true // отменяем
                                matrixFrame.frameExt!!.frame.isFinalFind = false
                                FrameController.save(matrixFrame.frameExt!!.frame)
                                splitOrUnionShots(matrixFrame)
                            } else { // и отменен вручную
                                matrixFrame.frameExt!!.frame.isManualCancel = false // восстанавливаем отметку
                                matrixFrame.frameExt!!.frame.isFinalFind = true
                                FrameController.save(matrixFrame.frameExt!!.frame)
                                splitOrUnionShots(matrixFrame)
                            }
                        } else { // не найден
                            if (!matrixFrame.frameExt!!.frame.isManualAdd) { // и не отменен вручную
                                matrixFrame.frameExt!!.frame.isManualAdd = true // отмечаем
                                matrixFrame.frameExt!!.frame.isFinalFind = true
                                FrameController.save(matrixFrame.frameExt!!.frame)
                                splitOrUnionShots(matrixFrame)
                            } else { // и отменен вручную
                                matrixFrame.frameExt!!.frame.isManualAdd = false // снимаем отметку
                                matrixFrame.frameExt!!.frame.isFinalFind = false
                                matrixFrame.frameExt!!.labelSmall?.graphic = matrixFrame.frameExt!!.previewSmall
                                matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1].frameExt?.labelSmall?.graphic = matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1].frameExt?.previewSmall
                                FrameController.save(matrixFrame.frameExt!!.frame)
                                splitOrUnionShots(matrixFrame)
                            }
                        }
                    }
                }
            }
        }
    }

    fun showMatrixPageFaces(matrixPageFaces: MatrixPageFaces) {
        val heightPadding = 10 // по высоте двойной отступ
        val widthPadding = 10 // по ширине двойной отступ
        val pane: Pane = paneFaces!!
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFace in matrixPageFaces.matrixFaces) {
            val lbl: Label = matrixFace.faceExt?.labelSmall!!
            val x: Double = widthPadding + matrixFace.column * (facePictW + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFace.row * (facePictH + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(facePictW, facePictH) //устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault //устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER //устанавливаем позиционирование по центру
            var resultImage: BufferedImage? = null
            resultImage = ImageIO.read(IOFile(matrixFace.faceExt!!.pathToFaceFile))

            val screenImageView = matrixFace.faceExt!!.previewSmall
            screenImageView!!.fitWidth = facePictW // устанавливаем ширину вьювера
            screenImageView.fitHeight = facePictH // устанавливаем высоту вьювера
            lbl.graphic = null //сбрасываем графику лейбла
            lbl.graphic = screenImageView // устанавливаем вьювер источником графики для лейбла

            pane.children.add(lbl)

            //событие "наведение мыши"
            lbl.onMouseEntered = EventHandler {
                lbl.style = fxBorderFocused
                lbl.toFront()
            }

            //событие "уход мыши"
            lbl.onMouseExited = EventHandler {
                lbl.style = if (matrixFace == currentMatrixFace) fxBorderSelected else fxBorderDefault
            }

            lbl.onMouseClicked = EventHandler { mouseEvent ->

                //событие двойного клика
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 1) {
                        currentMatrixFace!!.faceExt?.labelSmall!!.style = fxBorderDefault
                        currentMatrixFace = matrixFace
//                        loadPictureToFullFrameLabel(currentMatrixFace.faceExt.pathToFrameFile)
                    }

                }
            }
        }
    }

    fun getMatrixPageFacesByMatrixFace(matrixFace: MatrixFace): MatrixPageFaces? {
        for (page in listMatrixPageFaces) {
            if (page.matrixFaces.contains(matrixFace)) return page
        }
        return null
    }

    fun getMatrixPageFramesByFrame(frameNumber: Int): MatrixPageFrames? {
        for (page in listMatrixPageFrames) {
            if (page.firstFrameNumber!! <= frameNumber && page.lastFrameNumber!! >= frameNumber) return page
        }
        return null
    }

    fun getShotExtByFrameNumber(frameNumber: Int): ShotExt? {
        return currentFileExt!!.shotsExt.filter {
            frameNumber >= it.firstFrameExt.frame.frameNumber &&
            frameNumber <= it.lastFrameExt.frame.frameNumber }.firstOrNull()
    }

    fun listenToChangePaneSize() {

        if (runListThreadsFramesFlagIsDone.value) {
            val paneFramesWidth: Double = paneFrames!!.getWidth() // ширина центрального пэйна
            val paneFramesHeight: Double = paneFrames!!.getHeight() // высота центрального пейна
            val widthFramePadding = (framePictW + 2) * 2 + 20 // по ширине двойной отступ
            val heightFramePadding = (framePictH + 2) * 2 + 20 // по высоте двойной отступ
            if (paneFramesWidth > widthFramePadding && paneFramesHeight > heightFramePadding) {
                val prevCountColumnsInPage = countColumnsInPage
                val prevCountRowsInPage = countRowsInPage
                countColumnsInPage =
                    ((paneFramesWidth - widthFramePadding) / (framePictW + 2)).toInt() // количество столбцов, которое влезет на экран
                countRowsInPage =
                    ((paneFramesHeight - heightFramePadding) / (framePictH + 2)).toInt() // количество строк, которое влезет на экран

                // если значения кол-ва столбцов и/или строк изменилось при ресайзе
                if (prevCountColumnsInPage != countColumnsInPage || prevCountRowsInPage != countRowsInPage) {
                    val frameNumber = currentMatrixFrame?.frameNumber ?: 1
                    listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.getWidth(), paneFrames!!.getHeight(), framePictW, framePictH)
                    tblPagesFrames!!.items = listMatrixPageFrames
                    currentMatrixFrame = getMatrixFrameByFrameNumber(frameNumber)
                    currentMatrixPageFrames = getMatrixPageFramesByFrame(frameNumber)
                    currentShotExt = getShotExtByFrameNumber(frameNumber)
                    showMatrixPageFrames(currentMatrixPageFrames!!)
                    tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
                    goToFrame(currentMatrixFrame)

                }
            }
        }

        if (runListThreadsFramesFlagIsDone.value) {
            val paneFacesWidth: Double = paneFaces!!.getWidth() // ширина центрального пэйна
            val paneFacesHeight: Double = paneFaces!!.getHeight() // высота центрального пейна
            val widthFacePadding = (facePictW + 2) * 2 + 20 // по ширине двойной отступ
            val heightFacePadding = (facePictH + 2) * 2 + 20 // по высоте двойной отступ
            if (paneFacesWidth > widthFacePadding && paneFacesHeight > heightFacePadding) {
                val prevCountColumnsInPage = countColumnsInPage
                val prevCountRowsInPage = countRowsInPage
                countColumnsInPage =
                    ((paneFacesWidth - widthFacePadding) / (framePictW + 2)).toInt() // количество столбцов, которое влезет на экран
                countRowsInPage =
                    ((paneFacesHeight - heightFacePadding) / (framePictH + 2)).toInt() // количество строк, которое влезет на экран

                // если значения кол-ва столбцов и/или строк изменилось при ресайзе
                if (prevCountColumnsInPage != countColumnsInPage || prevCountRowsInPage != countRowsInPage) {

                    listMatrixPageFaces = MatrixPageFaces.createPages(listFacesExt, paneFaces!!.getWidth(), paneFaces!!.getHeight(), facePictW, facePictH)
                    tblPagesFaces!!.items = listMatrixPageFaces
                    if (currentMatrixFace == null) currentMatrixFace = listMatrixPageFaces.first().matrixFaces.first()
                    currentMatrixPageFaces = getMatrixPageFacesByMatrixFace(currentMatrixFace!!)

                    if (currentMatrixPageFaces != null) {
                        showMatrixPageFaces(currentMatrixPageFaces!!)
                        tblPagesFaces!!.selectionModel.select(currentMatrixPageFaces)
                    }

                    goToFace(currentMatrixFace)

                }
            }
        }


    }

    fun onActionButtonGetShotType(shotExt: ShotExt) {
        val contextMenuShotType = ContextMenu()
        ShotTypePerson.values().forEach { shotTypePerson ->
            println(shotTypePerson.pathToPicture)
            val imageView = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shotTypePerson.pathToPicture))))
            val contextMenuShotTypeItem = MenuItem(null, imageView)
            contextMenuShotTypeItem.onAction = EventHandler { e: ActionEvent? ->
                shotExt.shot.typePerson = shotTypePerson
                ShotController.save(shotExt.shot)
                shotExt.previewType = null
                shotExt.labelType = null
                shotExt.labelType
                tblShots!!.refresh()
            }
            contextMenuShotType.items.add(contextMenuShotTypeItem)
        }
        shotExt.buttonGetType.contextMenu = contextMenuShotType
        val screenBounds: Bounds = shotExt.buttonGetType.localToScreen(shotExt.buttonGetType.boundsInLocal)
        contextMenuShotType.show(mainStage, screenBounds.minX +screenBounds.width, screenBounds.minY)
    }

}
