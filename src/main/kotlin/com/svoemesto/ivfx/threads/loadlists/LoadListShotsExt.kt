package com.svoemesto.ivfx.threads.loadlists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ShotExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class LoadListShotsExt(
    private var list: ObservableList<ShotExt>,
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
                lbl!!.text = java.lang.String.format("Loading shots: ${fileExt.file.name}")
                lbl!!.isVisible = true
            }
        }

        val sourceIterable = Main.shotRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileExt.file.id, 0)
        list.clear()

        for ((i, shot) in sourceIterable.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/sourceIterable.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/sourceIterable.count().toDouble())} Loading: ${fileExt.file.name}, shot ($i/${sourceIterable.count()})"
            }

            shot.file = fileExt.file

            val shotExt = ShotExt(shot, fileExt,
                fileExt.framesExt.first { it.frame.frameNumber == shot.firstFrameNumber },
                fileExt.framesExt.first { it.frame.frameNumber == shot.lastFrameNumber })
            shotExt.previewsFirst
            shotExt.previewsLast
            list.add(shotExt)
        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}