package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Shot
import org.springframework.stereotype.Controller

@Controller
class FaceController {

    class FaceExt(val face: Face) {

    }


    fun getArrayFacesExt(file: File): Array<FaceExt> {
        val listFrameNumbers: List<Int> = getFramesToRecognize(file)
        val listFacesExt: MutableList<FaceExt> = mutableListOf() //<Frame>(listFrameNumbers.size)
        for (i in listFrameNumbers.indices) {
            val face = Face()
            face.id = 0
            face.file = file
            face.frameNumber = listFrameNumbers[i]
            face.faceNumberInFrame = 0
            face.tagId = 0
            face.tagRecognizedId = 0
            face.recognizeProbability = 0.0
            face.startX = 0
            face.startY = 0
            face.endX = 0
            face.endY = 0
            face.vector = doubleArrayOf(0.0)
            listFacesExt.add(FaceExt(face))
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
}