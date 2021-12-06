package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.FrameController.FrameExt
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.utils.ConvertToFxImage
import javafx.application.HostServices
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Slider
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
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
    private var tblPages: TableView<MatrixPage>? = null

    @FXML
    private var colDurationStart: TableColumn<MatrixPage, String>? = null

    @FXML
    private var colDurationEnd: TableColumn<MatrixPage, String>? = null

    @FXML
    private var colFrameStart: TableColumn<MatrixPage, String>? = null

    @FXML
    private var colFrameEnd: TableColumn<MatrixPage, String>? = null

    @FXML
    private var lblFrameFull: Label? = null

    @FXML
    private var contextMenuFrameFull: ContextMenu? = null

    @FXML
    private var btnOK: Button? = null

    @FXML
    private var paneCenter: Pane? = null

    @FXML
    private var pb: ProgressBar? = null

    @FXML
    private val slider: Slider? = null

    companion object {

        private var hostServices: HostServices? = null
        private var currentFile: File? = null
        private var mainStage: Stage? = null

        private const val fxBorderDefault = "-fx-border-color:#0f0f0f;-fx-border-width:1" // стиль бордюра лейбла по-умолчанию
        private const val fxBorderFocused = "-fx-border-color:YELLOW;-fx-border-width:1" // стиль бордюра лейбла в фокусе
        private const val fxBorderSelected = "-fx-border-color:RED;-fx-border-width:1" // стиль бордюра лейбла выбранного
        private const val COUNT_LOADED_PAGE_BEFORE_CURRENT = 50
        private const val COUNT_LOADED_PAGE_AFTER_CURRENT = 100
        private var isWorking = false
        private var isPressedControl = false
        private var isPlayingForward = false
        private const val pictW = 135 // ширина картинки
        private const val pictH = 75 // высота картинки
        private var fil: FramesImageLoader? = null
        private var plf: ProgressLoadFrames? = null
        private var currentPage: MatrixPage? = null
        private var currentFrame: MatrixFrame? = null
        private var currentNumPage = 0

        @Volatile
        private var countLoadedPages = 0

        fun editShots(file: File, hostServices: HostServices? = null) {
            currentFile = file
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("shots-edit-view.fxml"))
                mainStage?.scene = Scene(root)
                this.hostServices = hostServices
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы ShotsEditFXController.")
            mainStage = null

        }

    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна ShotsEditFXController.")
        }

        mainStage?.title = "Редактор планов. Файл: ${currentFile!!.name}"
        isWorking = true
        fil = FramesImageLoader()
        fil!!.start()
        plf = ProgressLoadFrames(pb!!)
        plf!!.start()



        println("Инициализация ShotsEditFXController.")
    }

    @FXML
    fun doOK(event: ActionEvent?) {
        isWorking = false
        mainStage?.close()
    }

    class MatrixFrame {
        var frameColumn = 0
        var frameRow = 0
        var isPrevious = false
        var isNext = false
        var frameExt: FrameExt? = null
        var nextMatrixFrame: MatrixFrame? = null
        var prevMatrixFrame: MatrixFrame? = null
        var page: MatrixPage? = null
    }

    class MatrixPage {
        var pageNumber = 0
        var firstFrameNumber = 0
        var lastFrameNumber = 0
        var strDurationStart: String? = null
        var strDurationEnd: String? = null
        var countFramesInPage = 0
        var countColumns = 0
        var countRows = 0
        var listMatrixFrames: MutableList<MatrixFrame> = mutableListOf()
        val isReadyToShow: Boolean
            get() {
                val countAll = listMatrixFrames.size
                val countReady = 0
                for (matrix in listMatrixFrames) {
                    if (matrix.frameExt?.label == null) return false
                }
                return true
            }

        override fun toString(): String {
            return "Page: #$pageNumber, [$firstFrameNumber-$lastFrameNumber] ($strDurationStart-$strDurationEnd), frames = $countFramesInPage"
        }
    }

    private class ProgressLoadFrames(val pb: ProgressBar) : Thread() {
        override fun run() {
            while (isWorking) {
                val progress: Double = 1.0 * countLoadedPages / (COUNT_LOADED_PAGE_AFTER_CURRENT + COUNT_LOADED_PAGE_BEFORE_CURRENT)
                pb.progress = progress
            }
            println("ProgressLoadFrames завершил работу.")
        }
    }

    private class FramesImageLoader : Thread() {
        override fun run() {
            var previousCurrentPage: MatrixPage? = null
            var prevPage: MatrixPage? = null
            var nextPage: MatrixPage? = null
            var prevMatFr: MatrixFrame? = null
            var nextMatFr: MatrixFrame? = null
            while (isWorking) { // работаем, пока на закроется форма
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                if (currentPage != null) {
                    if (currentPage != previousCurrentPage) { // если на очередном шаге цикла работа оказалось, что текущая страница поменялась
                        previousCurrentPage = currentPage
                        // начинаем идти от первого фрейма текущуй страницы в разные стороны
                        prevMatFr = currentPage!!.listMatrixFrames[0]
                        nextMatFr = currentPage!!.listMatrixFrames[0]
                        prevPage = currentPage
                        nextPage = currentPage
                        var prevCounterPages = 0
                        var nextCounterPages = 0
                        while (!(prevMatFr == null && nextMatFr == null)) {
                            if (currentPage != previousCurrentPage || isWorking) break
                            countLoadedPages = prevCounterPages + nextCounterPages
                            if (prevMatFr != null) {
                                if (prevMatFr.page != prevPage) {
                                    prevPage = prevMatFr.page
                                    prevCounterPages++
                                }
                                val frameExt: FrameExt? = prevMatFr.frameExt
                                if (prevCounterPages <= COUNT_LOADED_PAGE_BEFORE_CURRENT) {
                                    if (frameExt != null) {
                                        if (frameExt.label == null) {
                                            var fileName: String = frameExt.pathToSmall
                                            var file = IOFile(fileName)
                                            val label = Label()
                                            label.setMinSize((pictW + 2).toDouble(), (pictH + 2).toDouble())
                                            label.setPrefSize(pictW.toDouble(), pictH.toDouble())
                                            label.setMaxSize((pictW + 2).toDouble(), (pictH + 2).toDouble())
                                            if (!file.exists()) {
//                                                fileName = frameExt.getFileNamePreviewStub()
//                                                file = IOFile(fileName)
                                            } else {
                                                var bufferedImage: BufferedImage? = null
                                                try {
                                                    bufferedImage = ImageIO.read(file)
                                                } catch (e: IOException) {
                                                    e.printStackTrace()
                                                }
                                                val preview = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                                                label.graphic = preview
                                                label.contentDisplay = ContentDisplay.TOP
                                                frameExt.label = label
                                                frameExt.preview = preview
                                            }

                                        }
                                    }
                                } else {
                                    if (frameExt != null) {
                                        if (frameExt.label != null) {
                                            frameExt.label = null
                                            frameExt.preview =null
                                        }
                                    }
                                }
                                prevMatFr = prevMatFr.prevMatrixFrame
                            }
                            if (nextMatFr != null) {
                                if (nextMatFr.page != nextPage) {
                                    nextPage = nextMatFr.page
                                    nextCounterPages++
                                }
                                val frameExt: FrameExt? = nextMatFr.frameExt
                                if (nextCounterPages <= COUNT_LOADED_PAGE_AFTER_CURRENT) {
                                    if (frameExt != null) {
                                        if (frameExt.label == null) {
                                            var fileName: String = frameExt.pathToSmall
                                            var file = IOFile(fileName)
                                            val label = Label()
                                            label.setMinSize((pictW + 2).toDouble(), (pictH + 2).toDouble())
                                            label.setPrefSize(pictW.toDouble(), pictH.toDouble())
                                            label.setMaxSize((pictW + 2).toDouble(), (pictH + 2).toDouble())
                                            if (!file.exists()) {
//                                                fileName = frameExt.getFileNamePreviewStub()
//                                                file = java.io.File(fileName)
                                            } else {
                                                var bufferedImage: BufferedImage? = null
                                                try {
                                                    bufferedImage = ImageIO.read(file)
                                                } catch (e: IOException) {
                                                    e.printStackTrace()
                                                }
                                                val preview = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                                                label.graphic = preview
                                                label.contentDisplay = ContentDisplay.TOP
                                                frameExt.label = label
                                                frameExt.preview = preview
                                            }

                                        }
                                    }
                                } else {
                                    if (frameExt != null) {
                                        if (frameExt.label != null) {
                                            frameExt.label = null
                                            frameExt.preview = null
                                        }
                                    }
                                }
                                nextMatFr = nextMatFr.nextMatrixFrame
                            }
                        }
                    }
                }
            }
            println("FramesImageLoader завершил работу.")
        }
    }
}
