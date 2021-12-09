package com.svoemesto.ivfx.modelsext

class MatrixFrame {
    var frameExt: FrameExt? = null
    val frameNumber: Int? get() = frameExt?.frame?.frameNumber
    var matrixPage: MatrixPage? = null
    var column: Int = 0
    var row: Int = 0
}