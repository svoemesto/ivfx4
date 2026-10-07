package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListFileFacesExt(
    private var list: ObservableList<FaceExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListFileFacesExt"
        loadList()
    }

    private fun loadList() {

        Platform.runLater {
            if (pb != null) {
                pb!!.progress = -1.0
                pb!!.isVisible = true
            }
            if (lbl != null) {
                lbl!!.text = java.lang.String.format("Loading faces: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

        val sourceIterable = FaceController.getListFacesExt(fileExt)
        val sourceCount = sourceIterable.count()
        // Список для интерфейса меняется только из потока интерфейса: раньше
        // clear() и add() выполнялись здесь, из фонового потока, и перебор
        // списка в это время падал с ConcurrentModificationException.
        val loaded: MutableList<FaceExt> = mutableListOf()

        for ((i, faceExt) in sourceIterable.withIndex()) {
            if (currentThread().isInterrupted) {
                break
            }
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceCount
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceCount.toDouble())} Loading: ${fileExt.file.name}, face ($i/$sourceCount)"
            }

            loaded.add(faceExt)
        }
        Platform.runLater {
            list.clear()
            list.addAll(loaded)
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
            flagIsDone.set(true)
        }
    }
}