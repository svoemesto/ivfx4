package com.svoemesto.ivfx.fxcontrollers

import com.sun.javafx.scene.control.skin.VirtualFlow
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.MatrixFrame
import com.svoemesto.ivfx.modelsext.MatrixPage
import com.svoemesto.ivfx.modelsext.MatrixPage.Companion.createPages
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.application.HostServices
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
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
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
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
        private var currentFileExt: FileExt? = null
        private var mainStage: Stage? = null

        private var initFrameNumber: Int = 1
        private var listMatrixPages: ObservableList<MatrixPage> = FXCollections.observableArrayList()
        private var countColumnsInPage = 0
        private var countRowsInPage = 0


        private const val fxBorderDefault = "-fx-border-color:#0f0f0f;-fx-border-width:1" // стиль бордюра лейбла по-умолчанию
        private const val fxBorderFocused = "-fx-border-color:YELLOW;-fx-border-width:1" // стиль бордюра лейбла в фокусе
        private const val fxBorderSelected = "-fx-border-color:RED;-fx-border-width:1" // стиль бордюра лейбла выбранного
        private const val COUNT_LOADED_PAGE_BEFORE_CURRENT = 50
        private const val COUNT_LOADED_PAGE_AFTER_CURRENT = 100
        private var isWorking = false
        private var isPressedControl = false
        private var isPlayingForward = false
        private const val pictW = 135.0 // ширина картинки
        private const val pictH = 75.0 // высота картинки
        private var currentMatrixPage: MatrixPage? = null
        private var currentMatrixFrame: MatrixFrame? = null
        private var currentNumPage = 0
        private var flowTblPages: VirtualFlow<*>? = null

        @Volatile
        private var countLoadedPages = 0

        @Volatile
        private var listFramesExt: MutableList<FrameExt> = mutableListOf()

        @Volatile
        private var listShots: MutableList<Shot> = mutableListOf()

        fun editShots(fileExt: FileExt, hostServices: HostServices? = null) {
            currentFileExt = fileExt
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

        println("Инициализация ShotsEditFXController.")

        mainStage?.title = "Редактор планов. Файл: ${currentFileExt!!.file.name}"

        listFramesExt = FrameController.getListFramesExt(currentFileExt!!)
//        listShots = ShotController.getListShots(currentFileExt!!.file)

        isWorking = true
//        fil = FramesImageLoader()
//        fil!!.start()
//        plf = ProgressLoadFrames(pb!!)
//        plf!!.start()


        // устанавливаем соответствия для столбцов и таблицы
        colDurationStart?.setCellValueFactory(PropertyValueFactory("start"))
        colDurationEnd?.setCellValueFactory(PropertyValueFactory("end"))
        colFrameStart?.setCellValueFactory(PropertyValueFactory("firstFrameNumber"))
        colFrameEnd?.setCellValueFactory(PropertyValueFactory("lastFrameNumber"))

        tblPages!!.items = listMatrixPages

        slider?.min = -(listMatrixPages.size - 1).toDouble()
        slider?.max = 0.0

        // выбор записи в таблице tblPages

        // выбор записи в таблице tblPages
        tblPages!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out MatrixPage?>?, oldValue: MatrixPage?, newValue: MatrixPage? ->
                if (newValue != null) {
                    currentMatrixPage = newValue // текущая страница = выбранной
                    if (currentMatrixPage !== getPageByFrame(initFrameNumber)) {
                        // если инитный фрейм не в текущей странице - назначаем инитным фреймом первый фрейм текущей страницы
                        initFrameNumber = currentMatrixPage!!.firstFrameNumber!!
                    }
//                    while (!currentMatrixPage!!.isReadyToShow);
                    for (i in listMatrixPages.indices) {
                        if (currentMatrixPage === listMatrixPages[i]) {
                            currentNumPage = i + 1
                            slider?.value = -currentNumPage.toDouble()
                            break
                        }
                    }
                    showPage(currentMatrixPage!!)
                }
            }


        //лисенер на изменение ширины пейна
        paneCenter!!.widthProperty()
            .addListener { v: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> listenToChangePaneSize() }

        //лисенер на изменение высоты пейна
        paneCenter!!.heightProperty()
            .addListener { v: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> listenToChangePaneSize() }

    }

    @FXML
    fun doOK(event: ActionEvent?) {
        isWorking = false
        mainStage?.close()
    }

//    fun goToNextShot() {
//        var matrixFrame: MatrixFrame? = currentMatrixFrame?.nextMatrixFrame ?: return
//        while (!matrixFrame?.frameExt?.frame?.isFinalFind!!) {
//            matrixFrame = matrixFrame.nextMatrixFrame
//            if (matrixFrame == null) return
//        }
//        goToFrame(matrixFrame)
//    }
//
//    fun goToPreviousShot() {
//        var matrixFrame: MatrixFrame? = currentMatrixFrame?.prevMatrixFrame ?: return
//        while (!matrixFrame?.frameExt?.frame?.isFinalFind!!) {
//            matrixFrame = matrixFrame.prevMatrixFrame
//            if (matrixFrame == null) return
//        }
//        goToFrame(matrixFrame)
//    }

    fun loadPictureToFullFrameLabel(matrixFrame: MatrixFrame?) {

        if (matrixFrame != null) {
            try {
                val ioFile = IOFile(matrixFrame.frameExt!!.pathToMedium)
                if (ioFile.exists()) {
                    val bufferedImage = ImageIO.read(ioFile)
                    val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                    lblFrameFull?.graphic = imageView
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
    }

    fun goToFrame(matrixFrame: MatrixFrame?) {
        Platform.runLater {
            if (matrixFrame != null) {
                if (matrixFrame != currentMatrixFrame) {
                    // если новый фрейм находится на текущей странице
                    if (matrixFrame.matrixPage == currentMatrixPage) {
                        currentMatrixFrame?.frameExt?.label?.style = fxBorderDefault
                    } else {
                        currentMatrixPage = matrixFrame.matrixPage
                        tblPages!!.selectionModel.select(currentMatrixPage)
                        tblPagesSmartScroll(currentMatrixPage)
                    }
                    currentMatrixFrame = matrixFrame
                    currentMatrixFrame?.frameExt?.label?.style = fxBorderSelected
                    loadPictureToFullFrameLabel(currentMatrixFrame)
                }
            }
        }
    }

    fun tblPagesSmartScroll(matrixPage: MatrixPage?) {
        if (flowTblPages != null && flowTblPages!!.cellCount > 0) {
            val first: Int = flowTblPages!!.firstVisibleCell.getIndex()
            val last: Int = flowTblPages!!.lastVisibleCell.getIndex()
            val selected = tblPages!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblPages?.scrollTo(matrixPage)
            }
        }
    }

    fun showPage(matrixPage: MatrixPage) {
        val heightPadding = 10 // по высоте двойной отступ
        val widthPadding = 10 // по ширине двойной отступ
        val pane: Pane = paneCenter!!
        val listMatrixFrames: List<MatrixFrame> = matrixPage.matrixFrames
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFrame in listMatrixFrames) {
            val lbl: Label = matrixFrame.frameExt?.label!!
            val x: Double = widthPadding + matrixFrame.column * (pictW + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFrame.row * (pictH + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(pictW, pictH) //устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault //устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER //устанавливаем позиционирование по центру
            var currImage: BufferedImage? = null
            var resultImage: BufferedImage? = null

            // если кадр ключевой
            if (matrixFrame.frameExt!!.frame.isIFrame) {
                if (currImage == null) {
                    try {
                        currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                        resultImage = OverlayImage.setOverlayIFrame(currImage)
                        currImage = resultImage
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            // если кадр найденый
            if (matrixFrame.frameExt!!.frame.isFind) {
                if (currImage == null) {
                    try {
                        currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                        if (!matrixFrame.frameExt!!.frame.isManualCancel) { // и не отменен
                            resultImage = OverlayImage.setOverlayFirstFrameFound(currImage)
                            currImage = resultImage
                        } else { // и отменен
                            resultImage = OverlayImage.cancelOverlayFirstFrameManual(currImage)
                            currImage = resultImage
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            // если следующий кадр найденый
            if (listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isFind) {
                if (currImage == null) {
                    try {
                        currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                        if (!listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualCancel) { // и не отменен
                            resultImage = OverlayImage.setOverlayLastFrameFound(currImage)
                            currImage = resultImage
                        } else { // и отменен
                            resultImage = OverlayImage.cancelOverlayLastFrameManual(currImage)
                            currImage = resultImage
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            // если кадр установлен вручную
            if (matrixFrame.frameExt!!.frame.isManualAdd) {
                if (currImage == null) {
                    try {
                        currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                        resultImage = OverlayImage.setOverlayFirstFrameManual(currImage)
                        currImage = resultImage
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            // если следующий кадр устанвлен вручную
            if (currImage == null) {
                try {
                    currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                    if (listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualAdd) {
                        resultImage = OverlayImage.setOverlayLastFrameManual(currImage)
                        currImage = resultImage
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            if (resultImage != null) {
                val screenImageView = ImageView(ConvertToFxImage.convertToFxImage(resultImage)) // загружаем ресайзный буфер в новый вьювер
                screenImageView.fitWidth = pictW.toDouble() // устанавливаем ширину вьювера
                screenImageView.fitHeight = pictH.toDouble() // устанавливаем высоту вьювера
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
                if (matrixFrame === currentMatrixFrame) {
                    lbl.style = fxBorderSelected
                } else {
                    lbl.style = fxBorderDefault
                }
            }

            lbl.onMouseClicked = EventHandler { mouseEvent ->

                //событие двойного клика
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 1) {
                        currentMatrixFrame?.frameExt?.label?.style = fxBorderDefault
                        currentMatrixFrame = matrixFrame
                        loadPictureToFullFrameLabel(currentMatrixFrame)
                    }
                    if (mouseEvent.clickCount == 2) {
                        if (matrixFrame.frameExt!!.frame.isFind) { //фрейм найден
                            if (!matrixFrame.frameExt!!.frame.isManualCancel) { // и не отменен вручную
                                matrixFrame.frameExt!!.frame.isManualCancel = true // отменяем
                                matrixFrame.frameExt!!.frame.isFinalFind = false
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            } else { // и отменен вручную
                                matrixFrame.frameExt!!.frame.isManualCancel = false // восстанавливаем отметку
                                matrixFrame.frameExt!!.frame.isFinalFind = true
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            }
                        } else { // не найден
                            if (!matrixFrame.frameExt!!.frame.isManualAdd) { // и не отменен вручную
                                matrixFrame.frameExt!!.frame.isManualAdd = true // отмечаем
                                matrixFrame.frameExt!!.frame.isFinalFind = true
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            } else { // и отменен вручную
                                matrixFrame.frameExt!!.frame.isManualAdd = false // снимаем отметку
                                matrixFrame.frameExt!!.frame.isFinalFind = false
//                                matrixFrame.frameExt!!.label?.graphic = null
                                matrixFrame.frameExt!!.label?.graphic = matrixFrame.frameExt!!.preview
                                if (listMatrixFrames[listMatrixFrames.indexOf(matrixFrame)-1] != null) {
//                                    matrixFrame.prevMatrixFrame!!.frameExt?.label?.graphic = null
                                    listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt?.label?.graphic = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt?.preview
                                }
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            }
                        }
//                        actualizeShots(matrixFrame.frameExt)
                        initFrameNumber = currentMatrixPage?.firstFrameNumber!!
                        listMatrixPages = createPages(listFramesExt, paneCenter!!.getWidth(), paneCenter!!.getHeight(), pictW, pictH)
                        tblPages!!.items = listMatrixPages
                        currentMatrixPage = getPageByFrame(initFrameNumber)
                        tblPages!!.selectionModel.select(currentMatrixPage)
                    }
                }
            }
        }
    }

    fun getPageByFrame(frameNumber: Int): MatrixPage? {
        for (page in listMatrixPages) {
            if (page.firstFrameNumber!! <= frameNumber && page.lastFrameNumber!! >= frameNumber) return page
        }
        return null
    }


    fun listenToChangePaneSize() {
        val paneWidth: Double = paneCenter!!.getWidth() // ширина центрального пэйна
        val paneHeight: Double = paneCenter!!.getHeight() // высота центрального пейна
        val widthPadding = (pictW + 2) * 2 + 20 // по ширине двойной отступ
        val heightPadding = (pictH + 2) * 2 + 20 // по высоте двойной отступ
        if (paneWidth > widthPadding && paneHeight > heightPadding) {
            val prevCountColumnsInPage = countColumnsInPage
            val prevCountRowsInPage = countRowsInPage
            countColumnsInPage =
                ((paneWidth - widthPadding) / (pictW + 2)).toInt() // количество столбцов, которое влезет на экран
            countRowsInPage =
                ((paneHeight - heightPadding) / (pictH + 2)).toInt() // количество строк, которое влезет на экран

            // если значения кол-ва столбцов и/или строк изменилось при ресайзе
            if (prevCountColumnsInPage != countColumnsInPage || prevCountRowsInPage != countRowsInPage) {
                createPages(listFramesExt, paneWidth, paneHeight, pictW, pictH) // заново создаем список страниц

                // если список страниц не пустой
                if (listMatrixPages.size > 0) {
                    tblPages!!.setItems(listMatrixPages) // запихиваем список в таблицу
                    tblPages!!.refresh() // рефрешим таблицу
                    val currPage: MatrixPage? = getPageByFrame(initFrameNumber) // узнаем, в какой странице сидит инитный фрейм
                    slider?.setMin(-(listMatrixPages.size - 1).toDouble())
                    slider?.setMax(0.0)
                    for (i in listMatrixPages.indices) {
                        if (currPage === listMatrixPages.get(i)) {
                            slider?.setValue(-(i + 1).toDouble())
                            break
                        }
                    }
                    tblPages!!.selectionModel.select(currPage) // переходим на эту страницу в таблице
                    tblPagesSmartScroll(currPage)
                }
            }
        }
    }

}
