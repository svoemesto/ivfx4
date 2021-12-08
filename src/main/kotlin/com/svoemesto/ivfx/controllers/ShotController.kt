package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Shot
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class ShotController() {

    companion object {

        fun getProperties(shot: Shot) : List<Property> {
            return Main.propertyRepo.findByParentClassAndParentId(shot::class.simpleName!!, shot.id).toList()
        }

        fun getListShots(file: File): MutableList<Shot> {
            val result = Main.shotRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(file.id,0).toMutableList()
            result.forEach { it.file = file }
            return result
        }

        fun getPropertyValue(shot: Shot, key: String) : String {
            val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).firstOrNull()
            return property?.value ?: ""
        }

        fun isPropertyPresent(shot: Shot, key: String) : Boolean {
            return Main.propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).any()
        }

        fun save(shot: Shot) {
            Main.shotRepo.save(shot)
        }

        fun saveAll(shots: Iterable<Shot>) {
            Main.shotRepo.saveAll(shots)
        }

        fun delete(shot: Shot) {
            PropertyController.deleteAll(shot::class.java.simpleName, shot.id)
            PropertyCdfController.deleteAll(shot::class.java.simpleName, shot.id)
            Main.shotRepo.delete(shot)
        }

        fun deleteAll(file: File) {
            getListShots(file).forEach { shot ->
                PropertyController.deleteAll(shot::class.java.simpleName, shot.id)
                PropertyCdfController.deleteAll(shot::class.java.simpleName, shot.id)
            }
            Main.shotRepo.deleteAll(file.id)
        }

        fun getOrCreate(file: File, firstFrameNumber: Int, lastFrameNumber: Int, nearestIFrame: Int = 0): Shot {
            var entity = Main.shotRepo.findByFileIdAndFirstFrameNumberAndLastFrameNumber(file.id, firstFrameNumber, lastFrameNumber).firstOrNull()
            if (entity == null) {
                entity = Shot()
                entity.file = file
                entity.firstFrameNumber = firstFrameNumber
                entity.lastFrameNumber = lastFrameNumber
                entity.nearestIFrame = nearestIFrame
                save(entity)
            } else {
                entity.file = file
            }
            return entity
        }


    }


}