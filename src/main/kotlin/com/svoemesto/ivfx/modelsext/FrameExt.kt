package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

data class FrameExt(val frame: Frame,
               val fileExt: FileExt) {

    companion object {
        val pathToStubSmall: String = FrameExt::class.java.getResource("blank_frame_small.jpg")!!.toString()
        val pathToStubMedium: String = FrameExt::class.java.getResource("blank_frame_medium.jpg")!!.toString()
        val pathToStubFull: String = FrameExt::class.java.getResource("blank_frame_full.jpg")!!.toString()
    }

    val pathToSmall: String get() = "${fileExt.folderFramesSmall}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    val pathToMedium: String get() = "${fileExt.folderFramesMedium}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    val pathToFull: String get() = "${fileExt.folderFramesFull}${IOFile.separator}${fileExt.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    var biSmall: BufferedImage? = null
        get() {
            if (field == null) {
                field = ImageIO.read(IOFile(if (IOFile(pathToSmall).exists()) pathToSmall else pathToStubSmall))
            }
            return field
        }
    var previewSmall: ImageView? = null
        get() {
            if (field == null) {
                val bi: BufferedImage = biSmall!!
                field = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return field
        }
    var labelSmall: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                field!!.graphic = previewSmall
                field!!.alignment = Pos.CENTER
            }
            return field
        }
    var biMedium: BufferedImage? = null
        get() {
            if (field == null) {
                field = ImageIO.read(IOFile(if (IOFile(pathToMedium).exists()) pathToMedium else pathToStubMedium))
            }
            return field
        }
    var previewMedium: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(biMedium))
            }
            return field
        }
    var labelMedium: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(Main.MEDIUM_FRAME_W, Main.MEDIUM_FRAME_H)
                field!!.graphic = previewMedium
                field!!.alignment = Pos.CENTER
            }
            return field
        }

    var biFull: BufferedImage? = null
        get() {
            if (field == null) {
                field = ImageIO.read(IOFile(if (IOFile(pathToFull).exists()) pathToFull else pathToStubFull))
            }
            return field
        }
    var previewFull: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(biFull))
            }
            return field
        }
    var labelFull: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(Main.FULL_FRAME_W, Main.FULL_FRAME_H)
                field!!.graphic = previewFull
                field!!.alignment = Pos.CENTER
            }
            return field
        }

    fun facesExt() : MutableList<FaceExt> {
        val listFacesInCurrentFrame = Main.faceRepo.getListFacesInFrame(fileExt.file.id, frame.frameNumber).toMutableList()
        val listFacesExt: MutableList<FaceExt> = mutableListOf()
        var personId: Long = 0L
        listFacesInCurrentFrame.forEach { face ->

            face.file = fileExt.file

            val sqlFaces = "select * from tbl_faces as tf where tf.id = ?"
            val stFaces = Main.connection.prepareStatement(sqlFaces)
            stFaces.setLong(1, face.id)
            val rsFaces = stFaces.executeQuery()
            while (rsFaces.next()) {
                personId = rsFaces.getLong("person_id")
                break
            }

            if (personId != 0L) {

                val person = Main.personRepo.findById(personId).orElse(null)
                if (person != null) {
                    val currentPersonExt = PersonExt(person, fileExt.projectExt)
                    val currentFaceExt = FaceExt(face,fileExt, currentPersonExt)
                    listFacesExt.add(currentFaceExt)
                }
            }
        }
        return listFacesExt
    }
}