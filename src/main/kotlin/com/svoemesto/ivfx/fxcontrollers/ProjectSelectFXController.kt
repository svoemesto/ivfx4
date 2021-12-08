package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.getCurrentDatabase
import com.svoemesto.ivfx.models.Project
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException

class ProjectSelectFXController {

    @FXML
    private var lblDb: Label? = null

    @FXML
    private var tblProjects: TableView<Project>? = null

    @FXML
    private var colOrder: TableColumn<Project, Int>? = null

    @FXML
    private var colName: TableColumn<Project, String>? = null

    @FXML
    private var btnMoveToFirst: Button? = null

    @FXML
    private var btnMoveUp: Button? = null

    @FXML
    private var btnMoveDown: Button? = null

    @FXML
    private var btnMoveToLast: Button? = null

    @FXML
    private var btnOk: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {

        private var mainStage: Stage? = null
        private var currentProject: Project? = null
        private var incomingProject: Project? = null
        private var listProjects: ObservableList<Project> = FXCollections.observableArrayList()

        fun getProject(project: Project?): Project? {
            currentProject = project
            incomingProject = project
            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(ProjectSelectFXController::class.java.getResource("project-select-view.fxml"))
                mainStage?.setTitle("Выбор проекта.")
                mainStage?.setScene(Scene(root))
                mainStage?.initModality(Modality.APPLICATION_MODAL)

                mainStage?.setOnCloseRequest { println("Закрытие окна ProjectSelectFXController.") }

                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы ProjectSelectFXController.")
            mainStage = null
            return currentProject
        }

    }

    @FXML
    fun initialize() {
        println("Инициализация ProjectSelectFXController.")

        lblDb?.text = "БД: ${getCurrentDatabase()?.name}"

        btnMoveToFirst?.isDisable = true
        btnMoveUp?.isDisable = true
        btnMoveToLast?.isDisable = true
        btnMoveDown?.isDisable = true

        listProjects = FXCollections.observableArrayList(ProjectController.getListProjects())

        colOrder?.setCellValueFactory(PropertyValueFactory("order"))
        colName?.setCellValueFactory(PropertyValueFactory("name"))
        tblProjects?.items = listProjects

        tblProjects?.selectionModel?.select(currentProject)

        // обработка события выбора записи в таблице tblProjects
        tblProjects?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            currentProject = newValue
            btnMoveToFirst?.isDisable = currentProject == listProjects.first()
            btnMoveUp?.isDisable = currentProject == listProjects.first()
            btnMoveToLast?.isDisable = currentProject == listProjects.last()
            btnMoveDown?.isDisable = currentProject == listProjects.last()

        }

        // событие двойного клика в таблице tblProjects
        tblProjects?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    mainStage?.close()
                }
            }
        }
    }
    
    @FXML
    fun doCancel(event: ActionEvent?) {
        println("Нажатие кнопки Cancel")
        currentProject = incomingProject
        mainStage?.close()
    }

    @FXML
    fun doMoveDown(event: ActionEvent?) {
        doMove(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doMoveToFirst(event: ActionEvent?) {
        doMove(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doMoveToLast(event: ActionEvent?) {
        doMove(ReorderTypes.MOVE_TO_LAST)
    }

    @FXML
    fun doMoveUp(event: ActionEvent?) {
        doMove(ReorderTypes.MOVE_UP)
    }

    fun doMove(reorderType: ReorderTypes) {
        val id = currentProject?.id
        currentProject?.let { ProjectController.reOrder(reorderType, it) }
        listProjects = FXCollections.observableArrayList(ProjectController.getListProjects())
        tblProjects?.items = listProjects
        currentProject = listProjects.filter { it.id == id }.first()
        tblProjects?.selectionModel?.select(currentProject)
    }
    @FXML
    fun doOk(event: ActionEvent?) {
        println("Нажатие кнопки OK")
        mainStage?.close()
    }
}