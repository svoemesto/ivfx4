package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.EventController
import com.svoemesto.ivfx.controllers.SceneController
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayUnderlineText
import javafx.event.EventHandler
import javafx.scene.control.Button
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

data class EventExt(
    val event: Event,
    val fileExt: FileExt,
    var firstFrameExt: FrameExt,
    var lastFrameExt: FrameExt
): Comparable<EventExt> {

    override fun compareTo(other: EventExt): Int {
        return this.event.firstFrameNumber - other.event.firstFrameNumber
    }

    val shotsExt: MutableList<ShotExt>
        get() = fileExt.shotsExt.filter {it.shot.firstFrameNumber >= event.firstFrameNumber && it.shot.lastFrameNumber <=event.lastFrameNumber}.toMutableList()

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

    val start: String get() = convertDurationToString(getDurationByFrameNumber(event.firstFrameNumber - 1, fileExt.fps))
    val end: String get() = convertDurationToString(getDurationByFrameNumber(event.lastFrameNumber, fileExt.fps))
    val duration: Int get() = getDurationByFrameNumber(event.lastFrameNumber - event.firstFrameNumber + 1, fileExt.fps)
    val eventName: String get() = event.name
    val eventNameLabel: Label
        get() {
            val label = Label(eventName)
            label.prefHeight = Control.USE_COMPUTED_SIZE
            label.isWrapText = true
            val contextMenu = ContextMenu()
            val menuItemRename = MenuItem("Rename event")
            menuItemRename.onAction = EventHandler {
                val dialog = TextInputDialog(eventName)
                dialog.title = "Rename event"
                dialog.headerText = "Enter new event name:"
                dialog.contentText = "Name:"
                val result: Optional<String> = dialog.showAndWait()
                result.ifPresent { name ->
                    label.text = name
                    event.name = name
                    EventController.save(event)
                }
            }
            contextMenu.items.add(menuItemRename)

            label.contextMenu = contextMenu
            return label
        }
    private var _previewsFirst: Array<ImageView>? = null
    val previewsFirst: Array<ImageView>
        get() {
            if (_previewsFirst == null) {
                val list = mutableListOf<ImageView>()
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(firstFrameExt.pathToSmall).exists()) firstFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, start)
                    list.add(i, ImageView(ConvertToFxImage.convertToFxImage(bi)))
                }
                _previewsFirst = list.toTypedArray()
            }
            return _previewsFirst!!
        }
    private var _previewsLast: Array<ImageView>? = null
    val previewsLast: Array<ImageView>
        get() {
            if (_previewsLast == null) {
                val list = mutableListOf<ImageView>()
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(lastFrameExt.pathToSmall).exists()) lastFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, end)
                    list.add(i, ImageView(ConvertToFxImage.convertToFxImage(bi)))
                }
                _previewsLast = list.toTypedArray()
            }
            return _previewsLast!!
        }

    private var _labelsFirst: Array<Label>? = null
    val labelsFirst: Array<Label>
        get() {
            if (_labelsFirst == null) {
                val list = mutableListOf<Label>()
                for (i in 0..2) {
                    val label = Label()
                    label.setMinSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.setMaxSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.setPrefSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.graphic = previewsFirst[i]
                    label.contentDisplay = ContentDisplay.TOP
                    list.add(label)
                }
                _labelsFirst = list.toTypedArray()
            }
            return _labelsFirst!!
        }
    private var _labelsLast: Array<Label>? = null
    val labelsLast: Array<Label>
        get() {
            if (_labelsLast == null) {
                val list = mutableListOf<Label>()
                for (i in 0..2) {
                    val label = Label()
                    label.setMinSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.setMaxSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.setPrefSize(Main.PREVIEW_FRAME_W,Main.PREVIEW_FRAME_H)
                    label.graphic = previewsLast[i]
                    label.contentDisplay = ContentDisplay.TOP
                    list.add(label)
                }
                _labelsLast = list.toTypedArray()
            }
            return _labelsLast!!
        }

    val labelFirst1: Label get() = labelsFirst[0]
    val labelFirst2: Label get() = labelsFirst[1]
    val labelFirst3: Label get() = labelsFirst[2]
    val labelLast1: Label get() = labelsLast[0]
    val labelLast2: Label get() = labelsLast[1]
    val labelLast3: Label get() = labelsLast[2]
    var buttonGetType: Button = Button()

    fun resetPreview() {
        _previewsFirst = null
        _previewsLast = null
    }

}