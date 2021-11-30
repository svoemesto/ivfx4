package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.H2database
import com.svoemesto.ivfx.deleteH2Database
import com.svoemesto.ivfx.getListH2databases
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
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

class DatabaseSelectFXController {
    @FXML
    private var tblDatabases: TableView<H2database>? = null

    @FXML
    private var colDbName: TableColumn<H2database, String>? = null

    @FXML
    private var btnSelectDb: Button? = null

    @FXML
    private var btnEditDb: Button? = null

    @FXML
    private var btnCreateNewDb: Button? = null

    @FXML
    private var btnDeleteDb: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {

        private var mainStage: Stage = Stage()
        private var currentDatabase: H2database? = null
        private var incomingDatabase: H2database? = null
        private var listDatabases: ObservableList<H2database> = FXCollections.observableArrayList()

        fun getDatabase(h2database: H2database?): H2database? {
            currentDatabase = h2database
            incomingDatabase = h2database
            try {
                val root = FXMLLoader.load<Parent>(DatabaseSelectFXController::class.java.getResource("database-select-view.fxml"))
                mainStage.setTitle("Выбор базы данных")
                mainStage.setScene(Scene(root))
                mainStage.initModality(Modality.APPLICATION_MODAL)

                mainStage.setOnCloseRequest { println("Закрытие окна DatabaseSelectFXController.") }

                mainStage.showAndWait()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы DatabaseSelectFXController.")
            return currentDatabase
        }

    }


    @FXML
    fun initialize() {
        println("Инициализация DatabaseSelectFXController.")

        listDatabases = FXCollections.observableArrayList(getListH2databases())

        colDbName?.setCellValueFactory(PropertyValueFactory("name"))
        tblDatabases?.items = listDatabases

        tblDatabases?.selectionModel?.select(currentDatabase)
//        listDatabases.forEach { if (it.id == currentDatabase?.id) tblDatabases?.selectionModel?.select(it) }

        // обработка события выбора записи в таблице tblDatabases
        tblDatabases?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            currentDatabase = newValue
        }


        // событие двойного клика в таблице tblFilters
        tblDatabases?.setOnMouseClicked { mouseEvent ->
            //событие двойного клика
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    mainStage.close()
                }
            }
        }
    }


    @FXML
    fun doCancel(event: ActionEvent?) {
        println("Нажатие кнопки Cancel")
        currentDatabase = incomingDatabase
        mainStage.close()
    }

    @FXML
    fun doCreateNewDb(event: ActionEvent?) {
        println("Нажатие кнопки CreateNewDb")
        DatabaseEditFXController.editH2database(H2database())
        listDatabases = FXCollections.observableArrayList(getListH2databases())
        tblDatabases?.items = listDatabases
    }

    @FXML
    fun doDeleteDb(event: ActionEvent?) {
        println("Нажатие кнопки DeleteDb")
        currentDatabase?.let { deleteH2Database(it) }
        listDatabases = FXCollections.observableArrayList(getListH2databases())
        tblDatabases?.items = listDatabases
    }

    @FXML
    fun doEditDb(event: ActionEvent?) {
        println("Нажатие кнопки EditDb")
        currentDatabase?.let { DatabaseEditFXController.editH2database(it) }
        listDatabases = FXCollections.observableArrayList(getListH2databases())
        tblDatabases?.refresh()
    }

    @FXML
    fun doSelectDb(event: ActionEvent?) {
        println("Нажатие кнопки SelectDb")
        mainStage.close()
    }

}