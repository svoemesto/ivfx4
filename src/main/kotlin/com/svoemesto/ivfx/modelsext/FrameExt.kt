package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.utils.ConvertToFxImage
import javafx.scene.control.ContentDisplay
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

    var preview: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(if (IOFile(pathToSmall).exists()) pathToSmall else pathToStubSmall))))
            }
            return field
        }
    var label: Label? = null
        get() {
            if (field == null) {
                field = Label(frame.frameNumber.toString())
                field!!.prefWidth = 135.0
                field!!.graphic = preview
                field!!.contentDisplay = ContentDisplay.TOP
            }
            return field
        }
}