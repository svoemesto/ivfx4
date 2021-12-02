package com.svoemesto.ivfx

import com.svoemesto.ivfx.controllers.FileCdfController
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.FrameController
import com.svoemesto.ivfx.controllers.ProjectCdfController
import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.controllers.PropertyController
import com.svoemesto.ivfx.controllers.TrackController
import com.svoemesto.ivfx.fxcontrollers.ProjectSelectFXController
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.orm.hibernate5.LocalSessionFactoryBean

@SpringBootApplication
//@Scope("singleton")
class Main {
    companion object {
        val ccid = getCurrentComputerId()
//        val context = AnnotationConfigApplicationContext(SpringConfig::class.java)
//
//        val propertyRepo = context.getBean("propertyRepo", PropertyRepo::class.java)
//        val projectRepo = context.getBean("projectRepo", ProjectRepo::class.java)
//        val projectCdfRepo = context.getBean("projectCdfRepo", ProjectCdfRepo::class.java)
//        val fileRepo = context.getBean("fileRepo", FileRepo::class.java)
//        val fileCdfRepo = context.getBean("fileCdfRepo", FileCdfRepo::class.java)
//        val trackRepo = context.getBean("trackRepo", TrackRepo::class.java)
//        val frameRepo = context.getBean("frameRepo", FrameRepo::class.java)
//
//        val projectController = ProjectController(projectRepo, propertyRepo)
//        val projectCdfController = ProjectCdfController(projectCdfRepo)
//        val fileController = FileController(fileRepo, propertyRepo)
//        val fileCdfController = FileCdfController(fileCdfRepo)
//        val trackController = TrackController(trackRepo, propertyRepo)
//        val frameController = FrameController(frameRepo, propertyRepo)
//        val propertyController = PropertyController(propertyRepo)

    }

}