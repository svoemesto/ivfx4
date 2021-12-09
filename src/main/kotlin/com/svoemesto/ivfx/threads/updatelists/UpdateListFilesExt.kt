package com.svoemesto.ivfx.threads.updatelists

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar

class UpdateListFilesExt(
    private var list: ObservableList<FileExt>,
    private var projectExt: ProjectExt,
    private var pb: ProgressBar?,
    private var lbl: Label?
    ) : Thread(), Runnable {

    override fun run() {
        loadList()
    }

    private fun loadList() {

        Platform.runLater {
            if (pb != null) pb!!.isVisible = true
            if (lbl != null) lbl!!.isVisible = true
        }

        for ((i, fileExt) in list.withIndex()) {
            Platform.runLater {
                if (pb!=null) pb!!.progress = i.toDouble()/list.count()
                if (lbl!=null) lbl!!.text = "${java.lang.String.format("[%.0f%%]", 100*i/list.count().toDouble())} Updating: ${fileExt.file.name} ($i/${list.count()})"
            }
            val fileExt = list[i]

            fileExt.fps
            fileExt.framesCount
            fileExt.folderPreview
            fileExt.folderLossless
            fileExt.folderFavorites
            fileExt.folderShots
            fileExt.folderFramesSmall
            fileExt.folderFramesMedium
            fileExt.folderFramesFull
            fileExt.pathToLosslessFile
            fileExt.pathToPreviewFile
            fileExt.hasPreview
            fileExt.hasLossless
            fileExt.hasFramesSmall
            fileExt.hasFramesMedium
            fileExt.hasFramesFull
            fileExt.hasAnalyzedFrames
            fileExt.hasDetectedFaces
            fileExt.hasCreatedFaces

        }
        Platform.runLater {
            if (pb!=null) pb!!.isVisible = false
            if (lbl!=null) lbl!!.isVisible = false
        }
    }
}