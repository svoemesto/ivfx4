package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.ShotController
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.utils.FaceDetection
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile

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
        lbl2.isVisible = true
        pb2.isVisible = true

        Platform.runLater {
            lbl1.text = textLbl1
            pb1.progress = (numCurrentThread-1) / countThreads.toDouble()
            lbl2.text = "Detecting faces..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val builder = GsonBuilder()
        var gson = builder.create()
        val pathToFileJSON: String = fileExt.folderFramesFull + IOFile.separator + "frames.json"

        fileExt.file.shots = ShotController.getSetShots(fileExt.file)
        val arrFrameToDetectFaces: Array<FaceController.Companion.FrameToDetectFaces> = FaceController.getArrayFramesToDetectFaces(fileExt)

        try {
            FileWriter(pathToFileJSON).use { fileWriter -> gson.toJson(arrFrameToDetectFaces, fileWriter) }
        } catch (e: IOException) {
            e.printStackTrace()
        }


        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("${faceDetectorPath.first()}:\n")
        param.add("cd \"${faceDetectorPath}\"\n")
        param.add("py")
        param.add("\"${faceDetectorPath}/detect_faces_in_folder.py\"")
        param.add("-i")
        param.add("\"${fileExt.folderFramesFull}\"")
        param.add("-o")
        param.add("\"${fileExt.folderFacesFull}\"")
        param.add("-d")
        param.add("\"${faceDetectorPath}/face_detection_model\"")
        param.add("-m")
        param.add("\"${faceDetectorPath}/openface_nn4.small2.v1.t7\"")
        param.add("-c")
        param.add(0.3.toString())

        val cmdText = param.joinToString(separator=" ").replace("/","\\")

        println(cmdText)

        val runCmd = RunCmd(cmdText)
        runCmd.run()

        println("?????? ???????????? ???????????????????????? ?????????? ???????????????????? ???????????????? cmd")

        fileExt.hasDetectedFaces = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}