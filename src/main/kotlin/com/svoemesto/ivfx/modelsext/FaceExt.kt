package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Face
import java.io.File

class FaceExt {

    constructor(face: Face, fileExt: FileExt) {
        this.fileExt = fileExt
//            this.fileExt = FileExt(face.file)
        this.fileId = face.file.id
        this.frameNumber = face.frameNumber
        this.faceNumberInFrame = face.faceNumberInFrame
        this.pathToFrameFile = "${fileExt.folderFramesFull}${File.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}.jpg"
        this.pathToFaceFile = "${fileExt.folderFramesFull}.faces${File.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
        this.personId = face.personId
        this.personRecognizedName = face.personRecognizedName
        this.personRecognizedId = face.personRecognizedId
        this.recognizeProbability = face.recognizeProbability
        this.startX = face.startX
        this.startY = face.startY
        this.endX = face.endX
        this.endY = face.endY
        this.vectorText = face.vectorText

        val textVector: Array<String> = face.vectorText.split("\\|".toRegex()).toTypedArray()
        val result = DoubleArray(textVector.size)
        for (i in textVector.indices) {
            result[i] = textVector[i].toDouble()
        }
        this.vector = result

    }

    constructor()

    var fileId: Long = 0
    var frameNumber: Int = 0
    var faceNumberInFrame: Int = 0
    var pathToFrameFile: String = ""
    var pathToFaceFile: String = ""
    var personId: Long = 0
    var personRecognizedName: String = ""
    var personRecognizedId: Long = 0
    var recognizeProbability: Double = 0.0
    var startX: Int = 0
    var startY: Int = 0
    var endX: Int = 0
    var endY: Int = 0
    @Transient
    var vectorText:String = "0.0"
    var vector: DoubleArray = doubleArrayOf(0.0)
    @Transient
    var fileExt: FileExt? = null

}