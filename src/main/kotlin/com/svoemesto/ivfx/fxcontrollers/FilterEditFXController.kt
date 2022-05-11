package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FilterConditionController
import com.svoemesto.ivfx.controllers.FilterController
import com.svoemesto.ivfx.controllers.FilterGroupController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.controllers.ShotTmpCdfController
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.VideoContainers
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
import com.svoemesto.ivfx.threads.loadlists.LoadListFilesExt
import com.svoemesto.ivfx.threads.projectactions.CreateFilterResult
import javafx.application.HostServices
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.RadioButton
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException
import java.io.File as IOFile


class FilterEditFXController {

    // FILTER

    @FXML
    private var tblFilters: TableView<FilterExt>? = null

    @FXML
    private var colFilterOrder: TableColumn<FilterExt, Int>? = null

    @FXML
    private var colFilterName: TableColumn<FilterExt, String>? = null

    @FXML
    private var colFilterIsAnd: TableColumn<FilterExt, String>? = null

    @FXML
    private var fldFilterName: TextField? = null

    @FXML
    private var rbFilterIsAnd: RadioButton? = null

    @FXML
    private var tgFilter: ToggleGroup? = null

    @FXML
    private var rbFilterIsOr: RadioButton? = null

    @FXML
    private var btnFilterMoveToFirst: Button? = null

    @FXML
    private var btnFilterMoveUp: Button? = null

    @FXML
    private var btnFilterMoveDown: Button? = null

    @FXML
    private var btnFilterMoveToLast: Button? = null

    @FXML
    private var btnFilterAdd: Button? = null

    @FXML
    private var btnFilterDelete: Button? = null

    // FILGER GROUP

    @FXML
    private var tblFiltersGroups: TableView<FilterGroupExt>? = null

    @FXML
    private var colFilterGroupOrder: TableColumn<FilterGroupExt, Int>? = null

    @FXML
    private var colFilterGroupName: TableColumn<FilterGroupExt, String>? = null

    @FXML
    private var colFilterGroupIsAnd: TableColumn<FilterGroupExt, String>? = null

    @FXML
    private var fldFilterGroupName: TextField? = null

    @FXML
    private var rbFilterGroupIsAnd: RadioButton? = null

    @FXML
    private var tgFilterGroup: ToggleGroup? = null

    @FXML
    private var rbFilterGroupIsOr: RadioButton? = null

    @FXML
    private var btnFilterGroupMoveToFirst: Button? = null

    @FXML
    private var btnFilterGroupMoveUp: Button? = null

    @FXML
    private var btnFilterGroupMoveDown: Button? = null

    @FXML
    private var btnFilterGroupMoveToLast: Button? = null

    @FXML
    private var btnFilterGroupAdd: Button? = null

    @FXML
    private var btnFilterGroupDelete: Button? = null

    // FILTER CONDITION

    @FXML
    private var tblFiltersConditions: TableView<FilterConditionExt>? = null

    @FXML
    private var colFilterConditionOrder: TableColumn<FilterConditionExt, Int>? = null

    @FXML
    private var colFilterConditionName: TableColumn<FilterConditionExt, String>? = null

    @FXML
    private var btnFilterConditionMoveToFirst: Button? = null

    @FXML
    private var btnFilterConditionMoveUp: Button? = null

    @FXML
    private var btnFilterConditionMoveDown: Button? = null

    @FXML
    private var btnFilterConditionMoveToLast: Button? = null

    @FXML
    private var btnFilterConditionAdd: Button? = null

    @FXML
    private var btnFilterConditionDelete: Button? = null

    // FILES

    @FXML
    private var tblFiles: TableView<FileExt>? = null

    @FXML
    private var colFileOrder: TableColumn<FileExt, Int>? = null

    @FXML
    private var colFileName: TableColumn<FileExt, String>? = null

    @FXML
    private var btnFilter: Button? = null

    // SHOTS

    @FXML
    private var tblShots: TableView<ShotExt>? = null

    @FXML
    private var colShotFileName: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotFrom: TableColumn<ShotExt, String>? = null

    @FXML
    private var colShotTo: TableColumn<ShotExt, String>? = null


    @FXML
    private var btnCreateVideo: Button? = null

    @FXML
    private var btnCreateVideoForAllPersons: Button? = null

    // PROGRESS

    @FXML
    private var pb: ProgressBar? = null

    @FXML
    private var lblPb: Label? = null

    companion object {
        private var currentProjectExt: ProjectExt? = null
        private var mainStage: Stage? = null
        private var hostServices: HostServices? = null
    }

    private var listFilesExt: ObservableList<FileExt> = FXCollections.observableArrayList()
    private var listShotsExt: ObservableList<ShotExt> = FXCollections.observableArrayList()
    private var listFilterConditionsExt: ObservableList<FilterConditionExt> = FXCollections.observableArrayList()
    private var listFilterGroupsExt: ObservableList<FilterGroupExt> = FXCollections.observableArrayList()
    private var listFiltersExt: ObservableList<FilterExt> = FXCollections.observableArrayList()
    private var currentFilterConditionExt: FilterConditionExt? = null
    private var currentFilterGroupExt: FilterGroupExt? = null
    private var currentFilterExt: FilterExt? = null

    fun editFilters(projectExt: ProjectExt, hostServices: HostServices? = null) {
        currentProjectExt = projectExt
        mainStage = Stage()
        try {
            val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("filter-edit-view.fxml"))
            mainStage?.scene = Scene(root)
            FilterEditFXController.hostServices = hostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы FilterEditFXController.")
        mainStage = null

    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна FilterEditFXController.")
        }

        println("Инициализация FilterEditFXController.")

        colFilterConditionOrder?.cellValueFactory = PropertyValueFactory("order")
        colFilterConditionName?.cellValueFactory = PropertyValueFactory("name")

        colFilterGroupOrder?.cellValueFactory = PropertyValueFactory("order")
        colFilterGroupName?.cellValueFactory = PropertyValueFactory("name")
        colFilterGroupIsAnd?.cellValueFactory = PropertyValueFactory("andText")

        colFilterOrder?.cellValueFactory = PropertyValueFactory("order")
        colFilterName?.cellValueFactory = PropertyValueFactory("name")
        colFilterIsAnd?.cellValueFactory = PropertyValueFactory("andText")

        colFileOrder?.cellValueFactory = PropertyValueFactory("fileOrder")
        colFileName?.cellValueFactory = PropertyValueFactory("fileName")

        colShotFrom?.cellValueFactory = PropertyValueFactory("labelFirst1")
        colShotTo?.cellValueFactory = PropertyValueFactory("labelLast1")
        colShotFileName?.cellValueFactory = PropertyValueFactory("fileName")


        tblFiles!!.selectionModel.selectionMode = SelectionMode.MULTIPLE

        tblFiles?.items = listFilesExt
        tblFiltersGroups?.items = listFilterGroupsExt
        tblFiltersConditions?.items = listFilterConditionsExt

        LoadListFilesExt(listFilesExt, currentProjectExt!!, pb, lblPb).start()

        listFiltersExt = FXCollections.observableList(FilterController.getList(currentProjectExt!!))
        tblFilters?.items = listFiltersExt

        enabledButtons()

        // делаем поле colFilterConditionName таблицы tblFiltersConditions с переносом по словам и расширяемым по высоте
        colFilterConditionName?.setCellFactory { param: TableColumn<FilterConditionExt?, String?>? ->
            val cell: TableCell<FilterConditionExt, String> = TableCell<FilterConditionExt, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilterConditionName!!.widthProperty())
            cell
        }

        // делаем поле colFilterGroupName таблицы tblFiltersGroups с переносом по словам и расширяемым по высоте
        colFilterGroupName?.setCellFactory { param: TableColumn<FilterGroupExt?, String?>? ->
            val cell: TableCell<FilterGroupExt, String> = TableCell<FilterGroupExt, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilterGroupName!!.widthProperty())
            cell
        }

        // делаем поле colFilterName таблицы tblFilters с переносом по словам и расширяемым по высоте
        colFilterName?.setCellFactory { param: TableColumn<FilterExt?, String?>? ->
            val cell: TableCell<FilterExt, String> = TableCell<FilterExt, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilterName!!.widthProperty())
            cell
        }

        // selectedItemProperty
        tblFilters!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterExt? ->
                if (newValue != null) {

                    currentFilterExt = newValue
                    currentFilterGroupExt = null
                    currentFilterConditionExt = null

                    listFilterConditionsExt = FXCollections.observableArrayList()
                    tblFiltersConditions?.items = listFilterConditionsExt

                    enabledButtons()

                    fldFilterName?.text = currentFilterExt!!.name
                    rbFilterIsAnd?.isSelected = currentFilterExt!!.filter.isAnd
                    rbFilterIsOr?.isSelected = !currentFilterExt!!.filter.isAnd
                    listFilterGroupsExt = FXCollections.observableList(FilterGroupController.getList(currentFilterExt!!))
                    tblFiltersGroups?.items = listFilterGroupsExt


                }
            }

        // selectedItemProperty
        tblFiltersGroups!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterGroupExt? ->
                if (newValue != null) {
                    currentFilterGroupExt = newValue

                    enabledButtons()

                    fldFilterGroupName?.text = currentFilterGroupExt!!.name
                    rbFilterGroupIsAnd?.isSelected = currentFilterGroupExt!!.filterGroup.isAnd
                    rbFilterGroupIsOr?.isSelected = !currentFilterGroupExt!!.filterGroup.isAnd
                    listFilterConditionsExt = FXCollections.observableList(FilterConditionController.getList(currentFilterGroupExt!!))
                    tblFiltersConditions?.items = listFilterConditionsExt
                }
            }

        // selectedItemProperty
        tblFiltersConditions!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterConditionExt? ->
                if (newValue != null) {
                    currentFilterConditionExt = newValue

                    enabledButtons()

                }
            }


        // Click
        tblFiltersConditions!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentFilterConditionExt != null && currentFilterGroupExt != null) {
                        FilterConditionCreateFXController().createFilterCondition(currentProjectExt!!, currentFilterGroupExt!!)
                        tblFiltersConditions!!.refresh()
                    }
                }
            }
        }

        fldFilterGroupName!!.textProperty().addListener { _, _, newValue ->
            if (currentFilterGroupExt != null) {
                currentFilterGroupExt?.filterGroup?.name = newValue
                FilterGroupController.save(currentFilterGroupExt!!.filterGroup)
                tblFiltersGroups?.refresh()
            }
        }

        fldFilterName!!.textProperty().addListener { _, _, newValue ->
            if (currentFilterExt != null) {
                currentFilterExt?.filter?.name = newValue
                FilterController.save(currentFilterExt!!.filter)
                tblFilters?.refresh()
            }
        }

    }


    fun enabledButtons() {

        btnFilterDelete?.isDisable = currentFilterExt == null
        btnFilterMoveToFirst?.isDisable = listFiltersExt.isEmpty() || currentFilterExt == null || currentFilterExt == listFiltersExt.first()
        btnFilterMoveUp?.isDisable = listFiltersExt.isEmpty() || currentFilterExt == null || currentFilterExt == listFiltersExt.first()
        btnFilterMoveToLast?.isDisable = listFiltersExt.isEmpty() || currentFilterExt == null || currentFilterExt == listFiltersExt.last()
        btnFilterMoveDown?.isDisable = listFiltersExt.isEmpty() || currentFilterExt == null || currentFilterExt == listFiltersExt.last()
        btnFilterGroupAdd?.isDisable = currentFilterExt == null

        btnFilterGroupDelete?.isDisable = currentFilterGroupExt == null
        btnFilterGroupMoveToFirst?.isDisable = listFilterGroupsExt.isEmpty() || currentFilterGroupExt == null || currentFilterGroupExt == listFilterGroupsExt.first()
        btnFilterGroupMoveUp?.isDisable = listFilterGroupsExt.isEmpty() || currentFilterGroupExt == null || currentFilterGroupExt == listFilterGroupsExt.first()
        btnFilterGroupMoveToLast?.isDisable = listFilterGroupsExt.isEmpty() || currentFilterGroupExt == null || currentFilterGroupExt == listFilterGroupsExt.last()
        btnFilterGroupMoveDown?.isDisable = listFilterGroupsExt.isEmpty() || currentFilterGroupExt == null || currentFilterGroupExt == listFilterGroupsExt.last()
        btnFilterConditionAdd?.isDisable = currentFilterGroupExt == null


        btnFilterConditionDelete?.isDisable = currentFilterConditionExt == null
        btnFilterConditionMoveToFirst?.isDisable = listFilterConditionsExt.isEmpty() || currentFilterConditionExt == null || currentFilterConditionExt == listFilterConditionsExt.first()
        btnFilterConditionMoveUp?.isDisable = listFilterConditionsExt.isEmpty() || currentFilterConditionExt == null || currentFilterConditionExt == listFilterConditionsExt.first()
        btnFilterConditionMoveToLast?.isDisable = listFilterConditionsExt.isEmpty() || currentFilterConditionExt == null || currentFilterConditionExt == listFilterConditionsExt.last()
        btnFilterConditionMoveDown?.isDisable = listFilterConditionsExt.isEmpty() || currentFilterConditionExt == null || currentFilterConditionExt == listFilterConditionsExt.last()

    }




    @FXML // Изменения IsAnd у группы
    fun doFilterGroupIsAnd(event: ActionEvent?) {
        if (currentFilterGroupExt != null) {
            currentFilterGroupExt?.filterGroup?.isAnd = rbFilterGroupIsAnd!!.isSelected
            FilterGroupController.save(currentFilterGroupExt!!.filterGroup)
            tblFiltersGroups?.refresh()
        }
    }

    @FXML // Изменения IsAnd у фильтра
    fun doFilterIsAnd(event: ActionEvent?) {
        if (currentFilterExt != null) {
            currentFilterExt?.filter?.isAnd = rbFilterIsAnd!!.isSelected
            FilterController.save(currentFilterExt!!.filter)
            tblFilters?.refresh()
        }
    }

    @FXML
    fun doFilterAdd(event: ActionEvent?) {
        val filter = FilterController.create(currentProjectExt!!.project)
        listFiltersExt = FXCollections.observableList(FilterController.getList(currentProjectExt!!))
        tblFilters?.items = listFiltersExt
        tblFilters?.selectionModel?.select(listFiltersExt.first { it.filter.id == filter.id })
    }

    @FXML
    fun doFilterConditionAdd(event: ActionEvent?) {
        val filterCondition = FilterConditionCreateFXController().createFilterCondition(currentProjectExt!!, currentFilterGroupExt!!)
        if (filterCondition != null) {
            listFilterConditionsExt = FXCollections.observableList(FilterConditionController.getList(currentFilterGroupExt!!))
            tblFiltersConditions?.items = listFilterConditionsExt
            tblFiltersConditions?.selectionModel?.select(filterCondition)
        }
    }

    @FXML
    fun doFilterGroupAdd(event: ActionEvent?) {
        val filterGroup = FilterGroupController.create(currentFilterExt!!.filter)
        listFilterGroupsExt = FXCollections.observableList(FilterGroupController.getList(currentFilterExt!!))
        tblFiltersGroups?.items = listFilterGroupsExt
        tblFiltersGroups?.selectionModel?.select(listFilterGroupsExt.first { it.filterGroup.id == filterGroup.id })
    }

    @FXML
    fun doFilterConditionDelete(event: ActionEvent?) {
        if (currentFilterConditionExt != null && currentFilterGroupExt!= null) {
            FilterConditionController.delete(currentFilterConditionExt!!.filterCondition)
            currentFilterConditionExt = null
            listFilterConditionsExt = FXCollections.observableList(FilterConditionController.getList(currentFilterGroupExt!!))
            tblFiltersConditions?.items = listFilterConditionsExt
        }
    }

    @FXML
    fun doFilterDelete(event: ActionEvent?) {
        if (currentFilterExt != null) {
            FilterController.delete(currentFilterExt!!.filter)
            currentFilterExt = null
            listFiltersExt = FXCollections.observableList(FilterController.getList(currentProjectExt!!))
            tblFilters?.items = listFiltersExt
        }
    }

    @FXML
    fun doFilterGroupDelete(event: ActionEvent?) {
        if (currentFilterGroupExt!= null && currentFilterExt!= null) {
            FilterGroupController.delete(currentFilterGroupExt!!.filterGroup)
            currentFilterGroupExt = null
            listFilterGroupsExt = FXCollections.observableList(FilterGroupController.getList(currentFilterExt!!))
            tblFiltersGroups?.items = listFilterGroupsExt
        }
    }

    @FXML
    fun doFilterConditionMoveDown(event: ActionEvent?) {
        doMoveFilterCondition(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFilterConditionMoveToFirst(event: ActionEvent?) {
        doMoveFilterCondition(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFilterConditionMoveToLast(event: ActionEvent?) {
        doMoveFilterCondition(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFilterConditionMoveUp(event: ActionEvent?) {
        doMoveFilterCondition(ReorderTypes.MOVE_UP)
    }

    fun doMoveFilterCondition(reorderType: ReorderTypes) {
        val id = currentFilterConditionExt!!.filterCondition.id
        currentFilterConditionExt?.let { FilterConditionController.reOrder(reorderType, it.filterCondition) }
        tblFiltersConditions?.items = listFilterConditionsExt
        currentFilterConditionExt = listFilterConditionsExt.first { it.filterCondition.id == id }
        tblFiltersConditions?.selectionModel?.select(currentFilterConditionExt)
    }

    @FXML
    fun doFilterGroupMoveDown(event: ActionEvent?) {
        doMoveFilterGroup(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFilterGroupMoveToFirst(event: ActionEvent?) {
        doMoveFilterGroup(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFilterGroupMoveToLast(event: ActionEvent?) {
        doMoveFilterGroup(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFilterGroupMoveUp(event: ActionEvent?) {
        doMoveFilterGroup(ReorderTypes.MOVE_UP)
    }

    fun doMoveFilterGroup(reorderType: ReorderTypes) {
        val id = currentFilterGroupExt!!.filterGroup.id
        currentFilterGroupExt?.let { FilterGroupController.reOrder(reorderType, it.filterGroup) }
        tblFiltersGroups?.items = listFilterGroupsExt
        currentFilterGroupExt = listFilterGroupsExt.first { it.filterGroup.id == id }
        tblFiltersGroups?.selectionModel?.select(currentFilterGroupExt)
    }

    @FXML
    fun doFilterMoveDown(event: ActionEvent?) {
        doMoveFilter(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doFilterMoveToFirst(event: ActionEvent?) {
        doMoveFilter(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doFilterMoveToLast(event: ActionEvent?) {
        doMoveFilter(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doFilterMoveUp(event: ActionEvent?) {
        doMoveFilter(ReorderTypes.MOVE_UP)
    }

    fun doMoveFilter(reorderType: ReorderTypes) {
        val id = currentFilterExt!!.filter.id
        currentFilterExt?.let { FilterController.reOrder(reorderType, it.filter) }
        tblFilters?.items = listFiltersExt
        currentFilterExt = listFiltersExt.first { it.filter.id == id }
        tblFilters?.selectionModel?.select(currentFilterExt)
    }

    @FXML
    fun doFilter(event: ActionEvent?) {


        ShotTmpCdfController.deleteAll()
        tblFiles?.selectionModel?.selectedItems?.forEach { fileExt ->
            Main.shotTmpCdfRepo.addAllByFileId(Main.ccid, fileExt.file.id)
        }

        val shotsIds = FilterController.getFilterExt(currentProjectExt!!, tblFilters?.selectionModel?.selectedItem?.filter!!.id).shotsIds()

        val shotsExt = ShotController.convertSetShotsIdsToListShotsExt(shotsIds, currentProjectExt!!)
        tblShots?.items = FXCollections.observableArrayList(shotsExt)


    }

    @FXML
    fun doCreateVideo(event: ActionEvent?) {

        val shotsExt = tblShots?.items?.toMutableList()
        val fileExt = tblFiles?.items?.first()
        val filterExt = currentFilterExt

        if (shotsExt != null && fileExt != null && filterExt != null) {
            CreateFilterResult(filterExt, currentProjectExt!!, shotsExt, fileExt).run()
        }

    }

    @FXML
    fun doCreateVideoForAllPersons(event: ActionEvent?) {

        ShotTmpCdfController.deleteAll()
        tblFiles?.selectionModel?.selectedItems?.forEach { fileExt ->
            Main.shotTmpCdfRepo.addAllByFileId(Main.ccid, fileExt.file.id)
        }

        val personsExt = PersonController.getListPersonsExt(currentProjectExt!!).filter { PersonController.isPropertyPresent(it.person,"end") }

        if (personsExt.isNotEmpty()) {

            FilterController.deleteFilterExt(currentProjectExt!!, "AllEventsPerson")
            val filterExt = FilterController.getFilterExt(currentProjectExt!!, "AllEventsPerson")
            val filterGroup = FilterGroupController.create(filterExt.filter, true)
            filterGroup.name = "AllEventsPerson"
            FilterGroupController.save(filterGroup)

            filterExt.filter.filterGroups = mutableSetOf(filterGroup)

            personsExt.forEach { personExt ->
                val fileName = "${personExt.projectExt.folderFilters}${IOFile.separator}AllEventsPerson «${personExt.person.name}».${VideoContainers.valueOf(currentProjectExt!!.project.container).extention}"
                if (!IOFile(fileName).exists()) {
                    FilterConditionController.deleteAll(filterGroup)
                    val filterCondition = FilterConditionController.create(filterGroup,"AllEventsPerson",personExt.person.id,personExt.person.name,"",personExt.person::class.java.simpleName,Event::class.java.simpleName,true)
                    filterGroup.filterConditions = mutableSetOf(filterCondition)

                    val shotsIds = FilterController.getFilterExt(currentProjectExt!!, filterExt.filter.id).shotsIds()
                    val shotsExt = ShotController.convertSetShotsIdsToListShotsExt(shotsIds, currentProjectExt!!)
                    val fileExt = tblFiles?.items?.first()

                    if (fileExt != null) {
                        CreateFilterResult(filterExt, currentProjectExt!!, shotsExt, fileExt, fileName).run()
                        println(personExt.person.name)
                    }

                }

            }

//            personsExt.forEach { personExt -> println(personExt.person.name) }

        }



    }
}