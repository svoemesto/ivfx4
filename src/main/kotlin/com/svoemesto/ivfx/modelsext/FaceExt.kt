package com.svoemesto.ivfx.modelsext

import com.google.gson.annotations.SerializedName
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

class FaceExt(@Transient var face: Face, @Transient var fileExt: FileExt, @Transient var personExt: PersonExt): Comparable<FaceExt> {

    override fun compareTo(other: FaceExt): Int {
        return this.face.compareTo(other.face)
    }

    @SerializedName("projectId")
    var projectId: Long  = fileExt.projectExt.project.id

    @SerializedName("faceId")
    var faceId: Long = face.id

    @SerializedName("fileId")
    var fileId: Long = fileExt.file.id

    @SerializedName("personId")
    var personId: Long = personExt.person.id

    @SerializedName("personType")
    var personType: String = personExt.person.personType.name

    val frameNumber: Int get() = face.frameNumber
    @SerializedName("frameNumber")
    var toSerializeFrameNumber = frameNumber

    val faceNumberInFrame: Int get() = face.faceNumberInFrame
    @SerializedName("faceNumberInFrame")
    var toSerializeFaceNumberInFrame = faceNumberInFrame

    val pathToFrameFile: String get() = "${fileExt.folderFramesFull}${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}.jpg"
    @SerializedName("pathToFrameFile")
    var toSerializePathToFrameFile = pathToFrameFile

    val pathToFaceFile: String get() = "${fileExt.folderFacesFull}${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
    @SerializedName("pathToFaceFile")
    var toSerializePathToFaceFile = pathToFaceFile

    val pathToPreviewFile: String get() = "${fileExt.folderFacesPreview}${IOFile.separator}${face.file.shortName}_frame_${String.format("%06d", face.frameNumber)}_face_${String.format("%02d", face.faceNumberInFrame)}.jpg"
    @SerializedName("pathToPreviewFile")
    var toSerializePathToPreviewFile = pathToPreviewFile

    @SerializedName("personRecognizedName")
    var personRecognizedName: String = face.personRecognizedName

    val recognizeProbability: Double get() = face.recognizeProbability
    @SerializedName("recognizeProbability")
    var toSerializeRecognizeProbability = recognizeProbability

    val startX: Int get() = face.startX
    @SerializedName("startX")
    var toSerializeStartX = startX

    val startY: Int get() = face.startY
    @SerializedName("startY")
    var toSerializeStartY = startY

    val endX: Int get() = face.endX
    @SerializedName("endX")
    var toSerializeEndX = endX

    val endY: Int get() = face.endY
    @SerializedName("endY")
    var toSerializeEndY = endY

    val isExample: Boolean get() = face.isExample
    @SerializedName("isExample")
    var toSerializeIsExample = isExample

    val isManual: Boolean get() = face.isManual
    @SerializedName("isManual")
    var toSerializeIsManual = isManual

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
    @SerializedName("vector")
    var toSerializeVector = vector

    @Transient
    private var _previewSmall: ImageView? = null

    val previewSmall: ImageView
        get() {
            if (_previewSmall == null) {
                lateinit var bi: BufferedImage
                if (IOFile(pathToPreviewFile).exists()) {
                    bi = ImageIO.read(IOFile(pathToPreviewFile))
                } else {
                    if (!IOFile(pathToPreviewFile).parentFile.exists()) IOFile(pathToPreviewFile).parentFile.mkdir()
                    val biSource = ImageIO.read(IOFile(FrameController.getFrameExt(fileId, frameNumber, fileExt.projectExt.project).pathToFull))
                    bi = OverlayImage.extractRegion(biSource, startX, startY, endX, endY, Main.PREVIEW_FACE_W.toInt(), Main.PREVIEW_FACE_H.toInt(), Main.PREVIEW_FACE_EXPAND_FACTOR, Main.PREVIEW_FACE_CROPPING)
                    if (face.isExample) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.GREEN, 1.0F)
                    if (face.isManual) bi = OverlayImage.setOverlayTriangle(bi,3,0.2, Color.RED, 1.0F)
                    val outputfile = IOFile(pathToPreviewFile)
                    ImageIO.write(bi, "jpg", outputfile)
                }
                _previewSmall = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return _previewSmall!!
        }

    @Transient
    private var _labelPersonSmall: Label? = null

    val labelPersonSmall: Label
        get() {
            if (_labelPersonSmall == null) {
                _labelPersonSmall = Label()
                _labelPersonSmall!!.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
                _labelPersonSmall!!.graphic = personExt.previewSmall
                _labelPersonSmall!!.alignment = Pos.CENTER
            }
            return _labelPersonSmall!!
        }

    val isManualText = if (face.isManual) "✓" else ""

    @Transient
    private var _labelSmall: Label? = null

    val labelSmall: Label
        get() {
            if (_labelSmall == null) {
                _labelSmall = Label()
                _labelSmall!!.setPrefSize(Main.PREVIEW_FACE_W, Main.PREVIEW_FACE_H)
                _labelSmall!!.graphic = previewSmall
                _labelSmall!!.alignment = Pos.CENTER
            }
            return _labelSmall!!
        }

    fun resetPreviewSmall() {
        _previewSmall = null
    }
}