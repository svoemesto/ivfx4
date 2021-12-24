package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.H2DB_PROPERTYKEY_CURRENTDB_ID
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.ProjectCdfController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.PropertyCdfController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.enums.AudioCodecs
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.LosslessContainers
import com.svoemesto.ivfx.enums.LosslessVideoCodecs
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.VideoCodecs
import com.svoemesto.ivfx.enums.VideoContainers
import com.svoemesto.ivfx.getCurrentDatabase
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.PropertyCdf
import com.svoemesto.ivfx.models.Track
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.setPropertyValue
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListFilesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.threads.updatelists.UpdateListFilesExt
import com.svoemesto.ivfx.threads.updatelists.UpdateListFramesExt
import javafx.application.HostServices
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Bounds
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ProgressBar
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.File as IOFile


@Transactional
class ProjectEditFXController {

    // MENU

    @FXML
    private var menuProject: Menu? = null

    @FXML
    private var menuNewProject: MenuItem? = null

    @FXML
    private var menuOpenProject: MenuItem? = null

    @FXML
    private var menuDeleteProject: MenuItem? = null

    @FXML
    private var menuExit: MenuItem? = null

    @FXML
    private var menuActions: Menu? = null

    @FXML
    private var menuProjectActions: MenuItem? = null

    @FXML
    private var menuEditShots: MenuItem? = null

    @FXML
    private var menuDatabase: Menu? = null

    @FXML
    private var menuSelectDatabase: MenuItem? = null

    // PROJECT

    @FXML
    private var paneMain: AnchorPane? = null

    @FXML
    private var fldProjectName: TextField? = null

    @FXML
    private var fldProjectShortName: TextField? = null

    @FXML
    private var fldProjectFolder: TextField? = null

    @FXML
    private var btnSelectProjectFolder: Button? = null

    @FXML
    private var fldProjectWidth: TextField? = null

    @FXML
    private var fldProjectHeight: TextField? = null

    @FXML
    private var fldProjectFps: TextField? = null

    @FXML
    private var fldProjectVideoBitrate: TextField? = null

    @FXML
    private var cbProjectVideoCodec: ComboBox<String>? = null

    @FXML
    private var fldProjectAudioBitrate: TextField? = null

    @FXML
    private var fldProjectAudioFrequency: TextField? = null

    @FXML
    private var cbProjectAudioCodec: ComboBox<String>? = null

    @FXML
    private var cbProjectContainer: ComboBox<String>? = null

    @FXML
    private var cbProjectLosslessCodec: ComboBox<String>? = null

    @FXML
    private var cbProjectLosslessContainer: ComboBox<String>? = null

    // PROJECT FILES

    @FXML
    private var tblFiles: TableView<FileExt>? = null

    @FXML
    private var colFileOrder: TableColumn<FileExt, Int>? = null

    @FXML
    private var colFileName: TableColumn<FileExt, String>? = null

    @FXML
    private var btnFileMoveToFirst: Button? = null

    @FXML
    private var btnFileMoveUp: Button? = null

    @FXML
    private var btnFileMoveDown: Button? = null

    @FXML
    private var btnFileMoveToLast: Button? = null

    @FXML
    private var btnFileAdd: Button? = null

    @FXML
    private var btnFileAddFilesFromFolder: Button? = null

    @FXML
    private var btnFileDelete: Button? = null

    @FXML
    private var pbFiles: ProgressBar? = null

    @FXML
    private var lblPbFiles: Label? = null

    // FILE

    @FXML
    private var paneFile: AnchorPane? = null

    @FXML
    private var fldFileName: TextField? = null

    @FXML
    private var fldFileShortName: TextField? = null

    @FXML
    private var fldFilePath: TextField? = null

    @FXML
    private var btnSelectFilePath: Button? = null

    // PROJECT PROPERTIES

    @FXML
    private var tblProjectProperties: TableView<Property>? = null

    @FXML
    private var colProjectPropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colProjectPropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnProjectPropertyMoveToFirst: Button? = null

    @FXML
    private var btnProjectPropertyMoveUp: Button? = null

    @FXML
    private var btnProjectPropertyMoveDown: Button? = null

    @FXML
    private var btnProjectPropertyMoveToLast: Button? = null

    @FXML
    private var btnProjectPropertyAdd: Button? = null

    @FXML
    private var btnProjectPropertyDelete: Button? = null

    @FXML
    private var fldProjectPropertyKey: TextField? = null

    @FXML
    private var fldProjectPropertyValue: TextArea? = null
    
    // PROJECT CDF PROPERTIES

    @FXML
    private var tblProjectPropertiesCdf: TableView<PropertyCdf>? = null

    @FXML
    private var colProjectPropertyCdfKey: TableColumn<PropertyCdf, String>? = null

    @FXML
    private var colProjectPropertyCdfValue: TableColumn<PropertyCdf, String>? = null

    @FXML
    private var btnProjectPropertyCdfMoveToFirst: Button? = null

    @FXML
    private var btnProjectPropertyCdfMoveUp: Button? = null

    @FXML
    private var btnProjectPropertyCdfMoveDown: Button? = null

    @FXML
    private var btnProjectPropertyCdfMoveToLast: Button? = null

    @FXML
    private var btnProjectPropertyCdfAdd: Button? = null

    @FXML
    private var btnProjectPropertyCdfDelete: Button? = null

    @FXML
    private var fldProjectPropertyCdfKey: TextField? = null

    @FXML
    private var fldProjectPropertyCdfValue: TextArea? = null

    @FXML
    private var btnBrowseProjectPropertyCdfValue: Button? = null

    
    // FILE PROPERTIES

    @FXML
    private var tblFileProperties: TableView<Property>? = null

    @FXML
    private var colFilePropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colFilePropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnFilePropertyMoveToFirst: Button? = null

    @FXML
    private var btnFilePropertyMoveUp: Button? = null

    @FXML
    private var btnFilePropertyMoveDown: Button? = null

    @FXML
    private var btnFilePropertyMoveToLast: Button? = null

    @FXML
    private var btnFilePropertyAdd: Button? = null

    @FXML
    private var btnFilePropertyDelete: Button? = null

    @FXML
    private var fldFilePropertyKey: TextField? = null

    @FXML
    private var fldFilePropertyValue: TextArea? = null

    // FILE CDF PROPERTIES

    @FXML
    private var tblFilePropertiesCdf: TableView<PropertyCdf>? = null

    @FXML
    private var colFilePropertyCdfKey: TableColumn<PropertyCdf, String>? = null

    @FXML
    private var colFilePropertyCdfValue: TableColumn<PropertyCdf, String>? = null

    @FXML
    private var btnFilePropertyCdfMoveToFirst: Button? = null

    @FXML
    private var btnFilePropertyCdfMoveUp: Button? = null

    @FXML
    private var btnFilePropertyCdfMoveDown: Button? = null

    @FXML
    private var btnFilePropertyCdfMoveToLast: Button? = null

    @FXML
    private var btnFilePropertyCdfAdd: Button? = null

    @FXML
    private var btnFilePropertyCdfDelete: Button? = null

    @FXML
    private var fldFilePropertyCdfKey: TextField? = null

    @FXML
    private var fldFilePropertyCdfValue: TextArea? = null

    @FXML
    private var btnBrowseFilePropertyCdfValue: Button? = null

    @FXML
    private var btnGetFileTracksFromMediaInfo: Button? = null

    // TRACKS

    @FXML
    private var tblTracks: TableView<Track>? = null

    @FXML
    private var colTrackUse: TableColumn<Track, Boolean>? = null

    @FXML
    private var colTrackType: TableColumn<Track, String>? = null

    @FXML
    private var tblTrackProperties: TableView<Property>? = null

    @FXML
    private var colTrackPropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colTrackPropertyValue: TableColumn<Property, String>? = null

    companion object {
        private var currentProjectExt: ProjectExt? = null
        private var hostServices: HostServices? = null
    }

    private var mainStage: Stage? = null
    private var currentFileExt: FileExt? = null
    private var listFilesExt: ObservableList<FileExt> = FXCollections.observableArrayList()
    private var currentFileProperty: Property? = null
    private var listFileProperties: ObservableList<Property> = FXCollections.observableArrayList()
    private var currentFilePropertyCdf: PropertyCdf? = null
    private var listFilePropertiesCdf: ObservableList<PropertyCdf> = FXCollections.observableArrayList()
    private var currentProjectProperty: Property? = null
    private var listProjectProperties: ObservableList<Property> = FXCollections.observableArrayList()
    private var currentProjectPropertyCdf: PropertyCdf? = null
    private var listProjectPropertiesCdf: ObservableList<PropertyCdf> = FXCollections.observableArrayList()
    private var currentTrack: Track? = null
    private var listTracks: ObservableList<Track> = FXCollections.observableArrayList()
    private var currentTrackProperty: Property? = null
    private var listTrackProperties: ObservableList<Property> = FXCollections.observableArrayList()

    private var listVideoCodecs: ObservableList<String> = FXCollections.observableArrayList()
    private var listLosslessVideoCodecs: ObservableList<String> = FXCollections.observableArrayList()
    private var listAudioCodecs: ObservableList<String> = FXCollections.observableArrayList()
    private var listVideoContainers: ObservableList<String> = FXCollections.observableArrayList()
    private var listLosslessContainers: ObservableList<String> = FXCollections.observableArrayList()

    fun editProject(project: Project? = null, incomingHostServices: HostServices? = null): ProjectExt? {
        if (project == null) {
            val firstProject = ProjectController.getListProjects().firstOrNull()
            if (firstProject != null) currentProjectExt = ProjectExt(firstProject)
        } else {
            currentProjectExt = ProjectExt(project)
        }
        mainStage = Stage()
        try {
            val root = FXMLLoader.load<Parent>(ProjectEditFXController::class.java.getResource("project-edit-view.fxml"))
            mainStage?.scene = Scene(root)
            hostServices = incomingHostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы ProjectEditFXController.")
        mainStage = null
        return currentProjectExt
    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна ProjectEditFXController.")
            saveCurrentFileProperty()
            saveCurrentFilePropertyCdf()
            saveCurrentFile()
            saveCurrentProject()
        }

        println("Инициализация ProjectEditFXController.")

        tblFiles?.placeholder = Label("Project not selected or don't have any files.")
        tblProjectProperties?.placeholder = Label("Project not selected or don't have any properties.")
        tblFileProperties?.placeholder = Label("File not selected or don't have any properties.")
        tblProjectPropertiesCdf?.placeholder = Label("Project not selected or don't have any computer-depended properties.")
        tblFilePropertiesCdf?.placeholder = Label("File not selected or don't have any computer-depended properties.")
        tblTracks?.placeholder = Label("File not selected or don't have any tracks.")
        tblTrackProperties?.placeholder = Label("Track not selected or don't have any properties..")

        menuDatabase?.text = getCurrentDatabase()?.name
        mainStage?.title = if (currentProjectExt == null) "Откройте или создайте проект." else "Проект: ${currentProjectExt!!.project.name}"
        menuDeleteProject?.isDisable = currentProjectExt == null
        menuActions?.isDisable = currentProjectExt == null
        menuEditShots?.isDisable = currentFileExt == null
        paneMain?.isVisible = currentProjectExt != null
        paneFile?.isVisible = currentFileExt != null

        if (currentProjectExt == null) return

//        tblFiles?.selectionModel?.selectionMode = SelectionMode.MULTIPLE

        pbFiles?.isVisible = false
        lblPbFiles?.isVisible = false

        listVideoContainers.clear()
        VideoContainers.values().forEach{ listVideoContainers.add(it.name) }
        cbProjectContainer?.items = listVideoContainers

        listLosslessContainers.clear()
        LosslessContainers.values().forEach{ listLosslessContainers.add(it.name) }
        cbProjectLosslessContainer?.items = listLosslessContainers

        listVideoCodecs.clear()
        VideoCodecs.values().forEach{ listVideoCodecs.add(it.name) }
        cbProjectVideoCodec?.items = listVideoCodecs

        listLosslessVideoCodecs.clear()
        LosslessVideoCodecs.values().forEach{ listLosslessVideoCodecs.add(it.name) }
        cbProjectLosslessCodec?.items = listLosslessVideoCodecs

        listAudioCodecs.clear()
        AudioCodecs.values().forEach{ listAudioCodecs.add(it.name) }
        cbProjectAudioCodec?.items = listAudioCodecs

        listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
        tblProjectProperties?.items = listProjectProperties

        btnProjectPropertyMoveToFirst?.isDisable = currentProjectProperty == null
        btnProjectPropertyMoveUp?.isDisable = currentProjectProperty == null
        btnProjectPropertyMoveToLast?.isDisable = currentProjectProperty == null
        btnProjectPropertyMoveDown?.isDisable = currentProjectProperty == null
        btnProjectPropertyDelete?.isDisable = currentProjectProperty == null
        fldProjectPropertyKey?.isDisable = currentProjectProperty == null
        fldProjectPropertyValue?.isDisable = currentProjectProperty == null
        fldProjectPropertyKey?.text = ""
        fldProjectPropertyValue?.text = ""
        
        listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
        tblProjectPropertiesCdf?.items = listProjectPropertiesCdf

        colProjectPropertyKey?.setCellValueFactory(PropertyValueFactory("key"))
        colProjectPropertyValue?.setCellValueFactory(PropertyValueFactory("value"))

        colProjectPropertyCdfKey?.setCellValueFactory(PropertyValueFactory("key"))
        colProjectPropertyCdfValue?.setCellValueFactory(PropertyValueFactory("value"))
        
        btnProjectPropertyCdfMoveToFirst?.isDisable = currentProjectPropertyCdf == null
        btnProjectPropertyCdfMoveUp?.isDisable = currentProjectPropertyCdf == null
        btnProjectPropertyCdfMoveToLast?.isDisable = currentProjectPropertyCdf == null
        btnProjectPropertyCdfMoveDown?.isDisable = currentProjectPropertyCdf == null
        btnProjectPropertyCdfDelete?.isDisable = currentProjectPropertyCdf == null
        fldProjectPropertyCdfKey?.isDisable = currentProjectPropertyCdf == null
        fldProjectPropertyCdfValue?.isDisable = currentProjectPropertyCdf == null
        btnBrowseProjectPropertyCdfValue?.isDisable = currentProjectPropertyCdf == null
        fldProjectPropertyCdfKey?.text = ""
        fldProjectPropertyCdfValue?.text = ""
        
        btnFileMoveToFirst?.isDisable = currentFileExt == null
        btnFileMoveUp?.isDisable = currentFileExt == null
        btnFileMoveToLast?.isDisable = currentFileExt == null
        btnFileMoveDown?.isDisable = currentFileExt == null
        btnFileDelete?.isDisable = currentFileExt == null

        fldProjectName?.text = currentProjectExt!!.project.name
        fldProjectShortName?.text = currentProjectExt!!.project.shortName
        fldProjectFolder?.text = currentProjectExt!!.project.folder
        fldProjectWidth?.text = currentProjectExt!!.project.width.toString()
        fldProjectHeight?.text = currentProjectExt!!.project.height.toString()
        fldProjectFps?.text = currentProjectExt!!.project.fps.toString()
        fldProjectVideoBitrate?.text = currentProjectExt!!.project.videoBitrate.toString()
        fldProjectAudioBitrate?.text = currentProjectExt!!.project.audioBitrate.toString()
        fldProjectAudioFrequency?.text = currentProjectExt!!.project.audioFrequency.toString()

        cbProjectVideoCodec?.selectionModel?.select(currentProjectExt!!.project.videoCodec)
        cbProjectAudioCodec?.selectionModel?.select(currentProjectExt!!.project.audioCodec)
        cbProjectContainer?.selectionModel?.select(currentProjectExt!!.project.container)
        cbProjectLosslessCodec?.selectionModel?.select(currentProjectExt!!.project.lossLessCodec)
        cbProjectLosslessContainer?.selectionModel?.select(currentProjectExt!!.project.lossLessContainer)

        var listThreads: MutableList<Thread> = mutableListOf()
        listThreads.add(LoadListFilesExt(listFilesExt, currentProjectExt!!, pbFiles, lblPbFiles))
        listThreads.add(UpdateListFilesExt(listFilesExt, currentProjectExt!!, pbFiles, lblPbFiles))
        val runListThreadsFilesFlagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
        val runListThreadsFramesFlagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
        val runListThreadsFiles = RunListThreads(listThreads, runListThreadsFilesFlagIsDone)
        runListThreadsFiles.start()

//        runListThreadsFilesFlagIsDone.addListener { observable, oldValue, newValue ->
//            if (newValue == true) {
//                println("runListThreadsFlagIsDone is TRUE")
//                listThreads = mutableListOf()
//                listFilesExt.forEach { fileExt->
//                    if (fileExt.framesExt.size == 0) listThreads.add(LoadListFramesExt(fileExt.framesExt, fileExt, pbFiles, lblPbFiles))
//                }
//                val runListThreadsFrames = RunListThreads(listThreads, runListThreadsFramesFlagIsDone)
//                runListThreadsFrames.start()
//                runListThreadsFilesFlagIsDone.set(false)
//            }
//        }
//
//        runListThreadsFramesFlagIsDone.addListener { observable, oldValue, newValue ->
//            if (newValue == true) {
//                println("runListThreadsFramesFlagIsDone is TRUE")
//                listThreads = mutableListOf()
//                listFilesExt.forEach { fileExt->
//                    if (fileExt.framesExt.size != 0) listThreads.add(UpdateListFramesExt(fileExt.framesExt, fileExt, pbFiles, lblPbFiles))
//                }
//                val runListThreadsFramesUpdate = RunListThreads(listThreads)
//                runListThreadsFramesUpdate.start()
//                runListThreadsFramesFlagIsDone.set(false)
//            }
//        }

//        listFilesExt = FXCollections.observableArrayList(FileController.getListFiles(currentProjectExt!!))
//        listFiles = FXCollections.observableArrayList(Main.fileController.getListFiles(currentProject!!))

        colFileOrder?.setCellValueFactory(PropertyValueFactory("fileOrder"))
        colFileName?.setCellValueFactory(PropertyValueFactory("fileName"))
        tblFiles?.items = listFilesExt

        tblFiles?.selectionModel?.select(currentFileExt)

        colFilePropertyKey?.setCellValueFactory(PropertyValueFactory("key"))
        colFilePropertyValue?.setCellValueFactory(PropertyValueFactory("value"))

        colFilePropertyCdfKey?.setCellValueFactory(PropertyValueFactory("key"))
        colFilePropertyCdfValue?.setCellValueFactory(PropertyValueFactory("value"))

        colTrackUse?.setCellValueFactory(PropertyValueFactory("use"))
        colTrackType?.setCellValueFactory(PropertyValueFactory("type"))

        colTrackPropertyKey?.setCellValueFactory(PropertyValueFactory("key"))
        colTrackPropertyValue?.setCellValueFactory(PropertyValueFactory("value"))

        // обработка события выбора записи в таблице tblFiles
        tblFiles?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            saveCurrentFileProperty()
            if (tblFiles?.selectionModel?.selectedItems?.size != 1) {
                currentFileExt = null
                paneFile?.isVisible = currentFileExt!=null
                btnFileMoveToFirst?.isDisable = currentFileExt == null
                btnFileMoveUp?.isDisable = currentFileExt == null
                btnFileMoveToLast?.isDisable = currentFileExt == null
                btnFileMoveDown?.isDisable = currentFileExt == null
                btnFileDelete?.isDisable = currentFileExt == null
            } else {
                if (tblFiles?.selectionModel?.selectedItems?.size == 0) {
                    currentFileExt = null
                } else {
                    if (currentFileExt != tblFiles?.selectionModel?.selectedItems!![0]) saveCurrentFile()
                    currentFileExt = tblFiles?.selectionModel?.selectedItems!![0]
                }

                paneFile?.isVisible = currentFileExt!=null

                menuEditShots?.isDisable = currentFileExt == null

                if (currentFileExt != null) {
                    btnFileDelete?.isDisable = currentFileExt == null
                    btnFileMoveToFirst?.isDisable = currentFileExt == listFilesExt.first()
                    btnFileMoveUp?.isDisable = currentFileExt == listFilesExt.first()
                    btnFileMoveToLast?.isDisable = currentFileExt == listFilesExt.last()
                    btnFileMoveDown?.isDisable = currentFileExt == listFilesExt.last()
                    fldFileName?.text = currentFileExt!!.file.name
                    fldFileShortName?.text = currentFileExt!!.file.shortName
                    fldFilePath?.text = currentFileExt!!.file.path

                    listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFileProperties?.items = listFileProperties

                    btnFilePropertyMoveToFirst?.isDisable = currentFileProperty == null
                    btnFilePropertyMoveUp?.isDisable = currentFileProperty == null
                    btnFilePropertyMoveToLast?.isDisable = currentFileProperty == null
                    btnFilePropertyMoveDown?.isDisable = currentFileProperty == null
                    btnFilePropertyDelete?.isDisable = currentFileProperty == null
                    fldFilePropertyKey?.isDisable = currentFileProperty == null
                    fldFilePropertyValue?.isDisable = currentFileProperty == null
                    fldFilePropertyKey?.text = ""
                    fldFilePropertyValue?.text = ""

                    listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFilePropertiesCdf?.items = listFilePropertiesCdf

                    btnFilePropertyCdfMoveToFirst?.isDisable = currentFilePropertyCdf == null
                    btnFilePropertyCdfMoveUp?.isDisable = currentFilePropertyCdf == null
                    btnFilePropertyCdfMoveToLast?.isDisable = currentFilePropertyCdf == null
                    btnFilePropertyCdfMoveDown?.isDisable = currentFilePropertyCdf == null
                    btnFilePropertyCdfDelete?.isDisable = currentFilePropertyCdf == null
                    fldFilePropertyCdfKey?.isDisable = currentFilePropertyCdf == null
                    fldFilePropertyCdfValue?.isDisable = currentFilePropertyCdf == null
                    btnBrowseFilePropertyCdfValue?.isDisable = currentFilePropertyCdf == null
                    fldFilePropertyCdfKey?.text = ""
                    fldFilePropertyCdfValue?.text = ""

                    listTracks = FXCollections.observableArrayList(currentFileExt!!.file.tracks.toMutableList())
                    listTracks.sort()
                    tblTracks?.items = listTracks

                }

                currentTrack = null
                listTrackProperties = FXCollections.observableArrayList()
                tblTrackProperties?.items = listTrackProperties
            }


        }

        // обработка события выбора записи в таблице tblTracks
        tblTracks?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->

            currentTrack = newValue

            if (currentTrack != null) {
                listTrackProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentTrack!!::class.java.simpleName, currentTrack!!.id))
                tblTrackProperties?.items = listTrackProperties
            } else {
                listTrackProperties = FXCollections.observableArrayList()
                tblTrackProperties?.items = listTrackProperties
            }

        }

        tblFileProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentFileProperty != newValue) saveCurrentFileProperty()

            currentFileProperty = newValue

            btnFilePropertyDelete?.isDisable = currentFileProperty == null
            btnFilePropertyMoveToFirst?.isDisable = currentFileProperty == null || currentFileProperty == listFileProperties.first()
            btnFilePropertyMoveUp?.isDisable = currentFileProperty == null || currentFileProperty == listFileProperties.first()
            btnFilePropertyMoveToLast?.isDisable = currentFileProperty == null || currentFileProperty == listFileProperties.last()
            btnFilePropertyMoveDown?.isDisable = currentFileProperty == null || currentFileProperty == listFileProperties.last()

            fldFilePropertyKey?.isDisable = currentFileProperty == null
            fldFilePropertyValue?.isDisable = currentFileProperty == null

            fldFilePropertyKey?.text = currentFileProperty?.key
            fldFilePropertyValue?.text = currentFileProperty?.value

        }

        tblFilePropertiesCdf?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentFilePropertyCdf != newValue) saveCurrentFilePropertyCdf()

            currentFilePropertyCdf = newValue

            btnFilePropertyCdfDelete?.isDisable = currentFilePropertyCdf == null
            btnFilePropertyCdfMoveToFirst?.isDisable = currentFilePropertyCdf == null || currentFilePropertyCdf == listFilePropertiesCdf.first()
            btnFilePropertyCdfMoveUp?.isDisable = currentFilePropertyCdf == null || currentFilePropertyCdf == listFilePropertiesCdf.first()
            btnFilePropertyCdfMoveToLast?.isDisable = currentFilePropertyCdf == null || currentFilePropertyCdf == listFilePropertiesCdf.last()
            btnFilePropertyCdfMoveDown?.isDisable = currentFilePropertyCdf == null || currentFilePropertyCdf == listFilePropertiesCdf.last()

            fldFilePropertyCdfKey?.isDisable = currentFilePropertyCdf == null || Folders.values().any { it.propertyCdfKey == currentFilePropertyCdf!!.key }
            fldFilePropertyCdfValue?.isDisable = currentFilePropertyCdf == null
            btnBrowseFilePropertyCdfValue?.isDisable = currentFilePropertyCdf == null

            fldFilePropertyCdfKey?.text = currentFilePropertyCdf?.key
            fldFilePropertyCdfValue?.text = currentFilePropertyCdf?.value

        }

        tblProjectProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentProjectProperty != newValue) saveCurrentProjectProperty()

            currentProjectProperty = newValue

            btnProjectPropertyDelete?.isDisable = currentProjectProperty == null
            btnProjectPropertyMoveToFirst?.isDisable = currentProjectProperty == null || currentProjectProperty == listProjectProperties.first()
            btnProjectPropertyMoveUp?.isDisable = currentProjectProperty == null || currentProjectProperty == listProjectProperties.first()
            btnProjectPropertyMoveToLast?.isDisable = currentProjectProperty == null || currentProjectProperty == listProjectProperties.last()
            btnProjectPropertyMoveDown?.isDisable = currentProjectProperty == null || currentProjectProperty == listProjectProperties.last()

            fldProjectPropertyKey?.isDisable = currentProjectProperty == null
            fldProjectPropertyValue?.isDisable = currentProjectProperty == null

            fldProjectPropertyKey?.text = currentProjectProperty?.key
            fldProjectPropertyValue?.text = currentProjectProperty?.value

        }
        
        tblProjectPropertiesCdf?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentProjectPropertyCdf != newValue) saveCurrentProjectPropertyCdf()

            currentProjectPropertyCdf = newValue

            btnProjectPropertyCdfDelete?.isDisable = currentProjectPropertyCdf == null
            btnProjectPropertyCdfMoveToFirst?.isDisable = currentProjectPropertyCdf == null || currentProjectPropertyCdf == listProjectPropertiesCdf.first()
            btnProjectPropertyCdfMoveUp?.isDisable = currentProjectPropertyCdf == null || currentProjectPropertyCdf == listProjectPropertiesCdf.first()
            btnProjectPropertyCdfMoveToLast?.isDisable = currentProjectPropertyCdf == null || currentProjectPropertyCdf == listProjectPropertiesCdf.last()
            btnProjectPropertyCdfMoveDown?.isDisable = currentProjectPropertyCdf == null || currentProjectPropertyCdf == listProjectPropertiesCdf.last()

            fldProjectPropertyCdfKey?.isDisable = currentProjectPropertyCdf == null || Folders.values().any { it.propertyCdfKey == currentProjectPropertyCdf!!.key }
            fldProjectPropertyCdfValue?.isDisable = currentProjectPropertyCdf == null
            btnBrowseProjectPropertyCdfValue?.isDisable = currentProjectPropertyCdf == null

            fldProjectPropertyCdfKey?.text = currentProjectPropertyCdf?.key
            fldProjectPropertyCdfValue?.text = currentProjectPropertyCdf?.value

        }
        
        // изменение поля "fldProjectName" (событие потери фокуса полем) Нужно для изменения надписи окна
        fldProjectName?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProject()
            }
        }

        // изменение поля "fldFileName" (событие потери фокуса полем) Нужно для рефреша таблицы файлов
        fldFileName?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentFile()
            }
        }

        // изменение поля "fldFilePropertyKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldFilePropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentFileProperty()
            }
        }

        // изменение поля "fldFilePropertyCdfKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldFilePropertyCdfKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentFilePropertyCdf()
            }
        }

        // изменение поля "fldProjectPropertyKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldProjectPropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProjectProperty()
            }
        }
        
        // изменение поля "fldProjectPropertyCdfKey" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldProjectPropertyCdfKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProjectPropertyCdf()
            }
        }
        
        // изменение поля "fldFilePropertyValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldFilePropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentFileProperty()
            }
        }

        // изменение поля "fldFilePropertyCdfValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldFilePropertyCdfValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentFilePropertyCdf()
            }
        }

        // изменение поля "fldProjectPropertyValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldProjectPropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProjectProperty()
            }
        }
        
        // изменение поля "fldProjectPropertyCdfValue" (событие потери фокуса полем) Нужно для рефреша таблицы свойств файла
        fldProjectPropertyCdfValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProjectPropertyCdf()
            }
        }
        
        // делаем поле colFilePropertyValue таблицы tblFileProperties с переносом по словам и расширяемым по высоте
        colFilePropertyValue?.setCellFactory { param: TableColumn<Property?, String?>? ->
            val cell: TableCell<Property, String> = TableCell<Property, String>()
            val text = Text()
            text.style = ""
            cell.setGraphic(text)
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE)
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilePropertyValue!!.widthProperty())
            cell
        }

        // делаем поле colFilePropertyCdfValue таблицы tblFilePropertiesCdf с переносом по словам и расширяемым по высоте
        colFilePropertyCdfValue?.setCellFactory { param: TableColumn<PropertyCdf?, String?>? ->
            val cell: TableCell<PropertyCdf, String> = TableCell<PropertyCdf, String>()
            val text = Text()
            text.style = ""
            cell.setGraphic(text)
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE)
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilePropertyCdfValue!!.widthProperty())
            cell
        }

        // делаем поле colProjectPropertyValue таблицы tblProjectProperties с переносом по словам и расширяемым по высоте
        colProjectPropertyValue?.setCellFactory { param: TableColumn<Property?, String?>? ->
            val cell: TableCell<Property, String> = TableCell<Property, String>()
            val text = Text()
            text.style = ""
            cell.setGraphic(text)
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE)
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colProjectPropertyValue!!.widthProperty())
            cell
        }
        
        // делаем поле colProjectPropertyCdfValue таблицы tblProjectPropertiesCdf с переносом по словам и расширяемым по высоте
        colProjectPropertyCdfValue?.setCellFactory { param: TableColumn<PropertyCdf?, String?>? ->
            val cell: TableCell<PropertyCdf, String> = TableCell<PropertyCdf, String>()
            val text = Text()
            text.style = ""
            cell.setGraphic(text)
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE)
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colProjectPropertyCdfValue!!.widthProperty())
            cell
        }

        // делаем поле colTrackPropertyValue таблицы tblTrackProperties с переносом по словам и расширяемым по высоте
        colTrackPropertyValue?.setCellFactory { param: TableColumn<Property?, String?>? ->
            val cell: TableCell<Property, String> = TableCell<Property, String>()
            val text = Text()
            text.style = ""
            cell.setGraphic(text)
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE)
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colTrackPropertyValue!!.widthProperty())
            cell
        }
        
        // событие двойного клика в таблице tblFileProperties
        tblFileProperties?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (hostServices != null && currentFileProperty != null && currentFileProperty?.key?.startsWith("url_", ignoreCase = true) == true) {
                        hostServices!!.showDocument(currentFileProperty?.value)
                    }
                }
            }
        }

        // событие двойного клика в таблице tblFilePropertiesCdf
        tblFilePropertiesCdf?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (hostServices != null && currentFilePropertyCdf != null && currentFilePropertyCdf?.key?.startsWith("folder_", ignoreCase = true) == true) {
                        val fld = Folders.values().filter { it.propertyCdfKey == currentFilePropertyCdf?.key }.firstOrNull()
                        hostServices!!.showDocument(if (fld == null || currentFilePropertyCdf?.value != "") currentFilePropertyCdf?.value else FileController.getCdfFolder(currentFileExt!!.file, fld, true))
//                        hostServices!!.showDocument(if (fld == null || currentFilePropertyCdf?.value != "") currentFilePropertyCdf?.value else Main.fileController.getCdfFolder(currentFile!!, fld, true))
                    }
                }
            }
        }

        // событие двойного клика в таблице tblProjectProperties
        tblProjectProperties?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (hostServices != null && currentProjectProperty != null && currentProjectProperty?.key?.startsWith("url_", ignoreCase = true) == true) {
                        hostServices!!.showDocument(currentProjectProperty?.value)
                    }
                }
            }
        }

        // событие двойного клика в таблице tblProjectPropertiesCdf
        tblProjectPropertiesCdf?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (hostServices != null && currentProjectPropertyCdf != null && currentProjectPropertyCdf?.key?.startsWith("folder_", ignoreCase = true) == true) {
                        val fld = Folders.values().filter { it.propertyCdfKey == currentProjectPropertyCdf?.key }.firstOrNull()
                        hostServices!!.showDocument(if (fld == null || currentProjectPropertyCdf?.value != "") currentProjectPropertyCdf?.value else ProjectController.getCdfFolder(currentProjectExt!!.project, fld, true))
                    }
                }
            }
        }

        // событие двойного клика в таблице tblTracks
        tblTracks?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentTrack != null && currentTrack?.type != "General" && currentTrack?.type != "Video") {
                        currentTrack?.use = !currentTrack?.use!!
                        TrackController.save(currentTrack!!)
                        tblTracks?.refresh()
                    }
                }
            }
        }
        
    }

    fun saveCurrentFileProperty() {

        if (currentFileProperty != null) {
            var needToSave = false

            var tmp: String = fldFilePropertyKey?.text ?: ""
            if (tmp != currentFileProperty?.key) {
                currentFileProperty?.key = tmp
                needToSave = true
            }

            tmp = fldFilePropertyValue?.text ?: ""
            if (tmp != currentFileProperty?.value) {
                currentFileProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentFileProperty!!)
                tblFileProperties?.refresh()
            }

        }
    }

    fun saveCurrentFilePropertyCdf() {

        if (currentFilePropertyCdf != null) {
            var needToSave = false

            var tmp: String = fldFilePropertyCdfKey?.text ?: ""
            if (tmp != currentFilePropertyCdf?.key) {
                currentFilePropertyCdf?.key = tmp
                needToSave = true
            }

            tmp = fldFilePropertyCdfValue?.text ?: ""
            if (tmp != currentFilePropertyCdf?.value) {
                currentFilePropertyCdf?.value = tmp
                when(currentFilePropertyCdf?.key) {
                    Folders.PREVIEW.propertyCdfKey -> { currentFileExt!!.folderPreview = null
                        currentFileExt!!.hasPreview = null }
                    Folders.LOSSLESS.propertyCdfKey -> { currentFileExt!!.folderLossless = null
                        currentFileExt!!.hasLossless = null }
                    Folders.FAVORITES.propertyCdfKey -> currentFileExt!!.folderFavorites = null
                    Folders.SHOTS.propertyCdfKey -> currentFileExt!!.folderShots = null
                    Folders.FRAMES_SMALL.propertyCdfKey -> { currentFileExt!!.folderFramesSmall = null
                        currentFileExt!!.hasFramesSmall = null }
                    Folders.FRAMES_MEDIUM.propertyCdfKey -> { currentFileExt!!.folderFramesMedium = null
                        currentFileExt!!.hasFramesMedium = null }
                    Folders.FRAMES_FULL.propertyCdfKey -> { currentFileExt!!.folderFramesFull = null
                        currentFileExt!!.hasFramesFull = null
                        currentFileExt!!.hasDetectedFaces = null
                        currentFileExt!!.hasCreatedFaces = null
                    }
                }
                needToSave = true
            }

            if (needToSave) {
                PropertyCdfController.save(currentFilePropertyCdf!!)
                tblFilePropertiesCdf?.refresh()
            }

        }
    }

    fun saveCurrentProjectProperty() {

        if (currentProjectProperty != null) {
            var needToSave = false

            var tmp: String = fldProjectPropertyKey?.text ?: ""
            if (tmp != currentProjectProperty?.key) {
                currentProjectProperty?.key = tmp
                needToSave = true
            }

            tmp = fldProjectPropertyValue?.text ?: ""
            if (tmp != currentProjectProperty?.value) {
                currentProjectProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentProjectProperty!!)
                tblProjectProperties?.refresh()
            }

        }
    }
    
    fun saveCurrentProjectPropertyCdf() {

        if (currentProjectPropertyCdf != null) {
            var needToSave = false

            var tmp: String = fldProjectPropertyCdfKey?.text ?: ""
            if (tmp != currentProjectPropertyCdf?.key) {
                currentProjectPropertyCdf?.key = tmp
                needToSave = true
            }

            tmp = fldProjectPropertyCdfValue?.text ?: ""
            if (tmp != currentProjectPropertyCdf?.value) {
                currentProjectPropertyCdf?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyCdfController.save(currentProjectPropertyCdf!!)
                tblProjectPropertiesCdf?.refresh()
            }

        }
    }
    
    fun saveCurrentFile() {
        if (currentFileExt != null) {
            var needToSave = false

            var tmp: String = fldFileName?.text ?: ""
            if (tmp != currentFileExt!!.file.name) {
                currentFileExt!!.file.name = tmp
                needToSave = true
            }

            tmp = fldFileShortName?.text ?: ""
            if (tmp != currentFileExt!!.file.shortName) {
                currentFileExt!!.file.shortName = tmp
                currentFileExt!!.resetFieldsLinkedShortName()
                needToSave = true
            }

            tmp = fldFilePath?.text ?: ""
            if (tmp != currentFileExt!!.file.path) {
                currentFileExt!!.file.path = tmp
                currentFileExt!!.resetFieldsLinkedPath()
                needToSave = true
            }

            if (needToSave) {
                FileController.save(currentFileExt!!.file)
//                Main.fileController.save(currentFile!!)
                tblFiles?.refresh()
            }

        }
    }

    fun saveCurrentProject() {
        if (currentProjectExt != null) {
            var needToSave = false

            var tmp: String = fldProjectName?.text ?: ""
            if (tmp != currentProjectExt!!.project.name) {
                currentProjectExt!!.project.name = tmp
                needToSave = true
            }

            tmp = fldProjectShortName?.text ?: ""
            if (tmp != currentProjectExt!!.project.shortName) {
                currentProjectExt!!.project.shortName = tmp
                needToSave = true
            }

            tmp = fldProjectFolder?.text ?: ""
            if (tmp != currentProjectExt!!.project.folder) {
                currentProjectExt!!.project.folder = tmp
                needToSave = true
            }

            tmp = fldProjectWidth?.text ?: ""
            if (tmp != currentProjectExt!!.project.width.toString()) {
                currentProjectExt!!.project.width = (tmp.toIntOrNull() ?: currentProjectExt!!.project.width) as Int
                needToSave = true
            }

            tmp = fldProjectHeight?.text ?: ""
            if (tmp != currentProjectExt!!.project.height.toString()) {
                currentProjectExt!!.project.height = (tmp.toIntOrNull() ?: currentProjectExt!!.project.height) as Int
                needToSave = true
            }

            tmp = fldProjectFps?.text ?: ""
            if (tmp != currentProjectExt!!.project.fps.toString()) {
                currentProjectExt!!.project.fps = (tmp.toDoubleOrNull() ?: currentProjectExt!!.project.fps) as Double
                needToSave = true
            }

            tmp = fldProjectVideoBitrate?.text ?: ""
            if (tmp != currentProjectExt!!.project.videoBitrate.toString()) {
                currentProjectExt!!.project.videoBitrate = (tmp.toIntOrNull() ?: currentProjectExt!!.project.videoBitrate) as Int
                needToSave = true
            }

            tmp = fldProjectAudioBitrate?.text ?: ""
            if (tmp != currentProjectExt!!.project.audioBitrate.toString()) {
                currentProjectExt!!.project.audioBitrate = (tmp.toIntOrNull() ?: currentProjectExt!!.project.audioBitrate) as Int
                needToSave = true
            }

            tmp = fldProjectAudioFrequency?.text ?: ""
            if (tmp != currentProjectExt!!.project.audioFrequency.toString()) {
                currentProjectExt!!.project.audioFrequency = (tmp.toIntOrNull() ?: currentProjectExt!!.project.audioFrequency) as Int
                needToSave = true
            }

            tmp = cbProjectVideoCodec?.selectionModel?.selectedItem ?: ""
            if (tmp != currentProjectExt!!.project.videoCodec) {
                currentProjectExt!!.project.videoCodec = tmp
                needToSave = true
            }

            tmp = cbProjectAudioCodec?.selectionModel?.selectedItem ?: ""
            if (tmp != currentProjectExt!!.project.audioCodec) {
                currentProjectExt!!.project.audioCodec = tmp
                needToSave = true
            }

            tmp = cbProjectContainer?.selectionModel?.selectedItem ?: ""
            if (tmp != currentProjectExt!!.project.container) {
                currentProjectExt!!.project.container = tmp
                needToSave = true
            }

            tmp = cbProjectLosslessCodec?.selectionModel?.selectedItem ?: ""
            if (tmp != currentProjectExt!!.project.lossLessCodec) {
                currentProjectExt!!.project.lossLessCodec = tmp
                needToSave = true
            }

            tmp = cbProjectLosslessContainer?.selectionModel?.selectedItem ?: ""
            if (tmp != currentProjectExt!!.project.lossLessContainer) {
                currentProjectExt!!.project.lossLessContainer = tmp
                needToSave = true
            }

            if (needToSave) {
                ProjectCdfController.save(currentProjectExt!!.project.cdfs.first())
                ProjectController.save(currentProjectExt!!.project)
                mainStage?.setTitle("Проект: ${currentProjectExt!!.project.name}")
            }

        }
    }

    @FXML
    fun doGetFileTracksFromMediaInfo(event: ActionEvent?) {
        if (currentFileExt != null) {
            TrackController.createTracksFromMediaInfo(currentFileExt!!.file)
            listTracks = FXCollections.observableArrayList(currentFileExt!!.file.tracks.toMutableList())
            listTracks.sort()
            tblTracks?.items = listTracks
        }
    }

    @FXML
    fun doMenuDeleteProject(event: ActionEvent?) {

        if (currentProjectExt!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление проекта"
            alert.headerText = "Вы действительно хотите удалить проект «${currentProjectExt!!.project.name}»?"
            alert.contentText = "В случае утвердительного ответа проект будет удален из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить проект?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                ProjectController.delete(currentProjectExt!!.project)
                currentProjectExt = null
                initialize()
            }
        }

    }

    @FXML
    fun doMenuExit(event: ActionEvent?) {
        mainStage?.close()
    }

    @FXML
    fun doMenuNewProject(event: ActionEvent?) {
        currentProjectExt = ProjectExt(ProjectController.create())
        initialize()
    }

    @FXML
    fun doMenuOpen(event: ActionEvent?) {
        saveCurrentFile()
        saveCurrentProject()
        currentProjectExt = ProjectExt(ProjectSelectFXController().getProject(currentProjectExt!!.project)!!)
        initialize()
    }

    @FXML
    fun doMenuProjectActions(event: ActionEvent?) {
        if (currentProjectExt != null) {
            ProjectActionsFXController().actionsProject(currentProjectExt!!.project, listFilesExt, hostServices)
        }
    }

    @FXML
    fun doMenuEditShots(event: ActionEvent?) {
        if (currentFileExt != null) {
            ShotsEditFXController().editShots(FileExt(currentFileExt!!.file, currentProjectExt!!), hostServices)
//            ShotsEditFXController.editShots(currentFile!!, hostServices)
        }
    }

    @FXML
    fun doSelectDatabase(event: ActionEvent?) {

        val currentDatabase = getCurrentDatabase()
        val result = DatabaseSelectFXController().getDatabase(getCurrentDatabase())
        if (currentDatabase != result) {
            setPropertyValue(H2DB_PROPERTYKEY_CURRENTDB_ID,result?.id.toString())
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Выбор базы данных"
            alert.headerText = "Для вступления измненения в силу перезапустите приложение."
            alert.showAndWait()
            mainStage?.close()
        }
    }

    @FXML
    fun doSelectProjectFolder(event: ActionEvent?) {

        val directoryChooser = DirectoryChooser()
        val initialDirectory = if (currentProjectExt != null) IOFile(currentProjectExt!!.project.folder!!).absolutePath?:"" else ""
        if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
        val directorySelected = directoryChooser.showDialog(Stage())
        if (directorySelected != null) {
            fldProjectFolder?.text = directorySelected.absolutePath
            currentProjectExt!!.project.folder = directorySelected.absolutePath
            ProjectCdfController.save(currentProjectExt!!.project.cdfs.first())
            ProjectController.save(currentProjectExt!!.project)
        }
    }

    @FXML
    fun doSelectFilePath(event: ActionEvent?) {

        currentFileExt?.let { FileController.save(currentFileExt!!.file) }
//        currentFile?.let { Main.fileController.save(currentFile!!) }

        val fileChooser = FileChooser()
        fileChooser.title = "Выберите файл"
        fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("All Files", "*.*"))
        val initialDirectory = if (currentFileExt != null) IOFile(currentFileExt!!.file.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) fileChooser.initialDirectory = IOFile(initialDirectory)
        val ioFile = fileChooser.showOpenDialog(Stage())
        if (ioFile != null && currentFileExt != null) {
            currentFileExt!!.file.path = ioFile.absolutePath
            FileCdfController.save(currentFileExt!!.file.cdfs!!.first())
            fldFilePath?.text = currentFileExt!!.file.path
            FileController.save(currentFileExt!!.file)
//            Main.fileController.save(currentFile!!)
            TrackController.createTracksFromMediaInfo(currentFileExt!!.file)
            listTracks = FXCollections.observableArrayList(currentFileExt!!.file.tracks.toMutableList())
            listTracks.sort()
            tblTracks?.items = listTracks
        }

    }

    @FXML
    fun doFileAdd(event: ActionEvent?) {
        currentFileExt?.let { FileController.save(currentFileExt!!.file) }
//        currentFile?.let { Main.fileController.save(currentFile!!) }

        val fileChooser = FileChooser()
        fileChooser.title = "Добавить файл к проекту"
        fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("All Files", "*.*"))
        val initialDirectory = if (currentFileExt != null) IOFile(currentFileExt!!.file.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) fileChooser.initialDirectory = IOFile(initialDirectory)
        val ioFile = fileChooser.showOpenDialog(Stage())
        if (ioFile != null) {
            val foundFile = currentProjectExt!!.project.files.firstOrNull { it.path == ioFile.absolutePath }
            if (foundFile != null) {
                tblFiles?.selectionModel?.select(listFilesExt.filter { it.file == foundFile }.first())
            } else {
                val file = FileController.create(currentProjectExt!!.project, ioFile.absolutePath)
                val id = file.id
                LoadListFilesExt(listFilesExt, currentProjectExt!!, pbFiles, lblPbFiles).start()
                tblFiles?.items = listFilesExt
                currentFileExt = listFilesExt.filter { it.file.id == id }.first()
                tblFiles?.selectionModel?.select(currentFileExt)
                TrackController.createTracksFromMediaInfo(currentFileExt!!.file)
                listTracks = FXCollections.observableArrayList(currentFileExt!!.file.tracks.toMutableList())
                listTracks.sort()
                tblTracks?.items = listTracks
            }
        }
    }

    @FXML
    fun doFileAddFilesFromFolder(event: ActionEvent?) {

        saveCurrentFileProperty()
        saveCurrentFile()

        val directoryChooser = DirectoryChooser()
        val initialDirectory = if (currentFileExt != null) IOFile(currentFileExt!!.file.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
        val directorySelected = directoryChooser.showDialog(Stage())
        if (directorySelected != null) {
            directorySelected.listFiles()?.forEach { ioFile ->
                if (currentProjectExt!!.project.files.filter { it.path == ioFile.absolutePath }.isEmpty()) {
                    val file = FileController.create(currentProjectExt!!.project, ioFile.absolutePath)
                }
            }
            LoadListFilesExt(listFilesExt, currentProjectExt!!, pbFiles, lblPbFiles).start()
            tblFiles?.items = listFilesExt
        }

    }

    @FXML
    fun doFileDelete(event: ActionEvent?) {
        if (currentFileExt!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление файла"
            alert.headerText = "Вы действительно хотите удалить файл «${currentFileExt!!.file.name}»?"
            alert.contentText = "В случае утвердительного ответа файл будет удален из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить файл?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                FileController.delete(currentFileExt!!.file)
//                Main.fileController.delete(currentFile!!)
                currentFileExt = null
//                listFilesExt = FXCollections.observableArrayList(FileController.getListFiles(currentProjectExt!!))
//                listFiles = FXCollections.observableArrayList(Main.fileController.getListFiles(currentProject!!))
                tblFiles?.items = listFilesExt

                paneFile?.isVisible = currentFileExt != null
                btnFileMoveToFirst?.isDisable = currentFileExt == null
                btnFileMoveUp?.isDisable = currentFileExt == null
                btnFileMoveToLast?.isDisable = currentFileExt == null
                btnFileMoveDown?.isDisable = currentFileExt == null
                btnFileDelete?.isDisable = currentFileExt == null

            }
        }
    }

    @FXML
    fun doFileMoveDown(event: ActionEvent?) {
        doMoveFile(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFileMoveToFirst(event: ActionEvent?) {
        doMoveFile(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFileMoveToLast(event: ActionEvent?) {
        doMoveFile(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFileMoveUp(event: ActionEvent?) {
        doMoveFile(ReorderTypes.MOVE_UP)
    }

    fun doMoveFile(reorderType: ReorderTypes) {
        val id = currentFileExt!!.file.id
        currentFileExt?.let { FileController.reOrder(reorderType, it.file) }
//        currentFile?.let { Main.fileController.reOrder(reorderType, it) }
        updateOrderListFiles()
//        LoadListFilesExt(listFilesExt, currentProjectExt!!, pbFiles, lblPbFiles).start()
//        listFilesExt = FXCollections.observableArrayList(FileController.getListFiles(currentProjectExt!!))
//        listFiles = FXCollections.observableArrayList(Main.fileController.getListFiles(currentProject!!))
        tblFiles?.items = listFilesExt
        currentFileExt = listFilesExt.filter { it.file.id == id }.first()
        tblFiles?.selectionModel?.select(currentFileExt)
    }

    fun updateOrderListFiles() {
        val sourceIterable = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(currentProjectExt!!.project.id, 0)
        sourceIterable.forEach { file ->
            val fileExt = listFilesExt.filter { it.file.id == file.id }.first()
            if (fileExt.file.order != file.order) {
                fileExt.file.order = file.order
            }
        }
        listFilesExt.sort()
    }

    @FXML
    fun doFilePropertyAdd(event: ActionEvent?) {
        if (currentFileExt!=null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство файла"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства файла"
                alert.headerText = "Вы действительно хотите добавить новое свойство для файла?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentFileProperty()
                    val id = PropertyController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id).id
                    listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFileProperties?.items = listFileProperties
                    currentFileProperty = listFileProperties.filter { it.id == id }.first()
                    tblFileProperties?.selectionModel?.select(currentFileProperty)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val listKeys = Main.propertyRepo.getKeys(currentFileExt!!.file::class.java.simpleName)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listFileProperties.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentFileProperty()
                        val id = PropertyController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id, key).id
                        listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                        tblFileProperties?.items = listFileProperties
                        currentFileProperty = listFileProperties.filter { it.id == id }.first()
                        tblFileProperties?.selectionModel?.select(currentFileProperty)
                    }
                    menu.items.add(menuItem)
                }
            }

            if (countKeysAdded > 0) {
                menu.items.add(SeparatorMenuItem())
                menuItem = MenuItem()
                menuItem.text = "Добавить все свойства для файла"
                menuItem.onAction = EventHandler { e: ActionEvent? ->
                    saveCurrentFileProperty()
                    listKeys.forEach { key ->
                        if (listFileProperties.filter { it.key == key }.isEmpty()) {
                            PropertyController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id, key)
                        }
                    }
                    listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFileProperties?.items = listFileProperties
                }
                menu.items.add(menuItem)
            }

            btnFilePropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnFilePropertyAdd!!.localToScreen(btnFilePropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.minX +screenBounds.width, screenBounds.minY)

        }
    }

    @FXML
    fun doFilePropertyCdfAdd(event: ActionEvent?) {
        if (currentFileExt!=null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство файла"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства файла"
                alert.headerText = "Вы действительно хотите добавить новое свойство для файла?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentFilePropertyCdf()
                    val id = PropertyCdfController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id).id
                    listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFilePropertiesCdf?.items = listFilePropertiesCdf
                    currentFilePropertyCdf = listFilePropertiesCdf.filter { it.id == id }.first()
                    tblFilePropertiesCdf?.selectionModel?.select(currentFilePropertyCdf)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val listKeys = Main.propertyCdfRepo.getKeys(currentFileExt!!.file::class.java.simpleName, Main.ccid)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listFilePropertiesCdf.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentFilePropertyCdf()
                        val id = PropertyCdfController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id, key).id
                        listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                        tblFilePropertiesCdf?.items = listFilePropertiesCdf
                        currentFilePropertyCdf = listFilePropertiesCdf.filter { it.id == id }.first()
                        tblFilePropertiesCdf?.selectionModel?.select(currentFilePropertyCdf)
                    }
                    menu.items.add(menuItem)
                }
            }

            if (countKeysAdded > 0) {
                menu.items.add(SeparatorMenuItem())
                menuItem = MenuItem()
                menuItem.text = "Добавить все свойства для файла"
                menuItem.onAction = EventHandler { e: ActionEvent? ->
                    saveCurrentFilePropertyCdf()
                    listKeys.forEach { key ->
                        if (listFilePropertiesCdf.filter { it.key == key }.isEmpty()) {
                            PropertyCdfController.editOrCreate(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id, key)
                        }
                    }
                    listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                    tblFilePropertiesCdf?.items = listFilePropertiesCdf
                }
                menu.items.add(menuItem)
            }

            btnFilePropertyCdfAdd?.contextMenu = menu
            val screenBounds: Bounds = btnFilePropertyCdfAdd!!.localToScreen(btnFilePropertyCdfAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.getMinX()+screenBounds.getWidth(), screenBounds.getMinY())

        }
    }

    @FXML
    fun doProjectPropertyAdd(event: ActionEvent?) {
        if (currentProjectExt!=null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство проекта"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства проекта"
                alert.headerText = "Вы действительно хотите добавить новое свойство для проекта?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentProjectProperty()
                    val id = PropertyController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id).id
                    listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                    tblProjectProperties?.items = listProjectProperties
                    currentProjectProperty = listProjectProperties.filter { it.id == id }.first()
                    tblProjectProperties?.selectionModel?.select(currentProjectProperty)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val listKeys = PropertyController.getKeys(currentProjectExt!!.project::class.java.simpleName)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listProjectProperties.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentProjectProperty()
                        val id = PropertyController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id, key).id
                        listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                        tblProjectProperties?.items = listProjectProperties
                        currentProjectProperty = listProjectProperties.filter { it.id == id }.first()
                        tblProjectProperties?.selectionModel?.select(currentProjectProperty)
                    }
                    menu.items.add(menuItem)
                }
            }

            if (countKeysAdded > 0) {
                menu.items.add(SeparatorMenuItem())
                menuItem = MenuItem()
                menuItem.text = "Добавить все свойства для проекта"
                menuItem.onAction = EventHandler { e: ActionEvent? ->
                    saveCurrentProjectProperty()
                    listKeys.forEach { key ->
                        if (listProjectProperties.filter { it.key == key }.isEmpty()) {
                            PropertyController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id, key)
                        }
                    }
                    listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                    tblProjectProperties?.items = listProjectProperties
                }
                menu.items.add(menuItem)
            }

            btnProjectPropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnProjectPropertyAdd!!.localToScreen(btnProjectPropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.getMinX()+screenBounds.getWidth(), screenBounds.getMinY())

        }
    }

    @FXML
    fun doProjectPropertyCdfAdd(event: ActionEvent?) {
        if (currentProjectExt!=null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство проекта"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства проета"
                alert.headerText = "Вы действительно хотите добавить новое свойство для проекта?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentProjectPropertyCdf()
                    val id = PropertyCdfController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id).id
                    listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                    tblProjectPropertiesCdf?.items = listProjectPropertiesCdf
                    currentProjectPropertyCdf = listProjectPropertiesCdf.filter { it.id == id }.first()
                    tblProjectPropertiesCdf?.selectionModel?.select(currentProjectPropertyCdf)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val listKeys = PropertyCdfController.getKeys(currentProjectExt!!.project::class.java.simpleName, Main.ccid)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listProjectPropertiesCdf.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentProjectPropertyCdf()
                        val id = PropertyCdfController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id, key).id
                        listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                        tblProjectPropertiesCdf?.items = listProjectPropertiesCdf
                        currentProjectPropertyCdf = listProjectPropertiesCdf.filter { it.id == id }.first()
                        tblProjectPropertiesCdf?.selectionModel?.select(currentProjectPropertyCdf)
                    }
                    menu.items.add(menuItem)
                }
            }

            if (countKeysAdded > 0) {
                menu.items.add(SeparatorMenuItem())
                menuItem = MenuItem()
                menuItem.text = "Добавить все свойства для проекта"
                menuItem.onAction = EventHandler { e: ActionEvent? ->
                    saveCurrentProjectPropertyCdf()
                    listKeys.forEach { key ->
                        if (listProjectPropertiesCdf.filter { it.key == key }.isEmpty()) {
                            PropertyCdfController.editOrCreate(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id, key)
                        }
                    }
                    listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                    tblProjectPropertiesCdf?.items = listProjectPropertiesCdf
                }
                menu.items.add(menuItem)
            }

            btnProjectPropertyCdfAdd?.contextMenu = menu
            val screenBounds: Bounds = btnProjectPropertyCdfAdd!!.localToScreen(btnProjectPropertyCdfAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.getMinX()+screenBounds.getWidth(), screenBounds.getMinY())

        }
    }
    
    @FXML
    fun doFilePropertyDelete(event: ActionEvent?) {
        if (currentFileProperty!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства файла"
            alert.headerText = "Вы действительно хотите удалить свойство файла с ключом «${currentFileProperty?.key}» и значением «${currentFileProperty?.value}»?"
            alert.contentText = "В случае утвердительного ответа свойство файла будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство файла?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentFileProperty!!)
                currentFileProperty = null
                listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                tblFileProperties?.items = listFileProperties

                btnFilePropertyMoveToFirst?.isDisable = currentFileProperty == null
                btnFilePropertyMoveUp?.isDisable = currentFileProperty == null
                btnFilePropertyMoveToLast?.isDisable = currentFileProperty == null
                btnFilePropertyMoveDown?.isDisable = currentFileProperty == null
                btnFilePropertyDelete?.isDisable = currentFileProperty == null
                fldFilePropertyKey?.isDisable = currentFileProperty == null
                fldFilePropertyValue?.isDisable = currentFileProperty == null

                fldFilePropertyKey?.text = ""
                fldFilePropertyValue?.text = ""

            }
        }
    }

    @FXML
    fun doFilePropertyCdfDelete(event: ActionEvent?) {
        if (currentFilePropertyCdf!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства файла"
            alert.headerText = "Вы действительно хотите удалить свойство файла с ключом «${currentFilePropertyCdf?.key}» и значением «${currentFilePropertyCdf?.value}»?"
            alert.contentText = "В случае утвердительного ответа свойство файла будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство файла?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyCdfController.delete(currentFilePropertyCdf!!)
                currentFilePropertyCdf = null
                listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
                tblFilePropertiesCdf?.items = listFilePropertiesCdf

                btnFilePropertyCdfMoveToFirst?.isDisable = currentFilePropertyCdf == null
                btnFilePropertyCdfMoveUp?.isDisable = currentFilePropertyCdf == null
                btnFilePropertyCdfMoveToLast?.isDisable = currentFilePropertyCdf == null
                btnFilePropertyCdfMoveDown?.isDisable = currentFilePropertyCdf == null
                btnFilePropertyCdfDelete?.isDisable = currentFilePropertyCdf == null
                fldFilePropertyCdfKey?.isDisable = currentFilePropertyCdf == null
                fldFilePropertyCdfValue?.isDisable = currentFilePropertyCdf == null

                fldFilePropertyCdfKey?.text = ""
                fldFilePropertyCdfValue?.text = ""

            }
        }
    }

    @FXML
    fun doProjectPropertyDelete(event: ActionEvent?) {
        if (currentProjectProperty!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства проекта"
            alert.headerText = "Вы действительно хотите удалить свойство проекта с ключом «${currentProjectProperty?.key}» и значением «${currentProjectProperty?.value}»?"
            alert.contentText = "В случае утвердительного ответа свойство проета будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство проекта?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentProjectProperty!!)
                currentProjectProperty = null
                listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                tblProjectProperties?.items = listProjectProperties

                btnProjectPropertyMoveToFirst?.isDisable = currentProjectProperty == null
                btnProjectPropertyMoveUp?.isDisable = currentProjectProperty == null
                btnProjectPropertyMoveToLast?.isDisable = currentProjectProperty == null
                btnProjectPropertyMoveDown?.isDisable = currentProjectProperty == null
                btnProjectPropertyDelete?.isDisable = currentProjectProperty == null
                fldProjectPropertyKey?.isDisable = currentProjectProperty == null
                fldProjectPropertyValue?.isDisable = currentProjectProperty == null

                fldProjectPropertyKey?.text = ""
                fldProjectPropertyValue?.text = ""

            }
        }
    }

    
    @FXML
    fun doProjectPropertyCdfDelete(event: ActionEvent?) {
        if (currentProjectPropertyCdf!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства проекта"
            alert.headerText = "Вы действительно хотите удалить свойство проекта с ключом «${currentProjectPropertyCdf?.key}» и значением «${currentProjectPropertyCdf?.value}»?"
            alert.contentText = "В случае утвердительного ответа свойство проекта будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство проекта?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyCdfController.delete(currentProjectPropertyCdf!!)
                currentProjectPropertyCdf = null
                listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
                tblProjectPropertiesCdf?.items = listProjectPropertiesCdf

                btnProjectPropertyCdfMoveToFirst?.isDisable = currentProjectPropertyCdf == null
                btnProjectPropertyCdfMoveUp?.isDisable = currentProjectPropertyCdf == null
                btnProjectPropertyCdfMoveToLast?.isDisable = currentProjectPropertyCdf == null
                btnProjectPropertyCdfMoveDown?.isDisable = currentProjectPropertyCdf == null
                btnProjectPropertyCdfDelete?.isDisable = currentProjectPropertyCdf == null
                fldProjectPropertyCdfKey?.isDisable = currentProjectPropertyCdf == null
                fldProjectPropertyCdfValue?.isDisable = currentProjectPropertyCdf == null

                fldProjectPropertyCdfKey?.text = ""
                fldProjectPropertyCdfValue?.text = ""

            }
        }
    }
    
    @FXML
    fun doFilePropertyMoveDown(event: ActionEvent?) {
        doMoveFileProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFilePropertyMoveToFirst(event: ActionEvent?) {
        doMoveFileProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFilePropertyMoveToLast(event: ActionEvent?) {
        doMoveFileProperty(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFilePropertyMoveUp(event: ActionEvent?) {
        doMoveFileProperty(ReorderTypes.MOVE_UP)
    }

    fun doMoveFileProperty(reorderType: ReorderTypes) {
        val id = currentFileProperty?.id
        currentFileProperty?.let { PropertyController.reOrder(reorderType, it) }
        listFileProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
        tblFileProperties?.items = listFileProperties
        currentFileProperty = listFileProperties.filter { it.id == id }.first()
        tblFileProperties?.selectionModel?.select(currentFileProperty)
    }

    @FXML
    fun doFilePropertyCdfMoveDown(event: ActionEvent?) {
        doMoveFilePropertyCdf(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFilePropertyCdfMoveToFirst(event: ActionEvent?) {
        doMoveFilePropertyCdf(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFilePropertyCdfMoveToLast(event: ActionEvent?) {
        doMoveFilePropertyCdf(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFilePropertyCdfMoveUp(event: ActionEvent?) {
        doMoveFilePropertyCdf(ReorderTypes.MOVE_UP)
    }

    fun doMoveFilePropertyCdf(reorderType: ReorderTypes) {
        val id = currentFilePropertyCdf?.id
        currentFilePropertyCdf?.let { PropertyCdfController.reOrder(reorderType, it) }
        listFilePropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentFileExt!!.file::class.java.simpleName, currentFileExt!!.file.id))
        tblFilePropertiesCdf?.items = listFilePropertiesCdf
        currentFilePropertyCdf = listFilePropertiesCdf.filter { it.id == id }.first()
        tblFilePropertiesCdf?.selectionModel?.select(currentFilePropertyCdf)
    }

    @FXML
    fun doProjectPropertyMoveDown(event: ActionEvent?) {
        doMoveProjectProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doProjectPropertyMoveToFirst(event: ActionEvent?) {
        doMoveProjectProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doProjectPropertyMoveToLast(event: ActionEvent?) {
        doMoveProjectProperty(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doProjectPropertyMoveUp(event: ActionEvent?) {
        doMoveProjectProperty(ReorderTypes.MOVE_UP)
    }

    fun doMoveProjectProperty(reorderType: ReorderTypes) {
        val id = currentProjectProperty?.id
        currentProjectProperty?.let { PropertyController.reOrder(reorderType, it) }
        listProjectProperties = FXCollections.observableArrayList(PropertyController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
        tblProjectProperties?.items = listProjectProperties
        currentProjectProperty = listProjectProperties.filter { it.id == id }.first()
        tblProjectProperties?.selectionModel?.select(currentProjectProperty)
    }
    
    @FXML
    fun doBrowseFilePropertyCdfValue(event: ActionEvent?) {

        if (currentFilePropertyCdf != null) {

            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Выбор папки для свойства: ${currentFilePropertyCdf?.key}"
            val initialDirectory =
                if (IOFile(currentFilePropertyCdf!!.value).exists()) currentFilePropertyCdf!!.value else {
                    if (IOFile(currentProjectExt!!.project.folder).exists()) currentProjectExt!!.project.folder else ""
                }
            if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
            val directorySelected = directoryChooser.showDialog(Stage())

            if (directorySelected != null) {
                fldFilePropertyCdfValue?.text = directorySelected.absolutePath
            }
        }

    }

    @FXML
    fun doProjectPropertyCdfMoveDown(event: ActionEvent?) {
        doMoveProjectPropertyCdf(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doProjectPropertyCdfMoveToFirst(event: ActionEvent?) {
        doMoveProjectPropertyCdf(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doProjectPropertyCdfMoveToLast(event: ActionEvent?) {
        doMoveProjectPropertyCdf(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doProjectPropertyCdfMoveUp(event: ActionEvent?) {
        doMoveProjectPropertyCdf(ReorderTypes.MOVE_UP)
    }

    fun doMoveProjectPropertyCdf(reorderType: ReorderTypes) {
        val id = currentProjectPropertyCdf?.id
        currentProjectPropertyCdf?.let { PropertyCdfController.reOrder(reorderType, it) }
        listProjectPropertiesCdf = FXCollections.observableArrayList(PropertyCdfController.getListProperties(currentProjectExt!!.project::class.java.simpleName, currentProjectExt!!.project.id))
        tblProjectPropertiesCdf?.items = listProjectPropertiesCdf
        currentProjectPropertyCdf = listProjectPropertiesCdf.filter { it.id == id }.first()
        tblProjectPropertiesCdf?.selectionModel?.select(currentProjectPropertyCdf)
    }

    @FXML
    fun doBrowseProjectPropertyCdfValue(event: ActionEvent?) {

        if (currentProjectPropertyCdf != null) {

            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Выбор папки для свойства: ${currentProjectPropertyCdf?.key}"
            val initialDirectory =
                if (IOFile(currentProjectPropertyCdf!!.value).exists()) currentProjectPropertyCdf!!.value else {
                    if (IOFile(currentProjectExt!!.project.folder).exists()) currentProjectExt!!.project.folder else ""
                }
            if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
            val directorySelected = directoryChooser.showDialog(Stage())

            if (directorySelected != null) {
                fldProjectPropertyCdfValue?.text = directorySelected.absolutePath
            }
        }

    }
}