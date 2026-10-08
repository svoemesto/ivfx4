package com.svoemesto.ivfx.fxcontrollers

import com.google.gson.annotations.SerializedName
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.projectactions.AnalyzeFrames
import com.svoemesto.ivfx.threads.projectactions.CreateConcat
import com.svoemesto.ivfx.threads.projectactions.CreateFaces
import com.svoemesto.ivfx.threads.projectactions.CreateFacesPreview
import com.svoemesto.ivfx.threads.projectactions.CreateFramesFull
import com.svoemesto.ivfx.threads.projectactions.CreateFramesMedium
import com.svoemesto.ivfx.threads.projectactions.CreateFramesSmall
import com.svoemesto.ivfx.threads.projectactions.CreateLossless
import com.svoemesto.ivfx.threads.projectactions.CreatePreview
import com.svoemesto.ivfx.threads.projectactions.CreateShots
import com.svoemesto.ivfx.threads.projectactions.CreateShotsCompressedWithAudio
import com.svoemesto.ivfx.threads.projectactions.CreateShotsLosslessWithAudio
import com.svoemesto.ivfx.threads.projectactions.CreateShotsLosslessWithoutAudio
import com.svoemesto.ivfx.threads.projectactions.DetectFaces
import com.svoemesto.ivfx.threads.projectactions.RecheckFaces
import com.svoemesto.ivfx.threads.projectactions.RecognizeFaces
import com.svoemesto.ivfx.threads.projectactions.TrackFaces
import com.svoemesto.ivfx.utils.Trace
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
    private var colFileExtSCA: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtSLA: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtSLN: TableColumn<FileExt, String>? = null

    @FXML
    private var colFileExtCC: TableColumn<FileExt, String>? = null

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
    private var checkTrackFaces: CheckBox? = null

    @FXML
    private var checkRecheckFaces: CheckBox? = null

    @FXML
    private var checkCreateShotsCompressedWithAudio: CheckBox? = null

    @FXML
    private var checkCreateShotsLosslessWithAudio: CheckBox? = null

    @FXML
    private var checkCreateShotsLosslessWithoutAudio: CheckBox? = null

    @FXML
    private var checkCreateConcat: CheckBox? = null

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

    /**
     * Запущенная цепочка операций.
     *
     * Держится именно для прерывания: без ссылки прервать цепочку было
     * некому, а окно при этом закрывалось и работа продолжалась.
     */
    private var runListThreadsOperations: RunListThreads? = null

    private var currentFileExt: FileExt? = null

    fun actionsProject(
        project: Project,
        listFilesExt: ObservableList<FileExt>,
        hostServices: HostServices? = null,
    ) {
        currentProject = project
        ProjectActionsFXController.listFilesExt = listFilesExt
        mainStage = Stage()
        try {
            val loader = FXMLLoader(ProjectEditFXController::class.java.getResource("project-actions-view.fxml"))
            loader.setController(this)
            val root = loader.load<Parent>()
            mainStage?.setScene(Scene(root))
            ProjectActionsFXController.hostServices = hostServices
            mainStage?.initModality(Modality.NONE)
            mainStage?.showAndWait()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы ProjectActionsFXController.")
        // Окно закрылось — прерываем работу, если она ещё идёт.
        //
        // Раньше здесь не было ничего, и закрытие окна операций ничего не
        // останавливало: скрипт продолжал работать, а в журнале не
        // появлялось ни единой строки. Проверено на живом прогоне — окно
        // закрыли во время DetectFaces, а detect_faces_in_folder.py продолжал.
        interruptRunningOperations()
        mainStage = null
    }

    /**
     * Прерывает цепочку операций, если она запущена.
     *
     * Ссылка нужна потому, что цепочка запускается как
     * `RunListThreads(listThreads).start()` — без сохранения ссылки
     * обратиться к ней было нельзя, то есть прерывать её было некому.
     */
    private fun interruptRunningOperations() {
        val chain = runListThreadsOperations
        if (chain != null && chain.isAlive) {
            println("Окно закрыто, прерываю операцию.")
            chain.interrupt()
        }
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
        colFileExtSCA?.cellValueFactory = PropertyValueFactory("hasShotsCompressedWithAudioString")
        colFileExtSLA?.cellValueFactory = PropertyValueFactory("hasShotsLosslessWithAudioString")
        colFileExtSLN?.cellValueFactory = PropertyValueFactory("hasShotsLosslessWithoutAudioString")
        colFileExtCC?.cellValueFactory = PropertyValueFactory("hasConcatString")
        tblFilesExt?.items = listFilesExt

        // Полосы и подписи показываются всегда, в том числе когда ничего не
        // выполняется. Раньше все четыре элемента скрывались при старте, и в
        // покое их не было видно совсем: из-за этого нельзя было понять, что
        // прогресс здесь вообще есть. В покое подписи показывают состояние,
        // полосы пустые.
        pb1?.isVisible = true
        pb2?.isVisible = true
        lblPb1?.isVisible = true
        lblPb2?.isVisible = true
        pb1?.progress = 0.0
        pb2?.progress = 0.0
        lblPb1?.text = ""
        lblPb2?.text = "Ready"

        watchForDoActionsAvailability()

        // Раскладку смотрим не сразу, а через секунду: на момент initialize()
        // окно ещё не прошло первый расчёт размеров, и все поля нулевые.
        // Именно нулевые размеры у детей VBox уже сжимали полосы прогресса,
        // поэтому галочки проверяем фактической геометрией, а не догадкой.
        javafx.animation
            .Timeline(
                javafx.animation.KeyFrame(
                    javafx.util.Duration.seconds(1.5),
                    javafx.event.EventHandler { _ ->
                        val parts = mutableListOf<String>()
                        for (cb in actionCheckBoxes()) {
                            parts.add("${cb.text}=h${cb.height.toInt()}/v${if (cb.isVisible) 1 else 0}")
                        }
                        println("[TR-раскладка] галочек ${actionCheckBoxes().size}: ${parts.joinToString(" ")}")
                        println(
                            "[TR-раскладка] кнопка h=${btnDoActions?.height?.toInt()} " +
                                "включена=${if (btnDoActions?.isDisable == true) "нет" else "да"}",
                        )
                    },
                ),
            ).apply { cycleCount = 1 }
            .let { it.playFromStart() }
    }

    /**
     * Все галочки-действия: их состояние определяет, можно ли запускать.
     */
    private fun actionCheckBoxes(): List<CheckBox> =
        listOfNotNull(
            checkCreatePreview,
            checkCreateLossless,
            checkCreateFramesSmall,
            checkCreateFramesMedium,
            checkCreateFramesFull,
            checkAnalyzeFrames,
            checkCreateShots,
            checkDetectFaces,
            checkCreateFaces,
            checkCreateFacesPreview,
            checkRecognizeFaces,
            checkTrackFaces,
            checkRecheckFaces,
            checkCreateShotsCompressedWithAudio,
            checkCreateShotsLosslessWithAudio,
            checkCreateShotsLosslessWithoutAudio,
            checkCreateConcat,
        )

    /**
     * Кнопка «Do actions» доступна, только когда выделена хотя бы одна строка
     * таблицы и отмечено хотя бы одно действие.
     *
     * Раньше это не проверялось: если строка не выделена, цикл по действиям
     * проходил пустым, кнопка нажималась, и ничего не происходило — ни ошибки,
     * ни записи в журнале. Приходилось гадать, что нажатие не сработало.
     */
    private fun updateDoActionsAvailability() {
        val anyRowSelected = (tblFilesExt?.selectionModel?.selectedItems?.size ?: 0) > 0
        val checked = actionCheckBoxes().filter { it.isSelected }.map { it.text }
        val anyActionChecked = checked.isNotEmpty()
        btnDoActions?.isDisable = !(anyRowSelected && anyActionChecked)
        // Диагностика: по одному клику видно, что именно помешало кнопке.
        // Без неё приходится гадать — выделена ли строка и отмечена ли хоть
        // одна галочка, из формы это не различить.
        println(
            "[TR-диагностика] строк выделено: ${tblFilesExt?.selectionModel?.selectedItems?.size ?: 0}, " +
                "отмечено: ${checked.size} $checked, кнопка " +
                "${if (btnDoActions?.isDisable == true) "выключена" else "включена"}",
        )
    }

    private fun watchForDoActionsAvailability() {
        updateDoActionsAvailability()
        // Слушатель вешаем на сам список выделения: selectedItemsProperty()
        // в Kotlin не синтезируется как свойство, а список выделения —
        // обычный ObservableList, на который подписаться можно.
        tblFilesExt?.selectionModel?.selectedItems?.addListener(
            javafx.collections.ListChangeListener { updateDoActionsAvailability() },
        )
        actionCheckBoxes().forEach {
            it.selectedProperty().addListener { _, _, _ ->
                updateDoActionsAvailability()
            }
        }
    }

    @FXML
    fun doActions(event: ActionEvent?) {
        Trace.action("doActions")

        var countActions = 0

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->
            if (checkCreatePreview?.isSelected == true &&
                (!fileExt.hasPreview!! || (fileExt.hasPreview!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateLossless?.isSelected == true &&
                (!fileExt.hasLossless!! || (fileExt.hasLossless!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateFramesSmall?.isSelected == true &&
                (!fileExt.hasFramesSmall!! || (fileExt.hasFramesSmall!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateFramesMedium?.isSelected == true &&
                (!fileExt.hasFramesMedium!! || (fileExt.hasFramesMedium!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateFramesFull?.isSelected == true &&
                (!fileExt.hasFramesFull!! || (fileExt.hasFramesFull!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkAnalyzeFrames?.isSelected == true &&
                (!fileExt.hasAnalyzedFrames!! || (fileExt.hasAnalyzedFrames!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateShots?.isSelected == true &&
                (!fileExt.hasCreatedShots!! || (fileExt.hasCreatedShots!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkDetectFaces?.isSelected == true &&
                (!fileExt.hasDetectedFaces!! || (fileExt.hasDetectedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateFaces?.isSelected == true &&
                (!fileExt.hasCreatedFaces!! || (fileExt.hasCreatedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateFacesPreview?.isSelected == true &&
                (!fileExt.hasCreatedFacesPreview!! || (fileExt.hasCreatedFacesPreview!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkRecognizeFaces?.isSelected == true &&
                (!fileExt.hasRecognizedFaces!! || (fileExt.hasRecognizedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkTrackFaces?.isSelected == true) countActions++
            if (checkRecheckFaces?.isSelected == true) countActions++

            if (checkCreateShotsCompressedWithAudio?.isSelected == true &&
                (!fileExt.hasShotsCompressedWithAudio!! || (fileExt.hasShotsCompressedWithAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateShotsLosslessWithAudio?.isSelected == true &&
                (!fileExt.hasShotsLosslessWithAudio!! || (fileExt.hasShotsLosslessWithAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateShotsLosslessWithoutAudio?.isSelected == true &&
                (!fileExt.hasShotsLosslessWithoutAudio!! || (fileExt.hasShotsLosslessWithoutAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
            if (checkCreateConcat?.isSelected == true &&
                (!fileExt.hasConcat!! || (fileExt.hasConcat!! && checkReCreateIfExists?.isSelected!!))
            ) {
                countActions++
            }
        }
        var counterPb1 = 0

        val listThreads: MutableList<Thread> = mutableListOf()

        tblFilesExt?.selectionModel?.selectedItems?.forEach { fileExt ->

            if (checkCreatePreview?.isSelected == true &&
                (!fileExt.hasPreview!! || (fileExt.hasPreview!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreatePreview(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Preview, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateLossless?.isSelected == true &&
                (!fileExt.hasLossless!! || (fileExt.hasLossless!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateLossless(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Lossless, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateFramesSmall?.isSelected == true &&
                (!fileExt.hasFramesSmall!! || (fileExt.hasFramesSmall!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateFramesSmall(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (small size 175x35), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateFramesMedium?.isSelected == true &&
                (!fileExt.hasFramesMedium!! || (fileExt.hasFramesMedium!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateFramesMedium(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (medium size 720x400), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateFramesFull?.isSelected == true &&
                (!fileExt.hasFramesFull!! || (fileExt.hasFramesFull!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateFramesFull(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Frames (full size 1920x1080), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkAnalyzeFrames?.isSelected == true &&
                (!fileExt.hasAnalyzedFrames!! || (fileExt.hasAnalyzedFrames!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    AnalyzeFrames(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Analyze Frames, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateShots?.isSelected == true &&
                (!fileExt.hasCreatedShots!! || (fileExt.hasCreatedShots!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateShots(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create shots, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkDetectFaces?.isSelected == true &&
                (!fileExt.hasDetectedFaces!! || (fileExt.hasDetectedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    DetectFaces(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Detect Faces, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateFaces?.isSelected == true &&
                (!fileExt.hasCreatedFaces!! || (fileExt.hasCreatedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateFaces(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Faces, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateFacesPreview?.isSelected == true &&
                (!fileExt.hasCreatedFacesPreview!! || (fileExt.hasCreatedFacesPreview!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateFacesPreview(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Faces Preview, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkRecognizeFaces?.isSelected == true &&
                (!fileExt.hasRecognizedFaces!! || (fileExt.hasRecognizedFaces!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    RecognizeFaces(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Recognize Faces, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkTrackFaces?.isSelected == true) {
                counterPb1++
                listThreads.add(
                    TrackFaces(
                        fileExt!!,
                        "File: ${fileExt.file.name}, Action: Track Faces, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkRecheckFaces?.isSelected == true) {
                counterPb1++
                listThreads.add(
                    RecheckFaces(
                        fileExt!!,
                        "File: ${fileExt.file.name}, Action: Recheck Faces, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateShotsCompressedWithAudio?.isSelected == true &&
                (!fileExt.hasShotsCompressedWithAudio!! || (fileExt.hasShotsCompressedWithAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateShotsCompressedWithAudio(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Shots video files (compressed, with audio), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateShotsLosslessWithAudio?.isSelected == true &&
                (!fileExt.hasShotsLosslessWithAudio!! || (fileExt.hasShotsLosslessWithAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateShotsLosslessWithAudio(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Shots video files (lossless, with audio), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateShotsLosslessWithoutAudio?.isSelected == true &&
                (!fileExt.hasShotsLosslessWithoutAudio!! || (fileExt.hasShotsLosslessWithoutAudio!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateShotsLosslessWithoutAudio(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create Shots video files (lossless, without audio), Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }

            if (checkCreateConcat?.isSelected == true &&
                (!fileExt.hasConcat!! || (fileExt.hasConcat!! && checkReCreateIfExists?.isSelected!!))
            ) {
                counterPb1++
                listThreads.add(
                    CreateConcat(
                        fileExt!!,
                        tblFilesExt!!,
                        "File: ${fileExt.file.name}, Action: Create concatinated video file, Issue: [$counterPb1/$countActions]",
                        counterPb1,
                        countActions,
                        lblPb1!!,
                        pb1!!,
                        lblPb2!!,
                        pb2!!,
                    ),
                )
            }
        }

        val chain = RunListThreads(listThreads)
        runListThreadsOperations = chain
        chain.start()
    }

    class Embeddings(
        @SerializedName("embeddings") var vectors: Array<DoubleArray?>,
        @SerializedName("names") var tags: Array<String?>,
    )

    // Шаг обучения модели распознавания удалён вместе с кнопкой
    // «Train face model» и скриптом train_model_json.py.
    //
    // Раньше отмеченные лица превращались в обучающую выборку, по ней
    // подгонялся линейный SVM, он вместе с кодировщиком меток сохранялся
    // в два файла, а при распознавании для каждого лица вызывался
    // predict_proba.
    //
    // Сейчас распознавание идёт сравнением с отмеченными лицами напрямую:
    // вектор признаков у лица уже посчитан при поиске лиц и лежит в базе,
    // достаточно взять косинус угла с векторами отмеченных лиц. Ни модель
    // обучать, ни файлы её хранить не нужно.
    //
    // Отметка лиц в интерфейсе при этом остаётся: галерея строится из
    // отмеченных лиц, и без них распознавать нечего.
}
