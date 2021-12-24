package com.svoemesto.ivfx.controllers

import com.google.gson.internal.LinkedTreeMap
import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Track
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.utils.MediaInfo
import com.svoemesto.ivfx.utils.getFromMediaInfoTracks
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional


@Controller
//@Scope("prototype")
class TrackController() {

    companion object {

//        fun getListTracks(file: File): MutableList<Track> {
//            val result = Main.trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(file.id,0).toMutableList()
//            result.forEach { it.file = file }
//            return result
//        }

        fun getSetTracks(file: File): MutableSet<Track> {
            return Main.trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(file.id, 0)
                .map { it.file = file; it }
                .toMutableSet()
        }

        fun getProperties(track: Track) : List<Property> {
            return Main.propertyRepo.findByParentClassAndParentId(track::class.simpleName!!, track.id).toList()
        }

        fun getPropertyValue(track: Track, key: String) : String {
            val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(track::class.simpleName!!, track.id, key).firstOrNull()
            return if (property != null) property.value else ""
        }

        fun isPropertyPresent(track: Track, key: String) : Boolean {
            return Main.propertyRepo.findByParentClassAndParentIdAndKey(track::class.simpleName!!, track.id, key).any()
        }

        fun createTracksFromMediaInfo(file: File) {
            val json = MediaInfo.executeMediaInfo(file.path, "--Output=JSON")
            val listJsonTracks =  getFromMediaInfoTracks(json)
            if (listJsonTracks != null) {

                file.tracks.forEach{ track ->
                    PropertyController.deleteAll(track::class.java.simpleName, track.id)
                    PropertyCdfController.deleteAll(track::class.java.simpleName, track.id)
                }
                deleteAll(file)

                listJsonTracks.forEach { jsonTrack ->
                    val trackType = jsonTrack["@type"].toString()
                    val trackName = trackType
                    val track = create(file)
                    track.type = trackType
                    track.name = trackName
                    track.use = true
                    save(track)

                    jsonTrack.entries.forEach { entry ->
                        println("entry1.value = " + entry.value)
                        if (entry.value is LinkedTreeMap<*, *>) {
                            val jsonTrack2 =  entry.value as Map<*, *>
                            jsonTrack2.entries.forEach { entry2 ->
                                println("entry2.value = " + entry2.value)
                                PropertyController.editOrCreate(track::class.java.simpleName,
                                    track.id, entry2.key as String, entry2.value.toString())
                            }
                        } else {
                            PropertyController.editOrCreate(track::class.java.simpleName,
                                track.id, entry.key, entry.value.toString())
                        }
                    }
                }
            }
        }



        fun create(file: File): Track {
            val entity = Track()
            entity.file = file
            val lastEntity = Main.trackRepo.getEntityWithGreaterOrder(file.id).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.name = "New file track ${entity.order} to file ${file.id}"
            save(entity)
            return entity
        }

        fun delete(track: Track) {
            reOrder(ReorderTypes.MOVE_TO_LAST, track)
            PropertyController.deleteAll(track::class.java.simpleName, track.id)
            Main.trackRepo.delete(track.id)
        }

        fun deleteAll(file: File) {
            file.tracks.forEach { track ->
                PropertyController.deleteAll(track::class.java.simpleName, track.id)
                PropertyCdfController.deleteAll(track::class.java.simpleName, track.id)
            }
            Main.trackRepo.deleteAll(file.id)
        }

        fun save(track: Track) {
            Main.trackRepo.save(track)
        }

        fun saveAll(tracks: Iterable<Track>) {
            Main.trackRepo.saveAll(tracks)
        }

        fun reOrder(reorderType: ReorderTypes, track: Track) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(track.file.id, track.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        track.order += 1
                        save(track)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.trackRepo.findByFileIdAndOrderLessThanOrderByOrderDesc(track.file.id, track.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        track.order -= 1
                        save(track)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.trackRepo.findByFileIdAndOrderLessThanOrderByOrderDesc(track.file.id, track.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    track.order = 1
                    save(track)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(track.file.id, track.order)
                    if (nextEntities.count()>0) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        track.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(track)
                    }
                }
            }
        }


    }



}