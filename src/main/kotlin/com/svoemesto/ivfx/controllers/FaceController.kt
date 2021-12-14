package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
class FaceController {


    companion object {

        fun createOrUpdate(faceExt: FaceExt, fileExt: FileExt): Face {

            var face = Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(faceExt.fileId, faceExt.frameNumber, faceExt.faceNumberInFrame).firstOrNull()
            if (face != null) {
                face.file = fileExt.file
                val faceExtTmp = FaceExt(face, fileExt)
                if (faceExtTmp == faceExt) {
                    return face
                }
            } else {
                face = Face()
            }
            face.file = fileExt.file
            face.frameNumber = faceExt.frameNumber
            face.faceNumberInFrame = faceExt.faceNumberInFrame
            face.personId = faceExt.personId
            face.personRecognizedName = faceExt.personRecognizedName
            face.personRecognizedId = faceExt.personRecognizedId
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

        fun getListFacesExt(fileExt: FileExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileId(fileExt.file.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            result.forEach { face->
                face.file = fileExt.file
                listFacesExt.add(FaceExt(face, fileExt))
            }
            return listFacesExt
        }

        fun getListFacesExt(fileExt: FileExt, personExt: PersonExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileIdAndPersonRecognizedId(fileExt.file.id, personExt.person.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            result.forEach { face->
                face.file = fileExt.file
                listFacesExt.add(FaceExt(face, fileExt))
            }
            return listFacesExt
        }

        fun getArrayFacesExt(fileExt: FileExt): Array<FaceExt> {
            val listFrameNumbers: List<Int> = getFramesToRecognize(fileExt)
            val listFacesExt: MutableList<FaceExt> = mutableListOf() //<Frame>(listFrameNumbers.size)
            for (i in listFrameNumbers.indices) {
                val faceExt = FaceExt()
                faceExt.fileId = fileExt.file.id
                faceExt.frameNumber = listFrameNumbers[i]
                faceExt.pathToFrameFile = "${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", listFrameNumbers[i])}.jpg"
                listFacesExt.add(faceExt)
            }
            return listFacesExt.toTypedArray()
        }

        fun getFramesToRecognize(fileExt: FileExt): List<Int> {
            var curr = 0
            val listFrames: MutableList<Int> = mutableListOf()
            val listShots: List<Shot> = fileExt.file.shots
            val countFrames = fileExt.framesCount
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

        fun getFace(fileId: Long, frameNumber: Int, faceNumber: Int): Face? {
            return Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(fileId, frameNumber, faceNumber).firstOrNull()
        }

        fun getFaceExt(fileId: Long, frameNumber: Int, faceNumber: Int, project: Project): FaceExt? {
            val face = getFace(fileId, frameNumber, faceNumber)
            if (face != null) {
                val file = project.files.first { it.id == fileId }
                face.file = file
                val fileExt = FileExt(file, ProjectExt(project))
                return FaceExt(face, fileExt)
            }
            return null
        }

    }


}