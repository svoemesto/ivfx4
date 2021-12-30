package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayUnderlineText
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

data class SceneExt(
    val scene: Scene,
    val fileExt: FileExt,
    var firstFrameExt: FrameExt,
    var lastFrameExt: FrameExt
): Comparable<SceneExt> {
    val start: String get() = convertDurationToString(getDurationByFrameNumber(scene.firstFrameNumber - 1, fileExt.fps))
    val end: String get() = convertDurationToString(getDurationByFrameNumber(scene.lastFrameNumber, fileExt.fps))
    val duration: Int get() = getDurationByFrameNumber(scene.lastFrameNumber - scene.firstFrameNumber + 1, fileExt.fps)
    var previewsFirst: Array<ImageView?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(firstFrameExt.pathToSmall).exists()) firstFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, start)
//                    if (shot.isBodyScene) bi = setOverlayIsBodyScene(bi)
//                    if (shot.isStartScene) bi = setOverlayIsStartScene(bi)
//                    if (shot.isEndScene) bi = setOverlayIsEndScene(bi)
                    field!![i] = ImageView(ConvertToFxImage.convertToFxImage(bi))
                }
            }
            return field
        }
    var previewsLast: Array<ImageView?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(lastFrameExt.pathToSmall).exists()) lastFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, end)
//                    if (shot.isBodyEvent) bi = setOverlayIsBodyEvent(bi)
//                    if (shot.isStartEvent) bi = setOverlayIsStartEvent(bi)
//                    if (shot.isEndEvent) bi = setOverlayIsEndEvent(bi)
                    field!![i] = ImageView(ConvertToFxImage.convertToFxImage(bi))
                }
            }
            return field
        }
    var labelsFirst: Array<Label?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    field!![i] = Label()
                    field!![i]?.setMinSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.setMaxSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.setPrefSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.graphic = previewsFirst?.get(i)
                    field!![i]?.contentDisplay = ContentDisplay.TOP
                }
            }
            return field
        }
    var labelsLast: Array<Label?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    field!![i] = Label()
                    field!![i]?.setMinSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.setMaxSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.setPrefSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    field!![i]?.graphic = previewsLast?.get(i)
                    field!![i]?.contentDisplay = ContentDisplay.TOP
                }
            }
            return field
        }

    val labelFirst1: Label? get() = labelsFirst?.get(0)
    val labelFirst2: Label? get() = labelsFirst?.get(1)
    val labelFirst3: Label? get() = labelsFirst?.get(2)
    val labelLast1: Label? get() = labelsLast?.get(0)
    val labelLast2: Label? get() = labelsLast?.get(1)
    val labelLast3: Label? get() = labelsLast?.get(2)
    var buttonGetType: Button = Button()

    override fun compareTo(other: SceneExt): Int {
        return this.scene.firstFrameNumber - other.scene.firstFrameNumber
    }
}