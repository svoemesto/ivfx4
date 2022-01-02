package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.modelsext.EventExt
import com.svoemesto.ivfx.modelsext.SceneExt
import com.svoemesto.ivfx.modelsext.ShotExt
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class EventController() {

    companion object {

        fun getProperties(event: Event) : List<Property> {
            return Main.propertyRepo.findByParentClassAndParentId(event::class.simpleName!!, event.id).toList()
        }

        fun getSetEvents(file: File): MutableSet<Event> {
            val result = Main.eventRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(file.id,0).toMutableSet()
            result.forEach { shot ->
                shot.file = file
            }
            return result
        }

        fun getPropertyValue(event: Event, key: String) : String {
            val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(event::class.simpleName!!, event.id, key).firstOrNull()
            return property?.value ?: ""
        }

        fun isPropertyPresent(event: Event, key: String) : Boolean {
            return Main.propertyRepo.findByParentClassAndParentIdAndKey(event::class.simpleName!!, event.id, key).any()
        }

        fun save(event: Event) {
            Main.eventRepo.save(event)
        }

        fun saveAll(event: Iterable<Event>) {
            Main.eventRepo.saveAll(event)
        }

        fun delete(event: Event) {
            PropertyController.deleteAll(event::class.java.simpleName, event.id)
            PropertyCdfController.deleteAll(event::class.java.simpleName, event.id)
            TagController.deleteAll(event::class.java.simpleName, event.id)
            Main.eventRepo.delete(event)
        }

        fun deleteAll(file: File) {
            getSetEvents(file).forEach { event ->
                PropertyController.deleteAll(event::class.java.simpleName, event.id)
                PropertyCdfController.deleteAll(event::class.java.simpleName, event.id)
                TagController.deleteAll(event::class.java.simpleName, event.id)
            }
            Main.eventRepo.deleteAll(file.id)
        }

        fun getOrCreate(file: File, firstFrameNumber: Int, lastFrameNumber: Int): Event {
            var entity = Main.eventRepo.findByFileIdAndFirstFrameNumberAndLastFrameNumber(file.id, firstFrameNumber, lastFrameNumber).firstOrNull()
            if (entity == null) {

                entity = Event()
                entity.file = file
                entity.firstFrameNumber = firstFrameNumber
                entity.lastFrameNumber = lastFrameNumber
                entity.name = "Event $firstFrameNumber-$lastFrameNumber"
                save(entity)

            } else {
                entity.file = file
            }
            return entity
        }

        fun createEventExt(listShotsExt: MutableList<ShotExt>): EventExt? {
            if (listShotsExt.isNotEmpty()) {
                val event = getOrCreate(listShotsExt.first().fileExt.file, listShotsExt.first().shot.firstFrameNumber, listShotsExt.last().shot.lastFrameNumber)
                return EventExt(event,listShotsExt.first().fileExt,listShotsExt.first().firstFrameExt, listShotsExt.last().lastFrameExt)
            }
            return null
        }

    }


}