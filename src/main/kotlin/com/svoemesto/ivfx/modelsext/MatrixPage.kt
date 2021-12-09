package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MatrixPage {
    var pageNumber: Int? = null
    var countColumns: Int = 0
    var countRows: Int = 0
    var matrixFrames: MutableList<MatrixFrame> = mutableListOf()
    val firstFrameNumber: Int? get() = matrixFrames.firstOrNull()?.frameNumber
    val lastFrameNumber: Int? get() = matrixFrames.lastOrNull()?.frameNumber
    val start: String
        get() {
            val frameExt = matrixFrames.firstOrNull()?.frameExt
            return if (frameExt != null) convertDurationToString(getDurationByFrameNumber(frameExt.frame.frameNumber, frameExt.fileExt.fps)) else ""
        }
    val end: String
        get() {
            val frameExt = matrixFrames.lastOrNull()?.frameExt
            return if (frameExt != null) convertDurationToString(getDurationByFrameNumber(frameExt.frame.frameNumber, frameExt.fileExt.fps)) else ""
        }

    companion object {

        fun createPages(listFramesExt: List<FrameExt>, paneW: Double, paneH: Double, picW: Double, picH: Double): ObservableList<MatrixPage> {

            val countColumnsInPage = ((paneW - ((picW + 2) * 2 + 20)) / (picW + 2)).toInt()
            val countRowsInPage = ((paneH - ((picH + 2) * 2 + 20)) / (picH + 2)).toInt()
            val listMatrixPages: ObservableList<MatrixPage> = FXCollections.observableArrayList()
            val matrixPage = MatrixPage()
            var currentColumn = 1
            var currentRow = 1
            var wasAddedNewPage = false
            var zeroMatrixFrame: MatrixFrame?
            for ((i, currFrameExt) in listFramesExt.withIndex()) {
                val prevFrameExt = if (i > 0) listFramesExt[i-1] else null
                val nextFrameExt = if (i < (listFramesExt.size - 1)) listFramesExt[i+1] else null
                if (wasAddedNewPage) {
                    wasAddedNewPage = false
                    zeroMatrixFrame = MatrixFrame()
                    zeroMatrixFrame.frameExt = prevFrameExt
                    if (zeroMatrixFrame.frameExt?.frame?.isFinalFind == true) {
                        currentColumn = 1
                        currentRow = 0
                    } else {
                        currentColumn = 0
                        currentRow = 1
                    }
                    zeroMatrixFrame.column = currentColumn
                    zeroMatrixFrame.row = currentRow
                    zeroMatrixFrame.matrixPage = matrixPage
                    matrixPage.matrixFrames.add(zeroMatrixFrame)
                    currentColumn = 1
                    currentRow = 1
                }
                val matrixFrame = MatrixFrame()
                matrixFrame.frameExt = currFrameExt
                matrixFrame.column = currentColumn
                matrixFrame.row = currentRow
                matrixFrame.matrixPage = matrixPage
                matrixPage.matrixFrames.add(matrixFrame)

                if (nextFrameExt != null && nextFrameExt.frame.isFinalFind) {
                    currentRow++
                } else {
                    if (currentColumn < countColumnsInPage || (currentColumn == countColumnsInPage && currentRow == countRowsInPage)) {
                        currentColumn++
                    } else if (currentColumn == countColumnsInPage && currentRow < countRowsInPage) {
                        currentColumn = 1
                        currentRow++
                    }
                }

                if (i == listFramesExt.size - 1 || currentColumn == countColumnsInPage+1 || currentRow == countRowsInPage+1){
                    matrixPage.pageNumber = listMatrixPages.size + 1
                    matrixPage.countColumns = countColumnsInPage
                    matrixPage.countRows = countRowsInPage
                    listMatrixPages.add(matrixPage)
                    wasAddedNewPage = true
                }

            }

            return listMatrixPages
        }

    }
}