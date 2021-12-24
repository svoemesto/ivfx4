package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.enums.PersonType

class FaceExtJson() {

    var projectId: Long = 0
    var frameId: Long = 0
    var fileId: Long = 0
    var personId: Long = 0
    var faceId: Long = 0
    var personType: String = PersonType.UNDEFINDED.name
    var frameNumber: Int = 0
    var faceNumberInFrame: Int = 0
    var pathToFrameFile: String = ""
    var pathToFaceFile: String = ""
    var personRecognizedName: String = ""
    var recognizeProbability: Double = 0.0
    var startX: Int = 0
    var startY: Int = 0
    var endX: Int = 0
    var endY: Int = 0
    var vector: DoubleArray = doubleArrayOf(0.0)

}