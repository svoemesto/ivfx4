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
    private var _biSmall: BufferedImage? = null
    val biSmall: BufferedImage
        get() {
            if (_biSmall == null) {
                _biSmall = ImageIO.read(IOFile(if (IOFile(pathToSmall).exists()) pathToSmall else pathToStubSmall))
            }
            return _biSmall!!
        }
    private var _previewSmall: ImageView? = null
    val previewSmall: ImageView
        get() {
            if (_previewSmall == null) {
                val bi: BufferedImage = biSmall
                _previewSmall = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return _previewSmall!!
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
    private var _biMedium: BufferedImage? = null
    val biMedium: BufferedImage
        get() {
            if (_biMedium == null) {
                _biMedium = ImageIO.read(IOFile(if (IOFile(pathToMedium).exists()) pathToMedium else pathToStubMedium))
            }
            return _biMedium!!
        }
    private var _previewMedium: ImageView? = null
    val previewMedium: ImageView
        get() {
            if (_previewMedium == null) {
                _previewMedium = ImageView(ConvertToFxImage.convertToFxImage(biMedium))
            }
            return _previewMedium!!
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

    private var _biFull: BufferedImage? = null
    val biFull: BufferedImage
        get() {
            if (_biFull == null) {
                _biFull = ImageIO.read(IOFile(if (IOFile(pathToFull).exists()) pathToFull else pathToStubFull))
            }
            return _biFull!!
        }
    private var _previewFull: ImageView? = null
    val previewFull: ImageView
        get() {
            if (_previewFull == null) {
                _previewFull = ImageView(ConvertToFxImage.convertToFxImage(biFull))
            }
            return _previewFull!!
        }
    private var _labelFull: Label? = null
    val labelFull: Label
        get() {
            if (_labelFull == null) {
                _labelFull = Label()
                _labelFull!!.setPrefSize(Main.FULL_FRAME_W, Main.FULL_FRAME_H)
                _labelFull!!.graphic = previewFull
                _labelFull!!.alignment = Pos.CENTER
            }
            return _labelFull!!
        }

    fun resetPreviewSmall() {
        _biSmall = null
        _previewSmall = null
    }

    fun resetPreviewMedium() {
        _biMedium = null
        _previewMedium = null
    }

    fun resetPreviewFull() {
        _biFull = null
        _previewFull = null
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