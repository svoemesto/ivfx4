package com.svoemesto.ivfx.apps

import com.svoemesto.ivfx.SpringConfig
import com.svoemesto.ivfx.fxcontrollers.DatabaseSelectFXController
import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController
import com.svoemesto.ivfx.fxcontrollers.ProjectSelectFXController
import com.svoemesto.ivfx.initializeH2db
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.AnnotationConfigApplicationContext

@SpringBootApplication
class ProjectFXApp: Application() {
    override fun start(stage: Stage) {

        initializeH2db()
        ProjectEditFXController().editProject(null, hostServices)

//        val context = AnnotationConfigApplicationContext(SpringConfig::class.java)

//        DatabaseSelectFXController.getDatabase(null)
    }

}

fun main() {
    Application.launch(ProjectFXApp::class.java)
}