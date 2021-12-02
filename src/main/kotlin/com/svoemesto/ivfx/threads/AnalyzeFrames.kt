package com.svoemesto.ivfx.threads

import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.controllers.FrameController
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView

class AnalyzeFrames(var fileExt: FileExt,
                    val fileController: FileController,
                    val frameController: FrameController,
                    val table: TableView<FileExt>,
                    val textLbl1: String,
                    val numCurrentThread: Int,
                    val countThreads: Int,
                    var lbl1: Label, var pb1: ProgressBar,
                    var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        lbl1.isVisible = true
        lbl2.isVisible = true
        pb1.isVisible = true
        pb2.isVisible = true



        fileExt.hasLossless = true
        fileExt.hasLosslessString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}