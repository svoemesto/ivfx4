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
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.threads.CreateFramesFull
import com.svoemesto.ivfx.threads.CreateFramesMedium
import com.svoemesto.ivfx.threads.CreateFramesSmall
import com.svoemesto.ivfx.threads.CreateLossless
import com.svoemesto.ivfx.threads.CreatePreview
import com.svoemesto.ivfx.threads.RunListThreads
import javafx.application.HostServices
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
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

    @FXML
    private var checkReCreateIfExists: CheckBox? = null

    @FXML
    private var btnDoActions: Button? = null

    @FXML
    private var checkCreatePreview: CheckBox? = null

    @FXML
    private var checkCreateLossless: CheckBox? = null

    @FXML
    private var checkCreateFramesSmall: CheckBox? = null

    @FXML
    private var checkCreateFramesMedium: CheckBox? = null

    @FXML
    private var checkCreateFramesFull: CheckBox? = null

    @FXML
    private var pb1: ProgressBar? = null

    @FXML
    private var lblPb1: Label? = null

    @FXML
    private var pb2: ProgressBar? = null

    @FXML
    private var lblPb2: Label? = null


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
        colFileExtFM?.setCellValueFactory(PropertyValueFactory("hasFramesMediumString"))
        colFileExtFF?.setCellValueFactory(PropertyValueFactory("hasFramesFullString"))
        tblFilesExt?.items = listFilesExt

    }

    @FXML
    fun doActions(event: ActionEvent?) {

        val countSelectedCheckboxes: Int = (if (checkCreatePreview?.isSelected == true) 1 else 0) +
                (if (checkCreateLossless?.isSelected == true) 1 else 0) +
                (if (checkCreateFramesSmall?.isSelected == true) 1 else 0) +
                (if (checkCreateFramesMedium?.isSelected == true) 1 else 0) +
                (if (checkCreateFramesFull?.isSelected == true) 1 else 0)
        val countSelectedFiles: Int = tblFilesExt?.selectionModel?.selectedItems?.size?:0
        var countActions = 0

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->
            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview || (fileExt.hasPreview && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless || (fileExt.hasLossless && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall || (fileExt.hasFramesSmall && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium || (fileExt.hasFramesMedium && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull || (fileExt.hasFramesFull && checkReCreateIfExists?.isSelected!!))) countActions++
        }
        var counterPb1 = 0

        var listThreads: MutableList<Thread> = mutableListOf()

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->

            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview || (fileExt.hasPreview && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreatePreview(fileExt!!, fileController, tblFilesExt!!,
                    "File: ${fileExt.name}, Action: Create Preview, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless || (fileExt.hasLossless && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateLossless(fileExt!!, fileController, propertyController, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Lossless, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall || (fileExt.hasFramesSmall && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesSmall(fileExt!!, fileController, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (small size 175x35), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium || (fileExt.hasFramesMedium && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesMedium(fileExt!!, fileController, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (medium size 720x400), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull || (fileExt.hasFramesFull && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesFull(fileExt!!, fileController, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (full size 1920x1080), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }


        }

        RunListThreads(listThreads).start()

    }


}
