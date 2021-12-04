package com.svoemesto.ivfx.threads

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FileController.FileExt
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.utils.FaceDetection
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView

class DetectFaces(var fileExt: FileExt,
                  val table: TableView<FileExt>,
                  val textLbl1: String,
                  val numCurrentThread: Int,
                  val countThreads: Int,
                  var lbl1: Label, var pb1: ProgressBar,
                  var lbl2: Label, var pb2: ProgressBar): Thread(), Runnable {

    override fun run() {

        lbl1.isVisible = true
        pb1.isVisible = true
        lbl2.isVisible = false
        pb2.isVisible = false

        Platform.runLater {
            lbl1.text = textLbl1
            pb1.progress = (numCurrentThread-1) / countThreads.toDouble()
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("${faceDetectorPath.first()}:\n")
        param.add("cd \"${faceDetectorPath}\"\n")
        param.add("py")
        param.add("\"${faceDetectorPath}/detect_faces_in_folder.py\"")
        param.add("-i")
        param.add("\"${Main.fileController.getCdfFolder(fileExt.file, Folders.FRAMES_FULL)}\"")
        param.add("-d")
        param.add("\"${faceDetectorPath}/face_detection_model\"")
        param.add("-m")
        param.add("\"${faceDetectorPath}/openface_nn4.small2.v1.t7\"")
        param.add("-c")
        param.add(0.3.toString())

        val cmdText = param.joinToString(separator=" ").replace("/","\\")

        println(cmdText)

        RunCmd(cmdText).run()

        fileExt.hasFaces = true
        fileExt.hasFacesString = "✓"
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}