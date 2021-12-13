package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.controllers.TagController
import com.svoemesto.ivfx.enums.TagType
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FileExt
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView

class CreateShots(var fileExt: FileExt,
                  val table: TableView<FileExt>,
                  private val textLbl1: String,
                  private val numCurrentThread: Int,
                  private val countThreads: Int,
                  private var lbl1: Label, private var pb1: ProgressBar,
                  private var lbl2: Label, private var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        lbl1.isVisible = true
        lbl2.isVisible = true
        pb1.isVisible = true
        pb2.isVisible = true

        val mediaFile: String = fileExt.file.path
        val fps: Double = fileExt.fps
        val framesCount: Int = fileExt.framesCount

        val countBlocks = 1
        var currentBlock = 0

        // 1. Загружаем фреймы

        Platform.runLater {
            lbl1.text = textLbl1
            pb1.progress = 0.0
            lbl2.text = "Loading frames for ${fileExt.file.name}"
            pb2.progress = -1.0
        }
        val listFrames = FrameController.getListFrames(fileExt.file)

        // 2. Создаем планы
        currentBlock++
        var firstFrameNumber = 1
        var lastFrameNumber: Int
        var currentFrameNumber: Int
        var currentIFrame = 1
        var previousIFrame = 1
        ShotController.deleteAll(fileExt.file)
        TagController.deleteAllScenes(fileExt.file)
        val listShots: MutableList<Shot> = mutableListOf()

        for ((i, frame) in listFrames.withIndex()) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Creating shots for frames [$i/$framesCount]"
                pb2.progress = percentage2
            }

            if (frame.isIFrame) currentIFrame = frame.frameNumber
            currentFrameNumber = frame.frameNumber
            if (frame.isFinalFind || frame == listFrames.last()) {
                lastFrameNumber = currentFrameNumber - if (frame == listFrames.last()) 0 else 1
                val shot = Shot()
                shot.file = fileExt.file
                shot.firstFrameNumber = firstFrameNumber
                shot.lastFrameNumber = lastFrameNumber
                shot.nearestIFrame = previousIFrame
                listShots.add(shot)
                ShotController.save(shot)
                firstFrameNumber = currentFrameNumber
                previousIFrame = currentIFrame
            }
        }

//        val scene = TagController.create(fileExt.file::class.java.simpleName, fileExt.file.id,"",TagType.SCENE)
//        listShots.forEach { shot ->
//            TagController.create(scene::class.java.simpleName, scene.id)
//        }

        fileExt.hasCreatedShots = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }

}