package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListFramesExt(
    private var list: ObservableList<FrameExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?,
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
                lbl!!.text = java.lang.String.format("Loading frames: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

        val sourceIterable = Main.frameRepo.findByFileIdAndFrameNumberGreaterThanOrderByFrameNumber(fileExt.file.id, 0)
        list.clear()

        for ((i, frame) in sourceIterable.withIndex()) {
            if (!currentThread().isInterrupted) {
                Platform.runLater {
                    if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                    if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, frame ($i/${sourceIterable.count()})"
                }

                frame.file = fileExt.file

                val frameExt = FrameExt(frame, fileExt)
                list.add(frameExt)
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