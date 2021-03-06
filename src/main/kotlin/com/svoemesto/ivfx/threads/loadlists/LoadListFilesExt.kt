package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListFilesExt(
    private var list: ObservableList<FileExt>,
    private var projectExt: ProjectExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListFilesExt"
        loadList()
        flagIsDone.set(true)
    }

    private fun loadList() {
        val sourceIterable = Main.fileRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(projectExt.project.id, 0)
        list.clear()
        Platform.runLater {
            if (pb != null) pb!!.isVisible = true
            if (lbl != null) lbl!!.isVisible = true
        }

        for ((i, file) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater {
                    if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                    if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${file.name} ($i/${sourceIterable.count()})"
                }

                file.project = projectExt.project
                val cdf = FileCdfController.getFileCdf(file)
                file.cdfs = mutableSetOf()
                file.cdfs.add(cdf)
                file.tracks = TrackController.getSetTracks(file)

                val fileExt = FileExt(file, projectExt)
                list.add(fileExt)
            } else {
                return
            }

        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}