package com.svoemesto.ivfx.fxcontrollers

import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.MatrixFrame
import com.svoemesto.ivfx.modelsext.MatrixPage
import com.svoemesto.ivfx.modelsext.MatrixPage.Companion.createPages
import com.svoemesto.ivfx.modelsext.ShotExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListFramesExt
import com.svoemesto.ivfx.threads.loadlists.LoadListShotsExt
import com.svoemesto.ivfx.threads.updatelists.UpdateListFramesExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
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
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Skin
import javafx.scene.control.Slider
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
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
    private var tblShots: TableView<ShotExt>? = null

    @FXML
    private var colShotFrom: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotTo: TableColumn<ShotExt, String>? = null

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
    private var lblPb: Label? = null

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
        private val runListThreadsFramesFlagIsDone = SimpleBooleanProperty(false)

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
        private var currentShotExt: ShotExt? = null
        private var currentNumPage = 0
        private var flowTblPages: VirtualFlow<*>? = null
        private var flowTblShots: VirtualFlow<*>? = null

        @Volatile
        private var countLoadedPages = 0

//        @Volatile
//        private var listFramesExt: ObservableList<FrameExt> = FXCollections.observableArrayList()
//
//        @Volatile
//        private var listShotsExt: ObservableList<ShotExt> = FXCollections.observableArrayList()

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


        var listThreads: MutableList<Thread> = mutableListOf()
        listThreads.add(LoadListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb))
        listThreads.add(LoadListShotsExt(currentFileExt!!.shotsExt, currentFileExt!!, pb, lblPb))
        var runListThreadsFrames = RunListThreads(listThreads, runListThreadsFramesFlagIsDone)
        runListThreadsFrames.start()

        runListThreadsFramesFlagIsDone.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                listMatrixPages = createPages(currentFileExt!!.framesExt, paneCenter!!.getWidth(), paneCenter!!.getHeight(), pictW, pictH)
                tblPages!!.items = listMatrixPages
//                runListThreadsFramesFlagIsDone.set(false)
                listThreads = mutableListOf()
                listThreads.add(UpdateListFramesExt(currentFileExt!!.framesExt, currentFileExt!!, pb, lblPb))
                runListThreadsFrames = RunListThreads(listThreads)
                runListThreadsFrames.start()
            }
        }

        isWorking = true

        // устанавливаем соответствия для столбцов и таблицы
        colDurationStart?.cellValueFactory = PropertyValueFactory("start")
        colDurationEnd?.cellValueFactory = PropertyValueFactory("end")
        colFrameStart?.cellValueFactory = PropertyValueFactory("firstFrameNumber")
        colFrameEnd?.cellValueFactory = PropertyValueFactory("lastFrameNumber")
        tblPages!!.items = listMatrixPages

        colShotFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colShotTo?.cellValueFactory = PropertyValueFactory("labelLast1")
        tblShots!!.items = currentFileExt!!.shotsExt

        slider?.min = -(listMatrixPages.size - 1).toDouble()
        slider?.max = 0.0

        // выбор записи в таблице tblPages
        tblPages!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out MatrixPage?>?, oldValue: MatrixPage?, newValue: MatrixPage? ->
                if (newValue != null) {
                    currentMatrixPage = newValue // текущая страница = выбранной
                    if (currentMatrixPage !== getPageByFrame(initFrameNumber)) {
                        // если инитный фрейм не в текущей странице - назначаем инитным фреймом первый фрейм текущей страницы
                        initFrameNumber = currentMatrixPage!!.firstFrameNumber!!
                    }
                    for (i in listMatrixPages.indices) {
                        if (currentMatrixPage === listMatrixPages[i]) {
                            currentNumPage = i + 1
                            slider?.value = -currentNumPage.toDouble()
                            break
                        }
                    }
                    showPage(currentMatrixPage!!)
                    if (currentMatrixFrame == null || currentMatrixFrame!!.matrixPage != currentMatrixPage) {
                        goToFrame(currentMatrixPage!!.matrixFrames.first())
                    }

                }
            }

        tblShots!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out ShotExt?>?, oldValue: ShotExt?, newValue: ShotExt? ->
                if (newValue != null && newValue != currentShotExt) {
                    currentShotExt = newValue // текущая страница = выбранной
                    goToFrame(currentShotExt!!.firstFrameExt)
                }
            }

        // событие отслеживани видимости на экране текущего персонажа в таблице tblPages
        tblPages!!.skinProperty()
            .addListener(ChangeListener label@{ ov: ObservableValue<out Skin<*>?>?, t: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) {
                    return@label
                }
                val tvs =
                    t1 as TableViewSkin<*>
                val kids = tvs.children //    getChildrenUnmodifiable();
                if (kids == null || kids.isEmpty()) {
                    return@label
                }
                flowTblPages = kids[1] as VirtualFlow<*>
            })

        tblShots!!.skinProperty()
            .addListener(ChangeListener label@{ ov: ObservableValue<out Skin<*>?>?, t: Skin<*>?, t1: Skin<*>? ->
                if (t1 == null) {
                    return@label
                }
                val tvs =
                    t1 as TableViewSkin<*>
                val kids = tvs.children //    getChildrenUnmodifiable();
                if (kids == null || kids.isEmpty()) {
                    return@label
                }
                flowTblShots = kids[1] as VirtualFlow<*>
            })

        //лисенер на изменение ширины пейна
        paneCenter!!.widthProperty()
            .addListener { v: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> listenToChangePaneSize() }

        //лисенер на изменение высоты пейна
        paneCenter!!.heightProperty()
            .addListener { v: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> listenToChangePaneSize() }


        // прокрутка колеса мыши над CenterPane

        // прокрутка колеса мыши над CenterPane
        paneCenter!!.setOnScroll(EventHandler { e: ScrollEvent ->
            val delta = if (e.deltaY > 0) -1 else 1
            if (isPressedControl) {
//                if (delta > 0) {
//                    goToNextShot()
//                } else {
//                    goToPreviousShot()
//                }
            } else {
                val frameToGo = if (delta < 0) getPrevMatrixFrame(currentMatrixPage!!.matrixFrames.first()) else getNextMatrixFrame(currentMatrixPage!!.matrixFrames.last())
                if (currentMatrixPage != frameToGo.matrixPage) goToFrame(frameToGo)
            }
        })
    }

    @FXML
    fun doOK(event: ActionEvent?) {
        isWorking = false
        mainStage?.close()
    }

    fun getNextMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        if (matrixFrame == matrixFrame.matrixPage!!.matrixFrames.last()) {
            if (matrixFrame.matrixPage == listMatrixPages.last()) return matrixFrame
        }
        return listMatrixPages[listMatrixPages.indexOf(matrixFrame.matrixPage)+1].matrixFrames.first()
    }

    fun getPrevMatrixFrame(matrixFrame: MatrixFrame): MatrixFrame {
        if (matrixFrame == matrixFrame.matrixPage!!.matrixFrames.first()) {
            if (matrixFrame.matrixPage == listMatrixPages.first()) return matrixFrame
        }
        return listMatrixPages[listMatrixPages.indexOf(matrixFrame.matrixPage)-1].matrixFrames.last()
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

            Thread {
                try {
                    val ioFile = IOFile(matrixFrame.frameExt!!.pathToMedium)
                    if (ioFile.exists()) {
                        val bufferedImage = ImageIO.read(ioFile)
                        val imageView = ImageView(ConvertToFxImage.convertToFxImage(bufferedImage))
                        Platform.runLater {
                            lblFrameFull?.graphic = imageView
                        }

                    }
                } catch (exception: IOException) {
                    exception.printStackTrace()
                }
            }.start()

        }
    }

    fun goToFrame(frameExt: FrameExt) {

        val lst: MutableList<MatrixFrame> = mutableListOf()
        listMatrixPages.forEach { matrixPage ->
            lst.addAll(matrixPage.matrixFrames.filter { matrixFrame -> matrixFrame.frameExt == frameExt })
        }
        if (lst.size > 0) goToFrame(lst[0])

    }

    fun goToFrame(matrixFrame: MatrixFrame?) {
        Platform.runLater {
            if (matrixFrame != null) {
                if (matrixFrame != currentMatrixFrame) {
                    // если новый фрейм находится на текущей странице
                    currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderDefault
                    currentMatrixFrame = matrixFrame
                    if (matrixFrame.matrixPage == currentMatrixPage) {
                        currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderDefault
                    } else {
                        currentMatrixPage = matrixFrame.matrixPage
                        tblPages!!.selectionModel.select(currentMatrixPage)
                        tblPagesSmartScroll(currentMatrixPage)
                    }

                    goToShot(currentMatrixFrame)
                    currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderSelected
                    loadPictureToFullFrameLabel(currentMatrixFrame)
                }
            }
        }
    }

    fun goToShot(matrixFrame: MatrixFrame?) {
        if (matrixFrame != null) {
            val shotExt = currentFileExt!!.shotsExt.first {
                matrixFrame.frameNumber!! >= it.firstFrameExt.frame.frameNumber &&
                matrixFrame.frameNumber!! <= it.lastFrameExt.frame.frameNumber
            }
            if (shotExt != currentShotExt) {
                currentShotExt = shotExt
                tblShots!!.selectionModel.select(currentShotExt)
                tblShotsSmartScroll(currentShotExt)
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

    fun tblShotsSmartScroll(shotExt: ShotExt?) {
        if (flowTblShots != null && flowTblShots!!.cellCount > 0) {
            val first: Int = flowTblShots!!.firstVisibleCell.getIndex()
            val last: Int = flowTblShots!!.lastVisibleCell.getIndex()
            val selected = tblShots!!.selectionModel.selectedIndex
            if (selected < first || selected > last) {
                tblShots?.scrollTo(shotExt)
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
            val lbl: Label = matrixFrame.frameExt?.labelSmall!!
//            val lbl: Label = Label()
            val x: Double = widthPadding + matrixFrame.column * (pictW + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Double = heightPadding + matrixFrame.row * (pictH + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x
            lbl.translateY = y
            lbl.setPrefSize(pictW, pictH) //устанавливаем ширину и высоту лейбла
            lbl.style = fxBorderDefault //устанавливаем стиль бордюра по-дефолту
            lbl.alignment = Pos.CENTER //устанавливаем позиционирование по центру
            var currImage: BufferedImage? = null
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
            if (listMatrixFrames.indexOf(matrixFrame) + 1 < listMatrixFrames.size) {
                flagNextFrameIsFind = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isFind
                flagNextFrameIsManualAdd = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualAdd
                flagNextFrameIsManualCancel = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) + 1].frameExt!!.frame.isManualCancel
            }
            if (listMatrixFrames.indexOf(matrixFrame)>0) {
                flagPrevFrameIsFind = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isFind
                flagPrevFrameIsManualAdd = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isManualAdd
                flagPrevFrameIsManualCancel = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt!!.frame.isManualCancel
            }

            if (flagPrevFrameIsFind || flagPrevFrameIsManualAdd || flagPrevFrameIsManualCancel ||
                flagNextFrameIsFind || flagNextFrameIsManualAdd || flagNextFrameIsManualCancel ||
                flagCurrFrameIsIFrame || flagCurrFrameIsFind || flagCurrFrameIsManualAdd || flagCurrFrameIsManualCancel) {
                try {
                    resultImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            if (flagCurrFrameIsIFrame) resultImage = OverlayImage.setOverlayIFrame(resultImage!!)
            if (flagCurrFrameIsFind) resultImage = if (flagCurrFrameIsManualCancel) OverlayImage.cancelOverlayFirstFrameManual(resultImage!!) else OverlayImage.setOverlayFirstFrameFound(resultImage!!)
            if (flagNextFrameIsFind) resultImage = if (flagNextFrameIsManualCancel) OverlayImage.cancelOverlayLastFrameManual(resultImage!!) else OverlayImage.setOverlayLastFrameFound(resultImage!!)
            if (flagCurrFrameIsManualAdd) resultImage = OverlayImage.setOverlayFirstFrameManual(resultImage!!)
            if (flagNextFrameIsManualAdd) resultImage = OverlayImage.setOverlayLastFrameManual(resultImage!!)

            if (resultImage != null) {
                val screenImageView = ImageView(ConvertToFxImage.convertToFxImage(resultImage)) // загружаем ресайзный буфер в новый вьювер
                screenImageView.fitWidth = pictW // устанавливаем ширину вьювера
                screenImageView.fitHeight = pictH // устанавливаем высоту вьювера
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
                        currentMatrixFrame?.frameExt?.labelSmall?.style = fxBorderDefault
                        currentMatrixFrame = matrixFrame
                        goToShot(currentMatrixFrame)
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
                                matrixFrame.frameExt!!.labelSmall?.graphic = matrixFrame.frameExt!!.previewSmall
                                if (listMatrixFrames[listMatrixFrames.indexOf(matrixFrame)-1] != null) {
//                                    matrixFrame.prevMatrixFrame!!.frameExt?.label?.graphic = null
                                    listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt?.labelSmall?.graphic = listMatrixFrames[listMatrixFrames.indexOf(matrixFrame) - 1].frameExt?.previewSmall
                                }
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            }
                        }
//                        actualizeShots(matrixFrame.frameExt)
//                        initFrameNumber = currentMatrixPage?.firstFrameNumber!!+1
                        listMatrixPages = createPages(currentFileExt!!.framesExt, paneCenter!!.getWidth(), paneCenter!!.getHeight(), pictW, pictH)
                        tblPages!!.items = listMatrixPages
//                        currentMatrixPage = getPageByFrame(initFrameNumber)
                        currentMatrixPage = getPageByFrame(currentMatrixFrame?.frameNumber!!)
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

        if (runListThreadsFramesFlagIsDone.value) {
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
                    listMatrixPages = createPages(currentFileExt!!.framesExt, paneWidth, paneHeight, pictW, pictH) // заново создаем список страниц

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

}
