package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller

@Controller
class FilterConditionController {

    companion object {

        fun create(projectExt: ProjectExt, name: String, objectId: Long, objectClass: String, subjectClass: String, isIncluded: Boolean): FilterCondition {
            val filterCondition = FilterCondition()
            filterCondition.project = projectExt.project
            filterCondition.name = name
            filterCondition.objectId = objectId
            filterCondition.objectClass = objectClass
            filterCondition.subjectClass = subjectClass
            filterCondition.isIncluded = isIncluded
            save(filterCondition)
            return filterCondition
        }

        fun save(filterCondition: FilterCondition) {
            Main.filterConditionRepo.save(filterCondition)
        }

        fun saveAll(filterConditions: Iterable<FilterCondition>) {
            filterConditions.forEach { save(it) }
        }

        fun delete(filterCondition: FilterCondition) {

            PropertyController.deleteAll(filterCondition::class.java.simpleName, filterCondition.id)
            PropertyCdfController.deleteAll(filterCondition::class.java.simpleName, filterCondition.id)

            filterCondition.filterGroups.clear()

            Main.filterConditionRepo.delete(filterCondition)
        }

        fun deleteAll(projectId: Long) {
            Main.filterConditionRepo.deleteAll(projectId)
        }

    }

}