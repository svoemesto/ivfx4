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

class FaceExtJson() {

    var fileId: Long = 0
    var frameNumber: Int = 0
    var faceNumberInFrame: Int = 0
    var pathToFrameFile: String = ""
    var pathToFaceFile: String = ""
    var pathToPreviewFile: String = ""
    var personId: Long = 0
    var personRecognizedName: String = ""
    var personRecognizedId: Long = 0
    var recognizeProbability: Double = 0.0
    var startX: Int = 0
    var startY: Int = 0
    var endX: Int = 0
    var endY: Int = 0
    var isConfirmed: Boolean = false
    var vector: DoubleArray = doubleArrayOf(0.0)

}