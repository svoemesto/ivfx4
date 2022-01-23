package com.svoemesto.ivfx.modelsext

class MatrixFace(
    val faceExt: FaceExt?,
    val matrixPageFaces: MatrixPageFaces,
    val column: Int,
    val row: Int
): Comparable<MatrixFace> {

    override fun compareTo(other: MatrixFace): Int {
        return other.faceExt?.let { faceExt?.compareTo(it) ?: 0 }?: 0
    }
}