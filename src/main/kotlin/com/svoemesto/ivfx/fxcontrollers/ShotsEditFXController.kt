package com.svoemesto.ivfx.fxcontrollers

import com.sun.javafx.scene.control.skin.VirtualFlow
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import com.svoemesto.ivfx.utils.convertDurationToString
import com.svoemesto.ivfx.utils.getDurationByFrameNumber
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
        private const val pictW = 135 // ширина картинки
        private const val pictH = 75 // высота картинки
        private var fil: FramesImageLoader? = null
        private var plf: ProgressLoadFrames? = null
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
        listShots = ShotController.getListShots(currentFileExt!!.file)

        isWorking = true
        fil = FramesImageLoader()
        fil!!.start()
        plf = ProgressLoadFrames(pb!!)
        plf!!.start()


        // устанавливаем соответствия для столбцов и таблицы
        colDurationStart?.setCellValueFactory(PropertyValueFactory("strDurationStart"))
        colDurationEnd?.setCellValueFactory(PropertyValueFactory("strDurationEnd"))
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
                        initFrameNumber = currentMatrixPage!!.firstFrameNumber
                    }
                    while (!currentMatrixPage!!.isReadyToShow);
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

    fun goToNextShot() {
        var matrixFrame: MatrixFrame? = currentMatrixFrame?.nextMatrixFrame ?: return
        while (!matrixFrame?.frameExt?.frame?.isFinalFind!!) {
            matrixFrame = matrixFrame.nextMatrixFrame
            if (matrixFrame == null) return
        }
        goToFrame(matrixFrame)
    }

    fun goToPreviousShot() {
        var matrixFrame: MatrixFrame? = currentMatrixFrame?.prevMatrixFrame ?: return
        while (!matrixFrame?.frameExt?.frame?.isFinalFind!!) {
            matrixFrame = matrixFrame.prevMatrixFrame
            if (matrixFrame == null) return
        }
        goToFrame(matrixFrame)
    }

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
                    if (matrixFrame.page == currentMatrixPage) {
                        currentMatrixFrame?.frameExt?.label?.style = fxBorderDefault
                    } else {
                        currentMatrixPage = matrixFrame.page
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
        val listMatrixFrames: List<MatrixFrame> = matrixPage.listMatrixFrames
        pane.children.clear() // очищаем пэйн от старых лейблов
        for (matrixFrame in listMatrixFrames) {
            val lbl: Label = matrixFrame.frameExt?.label!!
            val x: Int = widthPadding + matrixFrame.frameColumn * (pictW + 2) // X = отступ по ширине + столбец*ширину картинки
            val y: Int = heightPadding + matrixFrame.frameRow * (pictH + 2) //Y = отступ по высоте + строка*высоту картинки
            lbl.translateX = x.toDouble()
            lbl.translateY = y.toDouble()
            lbl.setPrefSize(pictW.toDouble(), pictH.toDouble()) //устанавливаем ширину и высоту лейбла
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
            if (matrixFrame.nextMatrixFrame != null) {
                if (matrixFrame.nextMatrixFrame!!.frameExt!!.frame.isFind) {
                    if (currImage == null) {
                        try {
                            currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                            if (!matrixFrame.nextMatrixFrame!!.frameExt!!.frame.isManualCancel) { // и не отменен
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
            if (matrixFrame.nextMatrixFrame != null) {
                if (currImage == null) {
                    try {
                        currImage = ImageIO.read(IOFile(matrixFrame.frameExt!!.pathToSmall))
                        if (matrixFrame.nextMatrixFrame!!.frameExt!!.frame.isManualAdd) {
                            resultImage = OverlayImage.setOverlayLastFrameManual(currImage)
                            currImage = resultImage
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
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
                                if (matrixFrame.prevMatrixFrame != null) {
//                                    matrixFrame.prevMatrixFrame!!.frameExt?.label?.graphic = null
                                    matrixFrame.prevMatrixFrame!!.frameExt?.label?.graphic = matrixFrame.prevMatrixFrame!!.frameExt?.preview
                                }
                                FrameController.save(matrixFrame.frameExt!!.frame)
                            }
                        }
//                        actualizeShots(matrixFrame.frameExt)
                        initFrameNumber = currentMatrixPage?.firstFrameNumber!!
                        createPages()
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
            if (page.firstFrameNumber <= frameNumber && page.lastFrameNumber >= frameNumber) return page
        }
        return null
    }

    fun createPages() {
        val countFrames: Int = currentFileExt!!.framesCount // запоминаем кол-во фреймов в файле
        if (countFrames > 0) {
            listMatrixPages.clear() // очищаем список страниц
            var matrixPage: MatrixPage = MatrixPage() // создаем новую страницу
            val nextMatrixFrame: MatrixFrame? = null
            var prevMatrixFrame: MatrixFrame? = null
            var lastMatrixFrameInPage: MatrixFrame? = null
            var currentColumn = 1
            var currentRow = 1
            var wasAddedNewPage = false

            // цикл по списку всех фреймов
            for (i in 0 until countFrames) {

                // считываем из списка фреймов текущий, предыдущий и последующие фреймы
                val currFrame: FrameExt = listFramesExt[i]
                var previousFrame: FrameExt? = null
                var nextFrame: FrameExt? = null
                if (i > 0) previousFrame = listFramesExt[i - 1]
                if (i < countFrames - 1) nextFrame = listFramesExt[i + 1]
                val matrixFrame: MatrixFrame = MatrixFrame() // создаем матрикс-фрейм для текущего фрейма
                matrixFrame.frameExt = currFrame // записываем текущий фрейм в матрикс-фрейм
                matrixFrame.frameColumn = currentColumn // записываем в матрикс-фрейм значение текущего столбца
                matrixFrame.frameRow = currentRow // записываем в матрикс-фрейм значение текущей строки
                matrixFrame.prevMatrixFrame =
                    prevMatrixFrame // записываем в матрикс-фрейм ссылку на предыдущий матрикс-фрейм
                if (prevMatrixFrame != null) prevMatrixFrame.nextMatrixFrame =
                    matrixFrame // в предыдущий матрикс-фрейме записываем текущий матрикс-фрейм как "следующий"
                matrixFrame.page = matrixPage // сохраняем в матрикс-фрейме его страницу
                matrixPage.listMatrixFrames.add(matrixFrame) // добавляем матрикс-фрейм к списку матрикс-фреймов текущей страницы
                prevMatrixFrame = matrixFrame // записываем в "предыдущий" матрикс-фрейм ссылку на текущий матрикс-фрейм
                if (wasAddedNewPage) {
                    wasAddedNewPage = false
                    if (lastMatrixFrameInPage != null) {
                        lastMatrixFrameInPage.nextMatrixFrame = matrixFrame
                    }
                }

                // если оказалось, что мы только что добавили матрикс-фрейм за границы матрицы страницы - пора создавать новую страницу
                if (currentColumn == countColumnsInPage + 1 || currentRow == countRowsInPage + 1) {
                    matrixFrame.isNext = true // помечаем текущий матрикс-фрейм как "следующий"
                    lastMatrixFrameInPage = matrixFrame
                    listMatrixPages.add(matrixPage) // добавляем текущую страницу в список всех страниц
                    //                if (currentPage == null) {
//                    currentPage = matrixPage;
//                    currentFrame = currentPage.listMatrixFrames.get(0);
//                    currFrame.getLabel().setStyle(fxBorderSelected);
//                }

                    // теперь можно создавать новую страницу
                    matrixPage = MatrixPage() // создаем новую страницу (перезаписываем переменную новым объектом)
                    val zeroFrame: MatrixFrame = MatrixFrame() // создаем "нулевой" матрикс-фрейм, который будет копией пред-последнего матрикс-фрейма предыдущей страницы
                    zeroFrame.isPrevious = true // помечаем "нулевой" матрикс-фрейм как "предыдущий"
                    zeroFrame.frameExt = matrixFrame.prevMatrixFrame?.frameExt // помещаем в "нулевой" матрикс-фрейм текущий фрейм
                    zeroFrame.prevMatrixFrame = matrixFrame.prevMatrixFrame?.prevMatrixFrame // "обнуляем" у "нулевого" матрикс-фрейма "предыдущий" матрикс-фрейм
                    if (matrixFrame.frameExt!!.frame.isFinalFind) {
                        // если текущий фрейм явлется переходным, то "нулевой" матрикс-фрейм располагаем "над" первым матрикс-фреймом новой страницы (столбец 1, строка 0)
                        currentColumn = 1
                        currentRow = 0
                    } else {
                        // если текущий фрейм обычный, то "нулевой" матрикс-фрейм располагаем "слева" от первого матрикс-фрейма новой страницы (столбец 0, строка 1)
                        currentColumn = 0
                        currentRow = 1
                    }
                    zeroFrame.frameColumn = currentColumn // записываем в "нулевой" матрикс-фрейм значение его столбца
                    zeroFrame.frameRow = currentRow // записываем в "нулевой" матрикс-фрейм значение его строки
                    zeroFrame.page = matrixPage // сохраняем в матрикс-фрейме его страницу
                    matrixPage.listMatrixFrames.add(zeroFrame) // добавляем "нулевой" матрикс-фрейм к списку матрикс-фреймов вновь созданной страницы - он будет в этом списке первым
                    lastMatrixFrameInPage.nextMatrixFrame = zeroFrame
                    prevMatrixFrame = zeroFrame // записываем в "предыдущий" матрикс-фрейм ссылку на "нулевой" матрикс-фрейм
                    currentColumn = 1 // устанавливаем значение текущего столбцу = 1
                    currentRow = 1 // устанавливаем значение текущей строки = 1
                    val firstFrame: MatrixFrame = MatrixFrame() // создаем новый матрикс-фрейм, который будет первым на новой странице
                    firstFrame.frameExt = matrixFrame.frameExt // присваиваем ему текущий фрейм
                    firstFrame.frameColumn = currentColumn // устанавливаем столбец = 1
                    firstFrame.frameRow = currentRow // устанавливаем строку = 1
                    firstFrame.prevMatrixFrame = prevMatrixFrame // в поле "предыдущий" записываем ссылку на "нулевой" матрикс-фрейм
                    prevMatrixFrame.nextMatrixFrame = firstFrame // у "нулевого" мартикс-фрейма в поле "следующий" записываем ссылку на созданный матрикс-фрейм
                    firstFrame.page = matrixPage // сохраняем в матрикс-фрейме его страницу
                    matrixPage.listMatrixFrames.add(firstFrame) // добавляем созданный матрикс-фрейм к списку матрикс-фреймов новой страницы - он будет там вторым по счету
                    prevMatrixFrame = firstFrame // записываем в "предыдущий" матрикс-фрейм ссылку на "первый" матрикс-фрейм
                    wasAddedNewPage = true // устанавливаем флаг, что мы только что создали новую страницу и уже установили для нее координаты для "текущего" матрикс-фрейма, который будет создан на следующем шаге цикла
                }

                // надо правильно изменить значения текущих столбца и строки для следущюего шага
                if (nextFrame != null && nextFrame.frame.isFinalFind) {
                    // если следущий фрейм "найденый" - он 100% должен быть в том же столбце, но на новой строке
                    currentRow++
                } else {
                    // если следущий фрейм "обычный" - надо проверить, куда его поместить - в продолжении текущей строки или начать им новую строку
                    if (currentColumn < countColumnsInPage) {
                        // если на данный момент мы еще не дошли до последнего столбца страницы - "передвигаемся" на один столбец вправо
                        currentColumn++
                    } else if (currentColumn == countColumnsInPage && currentRow == countRowsInPage) {
                        // если мы стоим на последней яцейке страницы - тоже "передвигаемся" вправо
                        currentColumn++
                    } else if (currentColumn == countColumnsInPage && currentRow < countRowsInPage) {
                        // если мы уже стоим на последнем столбце сраницы, но еще не на последней строке - "перемещаемся" в начало новой строки
                        currentColumn = 1
                        currentRow++
                    }
                }
            }

            // добавляем последнюю страницу в список всех страниц
            listMatrixPages.add(matrixPage)

            // теперь для каждой страницы из списка нужно прописать время начала, время конца, первый и последний кадры
            for (i in listMatrixPages.indices) {
                val page: MatrixPage = listMatrixPages[i]
                page.pageNumber = i + 1
                val firstMatrixFrame: MatrixFrame = if (i == 0) page.listMatrixFrames[0] else page.listMatrixFrames[1]
                val lastMatrixFrame: MatrixFrame = if (i == listMatrixPages.size - 1) page.listMatrixFrames[page.listMatrixFrames.size - 1] else page.listMatrixFrames[page.listMatrixFrames.size - 2]
                page.firstFrameNumber = firstMatrixFrame.frameExt!!.frame.frameNumber
                page.lastFrameNumber = lastMatrixFrame.frameExt!!.frame.frameNumber
                page.strDurationStart = convertDurationToString(getDurationByFrameNumber(page.firstFrameNumber, currentFileExt!!.fps))
                page.strDurationEnd = convertDurationToString(getDurationByFrameNumber(page.lastFrameNumber, currentFileExt!!.fps))
                page.countFramesInPage = page.lastFrameNumber - page.firstFrameNumber
                page.countColumns = countColumnsInPage
                page.countRows = countRowsInPage
            }
        }
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
                createPages() // заново создаем список страниц

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
                if (currentMatrixPage != null) {
                    if (currentMatrixPage != previousCurrentPage) { // если на очередном шаге цикла работа оказалось, что текущая страница поменялась
                        previousCurrentPage = currentMatrixPage
                        // начинаем идти от первого фрейма текущуй страницы в разные стороны
                        prevMatFr = currentMatrixPage!!.listMatrixFrames[0]
                        nextMatFr = currentMatrixPage!!.listMatrixFrames[0]
                        prevPage = currentMatrixPage
                        nextPage = currentMatrixPage
                        var prevCounterPages = 0
                        var nextCounterPages = 0
                        while (!(prevMatFr == null && nextMatFr == null)) {
                            if (currentMatrixPage != previousCurrentPage || isWorking) break
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
