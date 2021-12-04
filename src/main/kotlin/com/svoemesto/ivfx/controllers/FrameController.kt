package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Property
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
class FrameController() {

    fun getProperties(frame: Frame) : List<Property> {
        return Main.propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
    }

    fun getOrCreate(file: File, frameNumber: Int): Frame {
        var entity = Main.frameRepo.findByFileIdAndFrameNumber(file.id, frameNumber).firstOrNull()
        if (entity == null) {
            entity = Frame()
            entity.file = file
            entity.frameNumber = frameNumber
            save(entity)
        } else {
            entity.file = file
        }
        return entity
    }

    fun getListFrames(file: File): MutableList<Frame> {
        val result = Main.frameRepo.findByFileIdAndFrameNumberGreaterThanOrderByFrameNumber(file.id,0).toMutableList()
        result.forEach { it.file = file }
        return result
    }

    fun getPropertyValue(frame: Frame, key: String) : String {
        val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).firstOrNull()
        return property?.value ?: ""
    }

    fun isPropertyPresent(frame: Frame, key: String) : Boolean {
        return Main.propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).any()
    }

    fun save(frame: Frame) {
        Main.frameRepo.save(frame)
    }

    fun delete(frame: Frame) {
        Main.propertyController.deleteAll(frame::class.java.simpleName, frame.id)
        Main.propertyCdfController.deleteAll(frame::class.java.simpleName, frame.id)
        Main.frameRepo.delete(frame)
    }

    fun deleteAll(file: File) {
        getListFrames(file).forEach { frame ->
            Main.propertyController.deleteAll(frame::class.java.simpleName, frame.id)
            Main.propertyCdfController.deleteAll(frame::class.java.simpleName, frame.id)
        }
        Main.frameRepo.deleteAll(file.id)
    }
    fun create(file: File, frameNumber: Int): Frame {
        val entity = Frame()
        entity.file = file
        entity.frameNumber = frameNumber
        save(entity)
        return entity
    }

    fun getFileNameFrameSmall(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_SMALL)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameMedium(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_MEDIUM)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameFull(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_FULL)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }
}