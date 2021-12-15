package com.svoemesto.ivfx

import com.svoemesto.ivfx.repos.EventRepo
import com.svoemesto.ivfx.repos.EventShotRepo
import com.svoemesto.ivfx.repos.FaceRepo
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.PersonRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.SceneRepo
import com.svoemesto.ivfx.repos.SceneShotRepo
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TagRepo
import com.svoemesto.ivfx.repos.TrackRepo
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.AnnotationConfigApplicationContext

@SpringBootApplication
//@Scope("singleton")
class Main {
    companion object {
        val ccid = getCurrentComputerId()
        val context = AnnotationConfigApplicationContext(SpringConfig::class.java)
        val connection = getConnection()
        val propertyRepo = context.getBean("propertyRepo", PropertyRepo::class.java)
        val propertyCdfRepo = context.getBean("propertyCdfRepo", PropertyCdfRepo::class.java)
        val projectRepo = context.getBean("projectRepo", ProjectRepo::class.java)
        val projectCdfRepo = context.getBean("projectCdfRepo", ProjectCdfRepo::class.java)
        val fileRepo = context.getBean("fileRepo", FileRepo::class.java)
        val fileCdfRepo = context.getBean("fileCdfRepo", FileCdfRepo::class.java)
        val trackRepo = context.getBean("trackRepo", TrackRepo::class.java)
        val frameRepo = context.getBean("frameRepo", FrameRepo::class.java)
        val shotRepo = context.getBean("shotRepo", ShotRepo::class.java)
        val faceRepo = context.getBean("faceRepo", FaceRepo::class.java)
        val tagRepo = context.getBean("tagRepo", TagRepo::class.java)
        val sceneRepo = context.getBean("sceneRepo", SceneRepo::class.java)
        val eventRepo = context.getBean("eventRepo", EventRepo::class.java)
        val sceneShotRepo = context.getBean("sceneShotRepo", SceneShotRepo::class.java)
        val eventShotRepo = context.getBean("eventShotRepo", EventShotRepo::class.java)
        val personRepo = context.getBean("personRepo", PersonRepo::class.java)

        const val MEDIUM_FRAME_W = 720.0
        const val MEDIUM_FRAME_H = 400.0
        const val PREVIEW_FRAME_W = 135.0
        const val PREVIEW_FRAME_H = 75.0
        const val PREVIEW_FACE_W = 75.0
        const val PREVIEW_FACE_H = 75.0
        const val PREVIEW_FACE_EXPAND_FACTOR = 1.4
        const val PREVIEW_FACE_CROPPING = false
        const val PREVIEW_PERSON_CROPPING = false
    }


}