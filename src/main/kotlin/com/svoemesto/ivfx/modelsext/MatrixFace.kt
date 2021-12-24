package com.svoemesto.ivfx.modelsext

class MatrixFace: Comparable<MatrixFace> {
    var faceExt: FaceExt? = null
    var matrixPageFaces: MatrixPageFaces? = null
    var column: Int = 0
    var row: Int = 0
    override fun compareTo(other: MatrixFace): Int {
        return other.faceExt?.let { faceExt?.compareTo(it) ?: 0 }?: 0
    }
}