package com.svoemesto.ivfx.threads

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.utils.getListIFrames
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import org.sikuli.basics.Settings
import org.sikuli.script.Finder
import org.sikuli.script.Match
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
        val fps: Double = Main.fileController.getFps(fileExt.file)
        val framesCount: Int = Main.fileController.getFramesCount(fileExt.file)

        // удаляем все фреймы файла
        Main.frameController.deleteAll(fileExt.file)

        // создаем новые фреймы



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
        val listFrames: MutableList<Frame> = mutableListOf()
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

            val frame: Frame = Main.frameController.getOrCreate(fileExt.file, frameNumber)
            frame.isIFrame = listIFrames.contains(frameNumber)
//            frameController.save(frame)
            listFrames.add(frame)

        }

        // 2. заполняем simScore's
        currentBlock++
        for (i in 0 until listFrames.size - 1) {

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

            val currentFrame: Frame = listFrames[i]
            val frameNext1: Frame? = if (i < listFrames.size - 1) listFrames[i + 1] else null
            val frameNext2: Frame? = if (i < listFrames.size - 2) listFrames[i + 2] else null
            val frameNext3: Frame? = if (i < listFrames.size - 3) listFrames[i + 3] else null
            simScore = 0.9999
            if (frameNext1 != null) {
                val fileName1: String = Main.frameController.getFileNameFrameSmall(currentFrame)
                val fileName2: String = Main.frameController.getFileNameFrameSmall(frameNext1)
                val f = Finder(fileName1)
                val targetImage = Pattern(fileName2)
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.score
                frameNext1.simScorePrev1 = simScore
            }
            currentFrame.simScoreNext1 = simScore
            simScore = 0.9999
            if (frameNext2 != null) {
                val f = Finder(Main.frameController.getFileNameFrameSmall(currentFrame))
                val targetImage = Pattern(Main.frameController.getFileNameFrameSmall(frameNext2))
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.score
                frameNext2.simScorePrev2 = simScore
            }
            currentFrame.simScoreNext2 = simScore
            simScore = 0.9999
            if (frameNext3 != null) {
                val f = Finder(Main.frameController.getFileNameFrameSmall(currentFrame))
                val targetImage = Pattern(Main.frameController.getFileNameFrameSmall(frameNext3))
                f.find(targetImage)
                val match: Match = f.next()
                simScore = match.score
                frameNext3.simScorePrev3 = simScore
            }
            currentFrame.simScoreNext3 = simScore
        }

        // 3. заполняем diff's
        currentBlock++
        for (i in 0 until listFrames.size - 1) {

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

            val currentFrame: Frame = listFrames[i]
            val framePrev1: Frame? = if (i > 0) listFrames[i - 1] else null
            val framePrev2: Frame? = if (i > 1) listFrames[i - 2] else null
            val frameNext1: Frame? = if (i < listFrames.size - 1) listFrames[i + 1] else null
            val frameNext2: Frame? = if (i < listFrames.size - 2) listFrames[i + 2] else null
            var diffNext: Double
            diffNext = 0.0
            if (frameNext1 != null) {
                diffNext = currentFrame.simScoreNext1 - frameNext1.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffNext1 = diffNext
            diffNext = 0.0
            if (frameNext1 != null && frameNext2 != null) {
                diffNext = frameNext1.simScoreNext1 - frameNext2.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffNext2 = diffNext
            diffNext = 0.0
            if (framePrev1 != null) {
                diffNext = framePrev1.simScoreNext1 - currentFrame.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffPrev1 = diffNext
            diffNext = 0.0
            if (framePrev1 != null && framePrev2 != null) {
                diffNext = framePrev2.simScoreNext1 - framePrev1.simScoreNext1
                if (diffNext < 0) diffNext = -diffNext
            }
            currentFrame.diffPrev2 = diffNext
        }

        // 4. находим переходы
        currentBlock++
        val diff1 = 0.4 //Порог обнаружения перехода
        val diff2 = 0.42 //Вторичный порог
        for (i in 0 until listFrames.size - 1) {

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

            val frame: Frame = listFrames[i]

            if (frame.simScorePrev1 < diff1) {
                if (frame.diffPrev1 > diff2 || frame.diffPrev2 > diff2) {
                    frame.isFind = true
                    frame.isManualAdd = false
                    frame.isManualCancel = false
                    frame.isFinalFind = false
                } else {
                    frame.isFind = false
                    frame.isManualAdd = false
                    frame.isManualCancel = false
                    frame.isFinalFind = false
                }
            } else if (frame.diffPrev1 > diff2 && frame.diffPrev2 > diff2 && frame.simScoreNext1 > diff1) {
                frame.isFind = true
                frame.isManualAdd = false
                frame.isManualCancel = false
                frame.isFinalFind = false
            } else {
                frame.isFind = false
                frame.isManualAdd = false
                frame.isManualCancel = false
                frame.isFinalFind = false
            }

        }

        // 5. сохраняем фреймы
        currentBlock++
        for (i in 0 until listFrames.size - 1) {

            val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
            val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
            val percentage2: Double = ((currentBlock-1) + (i+1)/framesCount.toDouble() ) / countBlocks.toDouble()
            val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
            Platform.runLater {
                lbl1.text = textLbl1
                pb1.progress = percentage1
                lbl2.text = "Saving frames [$i/$framesCount]"
                pb2.progress = percentage2
            }

            val frame: Frame = listFrames[i]

            Main.frameController.save(frame)
        }

        // 6. Создаем планы
        currentBlock++
        var firstFrameNumber = 1
        var lastFrameNumber: Int
        var currentFrameNumber: Int
        var currentIFrame = 1
        var previousIFrame = 1
        val listShotsTmp: List<Shot> = Main.shotController.getListShots(fileExt.file)
        val listShots: MutableList<Shot> = mutableListOf()

        for (i in 0 until listFrames.size - 1) {

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

            val frame: Frame = listFrames[i]

            if (frame.isIFrame) currentIFrame = frame.frameNumber
            currentFrameNumber = frame.frameNumber
            if (frame.isFinalFind) {
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
                lbl2.text = "Checking shots [$i/$framesCount]"
                pb2.progress = percentage2
            }

            val shot = listShots[i]

            var isFound = false
            listShotsTmp.forEach { shotTmp ->
                if (shot.firstFrameNumber == shotTmp.firstFrameNumber && shot.lastFrameNumber == shotTmp.lastFrameNumber) {
                    if (shot.nearestIFrame != shotTmp.nearestIFrame) {
                        shotTmp.nearestIFrame = shot.nearestIFrame
                        Main.shotController.save(shotTmp)
                    }
                    isFound = true
                    return@forEach
                }
            }
            if (!isFound) Main.shotController.save(shot)
        }

        fileExt.hasAnalyzedFrames = true
        fileExt.hasAnalyzedFramesString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }

}