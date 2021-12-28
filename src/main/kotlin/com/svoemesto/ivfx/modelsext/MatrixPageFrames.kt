package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MatrixPageFrames {
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

        fun createPages(listFramesExt: List<FrameExt>, paneW: Double, paneH: Double, picW: Double, picH: Double): ObservableList<MatrixPageFrames> {

            val countColumnsInPage = ((paneW - ((picW + 2) * 2 + 20)) / (picW + 2)).toInt()
            val countRowsInPage = ((paneH - ((picH + 2) * 2 + 20)) / (picH + 2)).toInt()
            val listMatrixPageFrames: ObservableList<MatrixPageFrames> = FXCollections.observableArrayList()
            var matrixPageFrames = MatrixPageFrames()
            var currentColumn = 1
            var currentRow = 1
            var wasAddedNewPage = false
            var alfaMatrixFrame: MatrixFrame?
            var omegaMatrixFrame: MatrixFrame?
            for ((i, currFrameExt) in listFramesExt.withIndex()) {
                val prevFrameExt = if (i > 0) listFramesExt[i-1] else null
                val nextFrameExt = if (i < (listFramesExt.size - 1)) listFramesExt[i+1] else null
                if (wasAddedNewPage) {
                    wasAddedNewPage = false

                    omegaMatrixFrame = MatrixFrame()
                    omegaMatrixFrame.frameExt = currFrameExt
                    if (currFrameExt.frame.isFinalFind) {
                        currentColumn = listMatrixPageFrames.last().matrixFrames.last().column
                        currentRow = listMatrixPageFrames.last().matrixFrames.last().row + 1
                    } else {
                        currentColumn = listMatrixPageFrames.last().matrixFrames.last().column + 1
                        currentRow = listMatrixPageFrames.last().matrixFrames.last().row
                    }
                    omegaMatrixFrame.column = currentColumn
                    omegaMatrixFrame.row = currentRow
                    omegaMatrixFrame.matrixPageFrames = listMatrixPageFrames.last()
                    listMatrixPageFrames.last().matrixFrames.add(omegaMatrixFrame)

                    alfaMatrixFrame = MatrixFrame()
                    alfaMatrixFrame.frameExt = prevFrameExt
                    if (currFrameExt.frame.isFinalFind == true) {
                        currentColumn = 1
                        currentRow = 0
                    } else {
                        currentColumn = 0
                        currentRow = 1
                    }
                    alfaMatrixFrame.column = currentColumn
                    alfaMatrixFrame.row = currentRow
                    alfaMatrixFrame.matrixPageFrames = matrixPageFrames
                    matrixPageFrames.matrixFrames.add(alfaMatrixFrame)

                    currentColumn = 1
                    currentRow = 1
                }
                val matrixFrame = MatrixFrame()
                matrixFrame.frameExt = currFrameExt
                matrixFrame.column = currentColumn
                matrixFrame.row = currentRow
                matrixFrame.matrixPageFrames = matrixPageFrames
                matrixPageFrames.matrixFrames.add(matrixFrame)

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
                    matrixPageFrames.pageNumber = listMatrixPageFrames.size + 1
                    matrixPageFrames.countColumns = countColumnsInPage
                    matrixPageFrames.countRows = countRowsInPage
                    listMatrixPageFrames.add(matrixPageFrames)
                    matrixPageFrames = MatrixPageFrames()
                    wasAddedNewPage = true
                }

            }

            return listMatrixPageFrames
        }

    }
}