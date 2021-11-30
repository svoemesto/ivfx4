package com.svoemesto.ivfx.apps

import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController
import com.svoemesto.ivfx.fxcontrollers.ProjectSelectFXController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ProjectFXApp: Application() {
    override fun start(stage: Stage) {

        ProjectEditFXController.editProject(null, hostServices)

    }

}

fun main() {
    Application.launch(ProjectFXApp::class.java)
}