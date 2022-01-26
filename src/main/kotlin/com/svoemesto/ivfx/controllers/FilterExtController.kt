package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller

@Controller
class FilterExtController {
    companion object {

        fun getList(projectExt: ProjectExt): MutableList<FilterExt> {
            return Main.filterRepo.findByProjectId(projectExt.project.id).map {filter ->
                filter.project = projectExt.project
                filter.filterGroups.forEach { it.project = projectExt.project }
                FilterExt(filter)
            }.toMutableList()
        }

        fun save(filterExt: FilterExt) {
            Main.filterRepo.save(filterExt.filter)
        }

        fun saveAll(filtersExt: Iterable<FilterExt>) {
            filtersExt.forEach { save(it) }
        }

        fun delete(filterExt: FilterExt) {

            PropertyController.deleteAll(filterExt::class.java.simpleName, filterExt.filter.id)
            PropertyCdfController.deleteAll(filterExt::class.java.simpleName, filterExt.filter.id)

            filterExt.filter.filterGroups.clear()

            Main.filterRepo.delete(filterExt.filter)
        }

        fun deleteAll(projectId: Long) {
            Main.filterRepo.deleteAll(projectId)
        }

        fun create(project: Project, isAnd: Boolean = true): FilterExt {
            val filter = Filter()
            filter.project = project
            filter.name = "Filter # ${Main.filterRepo.count()+1}"
            filter.isAnd = isAnd
            val filterExt = FilterExt(filter)
            save(filterExt)
            return filterExt
        }

    }
}