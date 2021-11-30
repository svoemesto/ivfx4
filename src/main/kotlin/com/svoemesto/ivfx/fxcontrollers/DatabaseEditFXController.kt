package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.H2database
import com.svoemesto.ivfx.saveH2database
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException

class DatabaseEditFXController {
    
    @FXML
    private var fldId: TextField? = null

    @FXML
    private var fldName: TextField? = null

    @FXML
    private var fldDriver: TextField? = null

    @FXML
    private var fldUrl: TextField? = null

    @FXML
    private var fldUser: TextField? = null

    @FXML
    private var fldPassword: TextField? = null

    @FXML
    private var btlOk: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {

        private var mainStage: Stage? = null
        private var currentDatabase: H2database = H2database()

        fun editH2database(h2database: H2database): H2database {
            currentDatabase = h2database
            try {
                val root = FXMLLoader.load<Parent>(DatabaseEditFXController::class.java.getResource("database-edit-view.fxml"))
                mainStage = Stage()
                mainStage?.setTitle(if (h2database.id != null) "Редактирование базы данных" else "Добавление базы данных")
                mainStage?.setScene(Scene(root))
                mainStage?.initModality(Modality.APPLICATION_MODAL)

                mainStage?.setOnCloseRequest { println("Закрытие окна DatabaseEditFXController.") }

                mainStage?.showAndWait()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы DatabaseEditFXController.")
            mainStage = null
            return currentDatabase
        }

    }

    @FXML
    fun initialize() {
        println("Инициализация DatabaseEditFXController.")

        fldId?.isDisable = true
        fldId?.text = currentDatabase.id.toString()
        fldName?.text = currentDatabase.name.toString()
        fldDriver?.text = currentDatabase.driver.toString()
        fldUrl?.text = currentDatabase.url.toString()
        fldUser?.text = currentDatabase.user.toString()
        fldPassword?.text = currentDatabase.password.toString()

    }
    
    @FXML
    fun doCancel(event: ActionEvent?) {
        println("Нажатие кнопки Cancel")
        mainStage?.close()
    }

    @FXML
    fun doOk(event: ActionEvent?) {
        println("Нажатие кнопки OK")
        currentDatabase.name = fldName?.text
        currentDatabase.driver = fldDriver?.text
        currentDatabase.url = fldUrl?.text
        currentDatabase.user = fldUser?.text
        currentDatabase.password = fldPassword?.text
        saveH2database(currentDatabase)
        mainStage?.close()
    }
}