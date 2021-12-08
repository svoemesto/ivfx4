package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Frame
import javafx.scene.control.Label
import javafx.scene.image.ImageView

data class FrameExt(val frame: Frame,
               val fileExt: FileExt,
               val pathToSmall: String,
               val pathToMedium: String,
               val pathToFull: String
               ) {
    var preview: ImageView? = null
    var label: Label? = null
}