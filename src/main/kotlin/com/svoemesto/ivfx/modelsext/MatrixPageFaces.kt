package com.svoemesto.ivfx.modelsext

import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MatrixPageFaces(
    val pageNumber: Int,
    val countColumns: Int,
    val countRows: Int,
    val matrixFaces: MutableList<MatrixFace>
) {

    companion object {

        fun createPages(listFacesExt: List<FaceExt>, paneW: Double, paneH: Double, picW: Double, picH: Double): ObservableList<MatrixPageFaces> {

            val countColumnsInPage = ((paneW - ((picW + 2) * 2 + 20)) / (picW + 2)).toInt()
            val countRowsInPage = ((paneH - ((picH + 2) * 2 + 20)) / (picH + 2)).toInt()
            val listMatrixPageFaces: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
            var matrixPageFaces = MatrixPageFaces(listMatrixPageFaces.size+1, countColumnsInPage, countRowsInPage, mutableListOf())
            var currentColumn = 1
            var currentRow = 1
            var wasAddedNewPage = false
            for ((i, currFaceExt) in listFacesExt.withIndex()) {
                val prevFaceExt = if (i > 0) listFacesExt[i-1] else null
                if (wasAddedNewPage) {
                    wasAddedNewPage = false
                    listMatrixPageFaces.last().matrixFaces.add(MatrixFace(currFaceExt, listMatrixPageFaces.last(), listMatrixPageFaces.last().matrixFaces.last().column + 1, listMatrixPageFaces.last().matrixFaces.last().row))
                    matrixPageFaces.matrixFaces.add(MatrixFace(prevFaceExt, matrixPageFaces, 0, 1))
                    currentColumn = 1
                    currentRow = 1
                }
                matrixPageFaces.matrixFaces.add(MatrixFace(currFaceExt, matrixPageFaces, currentColumn, currentRow))
                if (currentColumn < countColumnsInPage || (currentColumn == countColumnsInPage && currentRow == countRowsInPage)) {
                    currentColumn++
                } else if (currentColumn == countColumnsInPage && currentRow < countRowsInPage) {
                    currentColumn = 1
                    currentRow++
                }
                if (i == listFacesExt.size - 1 || currentColumn == countColumnsInPage+1 || currentRow == countRowsInPage+1){
                    listMatrixPageFaces.add(matrixPageFaces)
                    matrixPageFaces = MatrixPageFaces(listMatrixPageFaces.size+1, countColumnsInPage, countRowsInPage, mutableListOf())
                    wasAddedNewPage = true
                }
            }
            return listMatrixPageFaces
        }
    }
}