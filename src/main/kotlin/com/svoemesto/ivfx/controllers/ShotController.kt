package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TrackRepo
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class ShotController(val projectRepo: ProjectRepo,
                     val propertyRepo: PropertyRepo,
                     val propertyCdfRepo: PropertyCdfRepo,
                     val projectCdfRepo: ProjectCdfRepo,
                     val fileRepo: FileRepo,
                     val fileCdfRepo: FileCdfRepo,
                     val frameRepo: FrameRepo,
                     val trackRepo: TrackRepo,
                     val shotRepo: ShotRepo) {

    fun getProperties(frame: Frame) : List<Property> {
        return propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
    }


    fun getListShots(file: File): List<Shot> {
        return shotRepo.findByFileIdOrderByFirstFrameNumber(file.id,0).toList()
    }

    fun getPropertyValue(shot: Shot, key: String) : String {
        val property = propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).firstOrNull()
        return if (property != null) property.value else ""
    }

    fun isPropertyPresent(shot: Shot, key: String) : Boolean {
        return propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).any()
    }

    fun getOrCreate(file: File, firstFrameNumber: Int, lastFrameNumber: Int): Shot {
        var entity = shotRepo.findByFileIdAndFirstFrameNumberAndLastFrameNumber(file.id, firstFrameNumber, lastFrameNumber).firstOrNull()
        if (entity == null) {
            entity = Shot()
            entity.file = file
            entity.firstFrameNumber = firstFrameNumber
            entity.lastFrameNumber = lastFrameNumber
            shotRepo.save(entity)
        }
        return entity
    }

}