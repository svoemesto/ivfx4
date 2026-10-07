package com.svoemesto.ivfx.modelsext

import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MatrixPageFaces(
    val pageNumber: Int,
    val countColumns: Int,
    val countRows: Int,
    val matrixFaces: MutableList<MatrixFace>,
    /**
     * Номера строк, перед которыми рисуется горизонтальная черта.
     *
     * Нужна вкладке Tracks: там лица одной персоны идут по трекам, и между
     * треками черта отделяет один трек от другого. В Persons этого нет, поле
     * пустое и ничего не меняет.
     */
    val separatorRows: MutableSet<Int> = mutableSetOf()
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
        /**
         * Раскладка лиц по трекам, а не сплошной сеткой.
         *
         * Каждый трек начинается с новой строки, а между треками оставляется
         * пустая строка под горизонтальную черту. Иначе лица разных треков
         * одной и той же персоны слипаются в общую кашу, и не видно, где
         * кончается один трек и начинается другой.
         *
         * Трек целиком переносится на следующую страницу, если он туда не
         * помещается: разорванный пополам трек читать невозможно. Длиннее
         * страницы трек всё же делится — иначе он не поместился бы никуда.
         */
        fun createTrackPages(tracksFaces: List<List<FaceExt>>,
                             paneW: Double, paneH: Double,
                             picW: Double, picH: Double): ObservableList<MatrixPageFaces> {
            val columns = ((paneW - ((picW + 2) * 2 + 20)) / (picW + 2)).toInt().coerceAtLeast(1)
            val rowsPerPage = ((paneH - ((picH + 2) * 2 + 20)) / (picH + 2)).toInt().coerceAtLeast(1)
            val pages: ObservableList<MatrixPageFaces> = FXCollections.observableArrayList()
            var page = MatrixPageFaces(1, columns, rowsPerPage, mutableListOf())
            var currentRow = 1

            fun closePage() {
                pages.add(page)
                page = MatrixPageFaces(pages.size + 1, columns, rowsPerPage, mutableListOf())
                currentRow = 1
            }

            var lastRowUsed = 0
            for (faces in tracksFaces) {
                if (faces.isEmpty()) continue
                // Каждый трек начинается с новой строки: короткий трек не должен
                // дописывать лица в строку предыдущего. Поэтому курсор всегда
                // встаёт на строку ПОСЛЕ последней занятой, а не на неё саму.
                // Иначе трек из одного лица оставлял бы курсор на своей строке,
                // и лица разных треков оказывались бы в одной строке.
                currentRow = maxOf(currentRow, lastRowUsed + 1)
                val neededRows = (faces.size + columns - 1) / columns
                if (currentRow + neededRows - 1 > rowsPerPage && currentRow > 1) closePage()
                // Черта рисуется над первой строкой трека, а не на отдельной
                // пустой строке: иначе между чертой и лицами остаётся провал
                // во всю высоту карточки. Номер строки отдаём отрисовке.
                if (currentRow > 1 || pages.isNotEmpty()) page.separatorRows.add(currentRow)
                var column = 1
                for (faceExt in faces) {
                    if (column > columns) {
                        column = 1
                        currentRow++
                        if (currentRow > rowsPerPage) closePage()
                    }
                    page.matrixFaces.add(MatrixFace(faceExt, page, column, currentRow))
                    lastRowUsed = currentRow
                    column++
                }
            }
            if (page.matrixFaces.isNotEmpty()) pages.add(page)
            return pages
        }
    }
}