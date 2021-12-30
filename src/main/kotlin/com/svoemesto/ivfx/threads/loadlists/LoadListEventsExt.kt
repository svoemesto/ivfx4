package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.controllers.EventController
import com.svoemesto.ivfx.modelsext.EventExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.SceneExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListEventsExt(
    private var list: ObservableList<EventExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?
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
                lbl!!.text = java.lang.String.format("Loading events: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

//        val sourceIterable = Main.shotRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileExt.file.id, 0)
        val sourceIterable = EventController.getSetEvents(fileExt.file).toMutableList()
        sourceIterable.sort()
        list.clear()

        for ((i, shot) in sourceIterable.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, event ($i/${sourceIterable.count()})"
            }

            val eventExt = EventExt(shot, fileExt,
                fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber },
                fileExt.framesExt.first { it.frame.frameNumber == shot.lastFrameNumber })
            eventExt.previewsFirst
            eventExt.previewsLast

            list.add(eventExt)
        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}