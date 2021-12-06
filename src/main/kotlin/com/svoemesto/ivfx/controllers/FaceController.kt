package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Shot
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
class FaceController {

    class FaceExt{
        constructor(face: Face) {
            this.fileId = face.file.id
            this.frameNumber = face.frameNumber
            this.faceNumberInFrame = face.faceNumberInFrame
            this.pathToFrameFile = "${face.file.folderFramesFull}${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}.jpg"
            this.pathToFaceFile = "${face.file.folderFramesFull}.faces${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
            this.tagId = face.tagId
            this.tagRecognizedId = face.tagRecognizedId
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
        var tagId: Long = 0
        var tagRecognizedId: Long = 0
        var recognizeProbability: Double = 0.0
        var startX: Int = 0
        var startY: Int = 0
        var endX: Int = 0
        var endY: Int = 0
        var vectorText:String = "0.0"
        var vector: DoubleArray = doubleArrayOf(0.0)
    }

    fun createOrUpdate(faceExt: FaceExt, file: File): Face {

        var face = Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(faceExt.fileId, faceExt.frameNumber, faceExt.faceNumberInFrame).firstOrNull()
        if (face != null) {
            face.file = file
            var faceExtTmp = FaceExt(face)
            if (faceExtTmp == faceExt) {
                return face
            }
        } else {
            face = Face()
        }
        face.file = file
        face.frameNumber = faceExt.frameNumber
        face.faceNumberInFrame = faceExt.faceNumberInFrame
        face.tagId = faceExt.tagId
        face.tagRecognizedId = faceExt.tagRecognizedId
        face.recognizeProbability = faceExt.recognizeProbability
        face.startX = faceExt.startX
        face.startY = faceExt.startY
        face.endX = faceExt.endX
        face.endY = faceExt.endY
        face.vectorText = faceExt.vectorText

        save(face)
        return face

    }

    fun save(face: Face) {
        Main.faceRepo.save(face)
    }

    fun getListFaces(file: File): MutableList<Face> {
        val result = Main.faceRepo.findByFileId(file.id).toMutableList()
        result.forEach { it.file = file }
        return result
    }

    fun getArrayFacesExt(file: File): Array<FaceExt> {
        val listFrameNumbers: List<Int> = getFramesToRecognize(file)
        val listFacesExt: MutableList<FaceExt> = mutableListOf() //<Frame>(listFrameNumbers.size)
        for (i in listFrameNumbers.indices) {
            val faceExt = FaceExt()
            faceExt.fileId = file.id
            faceExt.frameNumber = listFrameNumbers[i]
            faceExt.pathToFrameFile = "${file.folderFramesFull}${IOFile.separator}${file.shortName}_frame_${String.format("%06d", listFrameNumbers[i])}.jpg"
            listFacesExt.add(faceExt)
        }
        return listFacesExt.toTypedArray()
    }

    fun getFramesToRecognize(file: File): List<Int> {
        var curr = 0
        val listFrames: MutableList<Int> = mutableListOf()
        val listShots: List<Shot> = file.shots
        val countFrames = Main.fileController.getFramesCount(file)
        for (shot in listShots) {
            val stepFrames = if (shot.lastFrameNumber - shot.firstFrameNumber < 60) 10 else 30
            var i: Int = shot.firstFrameNumber
            while (i < shot.lastFrameNumber) {
                curr = i
                if (curr <= countFrames) listFrames.add(curr)
                i += stepFrames
            }
            if (curr < shot.lastFrameNumber) curr = shot.lastFrameNumber
            if (curr <= countFrames) listFrames.add(curr)
        }
        return listFrames
    }

    fun deleteAll(file: File) {
        Main.faceRepo.deleteAll(file.id)
    }
}