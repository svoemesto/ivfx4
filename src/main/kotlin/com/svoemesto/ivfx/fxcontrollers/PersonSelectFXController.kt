package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForProject
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException


class PersonSelectFXController {
    @FXML
    private var tblPersons: TableView<PersonExt>? = null

    @FXML
    private var colPersonName: TableColumn<PersonExt, String>? = null

    @FXML
    private var btnPersonAdd: Button? = null

    @FXML
    private var btnPersonDelete: Button? = null

    @FXML
    private var btnOk: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {
        private var currentPersonExt: PersonExt? = null
        private var currentProjectExt: ProjectExt? = null
        private var mainStage: Stage? = null
        private var listPersonsExtAll: ObservableList<PersonExt> = FXCollections.observableArrayList()
        private val runListThreadsPersonFlagIsDone = SimpleBooleanProperty(false)
        fun getPersonExt(projectExt: ProjectExt, personExt: PersonExt? = null): PersonExt? {
            mainStage = Stage()
            currentProjectExt = projectExt
            currentPersonExt = personExt

            try {
                val root = FXMLLoader.load<Parent>(PersonSelectFXController::class.java.getResource("person-select-view.fxml"))
                mainStage?.scene = Scene(root)
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы PersonSelectFXController.")
            mainStage = null

            return currentPersonExt
        }

    }

    @FXML
    fun initialize() {
        mainStage?.setOnCloseRequest {
            println("Закрытие окна PersonSelectFXController.")
        }

        println("Инициализация PersonSelectFXController.")

        colPersonName?.cellValueFactory = PropertyValueFactory("labelSmall")
        tblPersons!!.items = listPersonsExtAll

        runListThreadsPersonFlagIsDone.set(false)

        var listThreads: MutableList<Thread> = mutableListOf()
        listThreads.add(LoadListPersonsExtForProject(listPersonsExtAll, currentProjectExt!!))
        var runListThreadsFrames = RunListThreads(listThreads, runListThreadsPersonFlagIsDone)
        runListThreadsFrames.start()

        runListThreadsPersonFlagIsDone.addListener { observable, oldValue, newValue ->
            if (newValue == true) {
                tblPersons!!.items = listPersonsExtAll
                if (currentPersonExt != null) {
                    tblPersons!!.selectionModel.select(currentPersonExt)
                }
            }
        }

        tblPersons!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out PersonExt?>?, oldValue: PersonExt?, newValue: PersonExt? ->
                if (newValue != null) {

                    currentPersonExt = newValue

                }
            }

        tblPersons!!.onMouseClicked = EventHandler { mouseEvent ->

            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (currentPersonExt != null) {
                        mainStage?.close()
                    }

                }

            }
        }

    }


    @FXML
    fun doCancel(event: ActionEvent?) {
        currentPersonExt = null
        mainStage?.close()
    }

    @FXML
    fun doOk(event: ActionEvent?) {
        mainStage?.close()
    }

    @FXML
    fun doPersonAdd(event: ActionEvent?) {
        PersonController.create(currentProjectExt!!.project)
        initialize()
    }

    @FXML
    fun doPersonDelete(event: ActionEvent?) {
    }
}
