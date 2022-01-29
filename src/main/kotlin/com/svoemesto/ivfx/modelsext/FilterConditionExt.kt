package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot

class FilterConditionExt(var filterCondition: FilterCondition): Comparable<FilterConditionExt> {

    override fun compareTo(other: FilterConditionExt): Int {
        return this.filterCondition.order - other.filterCondition.order
    }

    val name: String get() = filterCondition.name
    val order: Int get() = filterCondition.order

    fun shotsIds(): Set<Long> {
        val setOfShotsIds: Set<Long> = Main.shotRepo.getShotsForShotsTmp(Main.ccid).map { it.id }.toSet()
        val setOfShotsIdsWithObject: MutableSet<Long> = mutableSetOf()
        val shots: MutableSet<Long> = mutableSetOf()
        when(filterCondition.objectClass) {
            Person::class.java.simpleName ->
            {
                when (filterCondition.subjectClass) {
                    Shot::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForShotsTmpAndPerson(Main.ccid, filterCondition.objectId).map { it })
                    }
                    Scene::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForScenesTmpAndPerson(Main.ccid, filterCondition.objectId).map { it })
                    }
                    Event::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForEventsTmpAndPerson(Main.ccid, filterCondition.objectId).map { it })
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(setOfShotsIdsWithObject)
                } else {
                    shots.addAll(setOfShotsIds)
                    shots.removeAll(setOfShotsIdsWithObject)
                }
            }

            "${Person::class.java.simpleName} ${Property::class.java.simpleName}" -> {

                when (filterCondition.subjectClass) {
                    Shot::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForShotsTmpAndPersonProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    Scene::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForScenesTmpAndPersonProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    Event::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForEventsTmpAndPersonProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(setOfShotsIdsWithObject)
                } else {
                    shots.addAll(setOfShotsIds)
                    shots.removeAll(setOfShotsIdsWithObject)
                }
            }

            "${Shot::class.java.simpleName} ${Property::class.java.simpleName}" -> {

                when (filterCondition.subjectClass) {
                    Shot::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForShotsTmpAndShotProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    Scene::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForScenesTmpAndShotProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    Event::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForEventsTmpAndShotProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(setOfShotsIdsWithObject)
                } else {
                    shots.addAll(setOfShotsIds)
                    shots.removeAll(setOfShotsIdsWithObject)
                }
            }

            "${Scene::class.java.simpleName} ${Property::class.java.simpleName}" -> {

                when (filterCondition.subjectClass) {
                    Scene::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForScenesTmpAndSceneProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(setOfShotsIdsWithObject)
                } else {
                    shots.addAll(setOfShotsIds)
                    shots.removeAll(setOfShotsIdsWithObject)
                }
            }

            "${Event::class.java.simpleName} ${Property::class.java.simpleName}" -> {

                when (filterCondition.subjectClass) {
                    Event::class.java.simpleName -> {
                        setOfShotsIdsWithObject.addAll(Main.shotRepo.getShotsIdsForEventsTmpAndEventProperty(Main.ccid, filterCondition.objectName).map { it })
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(setOfShotsIdsWithObject)
                } else {
                    shots.addAll(setOfShotsIds)
                    shots.removeAll(setOfShotsIdsWithObject)
                }
            }

            else -> {}
        }
        return shots
    }

    fun shots(): Set<Shot> {
        val setOfShots: Set<Shot> = Main.shotRepo.getShotsForShotsTmp(Main.ccid).toSet()
        val mapOfShots: MutableMap<Long, Shot> = setOfShots.associateBy { it.id }.toMutableMap()
        val shots: MutableSet<Shot> = mutableSetOf()
        val shotWithPerson: MutableMap<Long, Shot> = mutableMapOf()
        when(filterCondition.objectClass) {
            Person::class.java.simpleName ->
            {
                when (filterCondition.subjectClass) {
                    Shot::class.java.simpleName -> {
                        shotWithPerson.putAll(Main.shotRepo.getShotsForShotsTmpAndPerson(Main.ccid, filterCondition.objectId).associateBy { it.id }.toMutableMap())
                    }
                    Scene::class.java.simpleName -> {
                        shotWithPerson.putAll(Main.shotRepo.getShotsForScenesTmpAndPerson(Main.ccid, filterCondition.objectId).associateBy { it.id }.toMutableMap())
                    }
                    Event::class.java.simpleName -> {
                        shotWithPerson.putAll(Main.shotRepo.getShotsForEventsTmpAndPerson(Main.ccid, filterCondition.objectId).associateBy { it.id }.toMutableMap())
                    }
                    File::class.java.simpleName -> {}
                }

                if (filterCondition.isIncluded) {
                    shots.addAll(shotWithPerson.values)
                } else {
                    shots.addAll(
                        mapOfShots.filter {!shotWithPerson.keys.contains(it.key)}.values
                    )
                }
            }
            else -> {}
        }
        val files: MutableSet<File> = mutableSetOf()
        shots.forEach { shot ->
            val file = files.firstOrNull { file -> file.shots.map{it.id}.contains(shot.id) }?: FileController.getFileForShotId(shot.id)
            files.add(file)
            shot.file = file
        }
        return shots
    }

    fun shotsExt(setOfShotsExt: Set<ShotExt>): Set<ShotExt> {

        val shotsExt: MutableSet<ShotExt> = mutableSetOf()
        when(filterCondition.objectClass) {
            Person::class.java.simpleName ->
            {
                val person = PersonController.getById(filterCondition.objectId)
                if (filterCondition.isIncluded) {
                    setOfShotsExt.forEach { currentShotExt ->
                        if (currentShotExt.personsExt.map{it.person.id}.contains(person.id)) {
                            when (filterCondition.subjectClass) {
                                Shot::class.java.simpleName -> {
                                    shotsExt.add(currentShotExt)
                                }
                                Scene::class.java.simpleName -> {
                                    currentShotExt.sceneExt?.shotsExt?.let { shotsExt.addAll(it) }
                                }
                                Event::class.java.simpleName -> {
                                    currentShotExt.eventsExt.forEach { eventExt ->
                                        eventExt.shotsExt.let { shotsExt.addAll(it) }
                                    }
                                }
                                File::class.java.simpleName -> {}
                                else -> {}
                            }
                        }
                    }
                } else {
                    shotsExt.addAll(setOfShotsExt)
                    setOfShotsExt.forEach { currentShotExt ->
                        if (currentShotExt.personsExt.map{it.person.id}.contains(person.id)) {
                            when (filterCondition.subjectClass) {
                                Shot::class.java.simpleName -> {
                                    shotsExt.remove(currentShotExt)
                                }
                                Scene::class.java.simpleName -> {
                                    currentShotExt.sceneExt?.shotsExt?.let { shotsExt.removeAll(it.toSet()) }
                                }
                                Event::class.java.simpleName -> {
                                    currentShotExt.eventsExt.forEach { eventExt ->
                                        eventExt.shotsExt.let { shotsExt.removeAll(it.toSet()) }
                                    }
                                }
                                File::class.java.simpleName -> {}
                                else -> {}
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        return shotsExt
    }

}