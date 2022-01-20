package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.SceneController
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayUnderlineText
import javafx.event.EventHandler
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.TextInputDialog
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import java.io.File as IOFile


data class SceneExt(
    val scene: Scene,
    val fileExt: FileExt,
    var firstFrameExt: FrameExt,
    var lastFrameExt: FrameExt
): Comparable<SceneExt> {

    override fun compareTo(other: SceneExt): Int {
        return this.scene.firstFrameNumber - other.scene.firstFrameNumber
    }

    val shotsExt: MutableList<ShotExt>
        get() = fileExt.shotsExt.filter {it.shot.firstFrameNumber >= scene.firstFrameNumber && it.shot.lastFrameNumber <=scene.lastFrameNumber}.toMutableList()

    val personsExt: MutableList<PersonExt>
        get() {
            var list: MutableList<PersonExt> = mutableListOf()
            var map: MutableMap<Long, PersonExt> = mutableMapOf()
            shotsExt.forEach { currentShotExt ->
                map.putAll(currentShotExt.personsExt.map { Pair(it.person.id, it) })
            }
            list = map.values.toMutableList()
            list.sort()
            return list
        }

    val start: String get() = convertDurationToString(getDurationByFrameNumber(scene.firstFrameNumber - 1, fileExt.fps))
    val end: String get() = convertDurationToString(getDurationByFrameNumber(scene.lastFrameNumber, fileExt.fps))
    val duration: Int get() = getDurationByFrameNumber(scene.lastFrameNumber - scene.firstFrameNumber + 1, fileExt.fps)
    val sceneName: String get() = scene.name
    val sceneNameLabel: Label
        get() {
            val label = Label(sceneName)
            label.prefHeight = Control.USE_COMPUTED_SIZE
            label.isWrapText = true
            val contextMenu = ContextMenu()
            val menuItemRename = MenuItem("Rename scene")
            menuItemRename.onAction = EventHandler {
                val dialog = TextInputDialog(sceneName)
                dialog.title = "Rename scene"
                dialog.headerText = "Enter new scene name:"
                dialog.contentText = "Name:"
                val result: Optional<String> = dialog.showAndWait()
                result.ifPresent { name ->
                    label.text = name
                    scene.name = name
                    SceneController.save(scene)
                }
            }
            contextMenu.items.add(menuItemRename)

            label.contextMenu = contextMenu
            return label
        }
    var previewsFirst: Array<ImageView?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(firstFrameExt.pathToSmall).exists()) firstFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, start)
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

}