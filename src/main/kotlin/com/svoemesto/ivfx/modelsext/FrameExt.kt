package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.utils.ConvertToFxImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
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
    var previewSmall: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(if (IOFile(pathToSmall).exists()) pathToSmall else pathToStubSmall))))
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
    var previewMedium: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(if (IOFile(pathToMedium).exists()) pathToMedium else pathToStubMedium))))
            }
            return field
        }
    var labelMedium: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(720.0, 400.0)
                field!!.graphic = previewMedium
                field!!.alignment = Pos.CENTER
            }
            return field
        }
}