package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.SpringConfig
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ProjectCdfController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.PropertyCdfController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.TrackController
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
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.io.IOException


class ProjectActionsFXController {
    @FXML
    private var tblFilesExt: TableView<FileExt>? = null

    @FXML
    private var colFileExtOrder: TableColumn<FileExt, Int>? = null

    @FXML
    private var colFileExtName: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtPW: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtLL: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtFS: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtFM: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtFF: TableColumn<FileExt, String>? = null

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
        private var currentProject: Project = Project()
        private var currentFileExt: FileExt? = null
        private var listFilesExt: ObservableList<FileExt> = FXCollections.observableArrayList()

        fun actionsProject(project: Project, hostServices: HostServices? = null) {
            currentProject = project
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(ProjectEditFXController::class.java.getResource("project-actions-view.fxml"))
                mainStage?.setScene(Scene(root))
                this.hostServices = hostServices
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы ProjectActionsFXController.")
            mainStage = null

        }

    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна ProjectActionsFXController.")
        }

        println("Инициализация ProjectActionsFXController.")

        tblFilesExt?.selectionModel?.selectionMode = SelectionMode.MULTIPLE

        listFilesExt = FXCollections.observableArrayList(fileController.getListFilesExt(currentProject))
        colFileExtOrder?.setCellValueFactory(PropertyValueFactory("order"))
        colFileExtName?.setCellValueFactory(PropertyValueFactory("name"))
        colFileExtPW?.setCellValueFactory(PropertyValueFactory("hasPreviewString"))
        colFileExtLL?.setCellValueFactory(PropertyValueFactory("hasLosslessString"))
        colFileExtFS?.setCellValueFactory(PropertyValueFactory("hasFramesSmallString"))
        tblFilesExt?.items = listFilesExt

    }
}
