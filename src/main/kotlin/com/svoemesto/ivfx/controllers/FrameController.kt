package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class FrameController(val repo: FrameRepo, val propertyRepo: PropertyRepo, val propertyCdfRepo: PropertyCdfRepo) {

    fun getProperties(frame: Frame) : List<Property> {
        return propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
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
        repo.save(entity)
        return entity
    }


}