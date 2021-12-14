package com.svoemesto.ivfx.modelsext

import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MatrixPageFaces {
    var pageNumber: Int? = null
    var countColumns: Int = 0
    var countRows: Int = 0
    var matrixFaces: MutableList<MatrixFace> = mutableListOf()

    companion object {

        fun createPages(listFacesExt: List<FaceExt>, paneW: Double, paneH: Double, picW: Double, picH: Double): ObservableList<MatrixPageFaces> {

            val countColumnsInPage = ((paneW - ((picW + 2) * 2 + 20)) / (picW + 2)).toInt()
            val countRowsInPage = ((paneH - ((picH + 2) * 2 + 20)) / (picH + 2)).toInt()
            val listMatrixPageFaces: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
            var matrixPageFaces = MatrixPageFaces()
            var currentColumn = 1
            var currentRow = 1
            var wasAddedNewPage = false
            var alfaMatrixFace: MatrixFace?
            var omegaMatrixFace: MatrixFace?
            for ((i, currFaceExt) in listFacesExt.withIndex()) {
                val prevFaceExt = if (i > 0) listFacesExt[i-1] else null
                val nextFaceExt = if (i < (listFacesExt.size - 1)) listFacesExt[i+1] else null
                if (wasAddedNewPage) {
                    wasAddedNewPage = false

                    omegaMatrixFace = MatrixFace()
                    omegaMatrixFace.faceExt = currFaceExt
                    currentColumn = listMatrixPageFaces.last().matrixFaces.last().column + 1
                    currentRow = listMatrixPageFaces.last().matrixFaces.last().row
                    omegaMatrixFace.column = currentColumn
                    omegaMatrixFace.row = currentRow
                    omegaMatrixFace.matrixPageFaces = listMatrixPageFaces.last()
                    listMatrixPageFaces.last().matrixFaces.add(omegaMatrixFace)

                    alfaMatrixFace = MatrixFace()
                    alfaMatrixFace.faceExt = prevFaceExt
                    currentColumn = 0
                    currentRow = 1
                    alfaMatrixFace.column = currentColumn
                    alfaMatrixFace.row = currentRow
                    alfaMatrixFace.matrixPageFaces = matrixPageFaces
                    matrixPageFaces.matrixFaces.add(alfaMatrixFace)

                    currentColumn = 1
                    currentRow = 1
                }
                val matrixFace = MatrixFace()
                matrixFace.faceExt = currFaceExt
                matrixFace.column = currentColumn
                matrixFace.row = currentRow
                matrixFace.matrixPageFaces = matrixPageFaces
                matrixPageFaces.matrixFaces.add(matrixFace)

                if (currentColumn < countColumnsInPage || (currentColumn == countColumnsInPage && currentRow == countRowsInPage)) {
                    currentColumn++
                } else if (currentColumn == countColumnsInPage && currentRow < countRowsInPage) {
                    currentColumn = 1
                    currentRow++
                }

                if (i == listFacesExt.size - 1 || currentColumn == countColumnsInPage+1 || currentRow == countRowsInPage+1){
                    matrixPageFaces.pageNumber = listMatrixPageFaces.size + 1
                    matrixPageFaces.countColumns = countColumnsInPage
                    matrixPageFaces.countRows = countRowsInPage
                    listMatrixPageFaces.add(matrixPageFaces)
                    matrixPageFaces = MatrixPageFaces()
                    wasAddedNewPage = true
                }

            }

            return listMatrixPageFaces
        }

    }
}