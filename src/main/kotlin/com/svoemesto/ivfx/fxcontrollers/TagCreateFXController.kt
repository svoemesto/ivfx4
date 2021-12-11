package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.controllers.TagController
import com.svoemesto.ivfx.enums.TagType
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.modelsext.FrameExt
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException


class TagCreateFXController {
    @FXML
    private var ap: AnchorPane? = null

    @FXML
    private var cbTagType: ComboBox<TagType>? = null

    @FXML
    private var fldTagName: TextField? = null

    @FXML
    private var btnOK: Button? = null

    @FXML
    private var btnCancel: Button? = null

    companion object {

        private var mainStage: Stage? = null
        private var currentProject: Project? = null
        private var currentTagType: TagType? = null
        private var currentTagName: String = ""
        private var currentFrameExt: FrameExt? = null
        private var currentDisableChoiceTagType: Boolean = false
        private var currentTag: Tag? = null
        private var listTagsTypes: ObservableList<TagType> = FXCollections.observableArrayList()

        fun getNewTag(project: Project, tagType: TagType, name: String, frameExt: FrameExt, disableChoiceTagType: Boolean = false): Tag? {
            currentProject = project
            currentTagType = tagType
            currentTagName = name
            currentFrameExt = frameExt
            currentDisableChoiceTagType = disableChoiceTagType

            mainStage = Stage()
            try {
                val root = FXMLLoader.load<Parent>(TagCreateFXController::class.java.getResource("tag-create-view.fxml"))
                mainStage?.title = "Создание нового тэга."
                mainStage?.scene = Scene(root)
                mainStage?.initModality(Modality.APPLICATION_MODAL)

                mainStage?.setOnCloseRequest { println("Закрытие окна TagCreateFXController.") }

                mainStage?.showAndWait()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            println("Завершение работы TagCreateFXController.")
            mainStage = null
            return currentTag

        }

    }

    @FXML
    fun initialize() {
        println("Инициализация TagCreateFXController.")

        listTagsTypes = FXCollections.observableList(TagType.values().asList())
        cbTagType!!.items = listTagsTypes
        if (currentTagType == null) currentTagType = TagType.DESCRIPTION
        cbTagType!!.value = currentTagType
        cbTagType!!.isDisable = currentDisableChoiceTagType
        fldTagName!!.text = currentTagName

        cbTagType!!.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (newValue != null) {
                currentTagType = newValue
            }
        }

        fldTagName!!.onKeyPressed = EventHandler { ke: KeyEvent ->
            if (ke.code == KeyCode.ENTER) {
                btnOK!!.requestFocus()
            }
        }

        btnOK!!.onKeyPressed = EventHandler { ke: KeyEvent ->
            if (ke.code == KeyCode.ENTER) {
                doOK(null)
            }
        }
    }

    @FXML
    fun doCancel(event: ActionEvent?) {
        currentTag = null
        mainStage?.close()
    }

    @FXML
    fun doOK(event: ActionEvent?) {
        currentTag = TagController.create(currentProject!!, fldTagName!!.text, currentTagType!!)
        mainStage?.close()
    }
}
