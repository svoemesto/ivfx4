package com.svoemesto.ivfx.threads.updatelists

import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class UpdateListFramesExt(
    private var list: ObservableList<FrameExt>,
    private var fileExt: FileExt,
    private var pb: ProgressBar?,
    private var lbl: Label?
    ) : Thread(), Runnable {

    override fun run() {
        updateList()
    }

    private fun updateList() {
        Platform.runLater {
            if (pb != null) pb!!.isVisible = true
            if (lbl != null) lbl!!.isVisible = true
        }

        for ((i, frameExt) in list.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/list.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/list.count().toDouble())}, updating frame ($i/${list.count()}) ${fileExt.file.name}"
            }

            try {
                frameExt.labelSmall
            } catch (e: IllegalStateException) {
                break
            }

        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}