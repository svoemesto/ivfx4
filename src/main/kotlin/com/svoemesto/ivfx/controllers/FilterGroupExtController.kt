package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller

@Controller
class FilterGroupExtController {

    companion object {

        fun getList(projectExt: ProjectExt): MutableList<FilterGroupExt> {
            return Main.filterGroupRepo.findByProjectId(projectExt.project.id).map {filterGroup ->
                filterGroup.project = projectExt.project
                filterGroup.filterConditions.forEach { it.project = projectExt.project }
                filterGroup.filterFilters.forEach { it.project = projectExt.project }
                FilterGroupExt(filterGroup)
            }.toMutableList()
        }

        fun save(filterGroupExt: FilterGroupExt) {
            Main.filterGroupRepo.save(filterGroupExt.filterGroup)
        }

        fun saveAll(filterGroupsExt: Iterable<FilterGroupExt>) {
            filterGroupsExt.forEach { save(it) }
        }

        fun delete(filterGroupExt: FilterGroupExt) {

            PropertyController.deleteAll(filterGroupExt::class.java.simpleName, filterGroupExt.filterGroup.id)
            PropertyCdfController.deleteAll(filterGroupExt::class.java.simpleName, filterGroupExt.filterGroup.id)

            filterGroupExt.filterGroup.filterConditions.clear()
            filterGroupExt.filterGroup.filterFilters.clear()


            Main.filterGroupRepo.delete(filterGroupExt.filterGroup)
        }

        fun deleteAll(projectId: Long) {
            Main.filterGroupRepo.deleteAll(projectId)
        }

        fun create(project: Project, isAnd: Boolean = true): FilterGroupExt {
            val filterGroup = FilterGroup()
            filterGroup.project = project
            filterGroup.name = "Filter group # ${Main.filterGroupRepo.count()+1}"
            filterGroup.isAnd = isAnd
            val filterGroupExt = FilterGroupExt(filterGroup)
            save(filterGroupExt)
            return filterGroupExt
        }

    }

}