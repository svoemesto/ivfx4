package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.PersonExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListPersonsExtForFile(
    private var list: ObservableList<PersonExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar? = null,
    private var lbl: Label? = null,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false),
    private val withoutNonPerson: Boolean = true
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListPersonsExtForFile"
        loadList()
        flagIsDone.set(true)
    }

    private fun loadList() {

        Platform.runLater {
            if (pb != null) {
                pb!!.progress = -1.0
                pb!!.isVisible = true
            }
            if (lbl != null) {
                lbl!!.text = java.lang.String.format("Loading persons: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

        // Персоны — по проекту, а не по файлу. Серии одного сериала показывают
        // одних и тех же людей, и персонаж, отмеченный в первой серии, должен
        // быть в списке и во второй: иначе назначить его нельзя даже вручную,
        // не только автоматически.
        val sourceIterable = Main.personRepo.findByProjectId(fileExt.projectExt.project.id)
        list.clear()

        for ((i, person) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater {
                    if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                    if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, person ($i/${sourceIterable.count()})"
                }
                if (!(withoutNonPerson && person.personType == PersonType.NONPERSON)) {
                    person.project = fileExt.projectExt.project
                    val personExt = PersonExt(person, fileExt.projectExt)
                    list.add(personExt)
                }
            } else {
                return
            }

        }

        list.sort()

        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}