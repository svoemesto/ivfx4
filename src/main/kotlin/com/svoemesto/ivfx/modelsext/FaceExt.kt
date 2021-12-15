package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

class FaceExt(@Transient var face: Face, @Transient var fileExt: FileExt) {

    val fileId: Long get() = face.file.id
    val frameNumber: Int get() = face.frameNumber
    val faceNumberInFrame: Int get() = face.faceNumberInFrame
    val pathToFrameFile: String get() = "${fileExt.folderFramesFull}${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}.jpg"
    val pathToFaceFile: String get() = "${fileExt.folderFramesFull}.faces${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
    val pathToPreviewFile: String get() = "${fileExt.folderFramesFull}.faces.preview${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
    val personId: Long get() = face.personId
    val personRecognizedName: String get() = face.personRecognizedName
    val personRecognizedId: Long get() = face.personRecognizedId
    val recognizeProbability: Double get() = face.recognizeProbability
    val startX: Int get() = face.startX
    val startY: Int get() = face.startY
    val endX: Int get() = face.endX
    val endY: Int get() = face.endY
    val isConfirmed: Boolean get() = face.isConfirmed
    var vector: DoubleArray
        get() {
            val textVector: Array<String> = face.vectorText.split("\\|".toRegex()).toTypedArray()
            val result = DoubleArray(textVector.size)
            for (i in textVector.indices) {
                result[i] = textVector[i].toDouble()
            }
            return result
        }
        set(value) {
            face.vectorText = value.joinToString(separator = "|", prefix = "", postfix = "")
            FaceController.save(face)
        }

    @Transient
    var previewSmall: ImageView? = null
        get() {
            if (field == null) {
                lateinit var bi: BufferedImage
                if (IOFile(pathToPreviewFile).exists()) {
                    bi = ImageIO.read(IOFile(pathToPreviewFile))
                } else {
                    if (!IOFile(pathToPreviewFile).parentFile.exists()) IOFile(pathToPreviewFile).parentFile.mkdir()
                    val biSource = ImageIO.read(IOFile(FrameController.getFrameExt(fileId, frameNumber, fileExt!!.projectExt.project).pathToFull))
                    bi = OverlayImage.extractRegion(biSource, startX, startY, endX, endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                    val outputfile = IOFile(pathToPreviewFile)
                    ImageIO.write(bi, "jpg", outputfile)
                }
                field = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return field
        }

    @Transient
    var labelSmall: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
                field!!.graphic = previewSmall
                field!!.alignment = Pos.CENTER
            }
            return field
        }

}