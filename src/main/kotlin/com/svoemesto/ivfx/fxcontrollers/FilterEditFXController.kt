package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.FilterConditionExtController
import com.svoemesto.ivfx.controllers.FilterExtController
import com.svoemesto.ivfx.controllers.FilterGroupExtController
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import com.svoemesto.ivfx.modelsext.ProjectExt
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
import javafx.scene.control.RadioButton
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

class FilterEditFXController {

    // FILTER CONDITION
    @FXML
    private var tblFiltersConditions: TableView<FilterConditionExt>? = null

    @FXML
    private var colFilterConditionName: TableColumn<FilterConditionExt, String>? = null

    @FXML
    private var btnAddFilterConditionToFilterGroup: Button? = null

    @FXML
    private var btnAddNewFilterCondition: Button? = null

    @FXML
    private var btnDeleteFilterCondition: Button? = null

    // FILTER GROUP
    @FXML
    private var tblFiltersGroups: TableView<FilterGroupExt>? = null

    @FXML
    private var colFilterGroupName: TableColumn<FilterGroupExt, String>? = null

    @FXML
    private var colFilterGroupIsAnd: TableColumn<FilterGroupExt, String>? = null

    @FXML
    private var btnAddFilterGroupToFilter: Button? = null

    @FXML
    private var btnAddNewFilterGroup: Button? = null

    @FXML
    private var btnDeleteFilterGroup: Button? = null

    @FXML
    private var fldFilterGroupName: TextField? = null

    @FXML
    private var rbFilterGroupIsAnd: RadioButton? = null

    @FXML
    private var tgFilterGroup: ToggleGroup? = null

    @FXML
    private var rbFilterGroupIsOr: RadioButton? = null

    // FILTER GROUP CONDITION
    @FXML
    private var tblFiltersGroupsConditions: TableView<FilterConditionExt>? = null

    @FXML
    private var colFilterGroupConditionName: TableColumn<FilterConditionExt, String>? = null

    // FILTER
    @FXML
    private var tblFilters: TableView<FilterExt>? = null

    @FXML
    private var colFilterName: TableColumn<FilterExt, String>? = null

    @FXML
    private var colFilterIsAnd: TableColumn<FilterExt, String>? = null

    @FXML
    private var btnAddNewFilter: Button? = null

    @FXML
    private var btnDeleteFilter: Button? = null

    @FXML
    private var fldFilterName: TextField? = null

    @FXML
    private var rbFilterIsAnd: RadioButton? = null

    @FXML
    private var tgFilter: ToggleGroup? = null

    @FXML
    private var rbFilterIsOr: RadioButton? = null

    // FILTER FILTER GROUP
    @FXML
    private var tblFiltersFiltersGroups: TableView<FilterGroupExt>? = null

    @FXML
    private var colFilterFilterGroupName: TableColumn<FilterGroupExt, String>? = null

    @FXML
    private var colFilterFilterGroupIsAnd: TableColumn<FilterGroupExt, String>? = null


    companion object {
        private var currentProjectExt: ProjectExt? = null
        private var mainStage: Stage? = null
        private var hostServices: HostServices? = null
    }

    private var listFilterConditionsExt: ObservableList<FilterConditionExt> = FXCollections.observableArrayList()
    private var listFilterGroupsExt: ObservableList<FilterGroupExt> = FXCollections.observableArrayList()
    private var listFilterFilterGroupsExt: ObservableList<FilterGroupExt> = FXCollections.observableArrayList()
    private var listFilterGroupConditionsExt: ObservableList<FilterConditionExt> = FXCollections.observableArrayList()
    private var listFiltersExt: ObservableList<FilterExt> = FXCollections.observableArrayList()
    private var currentFilterConditionExt: FilterConditionExt? = null
    private var currentFilterGroupConditionExt: FilterConditionExt? = null
    private var currentFilterGroupExt: FilterGroupExt? = null
    private var currentFilterFilterGroupExt: FilterGroupExt? = null
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

        colFilterConditionName?.cellValueFactory = PropertyValueFactory("name")
        listFilterConditionsExt = FXCollections.observableList(FilterConditionExtController.getList(currentProjectExt!!))
        tblFiltersConditions?.items = listFilterConditionsExt

        colFilterGroupName?.cellValueFactory = PropertyValueFactory("name")
        colFilterGroupIsAnd?.cellValueFactory = PropertyValueFactory("andText")
        listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
        tblFiltersGroups?.items = listFilterGroupsExt

        colFilterName?.cellValueFactory = PropertyValueFactory("name")
        colFilterIsAnd?.cellValueFactory = PropertyValueFactory("andText")
        listFiltersExt = FXCollections.observableList(FilterExtController.getList(currentProjectExt!!))
        tblFilters?.items = listFiltersExt

        colFilterGroupConditionName?.cellValueFactory = PropertyValueFactory("name")
        colFilterFilterGroupName?.cellValueFactory = PropertyValueFactory("name")
        colFilterFilterGroupIsAnd?.cellValueFactory = PropertyValueFactory("andText")


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

        // делаем поле colFilterGroupConditionName таблицы tblFiltersGroupsConditions с переносом по словам и расширяемым по высоте
        colFilterGroupConditionName?.setCellFactory { param: TableColumn<FilterConditionExt?, String?>? ->
            val cell: TableCell<FilterConditionExt, String> = TableCell<FilterConditionExt, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilterGroupConditionName!!.widthProperty())
            cell
        }

        // делаем поле colFilterFilterGroupName таблицы tblFiltersFiltersGroups с переносом по словам и расширяемым по высоте
        colFilterFilterGroupName?.setCellFactory { param: TableColumn<FilterGroupExt?, String?>? ->
            val cell: TableCell<FilterGroupExt, String> = TableCell<FilterGroupExt, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colFilterFilterGroupName!!.widthProperty())
            cell
        }

        // selectedItemProperty
        tblFiltersConditions!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterConditionExt? ->
                if (newValue != null) {
                    currentFilterConditionExt = newValue
                }
            }

        // selectedItemProperty
        tblFiltersGroups!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterGroupExt? ->
                if (newValue != null) {
                    currentFilterGroupExt = newValue
                    fldFilterGroupName?.text = currentFilterGroupExt!!.name
                    rbFilterGroupIsAnd?.isSelected = currentFilterGroupExt!!.filterGroup.isAnd
                    rbFilterGroupIsOr?.isSelected = !currentFilterGroupExt!!.filterGroup.isAnd
                    listFilterGroupConditionsExt = FXCollections.observableArrayList(currentFilterGroupExt!!.filterGroup.filterConditions.map{FilterConditionExt(it)}.toMutableList())
                    tblFiltersGroupsConditions?.items = listFilterGroupConditionsExt
                }
            }

        // selectedItemProperty
        tblFilters!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterExt? ->
                if (newValue != null) {
                    currentFilterExt = newValue
                    fldFilterName?.text = currentFilterExt!!.name
                    rbFilterIsAnd?.isSelected = currentFilterExt!!.filter.isAnd
                    rbFilterIsOr?.isSelected = !currentFilterExt!!.filter.isAnd
                    listFilterFilterGroupsExt = FXCollections.observableArrayList(currentFilterExt!!.filter.filterGroups.map{FilterGroupExt(it)}.toMutableList())
                    tblFiltersFiltersGroups?.items = listFilterFilterGroupsExt
                }
            }

        // selectedItemProperty
        tblFiltersGroupsConditions!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterConditionExt? ->
                if (newValue != null) {
                    currentFilterGroupConditionExt = newValue
                }
            }

        // selectedItemProperty
        tblFiltersFiltersGroups!!.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue: FilterGroupExt? ->
                if (newValue != null) {
                    currentFilterFilterGroupExt = newValue
                }
            }


        // Click
        tblFiltersConditions!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentFilterConditionExt != null) {
                        FilterConditionCreateFXController().createFilterCondition(currentProjectExt!!, currentFilterConditionExt)
                        tblFiltersConditions!!.refresh()
                    }
                }
            }
        }

        // Click
        tblFiltersGroupsConditions!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentFilterGroupConditionExt != null && currentFilterGroupExt != null) {

                        val currentFilterGroupId = currentFilterGroupExt!!.filterGroup.id
                        currentFilterGroupExt?.filterGroup?.filterConditions?.remove(currentFilterGroupConditionExt!!.filterCondition)
                        FilterGroupExtController.save(currentFilterGroupExt!!)

                        listFilterConditionsExt = FXCollections.observableList(FilterConditionExtController.getList(currentProjectExt!!))
                        tblFiltersConditions?.items = listFilterConditionsExt
                        if (currentFilterConditionExt != null) {
                            val currentFilterConditionId = currentFilterConditionExt!!.filterCondition.id
                            tblFiltersConditions?.selectionModel?.select(listFilterConditionsExt.first { it.filterCondition.id == currentFilterConditionId })
                        }

                        listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
                        tblFiltersGroups?.items = listFilterGroupsExt
                        tblFiltersGroups?.selectionModel?.select(listFilterGroupsExt.first { it.filterGroup.id == currentFilterGroupId })

                    }
                }
            }
        }

        // Click
        tblFiltersFiltersGroups!!.onMouseClicked = EventHandler { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentFilterFilterGroupExt != null && currentFilterExt != null) {

                        val currentFilterId = currentFilterExt!!.filter.id

                        currentFilterExt!!.filter.filterGroups.remove(currentFilterFilterGroupExt!!.filterGroup)
                        FilterExtController.save(currentFilterExt!!)
                        listFilterFilterGroupsExt = FXCollections.observableArrayList(currentFilterExt!!.filter.filterGroups.map{FilterGroupExt(it)}.toMutableList())
                        tblFiltersFiltersGroups?.items = listFilterFilterGroupsExt

                        listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
                        tblFiltersGroups?.items = listFilterGroupsExt
                        if (currentFilterGroupExt != null) {
                            val currentFilterGroupId = currentFilterGroupExt!!.filterGroup.id
                            tblFiltersGroups?.selectionModel?.select(listFilterGroupsExt.first { it.filterGroup.id == currentFilterGroupId })
                        }

                        listFiltersExt = FXCollections.observableList(FilterExtController.getList(currentProjectExt!!))
                        tblFilters?.items = listFiltersExt
                        tblFilters?.selectionModel?.select(listFiltersExt.first { it.filter.id == currentFilterId })

                    }
                }
            }
        }

        fldFilterGroupName!!.textProperty().addListener { _, _, newValue ->
            if (currentFilterGroupExt != null) {
                currentFilterGroupExt?.filterGroup?.name = newValue
                FilterGroupExtController.save(currentFilterGroupExt!!)
                tblFiltersGroups?.refresh()
            }
        }

        fldFilterName!!.textProperty().addListener { _, _, newValue ->
            if (currentFilterExt != null) {
                currentFilterExt?.filter?.name = newValue
                FilterExtController.save(currentFilterExt!!)
                tblFilters?.refresh()
            }
        }


    }

    @FXML //Создание нового фильтра
    fun doAddNewFilter(event: ActionEvent?) {
        val filter = FilterExtController.create(currentProjectExt!!.project)
        listFiltersExt = FXCollections.observableList(FilterExtController.getList(currentProjectExt!!))
        tblFilters?.items = listFiltersExt
        tblFilters?.selectionModel?.select(filter)
    }

    @FXML // Создание нового условия
    fun doAddNewFilterCondition(event: ActionEvent?) {
        val filterCondition = FilterConditionCreateFXController().createFilterCondition(currentProjectExt!!)
        if (filterCondition != null) {
            listFilterConditionsExt = FXCollections.observableList(FilterConditionExtController.getList(currentProjectExt!!))
            tblFiltersConditions?.items = listFilterConditionsExt
            tblFiltersConditions?.selectionModel?.select(filterCondition)
        }
    }

    @FXML // Создание новой группы
    fun doAddNewFilterGroup(event: ActionEvent?) {
        val filterGroup = FilterGroupExtController.create(currentProjectExt!!.project)
        listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
        tblFiltersGroups?.items = listFilterGroupsExt
        tblFiltersGroups?.selectionModel?.select(filterGroup)
    }

    @FXML // Удаление фильтра
    fun doDeleteFilter(event: ActionEvent?) {
    }

    @FXML // Удаление условия
    fun doDeleteFilterCondition(event: ActionEvent?) {
    }

    @FXML // Удаление группы
    fun doDeleteFilterGroup(event: ActionEvent?) {
    }

    @FXML // Изменения IsAnd у группы
    fun doFilterGroupIsAnd(event: ActionEvent?) {
        if (currentFilterGroupExt != null) {
            currentFilterGroupExt?.filterGroup?.isAnd = rbFilterGroupIsAnd!!.isSelected
            FilterGroupExtController.save(currentFilterGroupExt!!)
            tblFiltersGroups?.refresh()
        }
    }

    @FXML // Изменения IsAnd у фильтра
    fun doFilterIsAnd(event: ActionEvent?) {
        if (currentFilterExt != null) {
            currentFilterExt?.filter?.isAnd = rbFilterIsAnd!!.isSelected
            FilterExtController.save(currentFilterExt!!)
            tblFilters?.refresh()
        }
    }

    @FXML // Добавление условия к группе
    fun doAddFilterConditionToFilterGroup(event: ActionEvent?) {
        if (currentFilterConditionExt != null && currentFilterGroupExt != null) {

            val currentFilterConditionId = currentFilterConditionExt!!.filterCondition.id
            val currentFilterGroupId = currentFilterGroupExt!!.filterGroup.id

            currentFilterGroupExt!!.filterGroup.filterConditions.add(currentFilterConditionExt!!.filterCondition)
            FilterGroupExtController.save(currentFilterGroupExt!!)
            listFilterGroupConditionsExt = FXCollections.observableArrayList(currentFilterGroupExt!!.filterGroup.filterConditions.map{FilterConditionExt(it)}.toMutableList())
            tblFiltersGroupsConditions?.items = listFilterGroupConditionsExt

            listFilterConditionsExt = FXCollections.observableList(FilterConditionExtController.getList(currentProjectExt!!))
            tblFiltersConditions?.items = listFilterConditionsExt
            tblFiltersConditions?.selectionModel?.select(listFilterConditionsExt.first { it.filterCondition.id == currentFilterConditionId })

            listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
            tblFiltersGroups?.items = listFilterGroupsExt
            tblFiltersGroups?.selectionModel?.select(listFilterGroupsExt.first { it.filterGroup.id == currentFilterGroupId })

        }
    }

    @FXML // Добавление группы к фильтру
    fun doAddFilterGroupToFilter(event: ActionEvent?) {
        if (currentFilterGroupExt != null && currentFilterExt != null) {

            val currentFilterGroupId = currentFilterGroupExt!!.filterGroup.id
            val currentFilterId = currentFilterExt!!.filter.id

            currentFilterExt!!.filter.filterGroups.add(currentFilterGroupExt!!.filterGroup)
            FilterExtController.save(currentFilterExt!!)
            listFilterFilterGroupsExt = FXCollections.observableArrayList(currentFilterExt!!.filter.filterGroups.map{FilterGroupExt(it)}.toMutableList())
            tblFiltersFiltersGroups?.items = listFilterFilterGroupsExt

            listFilterGroupsExt = FXCollections.observableList(FilterGroupExtController.getList(currentProjectExt!!))
            tblFiltersGroups?.items = listFilterGroupsExt
            tblFiltersGroups?.selectionModel?.select(listFilterGroupsExt.first { it.filterGroup.id == currentFilterGroupId })

            listFiltersExt = FXCollections.observableList(FilterExtController.getList(currentProjectExt!!))
            tblFilters?.items = listFiltersExt
            tblFilters?.selectionModel?.select(listFiltersExt.first { it.filter.id == currentFilterId })

        }
    }

}