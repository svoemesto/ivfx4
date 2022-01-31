package com.svoemesto.ivfx.fxcontrollers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.threads.RunListThreads
import com.svoemesto.ivfx.threads.loadlists.LoadListPersonsExtForProject
import javafx.application.HostServices
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
import javafx.geometry.Bounds
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.IOException
import java.util.*
import java.util.function.Predicate


class PersonEditFXController {

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
    private var btnTagAdd: Button? = null

    @FXML
    private var btnTagDelete: Button? = null

    @FXML
    private var btnOk: Button? = null

    companion object {
        private var currentPersonExt: PersonExt? = null
        private var currentProjectExt: ProjectExt? = null
        private var hostServices: HostServices? = null
        private var mainStage: Stage? = null
        private var listLastSelectedPersons: MutableList<PersonExt> = mutableListOf()
    }

    private var currentProperty: Property? = null

    private var listProperties: ObservableList<Property> = FXCollections.observableArrayList()
    private var listPersonsExtAll: ObservableList<PersonExt> = FXCollections.observableArrayList()
    private var filteredPersonExt: FilteredList<PersonExt>? = FilteredList(listPersonsExtAll)
    private val runListThreadsPersonFlagIsDone = SimpleBooleanProperty(false)

    fun editPerson(projectExt: ProjectExt, personExt: PersonExt? = null, incomingHostServices: HostServices? = null) {
        currentPersonExt = personExt
        currentProjectExt = projectExt
        mainStage = Stage()

        try {
            val root = FXMLLoader.load<Parent>(PersonEditFXController::class.java.getResource("person-edit-view.fxml"))
            mainStage?.scene = Scene(root)
            hostServices = incomingHostServices
            mainStage?.initModality(Modality.WINDOW_MODAL)
            mainStage?.showAndWait()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        println("Завершение работы PersonEditFXController.")
        mainStage = null

    }

    @FXML
    fun initialize() {
        mainStage?.setOnCloseRequest {
            saveCurrentPerson()
            println("Закрытие окна PersonEditFXController.")
        }
        println("Инициализация PersonEditFXController.")

        colPersonName?.cellValueFactory = PropertyValueFactory("labelSmall")
        tblPersons!!.items = listPersonsExtAll

        runListThreadsPersonFlagIsDone.set(false)

        val listThreads: MutableList<Thread> = mutableListOf()
        listThreads.add(LoadListPersonsExtForProject(listPersonsExtAll, currentProjectExt!!))
        val runListThreadsFrames = RunListThreads(listThreads, runListThreadsPersonFlagIsDone)
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
                    tblPersons!!.selectionModel.select(listPersonsExtAll.first { it.person.id ==  currentPersonExt!!.person.id} )
                }
            }
        }

        tblPersons!!.selectionModel.selectedItemProperty()
            .addListener { v: ObservableValue<out PersonExt?>?, oldValue: PersonExt?, newValue: PersonExt? ->
                if (newValue != null) {

                    saveCurrentProperty()
                    saveCurrentPerson()

                    currentPersonExt = newValue

                    currentProperty = null

                    btnPropertyMoveToFirst?.isDisable = currentProperty == null
                    btnPropertyMoveUp?.isDisable = currentProperty == null
                    btnPropertyMoveToLast?.isDisable = currentProperty == null
                    btnPropertyMoveDown?.isDisable = currentProperty == null
                    btnPropertyDelete?.isDisable = currentProperty == null
                    fldPropertyKey?.isDisable = currentProperty == null
                    fldPropertyValue?.isDisable = currentProperty == null

                    fldPropertyKey?.text = ""
                    fldPropertyValue?.text = ""

                    lblMediumPreview?.graphic = currentPersonExt?.labelMedium
                    fldName?.text = currentPersonExt?.person?.name

                    listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
                        currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
                    tblProperties?.items = listProperties

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

        colPropertyKey?.cellValueFactory = PropertyValueFactory("key")
        colPropertyValue?.cellValueFactory = PropertyValueFactory("value")

        tblProperties?.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    if (hostServices != null && currentProperty != null && currentProperty?.key?.startsWith("url_", ignoreCase = true) == true) {
                        hostServices!!.showDocument(currentProperty?.value)
                    }
                }
            }
        }

        tblProperties?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue ->
            if (currentProperty != newValue) saveCurrentProperty()

            currentProperty = newValue

            btnPropertyDelete?.isDisable = currentProperty == null
            btnPropertyMoveToFirst?.isDisable = currentProperty == null || currentProperty == listProperties.first()
            btnPropertyMoveUp?.isDisable = currentProperty == null || currentProperty == listProperties.first()
            btnPropertyMoveToLast?.isDisable = currentProperty == null || currentProperty == listProperties.last()
            btnPropertyMoveDown?.isDisable = currentProperty == null || currentProperty == listProperties.last()

            fldPropertyKey?.isDisable = currentProperty == null
            fldPropertyValue?.isDisable = currentProperty == null

            fldPropertyKey?.text = currentProperty?.key
            fldPropertyValue?.text = currentProperty?.value

        }

        fldPropertyKey?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProperty()
            }
        }
        fldPropertyValue?.focusedProperty()?.addListener { _, _, newPropertyValue ->
            if (!newPropertyValue) {
                saveCurrentProperty()
            }
        }

        colPropertyValue?.setCellFactory {
            val cell: TableCell<Property, String> = TableCell<Property, String>()
            val text = Text()
            text.style = ""
            cell.graphic = text
            cell.prefHeight = Control.USE_COMPUTED_SIZE
            text.textProperty().bind(cell.itemProperty())
            text.wrappingWidthProperty().bind(colPropertyValue!!.widthProperty())
            cell
        }
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

        if (currentPersonExt != null && fldName?.text != "") {
            var needToSave = false

            val tmp: String = fldName?.text ?: ""
            if (tmp != currentPersonExt?.person?.name) {
                currentPersonExt?.person?.name = tmp
                needToSave = true
            }

            if (needToSave) {
                PersonController.save(currentPersonExt?.person!!)
            }
        }


    }


    @FXML
    fun doOk(event: ActionEvent?) {
        saveCurrentPerson()
        mainStage?.close()
    }


    @FXML
    fun doPropertyAdd(event: ActionEvent?) {

        if (currentPersonExt !=null) {
            val menu = ContextMenu()

            var menuItem = MenuItem()

            menuItem.text = "Добавить новое свойство персонажа"
            menuItem.onAction = EventHandler { e: ActionEvent? ->
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Добавление свойства персонажа"
                alert.headerText = "Вы действительно хотите добавить новое свойство для персонажа?"
                alert.contentText = "Имя и значение свойства будут сгенерированы автоматически."
                val option = alert.showAndWait()
                if (option.get() == ButtonType.OK) {
                    saveCurrentProperty()
                    val id = PropertyController.editOrCreate(currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id).id
                    listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
                        currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
                    tblProperties?.items = listProperties
                    currentProperty = listProperties.first { it.id == id }
                    tblProperties?.selectionModel?.select(currentProperty)
                }
            }
            menu.items.add(menuItem)

            menu.items.add(SeparatorMenuItem())

            val listKeys = Main.propertyRepo.getKeys(currentPersonExt!!.person::class.java.simpleName).toMutableList()
            listKeys.sort()
            var countKeysAdded = 0
            listKeys.forEach { key ->

                if (listProperties.filter { it.key == key }.isEmpty()) {
                    countKeysAdded++
                    menuItem = MenuItem()
                    menuItem.isMnemonicParsing = false
                    menuItem.text = key
                    menuItem.onAction = EventHandler { e: ActionEvent? ->
                        saveCurrentProperty()
                        val id = PropertyController.editOrCreate(currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id, key).id
                        listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
                            currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
                        tblProperties?.items = listProperties
                        currentProperty = listProperties.filter { it.id == id }.first()
                        tblProperties?.selectionModel?.select(currentProperty)
                    }
                    menu.items.add(menuItem)
                }
            }

            if (countKeysAdded > 0) {
                menu.items.add(SeparatorMenuItem())
                menuItem = MenuItem()
                menuItem.text = "Добавить все свойства для персонажа"
                menuItem.onAction = EventHandler { e: ActionEvent? ->
                    saveCurrentProperty()
                    listKeys.forEach { key ->
                        if (listProperties.filter { it.key == key }.isEmpty()) {
                            PropertyController.editOrCreate(currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id, key)
                        }
                    }
                    listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
                        currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
                    tblProperties?.items = listProperties
                }
                menu.items.add(menuItem)
            }

            btnPropertyAdd?.contextMenu = menu
            val screenBounds: Bounds = btnPropertyAdd!!.localToScreen(btnPropertyAdd!!.boundsInLocal)
            menu.show(mainStage, screenBounds.minX +screenBounds.width, screenBounds.minY)

        }
    }

    @FXML
    fun doPropertyDelete(event: ActionEvent?) {
        if (currentProperty !=null) {
            val alert = Alert(Alert.AlertType.CONFIRMATION)
            alert.title = "Удаление свойства персонажа"
            alert.headerText = "Вы действительно хотите удалить свойство персонажа с ключом «${currentProperty?.key}» и значением «${currentProperty?.value}»?"
            alert.contentText = "В случае утвердительного ответа свойство персонажа будет удалено из базы данных и его восстановление будет невозможно.\nВы уверены, что хотите удалить свойство персонажа?"
            val option = alert.showAndWait()
            if (option.get() == ButtonType.OK) {
                PropertyController.delete(currentProperty!!)
                currentProperty = null
                listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
                    currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
                tblProperties?.items = listProperties

                btnPropertyMoveToFirst?.isDisable = currentProperty == null
                btnPropertyMoveUp?.isDisable = currentProperty == null
                btnPropertyMoveToLast?.isDisable = currentProperty == null
                btnPropertyMoveDown?.isDisable = currentProperty == null
                btnPropertyDelete?.isDisable = currentProperty == null
                fldPropertyKey?.isDisable = currentProperty == null
                fldPropertyValue?.isDisable = currentProperty == null

                fldPropertyKey?.text = ""
                fldPropertyValue?.text = ""

            }
        }
    }

    @FXML
    fun doPropertyMoveDown(event: ActionEvent?) {
        doMoveProperty(ReorderTypes.MOVE_DOWN)
    }

    @FXML
    fun doPropertyMoveToFirst(event: ActionEvent?) {
        doMoveProperty(ReorderTypes.MOVE_TO_FIRST)
    }

    @FXML
    fun doPropertyMoveUp(event: ActionEvent?) {
        doMoveProperty(ReorderTypes.MOVE_UP)
    }

    @FXML
    fun doPropertyMoveToLast(event: ActionEvent?) {
        doMoveProperty(ReorderTypes.MOVE_TO_LAST)
    }

    fun doMoveProperty(reorderType: ReorderTypes) {
        val id = currentProperty?.id
        currentProperty?.let { PropertyController.reOrder(reorderType, it) }
        listProperties = FXCollections.observableArrayList(PropertyController.getListProperties(
            currentPersonExt!!.person::class.java.simpleName, currentPersonExt!!.person.id))
        tblProperties?.items = listProperties
        currentProperty = listProperties.first { it.id == id }
        tblProperties?.selectionModel?.select(currentProperty)
    }

    @FXML
    fun doPersonDelete(event: ActionEvent?) {
    }

    @FXML
    fun doPersonAdd(event: ActionEvent?) {

    }
}
