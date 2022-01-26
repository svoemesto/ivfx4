package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.FilterConditionExtController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.TagController
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import com.svoemesto.ivfx.modelsext.ProjectExt
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
    private var lblHeader: Label? = null

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
        private var initFilterConditionExt: FilterConditionExt? = null
    }

    private var currentFilterConditionExt: FilterConditionExt? = null
    private var currentObjectId: Long? = null
    private var currentObjectName: String = "???"

    fun createFilterCondition(projectExt: ProjectExt, initFilterConditionExt: FilterConditionExt? = null) : FilterConditionExt? {
        currentProjectExt = projectExt
        mainStage = Stage()
        try {
            FilterConditionCreateFXController.initFilterConditionExt = initFilterConditionExt
            val root = FXMLLoader.load<Parent>(ShotsEditFXController::class.java.getResource("filter-condition-create-view.fxml"))
            mainStage?.scene = Scene(root)
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы FilterConditionCreateFXController.")
        mainStage = null
        FilterConditionCreateFXController.initFilterConditionExt = null
        return currentFilterConditionExt
    }

    @FXML
    fun initialize() {

        mainStage?.setOnCloseRequest {
            println("Закрытие окна FilterConditionCreateFXController.")
        }

        println("Инициализация FilterConditionCreateFXController.")

        if (initFilterConditionExt != null) {
            rbIsIncluded!!.isSelected = initFilterConditionExt!!.filterCondition.isIncluded
            rbIsNotIncluded!!.isSelected = !initFilterConditionExt!!.filterCondition.isIncluded
            currentObjectId = initFilterConditionExt!!.filterCondition.objectId
            when(initFilterConditionExt!!.filterCondition.subjectClass) {
                com.svoemesto.ivfx.models.Shot::class.java.simpleName -> rbShot!!.isSelected = true
                com.svoemesto.ivfx.models.Scene::class.java.simpleName -> rbScene!!.isSelected = true
                com.svoemesto.ivfx.models.Event::class.java.simpleName -> rbEvent!!.isSelected = true
            }
            when(initFilterConditionExt!!.filterCondition.objectClass) {
                Person::class.java.simpleName -> {
                    rbPerson!!.isSelected = true
                    currentObjectName = PersonController.getById(currentObjectId!!).name
                }
                Tag::class.java.simpleName -> {
                    rbTag!!.isSelected = true
                    currentObjectName = TagController.getById(currentObjectId!!).name
                }
            }
            currentFilterConditionExt = initFilterConditionExt
            lblHeader!!.text = "Edit filter condition"
            btnOk!!.text = lblHeader!!.text
        }

        updateNameLabel()
    }

    @FXML
    fun doCancel(event: ActionEvent?) {
        currentFilterConditionExt = null
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
            if (currentFilterConditionExt == null) {
                currentFilterConditionExt = FilterConditionExtController.create(
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
            } else {
                currentFilterConditionExt!!.filterCondition.name = getCurrentName()
                currentFilterConditionExt!!.filterCondition.objectId = currentObjectId!!
                currentFilterConditionExt!!.filterCondition.objectClass = if (rbPerson!!.isSelected) Person::class.java.simpleName else Tag::class.java.simpleName
                currentFilterConditionExt!!.filterCondition.subjectClass =
                    when(true) {
                        rbShot!!.isSelected -> com.svoemesto.ivfx.models.Shot::class.java.simpleName
                        rbScene!!.isSelected -> com.svoemesto.ivfx.models.Scene::class.java.simpleName
                        rbEvent!!.isSelected -> com.svoemesto.ivfx.models.Event::class.java.simpleName
                        else -> "Error"
                    }
                currentFilterConditionExt!!.filterCondition.isIncluded = rbIsIncluded!!.isSelected
                FilterConditionExtController.save(currentFilterConditionExt!!)

            }

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