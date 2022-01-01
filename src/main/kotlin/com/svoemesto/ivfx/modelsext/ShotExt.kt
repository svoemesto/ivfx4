package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.utils.ConvertToFxImage
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.convertDurationToString
import com.svoemesto.ivfx.utils.IvfxFFmpegUtils.Companion.getDurationByFrameNumber
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsBodyEvent
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsBodyScene
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsEndEvent
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsEndScene
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsStartEvent
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayIsStartScene
import com.svoemesto.ivfx.utils.OverlayImage.Companion.setOverlayUnderlineText
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

data class ShotExt(
    val shot: Shot,
    val fileExt: FileExt,
    var firstFrameExt: FrameExt,
    var lastFrameExt: FrameExt
): Comparable<ShotExt> {

    override fun compareTo(other: ShotExt): Int {
        return this.shot.firstFrameNumber - other.shot.firstFrameNumber
    }

    val start: String get() = convertDurationToString(getDurationByFrameNumber(shot.firstFrameNumber - 1, fileExt.fps))
    val end: String get() = convertDurationToString(getDurationByFrameNumber(shot.lastFrameNumber, fileExt.fps))
    val duration: Int get() = getDurationByFrameNumber(shot.lastFrameNumber - shot.firstFrameNumber + 1, fileExt.fps)
    val sceneExt: SceneExt? get() = fileExt.scenesExt.firstOrNull { shot.firstFrameNumber >= it.scene.firstFrameNumber && shot.lastFrameNumber <= it.scene.lastFrameNumber }
    val eventExt: EventExt? get() = fileExt.eventsExt.firstOrNull { shot.firstFrameNumber >= it.event.firstFrameNumber && shot.lastFrameNumber <= it.event.lastFrameNumber }

    val personsExt: MutableList<PersonExt>
        get() {
            val list: MutableList<PersonExt> = mutableListOf()
            val sourceIterable = Main.personRepo.findByShotId(shot.id)
            for (person in sourceIterable) {
                person.project = fileExt.projectExt.project
                list.add(PersonExt(person, fileExt.projectExt))
            }
            list.sort()
            return list
        }

    var previewsFirst: Array<ImageView?>? = null
        get() {
            if (field == null) {
                field = arrayOfNulls(3)
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(firstFrameExt.pathToSmall).exists()) firstFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, start)
                    if (sceneExt != null) {
                        if (shot.firstFrameNumber >= sceneExt!!.scene.firstFrameNumber && shot.lastFrameNumber <= sceneExt!!.scene.lastFrameNumber) bi = setOverlayIsBodyScene(bi)
                        if (shot.firstFrameNumber == sceneExt!!.scene.firstFrameNumber) bi = setOverlayIsStartScene(bi)
                        if (shot.lastFrameNumber == sceneExt!!.scene.lastFrameNumber) bi = setOverlayIsEndScene(bi)
                    }
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
                    if (eventExt != null) {
                        if (shot.firstFrameNumber >= eventExt!!.event.firstFrameNumber && shot.lastFrameNumber <= eventExt!!.event.lastFrameNumber) bi = setOverlayIsBodyEvent(bi)
                        if (shot.firstFrameNumber == eventExt!!.event.firstFrameNumber) bi = setOverlayIsStartEvent(bi)
                        if (shot.lastFrameNumber == eventExt!!.event.lastFrameNumber) bi = setOverlayIsEndEvent(bi)
                    }
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
    var previewType: ImageView? = null
        get() {
            if (field == null) {
                field = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shot.typePerson.pathToPicture))))
            }
            return field
        }
    var labelType: Label? = null
        get() {
            if (field == null) {
                field = Label()
                field!!.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                field!!.graphic = previewType
                field!!.alignment = Pos.CENTER

//                val contextMenuShotType = ContextMenu()
//                ShotTypePerson.values().forEach { shotTypePerson ->
//                    val imageView = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shotTypePerson.pathToPicture))))
//                    val contextMenuShotTypeItem = MenuItem(null, imageView)
//                    contextMenuShotTypeItem.onAction = EventHandler { e: ActionEvent? ->
//                        this.shot.typePerson = shotTypePerson
//                        ShotController.save(shot)
//                        this.previewType = null
//                        this.labelType = null
//                        this.labelType
//                    }
//                    contextMenuShotType.items.add(contextMenuShotTypeItem)
//                }
//                field!!.contextMenu = contextMenuShotType

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