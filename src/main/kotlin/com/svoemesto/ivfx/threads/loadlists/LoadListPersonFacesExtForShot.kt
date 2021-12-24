package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListPersonFacesExtForShot(
    private var list: ObservableList<FaceExt>,
    private var shotExt: ShotExt,
    private var personExt: PersonExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private var loadNotExample: Boolean = true,
    private var loadExample: Boolean = true,
    private var loadNotManual: Boolean = true,
    private var loadManual: Boolean = true
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
                lbl!!.text = java.lang.String.format("Loading faces: ${personExt.person.name}")
                lbl!!.isVisible = true
            }
        }

        val sourceIterable = FaceController.getListFacesExt(
            shotExt,
            personExt,
            loadNotExample,
            loadExample,
            loadNotManual,
            loadManual
        )

        list.clear()

        for ((i, faceExt) in sourceIterable.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${personExt.person.name}, face ($i/${sourceIterable.count()})"
            }

            list.add(faceExt)
        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}