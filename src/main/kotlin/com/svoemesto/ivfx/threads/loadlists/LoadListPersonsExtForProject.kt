package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListPersonsExtForProject(
    private var list: ObservableList<PersonExt>,
    private var projectExt: ProjectExt,
    private var pb: ProgressBar? = null,
    private var lbl: Label? = null,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
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
                lbl!!.text = java.lang.String.format("Loading persons: ${projectExt.project.name}")
                lbl!!.isVisible = true
            }
        }

        val sourceIterable = projectExt.project.persons
        list.clear()

        for ((i, person) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater {
                    if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                    if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${projectExt.project.name}, person ($i/${sourceIterable.count()})"
                }
                person.project = projectExt.project
                val personExt = PersonExt(person, projectExt)
                list.add(personExt)
                println(personExt)
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