package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller

@Controller
class FilterController {
    companion object {

        fun getSetFilters(project: Project): MutableSet<Filter> {
            return Main.filterRepo.findByProjectId(project.id).map {
                it.project = project
                it.filterGroups = FilterGroupController.getSetFilterGroups(it)
                it
            }.toMutableSet()
        }

        fun getFilterExt(projectExt: ProjectExt, filterId: Long): FilterExt {
            val filter = Main.filterRepo.findById(filterId).get()
            filter.project = projectExt.project
            filter.filterGroups = FilterGroupController.getSetFilterGroups(filter)
            return FilterExt(filter)
        }

        fun getFilterExt(projectExt: ProjectExt, filterName: String): FilterExt {
            var filter = Main.filterRepo.findByProjectIdAndName(projectExt.project.id, filterName).firstOrNull()
            if (filter == null) {
                filter = create(projectExt.project,true)
                filter.name = filterName
                save(filter)
            }
            filter.project = projectExt.project
            filter.filterGroups = FilterGroupController.getSetFilterGroups(filter)
            return FilterExt(filter)
        }

        fun deleteFilterExt(projectExt: ProjectExt, filterName: String) {
            val filter = Main.filterRepo.findByProjectIdAndName(projectExt.project.id, filterName).firstOrNull()
            if (filter != null) {
                filter.project = projectExt.project
                filter.filterGroups = FilterGroupController.getSetFilterGroups(filter)
                delete(filter)
            }
        }

        fun getList(projectExt: ProjectExt): MutableList<FilterExt> {

            val filters = Main.filterRepo.findByProjectId(projectExt.project.id).toMutableList()
            val result = filters.map { filter ->
                filter.project = projectExt.project
                filter.filterGroups = FilterGroupController.getSetFilterGroups(filter)
                FilterExt(filter)
            }.toMutableList()
            result.sort()
            return result
        }

        fun save(filter: Filter) {
            Main.filterRepo.save(filter)
        }

        fun saveAll(filters: Iterable<Filter>) {
            filters.forEach { save(it) }
        }

        fun delete(filter: Filter) {
            reOrder(ReorderTypes.MOVE_TO_LAST, filter)
            FilterGroupController.deleteAll(filter)
            PropertyController.deleteAll(filter::class.java.simpleName, filter.id)
            PropertyCdfController.deleteAll(filter::class.java.simpleName, filter.id)
            filter.filterGroups.clear()
            Main.filterRepo.delete(filter)
        }

        fun deleteAll(project: Project) {
            project.filters.forEach { delete(it) }
        }

        fun create(project: Project, isAnd: Boolean = true): Filter {

            val entity = Filter()
            entity.project = project
            val lastEntity = Main.filterRepo.getEntityWithGreaterOrder(project.id).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.name = "Filter # ${entity.order}"
            entity.isAnd = isAnd
            entity.filterGroups = mutableSetOf()
            save(entity)

            return entity
        }


        fun reOrder(reorderType: ReorderTypes, filter: Filter) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.filterRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(filter.project.id, filter.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        filter.order += 1
                        save(filter)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.filterRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(filter.project.id, filter.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        filter.order -= 1
                        save(filter)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.filterRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(filter.project.id, filter.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    filter.order = 1
                    save(filter)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.filterRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(filter.project.id, filter.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        filter.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(filter)
                    }
                }
            }
        }

    }
}