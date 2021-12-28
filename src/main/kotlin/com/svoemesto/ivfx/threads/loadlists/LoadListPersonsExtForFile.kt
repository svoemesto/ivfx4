package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.PersonExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListPersonsExtForFile(
    private var list: ObservableList<PersonExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar? = null,
    private var lbl: Label? = null
    ) : Thread(), Runnable {

    override fun run() {
        loadList()
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

        val sourceIterable = Main.personRepo.findByFileId(fileExt.file.id)
        list.clear()

        for ((i, person) in sourceIterable.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, person ($i/${sourceIterable.count()})"
            }
            person.project = fileExt.projectExt.project

            val personExt = PersonExt(person, fileExt.projectExt)
            list.add(personExt)
            println(personExt)
        }

        list.sort()

        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}