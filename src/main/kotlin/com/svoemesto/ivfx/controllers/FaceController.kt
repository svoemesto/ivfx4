package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FaceExtJson
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
import javafx.geometry.Pos
import org.springframework.stereotype.Controller
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File as IOFile

@Controller
class FaceController {


    companion object {

        fun createOrUpdate(faceExtJson: FaceExtJson, fileExt: FileExt, undefindedPerson: PersonExt, nonPerson: PersonExt): FaceExt {

            val w = faceExtJson.endX - faceExtJson.startX
            val h = faceExtJson.endY - faceExtJson.startY
            val d = if(w>h) w/h.toDouble() else h/w.toDouble()

            var face = if (faceExtJson.frameId == 0L) {
                Main.faceRepo.findByFileIdAndFrameNumberAndFaceNumberInFrame(faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame).firstOrNull()
            } else {
                if (faceExtJson.faceId == 0L) {
                    null
                } else {
                    Main.faceRepo.findById(faceExtJson.frameId).orElse(null)
                }
            }

            if (face != null) {
                face.file = fileExt.file
                if (d > 4) {
                    face.person = nonPerson.person
                } else {
                    if (faceExtJson.personRecognizedName != "") {
                        if (faceExtJson.recognizeProbability > 0.3) {
                            face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                                faceExtJson.personRecognizedName, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                        } else {
                            face.person = undefindedPerson.person
                        }
                    } else {
                        face.person = undefindedPerson.person
                    }
                }

                val personExt = PersonExt(face.person, fileExt.projectExt)
                val faceExt = FaceExt(face, fileExt, personExt)

                var needToSave = false
                if (face.personRecognizedName != faceExtJson.personRecognizedName) {

                    if (faceExtJson.personRecognizedName != "") {
                        face.personRecognizedName = faceExtJson.personRecognizedName
                        if (faceExtJson.recognizeProbability > 0.3) {
                            face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                                faceExtJson.personRecognizedName, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                        } else {
                            face.person = undefindedPerson.person
                        }
                        needToSave = true
                    } else {
                        face.person = undefindedPerson.person
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

                return faceExt

            } else {
                face = Face()
                face.isExample = false
                face.isManual = false
                face.file = fileExt.file
            }

            if (d > 4) {
                face.person = nonPerson.person
            } else {
                if (faceExtJson.personRecognizedName != "") {
                    if (faceExtJson.recognizeProbability > 0.3) {
                        face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                            faceExtJson.personRecognizedName, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
                    } else {
                        face.person = undefindedPerson.person
                    }
                } else {
                    face.person = undefindedPerson.person
                }
            }

            val personExt = PersonExt(face.person, fileExt.projectExt)
            val faceExt = FaceExt(face, fileExt, personExt)

            face.frameNumber = faceExtJson.frameNumber
            face.faceNumberInFrame = faceExtJson.faceNumberInFrame
            face.personRecognizedName = faceExtJson.personRecognizedName
//            if (faceExtJson.personRecognizedName != "") {
//                face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
//                    faceExtJson.personRecognizedName, faceExtJson.fileId, faceExtJson.frameNumber, faceExtJson.faceNumberInFrame)
//            } else {
//                face.person = undefindedPerson.person
//            }
            face.recognizeProbability = faceExtJson.recognizeProbability
            face.startX = faceExtJson.startX
            face.startY = faceExtJson.startY
            face.endX = faceExtJson.endX
            face.endY = faceExtJson.endY
            face.vectorText = faceExtJson.vector.joinToString(separator = "|", prefix = "", postfix = "")

            save(face)

            return faceExt

        }

        fun save(face: Face) {
            Main.faceRepo.save(face)
        }

        fun getListFaces(file: File): MutableList<Face> {
            val result = Main.faceRepo.findByFileId(file.id).toMutableList()
            result.forEach { face->
                face.file = file
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(file.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(file.project)
                }
            }
            return result
        }

        fun getListFacesExt(fileExt: FileExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileId(fileExt.file.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            val personExtMap: MutableMap<String, PersonExt> = mutableMapOf()
            result.forEach { face->
                face.file = fileExt.file
                var person = if (personExtMap.containsKey(face.personRecognizedName)) personExtMap[face.personRecognizedName]?.person else {
                    if (face.personRecognizedName != "") {
                        PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                            face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                    } else {
                        PersonController.getUndefinded(fileExt.projectExt.project)
                    }
                }
                val personExt = PersonExt(person!!, fileExt.projectExt)
                personExtMap[face.personRecognizedName] = personExt
                face.person = person

                listFacesExt.add(FaceExt(face, fileExt , personExt))
            }
            return listFacesExt
        }

        fun getListFacesExtToRecognize(fileExt: FileExt): MutableList<FaceExt> {
            val undefindedPerson = PersonController.getUndefinded(fileExt.projectExt.project)
            val result = Main.faceRepo.findFacesToRecognize(fileExt.file.id, undefindedPerson.id).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            val personExtMap: MutableMap<String, PersonExt> = mutableMapOf()
            result.forEach { face->
                face.file = fileExt.file
                var person = if (personExtMap.containsKey(face.personRecognizedName)) personExtMap[face.personRecognizedName]?.person else {
                    if (face.personRecognizedName != "") {
                        PersonController.getPersonByProjectIdAndNameInRecognizer(fileExt.projectExt.project,
                            face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                    } else {
                        undefindedPerson
                    }
                }
                val personExt = PersonExt(person!!, fileExt.projectExt)
                personExtMap[face.personRecognizedName] = personExt
                face.person = person

                listFacesExt.add(FaceExt(face, fileExt , personExt))
            }
            return listFacesExt
        }

        fun getListFacesToTrain(project: Project): MutableList<Face> {
            return Main.faceRepo.getListFacesToTrain(project.id).toMutableList()
        }

        fun getListFacesExt(frameExt: FrameExt): MutableList<FaceExt> {
            val result = Main.faceRepo.findByFileIdAndFrameNumber(frameExt.fileExt.file.id, frameExt.frame.frameNumber).toMutableList()
            var listFacesExt: MutableList<FaceExt> = mutableListOf()
            result.forEach { face->
                face.file = frameExt.fileExt.file
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(frameExt.fileExt.projectExt.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(frameExt.fileExt.projectExt.project)
                }
                listFacesExt.add(FaceExt(face, frameExt.fileExt, PersonExt(face.person, frameExt.fileExt.projectExt)))
            }
            return listFacesExt
        }

        fun getListFacesExt(fileExt: FileExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByFileIdAndPersonId(fileExt.file.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            return result.map {
                it.file = fileExt.file
                it.person = personExt.person
                FaceExt(it, fileExt, personExt)
            }.toMutableList()

        }

        fun getListFacesExt(shotExt: ShotExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByShotIdAndPersonId(shotExt.shot.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            val setFilesExt: MutableSet<FileExt> = mutableSetOf()
            return result.mapNotNull { face ->

                var fileId = 0L
                var fileExt: FileExt? = null

                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
                val stFaces = Main.connection.prepareStatement(sqlFaces)
                stFaces.setLong(1, face.id)
                val rsFaces = stFaces.executeQuery()
                while (rsFaces.next()) {
                    fileId = rsFaces.getLong("file_id")
                    break
                }
                if (fileId != 0L) {
                    fileExt = setFilesExt.firstOrNull { it.file.id == fileId }
                    if (fileExt == null) {
                        fileExt = FileController.getFileExt(fileId, shotExt.fileExt.projectExt.project)
                        setFilesExt.add(fileExt)
                    }
                    face.file = fileExt.file
                    face.person = personExt.person
                    FaceExt(face, fileExt, personExt)
                } else {
                    null
                }
            }.toMutableList()

        }

        fun getListFacesExt(projectExt: ProjectExt,
                            personExt: PersonExt,
                            loadNotExample: Boolean = true,
                            loadExample: Boolean = true,
                            loadNotManual: Boolean = true,
                            loadManual: Boolean = true): MutableList<FaceExt> {

            var result: MutableList<Face> = mutableListOf()

            result = Main.faceRepo.findByProjectIdAndPersonId(projectExt.project.id, personExt.person.id, loadNotExample, loadExample, loadNotManual, loadManual).toMutableList()

            val setFilesExt: MutableSet<FileExt> = mutableSetOf()
            return result.mapNotNull { face ->

                var fileId = 0L
                var fileExt: FileExt? = null

                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
                val stFaces = Main.connection.prepareStatement(sqlFaces)
                stFaces.setLong(1, face.id)
                val rsFaces = stFaces.executeQuery()
                while (rsFaces.next()) {
                    fileId = rsFaces.getLong("file_id")
                    break
                }
                if (fileId != 0L) {
                    fileExt = setFilesExt.firstOrNull { it.file.id == fileId }
                    if (fileExt == null) {
                        fileExt = FileController.getFileExt(fileId, projectExt.project)
                        setFilesExt.add(fileExt)
                    }
                    face.file = fileExt.file
                    face.person = personExt.person
                    FaceExt(face, fileExt, personExt)
                } else {
                    null
                }
            }.toMutableList()

        }

        data class FrameToDetectFaces(val projectId: Long,
                                      val fileId: Long,
                                      val frameNumber: Int,
                                      val pathToFrameFile: String) {
        }

        fun getArrayFramesToDetectFaces(fileExt: FileExt): Array<FrameToDetectFaces> {
            val listFrameNumbers: List<Int> = getFramesToRecognize(fileExt)
            val list: MutableList<FrameToDetectFaces> = mutableListOf() //<Frame>(listFrameNumbers.size)
            for (i in listFrameNumbers.indices) {
                list.add(FrameToDetectFaces(fileExt.projectExt.project.id,
                    fileExt.file.id,
                    listFrameNumbers[i],
                    "${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", listFrameNumbers[i])}.jpg"))
            }
            return list.toTypedArray()
        }

        fun getFramesToRecognize(fileExt: FileExt): List<Int> {
            var curr = 0
            val listFrames: MutableList<Int> = mutableListOf()
            val listShots: MutableList<Shot> = fileExt.file.shots.toMutableList()
            listShots.sort()
            val countFrames = fileExt.framesCount
            for (shot in listShots) {
                val stepFrames = if (shot.lastFrameNumber - shot.firstFrameNumber < 15) 3
                                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 30) 5
                                 else if (shot.lastFrameNumber - shot.firstFrameNumber < 60) 10 else 20
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
                if (face.personRecognizedName != "") {
                    face.person = PersonController.getPersonByProjectIdAndNameInRecognizer(file.project,
                        face.personRecognizedName, face.file.id, face.frameNumber, face.faceNumberInFrame)
                } else {
                    face.person = PersonController.getUndefinded(file.project)
                }
                val projectExt = ProjectExt(project)
                val fileExt = FileExt(file, projectExt)
                return FaceExt(face, fileExt, PersonExt(face.person, fileExt.projectExt))
            }
            return null
        }

        fun getOverlayedFrame(frameExt: FrameExt, faceExt: FaceExt? = null, fullFrame: Boolean = false): BufferedImage? {

            var bi: BufferedImage? = null
            var fileId: Long = 0L
            var personId: Long = 0L
            var frameId: Long = 0L

            val fileExt = frameExt.fileExt
            val projectExt = frameExt.fileExt.projectExt
//            val personExt = faceExt!!.personExt

//            val sqlFrames = "select * from tbl_frames where file_id = ? and frame_number = ?"
//            val stFrames = Main.connection.prepareStatement(sqlFrames)
//            stFrames.setLong(1, fileExt.file.id)
//            stFrames.setInt(2, faceExt.face.frameNumber)
//            val rsFrames = stFrames.executeQuery()
//            while (rsFrames.next()) {
//                frameId = rsFrames.getLong("id")
//                break
//            }


//            val listFacesInCurrentFrame = Main.faceRepo.getListFacesInFrame(fileExt.file.id, frameExt.frame.frameNumber).toMutableList()
//            val listFacesExt: MutableList<FaceExt> = mutableListOf()
//            listFacesInCurrentFrame.forEach { face ->
//
//                face.file = fileExt.file
//
//                val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
//                val stFaces = Main.connection.prepareStatement(sqlFaces)
//                stFaces.setLong(1, face.id)
//                val rsFaces = stFaces.executeQuery()
//                while (rsFaces.next()) {
//                    personId = rsFaces.getLong("person_id")
//                    break
//                }
//
//                if (personId != 0L) {
//
//                    val person = Main.personRepo.findById(personId).orElse(null)
//                    if (person != null) {
//                        val currentPersonExt = PersonExt(person, projectExt)
//                        val currentFaceExt = FaceExt(face,fileExt, currentPersonExt)
//                        listFacesExt.add(currentFaceExt)
//                    }
//                }
//            }

            val listFacesExt = frameExt.facesExt()

            bi = if (fullFrame) frameExt.biFull else frameExt.biMedium

            if (bi != null) {

                val frameWidth: Int = bi.width
                val frameHeight: Int = bi.height
                //TODO Брать ширину картинки из свойств файла
                val faceSourceFrameWidth = Main.FULL_FRAME_W
                val scaleFactor = frameWidth / faceSourceFrameWidth

                listFacesExt.forEach { faceExtInFrame ->


                    val startX = (scaleFactor * faceExtInFrame.startX).toInt()
                    val startY = (scaleFactor * faceExtInFrame.startY).toInt()
                    val endX = (scaleFactor * faceExtInFrame.endX).toInt()
                    val endY = (scaleFactor * faceExtInFrame.endY).toInt()

                    val opaque = 1.0f

                    var textColor = Color.YELLOW
                    if (faceExtInFrame.face.isManual) textColor = Color.RED
                    if (faceExt!=null && faceExt.face.id == faceExtInFrame.face.id && faceExt.face.isManual) {
                        textColor = Color.ORANGE
                    } else if (faceExt!=null && faceExt.face.id == faceExtInFrame.face.id && !faceExt.face.isManual) {
                        textColor = Color.GREEN
                    }

                    val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
                    val imageType = BufferedImage.TYPE_INT_ARGB
                    val textPosition = Pos.BOTTOM_CENTER
                    val textToOverlay = faceExtInFrame.personExt.person.name

                    val resultImage = BufferedImage(frameWidth, frameHeight, imageType)
                    val graphics2D = resultImage.graphics as Graphics2D
                    graphics2D.drawImage(bi, 0, 0, null)
                    val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opaque)
                    graphics2D.composite = alphaChannel

                    graphics2D.font = textFont
                    val fontMetrics = graphics2D.fontMetrics
                    val rect = fontMetrics.getStringBounds(textToOverlay, graphics2D)
                    val rectW = rect.width.toInt()
                    val rectH = rect.height.toInt()

                    var centerY = startY
                    centerY = if (centerY < 20) {
                        endY - startY + 25
                    } else {
                        centerY - 3
                    }
                    if (centerY > frameHeight) centerY = frameHeight - 5

                    graphics2D.color = Color.BLACK
                    graphics2D.fillRect(startX - 3, centerY - rectH, rectW + 6, rectH + 6)
                    graphics2D.color = textColor

                    graphics2D.drawString(textToOverlay, startX, centerY)
                    graphics2D.drawRect(startX, startY, endX - startX, endY - startY)
                    if (faceExt != null && faceExt.face.id == faceExtInFrame.face.id) {
                        graphics2D.drawRect(startX - 1, startY - 1, endX - startX + 2, endY - startY + 2)
                    }
                    graphics2D.dispose()
                    bi = resultImage
                }
            }


            return bi
        }

    }


}