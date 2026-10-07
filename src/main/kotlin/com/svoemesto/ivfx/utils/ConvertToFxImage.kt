package com.svoemesto.ivfx.utils

import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import java.awt.image.BufferedImage

class ConvertToFxImage {

    companion object {

        /**
         * Переносит картинку из AWT в JavaFX.
         *
         * Раньше пиксели переносились по одному:
         *   for (x ...) for (y ...) pw.setArgb(x, y, image.getRGB(x, y))
         * Для кадра 720x400 это 288 тысяч вызовов, для полного 1920x1080 — больше
         * двух миллионов, и всё это в потоке интерфейса. При быстром листании
         * страниц превью очередь таких переносов не успевала обслуживаться, и
         * приложение подвисало.
         *
         * Теперь картинка целиком читается в массив int одной операцией
         * `BufferedImage.getRGB`, а затем кладётся в WritableImage одним вызовом
         * `PixelWriter.setPixels`. Формат `getIntArgbInstance` — без
         * предварительного умножения, ровно как отдаёт `getRGB`.
         *
         * @param image исходная картинка
         * @return картинка JavaFX либо null, если исходной нет
         */
        fun convertToFxImage(image: BufferedImage?): Image? {
            if (image == null) {
                return null
            }
            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getRGB(0, 0, width, height, pixels, 0, width)
            val writableImage = WritableImage(width, height)
            writableImage.pixelWriter.setPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width
            )
            return writableImage
        }

        /**
         * Копия картинки JavaFX.
         *
         * Раньше тоже копировался по одному пикселю, тем же вызовом `getArgb`.
         * Теперь читается целиком в массив и кладётся обратно одним вызовом.
         *
         * @param image исходная картинка
         * @return копия картинки либо null, если исходной нет
         */
        fun getClone(image: Image?): Image? {
            if (image == null) {
                return null
            }
            val width = image.width.toInt()
            val height = image.height.toInt()
            val pixels = IntArray(width * height)
            image.pixelReader.getPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width
            )
            val writableImage = WritableImage(width, height)
            writableImage.pixelWriter.setPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width
            )
            return writableImage
        }

    }
}
