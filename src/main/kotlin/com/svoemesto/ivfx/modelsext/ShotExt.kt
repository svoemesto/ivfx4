package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Shot
import javafx.scene.control.Label
import javafx.scene.image.ImageView

data class ShotExt(val shot: Shot,
                   val fileExt: FileExt
                   ) {
    val imageViewFirst = arrayOfNulls<ImageView>(3)
    val imageViewLast = arrayOfNulls<ImageView>(3)
    val labelFirst = arrayOfNulls<Label>(3)
    val labelLast = arrayOfNulls<Label>(3)
}