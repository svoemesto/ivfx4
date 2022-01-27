package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.controllers.PersonController
import com.svoemesto.ivfx.models.Event
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot

class FilterConditionExt(var filterCondition: FilterCondition): Comparable<FilterConditionExt> {

    override fun compareTo(other: FilterConditionExt): Int {
        return this.filterCondition.order - other.filterCondition.order
    }

    val name: String get() = filterCondition.name
    val order: Int get() = filterCondition.order

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