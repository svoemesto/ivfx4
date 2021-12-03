package com.svoemesto.ivfx.controllers

import com.google.gson.internal.LinkedTreeMap
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Track
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.TrackRepo
import com.svoemesto.ivfx.utils.MediaInfo
import com.svoemesto.ivfx.utils.getFromMediaInfoTracks
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional


@Controller
//@Scope("prototype")
class TrackController(val trackRepo: TrackRepo, val propertyRepo: PropertyRepo, val propertyCdfRepo: PropertyCdfRepo) {

    fun getListTracks(file: File): List<Track> {
        return trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(file.id,0).toList()
    }

    fun getProperties(track: Track) : List<Property> {
        return propertyRepo.findByParentClassAndParentId(track::class.simpleName!!, track.id).toList()
    }

    fun getPropertyValue(track: Track, key: String) : String {
        val property = propertyRepo.findByParentClassAndParentIdAndKey(track::class.simpleName!!, track.id, key).firstOrNull()
        return if (property != null) property.value else ""
    }

    fun isPropertyPresent(track: Track, key: String) : Boolean {
        return propertyRepo.findByParentClassAndParentIdAndKey(track::class.simpleName!!, track.id, key).any()
    }

    fun createTracksFromMediaInfo(file: File) {
        val json = MediaInfo.executeMediaInfo(file.path, "--Output=JSON")
        val listJsonTracks =  getFromMediaInfoTracks(json)
        if (listJsonTracks != null) {

            file.tracks.forEach{ track ->
                propertyRepo.deleteAll(track::class.java.simpleName, track.id)
                propertyCdfRepo.deleteAll(track::class.java.simpleName, track.id)
            }
            trackRepo.deleteAll(file.id)

            listJsonTracks.forEach { jsonTrack ->
                val trackType = jsonTrack["@type"].toString()
                val trackName = trackType
                val track = create(file)
                track.type = trackType
                track.name = trackName
                track.use = true
                trackRepo.save(track)

                jsonTrack.entries.forEach { entry ->
                    println("entry1.value = " + entry.value)
                    if (entry.value is LinkedTreeMap<*, *>) {
                        val jsonTrack2 =  entry.value as Map<*, *>
                        jsonTrack2.entries.forEach { entry2 ->
                            println("entry2.value = " + entry2.value)
                            PropertyController(propertyRepo).editOrCreate(track::class.java.simpleName,
                                track.id, entry2.key as String, entry2.value.toString())
                        }
                    } else {
                        PropertyController(propertyRepo).editOrCreate(track::class.java.simpleName,
                            track.id, entry.key, entry.value.toString())
                    }
                }
            }
        }
    }

    fun create(file: File): Track {
        val entity = Track()
        entity.file = file
        val lastEntity = trackRepo.getEntityWithGreaterOrder(file.id).firstOrNull()
        entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
        entity.name = "New file track ${entity.order} to file ${file.id}"
        trackRepo.save(entity)
        return entity
    }

    fun delete(track: Track) {
        reOrder(ReorderTypes.MOVE_TO_LAST, track)
        propertyRepo.deleteAll(track::class.java.simpleName, track.id)
        trackRepo.delete(track.id)
    }

    fun reOrder(reorderType: ReorderTypes, track: Track) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(track.file.id, track.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    track.order += 1
                    trackRepo.save(track)
                    trackRepo.save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = trackRepo.findByFileIdAndOrderLessThanOrderByOrderDesc(track.file.id, track.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    track.order -= 1
                    trackRepo.save(track)
                    trackRepo.save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = trackRepo.findByFileIdAndOrderLessThanOrderByOrderDesc(track.file.id, track.order)
                previousEntities.forEach{it.order++}
                trackRepo.saveAll(previousEntities)
                track.order = 1
                trackRepo.save(track)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = trackRepo.findByFileIdAndOrderGreaterThanOrderByOrder(track.file.id, track.order)
                if (nextEntities.count()>0) {
                    nextEntities.forEach{it.order--}
                    trackRepo.saveAll(nextEntities)
                    track.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    trackRepo.save(track)
                }
            }
        }
    }


}