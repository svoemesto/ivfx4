package com.svoemesto.ivfx.threads.projectactions

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import com.svoemesto.ivfx.utils.getListIFrames
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import org.sikuli.basics.Settings
import org.sikuli.script.Finder
import org.sikuli.script.Pattern

class AnalyzeFrames(var fileExt: FileExt,
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

        // создаем новые фреймы
        FrameController.createFrames(fileExt)

        Settings.MinSimilarity = 0.0
        var simScore: Double

        Platform.runLater {
            lbl1.text = textLbl1
            lbl2.text = "Getting I-Frames from file"
        }

        // 0. получаем список IFrame-ов
        val listIFrames = getListIFrames(mediaFile, fps)

        val countBlocks = 7
        var currentBlock = 0

        // 1. создаем список кадров и заполяем его номером, файлом и признаком isIFrame
        currentBlock++
        val listFramesExt = FrameController.getListFramesExt(fileExt)
        for (frameNumber in 1..framesCount) {
            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + frameNumber/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Create list Frames [$frameNumber/$framesCount]"
                pb2.progress = percentage2
            }
            val frameExt: FrameExt = listFramesExt.first { it.frame.frameNumber == frameNumber }
            frameExt.frame.isIFrame = listIFrames.contains(frameNumber)

        }

        // 2. заполняем simScore's
        currentBlock++
        for (i in 0 until listFramesExt.size - 1) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Analyze frame [$i/$framesCount]"
                pb2.progress = percentage2
            }

            val currentFrameExt: FrameExt = listFramesExt[i]
            val frameNextExt1: FrameExt? = if (i < listFramesExt.size - 1) listFramesExt[i + 1] else null
            val frameNextExt2: FrameExt? = if (i < listFramesExt.size - 2) listFramesExt[i + 2] else null
            val frameNextExt3: FrameExt? = if (i < listFramesExt.size - 3) listFramesExt[i + 3] else null
            simScore = 0.9999
            if (frameNextExt1 != null) {
                val f = Finder(currentFrameExt.pathToSmall)
                f.find(Pattern(frameNextExt1.pathToSmall))
                simScore = if (f.hasNext()) f.next().score else 0.0
                frameNextExt1.frame.simScorePrev1 = simScore
            }
            currentFrameExt.frame.simScoreNext1 = simScore
            simScore = 0.9999
            if (frameNextExt2 != null) {
                val f = Finder(currentFrameExt.pathToSmall)
                f.find(Pattern(frameNextExt2.pathToSmall))
                simScore = if (f.hasNext()) f.next().score else 0.0
                frameNextExt2.frame.simScorePrev2 = simScore
            }
            currentFrameExt.frame.simScoreNext2 = simScore
            simScore = 0.9999
            if (frameNextExt3 != null) {
                val f = Finder(currentFrameExt.pathToSmall)
                f.find(Pattern(frameNextExt3.pathToSmall))
                simScore = if (f.hasNext()) f.next().score else 0.0
                frameNextExt3.frame.simScorePrev3 = simScore
            }
            currentFrameExt.frame.simScoreNext3 = simScore
        }

        // 3. заполняем diff's
        currentBlock++
        for (i in 0 until listFramesExt.size - 1) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Fill diffs [$i/$framesCount]"
                pb2.progress = percentage2
            }

            val currentFrameExt: FrameExt = listFramesExt[i]
            val framePrevExt1: FrameExt? = if (i > 0) listFramesExt[i - 1] else null
            val framePrevExt2: FrameExt? = if (i > 1) listFramesExt[i - 2] else null
            val frameNextExt1: FrameExt? = if (i < listFramesExt.size - 1) listFramesExt[i + 1] else null
            val frameNextExt2: FrameExt? = if (i < listFramesExt.size - 2) listFramesExt[i + 2] else null
            var diffNext: Double
            diffNext = 0.0
            if (frameNextExt1 != null) {
                diffNext = currentFrameExt.frame.simScoreNext1 - frameNextExt1.frame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrameExt.frame.diffNext1 = diffNext
            diffNext = 0.0
            if (frameNextExt1 != null && frameNextExt2 != null) {
                diffNext = frameNextExt1.frame.simScoreNext1 - frameNextExt2.frame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrameExt.frame.diffNext2 = diffNext
            diffNext = 0.0
            if (framePrevExt1 != null) {
                diffNext = framePrevExt1.frame.simScoreNext1 - currentFrameExt.frame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrameExt.frame.diffPrev1 = diffNext
            diffNext = 0.0
            if (framePrevExt1 != null && framePrevExt2 != null) {
                diffNext = framePrevExt2.frame.simScoreNext1 - framePrevExt1.frame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrameExt.frame.diffPrev2 = diffNext
        }

        // 4. находим переходы
        currentBlock++
        val diff1 = 0.4 //Порог обнаружения перехода
        val diff2 = 0.42 //Вторичный порог
        for (i in 0 until listFramesExt.size - 1) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Finding transitions [$i/$framesCount]"
                pb2.progress = percentage2
            }

            val frameExt: FrameExt = listFramesExt[i]

            if (frameExt.frame.simScorePrev1 < diff1) {
                if (frameExt.frame.diffPrev1 > diff2 || frameExt.frame.diffPrev2 > diff2) {
                    frameExt.frame.isFind = true
                    frameExt.frame.isManualAdd = false
                    frameExt.frame.isManualCancel = false
                    frameExt.frame.isFinalFind = true
                } else {
                    frameExt.frame.isFind = false
                    frameExt.frame.isManualAdd = false
                    frameExt.frame.isManualCancel = false
                    frameExt.frame.isFinalFind = false
                }
            } else if (frameExt.frame.diffPrev1 > diff2 && frameExt.frame.diffPrev2 > diff2 && frameExt.frame.simScoreNext1 > diff1) {
                frameExt.frame.isFind = true
                frameExt.frame.isManualAdd = false
                frameExt.frame.isManualCancel = false
                frameExt.frame.isFinalFind = true
            } else {
                frameExt.frame.isFind = false
                frameExt.frame.isManualAdd = false
                frameExt.frame.isManualCancel = false
                frameExt.frame.isFinalFind = false
            }

        }

        // 5. сохраняем фреймы
        currentBlock++
        Platform.runLater {
            lbl1.text = textLbl1
            lbl2.text = "Saving frames"
        }
        val listFrames: MutableList<Frame> = mutableListOf()
        listFramesExt.forEach{ listFrames.add(it.frame)}
        Main.frameRepo.saveAll(listFrames)

        // 6. Создаем планы
        currentBlock++
        var firstFrameNumber = 1
        var lastFrameNumber: Int
        var currentFrameNumber: Int
        var currentIFrame = 1
        var previousIFrame = 1
        val listShotsTmp: List<Shot> = ShotController.getListShots(fileExt.file)
        val listShots: MutableList<Shot> = mutableListOf()

        for (i in 0 until listFramesExt.size - 1) {

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

            val frameExt: FrameExt = listFramesExt[i]

            if (frameExt.frame.isIFrame) currentIFrame = frameExt.frame.frameNumber
            currentFrameNumber = frameExt.frame.frameNumber
            if (frameExt.frame.isFinalFind) {
                lastFrameNumber = currentFrameNumber - 1
                val tmpShot = Shot()
                tmpShot.file = fileExt.file
                tmpShot.firstFrameNumber = firstFrameNumber
                tmpShot.lastFrameNumber = lastFrameNumber
                tmpShot.nearestIFrame = previousIFrame
                listShots.add(tmpShot)
                firstFrameNumber = currentFrameNumber
                previousIFrame = currentIFrame
            }

        }

        // 7. Проверяем планы
        currentBlock++
        for (i in 0 until listShots.size - 1) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/listShots.size.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Checking shots [$i/${listShots.size}]"
                pb2.progress = percentage2
            }

            val shot = listShots[i]

            var isFound = false
            listShotsTmp.forEach { shotTmp ->
                if (shot.firstFrameNumber == shotTmp.firstFrameNumber && shot.lastFrameNumber == shotTmp.lastFrameNumber) {
                    if (shot.nearestIFrame != shotTmp.nearestIFrame) {
                        shotTmp.nearestIFrame = shot.nearestIFrame
                        ShotController.save(shotTmp)
                    }
                    isFound = true
                    return@forEach
                }
            }
            if (!isFound) ShotController.save(shot)
        }

        fileExt.hasAnalyzedFrames = true
//        fileExt.hasAnalyzedFramesString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }

}