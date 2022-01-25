package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.ProjectExt
import javafx.application.HostServices
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.RadioButton
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException

class FilterEditFXController {

    // FILTER CONDITION
    @FXML
    private var tblFiltersConditions: TableView<FilterCondition>? = null

    @FXML
    private var colFilterConditionName: TableColumn<FilterCondition, String>? = null

    @FXML
    private var btnAddFilterConditionToFilterGroup: Button? = null

    @FXML
    private var btnAddNewFilterCondition: Button? = null

    @FXML
    private var btnDeleteFilterCondition: Button? = null

    // FILTER GROUP
    @FXML
    private var tblFiltersGroups: TableView<FilterGroup>? = null

    @FXML
    private var colFilterGroupName: TableColumn<FilterGroup, String>? = null

    @FXML
    private var colFilterGroupIsAnd: TableColumn<FilterGroup, String>? = null

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
    private var tblFiltersGroupsConditions: TableView<FilterCondition>? = null

    @FXML
    private var colFilterGroupConditionName: TableColumn<FilterCondition, String>? = null

    // FILTER
    @FXML
    private var tblFilters: TableView<Filter>? = null

    @FXML
    private var colFilterName: TableColumn<Filter, String>? = null

    @FXML
    private var colFilterIsAnd: TableColumn<Filter, String>? = null

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
    private var tblFiltersFiltersGroups: TableView<FilterGroup>? = null

    @FXML
    private var colFilterFilterGroup: TableColumn<FilterGroup, String>? = null


    companion object {
        private var currentProjectExt: ProjectExt? = null
        private var mainStage: Stage? = null
        private var hostServices: HostServices? = null
    }

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
    }

    @FXML //Создание нового фильтра
    fun doAddNewFilter(event: ActionEvent?) {
    }

    @FXML // Создание нового условия
    fun doAddNewFilterCondition(event: ActionEvent?) {
        val filterCondition = FilterConditionCreateFXController().createFilterCondition(currentProjectExt!!)
    }

    @FXML // Создание новой группы
    fun doAddNewFilterGroup(event: ActionEvent?) {
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
    }

    @FXML // Изменения IsAnd у фильтра
    fun doFilterIsAnd(event: ActionEvent?) {
    }

    @FXML // Добавление условия к группе
    fun doAddFilterConditionToFilterGroup(event: ActionEvent?) {
    }

    @FXML // Добавление группы к фильтру
    fun doAddFilterGroupToFilter(event: ActionEvent?) {
    }

}