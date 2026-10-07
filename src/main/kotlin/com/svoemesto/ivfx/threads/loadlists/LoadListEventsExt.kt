package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.controllers.EventController
import com.svoemesto.ivfx.modelsext.EventExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.SceneExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListEventsExt(
    private var list: ObservableList<EventExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
    private val flagIsDone: SimpleBooleanProperty = SimpleBooleanProperty(false)
    ) : Thread(), Runnable {

    override fun run() {
        this.name = "LoadListEventsExt"
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
        val sourceCount = sourceIterable.count()
        // Список для интерфейса меняется только из потока интерфейса: раньше
        // clear() и add() выполнялись здесь, из фонового потока, и перебор
        // списка в это время падал с ConcurrentModificationException.
        val loaded: MutableList<EventExt> = mutableListOf()

        for ((i, shot) in sourceIterable.withIndex()) {
            if (currentThread().isInterrupted) {
                break
            }
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceCount
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceCount.toDouble())} Loading: ${fileExt.file.name}, event ($i/$sourceCount)"
            }

            val eventExt = EventExt(shot, fileExt,
                fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber },
                fileExt.framesExt.first { it.frame.frameNumber == shot.lastFrameNumber })
            eventExt.previewsFirst
            eventExt.previewsLast

            loaded.add(eventExt)
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