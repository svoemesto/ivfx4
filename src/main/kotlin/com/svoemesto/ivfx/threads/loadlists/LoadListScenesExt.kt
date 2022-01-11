package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.controllers.SceneController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.SceneExt
import com.svoemesto.ivfx.modelsext.ShotExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListScenesExt(
    private var list: ObservableList<SceneExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListScenesExt"
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
                lbl!!.text = java.lang.String.format("Loading scenes: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

//        val sourceIterable = Main.shotRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileExt.file.id, 0)
        val sourceIterable = SceneController.getSetScenes(fileExt.file).toMutableList()
        sourceIterable.sort()
        list.clear()

        for ((i, shot) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater {
                    if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                    if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, scene ($i/${sourceIterable.count()})"
                }

                val sceneExt = SceneExt(shot, fileExt,
                    fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber },
                    fileExt.framesExt.first { it.frame.frameNumber == shot.lastFrameNumber })
                sceneExt.previewsFirst
                sceneExt.previewsLast

                list.add(sceneExt)
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