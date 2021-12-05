package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.SpringConfig
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ProjectCdfController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.PropertyCdfController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.threads.AnalyzeFrames
import com.svoemesto.ivfx.threads.CreateFramesFull
import com.svoemesto.ivfx.threads.CreateFramesMedium
import com.svoemesto.ivfx.threads.CreateFramesSmall
import com.svoemesto.ivfx.threads.CreateLossless
import com.svoemesto.ivfx.threads.CreatePreview
import com.svoemesto.ivfx.threads.DetectFaces
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
    private var colFileExtAF: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtDF: TableColumn<FileExt, String>? = null

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
    private var checkAnalyzeFrames: CheckBox? = null

    @FXML
    private var checkDetectFaces: CheckBox? = null

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

        listFilesExt = FXCollections.observableArrayList(Main.fileController.getListFilesExt(currentProject))
        colFileExtOrder?.setCellValueFactory(PropertyValueFactory("order"))
        colFileExtName?.setCellValueFactory(PropertyValueFactory("name"))
        colFileExtPW?.setCellValueFactory(PropertyValueFactory("hasPreviewString"))
        colFileExtLL?.setCellValueFactory(PropertyValueFactory("hasLosslessString"))
        colFileExtFS?.setCellValueFactory(PropertyValueFactory("hasFramesSmallString"))
        colFileExtFM?.setCellValueFactory(PropertyValueFactory("hasFramesMediumString"))
        colFileExtFF?.setCellValueFactory(PropertyValueFactory("hasFramesFullString"))
        colFileExtDF?.setCellValueFactory(PropertyValueFactory("hasFacesString"))
        colFileExtAF?.setCellValueFactory(PropertyValueFactory("hasAnalyzedFramesString"))
        tblFilesExt?.items = listFilesExt

        pb1?.isVisible = false
        pb2?.isVisible = false
        lblPb1?.isVisible = false
        lblPb2?.isVisible = false


    }

    @FXML
    fun doActions(event: ActionEvent?) {

        var countActions = 0

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->
            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview || (fileExt.hasPreview && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless || (fileExt.hasLossless && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall || (fileExt.hasFramesSmall && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium || (fileExt.hasFramesMedium && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull || (fileExt.hasFramesFull && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkAnalyzeFrames?.isSelected == true && (!fileExt.hasAnalyzedFrames || (fileExt.hasAnalyzedFrames && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkDetectFaces?.isSelected == true && (!fileExt.hasFaces || (fileExt.hasFaces && checkReCreateIfExists?.isSelected!!))) countActions++
        }
        var counterPb1 = 0

        val listThreads: MutableList<Thread> = mutableListOf()

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->

            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview || (fileExt.hasPreview && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreatePreview(fileExt!!, tblFilesExt!!,
                    "File: ${fileExt.name}, Action: Create Preview, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless || (fileExt.hasLossless && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateLossless(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Lossless, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall || (fileExt.hasFramesSmall && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesSmall(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (small size 175x35), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium || (fileExt.hasFramesMedium && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesMedium(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (medium size 720x400), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull || (fileExt.hasFramesFull && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesFull(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Create Frames (full size 1920x1080), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkAnalyzeFrames?.isSelected == true && (!fileExt.hasAnalyzedFrames || (fileExt.hasAnalyzedFrames && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    AnalyzeFrames(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Analyze Frames, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkDetectFaces?.isSelected == true && (!fileExt.hasFaces || (fileExt.hasFaces && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    DetectFaces(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.name}, Action: Detect Faces, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

        }

        RunListThreads(listThreads).start()

    }


}
