package com.svoemesto.ivfx.fxcontrollers

// В JavaFX 11 эти два класса переехали из внутреннего пакета com.sun.javafx
// в публичный javafx.scene.control.skin. Внутренний пакет модулем не
// экспортируется, поэтому на 11-й версии старые импорты не разрешаются.
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.EventController
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.SceneController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.FaceTrack
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.EventExt
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FaceTrackExt
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
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForProject
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForShot
import com.svoemesto.ivfx.threads.loadlists.LoadListScenesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListShotsExt
import com.svoemesto.ivfx.threads.updatelists.UpdateListFramesExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import com.svoemesto.ivfx.utils.Trace
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
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.RadioButton
import javafx.scene.control.SelectionMode
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.Skin
import javafx.scene.control.TabPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToggleGroup
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.skin.TableViewSkin
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.stage.Modality
import javafx.stage.Screen
import javafx.stage.Stage
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.prefs.Preferences
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

    // SHOT PROPERTIES

    @FXML
    private var tblShotProperties: TableView<Property>? = null

    @FXML
    private var colShotPropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colShotPropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnShotPropertyMoveToFirst: Button? = null

    @FXML
    private var btnShotPropertyMoveUp: Button? = null

    @FXML
    private var btnShotPropertyMoveDown: Button? = null

    @FXML
    private var btnShotPropertyMoveToLast: Button? = null

    @FXML
    private var btnShotPropertyAdd: Button? = null

    @FXML
    private var btnShotPropertyDelete: Button? = null

    @FXML
    private var fldShotPropertyKey: TextField? = null

    @FXML
    private var fldShotPropertyValue: TextArea? = null

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
    private var tblTracks: TableView<FaceTrackExt>? = null

    @FXML
    private var btAllToExtras: Button? = null

    @FXML
    private var colTrackLabel: TableColumn<FaceTrackExt, String>? = null

    @FXML
    private var colTrackFaces: TableColumn<FaceTrackExt, String>? = null

    @FXML
    private var paneTrackFaces: Pane? = null

    @FXML
    private var pbTrackFaces: ProgressBar? = null

    @FXML
    private var tblTrackPages: TableView<MatrixPageFaces>? = null

    @FXML
    private var colTrackPagesNumber: TableColumn<MatrixPageFaces, String>? = null

    @FXML
    private var pbTracks: ProgressBar? = null

    @FXML
    private var lblTracks: Label? = null

    /**
     * Панель вкладок редактора плана. Нужна, чтобы знать, открыта ли вкладка
     * Tracks: пока она закрыта, панель лиц не размечена (ширина 0), раскладка
     * по страницам не считается, и показывать нечего.
     */
    @FXML
    private var tabpaneShotsEdit: TabPane? = null

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
    private var btnCreateEventBasedScene: Button? = null

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

    // SCENE PROPERTIES

    @FXML
    private var tblSceneProperties: TableView<Property>? = null

    @FXML
    private var colScenePropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colScenePropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnScenePropertyMoveToFirst: Button? = null

    @FXML
    private var btnScenePropertyMoveUp: Button? = null

    @FXML
    private var btnScenePropertyMoveDown: Button? = null

    @FXML
    private var btnScenePropertyMoveToLast: Button? = null

    @FXML
    private var btnScenePropertyAdd: Button? = null

    @FXML
    private var btnScenePropertyDelete: Button? = null

    @FXML
    private var fldScenePropertyKey: TextField? = null

    @FXML
    private var fldScenePropertyValue: TextArea? = null

    // EVENTS

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

    // EVENT PROPERTIES

    @FXML
    private var tblEventProperties: TableView<Property>? = null

    @FXML
    private var colEventPropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colEventPropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnEventPropertyMoveToFirst: Button? = null

    @FXML
    private var btnEventPropertyMoveUp: Button? = null

    @FXML
    private var btnEventPropertyMoveDown: Button? = null

    @FXML
    private var btnEventPropertyMoveToLast: Button? = null

    @FXML
    private var btnEventPropertyAdd: Button? = null

    @FXML
    private var btnEventPropertyDelete: Button? = null

    @FXML
    private var fldEventPropertyKey: TextField? = null

    @FXML
    private var fldEventPropertyValue: TextArea? = null

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
            mainStage?.scene!!.onKeyPressed =
                EventHandler { event ->
                    if (event.code == KeyCode.CONTROL) isPressedControl = true
                    if (event.code == KeyCode.SHIFT) isPressedShift = true
                    if (event.code == KeyCode.Z) isPressedPlayBackward.set(true)
                    if (event.code == KeyCode.X) isPressedPlayForward.set(true)
                }

            mainStage?.scene!!.onKeyReleased =
                EventHandler { event ->
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
    private var listTracksExt: ObservableList<FaceTrackExt> = FXCollections.observableArrayList()
    private var listMatrixPageTrackFaces: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
    private var currentMatrixPageTrackFaces: MatrixPageFaces? = null
    private var trackExtToShow: FaceTrackExt? = null

    // Своё выделение для вкладки Tracks. Состояние вкладки Faces используется
    // в её отрисовке, и если пользовать его здесь, выделение в Persons ломалось
    // бы: выбор трека сбрасывал бы выбор лиц.
    private var selectedTrackFaces: MutableList<MatrixFace> = mutableListOf()
    private var lastClickedTrackFace: MatrixFace? = null

    private var countColumnsInPageFrames = 0
    private var countRowsInPageFrames = 0
    private var countColumnsInPageFaces = 0
    private var countRowsInPageFaces = 0

    private var currentMatrixPageFrames: MatrixPageFrames? = null
    private var currentMatrixPageFaces: MatrixPageFaces? = null
    private var currentMatrixFrame: MatrixFrame? = null
    private var currentMatrixFace: MatrixFace? = null
    private var currentShotExt: ShotExt? = null
    private var currentSceneExt: SceneExt? = null
    private var currentEventExt: EventExt? = null
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

    /**
     * Кадр, чья картинка сейчас показана в полном превью. Нужна, чтобы
     * освобождать её при переходе к следующему кадру: картинка кэшируется в
     * FrameExt навсегда, и без этого листание страниц съедало память.
     */
    private var frameExtWithLoadedPreview: FrameExt? = null

    /**
     * Номер последнего запроса картинки. Пока она считается, пользователь может
     * листать дальше; устаревший результат отбрасывается по этому номеру.
     */
    @Volatile
    private var lastFullFrameRequest: Int = 0

    /**
     * Один поток на загрузку картинки вместо своего на каждый вызов.
     */
    private val fullFrameExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "LoadFullFrame").apply { isDaemon = true }
        }

    private var selectedMatrixFaces: MutableSet<MatrixFace> = mutableSetOf()
    private var lastClickedMatrixFace: MatrixFace? = null
    private var currentPersonExtHovered: PersonExt? = null
    private var isNeedToAddDraggedFacesToPerson: Boolean = false

    /**
     * Персона, на строку которой сейчас наведён курсор с перетаскиванием.
     *
     * Раньше перетаскивание срабатывало по наведению: строка подходила и
     * персоны менялась сама, стоило просто навести на строку и отпустить
     * кнопку где угодно. Теперь наведение только запоминает цель, а
     * назначение происходит по событию отпускания над строкой.
     */
    private var dropTargetPersonExt: PersonExt? = null

    /**
     */

    private var currentMatrixPageFacesPageNumber: Int = 1

    var threadOnSelectScene: Thread? = null
    var threadOnSelectEvent: Thread? = null

    var projectPersonExtUndefinded: PersonExt? = null
    var projectPersonExtNonperson: PersonExt? = null
    var projectPersonExtExtras: PersonExt? = null

    private var currentShotProperty: Property? = null
    private var listShotProperties: ObservableList<Property> = FXCollections.observableArrayList()
    private var currentSceneProperty: Property? = null
    private var listSceneProperties: ObservableList<Property> = FXCollections.observableArrayList()
    private var currentEventProperty: Property? = null
    private var listEventProperties: ObservableList<Property> = FXCollections.observableArrayList()

    /**
     * Размер окна, каким его закрыли в прошлый раз.
     *
     * Хранится в системных настройках Java по пути узла приложения, поэтому
     * переживает перезапуск. Если окно больше экрана — берём размер экрана,
     * иначе открытое окно было бы не видно.
     */
    private val windowPrefs: Preferences = Preferences.userNodeForPackage(ShotsEditFXController::class.java)

    private fun readWindowSize(stage: Stage) {
        val d = windowPrefs
        val w = d.getInt("shotsEdit.windowWidth", 0)
        val h = d.getInt("shotsEdit.windowHeight", 0)
        if (w > 0 && h > 0) {
            val screen = Screen.getPrimary().bounds
            stage.width = minOf(w.toDouble(), screen.width.toDouble() - 40.0)
            stage.height = minOf(h.toDouble(), screen.height.toDouble() - 40.0)
        }
    }

    private fun writeWindowSize(stage: Stage) {
        try {
            val d = windowPrefs
            d.putInt("shotsEdit.windowWidth", stage.width.toInt())
            d.putInt("shotsEdit.windowHeight", stage.height.toInt())
            d.flush()
        } catch (e: Exception) {
            // размер окна не критичен: при ошибке просто откроем как есть
            println("Не удалось сохранить размер окна: ${e.message}")
        }
    }

    fun editShots(
        fileExt: FileExt,
        hostServices: HostServices? = null,
    ) {
        currentFileExt = fileExt
        mainStage = Stage()
        try {
            val loader = FXMLLoader(ShotsEditFXController::class.java.getResource("shots-edit-view.fxml"))
            loader.setController(this)
            val root = loader.load<Parent>()
            mainStage?.scene = Scene(root)
            ShotsEditFXController.hostServices = hostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            // Размер восстанавливаем ДО первого показа: сцена успевает
            // разложиться под него, и окно открывается сразу нужного размера.
            readWindowSize(mainStage!!)
            onStart()
            registerKeyboardNavigation()
            // Обработчик закрытия уже установлен в initialize(); здесь стоял
            // второй, который его перезаписывал, и очистка памяти не шла.
            mainStage?.showAndWait()
            // Страховка: если закрытие шло мимо обработчика, размер всё равно
            // запомним — он нужен к следующему открытию.
            writeWindowSize(mainStage!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы ShotsEditFXController.")
        mainStage = null
    }

    /**
     * Переход между страницами превью кадров.
     *
     * Общая часть для колеса мыши над превью и для клавиш PageUp и PageDown,
     * чтобы листание страниц работало одинаково обоими способами. С Ctrl
     * переход идёт не по страницам, а к началу либо к концу текущего плана —
     * так было и на колесе.
     *
     * @param delta -1 переход к началу, 1 переход к концу
     */
    private fun goToAdjacentPageOfFrames(delta: Int) {
        wasClickFrameLabel = false
        wasClickTablePagesFrames = false
        wasClickTableShots = false
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
                val frameToGo =
                    if (delta <
                        0
                    ) {
                        getPrevMatrixFrame(currentMatrixPageFrames!!.matrixFrames.first())
                    } else {
                        getNextMatrixFrame(currentMatrixPageFrames!!.matrixFrames.last())
                    }
                goToFrame(frameToGo)
            }
        }
    }

    /**
     * Листание страниц превью клавишами PageUp и PageDown.
     *
     * Обработчик вешается фильтром на всю сцену, а не на панель превью: чтобы
     * клавиши работали, мышь не обязана находиться над превью. В полях ввода эти
     * же клавиши двигают курсор по тексту, поэтому при фокусе на поле переход не
     * выполняется.
     */
    private fun registerKeyboardNavigation() {
        mainStage?.scene?.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code != KeyCode.PAGE_DOWN && event.code != KeyCode.PAGE_UP) {
                return@addEventFilter
            }
            if (mainStage?.scene?.focusOwner is TextInputControl) {
                return@addEventFilter
            }
            event.consume()
            goToAdjacentPageOfFrames(if (event.code == KeyCode.PAGE_DOWN) 1 else -1)
        }
    }

    @FXML
    fun initialize() {
        mainStage?.setOnCloseRequest {
            Trace.action("закрытие окна редактирования поймано")
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

        btnShotPropertyMoveToFirst?.isDisable = currentShotProperty == null
        btnShotPropertyMoveUp?.isDisable = currentShotProperty == null
        btnShotPropertyMoveToLast?.isDisable = currentShotProperty == null
        btnShotPropertyMoveDown?.isDisable = currentShotProperty == null
        btnShotPropertyDelete?.isDisable = currentShotProperty == null
        fldShotPropertyKey?.isDisable = currentShotProperty == null
        fldShotPropertyValue?.isDisable = currentShotProperty == null
        fldShotPropertyKey?.text = ""
        fldShotPropertyValue?.text = ""

        btnScenePropertyMoveToFirst?.isDisable = currentSceneProperty == null
        btnScenePropertyMoveUp?.isDisable = currentSceneProperty == null
        btnScenePropertyMoveToLast?.isDisable = currentSceneProperty == null
        btnScenePropertyMoveDown?.isDisable = currentSceneProperty == null
        btnScenePropertyDelete?.isDisable = currentSceneProperty == null
        fldScenePropertyKey?.isDisable = currentSceneProperty == null
        fldScenePropertyValue?.isDisable = currentSceneProperty == null
        fldScenePropertyKey?.text = ""
        fldScenePropertyValue?.text = ""

        btnEventPropertyMoveToFirst?.isDisable = currentEventProperty == null
        btnEventPropertyMoveUp?.isDisable = currentEventProperty == null
        btnEventPropertyMoveToLast?.isDisable = currentEventProperty == null
        btnEventPropertyMoveDown?.isDisable = currentEventProperty == null
        btnEventPropertyDelete?.isDisable = currentEventProperty == null
        fldEventPropertyKey?.isDisable = currentEventProperty == null
        fldEventPropertyValue?.isDisable = currentEventProperty == null
        fldEventPropertyKey?.text = ""
        fldEventPropertyValue?.text = ""

        initTracksTab()
        colShotPropertyKey?.cellValueFactory = PropertyValueFactory("key")
        colShotPropertyValue?.cellValueFactory = PropertyValueFactory("value")

        colScenePropertyKey?.cellValueFactory = PropertyValueFactory("key")
        colScenePropertyValue?.cellValueFactory = PropertyValueFactory("value")

        colEventPropertyKey?.cellValueFactory = PropertyValueFactory("key")
        colEventPropertyValue?.cellValueFactory = PropertyValueFactory("value")

        mainStage?.title = "Редактор планов. Файл: ${currentFileExt!!.file.name}"
        isWorking = true

        projectPersonExtUndefinded = PersonController.getUndefindedExt(currentFileExt!!.projectExt)
        projectPersonExtNonperson = PersonController.getNonpersonExt(currentFileExt!!.projectExt)
        projectPersonExtExtras = PersonController.getExtrasExt(currentFileExt!!.projectExt)

        /**
         * LoadListPersonsExtForFile
         */
        LoadListPersonsExtForFile(
            listPersonsExtForFile,
            currentFileExt!!,
            pbPersonsForFile,
            null,
            isDoneLoadListPersonsExtForFile,
            false,
        ).start()
        isDoneLoadListPersonsExtForFile.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListPersonsExtForFile.set(false)
                fillTrackFacesCount()
                restorePersonSelectionAfterPersonsReload()
            }
        }

        /**
         * LoadListPersonsExtForProject — тот же список персон, но из проекта:
         * его наполняет переключатель «All / File» (см. doSelectPersonsRb).
         */
        isDoneLoadListPersonsExtForProject.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListPersonsExtForProject.set(false)
                fillTrackFacesCount()
                restorePersonSelectionAfterPersonsReload()
            }
        }

        /**
         * LoadListFramesExt
         */
        LoadListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb, isDoneLoadListFramesExt).start()
        isDoneLoadListFramesExt.addListener { _, _, newValue ->
            if (newValue == true) {
                isDoneLoadListFramesExt.set(false)
                listMatrixPageFrames =
                    MatrixPageFrames.createPages(
                        currentFileExt!!.framesExt,
                        paneFrames!!.width,
                        paneFrames!!.height,
                        Main.PREVIEW_FRAME_W,
                        Main.PREVIEW_FRAME_H,
                    )
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

                listMatrixPageFaces =
                    MatrixPageFaces.createPages(
                        listFacesExt,
                        paneFaces!!.width,
                        paneFaces!!.height,
                        Main.PREVIEW_FACE_W,
                        Main.PREVIEW_FACE_H,
                    )
                tblPagesFaces!!.items = listMatrixPageFaces

                // Персон, у которого в этой серии нет ни одного лица, раньше
                // был невозможен, и список страниц всегда был непуст. Теперь
                // персоны общие для проекта, поэтому при выборе такого лица
                // список пуст, и first() ронял поток загрузки — вкладка
                // оставалась без лиц, а вместе с ними и без контекстного меню.
                if (listMatrixPageFaces.isEmpty()) {
                    currentMatrixFace = null
                    currentMatrixPageFaces = null
                    tblPagesFaces!!.selectionModel.clearSelection()
                    return@addListener
                }
                currentMatrixFace = listMatrixPageFaces.first().matrixFaces.first()
                if (currentMatrixPageFacesPageNumber > listMatrixPageFaces.size) currentMatrixPageFacesPageNumber = 1
                currentMatrixPageFaces = listMatrixPageFaces[currentMatrixPageFacesPageNumber - 1]

                if (currentMatrixPageFaces != null) {
                    Platform.runLater {
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
        tblPagesFrames!!
            .selectionModel
            .selectedItemProperty()
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
        tblPagesFrames!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblPagesFrames = kids[1] as VirtualFlow<*>
            },
        )

        // Click
        tblPagesFrames!!.onMouseClicked =
            EventHandler { mouseEvent ->
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
        tblPagesFaces!!
            .selectionModel
            .selectedItemProperty()
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
        tblPagesFaces!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblPagesFaces = kids[1] as VirtualFlow<*>
            },
        )

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
        tblShots!!
            .selectionModel
            .selectedItemProperty()
            .addListener { _, _, newValue: ShotExt? ->
                if (newValue != null) {
                    Thread {
                        Platform.runLater { tblPersonsAllForShot?.placeholder = ProgressIndicator(-1.0) }
                        currentShotExt = newValue

                        listPersonsExtForShot =
                            FXCollections.observableList(
                                currentShotExt!!.personsExt.filter { it.person.personType != PersonType.NONPERSON },
                            )
                        tblPersonsAllForShot!!.items = listPersonsExtForShot
                        Platform.runLater { tblPersonsAllForShot?.placeholder = Label("Shot not selected or don't have any persons.") }

                        listShotProperties =
                            FXCollections.observableArrayList(
                                PropertyController.getListProperties(
                                    currentShotExt!!.shot::class.java.simpleName,
                                    currentShotExt!!.shot.id,
                                ),
                            )
                        tblShotProperties?.items = listShotProperties
                        Platform.runLater { tblShotProperties?.placeholder = Label("Shot not selected or don't have any properties.") }

                        btnShotPropertyMoveToFirst?.isDisable = currentShotProperty == null
                        btnShotPropertyMoveUp?.isDisable = currentShotProperty == null
                        btnShotPropertyMoveToLast?.isDisable = currentShotProperty == null
                        btnShotPropertyMoveDown?.isDisable = currentShotProperty == null
                        btnShotPropertyDelete?.isDisable = currentShotProperty == null
                        fldShotPropertyKey?.isDisable = currentShotProperty == null
                        fldShotPropertyValue?.isDisable = currentShotProperty == null
                        fldShotPropertyKey?.text = ""
                        fldShotPropertyValue?.text = ""

                        if (wasClickTableShots) {
                            wasClickTableShots = false
                            goToFrame(getMatrixFrameByFrameExt(newValue.firstFrameExt))
                            if (currentShotExt != null && currentShotExt!!.sceneExt != null) {
                                if (!currentSelectedScenesExt.map { it.scene.id }.contains(currentShotExt!!.sceneExt!!.scene.id)) {
                                    tblScenes!!.selectionModel.clearSelection()
                                    val sceneToGo =
                                        currentFileExt!!.scenesExt.firstOrNull {
                                            it.scene.id ==
                                                currentShotExt!!.sceneExt!!.scene.id
                                        }
                                    if (sceneToGo != null) {
                                        tblScenes!!.selectionModel.select(sceneToGo)
                                        tblScenesSmartScroll(sceneToGo)
                                    }
                                } else {
                                    if (listShotsExtForScenes.map { it.shot.id }.contains(currentShotExt!!.shot.id)) {
                                        tblShotsForScenes!!.selectionModel.clearSelection()
                                        val shotForSceneToGo = listShotsExtForScenes.firstOrNull { it.shot.id == currentShotExt!!.shot.id }
                                        if (shotForSceneToGo != null) {
                                            tblShotsForScenes!!.selectionModel.select(shotForSceneToGo)
                                            tblShotsForScenesSmartScroll(shotForSceneToGo)
                                        }
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
        tblShots!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblShots = kids[1] as VirtualFlow<*>
            },
        )

        // Click
        tblShots!!.onMouseClicked =
            EventHandler { mouseEvent ->
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
        tblScenes!!
            .selectionModel
            .selectedItemProperty()
            .addListener { _, _, newValue: SceneExt? ->

                if (newValue != null) {
                    currentSceneExt = newValue
                    listSceneProperties =
                        FXCollections.observableArrayList(
                            PropertyController.getListProperties(
                                currentSceneExt!!.scene::class.java.simpleName,
                                currentSceneExt!!.scene.id,
                            ),
                        )
                    tblSceneProperties?.items = listSceneProperties
                    Platform.runLater { tblSceneProperties?.placeholder = Label("Scene not selected or don't have any properties.") }
                }

                btnScenePropertyMoveToFirst?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveUp?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveToLast?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveDown?.isDisable = currentSceneProperty == null
                btnScenePropertyDelete?.isDisable = currentSceneProperty == null
                fldScenePropertyKey?.isDisable = currentSceneProperty == null
                fldScenePropertyValue?.isDisable = currentSceneProperty == null
                fldScenePropertyKey?.text = ""
                fldScenePropertyValue?.text = ""

                if (threadOnSelectScene != null && threadOnSelectScene!!.isAlive) {
                    threadOnSelectScene!!.interrupt()
                    while (threadOnSelectScene!!.isAlive) {
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
                            currentSelectedScenesExt.forEach { currentSceneExt ->
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
                            if (listShotsExtForScenes.map { it.shot.id }.contains(currentShotExt!!.shot.id)) {
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
        tblScenes!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblScenes = kids[1] as VirtualFlow<*>
            },
        )

        /**
         * tblShotsForScenes events
         */

        // placeholder
        tblShotsForScenes?.placeholder = Label("Scene not selected or don't have any shots.")

        // PropertyValueFactory
        colShotForSceneFrom?.cellValueFactory = PropertyValueFactory("labelFirst2")
        colShotForSceneTo?.cellValueFactory = PropertyValueFactory("labelLast2")

        // selectedItemProperty
        tblShotsForScenes!!
            .selectionModel
            .selectedItemProperty()
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
        tblShotsForScenes!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblShotsForScenes = kids[1] as VirtualFlow<*>
            },
        )

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
        tblEvents!!
            .selectionModel
            .selectedItemProperty()
            .addListener { _, _, newValue: EventExt? ->

                if (newValue != null) {
                    currentEventExt = newValue
                    listEventProperties =
                        FXCollections.observableArrayList(
                            PropertyController.getListProperties(
                                currentEventExt!!.event::class.java.simpleName,
                                currentEventExt!!.event.id,
                            ),
                        )
                    tblEventProperties?.items = listEventProperties
                    Platform.runLater { tblEventProperties?.placeholder = Label("Event not selected or don't have any properties.") }
                }

                btnEventPropertyMoveToFirst?.isDisable = currentEventProperty == null
                btnEventPropertyMoveUp?.isDisable = currentEventProperty == null
                btnEventPropertyMoveToLast?.isDisable = currentEventProperty == null
                btnEventPropertyMoveDown?.isDisable = currentEventProperty == null
                btnEventPropertyDelete?.isDisable = currentEventProperty == null
                fldEventPropertyKey?.isDisable = currentEventProperty == null
                fldEventPropertyValue?.isDisable = currentEventProperty == null
                fldEventPropertyKey?.text = ""
                fldEventPropertyValue?.text = ""

                if (threadOnSelectEvent != null && threadOnSelectEvent!!.isAlive) {
                    threadOnSelectEvent!!.interrupt()
                    while (threadOnSelectEvent!!.isAlive) {
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
                            currentSelectedEventsExt.forEach { currentEventExt ->
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
                            if (listShotsExtForEvents.map { it.shot.id }.contains(currentShotExt!!.shot.id)) {
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
        tblEvents!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblEvents = kids[1] as VirtualFlow<*>
            },
        )

        /**
         * tblShotsForEvents events
         */

        // placeholder
        tblShotsForEvents?.placeholder = Label("Event not selected or don't have any shots.")

        // PropertyValueFactory
        colShotForEventFrom?.cellValueFactory = PropertyValueFactory("labelFirst3")
        colShotForEventTo?.cellValueFactory = PropertyValueFactory("labelLast3")

        // selectedItemProperty
        tblShotsForEvents!!
            .selectionModel
            .selectedItemProperty()
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
        tblShotsForEvents!!.skinProperty().addListener(
            ChangeListener label@{ _: ObservableValue<out Skin<*>?>?, _: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) return@label
                val tvs = t1 as TableViewSkin<*>
                val kids = tvs.children
                if (kids == null || kids.isEmpty()) return@label
                flowTblShotsForEvents = kids[1] as VirtualFlow<*>
            },
        )

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
        tblPersonsAllForFile!!
            .selectionModel
            .selectedItemProperty()
            .addListener { v: ObservableValue<out PersonExt?>?, oldValue: PersonExt?, newValue: PersonExt? ->
                if (newValue != null) {
                    currentPersonExt = newValue
                    doSelectFacesCb(null)
                }
            }

        // Click
        tblPersonsAllForFile!!.onMouseClicked =
            EventHandler { mouseEvent ->
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 2) {
                        Trace.action("двойной клик по лицу, кадр " + currentMatrixFrame?.frameNumber)
                        if (currentPersonExt != null) {
                            PersonEditFXController().editPerson(currentFileExt!!.projectExt, currentPersonExt!!, hostServices)
                        }
                    }
                }
            }

        // onMouseEntered / onMouseExited
        tblPersonsAllForFile!!.onMouseEntered = EventHandler { wasClickTablePersonsAllForFile = true }
        tblPersonsAllForFile!!.onMouseExited = EventHandler { wasClickTablePersonsAllForFile = false }

        // Перетаскивание лиц на строку персоны: назначение по отпусканию над
        // строкой, строка ищется по координатам, а не по наведению.
        setupFaceDropOnRows(
            tblPersonsAllForFile!!,
            { it },
            { selectedMatrixFaces },
            {
                reorganizeMatrixFaces()
                reloadPersonsForShot()
            },
        )

        // setRowFactory
        tblPersonsAllForFile!!.setRowFactory {
            val row: TableRow<PersonExt> = TableRow()
            row.hoverProperty().addListener { _ ->
                currentPersonExtHovered = if (row.isHover) row.item else null
            }
            row
        }

        /**
         * tblPersonsAllForShot events
         */

        // Перетаскивание лиц на строку персоны плана.
        setupFaceDropOnRows(
            tblPersonsAllForShot!!,
            { it },
            { selectedMatrixFaces },
            {
                reorganizeMatrixFaces()
                reloadPersonsForShot()
            },
        )

        // placeholder
        tblPersonsAllForShot?.placeholder = Label("Shot not selected or don't have any persons.")

        // PropertyValueFactory
        colTblPersonsAllForShotName?.cellValueFactory = PropertyValueFactory("labelSmall")

        // items
        tblPersonsAllForShot!!.items = listPersonsExtForShot

        // Click
        tblPersonsAllForShot!!.onMouseClicked =
            EventHandler { mouseEvent ->
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 2) {
                        Trace.action("двойной клик по персоне, кадр " + currentMatrixFrame?.frameNumber)
                        if (tblPersonsAllForShot!!.selectionModel.selectedItem != null) {
                            PersonEditFXController().editPerson(
                                currentFileExt!!.projectExt,
                                tblPersonsAllForShot!!.selectionModel.selectedItem,
                                hostServices,
                            )
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
            goToAdjacentPageOfFrames(if (e.deltaY > 0) -1 else 1)
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
                        goToFrame(currentMatrixFrame?.frameNumber!! + 1)
                    } else {
                        goToFrame(currentMatrixFrame?.frameNumber!! - 1)
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
                    val faceToGo =
                        if (delta < 0) {
                            if (currentMatrixPageFaces!!.matrixFaces.isNotEmpty()) {
                                getPrevMatrixFace(currentMatrixPageFaces!!.matrixFaces.first())
                            } else {
                                null
                            }
                        } else {
                            if (currentMatrixPageFaces!!.matrixFaces.isNotEmpty()) {
                                getNextMatrixFace(currentMatrixPageFaces!!.matrixFaces.last())
                            } else {
                                null
                            }
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
                listMatrixPageFrames =
                    MatrixPageFrames.createPages(
                        currentFileExt!!.framesExt,
                        paneFrames!!.width,
                        paneFrames!!.height,
                        Main.PREVIEW_FRAME_W,
                        Main.PREVIEW_FRAME_H,
                    )
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

        // изменение поля "fldShotPropertyKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств плана
        fldShotPropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentShotProperty()
            }
        }

        // изменение поля "fldShotPropertyValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств плана
        fldShotPropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentShotProperty()
            }
        }

        // изменение поля "fldScenePropertyKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств сцены
        fldScenePropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentSceneProperty()
            }
        }

        // изменение поля "fldScenePropertyValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств сцены
        fldScenePropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentSceneProperty()
            }
        }

        // изменение поля "fldEventPropertyKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств события
        fldEventPropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentEventProperty()
            }
        }

        // изменение поля "fldEventPropertyValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств события
        fldEventPropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentEventProperty()
            }
        }

        tblShotProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentShotProperty != newValue) saveCurrentShotProperty()

            currentShotProperty = newValue

            btnShotPropertyDelete?.isDisable = currentShotProperty == null
            btnShotPropertyMoveToFirst?.isDisable = currentShotProperty == null || currentShotProperty == listShotProperties.first()
            btnShotPropertyMoveUp?.isDisable = currentShotProperty == null || currentShotProperty == listShotProperties.first()
            btnShotPropertyMoveToLast?.isDisable = currentShotProperty == null || currentShotProperty == listShotProperties.last()
            btnShotPropertyMoveDown?.isDisable = currentShotProperty == null || currentShotProperty == listShotProperties.last()

            fldShotPropertyKey?.isDisable = currentShotProperty == null
            fldShotPropertyValue?.isDisable = currentShotProperty == null

            fldShotPropertyKey?.text = currentShotProperty?.key
            fldShotPropertyValue?.text = currentShotProperty?.value
        }

        tblSceneProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentSceneProperty != newValue) saveCurrentSceneProperty()

            currentSceneProperty = newValue

            btnScenePropertyDelete?.isDisable = currentSceneProperty == null
            btnScenePropertyMoveToFirst?.isDisable = currentSceneProperty == null || currentSceneProperty == listSceneProperties.first()
            btnScenePropertyMoveUp?.isDisable = currentSceneProperty == null || currentSceneProperty == listSceneProperties.first()
            btnScenePropertyMoveToLast?.isDisable = currentSceneProperty == null || currentSceneProperty == listSceneProperties.last()
            btnScenePropertyMoveDown?.isDisable = currentSceneProperty == null || currentSceneProperty == listSceneProperties.last()

            fldScenePropertyKey?.isDisable = currentSceneProperty == null
            fldScenePropertyValue?.isDisable = currentSceneProperty == null

            fldScenePropertyKey?.text = currentSceneProperty?.key
            fldScenePropertyValue?.text = currentSceneProperty?.value
        }

        tblEventProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentEventProperty != newValue) saveCurrentEventProperty()

            currentEventProperty = newValue

            btnEventPropertyDelete?.isDisable = currentEventProperty == null
            btnEventPropertyMoveToFirst?.isDisable = currentEventProperty == null || currentEventProperty == listEventProperties.first()
            btnEventPropertyMoveUp?.isDisable = currentEventProperty == null || currentEventProperty == listEventProperties.first()
            btnEventPropertyMoveToLast?.isDisable = currentEventProperty == null || currentEventProperty == listEventProperties.last()
            btnEventPropertyMoveDown?.isDisable = currentEventProperty == null || currentEventProperty == listEventProperties.last()

            fldEventPropertyKey?.isDisable = currentEventProperty == null
            fldEventPropertyValue?.isDisable = currentEventProperty == null

            fldEventPropertyKey?.text = currentEventProperty?.key
            fldEventPropertyValue?.text = currentEventProperty?.value
        }
    }

    fun saveCurrentShotProperty() {
        if (currentShotProperty != null) {
            var needToSave = false

            var tmp: String = fldShotPropertyKey?.text ?: ""
            if (tmp != currentShotProperty?.key) {
                currentShotProperty?.key = tmp
                needToSave = true
            }

            tmp = fldShotPropertyValue?.text ?: ""
            if (tmp != currentShotProperty?.value) {
                currentShotProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentShotProperty!!)
                tblShotProperties?.refresh()
            }
        }
    }

    fun saveCurrentSceneProperty() {
        if (currentSceneProperty != null) {
            var needToSave = false

            var tmp: String = fldScenePropertyKey?.text ?: ""
            if (tmp != currentSceneProperty?.key) {
                currentSceneProperty?.key = tmp
                needToSave = true
            }

            tmp = fldScenePropertyValue?.text ?: ""
            if (tmp != currentSceneProperty?.value) {
                currentSceneProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentSceneProperty!!)
                tblSceneProperties?.refresh()
            }
        }
    }

    fun saveCurrentEventProperty() {
        if (currentEventProperty != null) {
            var needToSave = false

            var tmp: String = fldEventPropertyKey?.text ?: ""
            if (tmp != currentEventProperty?.key) {
                currentEventProperty?.key = tmp
                needToSave = true
            }

            tmp = fldEventPropertyValue?.text ?: ""
            if (tmp != currentEventProperty?.value) {
                currentEventProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentEventProperty!!)
                tblEventProperties?.refresh()
            }
        }
    }

    @FXML
    fun doOK(event: ActionEvent?) {
        Trace.action("doOK")
        isWorking = false
        // Stage.close() НЕ порождает событие закрытия: обработчик
        // onCloseRequest срабатывает только по крестику или системному
        // закрытию окна. При закрытии кнопкой «ОК» он не вызывается, и
        // память серии оставалась в куче — 6 ГБ на одну открытую серию,
        // 17 ГБ на три. Поэтому здесь освобождение вызывается явно.
        // Пути не пересекаются: «ОК» ведёт сюда, крестик — в обработчик.
        mainStage?.close()
        clearOnExit()
    }

    fun getNextMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame =
        if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.last() &&
            matrixFrame.matrixPageFrames == listMatrixPageFrames.last()
        ) {
            matrixFrame
        } else {
            listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames) + 1].matrixFrames.first()
        }

    fun getPrevMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame =
        if (matrixFrame == matrixFrame.matrixPageFrames!!.matrixFrames.first() &&
            matrixFrame.matrixPageFrames == listMatrixPageFrames.first()
        ) {
            matrixFrame
        } else {
            listMatrixPageFrames[listMatrixPageFrames.indexOf(matrixFrame.matrixPageFrames) - 1].matrixFrames.last()
        }

    fun getNextMatrixFace(matrixFace: MatrixFace): MatrixFace =
        if (matrixFace == matrixFace.matrixPageFaces.matrixFaces.last() &&
            matrixFace.matrixPageFaces == listMatrixPageFaces.last()
        ) {
            matrixFace
        } else {
            listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces) + 1].matrixFaces.first()
        }

    fun getPrevMatrixFace(matrixFace: MatrixFace): MatrixFace =
        if (matrixFace == matrixFace.matrixPageFaces.matrixFaces.first() &&
            matrixFace.matrixPageFaces == listMatrixPageFaces.first()
        ) {
            matrixFace
        } else {
            listMatrixPageFaces[listMatrixPageFaces.indexOf(matrixFace.matrixPageFaces) - 1].matrixFaces.last()
        }

    fun loadPictureToFullFrameLabelForFrame(matrixFrame: MatrixFrame?) {
        if (matrixFrame == null) {
            return
        }
        val frameExt = matrixFrame.frameExt ?: return

        // Картинка кадра кэшируется в FrameExt навсегда, а раньше сброса у
        // FrameExt вообще не было: при листании страниц превью память росла до
        // десятков гигабайт. На экране всё равно остаётся только один кадр,
        // значит предыдущий можно освободить сразу.
        val previous = frameExtWithLoadedPreview
        if (previous != null && previous !== frameExt) {
            previous.resetPreviewMedium()
            previous.resetPreviewFull()
        }
        frameExtWithLoadedPreview = frameExt

        // Раньше на каждый вызов создавался свой поток, и они никогда не
        // присоединялись: при быстром листании их накапливались десятки, и
        // каждый заново читал и переносил картинку. Теперь работает один поток,
        // а номер запроса отсекает устаревшие результаты — иначе на экране
        // мог бы появиться кадр, который уже пролистан.
        val request = ++lastFullFrameRequest
        fullFrameExecutor.execute {
            try {
                val bufferedImage = FaceController.getOverlayedFrame(frameExt, null)
                if (lastFullFrameRequest != request) {
                    return@execute
                }
                val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                Platform.runLater {
                    if (lastFullFrameRequest == request) {
                        lblFrameFull?.graphic = imageView
                    }
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
    }

    fun loadPictureToFullFrameLabelForFace(
        frameExt: FrameExt,
        faceExt: FaceExt?,
    ) {
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

    /**
     * Разделяет или объединяет планы под кадром.
     *
     * Возвращает true, если планы действительно перестроены. Вызывающий
     * код меняет отметки кадра ДО вызова и обязан узнать, что получилось:
     * раньше метод возвращался молча, и неудачный клик оставлял в базе
     * наполовину применённую правку — флаг кадра сохранён, а план прежний.
     * Дальше такая правка расходилась с планом и мешала анализу кадров,
     * который ориентируется на is_final_find.
     */
    fun splitOrUnionShots(matrixFrame: MatrixFrame): Boolean {
        val shotExt = getShotExtByFrameNumber(matrixFrame.frameNumber!!)
        if (shotExt != null) {
            val frameNumber = matrixFrame.frameNumber
            // Отменённая вручную граница не должна выглядеть как начало
            // плана: метка is_final_find у неё остаётся, но плана с таким
            // началом в базе нет. Без проверки на отмену такие кадры
            // выводились каждый на новой строке — матрица рисовала то, чего
            // в разметке нет.
            if (matrixFrame.frameExt!!.frame.isFinalFind &&
                shotExt.firstFrameExt.frame.frameNumber != matrixFrame.frameNumber
            ) {
                // split
                val lastFrameNumber = shotExt.lastFrameExt.frame.frameNumber
                val lastFrameExt = currentFileExt!!.framesExt.first { it.frame.frameNumber == lastFrameNumber }
                shotExt.shot.lastFrameNumber = matrixFrame.frameNumber!! - 1
                shotExt.lastFrameExt = currentFileExt!!.framesExt.first { it.frame.frameNumber == matrixFrame.frameNumber!! - 1 }
                shotExt.resetPreview()
                ShotController.save(shotExt.shot)
                val shot = ShotController.getOrCreate(currentFileExt!!.file, matrixFrame.frameNumber!!, lastFrameNumber)
                val addedShotExt = ShotExt(shot, currentFileExt!!, matrixFrame.frameExt!!, lastFrameExt)
                addedShotExt.buttonGetType.setOnAction { onActionButtonGetShotType(addedShotExt) }
                currentFileExt!!.shotsExt.add(addedShotExt)
                currentFileExt!!.shotsExt.sort()
            } else if (!matrixFrame.frameExt!!.frame.isFinalFind &&
                shotExt.firstFrameExt.frame.frameNumber == matrixFrame.frameNumber
            ) {
                // union
                val unionShot = getShotExtByFrameNumber(matrixFrame.frameNumber!! - 1)
                if (unionShot != null) {
                    unionShot.shot.lastFrameNumber = shotExt.shot.lastFrameNumber
                    unionShot.lastFrameExt = shotExt.lastFrameExt
                    unionShot.resetPreview()
                    // Всё, что сделано по поглощаемому плану, переезжает на
                    // выживший: треки, свойства, свойства cdf. Раньше здесь был
                    // просто delete(shotExt.shot), и склейка падала по внешнему
                    // ключу tbl_faces_tracks → tbl_shots, стоило поглощаемому
                    // плану иметь хоть один трек. Перенос идёт до сохранения
                    // выжившего, чтобы его id был свежим.
                    ShotController.moveAll(shotExt.shot, unionShot.shot)
                    ShotController.save(unionShot.shot)
                    currentFileExt!!.shotsExt.remove(shotExt)
                    currentFileExt!!.shotsExt.sort()
                    ShotController.deleteAfterMove(shotExt.shot)
                } else {
                    println("Проблема: не найден план для объединения!")
                }
            } else {
                // Ни разделение, ни объединение не применимы: клик не по
                // границе плана или не по его первому кадру. Молчать здесь
                // нельзя — вызывающий уже переключил и сохранил отметку.
                println(
                    "[ПЛАН] кадр $frameNumber: ни разделение, ни склейка не применимы. " +
                        "Разделение — двойной клик по кадру внутри плана, склейка — по первому кадру плана.",
                )
                return false
            }

            listMatrixPageFrames =
                MatrixPageFrames.createPages(
                    currentFileExt!!.framesExt,
                    paneFrames!!.width,
                    paneFrames!!.height,
                    Main.PREVIEW_FRAME_W,
                    Main.PREVIEW_FRAME_H,
                )
            tblPagesFrames!!.items = listMatrixPageFrames
            currentMatrixFrame = getMatrixFrameByFrameNumber(frameNumber!!)
            currentMatrixPageFrames = getMatrixPageFramesByFrame(frameNumber)
            currentShotExt = getShotExtByFrameNumber(frameNumber)
            showMatrixPageFrames(currentMatrixPageFrames!!)
            tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
            goToFrame(currentMatrixFrame)
            return true
        } else {
            println("[ПЛАН] кадр ${matrixFrame.frameNumber}: план не найден, правка не применена")
            return false
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
                Platform.runLater { tblPagesFrames?.scrollTo(matrixPageFrames) }
            }
        }
    }

    fun tblPagesFacesSmartScroll(matrixPageFaces: MatrixPageFaces?) {
        if (flowTblPagesFaces != null && flowTblPagesFaces!!.cellCount > 0) {
            val first: Int = flowTblPagesFaces!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPagesFaces!!.lastVisibleCell.getIndex()
            val selected = tblPagesFaces!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblPagesFaces?.scrollTo(matrixPageFaces) }
            }
        }
    }

    fun tblShotsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShots != null && flowTblShots!!.cellCount > 0) {
            val first: Int = flowTblShots!!.firstVisibleCell.getIndex()
            val last: Int = flowTblShots!!.lastVisibleCell.getIndex()
            val selected = tblShots!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblShots?.scrollTo(shotExt) }
            }
        }
    }

    fun tblScenesSmartScroll(sceneExt: SceneExt?) {
        if (flowTblScenes != null && flowTblScenes!!.cellCount > 0) {
            val first: Int = flowTblScenes!!.firstVisibleCell.index
            val last: Int = flowTblScenes!!.lastVisibleCell.index
            val selected = tblScenes!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblScenes?.scrollTo(sceneExt) }
            }
        }
    }

    fun tblShotsForScenesSmartScroll(shotExt: ShotExt?) {
        if (flowTblShotsForScenes != null && flowTblShotsForScenes!!.cellCount > 0) {
            val first: Int = flowTblShotsForScenes!!.firstVisibleCell.index
            val last: Int = flowTblShotsForScenes!!.lastVisibleCell.index
            val selected = tblShotsForScenes!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblShotsForScenes?.scrollTo(shotExt) }
            }
        }
    }

    fun tblShotsForEventsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShotsForEvents != null && flowTblShotsForEvents!!.cellCount > 0) {
            val first: Int = flowTblShotsForEvents!!.firstVisibleCell.index
            val last: Int = flowTblShotsForEvents!!.lastVisibleCell.index
            val selected = tblShotsForEvents!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblShotsForEvents?.scrollTo(shotExt) }
            }
        }
    }

    fun tblPersonsAllSmartScroll(personExt: PersonExt?) {
        if (flowTblPersonsAll != null && flowTblPersonsAll!!.cellCount > 0) {
            val first: Int = flowTblPersonsAll!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPersonsAll!!.lastVisibleCell.getIndex()
            val selected = tblPersonsAllForFile!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                Platform.runLater { tblPersonsAllForFile?.scrollTo(personExt) }
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
            val y: Double = heightPadding + matrixFrame.row * (Main.PREVIEW_FRAME_H + 2) // Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H) // устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault // устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER // устанавливаем позиционирование по центру
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
                flagNextFrameIsFind =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1]
                        .frameExt!!
                        .frame.isFind
                flagNextFrameIsManualAdd =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1]
                        .frameExt!!
                        .frame.isManualAdd
                flagNextFrameIsManualCancel =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) + 1]
                        .frameExt!!
                        .frame.isManualCancel
            }
            if (matrixPageFrames.matrixFrames.indexOf(matrixFrame) > 0) {
                flagPrevFrameIsFind =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1]
                        .frameExt!!
                        .frame.isFind
                flagPrevFrameIsManualAdd =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1]
                        .frameExt!!
                        .frame.isManualAdd
                flagPrevFrameIsManualCancel =
                    matrixPageFrames.matrixFrames[matrixPageFrames.matrixFrames.indexOf(matrixFrame) - 1]
                        .frameExt!!
                        .frame.isManualCancel
            }

            if (flagPrevFrameIsFind ||
                flagPrevFrameIsManualAdd ||
                flagPrevFrameIsManualCancel ||
                flagNextFrameIsFind ||
                flagNextFrameIsManualAdd ||
                flagNextFrameIsManualCancel ||
                flagCurrFrameIsIFrame ||
                flagCurrFrameIsFind ||
                flagCurrFrameIsManualAdd ||
                flagCurrFrameIsManualCancel ||
                flagCurrFrameContainFaces
            ) {
//                resultImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                resultImage = matrixFrame.frameExt!!.biSmall
            }

            if (flagCurrFrameContainFaces) {
                resultImage = OverlayImage.setOverlayTriangle(resultImage!!, 4, 0.2, Color.BLUE, 1.0F)
            }
            if (flagCurrFrameIsIFrame) resultImage = OverlayImage.setOverlayIFrame(resultImage!!)
            if (flagCurrFrameIsFind) {
                resultImage =
                    if (flagCurrFrameIsManualCancel) {
                        OverlayImage.cancelOverlayFirstFrameManual(
                            resultImage!!,
                        )
                    } else {
                        OverlayImage.setOverlayFirstFrameFound(resultImage!!)
                    }
            }
            if (flagNextFrameIsFind) {
                resultImage =
                    if (flagNextFrameIsManualCancel) {
                        OverlayImage.cancelOverlayLastFrameManual(
                            resultImage!!,
                        )
                    } else {
                        OverlayImage.setOverlayLastFrameFound(resultImage!!)
                    }
            }
            if (flagCurrFrameIsManualAdd) resultImage = OverlayImage.setOverlayFirstFrameManual(resultImage!!)
            if (flagNextFrameIsManualAdd) resultImage = OverlayImage.setOverlayLastFrameManual(resultImage!!)

            if (resultImage != null) {
                val screenImageView = ImageView(ConvertToFxImage.convertToFxImage(resultImage)) // загружаем ресайзный буфер в новый вьювер
                screenImageView.fitWidth = Main.PREVIEW_FRAME_W // устанавливаем ширину вьювера
                screenImageView.fitHeight = Main.PREVIEW_FRAME_H // устанавливаем высоту вьювера
                lbl.graphic = null // сбрасываем графику лейбла
                lbl.graphic = screenImageView // устанавливаем вьювер источником графики для лейбла
            }

            pane.children.add(lbl)

            // событие "наведение мыши"
            lbl.onMouseEntered =
                EventHandler {
                    lbl.style = fxBorderFocused
                    lbl.toFront()
                }

            // событие "уход мыши"
            lbl.onMouseExited =
                EventHandler {
                    lbl.style = if (matrixFrame == currentMatrixFrame) fxBorderSelected else fxBorderDefault
                }

            lbl.onMouseClicked =
                EventHandler { mouseEvent ->

                    // событие двойного клика
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
                            Trace.action("двойной клик по кадру " + matrixFrame.frameExt!!.frame.frameNumber)
                            if (matrixFrame.frameExt!!.frame.isFind) { // фрейм найден
                                if (!matrixFrame.frameExt!!.frame.isManualCancel) { // и не отменен вручную
                                    matrixFrame.frameExt!!.frame.isManualCancel = true // отменяем
                                    matrixFrame.frameExt!!.frame.isFinalFind = false
                                    FrameController.save(matrixFrame.frameExt!!.frame)
                                    if (!splitOrUnionShots(matrixFrame)) {
                                        Trace.manualReset("ShotsEdit (откат)", matrixFrame.frameExt!!.frame.frameNumber, "cancel")
                                        matrixFrame.frameExt!!.frame.isManualCancel = false
                                        matrixFrame.frameExt!!.frame.isFinalFind = true
                                        FrameController.save(matrixFrame.frameExt!!.frame)
                                    }
                                } else { // и отменен вручную
                                    Trace.manualReset("ShotsEdit (откат)", matrixFrame.frameExt!!.frame.frameNumber, "cancel")
                                    matrixFrame.frameExt!!.frame.isManualCancel = false // восстанавливаем отметку
                                    matrixFrame.frameExt!!.frame.isFinalFind = true
                                    FrameController.save(matrixFrame.frameExt!!.frame)
                                    if (!splitOrUnionShots(matrixFrame)) {
                                        matrixFrame.frameExt!!.frame.isManualCancel = true
                                        matrixFrame.frameExt!!.frame.isFinalFind = false
                                        FrameController.save(matrixFrame.frameExt!!.frame)
                                    }
                                }
                            } else { // не найден
                                if (!matrixFrame.frameExt!!.frame.isManualAdd) { // и не отменен вручную
                                    matrixFrame.frameExt!!.frame.isManualAdd = true // отмечаем
                                    matrixFrame.frameExt!!.frame.isFinalFind = true
                                    FrameController.save(matrixFrame.frameExt!!.frame)
                                    if (!splitOrUnionShots(matrixFrame)) {
                                        Trace.manualReset("ShotsEdit (откат)", matrixFrame.frameExt!!.frame.frameNumber, "add")
                                        matrixFrame.frameExt!!.frame.isManualAdd = false
                                        matrixFrame.frameExt!!.frame.isFinalFind = false
                                        FrameController.save(matrixFrame.frameExt!!.frame)
                                    }
                                } else { // и отменен вручную
                                    Trace.manualReset("ShotsEdit (откат)", matrixFrame.frameExt!!.frame.frameNumber, "add")
                                    matrixFrame.frameExt!!.frame.isManualAdd = false // снимаем отметку
                                    matrixFrame.frameExt!!.frame.isFinalFind = false
                                    matrixFrame.frameExt!!.labelSmall.graphic = matrixFrame.frameExt!!.previewSmall
                                    matrixPageFrames.matrixFrames[
                                        matrixPageFrames.matrixFrames.indexOf(
                                            matrixFrame,
                                        ) - 1,
                                    ].frameExt
                                        ?.labelSmall
                                        ?.graphic =
                                        matrixPageFrames.matrixFrames[
                                            matrixPageFrames.matrixFrames.indexOf(
                                                matrixFrame,
                                            ) - 1,
                                        ].frameExt?.previewSmall
                                    FrameController.save(matrixFrame.frameExt!!.frame)
                                    if (!splitOrUnionShots(matrixFrame)) {
                                        matrixFrame.frameExt!!.frame.isManualAdd = true
                                        matrixFrame.frameExt!!.frame.isFinalFind = true
                                        FrameController.save(matrixFrame.frameExt!!.frame)
                                    }
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

    /**
     * Контекстное меню лица — то самое, что и у лиц в Persons: безымянный,
     * не человек, массовка, выбор персоны, создание персоны, картинка
     * персоны и отметки образцов. Для лиц трека добавляется первым пункт
     * разделения трека, остальное совпадает дословно.
     *
     * Различие одно и оно в намерении: назначение применяется либо к
     * выделенным лицам, либо, если передан трек, ко всему треку. Иначе
     * трек рассыпался бы — назвали одно лицо, а остальные остались
     * безымянными, и называть человека по треку перестало бы работать.
     */
    private fun faceContextMenu(
        matrixFace: MatrixFace,
        matrixPageFaces: MatrixPageFaces?,
        trackExt: FaceTrackExt?,
    ): ContextMenu {
        // Лица, к которым применяется выбор из меню. В Persons выделенные лица
        // лежат в selectedMatrixFaces, в Tracks — в selectedTrackFaces, и это
        // РАЗНЫЕ списки. Меню работало только с первым, поэтому в треках оно
        // действовало на одно лицо под курсором, а выделенные молча
        // игнорировались: человек отмечал пять лиц, назначал персону — и
        // менялся один. Тип общий — MutableCollection, чтобы не копировать
        // список: копия очищала бы сама себя, а не источник, и выделение
        // осталось бы висеть на экране.
        val menuFaces: MutableCollection<MatrixFace> =
            if (trackExt != null) selectedTrackFaces else selectedMatrixFaces
        val refreshFaces: () -> Unit =
            if (trackExt != null) {
                {
                    reloadTracks(currentShotExt)
                    // Список персон плана пересобирался только при выборе плана и
                    // при правке во вкладке Persons. Назначение из треков его не
                    // трогало, и новые персоны в списке плана не появлялись:
                    // в треках они были, а рядом в Persons — нет. Список берётся
                    // из базы, поэтому пересобирается на каждый такой пункт меню.
                    reloadPersonsForShot()
                }
            } else {
                { reorganizeMatrixFaces() }
            }
        // faceExt у MatrixFace объявлен как nullable, а внутри тела метода
        // умное приведение типа не работает, поэтому берём его один раз.
        val mfFaceExt = matrixFace.faceExt
        if (mfFaceExt == null) return ContextMenu()
        val contextMenu = ContextMenu()

        // Строки ниже взяты из меню вкладки Persons без изменений — чтобы
        // треки пользовались ровно тем же меню, а не похожим.

        var menuItem = MenuItem("UNDEFINDED")
        menuItem.setOnAction {
            var personExtUndefinded = listPersonsExtForFile.firstOrNull { it.person.personType == PersonType.UNDEFINDED }
            if (personExtUndefinded == null) {
                personExtUndefinded = projectPersonExtUndefinded
                listPersonsExtForFile.add(personExtUndefinded)
                listPersonsExtForFile.sort()
            }

            menuFaces.add(matrixFace)
            menuFaces.forEach { mf ->

                if (mf.faceExt!!
                        .personExt.person.personType != PersonType.UNDEFINDED
                ) {
                    mf.faceExt.personExt = personExtUndefinded!!
                    mf.faceExt.face.person = personExtUndefinded.person
                    mf.faceExt.face.personRecognizedName =
                        if (personExtUndefinded.person.personType ==
                            PersonType.UNDEFINDED
                        ) {
                            ""
                        } else {
                            personExtUndefinded.person.nameInRecognizer
                        }
                    FaceController.save(mf.faceExt.face)
                    listFacesExt.remove(mf.faceExt)
                    val page = matrixPageFaces
                    if (page != null && page.matrixFaces.size > 0) {
                        page.matrixFaces.remove(mf)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                    }
                }
            }
            menuFaces.clear()
            refreshFaces()
        }
        contextMenu.items.add(menuItem)

        menuItem = MenuItem("NONPERSON")
        menuItem.setOnAction {
            var personExtNonperson = listPersonsExtForFile.firstOrNull { it.person.personType == PersonType.NONPERSON }
            if (personExtNonperson == null) {
                personExtNonperson = projectPersonExtNonperson
                listPersonsExtForFile.add(personExtNonperson)
                listPersonsExtForFile.sort()
            }

            menuFaces.add(matrixFace)
            menuFaces.forEach { mf ->

                if (mf.faceExt!!
                        .personExt.person.personType != PersonType.NONPERSON
                ) {
                    mf.faceExt.personExt = personExtNonperson!!
                    mf.faceExt.face.person = personExtNonperson.person
                    mf.faceExt.face.personRecognizedName =
                        if (personExtNonperson.person.personType ==
                            PersonType.UNDEFINDED
                        ) {
                            ""
                        } else {
                            personExtNonperson.person.nameInRecognizer
                        }
                    FaceController.save(mf.faceExt.face)
                    listFacesExt.remove(mf.faceExt)
                    val page2 = matrixPageFaces
                    if (page2 != null && page2.matrixFaces.size > 0) {
                        page2.matrixFaces.remove(mf)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                    }
                }
            }
            menuFaces.clear()
            refreshFaces()
        }
        contextMenu.items.add(menuItem)

        menuItem = MenuItem("EXTRAS")
        menuItem.setOnAction {
            val personExtExtras = extrasPersonExt()

            menuFaces.add(matrixFace)
            menuFaces.forEach { mf ->

                // Ядро то же, что у кнопки «All to EXTRAS»: различается
                // только набор лиц.
                if (markFaceAsExtras(mf.faceExt!!, personExtExtras)) {
                    val page2 = matrixPageFaces
                    if (page2 != null && page2.matrixFaces.size > 0) {
                        page2.matrixFaces.remove(mf)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                    }
                }
            }
            menuFaces.clear()
            refreshFaces()
        }

        contextMenu.items.add(menuItem)

        menuItem = MenuItem("SELECT PERSON")
        menuItem.setOnAction {
            menuFaces.add(matrixFace)
            val selectedPerson = PersonSelectFXController().getPersonExt(currentFileExt!!.projectExt)

            if (selectedPerson != null) {
                if (!(listPersonsExtForFile.any { it.person.id == selectedPerson.person.id })) {
                    listPersonsExtForFile.add(selectedPerson)
                    listPersonsExtForFile.sort()
                }
                menuFaces.forEach { mf ->

                    mf.faceExt!!.personExt = selectedPerson
                    mf.faceExt.face.person = selectedPerson.person
                    mf.faceExt.face.personRecognizedName =
                        if (selectedPerson.person.personType == PersonType.UNDEFINDED) "" else selectedPerson.person.nameInRecognizer
                    FaceController.save(mf.faceExt.face)
                    listFacesExt.remove(mf.faceExt)
                    val page2 = matrixPageFaces
                    if (page2 != null && page2.matrixFaces.size > 0) {
                        page2.matrixFaces.remove(mf)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                    }
                }
            }

            menuFaces.clear()
            refreshFaces()
        }

        contextMenu.items.add(menuItem)

        menuItem = MenuItem("CREATE NEW PERSON")
        menuItem.setOnAction {
            menuFaces.add(matrixFace)

            val dialog = TextInputDialog("New person")

            dialog.title = "Create new person"
            dialog.headerText = "Enter person name:"
            dialog.contentText = "Name:"

            val result: Optional<String> = dialog.showAndWait()
            var personName: String? = null
            result.ifPresent { name -> personName = name }

            if (personName != null) {
                val newPerson =
                    PersonController.create(
                        currentFileExt!!.projectExt.project,
                        personName!!,
                        PersonType.PERSON,
                        "",
                        currentFileExt!!.file.id,
                        mfFaceExt.frameNumber,
                        mfFaceExt.faceNumberInFrame,
                    )
                val selectedPerson = PersonExt(newPerson, currentFileExt!!.projectExt)
                if (!listPersonsExtForFile.contains(selectedPerson)) {
                    listPersonsExtForFile.add(selectedPerson)
                    listPersonsExtForFile.sort()
                }

                menuFaces.forEach { mf ->
                    mf.faceExt!!.personExt = selectedPerson
                    mf.faceExt.face.person = selectedPerson.person
                    mf.faceExt.face.personRecognizedName =
                        if (selectedPerson.person.personType == PersonType.UNDEFINDED) "" else selectedPerson.person.nameInRecognizer

                    FaceController.save(mf.faceExt.face)
                    listFacesExt.remove(mf.faceExt)
                    val page2 = matrixPageFaces
                    if (page2 != null && page2.matrixFaces.size > 0) {
                        page2.matrixFaces.remove(mf)
                        mf.faceExt.labelSmall.graphic = null
                        mf.faceExt.labelSmall.style = fxBorderDefault
                    }
                }
                PersonEditFXController().editPerson(currentFileExt!!.projectExt, selectedPerson, hostServices)
            }

            menuFaces.clear()
            refreshFaces()
        }

        contextMenu.items.add(menuItem)

        // Персонаж, к которому относятся пункты меню. Во вкладке Persons это
        // выбранный в таблице, там лицо и его персонаж всегда совпадают. В
        // Tracks лицо принадлежит строке трека, и брать надо именно его.
        // Раньше здесь стоял currentPersonExt всегда, и в треках картинка лица
        // Чхику становилась картинкой Арьи: подставлялся тот персонаж,
        // который был выбран во вкладке Persons последним, а вовсе не тот,
        // кому принадлежит лицо под курсором.
        val menuPersonExt: PersonExt? =
            if (trackExt != null) mfFaceExt.personExt else currentPersonExt

        // Служебным персонам портрет не назначается: у «неопределённого»,
        // «массовки» и «не персонаж» нет личности, и картинка реального
        // человека на их строке только путает. Пункт оставлен на месте, но
        // выключен, чтобы привычное место в меню не исчезло из-под руки.
        val isServicePerson =
            menuPersonExt != null &&
                menuPersonExt.person.personType != PersonType.PERSON

        menuItem = MenuItem("Set as person picture")
        menuItem.isDisable = isServicePerson
        menuItem.setOnAction {
            if (isServicePerson) return@setOnAction

            val basePerson = menuPersonExt
            val selectedPerson = basePerson?.let { bp -> listPersonsExtForFile.firstOrNull { it.person.id == bp.person.id } }
            if (selectedPerson != null) {
                try {
                    IOFile(selectedPerson.pathToSmall).delete()
                    IOFile(selectedPerson.pathToMedium).delete()
                } catch (_: IOException) {
                }

                selectedPerson.person.fileIdForPreview = currentFileExt!!.file.id
                selectedPerson.person.faceNumberForPreview = mfFaceExt.face.faceNumberInFrame
                selectedPerson.person.frameNumberForPreview = mfFaceExt.face.frameNumber
                PersonController.save(selectedPerson.person)
                selectedPerson.resetPreview()
                selectedPerson.labelSmall
                menuFaces.clear()
                tblPersonsAllForFile!!.refresh()
                if (trackExt == null) {
                    currentPersonExt = selectedPerson
                } else {
                    // В треках картинка в строке не берётся из того же
                    // объекта персоны, что обновился во вкладке Persons:
                    // строка трека держит свой портрет, прочитанный из
                    // файла. Обновления таблицы Persons здесь ничего не
                    // дают — трбовалось перерисовать строку трека, и
                    // без этого старая картинка держалась до перезагрузки
                    // страницы.
                    reloadTracks(currentShotExt)
                }
            }
        }
        contextMenu.items.add(menuItem)

        if (!mfFaceExt.face.isManual && !mfFaceExt.face.isExample) {
            menuItem = MenuItem("Set as EXAMPLE")
            menuItem.setOnAction {
                menuFaces.add(matrixFace)
                val selectedPerson = menuPersonExt?.let { bp -> listPersonsExtForFile.firstOrNull { it.person.id == bp.person.id } }
                if (selectedPerson != null) {
                    menuFaces.forEach { mf ->
                        if (!mf.faceExt!!.face.isManual && !mf.faceExt.face.isExample) {
                            mf.faceExt.face.isExample = true
                            FaceController.save(mf.faceExt.face)
                            try {
                                IOFile(mf.faceExt.pathToPreviewFile).delete()
                            } catch (_: IOException) {
                            }

                            val frame =
                                Main.frameRepo
                                    .findByFileIdAndFrameNumber(
                                        mf.faceExt.fileId,
                                        mf.faceExt.frameNumber,
                                    ).firstOrNull()
                            if (frame != null) {
                                frame.file = mf.faceExt.fileExt.file
                                val frameExt = FrameExt(frame, mf.faceExt.fileExt)
                                if (!IOFile(
                                        mf.faceExt.pathToPreviewFile,
                                    ).parentFile.exists()
                                ) {
                                    IOFile(mf.faceExt.pathToPreviewFile).parentFile.mkdir()
                                }
                                val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
                                var bi =
                                    OverlayImage.extractRegion(
                                        biSource,
                                        mf.faceExt.startX,
                                        mf.faceExt.startY,
                                        mf.faceExt.endX,
                                        mf.faceExt.endY,
                                        Main.PREVIEW_FACE_W.toInt(),
                                        Main.PREVIEW_FACE_H.toInt(),
                                        Main.PREVIEW_FACE_EXPAND_FACTOR,
                                        Main.PREVIEW_FACE_CROPPING,
                                    )
                                if (mf.faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.GREEN, 1.0F)
                                if (mf.faceExt.face.isManual) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.RED, 1.0F)
                                val outputfile = IOFile(mf.faceExt.pathToPreviewFile)
                                ImageIO.write(bi, "jpg", outputfile)
                            }
                            mf.faceExt.resetPreviewSmall()
                            mf.faceExt.previewSmall
                        }
                    }
                }
                menuFaces.clear()
                refreshFaces()
            }
            contextMenu.items.add(menuItem)
        }

        if (!mfFaceExt.face.isManual && mfFaceExt.face.isExample) {
            menuItem = MenuItem("Remove from EXAMPLE")
            menuItem.setOnAction {
                menuFaces.add(matrixFace)
                val selectedPerson = menuPersonExt?.let { bp -> listPersonsExtForFile.firstOrNull { it.person.id == bp.person.id } }
                if (selectedPerson != null) {
                    menuFaces.forEach { mf ->
                        if (!mf.faceExt!!.face.isManual && mf.faceExt.face.isExample) {
                            mf.faceExt.face.isExample = false
                            FaceController.save(mf.faceExt.face)
                            try {
                                IOFile(mf.faceExt.pathToPreviewFile).delete()
                            } catch (_: IOException) {
                            }

                            val frame =
                                Main.frameRepo
                                    .findByFileIdAndFrameNumber(
                                        mf.faceExt.fileId,
                                        mf.faceExt.frameNumber,
                                    ).firstOrNull()
                            if (frame != null) {
                                frame.file = mf.faceExt.fileExt.file
                                val frameExt = FrameExt(frame, mf.faceExt.fileExt)
                                if (!IOFile(
                                        mf.faceExt.pathToPreviewFile,
                                    ).parentFile.exists()
                                ) {
                                    IOFile(mf.faceExt.pathToPreviewFile).parentFile.mkdir()
                                }
                                val biSource = ImageIO.read(IOFile(frameExt.pathToFull))
                                var bi =
                                    OverlayImage.extractRegion(
                                        biSource,
                                        mf.faceExt.startX,
                                        mf.faceExt.startY,
                                        mf.faceExt.endX,
                                        mf.faceExt.endY,
                                        Main.PREVIEW_FACE_W.toInt(),
                                        Main.PREVIEW_FACE_H.toInt(),
                                        Main.PREVIEW_FACE_EXPAND_FACTOR,
                                        Main.PREVIEW_FACE_CROPPING,
                                    )
                                if (mf.faceExt.face.isExample) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.GREEN, 1.0F)
                                if (mf.faceExt.face.isManual) bi = OverlayImage.setOverlayTriangle(bi, 3, 0.2, Color.RED, 1.0F)
                                val outputfile = IOFile(mf.faceExt.pathToPreviewFile)
                                ImageIO.write(bi, "jpg", outputfile)
                            }
                            mf.faceExt.resetPreviewSmall()
                            mf.faceExt.previewSmall
                        }
                    }
                }
                menuFaces.clear()
                refreshFaces()
            }
            contextMenu.items.add(menuItem)
        }
        return contextMenu
    }

    fun showMatrixPageFaces(matrixPageFaces: MatrixPageFaces) {
        val heightPadding = 10 // по высоте двойной отступ
        val widthPadding = 10 // по ширине двойной отступ
        val pane: Pane = paneFaces!!
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFace in matrixPageFaces.matrixFaces) {
            val lbl: Label = matrixFace.faceExt?.labelSmall!!
            val x: Double = widthPadding + matrixFace.column * (Main.PREVIEW_FACE_W + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFace.row * (Main.PREVIEW_FACE_H + 2) // Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H) // устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault // устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER // устанавливаем позиционирование по центру

            val screenImageView = matrixFace.faceExt.previewSmall
            screenImageView.fitWidth = Main.PREVIEW_FACE_W // устанавливаем ширину вьювера
            screenImageView.fitHeight = Main.PREVIEW_FACE_H // устанавливаем высоту вьювера
            lbl.graphic = null // сбрасываем графику лейбла
            lbl.graphic = screenImageView // устанавливаем вьювер источником графики для лейбла
            pane.children.add(lbl)

            if (matrixFace.column == 0 || matrixFace.column == currentMatrixPageFaces!!.countColumns + 1) continue

            val contextMenu = faceContextMenu(matrixFace, currentMatrixPageFaces, null)
            // Меню обязано быть привязано к картинке: без этой строки объект
            // создавался и сразу забывался, и правой кнопкой по лицу в Persons
            // не открывалось ничего. При выносе меню в общий метод строка
            // потерялась, а меню в треках привязывалось отдельной строкой —
            // поэтому там всё работало, а в Persons нет.
            lbl.contextMenu = contextMenu

            // событие "наведение мыши"
            lbl.onMouseEntered =
                EventHandler {
                    lbl.style = if (selectedMatrixFaces.contains(matrixFace)) fxBorderSelectedFocused else fxBorderFocused
                    lbl.toFront()
                }

            // событие "уход мыши"
            lbl.onMouseExited =
                EventHandler {
                    lbl.style = if (selectedMatrixFaces.contains(matrixFace)) fxBorderSelected else fxBorderDefault
                }

            lbl.onMouseClicked =
                EventHandler { mouseEvent ->
                    if (consumeDragHandled()) return@EventHandler

                    if (mouseEvent.button == MouseButton.PRIMARY) {
                        if (mouseEvent.clickCount == 1) {
                            val frameExt = currentFileExt!!.framesExt.firstOrNull { it.frame.frameNumber == matrixFace.faceExt.frameNumber }
                            if (frameExt != null) loadPictureToFullFrameLabelForFace(frameExt, matrixFace.faceExt)
                            if (!isPressedControl && !isPressedShift) {
                                selectedMatrixFaces.forEach { it.faceExt?.labelSmall?.style = fxBorderDefault }
                                selectedMatrixFaces.clear()
                            } else if (isPressedShift) {
                                val indexLastClicked = currentMatrixPageFaces!!.matrixFaces.indexOf(lastClickedMatrixFace)
                                val indexCurrentClicked = currentMatrixPageFaces!!.matrixFaces.indexOf(matrixFace)
                                // indexOf возвращает -1, если кадра на странице
                                // нет: страница могла смениться, пока последний
                                // клик остался от прежней. Диапазон -1..N тут же
                                // давал Index -1 и ронял выбор по Shift.
                                if (indexLastClicked >= 0 && indexCurrentClicked >= 0 && indexLastClicked < indexCurrentClicked) {
                                    for (i in indexLastClicked..indexCurrentClicked) {
                                        val mf = currentMatrixPageFaces!!.matrixFaces[i]
                                        selectedMatrixFaces.add(mf)
                                        mf.faceExt?.labelSmall?.style = fxBorderSelected
                                    }
                                }
                            } else if (selectedMatrixFaces.contains(matrixFace)) {
                                // Ctrl по уже выделенному лицу снимает выбор.
                                //
                                // Раньше такой возможности не было: с Ctrl лицо
                                // просто добавлялось в выделение повторно, то
                                // есть выбор не менялся. Снять одно лицо, не
                                // разрушая остальные, было нечем — только
                                // сбросить всё целиком обычным щелчком.
                                //
                                // Возврат здесь обязателен: ниже стоит безусловное
                                // добавление нажатого лица в выделение, и без
                                // возврата оно тут же добавилось бы обратно.
                                //
                                // Превью и текущее лицо не трогаем — по решению
                                // владельца картинка остаётся, гаснет только рамка.
                                selectedMatrixFaces.remove(matrixFace)
                                matrixFace.faceExt?.labelSmall?.style = fxBorderDefault
                                lbl.style = fxBorderDefault
                                return@EventHandler
                            }
                            lastClickedMatrixFace = matrixFace
                            selectedMatrixFaces.add(matrixFace)

                            currentMatrixFace!!.faceExt?.labelSmall!!.style =
                                if (selectedMatrixFaces.contains(currentMatrixFace)) fxBorderSelected else fxBorderDefault
                            currentMatrixFace = matrixFace
                            lbl.style = fxBorderSelected
//                        loadPictureToFullFrameLabel(currentMatrixFace.faceExt.pathToFrameFile)
                        }
                    }
                }

            // Перенос — на собственной обработке мыши, как в треках:
            // средства перетаскивания JavaFX событий о переносе не доставляли.
            lbl.onMousePressed =
                EventHandler { mouseEvent ->
                    beginFaceDrag(mouseEvent, matrixFace, selectedMatrixFaces)
                }
            lbl.onMouseDragged =
                EventHandler { mouseEvent ->
                    continueFaceDrag(mouseEvent)
                }
            lbl.onMouseReleased =
                EventHandler { mouseEvent ->
                    if (finishPersonsFaceDrag(mouseEvent)) mouseEvent.consume()
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

    fun getShotExtByFrameNumber(frameNumber: Int): ShotExt? =
        currentFileExt!!
            .shotsExt
            .filter {
                frameNumber >= it.firstFrameExt.frame.frameNumber &&
                    frameNumber <= it.lastFrameExt.frame.frameNumber
            }.firstOrNull()

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
                listMatrixPageFrames =
                    MatrixPageFrames.createPages(
                        currentFileExt!!.framesExt,
                        paneFrames!!.getWidth(),
                        paneFrames!!.getHeight(),
                        Main.PREVIEW_FRAME_W,
                        Main.PREVIEW_FRAME_H,
                    )
                tblPagesFrames!!.items = listMatrixPageFrames
                // Пока кадры не загружены, страниц нет, искать нечего:
                // без этой проверки showMatrixPageFrames получал null
                // и отрисовка падала с NullPointerException.
                if (listMatrixPageFrames.isNotEmpty()) {
                    currentMatrixFrame = getMatrixFrameByFrameNumber(frameNumber)
                    currentMatrixPageFrames = getMatrixPageFramesByFrame(frameNumber)
                    currentShotExt = getShotExtByFrameNumber(frameNumber)
                    if (currentMatrixPageFrames != null) {
                        showMatrixPageFrames(currentMatrixPageFrames!!)
                        tblPagesFrames!!.selectionModel.select(currentMatrixPageFrames)
                        goToFrame(currentMatrixFrame)
                    }
                }
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
                listMatrixPageFaces =
                    MatrixPageFaces.createPages(
                        listFacesExt,
                        paneFaces!!.getWidth(),
                        paneFaces!!.getHeight(),
                        Main.PREVIEW_FACE_W,
                        Main.PREVIEW_FACE_H,
                    )
                tblPagesFaces!!.items = listMatrixPageFaces
                // До детекции лиц список пуст, а .first() на пустом списке
                // ронял отрисовку с NoSuchElementException — окно редактора
                // планов из-за этого не открывалось вовсе.
                val firstFace = listMatrixPageFaces.firstOrNull()?.matrixFaces?.firstOrNull()
                if (firstFace != null) {
                    if (currentMatrixFace == null) currentMatrixFace = firstFace
                    currentMatrixPageFaces = getMatrixPageFacesByMatrixFace(currentMatrixFace!!)

                    if (currentMatrixPageFaces != null) {
                        showMatrixPageFaces(currentMatrixPageFaces!!)
                        tblPagesFaces!!.selectionModel.select(currentMatrixPageFaces)
                    }

                    goToFace(currentMatrixFace)
                }
            }
        }
//        }
    }

    fun onActionButtonGetShotType(shotExt: ShotExt) {
        val contextMenuShotType = ContextMenu()
        ShotTypePerson.values().forEach { shotTypePerson ->
            val imageView = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shotTypePerson.pathToPicture))))
            val contextMenuShotTypeItem = MenuItem(null, imageView)
            contextMenuShotTypeItem.onAction =
                EventHandler { e: ActionEvent? ->
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
        contextMenuShotType.show(mainStage, screenBounds.minX + screenBounds.width, screenBounds.minY)
    }

    fun reorganizeMatrixFaces() {
        val list: MutableList<FaceExt> =
            listMatrixPageFaces
                .flatMap { it.matrixFaces }
                .mapNotNull { it.faceExt }
                .toMutableSet()
                .toMutableList()
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
            isDoneLoadListPersonFacesExt.set(true)
        }
    }

    @FXML
    fun doSelectFacesCb(event: ActionEvent?) {
        Trace.action("doSelectFacesCb")
        isDoneLoadListPersonFacesExt.set(false)
        if (rbFaceFile!!.isSelected) {
            LoadListPersonFacesExtForFile(
                listFacesExt,
                currentFileExt!!,
                currentPersonExt!!,
                pbFaces,
                null,
                cbFacesNotExample!!.isSelected,
                cbFacesExample!!.isSelected,
                cbFacesNotManual!!.isSelected,
                cbFacesManual!!.isSelected,
                isDoneLoadListPersonFacesExt,
            ).start()
        } else if (rbFaceAll!!.isSelected) {
            LoadListPersonFacesExtForAll(
                listFacesExt,
                currentFileExt!!.projectExt,
                currentPersonExt!!,
                pbFaces,
                lblPb,
                cbFacesNotExample!!.isSelected,
                cbFacesExample!!.isSelected,
                cbFacesNotManual!!.isSelected,
                cbFacesManual!!.isSelected,
                isDoneLoadListPersonFacesExt,
            ).start()
        }
    }

    /**
     * Переключатель «All / File» для лиц.
     *
     * Обработчик был пустым — кнопки нарисованы, `onAction` есть, сборка
     * проходит, а нажатие ничего не делает. Реализовать пока нечем: в
     * контроллере нет двух наборов лиц, которые эти кнопки должны
     * переключать, — есть один `listFacesExt`. Поэтому оставляем только
     * отметку в журнале, чтобы факт нажатия был виден.
     */
    @FXML
    fun doSelectFacesRb(event: ActionEvent?) {
        Trace.action("doSelectFacesRb: наборы лиц для All и File не разделены, переключение не работает")
    }

    /**
     * Переключатель «All / File» для персон.
     *
     * Он выбирает **источник данных**, а не то, какая из двух таблиц видна.
     * Таблиц две, и стоят они в разных местах окна: `tblPersonsAllForFile` —
     * список серии на вкладке «Persons», `tblPersonsAllForShot` — персоны
     * выбранного плана в середине окна. Переключение видимости между ними
     * было ошибкой: нажатие «All» скрывало правую таблицу (персоны пропадали),
     * нажатие «File» — среднюю (пропадали персоны плана). Обе таблицы
     * показываются всегда, меняется только наполнение правой:
     *
     * - **File** — персоны, у которых есть лица в этой серии;
     * - **All** — все персоны проекта, включая отмеченных в других сериях.
     *
     * Второй список нужен, чтобы назначить персону, которой в этой серии ещё
     * не было: раньше он был единственным, и переключатель был лишней
     * кнопкой с двумя подписями на одном и том же списке.
     */
    @FXML
    fun doSelectPersonsRb(event: ActionEvent?) {
        val all = grpPersons?.selectedToggle == rbPersonAll
        val source = if (all) "All" else "File"
        Trace.action("doSelectPersonsRb: $source, в списке ${listPersonsExtForFile.size}")
        if (currentFileExt == null) return
        if (all) {
            LoadListPersonsExtForProject(
                listPersonsExtForFile,
                currentFileExt!!.projectExt,
                pbPersonsForFile,
                null,
                isDoneLoadListPersonsExtForProject,
            ).start()
        } else {
            LoadListPersonsExtForFile(
                listPersonsExtForFile,
                currentFileExt!!,
                pbPersonsForFile,
                null,
                isDoneLoadListPersonsExtForFile,
                false,
            ).start()
        }
    }

    /**
     * Смена источника пересоздаёт элементы списка, поэтому выделение снимается,
     * а `currentPersonExt` остаётся от прежнего человека — панель лиц продолжала
     * бы показывать того, кого в списке уже нет. Персона возвращается в выделение,
     * если она в новом списке есть; иначе лица и выделение очищаются.
     */
    private fun restorePersonSelectionAfterPersonsReload() {
        val previous = currentPersonExt
        val stillThere =
            previous?.let { p ->
                listPersonsExtForFile.firstOrNull { it.person.id == p.person.id }
            }
        if (stillThere != null) {
            tblPersonsAllForFile?.selectionModel?.select(stillThere)
            return
        }
        if (previous == null) return
        currentPersonExt = null
        tblPersonsAllForFile?.selectionModel?.clearSelection()
        listFacesExt.clear()
        listMatrixPageFaces.clear()
        tblPagesFaces?.items = listMatrixPageFaces
        paneFaces?.children?.clear()
        currentMatrixFace = null
        currentMatrixPageFaces = null
    }

    /**
     * Вкладка треков: слева узкий столбец треков текущего монтажного куска,
     * справа — картинки лиц выбранного трека. Устроена так же, как Persons,
     * только вместо персоны трек.
     *
     * Трек — это один и тот же человек в пределах одного куска. Работать
     * нужно именно с ним: назвать человека один раз на весь трек вместо
     * десяти отдельных назначений по кадрам.
     *
     * Порядок списка: сперва треки без персона — это очередь работы, потом
     * названные, а смешанные в самый конец: их нельзя назвать одним именем,
     * сначала надо разделить.
     */
    private fun initTracksTab() {
        colTrackLabel?.cellValueFactory = PropertyValueFactory("labelSmall")
        colTrackFaces?.cellValueFactory = PropertyValueFactory("faceNumberText")
        colTrackPagesNumber?.cellValueFactory = PropertyValueFactory("pageNumber")

        tblTracks?.placeholder = Label(FaceTrackExt.EMPTY_TEXT)

        // Замер кнопки «All to EXTRAS»: где она оказалась и какого размера.
        // Кнопка нарисована в форме, но на экране её не было — нужно понять,
        // это пустая раскладка, нулевой размер или её вообще перекрыло.
        paneTrackFaces?.sceneProperty()?.addListener { _, _, scene ->
            if (scene != null) {
                Platform.runLater {
                    val b = btAllToExtras
                    println(
                        "[КНОПКА-EXTRAS] есть=${b != null}, " +
                            "размер=${b?.width}x${b?.height}, " +
                            "видима=${b?.isVisible}, " +
                            "на экране=${b?.localToScreen(b.boundsInLocal)}",
                    )
                }
            }
        }

        paneTrackFaces?.widthProperty()?.addListener { _, _, newWidth ->
            if ((newWidth as Number).toDouble() > 100.0 && listMatrixPageTrackFaces.isEmpty()) {
                val trackExt = trackExtToShow ?: return@addListener
                Platform.runLater { showTrackFaces(trackExt) }
            }
        }
        tblTracks?.items = listTracksExt
        tblTrackPages?.placeholder = Label("—")
        tblTrackPages?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            val page = newValue ?: return@addListener
            currentMatrixPageTrackFaces = page
            Platform.runLater { showTrackPageFaces(page) }
        }

        tblTracks?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            val trackExt = newValue ?: return@addListener
            showTrackFaces(trackExt)
        }

        // Треки привязаны к монтажному куску, поэтому список перечитывается
        // при выборе куска. Эта подписка уже терялась однажды при переписывании
        // метода целиком, и признаком была пустая таблица при заведомо
        // работающих треках, — поэтому она отдельным пунктом.
        tblShots?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            val shotExt = newValue as? ShotExt ?: return@addListener
            Platform.runLater { reloadTracks(shotExt) }
        }

        // Перетаскивание выделенных лиц на строку трека работает так же,
        // как на строку персоны: лицо получает того человека, чья строка
        // под курсором. Строка «разные люди» и «— не определён —» назначать
        // некому — там конкретного человека нет.
        setupFaceDropOnRows(
            tblTracks!!,
            { row ->
                if (row.mixed) {
                    null
                } else {
                    listPersonsExtForFile.firstOrNull { it.person.id == row.personId }
                }
            },
            { selectedTrackFaces },
            {
                reloadTracks(currentShotExt)
                reloadPersonsForShot()
            },
        )

        tblTracks?.setOnMouseClicked { event ->
            if (event.button == javafx.scene.input.MouseButton.SECONDARY && event.target == tblTracks) {
                val trackExt = tblTracks!!.selectionModel.selectedItem
                if (trackExt != null) {
                    val menu = tracksContextMenu(trackExt, trackExtToShow?.let { trackMatrixFaces(it) }?.firstOrNull())
                    menu.show(tblTracks, event.screenX, event.screenY)
                }
            }
        }
    }

    /**
     * Поведение лица трека — то же, что у лица персоны: наведение, уход,
     * выделение щелчком, Ctrl и Shift, перетаскивание и контекстное меню.
     * Отличий нет ни одного, и это осознанно: свой список выделенных лиц у
     * треков свой, но все действия над ним работают так же, как у персон, —
     * иначе пришлось бы учить одно и то же дважды.
     */
    private fun setupTrackFaceHandlers(
        lbl: Label,
        matrixFace: MatrixFace,
        trackExt: FaceTrackExt,
    ) {
        lbl.onMouseEntered =
            EventHandler {
                lbl.style = if (selectedTrackFaces.contains(matrixFace)) fxBorderSelectedFocused else fxBorderFocused
                lbl.toFront()
            }

        lbl.onMouseExited =
            EventHandler {
                lbl.style = if (selectedTrackFaces.contains(matrixFace)) fxBorderSelected else fxBorderDefault
            }

        lbl.onMouseClicked =
            EventHandler { mouseEvent ->
                if (consumeDragHandled()) return@EventHandler
                if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.clickCount == 1) {
                    val faceExt = matrixFace.faceExt
                    if (faceExt != null) {
                        val frameExt =
                            currentFileExt!!
                                .framesExt
                                .firstOrNull { it.frame.frameNumber == faceExt.face.frameNumber }
                        if (frameExt != null) loadPictureToFullFrameLabelForFace(frameExt, faceExt)
                    }

                    if (!isPressedControl && !isPressedShift) {
                        selectedTrackFaces.forEach { it.faceExt?.labelSmall?.style = fxBorderDefault }
                        selectedTrackFaces.clear()
                    } else if (isPressedShift) {
                        val lastIndex = currentMatrixPageTrackFaces?.matrixFaces?.indexOf(lastClickedTrackFace) ?: -1
                        val currentIndex = currentMatrixPageTrackFaces?.matrixFaces?.indexOf(matrixFace) ?: -1
                        if (lastIndex >= 0 && currentIndex >= 0 && lastIndex < currentIndex) {
                            for (i in lastIndex..currentIndex) {
                                val mf = currentMatrixPageTrackFaces!!.matrixFaces[i]
                                if (mf.faceExt != null && !selectedTrackFaces.contains(mf)) {
                                    selectedTrackFaces.add(mf)
                                    mf.faceExt!!.labelSmall.style = fxBorderSelected
                                }
                            }
                        }
                    } else if (selectedTrackFaces.contains(matrixFace)) {
                        // Ctrl по уже выделенному лицу снимает выбор — как в Persons
                        selectedTrackFaces.remove(matrixFace)
                        lbl.style = fxBorderDefault
                        return@EventHandler
                    }
                    lastClickedTrackFace = matrixFace
                    if (!selectedTrackFaces.contains(matrixFace)) selectedTrackFaces.add(matrixFace)
                    lbl.style = fxBorderSelected
                }
            }

        // Перенос сделан на собственной обработке мыши, а не средствами
        // перетаскивания JavaFX. Те отдавали картинку, но ни одного события
        // о переносе не доставляли — ни строке, ни таблице, ни сцене: схватить
        // можно было, а положить некуда. Здесь всё под нашим контролем.
        lbl.onMousePressed =
            EventHandler { mouseEvent ->
                beginFaceDrag(mouseEvent, matrixFace, selectedTrackFaces)
            }
        lbl.onMouseDragged =
            EventHandler { mouseEvent ->
                continueFaceDrag(mouseEvent)
            }
        lbl.onMouseReleased =
            EventHandler { mouseEvent ->
                if (finishFaceDrag(mouseEvent)) mouseEvent.consume()
            }

        lbl.contextMenu = tracksContextMenu(trackExt, matrixFace)
    }

    /**
     * Отрисовка страницы лиц трека.
     *
     * Отдельная функция, а не переиспользование отрисовки вкладки Faces:
     * та опирается на состояние Faces — выделенные лица и текущую страницу —
     * и без него падает. Здесь своя панель и своё состояние.
     */
    private fun showTrackPageFaces(page: MatrixPageFaces) {
        val pane = paneTrackFaces ?: return
        pane.children.clear()
        println(
            "[TRACKS] страница: лиц ${page.matrixFaces.size}, панель ${pane.width}x${pane.height}, " +
                "колонок ${page.countColumns}",
        )
        for (matrixFace in page.matrixFaces) {
            val faceExt = matrixFace.faceExt ?: continue
            val lbl = faceExt.labelSmall
            lbl.translateX = 10 + matrixFace.column * (Main.PREVIEW_FACE_W + 2)
            lbl.translateY = 10 + matrixFace.row * (Main.PREVIEW_FACE_H + 2)
            lbl.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
            lbl.style = fxBorderDefault
            lbl.alignment = Pos.CENTER
            val image = faceExt.previewSmall
            image.fitWidth = Main.PREVIEW_FACE_W
            image.fitHeight = Main.PREVIEW_FACE_H
            lbl.graphic = image
            pane.children.add(lbl)
            val trackExt = trackExtToShow
            if (trackExt != null) setupTrackFaceHandlers(lbl, matrixFace, trackExt)
        }
        // Горизонтальная черта между треками одной персоны: без неё лица
        // разных треков слипаются, и не видно, где кончается один трек.
        val faceRowH = Main.PREVIEW_FACE_H + 2
        for (row in page.separatorRows) {
            // Лица строки row стоят на 10 + row * faceRowH — значит верх этой
            // строки там же. Раньше черта бралась на строку выше, и между
            // чертой и лицами оставалась пустая строка во всю карточку.
            val line =
                javafx.scene.shape.Line(
                    10.0,
                    10 + row * faceRowH - 2,
                    Math.max(0.0, pane.width - 10),
                    10 + row * faceRowH - 2,
                )
            line.stroke = javafx.scene.paint.Color.GRAY
            pane.children.add(line)
        }
        pbTrackFaces?.progress = 1.0
    }

    /** Показывает лица выбранного трека справа, постранично. */
    private fun showTrackFaces(trackExt: FaceTrackExt) {
        trackExtToShow = trackExt
        println(
            "[TRACKS] показ трека ${trackExt.track.id}: лиц в треке ${trackExt.faces.size}, " +
                "для сетки ${trackExt.facesExtForGrid.size}, панель ${paneTrackFaces!!.width}x${paneTrackFaces!!.height}",
        )
        // Число колонок считается от ширины панели. Если вкладку выбрали раньше,
        // чем панель получила размеры, ширина равна нулю, страница выходит пустой
        // и лица не видны — хотя треки есть. В этом случае ждём размеров и
        // перерисовываем сами, см. подписку на ширину панели.
        if (paneTrackFaces!!.width < 100) {
            println("[TRACKS] панель ещё не размечена, ждём размера")
            return
        }
        listMatrixPageTrackFaces =
            MatrixPageFaces.createTrackPages(
                trackExt.facesExtByTrack,
                paneTrackFaces!!.width,
                paneTrackFaces!!.height,
                Main.PREVIEW_FACE_W,
                Main.PREVIEW_FACE_H,
            )
        println("[TRACKS] страниц получилось: ${listMatrixPageTrackFaces.size}")
        if (listMatrixPageTrackFaces.size > 0) {
            currentMatrixPageTrackFaces = listMatrixPageTrackFaces.first()
            tblTrackPages?.items = listMatrixPageTrackFaces
            tblTrackPages?.selectionModel?.select(0)
        }
        if (listMatrixPageTrackFaces.size > 0) showTrackPageFaces(listMatrixPageTrackFaces.first())
        lblTracks?.text = "Трек: ${trackExt.nameText}, кадры ${trackExt.framesText}, лиц ${trackExt.faces.size}"
    }

    /** Меню трека: назначить персону всем его лицам либо разделить трек. */

    /**
     * Меню трека: то же, что у лиц в Persons, и первым пунктом разделение
     * трека. Само меню собирает общий метод — чтобы пункты не разъехались
     * с лицовыми при правке одной вкладки.
     */
    private fun tracksContextMenu(
        trackExt: FaceTrackExt,
        matrixFace: MatrixFace?,
    ): ContextMenu {
        val menu = if (matrixFace != null) faceContextMenu(matrixFace, currentMatrixPageTrackFaces, trackExt) else ContextMenu()

        val itemSplit = MenuItem("Split track by person")
        itemSplit.setOnAction {
            splitTrackByPerson(trackExt)
            reloadTracks(currentShotExt)
        }
        menu.items.add(0, itemSplit)
        return menu
    }

    /** Лица трека как список для меню: то, что сейчас показано на странице. */
    private fun trackMatrixFaces(trackExt: FaceTrackExt): MutableSet<MatrixFace> {
        val result = mutableSetOf<MatrixFace>()
        val page = currentMatrixPageTrackFaces ?: return result
        for (mf in page.matrixFaces) {
            if (mf.faceExt != null && trackExt.faces.any { it.id == mf.faceExt!!.face.id }) result.add(mf)
        }
        return result
    }

    /**
     * Назначает персону всем лицам трека сразу.
     *
     * Лица уходят в неопределённые по одной причине: трек, собранный по
     * сходству, может объединить двух похожих людей, и тогда ни одна персона
     * не подходит всем его лицам. В таком треке это видно по пометке
     * «СМЕШАН», и его сперва разделяют.
     */
    private fun assignPersonToTrack(
        trackExt: FaceTrackExt,
        personExt: PersonExt,
    ) {
        if (trackExt.mixed) {
            val text = "Трек смешанный — сначала разделите его по пункту «Split track by person»"
            lblTracks?.text = text
            println("[TRACKS] $text")
            return
        }
        for (face in trackExt.faces) {
            face.person = personExt.person
            face.personRecognizedName =
                if (personExt.person.personType == PersonType.UNDEFINDED) {
                    ""
                } else {
                    personExt.person.nameInRecognizer
                }
            Main.faceRepo.save(face)
        }
        trackExt.track.person = personExt.person
        Main.faceTrackRepo.save(trackExt.track)
        println("[TRACKS] треку ${trackExt.track.id} назначен персонаж ${personExt.person.name}")
    }

    /**
     * Разделяет смешанный трек по уже проставленным персонам: лица одного
     * человека уходят в отдельный трек, остальные остаются.
     *
     * Именно так чинятся пять смешанных треков этой серии — там Визерис с
     * Дейнерисом и Серсея с Джейме, и назначить им по одному имени нельзя.
     */
    private fun splitTrackByPerson(trackExt: FaceTrackExt) {
        if (!trackExt.mixed) {
            println("[TRACKS] трек ${trackExt.track.id} не смешанный, разделять нечего")
            return
        }
        val byPerson = mutableMapOf<Long, MutableList<Face>>()
        for (face in trackExt.faces) {
            byPerson.getOrPut(face.person.id) { mutableListOf() }.add(face)
        }
        // Первую группу оставляем в исходном треке, остальные получают новые.
        val groups = byPerson.values.toMutableList()
        var index = 0
        for (group in groups) {
            if (index == 0) {
                for (face in group) face.track = trackExt.track
                trackExt.track.faceCount = group.size
                trackExt.track.firstFrameNumber = group.minOf { it.frameNumber }
                trackExt.track.lastFrameNumber = group.maxOf { it.frameNumber }
                trackExt.track.person = group.first().person
                Main.faceTrackRepo.save(trackExt.track)
            } else {
                val newTrack =
                    com.svoemesto.ivfx.models
                        .FaceTrack()
                newTrack.shot = trackExt.track.shot
                newTrack.firstFrameNumber = group.minOf { it.frameNumber }
                newTrack.lastFrameNumber = group.maxOf { it.frameNumber }
                newTrack.faceCount = group.size
                newTrack.person = group.first().person
                val saved = Main.faceTrackRepo.save(newTrack)
                for (face in group) {
                    face.track = saved
                    // Привязку лица к новому треку нужно записать: без
                    // сохранения в базе она останется только в памяти и
                    // пропадёт при следующем открытии файла.
                    Main.faceRepo.save(face)
                }
            }
            index++
        }
        println("[TRACKS] трек ${trackExt.track.id} разделён на ${groups.size} частей")
    }

    /**
     * Проставляет каждой персоне число её лиц, состоящих в треках.
     *
     * Нужно, чтобы видеть разрывы в разметке: персона определён в одной сцене
     * и не определён в соседних — по таблице это видно сразу, иначе пришлось
     * бы искать глазами по кадрам.
     */
    private fun fillTrackFacesCount() {
        try {
            for (row in Main.faceRepo.getTrackFacesCountByPerson(currentFileExt!!.file.id)) {
                val personId = (row[0] as Number).toLong()
                val count = (row[1] as Number).toInt()
                listPersonsExtForFile.firstOrNull { it.person.id == personId }?.trackFacesCount = count
            }
        } catch (e: Exception) {
            // Счётчик — справочная величина, его отсутствие работе не мешает
            println("[TRACKS] счётчик лиц в треках не заполнен: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Перечитывает треки монтажного куска.
     *
     * Работа с базой идёт вне потока интерфейса — иначе таблица встанет на
     * время запроса, — поэтому вызывать нужно уже из потока JavaFX.
     */
    private var isReloadingTracks = false

    private fun reloadTracks(shotExt: ShotExt? = currentShotExt) {
        // Перезагрузка пересобирает строки таблицы, а выбор строки зовёт
        // перезагрузку. Без этой защиты на плане без треков список
        // очищался, выбор сбрасывался и вызывал перезагрузку снова — цикл,
        // который крутился, пока не съедал память.
        if (isReloadingTracks) return
        isReloadingTracks = true
        try {
            reloadTracksInner(shotExt)
        } finally {
            isReloadingTracks = false
        }
    }

    private fun reloadTracksInner(shotExt: ShotExt? = currentShotExt) {
        // Какой трек показывали до пересборки: строки создаются заново, и без
        // этой привязки выбор возвращался бы наверх при каждом изменении.
        val keepTrackId = trackExtToShow?.tracks?.firstOrNull()?.id
        // Смена плана и пересборка того же плана — разные вещи. При смене
        // плана выбирается первая строка, как и просили; при пересборке того
        // же плана остаётся та строка, на которой шла работа, иначе каждое
        // действие в треках уводило бы наверх.
        val shotChanged = (shotExt?.shot?.id ?: 0L) != lastLoadedShotId
        lastLoadedShotId = shotExt?.shot?.id ?: 0L
        shotExtIdForLog = lastLoadedShotId
        listTracksExt.clear()
        if (shotExt == null) {
            lblTracks?.text = ""
            clearTrackFaces()
            return
        }
        val tracks = Main.faceTrackRepo.findByShotId(shotExt.shot.id).toMutableList()
        println(
            "[ПЕРЕЗАГРУЗКА] план=${shotExt.shot.id} треков=${tracks.size} " +
                "текущийПлан=${currentShotExt?.shot?.id}",
        )
        tracks.sort()

        // Лица читаются ОДИН раз и раскладываются по трекам. Раньше они
        // читались по запросу на каждый трек, а потом ещё раз для каждого
        // лица при раскладке по трекам — на плане с десятками треков это
        // тысячи обращений к базе, и выбор плана висел на минуту.
        //
        // Позже выяснилось, что читались лица ВСЕЙ серии, а не только этого
        // плана: у лица лежит вектор признаков около десяти килобайт, и для
        // второй серии это 86 МБ на один переход между планами. Теперь
        // берутся только лица треков этого плана — десятки записей.
        val facesByTrack = HashMap<Long, MutableList<Face>>()
        val trackIds = tracks.map { it.id }
        val shotFaces =
            if (trackIds.isEmpty()) {
                emptyList<Face>()
            } else {
                Main.faceRepo.findByTrackIds(trackIds).toList()
            }
        for (face in shotFaces) {
            val tid = face.track?.id ?: continue
            facesByTrack.getOrPut(tid) { mutableListOf() }.add(face)
        }

        val byPerson = LinkedHashMap<Long, MutableList<FaceTrack>>()
        val singleRows = mutableListOf<FaceTrack>()
        for (track in tracks) {
            val faces = facesByTrack[track.id] ?: mutableListOf()
            val personIds = faces.map { it.person.id }.distinct()
            // Неопознанные треки собираются В ОДНУ строку, как во вкладке Persons:
            // там у плана тоже одна строка «— не определён —», и разница была
            // только в треках. В плане после распознавания их доходило до 49,
            // и все они выглядели одинаково — работать с таким списком
            // невозможно. Людей там действительно много разных, но пока мы их
            // не знаем, они для списка неразличимы: отдельной строкей на
            // каждого нет смысла, работать с ними всё равно одним движением.
            val isUnknown =
                personIds.size == 1 &&
                    personIds[0] == projectPersonExtUndefinded?.person?.id
            if (personIds.size == 1) {
                byPerson.getOrPut(personIds[0]) { mutableListOf() }.add(track)
            } else {
                singleRows.add(track)
            }
        }

        val rows = mutableListOf<FaceTrackExt>()
        for ((personId, personTracks) in byPerson) {
            val pExt = listPersonsExtForFile.firstOrNull { it.person.id == personId }
            // У неопознанного персона имя в базе есть (UNDEFINDED), но в списке
            // он должен выглядеть как «— не определён —», без портрета:
            // портрета неопознанного лица не существует.
            val isUndefinded = personId == projectPersonExtUndefinded?.person?.id
            rows.add(
                buildTrackRow(
                    personTracks,
                    facesByTrack,
                    if (isUndefinded) "" else (pExt?.person?.name ?: ""),
                    false,
                    if (isUndefinded) null else pExt?.pathToSmall,
                    personId,
                ),
            )
        }
        for (track in singleRows) {
            val faces = facesByTrack[track.id] ?: mutableListOf()
            val personIds = faces.map { it.person.id }.distinct()
            if (personIds.size > 1) {
                rows.add(buildTrackRow(listOf(track), facesByTrack, "разные люди", true, null))
            } else {
                val pExt =
                    if (personIds.isEmpty()) {
                        null
                    } else {
                        listPersonsExtForFile.firstOrNull { it.person.id == personIds[0] }
                    }
                val isUnknown =
                    pExt == null ||
                        pExt.person.id == projectPersonExtUndefinded?.person?.id
                rows.add(
                    buildTrackRow(
                        listOf(track),
                        facesByTrack,
                        if (isUnknown) "" else pExt!!.person.name,
                        false,
                        if (isUnknown) null else pExt!!.pathToSmall,
                    ),
                )
            }
        }

        // Порядок — как у персон: по имени. Безымянные строки при этом
        // оказываются в начале, то есть очередь работы сверху.
        rows.sortWith(Comparator { a, b -> a.nameText.compareTo(b.nameText) })
        rows.forEach { listTracksExt.add(it) }

        val notNamed = rows.count { !it.isNamed && !it.mixed }
        lblTracks?.text = "Строк: ${rows.size}, без персона: $notNamed, " +
            "смешанных: ${rows.count { it.mixed }}"

        showFirstTrackRowOrClear(if (shotChanged) null else keepTrackId)
    }

    /**
     * Показывает треки первого персонажа нового плана.
     *
     * Без этого после смены плана вкладка продолжала показывать лица
     * предыдущего, и это читалось как «треки не пересчитались». Если на плане
     * треков нет, содержимое очищается: лица прошлого плана к новому
     * отношения не имеют, и оставлять их — враньё на экране.
     *
     * Выбор выполняется только при открытой вкладке: пока вкладка закрыта,
     * панель не размечена (ширина 0), раскладка по страницам не считается и
     * показывать всё равно нечего. Очистка же нужна всегда — иначе устаревшее
     * содержимое встретит пользователя, когда он откроет вкладку.
     */
    private var shotExtIdForLog: Long = 0L

    /** План, для которого список строк уже собран. */
    private var lastLoadedShotId: Long = -1L

    private fun showFirstTrackRowOrClear(keepTrackId: Long? = null) {
        clearTrackFaces()
        val tabOpen = tabpaneShotsEdit?.selectionModel?.selectedItem?.text == "Tracks"
        if (!tabOpen) return
        // Возвращаемся на прежнюю строку, если она ещё есть. Списки строк
        // пересобираются целиком при каждом изменении, и без этого любой
        // повторный показ уводил бы наверх — в том числе после установки
        // портрета, где это выглядело как «картинка не обновилась».
        val row =
            if (keepTrackId != null) {
                listTracksExt.firstOrNull { r -> r.tracks.any { it.id == keepTrackId } }
            } else {
                null
                    ?: listTracksExt.firstOrNull()
            }
        if (row == null) {
            println("[ВЫБОР] план=$shotExtIdForLog строк нет, вкладка открыта=$tabOpen")
            return
        }
        val t0 = System.currentTimeMillis()
        tblTracks?.selectionModel?.select(row)
        tblTracks?.scrollTo(listTracksExt.indexOf(row))
        println(
            "[ВЫБОР] план=$shotExtIdForLog строк=${listTracksExt.size} " +
                "выбрана='${row.nameText}' ширинаПанели=${paneTrackFaces?.width} " +
                "за=${System.currentTimeMillis() - t0} мс выделено=${tblTracks?.selectionModel?.selectedItem != null}",
        )
    }

    /**
     * Приём перетаскивания лиц на строку таблицы персон.
     *
     * Строка под курсором ищется по координатам отпускания, а не по
     * наведению: во время переноса наведение на строку таблицы приходит
     * не всегда, и перетаскивание просто не срабатывало. Поиск идёт по
     * реальным границам строк, поэтому работает одинаково и для Persons,
     * и для треков, и не зависит от того, какая строка последней была
     * под мышью до начала переноса.
     *
     * @param table таблица-приёмник
     * @param personOfRow из строки таблицы получаем персону; null — строка
     *        перетаскивание принимает, но назначать некому
     * @param faces выделенные лица той вкладки, откуда тянут
     * @param afterAssign что пересчитать после назначения
     */

    /**
     * Перенос выделенных лиц на строку персоны — на собственной обработке
     * мыши: зажали на лице, повели, отпустили над строкой.
     *
     * Почему не средствами перетаскивания JavaFX: они отдавали картинку, но
     * ни одного события о переносе не доставляли — ни строке таблицы, ни
     * самой таблице, ни сцене. Проверено замерами на всех трёх уровнях.
     *
     * Обычный щелчок при этом не мешается: пока указатель не сдвинулся,
     * переноса нет, и выделение работает как раньше.
     */
    private var dragPressedFace: MatrixFace? = null
    private var dragPressedX = 0.0
    private var dragPressedY = 0.0
    private var dragMoved = false
    private var dragFaces: MutableList<MatrixFace> = mutableListOf()
    private var dragHandled = false
    private var dragScene: Scene? = null

    private fun beginFaceDrag(
        event: MouseEvent,
        matrixFace: MatrixFace,
        selected: MutableCollection<MatrixFace>,
    ) {
        dragPressedFace = matrixFace
        dragPressedX = event.sceneX
        dragPressedY = event.sceneY
        dragMoved = false
        dragScene = event.source?.let { src -> (src as? Node)?.scene }
        dragFaces = selected.toMutableList()
        if (!dragFaces.contains(matrixFace)) dragFaces.add(matrixFace)
        // На всё время схватывания курсор сжатой руки: видно, что перенос
        // начался, даже если указатель ещё не двинулся.
        dragScene?.cursor = Cursor.CLOSED_HAND
    }

    /**
     * Порог в четыре пикселя: столько нужно, чтобы отделить перенос от щелчка.
     *
     * Курсор показывает, есть ли куда отпускать: над строкой персоны рука,
     * над пустым местом — обычная стрелка. Без этого перенос работал, но
     * выглядел так, будто указатель его не признаёт.
     *
     * Констант COPY и NO_DROP у курсора JavaFX нет — проверено javap по
     * javafx-graphics-11.0.2-linux.jar, там только стрелки и руки.
     */
    private fun continueFaceDrag(event: MouseEvent) {
        if (dragPressedFace == null) return
        val dx = event.sceneX - dragPressedX
        val dy = event.sceneY - dragPressedY
        if (dx * dx + dy * dy > 16.0) dragMoved = true
        if (!dragMoved) return
        val scene = dragScene ?: return
        // Курсор держим на всё время переноса: над строкой — рука, в любом
        // другом месте — сжатая рука. Раньше указатель оживал только над
        // строками, и начало переноса не было видно нигде.
        val target = personRowUnderCursor(event.sceneX, event.sceneY)
        scene.cursor =
            if (target != null && dragFaces.isNotEmpty()) {
                Cursor.HAND
            } else {
                Cursor.CLOSED_HAND
            }
    }

    /**
     * Возвращает true, если перенос состоялся и событие нужно съесть:
     * иначе после отпускания сработал бы ещё и щелчок.
     */
    private fun finishFaceDrag(event: MouseEvent): Boolean {
        val started = dragPressedFace != null
        val moved = dragMoved
        val faces = dragFaces
        dragPressedFace = null
        dragFaces = mutableListOf()
        dragScene?.cursor = Cursor.DEFAULT
        dragScene = null
        if (!started || !moved || faces.isEmpty()) return false

        val target = personRowUnderCursor(event.sceneX, event.sceneY)
        if (target == null) return true

        assignFacesToPerson(target, faces)
        reloadTracks(currentShotExt)
        reloadPersonsForShot()
        dragHandled = true
        return true
    }

    /** То же, что finishFaceDrag, но для вкладки Persons. */
    private fun finishPersonsFaceDrag(event: MouseEvent): Boolean {
        val started = dragPressedFace != null
        val moved = dragMoved
        val faces = dragFaces
        dragPressedFace = null
        dragFaces = mutableListOf()
        dragScene?.cursor = Cursor.DEFAULT
        dragScene = null
        if (!started || !moved || faces.isEmpty()) return false
        val target = personRowUnderCursor(event.sceneX, event.sceneY) ?: return true
        assignFacesToPerson(target, faces)
        reorganizeMatrixFaces()
        reloadPersonsForShot()
        dragHandled = true
        return true
    }

    /**
     * Персона массовки из списка файла, при необходимости добавленная в него.
     */
    private fun extrasPersonExt(): PersonExt {
        val existing = listPersonsExtForFile.firstOrNull { it.person.personType == PersonType.EXTRAS }
        if (existing != null) return existing
        val extras =
            projectPersonExtExtras
                ?: throw IllegalStateException("Персона массовки не найдена для проекта")
        listPersonsExtForFile.add(extras)
        listPersonsExtForFile.sort()
        return extras
    }

    /**
     * Перевод одного лица в массовку. Возвращает true, если лицо переведено,
     * и false, если оно уже массовка.
     *
     * Общее ядро для пункта меню EXTRAS и для кнопки «All to EXTRAS»: оба
     * делают ровно одно и то же, различается только набор лиц.
     */
    private fun markFaceAsExtras(
        faceExt: FaceExt,
        extras: PersonExt,
    ): Boolean {
        if (faceExt.personExt.person.personType == PersonType.EXTRAS) return false
        faceExt.personExt = extras
        faceExt.face.person = extras.person
        faceExt.face.personRecognizedName = ""
        FaceController.save(faceExt.face)
        listFacesExt.remove(faceExt)
        return true
    }

    /**
     * Кнопка «All to EXTRAS»: все лица выбранного трека становятся массовкой —
     * то же, что выбрать их все и вызвать в меню EXTRAS.
     */
    @FXML
    fun onAllToExtras() {
        Trace.action("onAllToExtras")
        val trackExt = trackExtToShow
        if (trackExt == null) {
            println("[EXTRAS] трек не выбран")
            return
        }
        val extras = extrasPersonExt()
        // facesExtByTrack разложен по страницам, поэтому в один список.
        val all = trackExt.facesExtByTrack.flatten()
        var changed = 0
        for (fe in all) if (markFaceAsExtras(fe, extras)) changed++

        // Трек целиком стал массовочным — пересчитываем его персону, иначе в
        // строке останется прежний человек при уже массовочных лицах.
        val track = trackExt.tracks.firstOrNull()
        if (track != null) {
            track.person = extras.person
            Main.faceTrackRepo.save(track)
        }
        println("[EXTRAS] трек ${track?.id}: в массовку переведено $changed из ${all.size}")
        // Обновление то же, что у пункта меню в треках: пересобрать строки
        // треков и список персон плана.
        reloadTracks(currentShotExt)
        reloadPersonsForShot()
    }

    /** True, если только что был обработан перенос: щелчок после него лишний. */
    private fun consumeDragHandled(): Boolean {
        if (!dragHandled) return false
        dragHandled = false
        return true
    }

    /**
     * Персона, строка которой оказалась под указателем: строки треков,
     * строки персон файла и строки персон плана.
     */
    private fun personRowUnderCursor(
        sceneX: Double,
        sceneY: Double,
    ): PersonExt? {
        val tbl = tblTracks
        if (tbl != null && containsScenePoint(tbl, sceneX, sceneY)) {
            val row = rowUnder(sceneX, sceneY, tbl) ?: return null
            val item = row.item as? FaceTrackExt ?: return null
            if (item.mixed || item.personId == 0L) return null
            return listPersonsExtForFile.firstOrNull { it.person.id == item.personId }
        }
        for (t in listOfNotNull(tblPersonsAllForShot, tblPersonsAllForFile)) {
            if (!containsScenePoint(t, sceneX, sceneY)) continue
            val row = rowUnder(sceneX, sceneY, t) ?: return null
            return row.item as? PersonExt
        }
        return null
    }

    private fun containsScenePoint(
        table: TableView<*>,
        sceneX: Double,
        sceneY: Double,
    ): Boolean {
        val p = table.localToScene(table.boundsInLocal)
        return sceneX >= p.minX && sceneX <= p.maxX && sceneY >= p.minY && sceneY <= p.maxY
    }

    private fun rowUnder(
        sceneX: Double,
        sceneY: Double,
        table: TableView<*>,
    ): TableRow<*>? {
        val local = table.sceneToLocal(sceneX, sceneY)
        return table
            .lookupAll(".table-row-cell")
            .filterIsInstance<TableRow<*>>()
            .firstOrNull { it.boundsInParent.contains(local.x, local.y) }
    }

    private fun <T> setupFaceDropOnRows(
        table: TableView<T>,
        personOfRow: (T) -> PersonExt?,
        faces: () -> MutableCollection<MatrixFace>,
        afterAssign: () -> Unit,
    ) {
        // Обработчики вешаются на САМИ СТРОКИ, а не на таблицу. События
        // перетаскивания адресуются внутренним узлам таблицы, и до обработчика
        // таблицы они не доходили: курсор над строкой оставался обычным,
        // отпускание ничего не делало. Строка же знает и свой предмет, и
        // координаты не нужны — цель известна из самой строки.
        table.setRowFactory {
            val row: TableRow<T> = TableRow()

            row.onDragOver =
                EventHandler { mouseEvent ->
                    // Принимаем безусловно: на этом шаге переноса данные ещё не
                    // согласованы, и любая проверка содержимого даёт «не наше».
                    mouseEvent.acceptTransferModes(TransferMode.COPY)
                }

            row.onDragDropped =
                EventHandler { mouseEvent ->
                    var success = false
                    val item = row.item
                    val person = if (item != null) personOfRow(item) else null
                    val selected = faces()
                    if (person != null && selected.isNotEmpty()) {
                        assignFacesToPerson(person, selected)
                        afterAssign()
                        success = true
                    }
                    mouseEvent.isDropCompleted = success
                    mouseEvent.consume()
                }
            row
        }
    }

    /**
     * Назначает перетащенные лица персонажу — ровно то же, что делает пункт
     * «SELECT PERSON» в контекстном меню: та же запись в базу, то же имя для
     * распознавателя, то же обновление экрана. Иначе перетаскивание и меню
     * расходились бы в деталях, и распознавание потом считало бы не так.
     *
     * Список лиц передаётся вызывающим: во вкладке Persons это выделенные
     * лица её сетки, в Tracks — выделенные лица сетки треков.
     */
    private fun assignFacesToPerson(
        personExt: PersonExt,
        faces: MutableCollection<MatrixFace>,
    ) {
        if (!(listPersonsExtForFile.any { it.person == personExt.person })) {
            listPersonsExtForFile.add(personExt)
            listPersonsExtForFile.sort()
        }
        for (mf in faces) {
            val faceExt = mf.faceExt ?: continue
            faceExt.personExt = personExt
            faceExt.face.person = personExt.person
            faceExt.face.personRecognizedName =
                if (personExt.person.personType == PersonType.UNDEFINDED) {
                    ""
                } else {
                    personExt.person.nameInRecognizer
                }
            FaceController.save(faceExt.face)
            listFacesExt.remove(faceExt)
            faceExt.labelSmall.graphic = null
            faceExt.labelSmall.style = fxBorderDefault
            currentMatrixPageFaces?.matrixFaces?.remove(mf)
        }
        faces.clear()
    }

    /**
     * Пересобирает список персон монтажного плана.
     *
     * Список кэшируется в listPersonsExtForShot и наполняется при выборе
     * плана. Всё, что меняет персону у лица в обход вкладки Persons — а это
     * пункты контекстного меню в треках — обязано вызывать и это: иначе
     * новая персонажа появляется в треках, но в списке плана её нет, и
     * две вкладки показывают разное.
     */
    private fun reloadPersonsForShot() {
        val shotExt = currentShotExt ?: return
        listPersonsExtForShot =
            FXCollections.observableList(
                shotExt.personsExt.filter { it.person.personType != PersonType.NONPERSON },
            )
        tblPersonsAllForShot?.items = listPersonsExtForShot
    }

    /**
     * Сбрасывает всё, что показывает треки: выделенные трек и лица, лица на
     * панели, страницы и подпись.
     */
    private fun clearTrackFaces() {
        trackExtToShow = null
        currentMatrixPageTrackFaces = null
        selectedTrackFaces.clear()
        paneTrackFaces?.children?.clear()
        listMatrixPageTrackFaces.clear()
        tblTrackPages?.items = FXCollections.observableArrayList()
        lblTracks?.text = ""
    }

    /**
     * Собирает строку списка из одного или нескольких треков.
     *
     * Лица берутся из готовой карты по трекам: обращаться к базе отсюда нельзя,
     * здесь всё уже прочитано — иначе на плане с десятками треков список
     * собирался бы десятки секунд.
     */
    private fun buildTrackRow(
        rowTracks: List<FaceTrack>,
        facesByTrack: Map<Long, List<Face>>,
        name: String,
        isMixed: Boolean,
        personPreview: String?,
        personId: Long = 0L,
    ): FaceTrackExt {
        val faces = mutableListOf<Face>()
        for (t in rowTracks) faces.addAll(facesByTrack[t.id] ?: emptyList())
        val ext = FaceTrackExt(rowTracks, faces, name, isMixed, personPreview, personId)
        ext.isNamed = name.isNotEmpty() && !isMixed
        for (t in rowTracks) {
            val trackFaces = mutableListOf<FaceExt>()
            for (face in facesByTrack[t.id] ?: emptyList()) {
                var pExt = listPersonsExtForFile.firstOrNull { it.person.id == face.person.id }
                if (pExt == null) pExt = projectPersonExtUndefinded
                if (pExt != null) {
                    // Подменяем отложенные связи конкретными объектами: в
                    // конструкторе FaceExt читается face.file.shortName, а
                    // формы сессии Hibernate не имеют.
                    face.file = currentFileExt!!.file
                    face.person = pExt.person
                    val fExt = FaceExt(face, currentFileExt!!, pExt)
                    trackFaces.add(fExt)
                    ext.facesExtForGrid.add(fExt)
                }
            }
            ext.facesExtByTrack.add(trackFaces)
        }
        return ext
    }

    /** Собирает строку списка из одного или нескольких треков. */

    @FXML
    fun doCreateNewSceneBySelectedShots(event: ActionEvent?) {
        Trace.action("doCreateNewSceneBySelectedShots")

        val selectedShots = tblShots!!.selectionModel.selectedItems
        if (selectedShots.isNotEmpty()) {
            val selectedShotsExtSorted = selectedShots.toMutableList()
            selectedShotsExtSorted.sort()
            val listShotsExt: MutableList<ShotExt> = mutableListOf()
            for ((i, shotExt) in selectedShotsExtSorted.withIndex()) {
                if (i < selectedShotsExtSorted.size - 1) {
                    if (shotExt.shot.lastFrameNumber + 1 == selectedShotsExtSorted[i + 1].shot.firstFrameNumber) {
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
                    val dialog = TextInputDialog(sceneExt.sceneName)
                    dialog.title = "Rename scene"
                    dialog.headerText = "Enter new scene name:"
                    dialog.contentText = "Name:"
                    val result: Optional<String> = dialog.showAndWait()
                    result.ifPresent { name ->
                        sceneExt.scene.name = name
                        SceneController.save(sceneExt.scene)
                    }

                    LoadListScenesExt(currentFileExt!!.scenesExt, currentFileExt!!, pb, lblPb).run()
                    tblScenes!!.items = currentFileExt!!.scenesExt
                    val sceneInTable = currentFileExt!!.scenesExt.firstOrNull { it.scene.id == sceneExt.scene.id }
                    currentFileExt!!
                        .shotsExt
                        .filter { shotExt ->
                            isPairIntersected(
                                Pair(shotExt.shot.firstFrameNumber, shotExt.shot.lastFrameNumber),
                                Pair(
                                    sceneExt.scene.firstFrameNumber - 1,
                                    sceneExt.scene.lastFrameNumber + 1,
                                ),
                            )
                        }.forEach {
                            it.resetPreview()
                            it.labelsFirst
                            it.labelsLast
                        }

                    if (sceneInTable != null) tblScenes!!.selectionModel.select(sceneInTable)

                    tblShots!!.refresh()
                }
            }
        }
    }

    @FXML
    fun doCreateNewEventBySelectedShots(event: ActionEvent?) {
        Trace.action("doCreateNewEventBySelectedShots")

        val selectedShots = tblShots!!.selectionModel.selectedItems
        if (selectedShots.isNotEmpty()) {
            val selectedShotsExtSorted = selectedShots.toMutableList()
            selectedShotsExtSorted.sort()
            val listShotsExt: MutableList<ShotExt> = mutableListOf()
            for ((i, shotExt) in selectedShotsExtSorted.withIndex()) {
                if (i < selectedShotsExtSorted.size - 1) {
                    if (shotExt.shot.lastFrameNumber + 1 == selectedShotsExtSorted[i + 1].shot.firstFrameNumber) {
                        listShotsExt.add(shotExt)
                    } else {
                        break
                    }
                } else {
                    listShotsExt.add(shotExt)
                }
            }
            if (listShotsExt.isNotEmpty()) {
                val eventExt = EventController.createEventExt(listShotsExt)
                if (eventExt != null) {
                    val dialog = TextInputDialog(eventExt.eventName)
                    dialog.title = "Rename event"
                    dialog.headerText = "Enter new event name:"
                    dialog.contentText = "Name:"
                    val result: Optional<String> = dialog.showAndWait()
                    result.ifPresent { name ->
                        eventExt.event.name = name
                        EventController.save(eventExt.event)
                    }

                    LoadListEventsExt(currentFileExt!!.eventsExt, currentFileExt!!, pb, lblPb).run()
                    tblEvents!!.items = currentFileExt!!.eventsExt
                    val eventInTable = currentFileExt!!.eventsExt.firstOrNull { it.event.id == eventExt.event.id }
                    currentFileExt!!
                        .shotsExt
                        .filter { shotExt ->
                            isPairIntersected(
                                Pair(shotExt.shot.firstFrameNumber, shotExt.shot.lastFrameNumber),
                                Pair(
                                    eventExt.event.firstFrameNumber - 1,
                                    eventExt.event.lastFrameNumber + 1,
                                ),
                            )
                        }.forEach {
                            it.resetPreview()
                            it.labelsFirst
                            it.labelsLast
                        }

                    if (eventInTable != null) tblEvents!!.selectionModel.select(eventInTable)

                    tblShots!!.refresh()
                }
            }
        }
    }

    @FXML
    fun doDeleteSelectedScenes(event: ActionEvent?) {
        Trace.action("doDeleteSelectedScenes")
    }

    @FXML
    fun doDeleteSelectedEvents(event: ActionEvent?) {
        Trace.action("doDeleteSelectedEvents")

        val setShotExtToUpdate: MutableSet<ShotExt> = mutableSetOf()
        tblEvents!!.selectionModel.selectedItems.forEach { eventExt ->
            val ffm = eventExt.event.firstFrameNumber - 1
            val lfm = eventExt.event.lastFrameNumber + 1
            setShotExtToUpdate.addAll(
                currentFileExt!!
                    .shotsExt
                    .filter { shotExt ->
                        isPairIntersected(Pair(shotExt.shot.firstFrameNumber, shotExt.shot.lastFrameNumber), Pair(ffm, lfm))
                    },
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

    fun isPairIntersected(
        firstPair: Pair<Int, Int>,
        secondPair: Pair<Int, Int>,
    ): Boolean = (min(firstPair.second, secondPair.second) - max(firstPair.first, secondPair.first)) >= 0

    /**
     * Освобождает память серии при закрытии окна.
     *
     * Окно открывается на одну серию за раз, но держит её очень много:
     * FileExt с 82336 кадрами, у каждого кэш превью в BufferedImage, плюс
     * сцена со всеми таблицами. `currentFileExt` — **статическое** поле,
     * то есть живёт, пока жив класс, и без сброса прошлая серия продолжает
     * занимать память при переходе к следующей.
     *
     * Очистка была написана и раньше, но **не вызывалась**: в проекте стояли
     * два `setOnCloseRequest`, и второй перезаписывал первый — обработчик с
     * очисткой был мёртвым кодом. Отсюда рост до 12 ГБ при работе с
     * несколькими сериями подряд.
     */
    fun clearOnExit() {
        Trace.action("освобождение началось, кадров: ${currentFileExt?.framesExt?.size ?: 0}")
        currentFileExt?.let { file ->
            // Превью — самая тяжёлая часть, BufferedImage на каждый кадр.
            // Сбрасываем до отпускания списков: иначе сборщик мусора может
            // не справиться за один проход.
            file.framesExt.forEach {
                it.resetPreviewSmall()
                it.resetPreviewMedium()
                it.resetPreviewFull()
            }
            file.shotsExt.clear()
            file.scenesExt.clear()
            file.eventsExt.clear()
            file.facesExt.clear()
            file.framesExt.clear()
        }
        currentFileExt = null
        listMatrixPageFrames.clear()
        listFacesExt.clear()
        listTracksExt.clear()
        listShotsExtForScenes.clear()
        listShotsExtForEvents.clear()
        listPersonsExtForFile.clear()
        currentMatrixFrame = null
        currentShotExt = null
        currentShotExtForScene = null
        Trace.done("память серии освобождена")
    }

    @FXML
    fun doEventPropertyAdd(event: ActionEvent?) {
        Trace.action("doEventPropertyAdd")

        if (currentEventExt != null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство события"
            menuItem.onAction =
                EventHandler { e: ActionEvent? ->
                    val alert = Alert(Alert.AlertType.CONFIRMATION)
                    alert.title = "Добавление свойства события"
                    alert.headerText = "Вы действительно хотите добавить новое свойство для события?"
                    alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                    val option = alert.showAndWait()
                    if (option.get() == ButtonType.OK) {
                        saveCurrentEventProperty()
                        val id =
                            PropertyController
                                .editOrCreate(
                                    currentEventExt!!.event::class.java.simpleName,
                                    currentEventExt!!.event.id,
                                ).id
                        listEventProperties =
                            FXCollections.observableArrayList(
                                PropertyController.getListProperties(
                                    currentEventExt!!.event::class.java.simpleName,
                                    currentEventExt!!.event.id,
                                ),
                            )
                        tblEventProperties?.items = listEventProperties
                        currentEventProperty = listEventProperties.first { it.id == id }
                        tblEventProperties?.selectionModel?.select(currentEventProperty)
                    }
                }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val mapKeyValues = PropertyController.getMapKeyValuesByParentClass(Event::class.java.simpleName)
            var countKeysAdded = 0
            mapKeyValues.forEach { (key, value) ->
                val menuGroup = Menu()
                menuGroup.isMnemonicParsing = false
                menuGroup.text = key
                value.forEach { value ->
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = if (value == "") "<пусто>" else value
                    menuItem.onAction =
                        EventHandler {
                            saveCurrentEventProperty()
                            val id =
                                PropertyController
                                    .editOrCreate(
                                        currentEventExt!!.event::class.java.simpleName,
                                        currentEventExt!!.event.id,
                                        key,
                                        value,
                                    ).id
                            listEventProperties =
                                FXCollections.observableArrayList(
                                    PropertyController.getListProperties(
                                        currentEventExt!!.event::class.java.simpleName,
                                        currentEventExt!!.event.id,
                                    ),
                                )
                            tblEventProperties?.items = listEventProperties
                            currentEventProperty = listEventProperties.first { it.id == id }
                            tblEventProperties?.selectionModel?.select(currentEventProperty)
                        }
                    menuGroup.items.add(menuItem)
                }
                menu.items.add(menuGroup)
            }

            btnEventPropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnEventPropertyAdd!!.localToScreen(btnEventPropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.minX + screenBounds.width, screenBounds.minY)
        }
    }

    @FXML
    fun doEventPropertyDelete(event: ActionEvent?) {
        Trace.action("doEventPropertyDelete")

        if (currentEventProperty != null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства события"
            alert.headerText =
                "Вы действительно хотите удалить свойство события с ключом «${currentEventProperty?.key}» и значением «${currentEventProperty?.value}»?"
            alert.contentText =
                "В случае утвердительного ответа свойство события будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство события?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentEventProperty!!)
                currentEventProperty = null
                listEventProperties =
                    FXCollections.observableArrayList(
                        PropertyController.getListProperties(currentEventExt!!.event::class.java.simpleName, currentEventExt!!.event.id),
                    )
                tblEventProperties?.items = listEventProperties

                btnEventPropertyMoveToFirst?.isDisable = currentEventProperty == null
                btnEventPropertyMoveUp?.isDisable = currentEventProperty == null
                btnEventPropertyMoveToLast?.isDisable = currentEventProperty == null
                btnEventPropertyMoveDown?.isDisable = currentEventProperty == null
                btnEventPropertyDelete?.isDisable = currentEventProperty == null
                fldEventPropertyKey?.isDisable = currentEventProperty == null
                fldEventPropertyValue?.isDisable = currentEventProperty == null

                fldEventPropertyKey?.text = ""
                fldEventPropertyValue?.text = ""
            }
        }
    }

    @FXML
    fun doEventPropertyMoveDown(event: ActionEvent?) {
        Trace.action("doEventPropertyMoveDown")
        doMoveEventProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doEventPropertyMoveToFirst(event: ActionEvent?) {
        Trace.action("doEventPropertyMoveToFirst")
        doMoveEventProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doEventPropertyMoveToLast(event: ActionEvent?) {
        Trace.action("doEventPropertyMoveToLast")
        doMoveEventProperty(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doEventPropertyMoveUp(event: ActionEvent?) {
        Trace.action("doEventPropertyMoveUp")
        doMoveEventProperty(ReorderTypes.MOVE_UP)
    }

    @FXML
    fun doScenePropertyAdd(event: ActionEvent?) {
        Trace.action("doScenePropertyAdd")

        if (currentSceneExt != null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство сцены"
            menuItem.onAction =
                EventHandler { e: ActionEvent? ->
                    val alert = Alert(Alert.AlertType.CONFIRMATION)
                    alert.title = "Добавление свойства сцены"
                    alert.headerText = "Вы действительно хотите добавить новое свойство для сцены?"
                    alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                    val option = alert.showAndWait()
                    if (option.get() == ButtonType.OK) {
                        saveCurrentSceneProperty()
                        val id =
                            PropertyController
                                .editOrCreate(
                                    currentSceneExt!!.scene::class.java.simpleName,
                                    currentSceneExt!!.scene.id,
                                ).id
                        listSceneProperties =
                            FXCollections.observableArrayList(
                                PropertyController.getListProperties(
                                    currentSceneExt!!.scene::class.java.simpleName,
                                    currentSceneExt!!.scene.id,
                                ),
                            )
                        tblSceneProperties?.items = listSceneProperties
                        currentSceneProperty = listSceneProperties.first { it.id == id }
                        tblSceneProperties?.selectionModel?.select(currentSceneProperty)
                    }
                }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val mapKeyValues = PropertyController.getMapKeyValuesByParentClass(com.svoemesto.ivfx.models.Scene::class.java.simpleName)
            var countKeysAdded = 0
            mapKeyValues.forEach { (key, value) ->
                val menuGroup = Menu()
                menuGroup.isMnemonicParsing = false
                menuGroup.text = key
                value.forEach { value ->
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = if (value == "") "<пусто>" else value
                    menuItem.onAction =
                        EventHandler {
                            saveCurrentSceneProperty()
                            val id =
                                PropertyController
                                    .editOrCreate(
                                        currentSceneExt!!.scene::class.java.simpleName,
                                        currentSceneExt!!.scene.id,
                                        key,
                                        value,
                                    ).id
                            listSceneProperties =
                                FXCollections.observableArrayList(
                                    PropertyController.getListProperties(
                                        currentSceneExt!!.scene::class.java.simpleName,
                                        currentSceneExt!!.scene.id,
                                    ),
                                )
                            tblSceneProperties?.items = listSceneProperties
                            currentSceneProperty = listSceneProperties.first { it.id == id }
                            tblSceneProperties?.selectionModel?.select(currentSceneProperty)
                        }
                    menuGroup.items.add(menuItem)
                }
                menu.items.add(menuGroup)
            }

            btnScenePropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnScenePropertyAdd!!.localToScreen(btnScenePropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.minX + screenBounds.width, screenBounds.minY)
        }
    }

    @FXML
    fun doScenePropertyDelete(event: ActionEvent?) {
        Trace.action("doScenePropertyDelete")

        if (currentSceneProperty != null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства сцены"
            alert.headerText =
                "Вы действительно хотите удалить свойство сцены с ключом «${currentSceneProperty?.key}» и значением «${currentSceneProperty?.value}»?"
            alert.contentText =
                "В случае утвердительного ответа свойство сцены будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство сцены?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentSceneProperty!!)
                currentSceneProperty = null
                listSceneProperties =
                    FXCollections.observableArrayList(
                        PropertyController.getListProperties(currentSceneExt!!.scene::class.java.simpleName, currentSceneExt!!.scene.id),
                    )
                tblSceneProperties?.items = listSceneProperties

                btnScenePropertyMoveToFirst?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveUp?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveToLast?.isDisable = currentSceneProperty == null
                btnScenePropertyMoveDown?.isDisable = currentSceneProperty == null
                btnScenePropertyDelete?.isDisable = currentSceneProperty == null
                fldScenePropertyKey?.isDisable = currentSceneProperty == null
                fldScenePropertyValue?.isDisable = currentSceneProperty == null

                fldScenePropertyKey?.text = ""
                fldScenePropertyValue?.text = ""
            }
        }
    }

    @FXML
    fun doScenePropertyMoveDown(event: ActionEvent?) {
        Trace.action("doScenePropertyMoveDown")
        doMoveSceneProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doScenePropertyMoveToFirst(event: ActionEvent?) {
        Trace.action("doScenePropertyMoveToFirst")
        doMoveSceneProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doScenePropertyMoveToLast(event: ActionEvent?) {
        Trace.action("doScenePropertyMoveToLast")
        doMoveSceneProperty(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doScenePropertyMoveUp(event: ActionEvent?) {
        Trace.action("doScenePropertyMoveUp")
        doMoveSceneProperty(ReorderTypes.MOVE_UP)
    }

    @FXML
    fun doShotPropertyAdd(event: ActionEvent?) {
        Trace.action("doShotPropertyAdd")

        if (currentShotExt != null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство плана"
            menuItem.onAction =
                EventHandler { e: ActionEvent? ->
                    val alert = Alert(Alert.AlertType.CONFIRMATION)
                    alert.title = "Добавление свойства плана"
                    alert.headerText = "Вы действительно хотите добавить новое свойство для плана?"
                    alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                    val option = alert.showAndWait()
                    if (option.get() == ButtonType.OK) {
                        saveCurrentShotProperty()
                        val id = PropertyController.editOrCreate(currentShotExt!!.shot::class.java.simpleName, currentShotExt!!.shot.id).id
                        listShotProperties =
                            FXCollections.observableArrayList(
                                PropertyController.getListProperties(
                                    currentShotExt!!.shot::class.java.simpleName,
                                    currentShotExt!!.shot.id,
                                ),
                            )
                        tblShotProperties?.items = listShotProperties
                        currentShotProperty = listShotProperties.first { it.id == id }
                        tblShotProperties?.selectionModel?.select(currentShotProperty)
                    }
                }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val mapKeyValues = PropertyController.getMapKeyValuesByParentClass(Shot::class.java.simpleName)
            var countKeysAdded = 0
            mapKeyValues.forEach { (key, value) ->
                val menuGroup = Menu()
                menuGroup.isMnemonicParsing = false
                menuGroup.text = key
                value.forEach { propValue ->
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = if (propValue == "") "<пусто>" else propValue
                    menuItem.onAction =
                        EventHandler {
                            saveCurrentShotProperty()
                            val id =
                                PropertyController
                                    .editOrCreate(
                                        currentShotExt!!.shot::class.java.simpleName,
                                        currentShotExt!!.shot.id,
                                        key,
                                        propValue,
                                    ).id
                            listShotProperties =
                                FXCollections.observableArrayList(
                                    PropertyController.getListProperties(
                                        currentShotExt!!.shot::class.java.simpleName,
                                        currentShotExt!!.shot.id,
                                    ),
                                )
                            tblShotProperties?.items = listShotProperties
                            currentShotProperty = listShotProperties.first { it.id == id }
                            tblShotProperties?.selectionModel?.select(currentShotProperty)
                        }
                    menuGroup.items.add(menuItem)
                }
                menu.items.add(menuGroup)
            }

            btnShotPropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnShotPropertyAdd!!.localToScreen(btnShotPropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.minX + screenBounds.width, screenBounds.minY)
        }
    }

    @FXML
    fun doShotPropertyDelete(event: ActionEvent?) {
        Trace.action("doShotPropertyDelete")

        if (currentShotProperty != null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства плана"
            alert.headerText =
                "Вы действительно хотите удалить свойство плана с ключом «${currentShotProperty?.key}» и значением «${currentShotProperty?.value}»?"
            alert.contentText =
                "В случае утвердительного ответа свойство плана будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство плана?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentShotProperty!!)
                currentShotProperty = null
                listShotProperties =
                    FXCollections.observableArrayList(
                        PropertyController.getListProperties(currentShotExt!!.shot::class.java.simpleName, currentShotExt!!.shot.id),
                    )
                tblShotProperties?.items = listShotProperties

                btnShotPropertyMoveToFirst?.isDisable = currentShotProperty == null
                btnShotPropertyMoveUp?.isDisable = currentShotProperty == null
                btnShotPropertyMoveToLast?.isDisable = currentShotProperty == null
                btnShotPropertyMoveDown?.isDisable = currentShotProperty == null
                btnShotPropertyDelete?.isDisable = currentShotProperty == null
                fldShotPropertyKey?.isDisable = currentShotProperty == null
                fldShotPropertyValue?.isDisable = currentShotProperty == null

                fldShotPropertyKey?.text = ""
                fldShotPropertyValue?.text = ""
            }
        }
    }

    @FXML
    fun doShotPropertyMoveDown(event: ActionEvent?) {
        Trace.action("doShotPropertyMoveDown")
        doMoveShotProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doShotPropertyMoveToFirst(event: ActionEvent?) {
        Trace.action("doShotPropertyMoveToFirst")
        doMoveShotProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doShotPropertyMoveToLast(event: ActionEvent?) {
        Trace.action("doShotPropertyMoveToLast")
        doMoveShotProperty(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doShotPropertyMoveUp(event: ActionEvent?) {
        Trace.action("doShotPropertyMoveUp")
        doMoveShotProperty(ReorderTypes.MOVE_UP)
    }

    fun doMoveShotProperty(reorderType: ReorderTypes) {
        val id = currentShotProperty?.id
        currentShotProperty?.let { PropertyController.reOrder(reorderType, it) }
        listShotProperties =
            FXCollections.observableArrayList(
                PropertyController.getListProperties(currentShotExt!!.shot::class.java.simpleName, currentShotExt!!.shot.id),
            )
        tblShotProperties?.items = listShotProperties
        currentShotProperty = listShotProperties.first { it.id == id }
        tblShotProperties?.selectionModel?.select(currentShotProperty)
    }

    fun doMoveSceneProperty(reorderType: ReorderTypes) {
        val id = currentSceneProperty?.id
        currentSceneProperty?.let { PropertyController.reOrder(reorderType, it) }
        listSceneProperties =
            FXCollections.observableArrayList(
                PropertyController.getListProperties(currentSceneExt!!.scene::class.java.simpleName, currentSceneExt!!.scene.id),
            )
        tblSceneProperties?.items = listSceneProperties
        currentSceneProperty = listSceneProperties.first { it.id == id }
        tblSceneProperties?.selectionModel?.select(currentSceneProperty)
    }

    fun doMoveEventProperty(reorderType: ReorderTypes) {
        val id = currentEventProperty?.id
        currentEventProperty?.let { PropertyController.reOrder(reorderType, it) }
        listEventProperties =
            FXCollections.observableArrayList(
                PropertyController.getListProperties(currentEventExt!!.event::class.java.simpleName, currentEventExt!!.event.id),
            )
        tblEventProperties?.items = listEventProperties
        currentEventProperty = listEventProperties.first { it.id == id }
        tblEventProperties?.selectionModel?.select(currentEventProperty)
    }

    @FXML
    fun doCreateEventBasedScene(event: ActionEvent?) {
        Trace.action("doCreateEventBasedScene")

        if (currentSceneExt != null) {
            val eventExt = EventController.createEventExt(currentSceneExt!!)
            if (eventExt != null) {
                LoadListEventsExt(currentFileExt!!.eventsExt, currentFileExt!!, pb, lblPb).run()
                tblEvents!!.items = currentFileExt!!.eventsExt
                val eventInTable = currentFileExt!!.eventsExt.firstOrNull { it.event.id == eventExt.event.id }
                currentFileExt!!
                    .shotsExt
                    .filter { shotExt ->
                        isPairIntersected(
                            Pair(shotExt.shot.firstFrameNumber, shotExt.shot.lastFrameNumber),
                            Pair(
                                eventExt.event.firstFrameNumber - 1,
                                eventExt.event.lastFrameNumber + 1,
                            ),
                        )
                    }.forEach {
                        it.resetPreview()
                        it.labelsFirst
                        it.labelsLast
                    }

                if (eventInTable != null) tblEvents!!.selectionModel.select(eventInTable)

                tblShots!!.refresh()
            }
        }
    }
}
