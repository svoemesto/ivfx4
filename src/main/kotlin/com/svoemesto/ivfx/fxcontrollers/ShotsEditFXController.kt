package com.svoemesto.ivfx.fxcontrollers

import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.EventController
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.SceneController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.modelsext.EventExt
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.MatrixFace
import com.svoemesto.ivfx.modelsext.MatrixFrame
import com.svoemesto.ivfx.modelsext.MatrixPageFaces
import com.svoemesto.ivfx.modelsext.MatrixPageFrames
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.SceneExt
import com.svoemesto.ivfx.modelsext.ShotExt
import com.svoemesto.ivfx.threads.loadlists.LoadListEventsExt
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonFacesExtForAll
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonFacesExtForFile
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForFile
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForShot
import com.svoemesto.ivfx.threads.loadlists.LoadListScenesExt
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
import javafx.scene.control.CheckBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.RadioButton
import javafx.scene.control.SelectionMode
import javafx.scene.control.Skin
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToggleGroup
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.Dragboard
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import java.io.File as IOFile


@Transactional
class ShotsEditFXController {

   // SHOTS


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
    private var pbShots: ProgressBar? = null


    @FXML
    private var rbPersonAll: RadioButton? = null

    @FXML
    private var grpPersons: ToggleGroup? = null

    @FXML
    private var rbPersonFile: RadioButton? = null

    @FXML
    private var rbFaceAll: RadioButton? = null

    @FXML
    private var grpFaces: ToggleGroup? = null

    @FXML
    private var rbFaceFile: RadioButton? = null

    @FXML
    private var cbFacesNotExample: CheckBox? = null

    @FXML
    private var cbFacesExample: CheckBox? = null

    @FXML
    private var cbFacesNotManual: CheckBox? = null

    @FXML
    private var cbFacesManual: CheckBox? = null


    @FXML
    private var tblPersonsAllForShot: TableView<PersonExt>? = null

    @FXML
    private var colTblPersonsAllForShotName: TableColumn<PersonExt, String>? = null

    @FXML
    private var pbPersonsForShot: ProgressBar? = null


    @FXML
    private var lblFrameFull: Label? = null

    @FXML
    private var contextMenuFrameFull: ContextMenu? = null

    @FXML
    private var btnOK: Button? = null


    // FRAMES

    @FXML
    private var paneFrames: Pane? = null

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
    private var pbPagesFrames: ProgressBar? = null

    // PERSONS / FACES


    @FXML
    private var tblPersonsAllForFile: TableView<PersonExt>? = null

    @FXML
    private var colTblPersonsAllForFileName: TableColumn<PersonExt, String>? = null

    @FXML
    private var tblPagesFaces: TableView<MatrixPageFaces>? = null

    @FXML
    private var colTblPagesFacesNumber: TableColumn<MatrixPageFaces, String>? = null

    @FXML
    private var pbPersonsForFile: ProgressBar? = null

    @FXML
    private var paneFaces: Pane? = null

    @FXML
    private var pbFaces: ProgressBar? = null

    // SCENES

    @FXML
    private var tblScenes: TableView<SceneExt>? = null

    @FXML
    private var colSceneName: TableColumn<SceneExt, String>? = null

    @FXML
    private var colSceneFrom: TableColumn<SceneExt, String>? = null

    @FXML
    private var colSceneTo: TableColumn<SceneExt, String>? = null

    @FXML
    private var pbScenes: ProgressBar? = null

    @FXML
    private var btnCreateNewSceneBySelectedShots: Button? = null

    @FXML
    private var btnDeleteSelectedScenes: Button? = null

    @FXML
    private var tblShotsForScenes: TableView<ShotExt>? = null

    @FXML
    private var colShotForSceneFrom: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotForSceneTo: TableColumn<ShotExt, String>? = null

    @FXML
    private var pbShotsForScenes: ProgressBar? = null

    @FXML
    private var tblPersonsAllForScenes: TableView<PersonExt>? = null

    @FXML
    private var colTblPersonsAllForSceneName: TableColumn<PersonExt, String>? = null

    @FXML
    private var pbPersonsForScenes: ProgressBar? = null


    // SCENES

    @FXML
    private var tblEvents: TableView<EventExt>? = null

    @FXML
    private var colEventName: TableColumn<EventExt, String>? = null

    @FXML
    private var colEventFrom: TableColumn<EventExt, String>? = null

    @FXML
    private var colEventTo: TableColumn<EventExt, String>? = null

    @FXML
    private var pbEvents: ProgressBar? = null

    @FXML
    private var btnCreateNewEventBySelectedShots: Button? = null

    @FXML
    private var btnDeleteSelectedEvents: Button? = null

    @FXML
    private var tblShotsForEvents: TableView<ShotExt>? = null

    @FXML
    private var colShotForEventFrom: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotForEventTo: TableColumn<ShotExt, String>? = null

    @FXML
    private var pbShotsForEvents: ProgressBar? = null

    @FXML
    private var tblPersonsAllForEvents: TableView<PersonExt>? = null

    @FXML
    private var colTblPersonsAllForEventName: TableColumn<PersonExt, String>? = null

    @FXML
    private var pbPersonsForEvents: ProgressBar? = null
    
    // FOOTER

    @FXML
    private var pb: ProgressBar? = null

    @FXML
    private var lblPb: Label? = null


    companion object {
        private var currentFileExt: FileExt? = null
        private var hostServices: HostServices? = null
        private var mainStage: Stage? = null
        private var isWorking = false
        private var isPressedControl = false
        private var isPressedShift = false
        private var isPlayingForward = false
        private var isPressedPlayForward = SimpleBooleanProperty(false)
        private var isPressedPlayBackward = SimpleBooleanProperty(false)

        fun onStart() {

            mainStage?.scene!!.onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.CONTROL) isPressedControl = true
                if (event.code == KeyCode.SHIFT) isPressedShift = true
                if (event.code == KeyCode.Z) isPressedPlayBackward.set(true)
                if (event.code == KeyCode.X) isPressedPlayForward.set(true)
            }

            mainStage?.scene!!.onKeyReleased = EventHandler { event ->
                if (event.code == KeyCode.CONTROL) isPressedControl = false
                if (event.code == KeyCode.SHIFT) isPressedShift = false
                if (event.code == KeyCode.Z) isPressedPlayBackward.set(false)
                if (event.code == KeyCode.X) isPressedPlayForward.set(false)
            }

        }

    }

    private val fxBorderDefault = "-fx-border-color:#0f0f0f;-fx-border-width:1" // стиль бордюра лейбла по-умолчанию
    private val fxBorderFocused = "-fx-border-color:YELLOW;-fx-border-width:1" // стиль бордюра лейбла в фокусе
    private val fxBorderSelected = "-fx-border-color:RED;-fx-border-width:1" // стиль бордюра лейбла выбранного
    private val fxBorderSelectedFocused = "-fx-border-color:ORANGE;-fx-border-width:1" // стиль бордюра лейбла выбранного

//    private val runListThreadsFramesFlagIsDone = SimpleBooleanProperty(false)
    private val runListThreadsFacesFlagIsDone = SimpleBooleanProperty(false)
    private val sbpCurrentMatrixPageWasChanged = SimpleBooleanProperty(false)
    private val sbpCurrentMatrixFrameWasChanged = SimpleBooleanProperty(false)
    private val sbpCurrentShotExtWasChanged = SimpleBooleanProperty(false)
    private val sbpNeedCreatePagesWasChanged = SimpleBooleanProperty(false)

    private val isDoneLoadListEventsExt = SimpleBooleanProperty(false)
    private val isDoneLoadListFileFacesExt = SimpleBooleanProperty(false)
    private val isDoneLoadListFilesExt = SimpleBooleanProperty(false)
    private val isDoneLoadListFramesExt = SimpleBooleanProperty(false)
    private val isDoneLoadListPersonFacesExt = SimpleBooleanProperty(false)
    private val isDoneLoadListPersonsExtForFile = SimpleBooleanProperty(false)
    private val isDoneLoadListPersonsExtForProject = SimpleBooleanProperty(false)
    private val isDoneLoadListPersonsExtForShot = SimpleBooleanProperty(false)
    private val isDoneLoadListProjectsExt = SimpleBooleanProperty(false)
    private val isDoneLoadListScenesExt = SimpleBooleanProperty(false)
    private val isDoneLoadListShotsExt = SimpleBooleanProperty(false)
    private val isDoneUpdateListFilesExt = SimpleBooleanProperty(false)
    private val isDoneUpdateListFramesExt = SimpleBooleanProperty(false)

    private var listMatrixPageFrames: ObservableList<MatrixPageFrames> = FXCollections.observableArrayList()
    private var listMatrixPageFaces: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
    private var listPersonsExtForFile: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private var listPersonsExtForShot: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private var listPersonsExtForScene: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private var listPersonsExtForEvent: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private var listShotsExtForScenes: ObservableList<ShotExt> = FXCollections.observableArrayList()
    private var listShotsExtForEvents: ObservableList<ShotExt> = FXCollections.observableArrayList()
    private var listFacesExt: ObservableList<FaceExt> = FXCollections.observableArrayList()

    private var countColumnsInPageFrames = 0
    private var countRowsInPageFrames = 0
    private var countColumnsInPageFaces = 0
    private var countRowsInPageFaces = 0

    private var currentMatrixPageFrames: MatrixPageFrames? = null
    private var currentMatrixPageFaces: MatrixPageFaces? = null
    private var currentMatrixFrame: MatrixFrame? = null
    private var currentMatrixFace: MatrixFace? = null
    private var currentShotExt: ShotExt? = null
    private var currentSelectedScenesExt: MutableList<SceneExt> = mutableListOf()
    private var currentSelectedEventsExt: MutableList<EventExt> = mutableListOf()
    private var currentShotExtForScene: ShotExt? = null
    private var currentPersonExt: PersonExt? = null
    private var currentNumPage = 0
    private var flowTblPagesFrames: VirtualFlow<*>? = null
    private var flowTblPagesFaces: VirtualFlow<*>? = null
    private var flowTblShots: VirtualFlow<*>? = null
    private var flowTblShotsForScenes: VirtualFlow<*>? = null
    private var flowTblShotsForEvents: VirtualFlow<*>? = null
    private var flowTblScenes: VirtualFlow<*>? = null
    private var flowTblEvents: VirtualFlow<*>? = null
    private var flowTblPersonsAll: VirtualFlow<*>? = null

    private var wasClickTablePagesFrames = false
    private var wasClickTablePagesFaces = false
    private var wasClickTableShots = false
    private var wasClickTableShotsForScenes = false
    private var wasClickTableShotsForEvents = false
    private var wasClickTableScenes = false
    private var wasClickTableEvents = false
    private var wasClickTablePersonsAllForScenes = false
    private var wasClickTablePersonsAllForEvents = false
    private var wasClickTablePersonsAllForFile = false
    private var wasClickFrameLabel = false

    private var selectedMatrixFaces: MutableSet<MatrixFace> = mutableSetOf()
    private var lastClickedMatrixFace: MatrixFace? = null
    private var currentPersonExtHovered: PersonExt? = null
    private var isNeedToAddDraggedFacesToPerson: Boolean = false
    private var currentMatrixPageFacesPageNumber: Int = 1

    var threadOnSelectScene: Thread? = null
    var threadOnSelectEvent: Thread? = null

    var projectPersonExtUndefinded: PersonExt? = null
    var projectPersonExtNonperson: PersonExt? = null
    var projectPersonExtExtras: PersonExt? = null

    fun editShots(fileExt: FileExt, hostServices: HostServices? = null) {
        currentFileExt = fileExt
        mainStage = Stage()
        try {
            val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("shots-edit-view.fxml"))
            mainStage?.scene = Scene(root)
            ShotsEditFXController.hostServices = hostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            onStart()
            mainStage?.showAndWait()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы ShotsEditFXController.")
        mainStage = null

    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            clearOnExit()
            println("Закрытие окна ShotsEditFXController.")
        }

        println("Инициализация ShotsEditFXController.")

        /**
         * Первичная инициализация переменных. Нужна для правильно работы при повторном открытии формы
         */
        listMatrixPageFrames = FXCollections.observableArrayList()
        listPersonsExtForFile = FXCollections.observableArrayList()
        listFacesExt = FXCollections.observableArrayList()
        countColumnsInPageFrames = 0
        countRowsInPageFrames = 0
        countColumnsInPageFaces = 0
        countRowsInPageFaces = 0
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
//        runListThreadsFramesFlagIsDone.value = false
        runListThreadsFacesFlagIsDone.value = false
        sbpCurrentMatrixPageWasChanged.value = false
        sbpCurrentMatrixFrameWasChanged.value = false
        sbpCurrentShotExtWasChanged.value = false
        sbpNeedCreatePagesWasChanged.value = false
        lastClickedMatrixFace = null

        mainStage?.title = "Редактор планов. Файл: ${currentFileExt!!.file.name}"
        isWorking = true

        projectPersonExtUndefinded = PersonController.getUndefindedExt(currentFileExt!!.projectExt)
        projectPersonExtNonperson = PersonController.getNonpersonExt(currentFileExt!!.projectExt)
        projectPersonExtExtras = PersonController.getExtrasExt(currentFileExt!!.projectExt)

        /**
         * LoadListPersonsExtForFile
         */
        LoadListPersonsExtForFile(listPersonsExtForFile, currentFileExt!!, pbPersonsForFile, null, isDoneLoadListPersonsExtForFile, false).start()
        isDoneLoadListPersonsExtForFile.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListPersonsExtForFile.set(false)
            }
        }

        /**
         * LoadListFramesExt
         */
        LoadListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb, isDoneLoadListFramesExt).start()
        isDoneLoadListFramesExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListFramesExt.set(false)
                listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.width, paneFrames!!.height, Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                tblPagesFrames!!.items = listMatrixPageFrames
                UpdateListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb, isDoneUpdateListFramesExt).start()

                LoadListShotsExt(currentFileExt!!.shotsExt, currentFileExt!!, pbShots, null, isDoneLoadListShotsExt).start()
                LoadListScenesExt(currentFileExt!!.scenesExt, currentFileExt!!, pbScenes, null, isDoneLoadListScenesExt).start()
                LoadListEventsExt(currentFileExt!!.eventsExt, currentFileExt!!, null, null, isDoneLoadListEventsExt).start()

            }
        }

        /**
         * LoadListScenesExt
         */
        isDoneLoadListScenesExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListScenesExt.set(false)
                tblScenes!!.items = currentFileExt!!.scenesExt
            }
        }

        /**
         * LoadListEventsExt
         */
        isDoneLoadListEventsExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListEventsExt.set(false)
                tblEvents!!.items = currentFileExt!!.eventsExt
            }
        }

        /**
         * LoadListShotsExt
         */
        isDoneLoadListShotsExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListShotsExt.set(false)
                currentFileExt!!.shotsExt.forEach { shotExt ->
                    shotExt.buttonGetType.setOnAction { onActionButtonGetShotType(shotExt) }
                }
                tblShots!!.items = currentFileExt!!.shotsExt
            }
        }

        /**
         * isDoneLoadListPersonFacesExt
         */
        isDoneLoadListPersonFacesExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListPersonFacesExt.set(false)

                listMatrixPageFaces = MatrixPageFaces.createPages(listFacesExt, paneFaces!!.width, paneFaces!!.height, Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
                tblPagesFaces!!.items = listMatrixPageFaces

                currentMatrixFace = listMatrixPageFaces.first().matrixFaces.first()
                if (currentMatrixPageFacesPageNumber > listMatrixPageFaces.size) currentMatrixPageFacesPageNumber = 1
                currentMatrixPageFaces = listMatrixPageFaces[currentMatrixPageFacesPageNumber-1]

                if (currentMatrixPageFaces != null) {
                    Platform.runLater{
                        showMatrixPageFaces(currentMatrixPageFaces!!)
                        tblPagesFaces!!.items = listMatrixPageFaces
                        tblPagesFaces!!.selectionModel.select(currentMatrixPageFaces)
                    }

                }

            }
        }



        /**
         * tblPagesFrames events
         */

        // placeholder
        tblPagesFrames?.placeholder = ProgressIndicator(-1.0)

        // PropertyValueFactory
        colDurationStart?.cellValueFactory = PropertyValueFactory("start")
        colDurationEnd?.cellValueFactory = PropertyValueFactory("end")
        colFrameStart?.cellValueFactory = PropertyValueFactory("firstFrameNumber")
        colFrameEnd?.cellValueFactory = PropertyValueFactory("lastFrameNumber")

        // items
        tblPagesFrames!!.items = listMatrixPageFrames

        // selectedItemProperty
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

        // onMouseEntered / onMouseExited
        tblPagesFrames!!.onMouseEntered = EventHandler { wasClickTablePagesFrames = true }
        tblPagesFrames!!.onMouseExited = EventHandler { wasClickTablePagesFrames = false }

        // flow
        tblPagesFrames!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblPagesFrames = kids[1] as VirtualFlow<*>
        })

        // Click
        tblPagesFrames!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 1) {
                    wasClickTablePagesFrames = true
                    wasClickFrameLabel = false
                    wasClickTableShots = false
                }
            }
        }

        /**
         * tblPagesFaces events
         */

        // PropertyValueFactory
        colTblPagesFacesNumber?.cellValueFactory = PropertyValueFactory("pageNumber")

        // items
        tblPagesFaces!!.items = listMatrixPageFaces

        // selectedItemProperty
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

        // onMouseEntered / onMouseExited
        tblPagesFaces!!.onMouseEntered = EventHandler { wasClickTablePagesFaces = true }
        tblPagesFaces!!.onMouseExited = EventHandler { wasClickTablePagesFaces = false }

        // flow
        tblPagesFaces!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblPagesFaces = kids[1] as VirtualFlow<*>
        })


        /**
         * tblShots events
         */

        // placeholder
        tblShots?.placeholder = ProgressIndicator(-1.0)

        // SelectionMode
        tblShots!!.selectionModel.selectionMode = SelectionMode.MULTIPLE

        // PropertyValueFactory
        colShotFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colShotTo?.cellValueFactory = PropertyValueFactory("labelLast1")
        colShotType?.cellValueFactory = PropertyValueFactory("labelType")
        colButtonGetType?.cellValueFactory = PropertyValueFactory("buttonGetType")

        // items
        tblShots!!.items = currentFileExt!!.shotsExt

        // selectedItemProperty
        tblShots!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out ShotExt?>?, oldValue: ShotExt?, newValue: ShotExt? ->
                if (newValue != null) {
                    Thread {
                        Platform.runLater{ tblPersonsAllForShot?.placeholder = ProgressIndicator(-1.0) }
                        currentShotExt = newValue
                        listPersonsExtForShot = FXCollections.observableList(currentShotExt!!.personsExt.filter { it.person.personType != PersonType.NONPERSON })
                        tblPersonsAllForShot!!.items = listPersonsExtForShot
                        Platform.runLater{tblPersonsAllForShot?.placeholder = Label("Shot not selected or don't have any persons.") }
                        if (wasClickTableShots) {
                            wasClickTableShots = false
                            goToFrame(getMatrixFrameByFrameExt(newValue.firstFrameExt))
                            if (!currentSelectedScenesExt.map{it.scene.id}.contains(currentShotExt!!.sceneExt!!.scene.id)) {
                                tblScenes!!.selectionModel.clearSelection()
                                val sceneToGo = currentFileExt!!.scenesExt.firstOrNull { it.scene.id == currentShotExt!!.sceneExt!!.scene.id }
                                if (sceneToGo != null) {
                                    tblScenes!!.selectionModel.select(sceneToGo)
                                    tblScenesSmartScroll(sceneToGo)
                                }
                            } else {
                                if (listShotsExtForScenes.map{it.shot.id}.contains(currentShotExt!!.shot.id)) {
                                    tblShotsForScenes!!.selectionModel.clearSelection()
                                    val shotForSceneToGo = listShotsExtForScenes.firstOrNull { it.shot.id == currentShotExt!!.shot.id }
                                    if (shotForSceneToGo != null) {
                                        tblShotsForScenes!!.selectionModel.select(shotForSceneToGo)
                                        tblShotsForScenesSmartScroll(shotForSceneToGo)
                                    }
                                }
                            }
                        } else {
                            tblShotsSmartScroll(newValue)
                        }
                    }.start()
                }
            }

        // onMouseEntered / onMouseExited
        tblShots!!.onMouseEntered = EventHandler { wasClickTableShots = true }
        tblShots!!.onMouseExited = EventHandler { wasClickTableShots = false }

        // flow
        tblShots!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblShots = kids[1] as VirtualFlow<*>
        })

        // Click
        tblShots!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 1) {
                    wasClickTableShots = true
                    wasClickFrameLabel = false
                    wasClickTablePagesFrames = false
                }
            }
        }

        /**
         * tblScenes events
         */

        // placeholder
        tblScenes?.placeholder = ProgressIndicator(-1.0)

        // SelectionMode
        tblScenes!!.selectionModel.selectionMode = SelectionMode.MULTIPLE

        // PropertyValueFactory
        colSceneName?.cellValueFactory = PropertyValueFactory("sceneNameLabel")
        colSceneFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colSceneTo?.cellValueFactory = PropertyValueFactory("labelLast1")

        // items
        tblScenes!!.items = currentFileExt!!.scenesExt

        // selectedItemProperty
        tblScenes!!.selectionModel.selectedItemProperty()
            .addListener { _, _, _ ->

                if (threadOnSelectScene!=null && threadOnSelectScene!!.isAlive) {
                    threadOnSelectScene!!.interrupt()
                    while (threadOnSelectScene!!.isAlive){
                        Thread.sleep(100)
                    }
                }
                threadOnSelectScene =
                Thread {
                    Platform.runLater {
                        tblShotsForScenes?.placeholder = ProgressIndicator(-1.0)
                        tblPersonsAllForScenes?.placeholder = ProgressIndicator(-1.0)
                    }
                    currentSelectedScenesExt = tblScenes!!.selectionModel.selectedItems
                    listShotsExtForScenes.clear()
                    listPersonsExtForScene.clear()
                    if (currentSelectedScenesExt.isNotEmpty()) {
                        val mapShotsExt: MutableMap<Long, ShotExt> = mutableMapOf()
                        val mapPersonsExt: MutableMap<Long, PersonExt> = mutableMapOf()
                        currentSelectedScenesExt.forEach { currentSceneExt->
                            if (Thread.currentThread().isInterrupted) return@Thread
                            mapShotsExt.putAll(currentSceneExt.shotsExt.map { Pair(it.shot.id, it) })
                            mapPersonsExt.putAll(currentSceneExt.personsExt.map { Pair(it.person.id, it) })
                        }
                        val tmpListShotsExt = mapShotsExt.values.toMutableList()
                        val tmpListPersonsExt = mapPersonsExt.values.toMutableList()
                        tmpListShotsExt.sort()
                        tmpListPersonsExt.sort()
                        listShotsExtForScenes.addAll(FXCollections.observableList(tmpListShotsExt))
                        listPersonsExtForScene.addAll(FXCollections.observableList(tmpListPersonsExt))

                    }
                    tblShotsForScenes!!.items = listShotsExtForScenes

                    if (wasClickTableScenes) {
                        val shotForSceneToGo = listShotsExtForScenes.firstOrNull()
                        if (shotForSceneToGo != null) {
                            tblShotsForScenes!!.selectionModel.select(shotForSceneToGo)
                            tblShotsForScenesSmartScroll(shotForSceneToGo)
                        }
                    } else {
                        if (listShotsExtForScenes.map{it.shot.id}.contains(currentShotExt!!.shot.id)) {
                            tblShotsForScenes!!.selectionModel.clearSelection()
                            val shotForSceneToGo = listShotsExtForScenes.firstOrNull { it.shot.id == currentShotExt!!.shot.id }
                            if (shotForSceneToGo != null) {
                                tblShotsForScenes!!.selectionModel.select(shotForSceneToGo)
                                tblShotsForScenesSmartScroll(shotForSceneToGo)
                            }
                        }
                    }

                    tblPersonsAllForScenes!!.items = listPersonsExtForScene

                    Platform.runLater {
                        tblShotsForScenes?.placeholder = Label("Scene not selected or don't have any shots.")
                        tblPersonsAllForScenes?.placeholder = Label("Scene not selected or don't have any persons.")
                    }

                }
                threadOnSelectScene!!.start()
            }

        // onMouseEntered / onMouseExited
        tblScenes!!.onMouseEntered = EventHandler { wasClickTableScenes = true }
        tblScenes!!.onMouseExited = EventHandler { wasClickTableScenes = false }

        // flow
        tblScenes!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblScenes = kids[1] as VirtualFlow<*>
        })

        /**
         * tblShotsForScenes events
         */

        // placeholder
        tblShotsForScenes?.placeholder = Label("Scene not selected or don't have any shots.")

        // PropertyValueFactory
        colShotForSceneFrom?.cellValueFactory = PropertyValueFactory("labelFirst2")
        colShotForSceneTo?.cellValueFactory = PropertyValueFactory("labelLast2")

        // selectedItemProperty
        tblShotsForScenes!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out ShotExt?>?, oldValue: ShotExt?, newValue: ShotExt? ->
                if (newValue != null) {
                    if (wasClickTableShotsForScenes || wasClickTableScenes) {
                        tblShots!!.selectionModel.clearSelection()
                        val shotToGo = currentFileExt!!.shotsExt.firstOrNull { it.shot.id == newValue.shot.id }
                        if (shotToGo != null) {
                            tblShots!!.selectionModel.select(shotToGo)
                            tblShotsSmartScroll(shotToGo)
                        }
                    } else {
                        tblShotsForScenesSmartScroll(newValue)
                    }
                }
            }

        // onMouseEntered / onMouseExited
        tblShotsForScenes!!.onMouseEntered = EventHandler { wasClickTableShotsForScenes = true }
        tblShotsForScenes!!.onMouseExited = EventHandler { wasClickTableShotsForScenes = false }

        // flow
        tblShotsForScenes!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblShotsForScenes = kids[1] as VirtualFlow<*>
        })


        /**
         * tblPersonsAllForScenes events
         */

        // placeholder
        tblPersonsAllForScenes?.placeholder = Label("Scene not selected or don't have any persons.")

        // PropertyValueFactory
        colTblPersonsAllForSceneName?.cellValueFactory = PropertyValueFactory("labelSmall")

        // onMouseEntered / onMouseExited
        tblPersonsAllForScenes!!.onMouseEntered = EventHandler { wasClickTablePersonsAllForScenes = true }
        tblPersonsAllForScenes!!.onMouseExited = EventHandler { wasClickTablePersonsAllForScenes = false }



        /**
         * tblEvents events
         */

        // placeholder
        tblEvents?.placeholder = ProgressIndicator(-1.0)

        // SelectionMode
        tblEvents!!.selectionModel.selectionMode = SelectionMode.MULTIPLE

        // PropertyValueFactory
        colEventName?.cellValueFactory = PropertyValueFactory("eventNameLabel")
        colEventFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colEventTo?.cellValueFactory = PropertyValueFactory("labelLast1")

        // items
        tblEvents!!.items = currentFileExt!!.eventsExt

        // selectedItemProperty
        tblEvents!!.selectionModel.selectedItemProperty()
            .addListener { _, _, _ ->

                if (threadOnSelectEvent!=null && threadOnSelectEvent!!.isAlive) {
                    threadOnSelectEvent!!.interrupt()
                    while (threadOnSelectEvent!!.isAlive){
                        Thread.sleep(100)
                    }
                }
                threadOnSelectEvent =
                    Thread {
                        Platform.runLater {
                            tblShotsForEvents?.placeholder = ProgressIndicator(-1.0)
                            tblPersonsAllForEvents?.placeholder = ProgressIndicator(-1.0)
                        }
                        currentSelectedEventsExt = tblEvents!!.selectionModel.selectedItems
                        listShotsExtForEvents.clear()
                        listPersonsExtForEvent.clear()
                        if (currentSelectedEventsExt.isNotEmpty()) {
                            val mapShotsExt: MutableMap<Long, ShotExt> = mutableMapOf()
                            val mapPersonsExt: MutableMap<Long, PersonExt> = mutableMapOf()
                            currentSelectedEventsExt.forEach { currentEventExt->
                                if (Thread.currentThread().isInterrupted) return@Thread
                                mapShotsExt.putAll(currentEventExt.shotsExt.map { Pair(it.shot.id, it) })
                                mapPersonsExt.putAll(currentEventExt.personsExt.map { Pair(it.person.id, it) })
                            }
                            val tmpListShotsExt = mapShotsExt.values.toMutableList()
                            val tmpListPersonsExt = mapPersonsExt.values.toMutableList()
                            tmpListShotsExt.sort()
                            tmpListPersonsExt.sort()
                            listShotsExtForEvents.addAll(FXCollections.observableList(tmpListShotsExt))
                            listPersonsExtForEvent.addAll(FXCollections.observableList(tmpListPersonsExt))

                        }
                        tblShotsForEvents!!.items = listShotsExtForEvents

                        if (wasClickTableEvents) {
                            val shotForEventToGo = listShotsExtForEvents.firstOrNull()
                            if (shotForEventToGo != null) {
                                tblShotsForEvents!!.selectionModel.select(shotForEventToGo)
                                tblShotsForEventsSmartScroll(shotForEventToGo)
                            }
                        } else {
                            if (listShotsExtForEvents.map{it.shot.id}.contains(currentShotExt!!.shot.id)) {
                                tblShotsForEvents!!.selectionModel.clearSelection()
                                val shotForEventToGo = listShotsExtForEvents.firstOrNull { it.shot.id == currentShotExt!!.shot.id }
                                if (shotForEventToGo != null) {
                                    tblShotsForEvents!!.selectionModel.select(shotForEventToGo)
                                    tblShotsForEventsSmartScroll(shotForEventToGo)
                                }
                            }
                        }

                        tblPersonsAllForEvents!!.items = listPersonsExtForEvent

                        Platform.runLater {
                            tblShotsForEvents?.placeholder = Label("Event not selected or don't have any shots.")
                            tblPersonsAllForEvents?.placeholder = Label("Event not selected or don't have any persons.")
                        }

                    }
                threadOnSelectEvent!!.start()
            }

        // onMouseEntered / onMouseExited
        tblEvents!!.onMouseEntered = EventHandler { wasClickTableEvents = true }
        tblEvents!!.onMouseExited = EventHandler { wasClickTableEvents = false }

        // flow
        tblEvents!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblEvents = kids[1] as VirtualFlow<*>
        })

        /**
         * tblShotsForEvents events
         */

        // placeholder
        tblShotsForEvents?.placeholder = Label("Event not selected or don't have any shots.")

        // PropertyValueFactory
        colShotForEventFrom?.cellValueFactory = PropertyValueFactory("labelFirst3")
        colShotForEventTo?.cellValueFactory = PropertyValueFactory("labelLast3")

        // selectedItemProperty
        tblShotsForEvents!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out ShotExt?>?, oldValue: ShotExt?, newValue: ShotExt? ->
                if (newValue != null) {
                    if (wasClickTableShotsForEvents || wasClickTableEvents) {
                        tblShots!!.selectionModel.clearSelection()
                        val shotToGo = currentFileExt!!.shotsExt.firstOrNull { it.shot.id == newValue.shot.id }
                        if (shotToGo != null) {
                            tblShots!!.selectionModel.select(shotToGo)
                            tblShotsSmartScroll(shotToGo)
                        }
                    } else {
                        tblShotsForEventsSmartScroll(newValue)
                    }
                }
            }

        // onMouseEntered / onMouseExited
        tblShotsForEvents!!.onMouseEntered = EventHandler { wasClickTableShotsForEvents = true }
        tblShotsForEvents!!.onMouseExited = EventHandler { wasClickTableShotsForEvents = false }

        // flow
        tblShotsForEvents!!.skinProperty().addListener(ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
            if (t1 == null) return@label
            val tvs = t1 as TableViewSkin<*>
            val kids = tvs.children
            if (kids == null || kids.isEmpty()) return@label
            flowTblShotsForEvents = kids[1] as VirtualFlow<*>
        })

        /**
         * tblPersonsAllForEvents events
         */

        // placeholder
        tblPersonsAllForEvents?.placeholder = Label("Event not selected or don't have any persons.")

        // PropertyValueFactory
        colTblPersonsAllForEventName?.cellValueFactory = PropertyValueFactory("labelSmall")

        // onMouseEntered / onMouseExited
        tblPersonsAllForEvents!!.onMouseEntered = EventHandler { wasClickTablePersonsAllForEvents = true }
        tblPersonsAllForEvents!!.onMouseExited = EventHandler { wasClickTablePersonsAllForEvents = false }


        /**
         * tblPersonsAllForFile events
         */

        // placeholder
        tblPersonsAllForFile?.placeholder = Label("File don't have any persons.")

        // PropertyValueFactory
        colTblPersonsAllForFileName?.cellValueFactory = PropertyValueFactory("labelSmall")

        // items
        tblPersonsAllForFile!!.items = listPersonsExtForFile

        // selectedItemProperty
        tblPersonsAllForFile!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out PersonExt?>?, oldValue: PersonExt?, newValue: PersonExt? ->
                if (newValue != null) {
                    currentPersonExt = newValue
                    doSelectFacesCb(null)
                }
            }

        // Click
        tblPersonsAllForFile!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentPersonExt != null) {
                        PersonEditFXController().editPerson(currentPersonExt!!, hostServices)
                    }
                }
            }
        }

        // onMouseEntered / onMouseExited
        tblPersonsAllForFile!!.onMouseEntered = EventHandler { wasClickTablePersonsAllForFile = true }
        tblPersonsAllForFile!!.onMouseExited = EventHandler { wasClickTablePersonsAllForFile = false }

        // onDragOver
        tblPersonsAllForFile!!.onDragOver = EventHandler { mouseEvent ->
            if (mouseEvent.dragboard.string == "labelFace") {
                mouseEvent.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }
            mouseEvent.consume()
        }

        // onDragDropped
        tblPersonsAllForFile!!.onDragDropped = EventHandler { mouseEvent ->
            var success = false
            if (mouseEvent.dragboard.string == "labelFace") {
                isNeedToAddDraggedFacesToPerson = true
                success = true
            }
            mouseEvent.isDropCompleted = success
            mouseEvent.consume()
        }

        // setRowFactory
        tblPersonsAllForFile!!.setRowFactory {
            val row: TableRow<PersonExt> = TableRow()
            row.hoverProperty().addListener { observable ->
                val personExt = row.item
                currentPersonExtHovered = if (row.isHover && personExt != null) personExt else null

                if (isNeedToAddDraggedFacesToPerson && currentPersonExtHovered != null) {
                    isNeedToAddDraggedFacesToPerson = false
                    if (!(listPersonsExtForFile.any { it.person == currentPersonExtHovered!!.person })) {
                        listPersonsExtForFile.add(currentPersonExtHovered)
                        listPersonsExtForFile.sort()
                    }
                    selectedMatrixFaces.forEach { mf->

                        mf.faceExt!!.personExt = currentPersonExtHovered as PersonExt
                        mf.faceExt.face.person = currentPersonExtHovered!!.person
                        mf.faceExt.face.personRecognizedName = if (currentPersonExtHovered!!.person.personType == PersonType.UNDEFINDED) "" else  currentPersonExtHovered!!.person.nameInRecognizer
                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                        currentMatrixPageFaces?.matrixFaces?.remove(mf)
                    }
                    selectedMatrixFaces.clear()
                    reorganizeMatrixFaces()
                }
            }
            return@setRowFactory row
        }

        /**
         * tblPersonsAllForShot events
         */

        // placeholder
        tblPersonsAllForShot?.placeholder = Label("Shot not selected or don't have any persons.")

        // PropertyValueFactory
        colTblPersonsAllForShotName?.cellValueFactory = PropertyValueFactory("labelSmall")

        // items
        tblPersonsAllForShot!!.items = listPersonsExtForShot

        // Click
        tblPersonsAllForShot!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (tblPersonsAllForShot!!.selectionModel.selectedItem != null) {
                        PersonEditFXController().editPerson(tblPersonsAllForShot!!.selectionModel.selectedItem, hostServices)
                    }
                }
            }
        }

        /**
         * paneFrames events
         */

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

        /**
         * lblFrameFull events
         */

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

        val contextMenuFrameFull = ContextMenu()
        val menuItemEditFrameFaces = MenuItem("Edit frame faces")
        menuItemEditFrameFaces.setOnAction {
            FrameFacesEditFXController.editFrame(currentMatrixFrame?.frameExt!!)
            if (currentShotExt != null) {
                LoadListPersonsExtForShot(listPersonsExtForShot, currentShotExt!!, pb, lblPb).run()
                tblPersonsAllForShot!!.items = listPersonsExtForShot
                loadPictureToFullFrameLabelForFrame(currentMatrixFrame)
            }
        }
        contextMenuFrameFull.items.add(menuItemEditFrameFaces)
        lblFrameFull!!.contextMenu = contextMenuFrameFull

        /**
         * paneFaces events
         */

        paneFaces!!.setOnScroll { e: ScrollEvent ->
            if (listMatrixPageFaces.isNotEmpty()) {
                val delta = if (e.deltaY > 0) -1 else 1
                if (currentMatrixPageFaces == null) {
                    goToFace(listMatrixPageFaces.first().matrixFaces.first())
                } else {
                    val faceToGo = if (delta < 0)
                        if (currentMatrixPageFaces!!.matrixFaces.isNotEmpty()) {
                            getPrevMatrixFace(currentMatrixPageFaces!!.matrixFaces.first())
                        } else {
                            null
                        }
                    else
                        if (currentMatrixPageFaces!!.matrixFaces.isNotEmpty()) {
                            getNextMatrixFace(currentMatrixPageFaces!!.matrixFaces.last())
                        } else {
                            null
                        }

                    goToFace(faceToGo)
                }
            }
        }

        /**
         * SBP events
         */

        sbpNeedCreatePagesWasChanged.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                sbpNeedCreatePagesWasChanged.value = false
                listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.width, paneFrames!!.height, Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
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

    }



    @FXML
    fun doOK(event: ActionEvent?) {
        isWorking = false
        mainStage?.close()
    }

    fun getNextMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        return if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.last() && matrixFrame.matrixPageFrames == listMatrixPageFrames.last()) matrixFrame
        else listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames)+1].matrixFrames.first()
    }

    fun getPrevMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        return if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.first() && matrixFrame.matrixPageFrames == listMatrixPageFrames.first()) matrixFrame
        else listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames)-1].matrixFrames.last()
    }

    fun getNextMatrixFace(matrixFace: MatrixFace): MatrixFace {
        return if (matrixFace == matrixFace.matrixPageFaces.matrixFaces.last() && matrixFace.matrixPageFaces == listMatrixPageFaces.last()) matrixFace
        else listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces)+1].matrixFaces.first()
    }

    fun getPrevMatrixFace(matrixFace: MatrixFace): MatrixFace {
        return if (matrixFace == matrixFace.matrixPageFaces.matrixFaces.first() && matrixFace.matrixPageFaces == listMatrixPageFaces.first()) matrixFace
        else listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces)-1].matrixFaces.last()
    }


    fun loadPictureToFullFrameLabelForFrame(matrixFrame: MatrixFrame?) {

        if (matrixFrame != null) {

            Thread {
                try {
                    val bufferedImage = FaceController.getOverlayedFrame(matrixFrame.frameExt!!, null)
                    val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                    Platform.runLater {
                        lblFrameFull?.graphic = imageView
                    }
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }
            }.start()

        }
    }

    fun loadPictureToFullFrameLabelForFace(frameExt: FrameExt, faceExt: FaceExt?) {

        if (faceExt != null) {

            Thread {
                try {
                    val bufferedImage = FaceController.getOverlayedFrame(frameExt, faceExt)
                    val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                    Platform.runLater {
                        lblFrameFull?.graphic = imageView
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
                shotExt.resetPreview()
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
                    unionShot.resetPreview()
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

            listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.width, paneFrames!!.height, Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
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
                tblShots!!.selectionModel.clearSelection()
                currentShotExt = getShotExtByFrameNumber(currentMatrixFrame!!.frameNumber!!)
                if (!tblShots!!.selectionModel.selectedItems.contains(currentShotExt)) {

                    tblShots!!.selectionModel.select(currentShotExt)
                }

                currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderSelected
                loadPictureToFullFrameLabelForFrame(currentMatrixFrame)
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
                Platform.runLater{tblPagesFrames?.scrollTo(matrixPageFrames)}
            }
        }
    }

    fun tblPagesFacesSmartScroll(matrixPageFaces: MatrixPageFaces?) {
        if (flowTblPagesFaces != null && flowTblPagesFaces!!.cellCount > 0) {
            val first: Int = flowTblPagesFaces!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPagesFaces!!.lastVisibleCell.getIndex()
            val selected = tblPagesFaces!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblPagesFaces?.scrollTo(matrixPageFaces)}
            }
        }
    }

    fun tblShotsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShots != null && flowTblShots!!.cellCount > 0) {
            val first: Int = flowTblShots!!.firstVisibleCell.getIndex()
            val last: Int = flowTblShots!!.lastVisibleCell.getIndex()
            val selected = tblShots!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblShots?.scrollTo(shotExt)}
            }
        }
    }

    fun tblScenesSmartScroll(sceneExt: SceneExt?) {
        if (flowTblScenes != null && flowTblScenes!!.cellCount > 0) {
            val first: Int = flowTblScenes!!.firstVisibleCell.index
            val last: Int = flowTblScenes!!.lastVisibleCell.index
            val selected = tblScenes!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblScenes?.scrollTo(sceneExt)}
            }
        }
    }

    fun tblShotsForScenesSmartScroll(shotExt: ShotExt?) {
        if (flowTblShotsForScenes != null && flowTblShotsForScenes!!.cellCount > 0) {
            val first: Int = flowTblShotsForScenes!!.firstVisibleCell.index
            val last: Int = flowTblShotsForScenes!!.lastVisibleCell.index
            val selected = tblShotsForScenes!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblShotsForScenes?.scrollTo(shotExt)}
            }
        }
    }
    
    fun tblShotsForEventsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShotsForEvents != null && flowTblShotsForEvents!!.cellCount > 0) {
            val first: Int = flowTblShotsForEvents!!.firstVisibleCell.index
            val last: Int = flowTblShotsForEvents!!.lastVisibleCell.index
            val selected = tblShotsForEvents!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblShotsForEvents?.scrollTo(shotExt)}
            }
        }
    }

    fun tblPersonsAllSmartScroll(personExt: PersonExt?) {
        if (flowTblPersonsAll != null && flowTblPersonsAll!!.cellCount > 0) {
            val first: Int = flowTblPersonsAll!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPersonsAll!!.lastVisibleCell.getIndex()
            val selected = tblPersonsAllForFile!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater{tblPersonsAllForFile?.scrollTo(personExt)}
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

            val contextMenuFrameFull = ContextMenu()
            val menuItemEditFrameFaces = MenuItem("Edit frame faces")
            menuItemEditFrameFaces.setOnAction {
                FrameFacesEditFXController.editFrame(currentMatrixFrame?.frameExt!!)
                if (currentShotExt != null) {
                    LoadListPersonsExtForShot(listPersonsExtForShot, currentShotExt!!, pb, lblPb).run()
                    tblPersonsAllForShot!!.items = listPersonsExtForShot
                    loadPictureToFullFrameLabelForFrame(currentMatrixFrame)
                }
            }
            contextMenuFrameFull.items.add(menuItemEditFrameFaces)
            lbl.contextMenu = contextMenuFrameFull

            val x: Double = widthPadding + matrixFrame.column * (Main.PREVIEW_FRAME_W + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFrame.row * (Main.PREVIEW_FRAME_H + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H) //устанавливаем ширину и высоту лейбла
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
            val flagCurrFrameContainFaces = currentFileExt!!.framesWithFaces.contains(matrixFrame.frameExt!!.frame.frameNumber)

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
                flagCurrFrameIsIFrame || flagCurrFrameIsFind || flagCurrFrameIsManualAdd || flagCurrFrameIsManualCancel || flagCurrFrameContainFaces) {
//                resultImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                resultImage = matrixFrame.frameExt!!.biSmall
            }

            if (flagCurrFrameContainFaces) {
                resultImage = OverlayImage.setOverlayTriangle(resultImage!!,4,0.2, Color.BLUE, 1.0F)
            }
            if (flagCurrFrameIsIFrame) resultImage = OverlayImage.setOverlayIFrame(resultImage!!)
            if (flagCurrFrameIsFind) resultImage = if (flagCurrFrameIsManualCancel) OverlayImage.cancelOverlayFirstFrameManual(resultImage!!) else OverlayImage.setOverlayFirstFrameFound(resultImage!!)
            if (flagNextFrameIsFind) resultImage = if (flagNextFrameIsManualCancel) OverlayImage.cancelOverlayLastFrameManual(resultImage!!) else OverlayImage.setOverlayLastFrameFound(resultImage!!)
            if (flagCurrFrameIsManualAdd) resultImage = OverlayImage.setOverlayFirstFrameManual(resultImage!!)
            if (flagNextFrameIsManualAdd) resultImage = OverlayImage.setOverlayLastFrameManual(resultImage!!)

            if (resultImage != null) {
                val screenImageView = ImageView(ConvertToFxImage.convertToFxImage(resultImage)) // загружаем ресайзный буфер в новый вьювер
                screenImageView.fitWidth = Main.PREVIEW_FRAME_W // устанавливаем ширину вьювера
                screenImageView.fitHeight = Main.PREVIEW_FRAME_H // устанавливаем высоту вьювера
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
                        tblShots!!.selectionModel.clearSelection()
                        tblShots!!.selectionModel.select(currentShotExt)
                        loadPictureToFullFrameLabelForFrame(currentMatrixFrame)
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
                                matrixFrame.frameExt!!.labelSmall.graphic = matrixFrame.frameExt!!.previewSmall
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

//    fun changePersonForFace() {
//        matrixFace.faceExt!!.personExt = personExtNonperson
//        matrixFace.faceExt!!.face.person = personExtNonperson.person
//        FaceController.save(matrixFace.faceExt!!.face)
//        var indexPreviousFaceExt = matrixPageFaces.matrixFaces.indexOf(matrixFace)-1
//        if (indexPreviousFaceExt < 0 ) indexPreviousFaceExt = 0
//        listFacesExt.remove(matrixFace.faceExt!!)
//        if (matrixPageFaces.matrixFaces.size > 0) {
//            matrixPageFaces.matrixFaces.remove(matrixFace)
//            lbl.graphic = null
//            val matrixFaceToGo = matrixPageFaces.matrixFaces[indexPreviousFaceExt]
//            goToFace(matrixFaceToGo)
//        }
//    }

    fun showMatrixPageFaces(matrixPageFaces: MatrixPageFaces) {
        val heightPadding = 10 // по высоте двойной отступ
        val widthPadding = 10 // по ширине двойной отступ
        val pane: Pane = paneFaces!!
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFace in matrixPageFaces.matrixFaces) {
            val lbl: Label = matrixFace.faceExt?.labelSmall!!
            val x: Double = widthPadding + matrixFace.column * (Main.PREVIEW_FACE_W + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFace.row * (Main.PREVIEW_FACE_H + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H) //устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault //устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER //устанавливаем позиционирование по центру


            val screenImageView = matrixFace.faceExt.previewSmall
            screenImageView.fitWidth = Main.PREVIEW_FACE_W // устанавливаем ширину вьювера
            screenImageView.fitHeight = Main.PREVIEW_FACE_H // устанавливаем высоту вьювера
            lbl.graphic = null //сбрасываем графику лейбла
            lbl.graphic = screenImageView // устанавливаем вьювер источником графики для лейбла
            pane.children.add(lbl)

            if (matrixFace.column == 0 || matrixFace.column == currentMatrixPageFaces!!.countColumns + 1) continue

            val contextMenu = ContextMenu()

            var menuItem = MenuItem("UNDEFINDED")
            menuItem.setOnAction {

                var personExtUndefinded = listPersonsExtForFile.firstOrNull{it.person.personType == PersonType.UNDEFINDED}
                if (personExtUndefinded == null) {
                    personExtUndefinded = projectPersonExtUndefinded
                    listPersonsExtForFile.add(personExtUndefinded)
                    listPersonsExtForFile.sort()
                }

                selectedMatrixFaces.add(matrixFace)
                selectedMatrixFaces.forEach { mf->

                    if (mf.faceExt!!.personExt.person.personType != PersonType.UNDEFINDED) {
                        mf.faceExt.personExt = personExtUndefinded!!
                        mf.faceExt.face.person = personExtUndefinded.person
                        mf.faceExt.face.personRecognizedName = if (personExtUndefinded.person.personType == PersonType.UNDEFINDED) "" else personExtUndefinded.person.nameInRecognizer
                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        if (matrixPageFaces.matrixFaces.size > 0) {
                            matrixPageFaces.matrixFaces.remove(mf)
                            mf.faceExt.labelSmall.graphic = null
                            mf.faceExt.labelSmall.style = fxBorderDefault
                        }
                    }

                }
                selectedMatrixFaces.clear()
                reorganizeMatrixFaces()
            }
            contextMenu.items.add(menuItem)

            menuItem = MenuItem("NONPERSON")
            menuItem.setOnAction {

                var personExtNonperson = listPersonsExtForFile.firstOrNull{it.person.personType == PersonType.NONPERSON}
                if (personExtNonperson == null) {
                    personExtNonperson = projectPersonExtNonperson
                    listPersonsExtForFile.add(personExtNonperson)
                    listPersonsExtForFile.sort()
                }

                selectedMatrixFaces.add(matrixFace)
                selectedMatrixFaces.forEach { mf->

                    if (mf.faceExt!!.personExt.person.personType != PersonType.NONPERSON) {
                        mf.faceExt.personExt = personExtNonperson!!
                        mf.faceExt.face.person = personExtNonperson.person
                        mf.faceExt.face.personRecognizedName = if (personExtNonperson.person.personType == PersonType.UNDEFINDED) "" else personExtNonperson.person.nameInRecognizer
                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        if (matrixPageFaces.matrixFaces.size > 0) {
                            matrixPageFaces.matrixFaces.remove(mf)
                            mf.faceExt.labelSmall.graphic = null
                            mf.faceExt.labelSmall.style = fxBorderDefault
                        }
                    }

                }
                selectedMatrixFaces.clear()
                reorganizeMatrixFaces()
            }
            contextMenu.items.add(menuItem)

            menuItem = MenuItem("EXTRAS")
            menuItem.setOnAction {

                var personExtExtras = listPersonsExtForFile.firstOrNull{it.person.personType == PersonType.EXTRAS}
                if (personExtExtras == null) {
                    personExtExtras = projectPersonExtExtras
                    listPersonsExtForFile.add(personExtExtras)
                    listPersonsExtForFile.sort()
                }

                selectedMatrixFaces.add(matrixFace)
                selectedMatrixFaces.forEach { mf->

                    if (mf.faceExt!!.personExt.person.personType != PersonType.EXTRAS) {
                        mf.faceExt.personExt = personExtExtras!!
                        mf.faceExt.face.person = personExtExtras.person
                        mf.faceExt.face.personRecognizedName = if (personExtExtras.person.personType == PersonType.UNDEFINDED) "" else personExtExtras.person.nameInRecognizer
                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        if (matrixPageFaces.matrixFaces.size > 0) {
                            matrixPageFaces.matrixFaces.remove(mf)
                            mf.faceExt.labelSmall.graphic = null
                            mf.faceExt.labelSmall.style = fxBorderDefault
                        }
                    }

                }
                selectedMatrixFaces.clear()
                reorganizeMatrixFaces()

            }

            contextMenu.items.add(menuItem)

            menuItem = MenuItem("SELECT PERSON")
            menuItem.setOnAction {

                selectedMatrixFaces.add(matrixFace)
                val selectedPerson = PersonSelectFXController().getPersonExt(currentFileExt!!.projectExt)

                if (selectedPerson != null) {
                    if (!(listPersonsExtForFile.any { it.person.id == selectedPerson.person.id })) {
                        listPersonsExtForFile.add(selectedPerson)
                        listPersonsExtForFile.sort()
                    }
                    selectedMatrixFaces.forEach { mf->

                        mf.faceExt!!.personExt = selectedPerson
                        mf.faceExt.face.person = selectedPerson.person
                        mf.faceExt.face.personRecognizedName = if (selectedPerson.person.personType == PersonType.UNDEFINDED) "" else selectedPerson.person.nameInRecognizer
                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        if (matrixPageFaces.matrixFaces.size > 0) {
                            matrixPageFaces.matrixFaces.remove(mf)
                            mf.faceExt.labelSmall.graphic = null
                            mf.faceExt.labelSmall.style = fxBorderDefault
                        }

                    }

                }

                selectedMatrixFaces.clear()
                reorganizeMatrixFaces()

            }

            contextMenu.items.add(menuItem)

            menuItem = MenuItem("CREATE NEW PERSON")
            menuItem.setOnAction {

                selectedMatrixFaces.add(matrixFace)

                val dialog = TextInputDialog("New person")

                dialog.title = "Create new person"
                dialog.headerText = "Enter person name:"
                dialog.contentText = "Name:"

                val result: Optional<String> = dialog.showAndWait()
                var personName: String? = null
                result.ifPresent { name -> personName = name }

                if (personName != null) {
                    val newPerson = PersonController.create(currentFileExt!!.projectExt.project, personName!!,
                        PersonType.PERSON,"", currentFileExt!!.file.id, matrixFace.faceExt.frameNumber,
                        matrixFace.faceExt.faceNumberInFrame)
                    val selectedPerson = PersonExt(newPerson, currentFileExt!!.projectExt)
                    if (!listPersonsExtForFile.contains(selectedPerson)) {
                        listPersonsExtForFile.add(selectedPerson)
                        listPersonsExtForFile.sort()
                    }

                    selectedMatrixFaces.forEach { mf ->
                        mf.faceExt!!.personExt = selectedPerson
                        mf.faceExt.face.person = selectedPerson.person
                        mf.faceExt.face.personRecognizedName = if (selectedPerson.person.personType == PersonType.UNDEFINDED) "" else selectedPerson.person.nameInRecognizer


                        FaceController.save(mf.faceExt.face)
                        listFacesExt.remove(mf.faceExt)
                        if (matrixPageFaces.matrixFaces.size > 0) {
                            matrixPageFaces.matrixFaces.remove(mf)
                            mf.faceExt.labelSmall.graphic = null
                            mf.faceExt.labelSmall.style = fxBorderDefault
                        }
                    }
                    PersonEditFXController().editPerson(selectedPerson, hostServices)

                }

                selectedMatrixFaces.clear()
                reorganizeMatrixFaces()
            }

            contextMenu.items.add(menuItem)

            menuItem = MenuItem("Set as person picture")
            menuItem.setOnAction {

                val selectedPerson = listPersonsExtForFile.firstOrNull { it.person.id == currentPersonExt!!.person.id }
                if (selectedPerson != null) {

                    try {
                        IOFile(currentPersonExt!!.pathToSmall).delete()
                        IOFile(currentPersonExt!!.pathToMedium).delete()
                    } catch (_: IOException) {
                    }

                    selectedPerson.person.fileIdForPreview = currentFileExt!!.file.id
                    selectedPerson.person.faceNumberForPreview = matrixFace.faceExt.face.faceNumberInFrame
                    selectedPerson.person.frameNumberForPreview = matrixFace.faceExt.face.frameNumber
                    PersonController.save(selectedPerson.person)
                    selectedPerson.resetPreview()
                    selectedPerson.labelSmall
                    selectedMatrixFaces.clear()
                    tblPersonsAllForFile!!.refresh()
                    currentPersonExt = selectedPerson
                }

            }
            contextMenu.items.add(menuItem)

            if (!matrixFace.faceExt.face.isManual && !matrixFace.faceExt.face.isExample) {
                menuItem = MenuItem("Set as EXAMPLE")
                menuItem.setOnAction {
                    selectedMatrixFaces.add(matrixFace)
                    val selectedPerson = listPersonsExtForFile.firstOrNull { it.person.id == currentPersonExt!!.person.id }
                    if (selectedPerson != null) {
                        selectedMatrixFaces.forEach { mf ->
                            if (!mf.faceExt!!.face.isManual && !mf.faceExt.face.isExample) {
                                mf.faceExt.face.isExample = true
                                FaceController.save(mf.faceExt.face)
                                try {
                                    IOFile(mf.faceExt.pathToPreviewFile).delete()
                                } catch (_: IOException) {
                                }

                                val frame = Main.frameRepo.findByFileIdAndFrameNumber(mf.faceExt.fileId, mf.faceExt.frameNumber).firstOrNull()
                                if (frame != null) {
                                    frame.file = mf.faceExt.fileExt.file
                                    val frameExt = FrameExt(frame, mf.faceExt.fileExt)
                                    if (!IOFile(mf.faceExt.pathToPreviewFile).parentFile.exists()) IOFile(mf.faceExt.pathToPreviewFile).parentFile.mkdir()
                                    val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
                                    var bi = OverlayImage.extractRegion(biSource, mf.faceExt.startX, mf.faceExt.startY, mf.faceExt.endX, mf.faceExt.endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                                    if (mf.faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.GREEN, 1.0F)
                                    if (mf.faceExt.face.isManual) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.RED, 1.0F)
                                    val outputfile = IOFile(mf.faceExt.pathToPreviewFile)
                                    ImageIO.write(bi, "jpg", outputfile)
                                }
                                mf.faceExt.resetPreviewSmall()
                                mf.faceExt.previewSmall
                            }
                        }
                    }
                    selectedMatrixFaces.clear()
                    reorganizeMatrixFaces()
                }
                contextMenu.items.add(menuItem)
            }

            if (!matrixFace.faceExt.face.isManual && matrixFace.faceExt.face.isExample) {
                menuItem = MenuItem("Remove from EXAMPLE")
                menuItem.setOnAction {
                    selectedMatrixFaces.add(matrixFace)
                    val selectedPerson = listPersonsExtForFile.firstOrNull { it.person.id == currentPersonExt!!.person.id }
                    if (selectedPerson != null) {
                        selectedMatrixFaces.forEach { mf ->
                            if (!mf.faceExt!!.face.isManual && mf.faceExt.face.isExample) {
                                mf.faceExt.face.isExample = false
                                FaceController.save(mf.faceExt.face)
                                try {
                                    IOFile(mf.faceExt.pathToPreviewFile).delete()
                                } catch (_: IOException) {
                                }

                                val frame = Main.frameRepo.findByFileIdAndFrameNumber(mf.faceExt.fileId, mf.faceExt.frameNumber).firstOrNull()
                                if (frame != null) {
                                    frame.file = mf.faceExt.fileExt.file
                                    val frameExt = FrameExt(frame, mf.faceExt.fileExt)
                                    if (!IOFile(mf.faceExt.pathToPreviewFile).parentFile.exists()) IOFile(mf.faceExt.pathToPreviewFile).parentFile.mkdir()
                                    val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
                                    var bi = OverlayImage.extractRegion(biSource, mf.faceExt.startX, mf.faceExt.startY, mf.faceExt.endX, mf.faceExt.endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                                    if (mf.faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.GREEN, 1.0F)
                                    if (mf.faceExt.face.isManual) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.RED, 1.0F)
                                    val outputfile = IOFile(mf.faceExt.pathToPreviewFile)
                                    ImageIO.write(bi, "jpg", outputfile)
                                }
                                mf.faceExt.resetPreviewSmall()
                                mf.faceExt.previewSmall
                            }
                        }
                    }
                    selectedMatrixFaces.clear()
                    reorganizeMatrixFaces()
                }
                contextMenu.items.add(menuItem)
            }


            lbl.contextMenu = contextMenu

            //событие "наведение мыши"
            lbl.onMouseEntered = EventHandler {
                lbl.style = if (selectedMatrixFaces.contains(matrixFace)) fxBorderSelectedFocused else fxBorderFocused
                lbl.toFront()
            }

            //событие "уход мыши"
            lbl.onMouseExited = EventHandler {
                lbl.style = if (selectedMatrixFaces.contains(matrixFace)) fxBorderSelected else fxBorderDefault
            }

            lbl.onMouseClicked = EventHandler { mouseEvent ->

                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 1) {
                        val frameExt = currentFileExt!!.framesExt.firstOrNull { it.frame.frameNumber == matrixFace.faceExt.frameNumber }
                        if (frameExt != null) loadPictureToFullFrameLabelForFace(frameExt, matrixFace.faceExt)
                        if (!isPressedControl && !isPressedShift) {
                            selectedMatrixFaces.forEach { it.faceExt?.labelSmall?.style = fxBorderDefault }
                            selectedMatrixFaces.clear()
                        } else if(isPressedShift) {
                            val indexLastClicked = currentMatrixPageFaces!!.matrixFaces.indexOf(lastClickedMatrixFace)
                            val indexCurrentClicked = currentMatrixPageFaces!!.matrixFaces.indexOf(matrixFace)
                            if (indexLastClicked < indexCurrentClicked) {
                                for (i in indexLastClicked..indexCurrentClicked) {
                                    val mf = currentMatrixPageFaces!!.matrixFaces[i]
                                    selectedMatrixFaces.add(mf)
                                    mf.faceExt?.labelSmall?.style = fxBorderSelected
                                }
                            }
                        }
                        lastClickedMatrixFace = matrixFace
                        selectedMatrixFaces.add(matrixFace)

                        currentMatrixFace!!.faceExt?.labelSmall!!.style = if (selectedMatrixFaces.contains(currentMatrixFace)) fxBorderSelected else fxBorderDefault
                        currentMatrixFace = matrixFace
                        lbl.style = fxBorderSelected
//                        loadPictureToFullFrameLabel(currentMatrixFace.faceExt.pathToFrameFile)
                    }

                }
            }

            lbl.onDragDetected = EventHandler { mouseEvent ->
                selectedMatrixFaces.add(matrixFace)
                val db: Dragboard = lbl.startDragAndDrop(*TransferMode.ANY)
                val content = ClipboardContent()
                content.putString("labelFace")
                db.setContent(content)
                mouseEvent.consume()
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

//        if (runListThreadsFramesFlagIsDone.value) {
            val paneFramesWidth: Double = paneFrames!!.getWidth() // ширина центрального пэйна
            val paneFramesHeight: Double = paneFrames!!.getHeight() // высота центрального пейна
            val widthFramePadding = (Main.PREVIEW_FRAME_W + 2) * 2 + 20 // по ширине двойной отступ
            val heightFramePadding = (Main.PREVIEW_FRAME_H + 2) * 2 + 20 // по высоте двойной отступ
            if (paneFramesWidth > widthFramePadding && paneFramesHeight > heightFramePadding) {
                val prevCountColumnsInPage = countColumnsInPageFrames
                val prevCountRowsInPage = countRowsInPageFrames
                countColumnsInPageFrames =
                    ((paneFramesWidth - widthFramePadding) / (Main.PREVIEW_FRAME_W + 2)).toInt() // количество столбцов, которое влезет на экран
                countRowsInPageFrames =
                    ((paneFramesHeight - heightFramePadding) / (Main.PREVIEW_FRAME_H + 2)).toInt() // количество строк, которое влезет на экран

                // если значения кол-ва столбцов и/или строк изменилось при ресайзе
                if (prevCountColumnsInPage != countColumnsInPageFrames || prevCountRowsInPage != countRowsInPageFrames) {
                    val frameNumber = currentMatrixFrame?.frameNumber ?: 1
                    listMatrixPageFrames = MatrixPageFrames.createPages(currentFileExt!!.framesExt, paneFrames!!.getWidth(), paneFrames!!.getHeight(), Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                    tblPagesFrames!!.items = listMatrixPageFrames
                    currentMatrixFrame = getMatrixFrameByFrameNumber(frameNumber)
                    currentMatrixPageFrames = getMatrixPageFramesByFrame(frameNumber)
                    currentShotExt = getShotExtByFrameNumber(frameNumber)
                    showMatrixPageFrames(currentMatrixPageFrames!!)
                    tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
                    goToFrame(currentMatrixFrame)

                }
            }
//        }

//        if (runListThreadsFramesFlagIsDone.value) {
            val paneFacesWidth: Double = paneFaces!!.getWidth() // ширина центрального пэйна
            val paneFacesHeight: Double = paneFaces!!.getHeight() // высота центрального пейна
            val widthFacePadding = (Main.PREVIEW_FACE_W + 2) * 2 + 20 // по ширине двойной отступ
            val heightFacePadding = (Main.PREVIEW_FACE_H + 2) * 2 + 20 // по высоте двойной отступ
            if (paneFacesWidth > widthFacePadding && paneFacesHeight > heightFacePadding) {
                val prevCountColumnsInPage = countColumnsInPageFaces
                val prevCountRowsInPage = countRowsInPageFaces
                countColumnsInPageFaces =
                    ((paneFacesWidth - widthFacePadding) / (Main.PREVIEW_FRAME_W + 2)).toInt() // количество столбцов, которое влезет на экран
                countRowsInPageFaces =
                    ((paneFacesHeight - heightFacePadding) / (Main.PREVIEW_FRAME_H + 2)).toInt() // количество строк, которое влезет на экран

                // если значения кол-ва столбцов и/или строк изменилось при ресайзе
                if (prevCountColumnsInPage != countColumnsInPageFaces || prevCountRowsInPage != countRowsInPageFaces) {

                    listMatrixPageFaces = MatrixPageFaces.createPages(listFacesExt, paneFaces!!.getWidth(), paneFaces!!.getHeight(), Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
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
//        }


    }

    fun onActionButtonGetShotType(shotExt: ShotExt) {
        val contextMenuShotType = ContextMenu()
        ShotTypePerson.values().forEach { shotTypePerson ->
            val imageView = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shotTypePerson.pathToPicture))))
            val contextMenuShotTypeItem = MenuItem(null, imageView)
            contextMenuShotTypeItem.onAction = EventHandler { e: ActionEvent? ->
                shotExt.shot.typePerson = shotTypePerson
                ShotController.save(shotExt.shot)
                shotExt.resetPreview()
                shotExt.labelType
                tblShots!!.refresh()
            }
            contextMenuShotType.items.add(contextMenuShotTypeItem)
        }
//        shotExt.labelType!!.contextMenu = contextMenuShotType
        shotExt.buttonGetType.contextMenu = contextMenuShotType
        val screenBounds: Bounds = shotExt.buttonGetType.localToScreen(shotExt.buttonGetType.boundsInLocal)
        contextMenuShotType.show(mainStage, screenBounds.minX +screenBounds.width, screenBounds.minY)
    }

    fun reorganizeMatrixFaces() {
        val list: MutableList<FaceExt> = listMatrixPageFaces.flatMap { it.matrixFaces }.mapNotNull { it.faceExt }.toMutableSet().toMutableList()
        list.sort()

        if (list.isEmpty()) {
            var index = listPersonsExtForFile.indexOf(currentPersonExt) - 1
            if (index < 0) index = 0
            listPersonsExtForFile.remove(currentPersonExt)
            tblPersonsAllForFile!!.selectionModel.select(index)
        } else {
            listFacesExt = FXCollections.observableArrayList(list)
            currentMatrixPageFacesPageNumber = currentMatrixPageFaces!!.pageNumber
            runListThreadsFacesFlagIsDone.set(true)
        }

    }


    @FXML
    fun doSelectFacesCb(event: ActionEvent?) {
        isDoneLoadListPersonFacesExt.set(false)
        if (rbFaceFile!!.isSelected) {
            LoadListPersonFacesExtForFile(listFacesExt, currentFileExt!!, currentPersonExt!!, pbFaces, null,
                cbFacesNotExample!!.isSelected, cbFacesExample!!.isSelected, cbFacesNotManual!!.isSelected, cbFacesManual!!.isSelected,
                isDoneLoadListPersonFacesExt).start()
        } else if (rbFaceAll!!.isSelected) {
            LoadListPersonFacesExtForAll(listFacesExt, currentFileExt!!.projectExt, currentPersonExt!!, pbFaces, lblPb,
                cbFacesNotExample!!.isSelected, cbFacesExample!!.isSelected, cbFacesNotManual!!.isSelected, cbFacesManual!!.isSelected,
                isDoneLoadListPersonFacesExt).start()
        }
    }

    @FXML
    fun doSelectFacesRb(event: ActionEvent?) {
    }

    @FXML
    fun doSelectPersonsRb(event: ActionEvent?) {
    }

    @FXML
    fun doCreateNewSceneBySelectedShots(event: ActionEvent?) {

        val selectedShots = tblShots!!.selectionModel.selectedItems
        if (selectedShots.isNotEmpty()) {
            val selectedShotsExtSorted = selectedShots.toMutableList()
            selectedShotsExtSorted.sort()
            val listShotsExt: MutableList<ShotExt> = mutableListOf()
            for ((i, shotExt) in selectedShotsExtSorted.withIndex()) {
                if (i < selectedShotsExtSorted.size-1) {
                    if (shotExt.shot.lastFrameNumber+1 == selectedShotsExtSorted[i+1].shot.firstFrameNumber) {
                        listShotsExt.add(shotExt)
                    } else {
                        break
                    }
                } else {
                    listShotsExt.add(shotExt)
                }
            }
            if (listShotsExt.isNotEmpty()) {
                val sceneExt = SceneController.createSceneExt(listShotsExt)
                if (sceneExt != null) {
                    LoadListScenesExt(currentFileExt!!.scenesExt, currentFileExt!!, pb, lblPb).run()
                    tblScenes!!.items = currentFileExt!!.scenesExt
                    val sceneInTable = currentFileExt!!.scenesExt.firstOrNull { it.scene.id == sceneExt.scene.id }
                    if (sceneInTable != null) tblScenes!!.selectionModel.select(sceneInTable)
                    selectedShots.forEach {
                        it.resetPreview()
                        it.labelsFirst
                        it.labelsLast
                    }
                    val prevIndex = currentFileExt!!.shotsExt.indexOf(selectedShots.first())-1

                    if (prevIndex >= 0) {
                        val shotExt = currentFileExt!!.shotsExt[prevIndex]
                        shotExt.resetPreview()
                        shotExt.labelsFirst
                        shotExt.labelsLast
                    }

                    val nextIndex = currentFileExt!!.shotsExt.indexOf(selectedShots.last())+1
                    if (nextIndex < currentFileExt!!.shotsExt.size) {
                        val shotExt = currentFileExt!!.shotsExt[nextIndex]
                        shotExt.resetPreview()
                        shotExt.labelsFirst
                        shotExt.labelsLast
                    }

                    tblShots!!.refresh()
                }
            }

        }

    }


    @FXML
    fun doCreateNewEventBySelectedShots(event: ActionEvent?) {

        val selectedShots = tblShots!!.selectionModel.selectedItems
        if (selectedShots.isNotEmpty()) {
            val selectedShotsExtSorted = selectedShots.toMutableList()
            selectedShotsExtSorted.sort()
            val listShotsExt: MutableList<ShotExt> = mutableListOf()
            for ((i, shotExt) in selectedShotsExtSorted.withIndex()) {
                if (i < selectedShotsExtSorted.size-1) {
                    if (shotExt.shot.lastFrameNumber+1 == selectedShotsExtSorted[i+1].shot.firstFrameNumber) {
                        listShotsExt.add(shotExt)
                    } else {
                        break
                    }
                } else {
                    listShotsExt.add(shotExt)
                }
            }
            if (listShotsExt.isNotEmpty()) {
                val sceneExt = EventController.createEventExt(listShotsExt)
                if (sceneExt != null) {
                    LoadListEventsExt(currentFileExt!!.eventsExt, currentFileExt!!, pb, lblPb).run()
                    tblEvents!!.items = currentFileExt!!.eventsExt
                    val eventInTable = currentFileExt!!.eventsExt.firstOrNull { it.event.id == sceneExt.event.id }
                    if (eventInTable != null) tblEvents!!.selectionModel.select(eventInTable)
                    selectedShots.forEach {
                        it.resetPreview()
                        it.labelsFirst
                        it.labelsLast
                    }
                    val prevIndex = currentFileExt!!.shotsExt.indexOf(selectedShots.first())-1

                    if (prevIndex >= 0) {
                        val shotExt = currentFileExt!!.shotsExt[prevIndex]
                        shotExt.resetPreview()
                        shotExt.labelsFirst
                        shotExt.labelsLast
                    }

                    val nextIndex = currentFileExt!!.shotsExt.indexOf(selectedShots.last())+1
                    if (nextIndex < currentFileExt!!.shotsExt.size) {
                        val shotExt = currentFileExt!!.shotsExt[nextIndex]
                        shotExt.resetPreview()
                        shotExt.labelsFirst
                        shotExt.labelsLast
                    }

                    tblShots!!.refresh()
                }
            }

        }

    }
    
    @FXML
    fun doDeleteSelectedScenes(event: ActionEvent?) {

    }

    @FXML
    fun doDeleteSelectedEvents(event: ActionEvent?) {

        val setShotExtToUpdate: MutableSet<ShotExt> = mutableSetOf()
        tblEvents!!.selectionModel.selectedItems.forEach { eventExt ->
            val ffm = eventExt.event.firstFrameNumber - 1
            val lfm = eventExt.event.lastFrameNumber + 1
            setShotExtToUpdate.addAll(currentFileExt!!.shotsExt
                .filter { shotExt -> isPairIntersected(Pair(shotExt.shot.firstFrameNumber, shotExt.shot.lastFrameNumber), Pair(ffm, lfm)) }
            )
            EventController.delete(eventExt.event)
        }

        LoadListEventsExt(currentFileExt!!.eventsExt, currentFileExt!!, pb, lblPb).run()
        tblEvents!!.items = currentFileExt!!.eventsExt

        setShotExtToUpdate.map {
            it.resetPreview()
            it.labelsFirst
            it.labelsLast
        }


        tblShots!!.refresh()

    }

    fun isPairIntersected(firstPair: Pair<Int, Int>, secondPair: Pair<Int, Int>): Boolean {
        return  (min(firstPair.second, secondPair.second) - max(firstPair.first, secondPair.first)) >= 0
    }

    fun clearOnExit() {
        currentFileExt!!.shotsExt.clear()
        currentFileExt!!.scenesExt.clear()
        currentFileExt!!.eventsExt.clear()
        currentFileExt!!.framesExt.clear()
        System.gc()
    }

}
