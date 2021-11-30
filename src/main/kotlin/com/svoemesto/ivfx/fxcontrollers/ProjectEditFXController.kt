package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.SpringConfig
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ProjectCdfController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.PropertyCdfController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.getCurrentDatabase
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.PropertyCdf
import com.svoemesto.ivfx.models.Track
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import javafx.application.HostServices
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
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.SelectionMode
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.io.IOException
import java.io.File as IOFile

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

    // PROJECT FILES

    @FXML
    private var tblFiles: TableView<File>? = null

    @FXML
    private var colFileOrder: TableColumn<File, Int>? = null

    @FXML
    private var colFileName: TableColumn<File, String>? = null

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

        private var hostServices: HostServices? = null
        private val context = AnnotationConfigApplicationContext(SpringConfig::class.java)

        private val propertyRepo = context.getBean("propertyRepo", PropertyRepo::class.java)
        private val propertyCdfRepo = context.getBean("propertyCdfRepo", PropertyCdfRepo::class.java)
        private val projectRepo = context.getBean("projectRepo", ProjectRepo::class.java)
        private val projectCdfRepo = context.getBean("projectCdfRepo", ProjectCdfRepo::class.java)
        private val fileRepo = context.getBean("fileRepo", FileRepo::class.java)
        private val fileCdfRepo = context.getBean("fileCdfRepo", FileCdfRepo::class.java)
        private val trackRepo = context.getBean("trackRepo", TrackRepo::class.java)
        private val frameRepo = context.getBean("frameRepo", FrameRepo::class.java)

        private val projectController = ProjectController(projectRepo, propertyRepo, propertyCdfRepo, projectCdfRepo, fileRepo, fileCdfRepo, frameRepo, trackRepo)
        private val projectCdfController = ProjectCdfController(projectCdfRepo)
        private val fileController = FileController(projectRepo, propertyRepo, propertyCdfRepo, projectCdfRepo, fileRepo, fileCdfRepo, frameRepo, trackRepo)
        private val fileCdfController = FileCdfController(fileCdfRepo)
        private val trackController = TrackController(trackRepo, propertyRepo, propertyCdfRepo)
        private val frameController = FrameController(frameRepo, propertyRepo, propertyCdfRepo)
        private val propertyController = PropertyController(propertyRepo)
        private val propertyCdfController = PropertyCdfController(propertyCdfRepo)

        private var mainStage: Stage? = null
        private var currentProject: Project? = Project()
        private var currentFile: File? = null
        private var listFiles: ObservableList<File> = FXCollections.observableArrayList()
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

        fun editProject(project: Project? = null, hostServices: HostServices? = null): Project? {
            if (project == null) {
                currentProject = projectController.getListProjects().firstOrNull()
            } else {
                currentProject = project
            }
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(ProjectEditFXController::class.java.getResource("project-edit-view.fxml"))
                mainStage?.setScene(Scene(root))
                this.hostServices = hostServices
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы ProjectEditFXController.")
            mainStage = null
            return currentProject
        }

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
        menuDatabase?.text = getCurrentDatabase()?.name
        mainStage?.setTitle(if (currentProject?.name == null) "Откройте или создайте проект." else "Проект: ${currentProject?.name}")
        menuDeleteProject?.isDisable = currentProject == null
        paneMain?.isVisible = currentProject != null
        paneFile?.isVisible = currentFile != null

        if (currentProject == null) return

        tblFiles?.selectionModel?.selectionMode = SelectionMode.MULTIPLE

        listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
        
        listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
        
        btnFileMoveToFirst?.isDisable = currentFile == null
        btnFileMoveUp?.isDisable = currentFile == null
        btnFileMoveToLast?.isDisable = currentFile == null
        btnFileMoveDown?.isDisable = currentFile == null
        btnFileDelete?.isDisable = currentFile == null

        fldProjectName?.text = currentProject?.name
        fldProjectShortName?.text = currentProject?.shortName
        fldProjectFolder?.text = currentProject?.folder

        listFiles = FXCollections.observableArrayList(fileController.getListFiles(currentProject!!))

        colFileOrder?.setCellValueFactory(PropertyValueFactory("order"))
        colFileName?.setCellValueFactory(PropertyValueFactory("name"))
        tblFiles?.items = listFiles

        tblFiles?.selectionModel?.select(currentFile)

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
                currentFile = null
                paneFile?.isVisible = currentFile!=null
                btnFileMoveToFirst?.isDisable = currentFile == null
                btnFileMoveUp?.isDisable = currentFile == null
                btnFileMoveToLast?.isDisable = currentFile == null
                btnFileMoveDown?.isDisable = currentFile == null
                btnFileDelete?.isDisable = currentFile == null
            } else {
                if (tblFiles?.selectionModel?.selectedItems?.size == 0) {
                    currentFile = null
                } else {
                    if (currentFile != tblFiles?.selectionModel?.selectedItems!![0]) saveCurrentFile()
                    currentFile = tblFiles?.selectionModel?.selectedItems!![0]
                }

                paneFile?.isVisible = currentFile!=null

                if (currentFile != null) {
                    btnFileDelete?.isDisable = currentFile == null
                    btnFileMoveToFirst?.isDisable = currentFile == listFiles.first()
                    btnFileMoveUp?.isDisable = currentFile == listFiles.first()
                    btnFileMoveToLast?.isDisable = currentFile == listFiles.last()
                    btnFileMoveDown?.isDisable = currentFile == listFiles.last()
                    fldFileName?.text = currentFile?.name
                    fldFileShortName?.text = currentFile?.shortName
                    fldFilePath?.text = currentFile?.path

                    listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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

                    listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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

                    listTracks = FXCollections.observableArrayList(trackController.getListTracks(currentFile!!))
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
                listTrackProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentTrack!!::class.java.simpleName, currentTrack!!.id))
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

            fldFilePropertyCdfKey?.isDisable = currentFilePropertyCdf == null
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

            fldProjectPropertyCdfKey?.isDisable = currentProjectPropertyCdf == null
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
            val cell: TableCell<Property, String> =
                TableCell<Property, String>()
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
            val cell: TableCell<PropertyCdf, String> =
                TableCell<PropertyCdf, String>()
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
            val cell: TableCell<Property, String> =
                TableCell<Property, String>()
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
            val cell: TableCell<PropertyCdf, String> =
                TableCell<PropertyCdf, String>()
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
            val cell: TableCell<Property, String> =
                TableCell<Property, String>()
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
                        hostServices!!.showDocument(if (fld == null || currentFilePropertyCdf?.value != "") currentFilePropertyCdf?.value else fileController.getCdfFolder(currentFile!!, fld, true))
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
                        hostServices!!.showDocument(if (fld == null || currentProjectPropertyCdf?.value != "") currentProjectPropertyCdf?.value else projectController.getCdfFolder(currentProject!!, fld, true))
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
                        trackRepo.save(currentTrack!!)
                        tblTracks?.refresh()
                    }
                }
            }
        }
        
    }

    fun saveCurrentFileProperty() {

        if (currentFileProperty != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldFilePropertyKey?.text ?: ""
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
                propertyRepo.save(currentFileProperty!!)
                tblFileProperties?.refresh()
            }

        }
    }

    fun saveCurrentFilePropertyCdf() {

        if (currentFilePropertyCdf != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldFilePropertyCdfKey?.text ?: ""
            if (tmp != currentFilePropertyCdf?.key) {
                currentFilePropertyCdf?.key = tmp
                needToSave = true
            }

            tmp = fldFilePropertyCdfValue?.text ?: ""
            if (tmp != currentFilePropertyCdf?.value) {
                currentFilePropertyCdf?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                propertyCdfRepo.save(currentFilePropertyCdf!!)
                tblFilePropertiesCdf?.refresh()
            }

        }
    }

    fun saveCurrentProjectProperty() {

        if (currentProjectProperty != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldProjectPropertyKey?.text ?: ""
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
                propertyRepo.save(currentProjectProperty!!)
                tblProjectProperties?.refresh()
            }

        }
    }
    
    fun saveCurrentProjectPropertyCdf() {

        if (currentProjectPropertyCdf != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldProjectPropertyCdfKey?.text ?: ""
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
                propertyCdfRepo.save(currentProjectPropertyCdf!!)
                tblProjectPropertiesCdf?.refresh()
            }

        }
    }
    
    fun saveCurrentFile() {
        if (currentFile != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldFileName?.text ?: ""
            if (tmp != currentFile?.name) {
                currentFile?.name = tmp
                needToSave = true
            }

            tmp = fldFileShortName?.text ?: ""
            if (tmp != currentFile?.shortName) {
                currentFile?.shortName = tmp
                needToSave = true
            }

            tmp = fldFilePath?.text ?: ""
            if (tmp != currentFile?.path) {
                currentFile?.path = tmp
                needToSave = true
            }

            if (needToSave) {
                fileRepo.save(currentFile!!)
                tblFiles?.refresh()
            }

        }
    }

    fun saveCurrentProject() {
        if (currentProject != null) {
            var needToSave: Boolean = false
            var tmp = ""

            tmp = fldProjectName?.text ?: ""
            if (tmp != currentProject?.name) {
                currentProject?.name = tmp
                needToSave = true
            }

            tmp = fldProjectShortName?.text ?: ""
            if (tmp != currentProject?.shortName) {
                currentProject?.shortName = tmp
                needToSave = true
            }

            tmp = fldProjectFolder?.text ?: ""
            if (tmp != currentProject?.folder) {
                currentProject?.folder = tmp
                needToSave = true
            }

            if (needToSave) {
                projectRepo.save(currentProject!!)
                mainStage?.setTitle("Проект: ${currentProject?.name}")
            }

        }
    }

    @FXML
    fun doGetFileTracksFromMediaInfo(event: ActionEvent?) {
        if (currentFile != null) {
            trackController.createTracksFromMediaInfo(currentFile!!)
            listTracks = FXCollections.observableArrayList(trackController.getListTracks(currentFile!!))
            tblTracks?.items = listTracks
        }
    }

    @FXML
    fun doMenuDeleteProject(event: ActionEvent?) {

        if (currentProject!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление проекта"
            alert.headerText = "Вы действительно хотите удалить проект «${currentProject?.name}»?"
            alert.contentText = "В случае утвердительного ответа проект будет удален из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить проект?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                projectController.delete(currentProject!!)
                currentProject = null
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
        currentProject = projectController.create()
        initialize()
    }

    @FXML
    fun doMenuOpen(event: ActionEvent?) {
        saveCurrentFile()
        saveCurrentProject()
        currentProject = ProjectSelectFXController.getProject(currentProject)
        initialize()
    }

    @FXML
    fun doSelectDatabase(event: ActionEvent?) {
    }

    @FXML
    fun doSelectProjectFolder(event: ActionEvent?) {

        val directoryChooser = DirectoryChooser()
        val initialDirectory = if (currentProject != null) IOFile(currentProject?.folder!!).absolutePath?:"" else ""
        if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
        val directorySelected = directoryChooser.showDialog(Stage())
        if (directorySelected != null) {
            fldProjectFolder?.text = directorySelected.absolutePath
            currentProject?.folder = directorySelected.absolutePath
            projectRepo.save(currentProject!!)
        }
    }

    @FXML
    fun doSelectFilePath(event: ActionEvent?) {

        currentFile?.let { fileRepo.save(currentFile!!) }

        val fileChooser = FileChooser()
        fileChooser.title = "Выберите файл"
        fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("All Files", "*.*"))
        val initialDirectory = if (currentFile != null) IOFile(currentFile?.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) fileChooser.initialDirectory = IOFile(initialDirectory)
        val ioFile = fileChooser.showOpenDialog(Stage())
        if (ioFile != null && currentFile != null) {
            currentFile?.path = ioFile.absolutePath
            fldFilePath?.text = currentFile?.path
            fileRepo.save(currentFile!!)
        }

    }

    @FXML
    fun doFileAdd(event: ActionEvent?) {
        currentFile?.let { fileRepo.save(currentFile!!) }

        val fileChooser = FileChooser()
        fileChooser.title = "Добавить файл к проекту"
        fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("All Files", "*.*"))
        val initialDirectory = if (currentFile != null) IOFile(currentFile?.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) fileChooser.initialDirectory = IOFile(initialDirectory)
        val ioFile = fileChooser.showOpenDialog(Stage())
        if (ioFile != null) {
            var findedFile = fileController.getListFiles(currentProject!!).filter { it.path == ioFile.absolutePath }.firstOrNull()
            if (findedFile != null) {
                tblFiles?.selectionModel?.select(findedFile)
            } else {
                var file = fileController.create(currentProject!!)
                file.path = ioFile.absolutePath
                file.name = ioFile.nameWithoutExtension
                fileRepo.save(file)
                val id = file.id
                listFiles = FXCollections.observableArrayList(fileController.getListFiles(currentProject!!))
                tblFiles?.items = listFiles
                currentFile = listFiles.filter { it.id == id }.first()
                tblFiles?.selectionModel?.select(currentFile)
            }
        }
    }

    @FXML
    fun doFileAddFilesFromFolder(event: ActionEvent?) {

        saveCurrentFileProperty()
        saveCurrentFile()

        val directoryChooser = DirectoryChooser()
        val initialDirectory = if (currentFile != null) IOFile(currentFile?.path!!).parent?:"" else ""
        if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
        val directorySelected = directoryChooser.showDialog(Stage())
        if (directorySelected != null) {
            directorySelected.listFiles().forEach { ioFile ->
                if (fileController.getListFiles(currentProject!!).filter { it.path == ioFile.absolutePath }.count() == 0) {
                    var file = fileController.create(currentProject!!)
                    file.path = ioFile.absolutePath
                    file.name = ioFile.nameWithoutExtension
                    fileRepo.save(file)
                }
            }
            listFiles = FXCollections.observableArrayList(fileController.getListFiles(currentProject!!))
            tblFiles?.items = listFiles
        }

    }

    @FXML
    fun doFileDelete(event: ActionEvent?) {
        if (currentFile!=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление файла"
            alert.headerText = "Вы действительно хотите удалить файл «${currentFile?.name}»?"
            alert.contentText = "В случае утвердительного ответа файл будет удален из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить файл?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                fileController.delete(currentFile!!)
                currentFile = null
                listFiles = FXCollections.observableArrayList(fileController.getListFiles(currentProject!!))
                tblFiles?.items = listFiles

                paneFile?.isVisible = currentFile != null
                btnFileMoveToFirst?.isDisable = currentFile == null
                btnFileMoveUp?.isDisable = currentFile == null
                btnFileMoveToLast?.isDisable = currentFile == null
                btnFileMoveDown?.isDisable = currentFile == null
                btnFileDelete?.isDisable = currentFile == null

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
        val id = currentFile?.id
        currentFile?.let { fileController.reOrder(reorderType, it) }
        listFiles = FXCollections.observableArrayList(fileController.getListFiles(currentProject!!))
        tblFiles?.items = listFiles
        currentFile = listFiles.filter { it.id == id }.first()
        tblFiles?.selectionModel?.select(currentFile)
    }


    @FXML
    fun doFilePropertyAdd(event: ActionEvent?) {
        if (currentFile!=null) {
            var menu: ContextMenu = ContextMenu()

            var menuItem: MenuItem = MenuItem()

            menuItem.text = "Добавить новое свойство файла"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства файла"
                alert.headerText = "Вы действительно хотите добавить новое свойство для файла?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentFileProperty()
                    val id = propertyController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id).id
                    listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
                    tblFileProperties?.items = listFileProperties
                    currentFileProperty = listFileProperties.filter { it.id == id }.first()
                    tblFileProperties?.selectionModel?.select(currentFileProperty)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            var listKeys = propertyRepo.getKeys(currentFile!!::class.java.simpleName)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listFileProperties.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentFileProperty()
                        val id = propertyController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id, key).id
                        listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
                            val id = propertyController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id, key).id
                        }
                    }
                    listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
                    tblFileProperties?.items = listFileProperties
                }
                menu.items.add(menuItem)
            }

            btnFilePropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnFilePropertyAdd!!.localToScreen(btnFilePropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.getMinX()+screenBounds.getWidth(), screenBounds.getMinY())

        }
    }

    @FXML
    fun doFilePropertyCdfAdd(event: ActionEvent?) {
        if (currentFile!=null) {
            var menu: ContextMenu = ContextMenu()

            var menuItem: MenuItem = MenuItem()

            menuItem.text = "Добавить новое свойство файла"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства файла"
                alert.headerText = "Вы действительно хотите добавить новое свойство для файла?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentFilePropertyCdf()
                    val id = propertyCdfController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id).id
                    listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
                    tblFilePropertiesCdf?.items = listFilePropertiesCdf
                    currentFilePropertyCdf = listFilePropertiesCdf.filter { it.id == id }.first()
                    tblFilePropertiesCdf?.selectionModel?.select(currentFilePropertyCdf)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            var listKeys = propertyCdfRepo.getKeys(currentFile!!::class.java.simpleName, Main.ccid)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listFilePropertiesCdf.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentFilePropertyCdf()
                        val id = propertyCdfController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id, key).id
                        listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
                            val id = propertyCdfController.editOrCreate(currentFile!!::class.java.simpleName, currentFile!!.id, key).id
                        }
                    }
                    listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
        if (currentProject!=null) {
            var menu: ContextMenu = ContextMenu()

            var menuItem: MenuItem = MenuItem()

            menuItem.text = "Добавить новое свойство проекта"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства проекта"
                alert.headerText = "Вы действительно хотите добавить новое свойство для проекта?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentProjectProperty()
                    val id = propertyController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id).id
                    listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
                    tblProjectProperties?.items = listProjectProperties
                    currentProjectProperty = listProjectProperties.filter { it.id == id }.first()
                    tblProjectProperties?.selectionModel?.select(currentProjectProperty)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            var listKeys = propertyRepo.getKeys(currentProject!!::class.java.simpleName)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listProjectProperties.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentProjectProperty()
                        val id = propertyController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id, key).id
                        listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                            val id = propertyController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id, key).id
                        }
                    }
                    listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
        if (currentProject!=null) {
            var menu: ContextMenu = ContextMenu()

            var menuItem: MenuItem = MenuItem()

            menuItem.text = "Добавить новое свойство проекта"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства проета"
                alert.headerText = "Вы действительно хотите добавить новое свойство для проекта?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentProjectPropertyCdf()
                    val id = propertyCdfController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id).id
                    listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
                    tblProjectPropertiesCdf?.items = listProjectPropertiesCdf
                    currentProjectPropertyCdf = listProjectPropertiesCdf.filter { it.id == id }.first()
                    tblProjectPropertiesCdf?.selectionModel?.select(currentProjectPropertyCdf)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            var listKeys = propertyCdfRepo.getKeys(currentProject!!::class.java.simpleName, Main.ccid)
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listProjectPropertiesCdf.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentProjectPropertyCdf()
                        val id = propertyCdfController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id, key).id
                        listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                            val id = propertyCdfController.editOrCreate(currentProject!!::class.java.simpleName, currentProject!!.id, key).id
                        }
                    }
                    listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                propertyController.delete(currentFileProperty!!)
                currentFileProperty = null
                listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
                propertyCdfController.delete(currentFilePropertyCdf!!)
                currentFilePropertyCdf = null
                listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
                propertyController.delete(currentProjectProperty!!)
                currentProjectProperty = null
                listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                propertyCdfController.delete(currentProjectPropertyCdf!!)
                currentProjectPropertyCdf = null
                listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
        currentFileProperty?.let { propertyController.reOrder(reorderType, it) }
        listFileProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
        currentFilePropertyCdf?.let { propertyCdfController.reOrder(reorderType, it) }
        listFilePropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentFile!!::class.java.simpleName, currentFile!!.id))
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
        currentProjectProperty?.let { propertyController.reOrder(reorderType, it) }
        listProjectProperties = FXCollections.observableArrayList(propertyController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                    if (IOFile(currentProject!!.folder).exists()) currentProject!!.folder else ""
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
        currentProjectPropertyCdf?.let { propertyCdfController.reOrder(reorderType, it) }
        listProjectPropertiesCdf = FXCollections.observableArrayList(propertyCdfController.getListProperties(currentProject!!::class.java.simpleName, currentProject!!.id))
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
                    if (IOFile(currentProject!!.folder).exists()) currentProject!!.folder else ""
                }
            if (IOFile(initialDirectory).exists()) directoryChooser.initialDirectory = IOFile(initialDirectory)
            val directorySelected = directoryChooser.showDialog(Stage())

            if (directorySelected != null) {
                fldProjectPropertyCdfValue?.text = directorySelected.absolutePath
            }
        }

    }
}