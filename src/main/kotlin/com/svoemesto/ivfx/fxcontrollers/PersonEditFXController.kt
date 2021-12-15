package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.modelsext.PersonExt
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException


class PersonEditFXController {
    @FXML
    private var lblMediumPreview: Label? = null

    @FXML
    private var fldName: TextField? = null

    @FXML
    private var tblProperties: TableView<Property>? = null

    @FXML
    private var colPropertyKey: TableColumn<Property, String>? = null

    @FXML
    private var colPropertyValue: TableColumn<Property, String>? = null

    @FXML
    private var btnPropertyMoveToFirst: Button? = null

    @FXML
    private var btnPropertyMoveUp: Button? = null

    @FXML
    private var btnPropertyMoveDown: Button? = null

    @FXML
    private var btnPropertyMoveToLast: Button? = null

    @FXML
    private var btnPropertyAdd: Button? = null

    @FXML
    private var btnPropertyDelete: Button? = null

    @FXML
    private var fldPropertyKey: TextField? = null

    @FXML
    private var fldPropertyValue: TextArea? = null

    @FXML
    private var tblTags: TableView<Tag>? = null

    @FXML
    private var colTagName: TableColumn<Tag, String>? = null

    @FXML
    private var btnTagAdd: Button? = null

    @FXML
    private var btnTagDelete: Button? = null

    @FXML
    private var btnOk: Button? = null

    companion object {

        private var mainStage: Stage? = null
        private var currentPersonExt: PersonExt? = null
        private var currentProperty: Property? = null

        fun editPerson(personExt: PersonExt) {
            currentPersonExt = personExt
            mainStage = Stage()

            try {
                val root = FXMLLoader.load<Parent>(PersonEditFXController::class.java.getResource("person-edit-view.fxml"))
                mainStage?.scene = Scene(root)
                mainStage?.initModality(Modality.APPLICATION_MODAL)
                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы PersonEditFXController.")
            mainStage = null

        }

    }

    @FXML
    fun initialize() {
        mainStage?.setOnCloseRequest {
            saveCurrentPerson()
            println("Закрытие окна PersonEditFXController.")
        }
        println("Инициализация PersonEditFXController.")

        lblMediumPreview?.graphic = currentPersonExt?.labelMedium
        fldName?.text = currentPersonExt?.person?.name

    }

    fun saveCurrentProperty() {

        if (currentProperty != null) {
            var needToSave = false

            var tmp: String = fldPropertyKey?.text ?: ""
            if (tmp != currentProperty?.key) {
                currentProperty?.key = tmp
                needToSave = true
            }

            tmp = fldPropertyValue?.text ?: ""
            if (tmp != currentProperty?.value) {
                currentProperty?.value = tmp
                needToSave = true
            }

            if (needToSave) {
                PropertyController.save(currentProperty!!)
                tblProperties?.refresh()
            }

        }
    }

    fun saveCurrentPerson() {

        var needToSave = false

        var tmp: String = fldName?.text ?: ""
        if (tmp != currentPersonExt?.person?.name) {
            currentPersonExt?.person?.name = tmp
            needToSave = true
        }

        if (needToSave) {
            PersonController.save(currentPersonExt?.person!!)
        }

    }


    @FXML
    fun doOk(event: ActionEvent?) {
        saveCurrentPerson()
        mainStage?.close()
    }

    @FXML
    fun doTagAdd(event: ActionEvent?) {
    }

    @FXML
    fun doTagDelete(event: ActionEvent?) {
    }

    @FXML
    fun doFilePropertyMoveToLast(event: ActionEvent?) {
    }

    @FXML
    fun doPropertyAdd(event: ActionEvent?) {
    }

    @FXML
    fun doPropertyDelete(event: ActionEvent?) {
    }

    @FXML
    fun doPropertyMoveDown(event: ActionEvent?) {
    }

    @FXML
    fun doPropertyMoveToFirst(event: ActionEvent?) {
    }

    @FXML
    fun doPropertyMoveUp(event: ActionEvent?) {
    }
}
