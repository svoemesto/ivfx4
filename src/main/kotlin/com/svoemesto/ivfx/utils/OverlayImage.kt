package com.svoemesto.ivfx.utils

import javafx.geometry.Pos
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

class OverlayImage {
    companion object {

        fun setOverlayFirstFrameFound(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "\u255A" + "\u2550" + "\u2550" + "\u2550" + "\u2550" + "\u255D"
            val textColor = Color.RED
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 24)
            val textPosition = Pos.TOP_CENTER
            val opaque = 1.0f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayLastFrameFound(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "\u2554" + "\u2550" + "\u2550" + "\u2550" + "\u2550" + "\u2557"
            val textColor = Color.RED
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 24)
            val textPosition = Pos.BOTTOM_CENTER
            val opaque = 1.0f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayFirstFrameManual(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "\u255A" + "\u2550" + "\u2550" + "\u2550" + "\u2550" + "\u255D"
            val textColor = Color.GREEN
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 24)
            val textPosition = Pos.TOP_CENTER
            val opaque = 1.0f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayLastFrameManual(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "\u2554" + "\u2550" + "\u2550" + "\u2550" + "\u2550" + "\u2557"
            val textColor = Color.GREEN
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 24)
            val textPosition = Pos.BOTTOM_CENTER
            val opaque = 1.0f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun cancelOverlayFirstFrameManual(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "["
            val textColor = Color.ORANGE
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 48)
            val textPosition = Pos.CENTER_LEFT
            val opaque = 0.2f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun cancelOverlayLastFrameManual(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "]"
            val textColor = Color.ORANGE
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 48)
            val textPosition = Pos.CENTER_RIGHT
            val opaque = 0.2f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayIFrame(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay = "I"
            val textColor = Color.BLUE
            val textFont = Font(Font.MONOSPACED, Font.BOLD, 26)
            val textPosition = Pos.CENTER
            val opaque = 0.9f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayUnderlinePlate(sourceImage: BufferedImage): BufferedImage {
            val textToOverlay =
                "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"
            val textColor = Color.BLACK
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 14)
            val textPosition = Pos.BOTTOM_CENTER
            val opaque = 1.0f
            return setTextOverlay(sourceImage, textToOverlay, textColor, textFont, textPosition, opaque)
        }

        fun setOverlayIsBodyScene(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.ORANGE
            return setOverlayRectangle(sourceImage, 0, 0, 10, imageH, color, opaque)
        }

        fun setOverlayIsStartScene(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.ORANGE
            return setOverlayRectangle(sourceImage, 10, 0, 20, 10, color, opaque)
        }

        fun setOverlayIsEndScene(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.ORANGE
            return setOverlayRectangle(sourceImage, 10, imageH - 10, 20, 10, color, opaque)
        }

        fun setOverlayIsBodyEvent(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.GREEN
            return setOverlayRectangle(sourceImage, imageW - 11, 0, 10, imageH, color, opaque)
        }

        fun setOverlayIsStartEvent(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.GREEN
            return setOverlayRectangle(sourceImage, imageW - 31, 0, 20, 10, color, opaque)
        }

        fun setOverlayIsEndEvent(sourceImage: BufferedImage): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val opaque = 1.0f
            val color = Color.GREEN
            return setOverlayRectangle(sourceImage, imageW - 31, imageH - 10, 20, 10, color, opaque)
        }


        fun setOverlayRectangle(
            sourceImage: BufferedImage,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            color: Color?,
            opaque: Float
        ): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val imageType = BufferedImage.TYPE_INT_ARGB
            val resultImage = BufferedImage(imageW, imageH, imageType)
            val graphics2D = resultImage.graphics as Graphics2D
            graphics2D.drawImage(sourceImage, 0, 0, null)
            val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opaque)
            graphics2D.composite = alphaChannel
            graphics2D.color = color
            graphics2D.fillRect(x, y, width, height)
            graphics2D.dispose()
            return resultImage
        }

        fun setOverlayTriangle(
            sourceImage: BufferedImage,
            corner: Int,
            percentOfLowerSize: Double,
            color: Color?,
            opaque: Float
        ): BufferedImage {
            // corner: 1 - верх лево, 2 - верх право, 3 - низ право, 4 - низ лево
            // percentOfLowerSize: 0-1 в частях от наиболее короткой стороны
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val imageType = BufferedImage.TYPE_INT_RGB
            val triangleSide =
                if (imageW > imageH) (imageH * percentOfLowerSize).toInt() else (imageW * percentOfLowerSize).toInt()
            val resultImage = BufferedImage(imageW, imageH, imageType)
            val graphics2D = resultImage.graphics as Graphics2D
            graphics2D.drawImage(sourceImage, 0, 0, null)
            val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opaque)
            graphics2D.composite = alphaChannel
            graphics2D.color = color
            graphics2D.paint = color
            val myPath: Path2D = Path2D.Double()
            var x1 = 0.0
            var y1 = 0.0
            var x2 = 0.0
            var y2 = 0.0
            var x3 = 0.0
            var y3 = 0.0
            when (corner) {
                1 -> {
                    x1 = triangleSide.toDouble()
                    y1 = 0.0
                    x2 = 0.0
                    y2 = triangleSide.toDouble()
                    x3 = 0.0
                    y3 = 0.0
                }
                2 -> {
                    x1 = (imageW - triangleSide).toDouble()
                    y1 = 0.0
                    x2 = imageW.toDouble()
                    y2 = triangleSide.toDouble()
                    x3 = imageW.toDouble()
                    y3 = 0.0
                }
                3 -> {
                    x1 = imageW.toDouble()
                    y1 = (imageH - triangleSide).toDouble()
                    x2 = (imageW - triangleSide).toDouble()
                    y2 = imageH.toDouble()
                    x3 = imageW.toDouble()
                    y3 = imageH.toDouble()
                }
                4 -> {
                    x1 = triangleSide.toDouble()
                    y1 = imageH.toDouble()
                    x2 = 0.0
                    y2 = (imageH - triangleSide).toDouble()
                    x3 = 0.0
                    y3 = imageH.toDouble()
                }
                else -> {}
            }
            myPath.moveTo(x1, y1)
            myPath.lineTo(x2, y2)
            myPath.lineTo(x3, y3)
            myPath.closePath()
            graphics2D.fill(myPath)
            graphics2D.dispose()
            return resultImage
        }

        fun setOverlayUnderlineText(sourceImage: BufferedImage, text: String): BufferedImage {
            val textColor = Color.YELLOW
            val textFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)
            val textPosition = Pos.BOTTOM_CENTER
            val opaque = 1.0f
            return setTextOverlay(
                setOverlayUnderlinePlate(sourceImage),
                text,
                textColor,
                textFont,
                textPosition,
                opaque
            )
        }

        fun setTextOverlay(
            sourceImage: BufferedImage,
            textToOverlay: String?,
            textColor: Color?,
            textFont: Font?,
            textPosition: Pos?,
            opaque: Float
        ): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val imageType = BufferedImage.TYPE_INT_ARGB
            val resultImage = BufferedImage(imageW, imageH, imageType)
            val graphics2D = resultImage.graphics as Graphics2D
            graphics2D.drawImage(sourceImage, 0, 0, null)
            val alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opaque)
            graphics2D.composite = alphaChannel
            graphics2D.color = textColor
            graphics2D.font = textFont
            val fontMetrics = graphics2D.fontMetrics
            val rect = fontMetrics.getStringBounds(textToOverlay, graphics2D)
            val rectW = rect.width.toInt()
            val rectH = rect.height.toInt()
            var centerX = 0
            var centerY = 0
            when (textPosition) {
                Pos.CENTER -> {
                    centerX = (imageW - rectW) / 2
                    centerY = imageH / 2 + rectH / 5
                }
                Pos.CENTER_LEFT -> {
                    centerX = 0
                    centerY = imageH / 2 + rectH / 5
                }
                Pos.CENTER_RIGHT -> {
                    centerX = imageW - rectW
                    centerY = imageH / 2 + rectH / 5
                }
                Pos.TOP_CENTER -> {
                    centerX = (imageW - rectW) / 2
                    centerY = 0 + rectH / 2
                }
                Pos.BOTTOM_CENTER -> {
                    centerX = (imageW - rectW) / 2
                    centerY = imageH - rectH / 8 - 2
                }
            }
            graphics2D.drawString(textToOverlay, centerX, centerY)
            graphics2D.dispose()
            return resultImage
        }

        fun resizeImage(sourceImage: BufferedImage, resizedW: Int, resizedH: Int, bgColor: Color? = null): BufferedImage {
            val imageW = sourceImage.width
            val imageH = sourceImage.height
            val imageType = BufferedImage.TYPE_INT_ARGB
            val scaleCoeff = (resizedW.toDouble() / imageW).coerceAtMost(resizedH.toDouble() / imageH)
            val resultImage = BufferedImage(resizedW, resizedH, imageType)
            var afterResize = BufferedImage(resizedW, resizedH, imageType)
            val graphics2D = resultImage.graphics as Graphics2D

            if (bgColor != null) {
                graphics2D.color = bgColor
                graphics2D.fillRect ( 0, 0, resizedW, resizedH)
            }

            val at = AffineTransform()
            at.scale(scaleCoeff, scaleCoeff)
            val scaleOp = AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
            afterResize = scaleOp.filter(sourceImage, afterResize)
            val x = (resizedW - imageW * scaleCoeff).toInt() / 2
            val y = (resizedH - imageH * scaleCoeff).toInt() / 2

            graphics2D.drawImage(afterResize, x, y, null)
            graphics2D.dispose()
            return resultImage
        }

        fun extractRegion(sourceImage: BufferedImage,
                          startX: Int,
                          startY: Int,
                          endX: Int,
                          endY: Int,
                          resultedRegionW: Int,
                          resultedRegionH: Int,
                          expandFactor: Double = 1.0,
                          cropping: Boolean = true,
                          bgColor: Color = Color.BLACK): BufferedImage {

            val imageW = sourceImage.width
            val imageH = sourceImage.height
            var sourceRegionStartX = startX
            var sourceRegionStartY = startY
            var sourceRegionEndX = endX
            var sourceRegionEndY = endY

            var sourceRegionW = sourceRegionEndX - sourceRegionStartX
            var sourceRegionH = sourceRegionEndY - sourceRegionStartY

            val expandedRegionW = (sourceRegionW * expandFactor).toInt()
            val expandedRegionH = (sourceRegionH * expandFactor).toInt()
            sourceRegionStartX += (sourceRegionW - expandedRegionW) / 2
            sourceRegionEndX -= (sourceRegionW - expandedRegionW) / 2
            sourceRegionStartY += (sourceRegionH - expandedRegionH) / 2
            sourceRegionEndY -= (sourceRegionH - expandedRegionH) / 2
            sourceRegionW = expandedRegionW
            sourceRegionH = expandedRegionH

            val sourceRegionAspect = sourceRegionW / sourceRegionH.toDouble()
            val resultedRegionAspect = resultedRegionW / resultedRegionH.toDouble()

            var workRegionStartX: Int = 0
            var workRegionStartY: Int = 0
            var workRegionEndX: Int = 0
            var workRegionEndY: Int = 0
            var workRegionW: Int = 0
            var workRegionH: Int = 0
            var workRegionDelta: Int = 0
            var workRegionScale: Double = 1.0

//            val imageType = BufferedImage.TYPE_INT_ARGB
            val imageType = BufferedImage.TYPE_INT_RGB
            val resultImage = BufferedImage(resultedRegionW, resultedRegionH, imageType)
            var afterResize = BufferedImage(resultedRegionW, resultedRegionH, imageType)
            val graphics2DresultedImage = resultImage.graphics as Graphics2D
            if (bgColor != null) {
                graphics2DresultedImage.color = bgColor
                graphics2DresultedImage.fillRect ( 0, 0, resultedRegionW, resultedRegionH)
            }

            workRegionStartX = sourceRegionStartX
            workRegionEndX = sourceRegionEndX
            workRegionStartY = sourceRegionStartY
            workRegionEndY = sourceRegionEndY
            if (cropping) {
                if (sourceRegionAspect > resultedRegionAspect) { // исходный регион выше - отбрасываем края
                    workRegionDelta = ((sourceRegionW - resultedRegionW * sourceRegionH / resultedRegionH.toDouble()) / 2).toInt()
                    workRegionStartX = workRegionStartX - workRegionDelta
                    workRegionEndX = workRegionEndX + workRegionDelta
                } else if (sourceRegionAspect < resultedRegionAspect) { // исходный регион ниже - отбрасываем верх и низ
                    workRegionDelta = ((sourceRegionH - resultedRegionH * sourceRegionW / resultedRegionW.toDouble()) / 2).toInt()
                    workRegionStartY = workRegionStartY - workRegionDelta
                    workRegionEndY = workRegionEndY + workRegionDelta
                }
            } else {
                if (sourceRegionAspect > resultedRegionAspect) { // исходный регион выше - забираем сверху и снизу
                    workRegionDelta = ((sourceRegionH - resultedRegionH * sourceRegionW / resultedRegionW.toDouble()) / 2).toInt()
                    workRegionStartY = workRegionStartY + workRegionDelta
                    workRegionEndY = workRegionEndY - workRegionDelta

                } else if (sourceRegionAspect < resultedRegionAspect) { // исходный регион ниже - забираем с боков
                    workRegionDelta = ((sourceRegionW - resultedRegionW * sourceRegionH / resultedRegionH.toDouble()) / 2).toInt()
                    workRegionStartX = workRegionStartX + workRegionDelta
                    workRegionEndX = workRegionEndX - workRegionDelta
                }

            }
            workRegionW = workRegionEndX - workRegionStartX
            workRegionH = workRegionEndY - workRegionStartY

            val expandSourceImageToLeft = if (workRegionStartX >= 0) 0 else workRegionStartX * (-1)
            val expandSourceImageToRight = if (workRegionEndX <= imageW) 0 else workRegionEndX - imageW
            val expandSourceImageToUp = if (workRegionStartY >= 0) 0 else workRegionStartY * (-1)
            val expandSourceImageToDown = if (workRegionEndY <= imageH) 0 else workRegionEndY - imageH

            val expandedSourceImageW = imageW + expandSourceImageToLeft + expandSourceImageToRight
            val expandedSourceImageH = imageH + expandSourceImageToUp + expandSourceImageToDown

            val expandedSourceImage = BufferedImage(expandedSourceImageW, expandedSourceImageH, imageType)
            val graphics2DexpandedSourceImage = expandedSourceImage.graphics as Graphics2D
            graphics2DexpandedSourceImage.color = bgColor
            graphics2DexpandedSourceImage.fillRect ( 0, 0, expandedSourceImageW, expandedSourceImageH)
            graphics2DexpandedSourceImage.drawImage(sourceImage, expandSourceImageToLeft, expandSourceImageToUp, null)

            workRegionStartX += expandSourceImageToLeft
            workRegionEndX += expandSourceImageToLeft
            workRegionStartY += expandSourceImageToUp
            workRegionEndY += expandSourceImageToUp

            val subImage = expandedSourceImage.getSubimage(workRegionStartX, workRegionStartY, workRegionW, workRegionH)

            val at = AffineTransform()
            val scaleCoeff = (resultedRegionW.toDouble() / subImage.width).coerceAtMost(resultedRegionH.toDouble() / subImage.height)
            at.scale(scaleCoeff, scaleCoeff)
            val scaleOp = AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
            afterResize = scaleOp.filter(subImage, afterResize)
            val x = (resultedRegionW - subImage.width * scaleCoeff).toInt() / 2
            val y = (resultedRegionH - subImage.height * scaleCoeff).toInt() / 2

            graphics2DresultedImage.drawImage(afterResize, x, y, null)

            graphics2DresultedImage.dispose()
            graphics2DexpandedSourceImage.dispose()

            return resultImage
        }

    }
}