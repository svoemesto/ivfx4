package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.enums.VideoContainers
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
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File as IOFile

data class ShotExt(
    val shot: Shot,
    private var _fileExt: FileExt? = null,
    private var _firstFrameExt: FrameExt? = null,
    private var _lastFrameExt: FrameExt? = null
): Comparable<ShotExt> {

    override fun compareTo(other: ShotExt): Int {
        return (this.shot.file.order - other.shot.file.order) * 1000000 + (this.shot.firstFrameNumber - other.shot.firstFrameNumber)
    }

//    private var _fileExt: FileExt? = null
//    private var _firstFrameExt: FrameExt? = null
//    private var _lastFrameExt: FrameExt? = null

    val fileExt: FileExt
        get() {
            if (_fileExt == null) {
                _fileExt = FileExt(shot.file, ProjectExt(shot.file.project))
            }
            return _fileExt!!
        }

    var firstFrameExt: FrameExt
        get() {
            if (_firstFrameExt == null) {
                _firstFrameExt = FrameController.getFrameExt(fileExt, shot.firstFrameNumber)
            }
            return _firstFrameExt!!
        }
        set(value) {_firstFrameExt = value}

    var lastFrameExt: FrameExt
        get() {
            if (_lastFrameExt == null) {
                _lastFrameExt = FrameController.getFrameExt(fileExt, shot.lastFrameNumber)
            }
            return _lastFrameExt!!
        }
        set(value) {_lastFrameExt = value}

    val start: String get() = convertDurationToString(getDurationByFrameNumber(shot.firstFrameNumber - 1, fileExt.fps))
    val end: String get() = convertDurationToString(getDurationByFrameNumber(shot.lastFrameNumber, fileExt.fps))
    val duration: Int get() = getDurationByFrameNumber(shot.lastFrameNumber - shot.firstFrameNumber + 1, fileExt.fps)
    val sceneExt: SceneExt? get() = fileExt.scenesExt.firstOrNull { shot.firstFrameNumber >= it.scene.firstFrameNumber && shot.lastFrameNumber <= it.scene.lastFrameNumber }
    val eventsExt: List<EventExt> get() = fileExt.eventsExt.filter { shot.firstFrameNumber >= it.event.firstFrameNumber && shot.lastFrameNumber <= it.event.lastFrameNumber }
    val filenameWithoutExt: String get() = "${fileExt.file.shortName}_shot_[${start.replace(":",".")}-${end.replace(":",".")}]-(${shot.firstFrameNumber}-${shot.lastFrameNumber})"
    val pathToCompressedWithAudio: String get() = "${fileExt.folderShotsCompressedWithAudio}${IOFile.separator}${filenameWithoutExt}" +
            ".${VideoContainers.valueOf(fileExt.projectExt.project.container).extention}"
    val pathToLosslessWithAudio: String get() = "${fileExt.folderShotsLosslessWithAudio}${IOFile.separator}${filenameWithoutExt}" +
            "_audioON.mxf"
    val pathToLosslessWithoutAudio: String get() = "${fileExt.folderShotsLosslessWithoutAudio}${IOFile.separator}${filenameWithoutExt}" +
            "_audioOFF.mxf"
    val hasCompressedWithAudio: Boolean get() = IOFile(pathToCompressedWithAudio).exists()
    val hasLosslessWithAudio: Boolean get() = IOFile(pathToLosslessWithAudio).exists()
    val hasLosslessWithoutAudio: Boolean get() = IOFile(pathToLosslessWithoutAudio).exists()
    val fileName: String get() = fileExt.file.name

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

    private var _previewsFirst: Array<ImageView>? = null
    val previewsFirst: Array<ImageView>
        get() {
            if (_previewsFirst == null) {
                val list = mutableListOf<ImageView>()
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(firstFrameExt.pathToSmall).exists()) firstFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, start)
                    if (sceneExt != null) {
                        if (shot.firstFrameNumber >= sceneExt!!.scene.firstFrameNumber && shot.lastFrameNumber <= sceneExt!!.scene.lastFrameNumber) bi = setOverlayIsBodyScene(bi)
                        if (shot.firstFrameNumber == sceneExt!!.scene.firstFrameNumber) bi = setOverlayIsStartScene(bi)
                        if (shot.lastFrameNumber == sceneExt!!.scene.lastFrameNumber) bi = setOverlayIsEndScene(bi)
                    }
                    list.add(i, ImageView(ConvertToFxImage.convertToFxImage(bi)))
                }
                _previewsFirst = list.toTypedArray()
            }
            return  _previewsFirst!!
        }
    private var _previewsLast: Array<ImageView>? = null
    val previewsLast: Array<ImageView>
        get() {
            if (_previewsLast == null) {
                val list = mutableListOf<ImageView>()
                for (i in 0..2) {
                    var bi: BufferedImage = ImageIO.read(IOFile(if (IOFile(lastFrameExt.pathToSmall).exists()) lastFrameExt.pathToSmall else FrameExt.pathToStubSmall))
                    bi = setOverlayUnderlineText(bi, end)
                    eventsExt.forEach { eventExt ->
                        if (shot.firstFrameNumber >= eventExt.event.firstFrameNumber && shot.lastFrameNumber <= eventExt.event.lastFrameNumber) bi = setOverlayIsBodyEvent(bi)
                        if (shot.firstFrameNumber == eventExt.event.firstFrameNumber) bi = setOverlayIsStartEvent(bi)
                        if (shot.lastFrameNumber == eventExt.event.lastFrameNumber) bi = setOverlayIsEndEvent(bi)
                    }
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
    private var _previewType: ImageView? = null
    val previewType: ImageView
        get() {
            if (_previewType == null) {
                _previewType = ImageView(ConvertToFxImage.convertToFxImage(ImageIO.read(IOFile(shot.typePerson.pathToPicture))))
            }
            return _previewType!!
        }
    private var _labelType: Label? = null
    val labelType: Label
        get() {
            if (_labelType == null) {
                _labelType = Label()
                _labelType!!.setPrefSize(Main.PREVIEW_FRAME_W, Main.PREVIEW_FRAME_H)
                _labelType!!.graphic = previewType
                _labelType!!.alignment = Pos.CENTER
            }
            return _labelType!!
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
        _previewType = null
    }

}