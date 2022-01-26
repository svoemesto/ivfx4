package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller

@Controller
class FilterConditionExtController {

    companion object {

        fun getList(projectExt: ProjectExt): MutableList<FilterConditionExt> {
            return Main.filterConditionRepo.findByProjectId(projectExt.project.id).map { filterCondition ->
                filterCondition.project = projectExt.project
                filterCondition.filterGroups.forEach { it.project = projectExt.project }
                FilterConditionExt(filterCondition)
            }.toMutableList()
        }

        fun create(projectExt: ProjectExt, name: String, objectId: Long, objectClass: String, subjectClass: String, isIncluded: Boolean): FilterConditionExt {
            val filterCondition = FilterCondition()
            filterCondition.project = projectExt.project
            filterCondition.name = name
            filterCondition.objectId = objectId
            filterCondition.objectClass = objectClass
            filterCondition.subjectClass = subjectClass
            filterCondition.isIncluded = isIncluded
            val filterConditionExt = FilterConditionExt(filterCondition)
            save(filterConditionExt)
            return filterConditionExt
        }

        fun save(filterConditionExt: FilterConditionExt) {
            Main.filterConditionRepo.save(filterConditionExt.filterCondition)
        }

        fun saveAll(filterConditionsExt: Iterable<FilterConditionExt>) {
            filterConditionsExt.forEach { save(it) }
        }

        fun delete(filterConditionExt: FilterConditionExt) {

            PropertyController.deleteAll(filterConditionExt::class.java.simpleName, filterConditionExt.filterCondition.id)
            PropertyCdfController.deleteAll(filterConditionExt::class.java.simpleName, filterConditionExt.filterCondition.id)

            filterConditionExt.filterCondition.filterGroups.clear()

            Main.filterConditionRepo.delete(filterConditionExt.filterCondition)
        }

        fun deleteAll(projectId: Long) {
            Main.filterConditionRepo.deleteAll(projectId)
        }

    }

}