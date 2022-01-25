package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.FilterConditionController
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.modelsext.ProjectExt
import javafx.application.HostServices
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException

class FilterConditionCreateFXController {
    @FXML
    private var rbPerson: RadioButton? = null

    @FXML
    private var tgObject: ToggleGroup? = null

    @FXML
    private var rbTag: RadioButton? = null

    @FXML
    private var btnSelectObject: Button? = null

    @FXML
    private var rbIsIncluded: RadioButton? = null

    @FXML
    private var tgIncluded: ToggleGroup? = null

    @FXML
    private var rbIsNotIncluded: RadioButton? = null

    @FXML
    private var rbShot: RadioButton? = null

    @FXML
    private var tgSubjectClass: ToggleGroup? = null

    @FXML
    private var rbScene: RadioButton? = null

    @FXML
    private var rbEvent: RadioButton? = null

    @FXML
    private var lblName: Label? = null

    @FXML
    private var btnOk: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {
        private var currentProjectExt: ProjectExt? = null
        private var mainStage: Stage? = null
        private var hostServices: HostServices? = null
    }

    private var currentFilterCondition: FilterCondition? = null
    private var currentObjectId: Long? = null
    private var currentObjectName: String = "???"

    fun createFilterCondition(projectExt: ProjectExt, hostServices: HostServices? = null) : FilterCondition? {
        currentProjectExt = projectExt
        mainStage = Stage()
        try {
            val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("filter-condition-create-view.fxml"))
            mainStage?.scene = Scene(root)
            FilterConditionCreateFXController.hostServices = hostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы FilterConditionCreateFXController.")
        mainStage = null
        return currentFilterCondition
    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна FilterConditionCreateFXController.")
        }

        println("Инициализация FilterConditionCreateFXController.")

        updateNameLabel()
    }

    @FXML
    fun doCancel(event: ActionEvent?) {
        currentFilterCondition = null
        mainStage?.close()
    }

    @FXML
    fun doChangeIsIncluded(event: ActionEvent?) {
        updateNameLabel()
    }

    @FXML
    fun doChangeObjectClass(event: ActionEvent?) {
        currentObjectId = null
        currentObjectName = "???"
        updateNameLabel()
    }

    @FXML
    fun doChangeSubjectClass(event: ActionEvent?) {
        updateNameLabel()
    }

    @FXML
    fun doOk(event: ActionEvent?) {
        if (currentObjectId != null) {
            currentFilterCondition = FilterConditionController.create(
                currentProjectExt!!,
                getCurrentName(),
                currentObjectId!!,
                if (rbPerson!!.isSelected) Person::class.java.simpleName else Tag::class.java.simpleName,
                when(true) {
                    rbShot!!.isSelected -> com.svoemesto.ivfx.models.Shot::class.java.simpleName
                    rbScene!!.isSelected -> com.svoemesto.ivfx.models.Scene::class.java.simpleName
                    rbEvent!!.isSelected -> com.svoemesto.ivfx.models.Event::class.java.simpleName
                    else -> "Error"
                },
                rbIsIncluded!!.isSelected
            )
            mainStage?.close()
        }

    }

    private fun getCurrentName() : String {
        val objectClass = if (rbPerson!!.isSelected) Person::class.java.simpleName else Tag::class.java.simpleName
        val subjectClass = when(true) {
            rbShot!!.isSelected -> com.svoemesto.ivfx.models.Shot::class.java.simpleName
            rbScene!!.isSelected -> com.svoemesto.ivfx.models.Scene::class.java.simpleName
            rbEvent!!.isSelected -> com.svoemesto.ivfx.models.Event::class.java.simpleName
            else -> "Error"
        }
        val including = if(rbIsIncluded!!.isSelected) " " else " NOT "
        return "$objectClass «$currentObjectName» is${including}included in $subjectClass"
    }

    private fun updateNameLabel() {
        lblName!!.text = getCurrentName()
        btnOk!!.isDisable = currentObjectId == null
    }

    @FXML
    fun doSelectObject(event: ActionEvent?) {
        if (rbPerson!!.isSelected) {
            val selectedPerson = PersonSelectFXController().getPersonExt(currentProjectExt!!, null)
            if (selectedPerson != null) {
                currentObjectId = selectedPerson.person.id
                currentObjectName = selectedPerson.person.name
            }
        } else {

        }
        updateNameLabel()
    }
}