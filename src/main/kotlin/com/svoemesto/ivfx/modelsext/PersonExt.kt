package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

class PersonExt(val person: Person, val projectExt: ProjectExt) : Comparable<PersonExt> {
    override fun compareTo(other: PersonExt): Int {
        return this.person.compareTo(other.person)
    }
    val pathToSmall: String get() = "${projectExt.folderPersons}${IOFile.separator}${person.uuid}.small.jpg"
    val pathToMedium: String get() = "${projectExt.folderPersons}${IOFile.separator}${person.uuid}.medium.jpg"
    private var _previewSmall: ImageView? = null
    val previewSmall: ImageView
        get() {
            if (_previewSmall == null) {
                lateinit var bi: BufferedImage
                if (IOFile(pathToSmall).exists()) {
                    bi = ImageIO.read(IOFile(pathToSmall))
                } else {
                    if (person.fileIdForPreview != 0L) {
                        if (person.frameNumberForPreview != 0 && person.faceNumberForPreview == 0) {
                            // берем картинку из кадра
                            val frameExt = FrameController.getFrameExt(person.fileIdForPreview, person.frameNumberForPreview, projectExt.project)
                            bi = ImageIO.read(IOFile(frameExt.pathToSmall))
                        } else if (person.frameNumberForPreview != 0 && person.faceNumberForPreview != 0) {
                            // берем картинку из лица
                            val faceExt = FaceController.getFaceExt(person.fileIdForPreview, person.frameNumberForPreview, person.faceNumberForPreview, projectExt.project)
                            if (faceExt != null) {
                                if (!IOFile(pathToSmall).parentFile.exists()) IOFile(pathToSmall).parentFile.mkdir()
                                val biSource = ImageIO.read(IOFile(FrameController.getFrameExt(faceExt.fileId, faceExt.frameNumber, faceExt.fileExt.projectExt.project).pathToFull))

                                bi = OverlayImage.extractRegion(biSource, faceExt.startX, faceExt.startY, faceExt.endX, faceExt.endY,
                                    Main.PREVIEW_FRAME_W.toInt(), Main.PREVIEW_FRAME_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_PERSON_CROPPING)
                                val outputfile = IOFile(pathToSmall)
                                ImageIO.write(bi, "jpg", outputfile)
                            }
                        } else {
                            val fileName = PersonExt::class.java.getResource("blank_person_small.jpg")!!.file.substring(1)
                            println(fileName)
                            bi = ImageIO.read(IOFile(fileName))
                        }
                    } else {
                        val fileName = PersonExt::class.java.getResource("blank_person_small.jpg")!!.file.substring(1)
                        println(fileName)
                        bi = ImageIO.read(IOFile(fileName))
                    }
                }

                bi = OverlayImage.setOverlayUnderlineText(bi, person.name)
                _previewSmall = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return _previewSmall!!
        }

    private var _previewMedium: ImageView? = null
    val previewMedium: ImageView
        get() {
            if (_previewMedium == null) {
                lateinit var bi: BufferedImage
                if (IOFile(pathToMedium).exists()) {
                    bi = ImageIO.read(IOFile(pathToMedium))
                } else {
                    if (person.fileIdForPreview != 0L) {
                        if (person.frameNumberForPreview != 0 && person.faceNumberForPreview == 0) {
                            // берем картинку из кадра
                            val frameExt = FrameController.getFrameExt(person.fileIdForPreview, person.frameNumberForPreview, projectExt.project)
                            bi = ImageIO.read(IOFile(frameExt.pathToSmall))
                        } else if (person.frameNumberForPreview != 0 && person.faceNumberForPreview != 0) {
                            // берем картинку из лица
                            val faceExt = FaceController.getFaceExt(person.fileIdForPreview, person.frameNumberForPreview, person.faceNumberForPreview, projectExt.project)
                            if (faceExt != null) {
                                if (!IOFile(pathToMedium).parentFile.exists()) IOFile(pathToMedium).parentFile.mkdir()
                                val biSource = ImageIO.read(IOFile(FrameController.getFrameExt(faceExt.fileId, faceExt.frameNumber, faceExt.fileExt.projectExt.project).pathToFull))
                                bi = OverlayImage.extractRegion(biSource, faceExt.startX, faceExt.startY, faceExt.endX, faceExt.endY,
                                    Main.MEDIUM_FRAME_W.toInt(), Main.MEDIUM_FRAME_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                                val outputfile = IOFile(pathToMedium)
                                ImageIO.write(bi, "jpg", outputfile)
                            }
                        } else {
                            val fileName = PersonExt::class.java.getResource("blank_person_medium.jpg")!!.file.substring(1)
                            println(fileName)
                            bi = ImageIO.read(IOFile(fileName))
                        }
                    } else {
                        val fileName = PersonExt::class.java.getResource("blank_person_medium.jpg")!!.file.substring(1)
                        println(fileName)
                        bi = ImageIO.read(IOFile(fileName))
                    }
                }

                _previewMedium = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return _previewMedium!!
        }

    private var _labelSmall: Label? = null
    val labelSmall: Label
        get() {
            if (_labelSmall == null) {
                _labelSmall = Label()
                _labelSmall!!.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                _labelSmall!!.graphic = previewSmall
                _labelSmall!!.alignment = Pos.CENTER
            }
            return _labelSmall!!
        }

    private var _labelMedium: Label? = null
    val labelMedium: Label
        get() {
            if (_labelMedium == null) {
                _labelMedium = Label()
                _labelMedium!!.setPrefSize(Main.MEDIUM_FRAME_W, Main.MEDIUM_FRAME_H)
                _labelMedium!!.graphic = previewMedium
                _labelMedium!!.alignment = Pos.CENTER
            }
            return _labelMedium!!
        }

    fun resetPreview() {
        _previewSmall = null
        _previewMedium = null
        _labelSmall = null
        _labelMedium = null
    }
}