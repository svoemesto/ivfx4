package com.svoemesto.ivfx.threads.projectactions

import com.google.gson.GsonBuilder
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FaceController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.threads.RunCmd
import com.svoemesto.ivfx.utils.FaceDetection
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableView
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.File as IOFile

class RecognizeFaces(var fileExt: FileExt,
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
            lbl2.text = "Recognizing faces..."
            pb2.progress = ProgressBar.INDETERMINATE_PROGRESS
        }

        val builder = GsonBuilder()
        val gson = builder.create()

        val pathToFileJSON: String = fileExt.folderFramesFull + IOFile.separator + "faces.json"
        val arrFrameFaces: Array<FaceExt> = FaceController.getListFacesExt(fileExt).toTypedArray()

        try {
            FileWriter(pathToFileJSON).use { fileWriter -> gson.toJson(arrFrameFaces, fileWriter) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val faceDetectorPath = FaceDetection.FACE_DETECTOR_PATH

        val param: MutableList<String> = mutableListOf()

        param.add("${faceDetectorPath.first()}:\n")
        param.add("cd \"${faceDetectorPath}\"\n")
        param.add("py")
        param.add("\"${faceDetectorPath}/recognize_faces.py\"")
        param.add("-i")
        param.add("\"${pathToFileJSON}\"")
        param.add("-d")
        param.add("\"${faceDetectorPath}/face_detection_model\"")
        param.add("-m")
        param.add("\"${faceDetectorPath}/openface_nn4.small2.v1.t7\"")
        param.add("-r")
        param.add("\"${fileExt.projectExt.project.folder}/recognizer.pickle\"")
        param.add("-l")
        param.add("\"${fileExt.projectExt.project.folder}/le.pickle\"")
        param.add("-c")
        param.add(0.3.toString())

        val cmdText = param.joinToString(separator=" ").replace("/","\\")

        println(cmdText)

        val runCmd = RunCmd(cmdText)
        runCmd.run()

        try {
            FileReader(pathToFileJSON).use { fileReader ->
                val facesExtArray: Array<FaceExt> = gson.fromJson(fileReader, Array<FaceExt>::class.java)
                for ((i, faceExt) in facesExtArray.withIndex()) {

                    val initProgress1: Double = (numCurrentThread-1) / (countThreads.toDouble())
                    val onePeaceOfProgress: Double = 1 / (countThreads.toDouble())
                    val percentage2: Double = (i+1)/facesExtArray.size.toDouble()
                    val percentage1: Double = initProgress1 + (onePeaceOfProgress * percentage2)
                    Platform.runLater {
                        lbl1.text = textLbl1
                        pb1.progress = percentage1
                        lbl2.text = "Recognize face [$i/${facesExtArray.size}]"
                        pb2.progress = percentage2
                    }

                    faceExt.vectorText = faceExt.vector.joinToString(separator = "|", prefix = "", postfix = "")
                    val face = FaceController.createOrUpdate(faceExt, fileExt)

                    if (faceExt.personRecognizedName != "") {
                        val person =
                            Main.personRepo.findByProjectIdAndNameInRecognizer(fileExt.projectExt.project.id, faceExt.personRecognizedName).firstOrNull()
                                ?:
                                PersonController.create(fileExt.projectExt.project,"",
                                    faceExt.personRecognizedName,
                                    fileExt.file.id, face.frameNumber, face.faceNumberInFrame)
                        face.personRecognizedId = person.id
                        FaceController.save(face)
                        faceExt.personRecognizedId = person.id
                    }

                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        println("Это должно напечататься после завершения процесса cmd")

        fileExt.hasRecognizedFaces = true
        table.refresh()

        lbl1.isVisible = false
        lbl2.isVisible = false
        pb1.isVisible = false
        pb2.isVisible = false

    }
}