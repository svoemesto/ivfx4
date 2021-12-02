package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class FrameController(val projectRepo: ProjectRepo,
                      val propertyRepo: PropertyRepo,
                      val propertyCdfRepo: PropertyCdfRepo,
                      val projectCdfRepo: ProjectCdfRepo,
                      val fileRepo: FileRepo,
                      val fileCdfRepo: FileCdfRepo,
                      val frameRepo: FrameRepo,
                      val trackRepo: TrackRepo) {

    fun getProperties(frame: Frame) : List<Property> {
        return propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
    }

    fun getOrCreate(file: File, frameNumber: Int): Frame {
        var entity = frameRepo.findByFileIdAndFrameNumber(file.id, frameNumber).firstOrNull()
        if (entity == null) {
            entity = Frame()
            entity.file = file
            entity.frameNumber = frameNumber
            frameRepo.save(entity)
        }
        return entity
    }

    fun getListFrames(file: File): List<Frame> {
        return frameRepo.findByFileIdAndFrameNumberGreaterThanOrderByFrameNumber(file.id,0).toList()
    }

    fun getPropertyValue(frame: Frame, key: String) : String {
        val property = propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).firstOrNull()
        return if (property != null) property.value else ""
    }

    fun isPropertyPresent(frame: Frame, key: String) : Boolean {
        return propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).any()
    }

    fun create(file: File, frameNumber: Int): Frame {
        val entity = Frame()
        entity.file = file
        entity.frameNumber = frameNumber
        frameRepo.save(entity)
        return entity
    }

    fun getFileNameFrameSmall(frame: Frame): String {
        val fileController = FileController(projectRepo, propertyRepo, propertyCdfRepo, projectCdfRepo, fileRepo, fileCdfRepo, frameRepo, trackRepo)
        val fld = fileController.getCdfFolder(frame.file, Folders.FRAMES_SMALL)
        return "$fld${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameMedium(frame: Frame): String {
        val fileController = FileController(projectRepo, propertyRepo, propertyCdfRepo, projectCdfRepo, fileRepo, fileCdfRepo, frameRepo, trackRepo)
        val fld = fileController.getCdfFolder(frame.file, Folders.FRAMES_MEDIUM)
        return "$fld${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameFull(frame: Frame): String {
        val fileController = FileController(projectRepo, propertyRepo, propertyCdfRepo, projectCdfRepo, fileRepo, fileCdfRepo, frameRepo, trackRepo)
        val fld = fileController.getCdfFolder(frame.file, Folders.FRAMES_FULL)
        return "$fld${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }
}