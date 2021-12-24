package com.svoemesto.ivfx.fxcontrollers

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.projectactions.AnalyzeFrames
import com.svoemesto.ivfx.threads.projectactions.CreateFaces
import com.svoemesto.ivfx.threads.projectactions.CreateFacesPreview
import com.svoemesto.ivfx.threads.projectactions.CreateFramesFull
import com.svoemesto.ivfx.threads.projectactions.CreateFramesMedium
import com.svoemesto.ivfx.threads.projectactions.CreateFramesSmall
import com.svoemesto.ivfx.threads.projectactions.CreateLossless
import com.svoemesto.ivfx.threads.projectactions.CreatePreview
import com.svoemesto.ivfx.threads.projectactions.CreateShots
import com.svoemesto.ivfx.threads.projectactions.DetectFaces
import com.svoemesto.ivfx.threads.projectactions.RecognizeFaces
import com.svoemesto.ivfx.utils.FaceDetection
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
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile

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
    private var colFileExtCS: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtDF: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtCF: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtCFP: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtRF: TableColumn<FileExt, String>? = null


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
    private var checkCreateShots: CheckBox? = null

    @FXML
    private var checkDetectFaces: CheckBox? = null

    @FXML
    private var checkCreateFaces: CheckBox? = null

    @FXML
    private var checkCreateFacesPreview: CheckBox? = null

    @FXML
    private var checkRecognizeFaces: CheckBox? = null

    @FXML
    private var btnTrainFaceModel: Button? = null

    @FXML
    private var pb1: ProgressBar? = null

    @FXML
    private var lblPb1: Label? = null

    @FXML
    private var pb2: ProgressBar? = null

    @FXML
    private var lblPb2: Label? = null

    companion object {
        private var currentProject: Project = Project()
        private var listFilesExt: ObservableList<FileExt> = FXCollections.observableArrayList()
        private var hostServices: HostServices? = null
    }


    private var mainStage: Stage? = null

    private var currentFileExt: FileExt? = null


    fun actionsProject(project: Project, listFilesExt: ObservableList<FileExt>, hostServices: HostServices? = null) {
        currentProject = project
        ProjectActionsFXController.listFilesExt = listFilesExt
        mainStage = Stage()
        try {
            val root = FXMLLoader.load<Parent>(ProjectEditFXController::class.java.getResource("project-actions-view.fxml"))
            mainStage?.setScene(Scene(root))
            ProjectActionsFXController.hostServices = hostServices
            mainStage?.initModality(Modality.NONE)
            mainStage?.showAndWait()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы ProjectActionsFXController.")
        mainStage = null

    }


    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна ProjectActionsFXController.")
        }

        println("Инициализация ProjectActionsFXController.")

        tblFilesExt?.selectionModel?.selectionMode = SelectionMode.MULTIPLE

//        listFilesExt = FXCollections.observableArrayList(FileController.getListFilesExt(currentProject))
        colFileExtOrder?.cellValueFactory = PropertyValueFactory("fileOrder")
        colFileExtName?.cellValueFactory = PropertyValueFactory("fileName")
        colFileExtPW?.cellValueFactory = PropertyValueFactory("hasPreviewString")
        colFileExtLL?.cellValueFactory = PropertyValueFactory("hasLosslessString")
        colFileExtFS?.cellValueFactory = PropertyValueFactory("hasFramesSmallString")
        colFileExtFM?.cellValueFactory = PropertyValueFactory("hasFramesMediumString")
        colFileExtFF?.cellValueFactory = PropertyValueFactory("hasFramesFullString")
        colFileExtAF?.cellValueFactory = PropertyValueFactory("hasAnalyzedFramesString")
        colFileExtCS?.cellValueFactory = PropertyValueFactory("hasCreatedShotsString")
        colFileExtDF?.cellValueFactory = PropertyValueFactory("hasDetectedFacesString")
        colFileExtCF?.cellValueFactory = PropertyValueFactory("hasCreatedFacesString")
        colFileExtCFP?.cellValueFactory = PropertyValueFactory("hasCreatedFacesPreviewString")
        colFileExtRF?.cellValueFactory = PropertyValueFactory("hasRecognizedFacesString")
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
            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview!! || (fileExt.hasPreview!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless!! || (fileExt.hasLossless!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall!! || (fileExt.hasFramesSmall!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium!! || (fileExt.hasFramesMedium!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull!! || (fileExt.hasFramesFull!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkAnalyzeFrames?.isSelected == true && (!fileExt.hasAnalyzedFrames!! || (fileExt.hasAnalyzedFrames!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateShots?.isSelected == true && (!fileExt.hasCreatedShots!! || (fileExt.hasCreatedShots!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkDetectFaces?.isSelected == true && (!fileExt.hasDetectedFaces!! || (fileExt.hasDetectedFaces!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFaces?.isSelected == true && (!fileExt.hasCreatedFaces!! || (fileExt.hasCreatedFaces!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkCreateFacesPreview?.isSelected == true && (!fileExt.hasCreatedFacesPreview!! || (fileExt.hasCreatedFacesPreview!! && checkReCreateIfExists?.isSelected!!))) countActions++
            if (checkRecognizeFaces?.isSelected == true && (!fileExt.hasRecognizedFaces!! || (fileExt.hasRecognizedFaces!! && checkReCreateIfExists?.isSelected!!))) countActions++
        }
        var counterPb1 = 0

        val listThreads: MutableList<Thread> = mutableListOf()

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->

            if (checkCreatePreview?.isSelected == true && (!fileExt.hasPreview!! || (fileExt.hasPreview!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreatePreview(fileExt!!, tblFilesExt!!,
                    "File: ${fileExt.file.name}, Action: Create Preview, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateLossless?.isSelected == true && (!fileExt.hasLossless!! || (fileExt.hasLossless!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateLossless(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Lossless, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesSmall?.isSelected == true && (!fileExt.hasFramesSmall!! || (fileExt.hasFramesSmall!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesSmall(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (small size 175x35), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesMedium?.isSelected == true && (!fileExt.hasFramesMedium!! || (fileExt.hasFramesMedium!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesMedium(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (medium size 720x400), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFramesFull?.isSelected == true && (!fileExt.hasFramesFull!! || (fileExt.hasFramesFull!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFramesFull(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (full size 1920x1080), Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkAnalyzeFrames?.isSelected == true && (!fileExt.hasAnalyzedFrames!! || (fileExt.hasAnalyzedFrames!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    AnalyzeFrames(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Analyze Frames, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateShots?.isSelected == true && (!fileExt.hasCreatedShots!! || (fileExt.hasCreatedShots!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateShots(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create shots, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkDetectFaces?.isSelected == true && (!fileExt.hasDetectedFaces!! || (fileExt.hasDetectedFaces!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    DetectFaces(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Detect Faces, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFaces?.isSelected == true && (!fileExt.hasCreatedFaces!! || (fileExt.hasCreatedFaces!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFaces(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Faces, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkCreateFacesPreview?.isSelected == true && (!fileExt.hasCreatedFacesPreview!! || (fileExt.hasCreatedFacesPreview!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    CreateFacesPreview(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Faces Preview, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

            if (checkRecognizeFaces?.isSelected == true && (!fileExt.hasRecognizedFaces!! || (fileExt.hasRecognizedFaces!! && checkReCreateIfExists?.isSelected!!))) {
                counterPb1++
                listThreads.add(
                    RecognizeFaces(fileExt!!, tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Recognize Faces, Issue: [${counterPb1}/${countActions}]",
                        counterPb1, countActions, lblPb1!!, pb1!!, lblPb2!!, pb2!!)
                )
            }

        }

        RunListThreads(listThreads).start()

    }

    class Embeddings(
        @SerializedName("embeddings") var vectors: Array<DoubleArray?>,
        @SerializedName("names") var tags: Array<String?>
    )

    @FXML
    fun doTrainFaceModel(event: ActionEvent?) {

        val listFacesToTrain = FaceController.getListFacesToTrain(currentProject)
        val embeddings = Embeddings( arrayOfNulls(listFacesToTrain.size), arrayOfNulls(listFacesToTrain.size))
        for ((i, face) in listFacesToTrain.withIndex()) {
            embeddings.vectors[i] = face.vector
            embeddings.tags[i] = face.personRecognizedName
        }

        val builder = GsonBuilder()
        var gson = builder.create()
        val pathToFileJSON: String = currentProject.folder + IOFile.separator + "embeddings.json"

        try {
            FileWriter(pathToFileJSON).use { fileWriter -> gson.toJson(embeddings, fileWriter) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("${faceDetectorPath.first()}:\n")
        param.add("cd \"${faceDetectorPath}\"\n")
        param.add("py")
        param.add("\"${faceDetectorPath}/train_model_json.py\"")
        param.add("-e")
        param.add("\"${pathToFileJSON}\"")
        param.add("-r")
        param.add("\"${currentProject.folder}${IOFile.separator}recognizer.pickle\"")
        param.add("-l")
        param.add("\"${currentProject.folder}${IOFile.separator}le.pickle\"")

        val cmdText = param.joinToString(separator=" ").replace("/","\\")

        println(cmdText)

        val runCmd = RunCmd(cmdText)
        runCmd.run()

    }

}
