package com.svoemesto.ivfx.utils

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import java.awt.image.BufferedImage

class ConvertToFxImage {

    companion object {
        fun convertToFxImage(image: BufferedImage?): Image? {
            var wr: WritableImage? = null
            if (image != null) {
                wr = WritableImage(image.width, image.height)
                val pw = wr.pixelWriter
                for (x in 0 until image.width) {
                    for (y in 0 until image.height) {
                        pw.setArgb(x, y, image.getRGB(x, y))
                    }
                }
            }
            return ImageView(wr).image
        }

        fun getClone(image: Image?): Image? {
            var wr: WritableImage? = null
            if (image != null) {
                wr = WritableImage(image.width.toInt(), image.height.toInt())
                val pw = wr.pixelWriter
                var x = 0
                while (x < image.width) {
                    var y = 0
                    while (y < image.height) {
                        pw.setArgb(x, y, image.pixelReader.getArgb(x, y))
                        y++
                    }
                    x++
                }
            }
            return ImageView(wr).image
        }

    }
}