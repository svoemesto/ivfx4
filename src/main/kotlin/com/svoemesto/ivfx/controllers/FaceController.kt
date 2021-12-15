package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
class FaceController {


    companion object {

        fun createOrUpdate(faceExtJson: FaceExtJson, fileExt: FileExt): Face {

            var face = Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame).firstOrNull()
            if (face != null) {
                face.file = fileExt.file
                val faceExt = FaceExt(face, fileExt)
                if (!faceExt.isConfirmed) {
                    var needToSave = false
                    if (face.personRecognizedName != faceExtJson.personRecognizedName) {

                        if (faceExtJson.personRecognizedName != "") {
                            face.personRecognizedName = faceExtJson.personRecognizedName
                            face.personRecognizedId = PersonController.getPersonExtIdByProjectExtIdAndNameInRecognizer(fileExt.projectExt,
                                faceExtJson.personRecognizedName, true, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                            needToSave = true
                        } else {
                            face.personRecognizedId = PersonController.getUndefinded(fileExt.projectExt).person.id
                        }

                    }
                    if (face.recognizeProbability != faceExtJson.recognizeProbability) {
                        face.recognizeProbability = faceExtJson.recognizeProbability
                        needToSave = true
                    }
                    if (face.startX != faceExtJson.startX) {
                        face.startX = faceExtJson.startX
                        needToSave = true
                    }
                    if (face.startY != faceExtJson.startY) {
                        face.startY = faceExtJson.startY
                        needToSave = true
                    }
                    if (face.endX != faceExtJson.endX) {
                        face.endX = faceExtJson.endX
                        needToSave = true
                    }
                    if (face.endY != faceExtJson.endY) {
                        face.endY = faceExtJson.endY
                        needToSave = true
                    }
                    if (!faceExt.vector.contentEquals(faceExtJson.vector)) {
                        faceExt.vector = faceExtJson.vector
                        needToSave = true
                    }
                    if (needToSave) save(face)
                }
                return face

            } else {
                face = Face()
            }
            face.file = fileExt.file
            face.frameNumber = faceExtJson.frameNumber
            face.faceNumberInFrame = faceExtJson.faceNumberInFrame
            face.personId = 0
            face.personRecognizedName = faceExtJson.personRecognizedName
            if (faceExtJson.personRecognizedName != "") {
                face.personRecognizedId = PersonController.getPersonExtIdByProjectExtIdAndNameInRecognizer(fileExt.projectExt,
                    faceExtJson.personRecognizedName, true, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
            } else {
                face.personRecognizedId = PersonController.getUndefinded(fileExt.projectExt).person.id
            }
            face.recognizeProbability = faceExtJson.recognizeProbability
            face.startX = faceExtJson.startX
            face.startY = faceExtJson.startY
            face.endX = faceExtJson.endX
            face.endY = faceExtJson.endY
            face.vectorText = faceExtJson.vector.joinToString(separator = "|", prefix = "", postfix = "")
            face.isConfirmed = false

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

        data class FrameToDetectFaces(val fileId: Long, val frameNumber: Int, val pathToFrameFile: String) {
        }

        fun getArrayFramesToDetectFaces(fileExt: FileExt): Array<FrameToDetectFaces> {
            val listFrameNumbers: List<Int> = getFramesToRecognize(fileExt)
            val list: MutableList<FrameToDetectFaces> = mutableListOf() //<Frame>(listFrameNumbers.size)
            for (i in listFrameNumbers.indices) {
                list.add(FrameToDetectFaces(fileExt.file.id, listFrameNumbers[i], "${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", listFrameNumbers[i])}.jpg"))
            }
            return list.toTypedArray()
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