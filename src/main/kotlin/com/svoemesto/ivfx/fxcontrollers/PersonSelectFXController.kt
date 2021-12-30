package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForProject
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException
import java.util.*
import java.util.function.Predicate


class PersonSelectFXController {

    @FXML
    private var fldFind: TextField? = null

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
        private var listLastSelectedPersons: MutableList<PersonExt> = mutableListOf()
    }

    private var listPersonsExtAll: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private val runListThreadsPersonFlagIsDone = SimpleBooleanProperty(false)
    private var filteredPersonExt: FilteredList<PersonExt>? = FilteredList(listPersonsExtAll)

    fun getPersonExt(projectExt: ProjectExt, personExt: PersonExt? = null): PersonExt? {
        mainStage = Stage()
        currentProjectExt = projectExt
        currentPersonExt = personExt

        try {
            val root = FXMLLoader.load<Parent>(PersonSelectFXController::class.java.getResource("person-select-view.fxml"))
            mainStage?.scene = Scene(root)
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы PersonSelectFXController.")
        mainStage = null

        return currentPersonExt
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
                val tmpList: ObservableList<PersonExt> = FXCollections.observableArrayList()
                listLastSelectedPersons.forEach { lastPerson ->
                    tmpList.add(listPersonsExtAll.first { lastPerson.person.id == it.person.id })
                }
                listPersonsExtAll.forEach {
                    if (!tmpList.contains(it)) tmpList.add(it)
                }
                listPersonsExtAll = tmpList
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
                        doOk(null)
                    }

                }

            }
        }


        // обработка события отпускания кнопки в поле поиска fldFind
        fldFind!!.onKeyReleased = EventHandler { e: KeyEvent? ->
            fldFind!!.textProperty()
                .addListener(ChangeListener { v: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
                    filteredPersonExt!!.setPredicate(Predicate<PersonExt?> { personExt: PersonExt? ->
                        if (newValue == null || newValue.isEmpty()) {
                            return@Predicate true
                        }
                        val lowerCaseFilter = newValue.lowercase(Locale.getDefault())
                        if (personExt!!.person.name.lowercase().contains(lowerCaseFilter)) return@Predicate true
                        return@Predicate false
                    } as Predicate<in PersonExt?>?)
                })
            val sortedTags: SortedList<PersonExt> = SortedList(filteredPersonExt)
            sortedTags.comparatorProperty().bind(tblPersons!!.comparatorProperty())
            tblPersons!!.items = sortedTags
            if (sortedTags.size > 0) {
                Platform.runLater {
                    tblPersons!!.selectionModel.select(sortedTags[0])
                    tblPersons!!.scrollTo(sortedTags[0])
                }
            }
        }

        // нажатие Enter в поле fldFind - переход на первую запись в таблице tblPersons
        fldFind!!.onKeyPressed = EventHandler { ke: KeyEvent ->
            if (ke.code == KeyCode.ENTER) {
                Platform.runLater {
                    tblPersons!!.requestFocus()
                    tblPersons!!.selectionModel.select(0)
                    tblPersons!!.scrollTo(0)
                }
            }
        }

        // нажатие Enter в поле в таблице tblPersons
        tblPersons!!.onKeyPressed = EventHandler { ke: KeyEvent ->
            if (ke.code == KeyCode.ENTER) {
                btnOk!!.requestFocus()
//                fldFind!!.text = ""
            }
        }


        // обработка события нажатия Enter на кнопке btnOK - нажатие на кнопку OK
        btnOk!!.setOnKeyPressed(EventHandler { ke: KeyEvent ->
            if (ke.code == KeyCode.ENTER) {
                doOk(null)
            }
        })
    }


    @FXML
    fun doCancel(event: ActionEvent?) {
        currentPersonExt = null
        mainStage?.close()
    }

    @FXML
    fun doOk(event: ActionEvent?) {
        if (listLastSelectedPersons.firstOrNull { it.person.id == currentPersonExt!!.person.id } == null) listLastSelectedPersons.add(0, currentPersonExt!!)
        if (listLastSelectedPersons.size > 10) listLastSelectedPersons.removeAt(9)
        mainStage?.close()
    }

    @FXML
    fun doPersonAdd(event: ActionEvent?) {
        PersonEditFXController().editPerson(PersonExt(PersonController.create(currentProjectExt!!.project), currentProjectExt!!))
        initialize()
    }

    @FXML
    fun doPersonDelete(event: ActionEvent?) {
    }
}
