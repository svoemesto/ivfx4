package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.OverlayImage
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.Color
import javax.imageio.ImageIO
import java.io.File as IOFile

class PersonExt(val person: Person, val projectExt: ProjectExt) : Comparable<PersonExt> {
    override fun compareTo(other: PersonExt): Int {
        return this.person.compareTo(other.person)
    }
    val pathToSmall: String
        get() {
            if (person.fileIdForPreview != 0L) {
                if (person.frameNumberForPreview != 0 && person.faceNumberForPreview == 0) {
                    // берем картинку из кадра
                    val frameExt = FrameController.getFrameExt(person.fileIdForPreview, person.frameNumberForPreview, projectExt.project)
                    return frameExt.pathToSmall
                } else if (person.frameNumberForPreview != 0 && person.faceNumberForPreview != 0) {
                    // берем картинку из лица
                    val faceExt = FaceController.getFaceExt(person.fileIdForPreview, person.frameNumberForPreview, person.faceNumberForPreview, projectExt.project)
                    if (faceExt != null) {
                        return faceExt.pathToFaceFile
                    }
                }
            }
            return PersonExt::class.java.getResource("blank_person_small.jpg")!!.file.substring(1)
        }
    var previewSmall: ImageView? = null
        get() {
            if (field == null) {
                var bi = ImageIO.read(IOFile(pathToSmall))
                bi = OverlayImage.resizeImage(bi, 135, 75, Color.BLACK)
                bi = OverlayImage.setOverlayUnderlineText(bi, person.name)
                field = ImageView(ConvertToFxImage.convertToFxImage(bi))
            }
            return field
        }
    var labelSmall: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(135.0, 75.0)
                field!!.graphic = previewSmall
                field!!.alignment = Pos.CENTER
            }
            return field
        }
}