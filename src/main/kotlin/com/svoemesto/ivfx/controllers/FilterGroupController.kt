package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterExt
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import org.springframework.stereotype.Controller

@Controller
class FilterGroupController {

    companion object {

        fun getSetFilterGroups(filter: Filter): MutableSet<FilterGroup> {
            return Main.filterGroupRepo.findByFilterId(filter.id).map {
                it.filter = filter
                it.filterConditions = FilterConditionController.getSetFilterConditions(it)
                it
            }.toMutableSet()
        }

        fun getList(filterExt: FilterExt): MutableList<FilterGroupExt> {
            val filterGroups = Main.filterGroupRepo.findByFilterId(filterExt.filter.id).toMutableList()
            val result = filterGroups.map { filterGroup ->
                filterGroup.filter = filterExt.filter
                filterGroup.filterConditions = FilterConditionController.getSetFilterConditions(filterGroup)
                FilterGroupExt(filterGroup)
            }.toMutableList()
            result.sort()
            return result
        }

        fun save(filterGroup: FilterGroup) {
            Main.filterGroupRepo.save(filterGroup)
        }

        fun saveAll(filterGroups: Iterable<FilterGroup>) {
            filterGroups.forEach { save(it) }
        }

        fun delete(filterGroup: FilterGroup) {

            reOrder(ReorderTypes.MOVE_TO_LAST, filterGroup)
            FilterConditionController.deleteAll(filterGroup)
            PropertyController.deleteAll(filterGroup::class.java.simpleName, filterGroup.id)
            PropertyCdfController.deleteAll(filterGroup::class.java.simpleName, filterGroup.id)
            filterGroup.filterConditions.clear()
            Main.filterGroupRepo.delete(filterGroup)

        }

        fun deleteAll(filter: Filter) {
            filter.filterGroups.forEach { delete(it) }
        }

        fun create(filter: Filter, isAnd: Boolean = true): FilterGroup {

            val entity = FilterGroup()
            entity.filter = filter
            val lastEntity = Main.filterGroupRepo.getEntityWithGreaterOrder(filter.id).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.name = "Filter group # ${entity.order}"
            entity.isAnd = isAnd
            entity.filterConditions = mutableSetOf()
            save(entity)

            return entity

        }

        fun reOrder(reorderType: ReorderTypes, filterGroup: FilterGroup) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.filterGroupRepo.findByFilterIdAndOrderGreaterThanOrderByOrder(filterGroup.filter.id, filterGroup.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        filterGroup.order += 1
                        save(filterGroup)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.filterGroupRepo.findByFilterIdAndOrderLessThanOrderByOrderDesc(filterGroup.filter.id, filterGroup.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        filterGroup.order -= 1
                        save(filterGroup)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.filterGroupRepo.findByFilterIdAndOrderLessThanOrderByOrderDesc(filterGroup.filter.id, filterGroup.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    filterGroup.order = 1
                    save(filterGroup)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.filterGroupRepo.findByFilterIdAndOrderGreaterThanOrderByOrder(filterGroup.filter.id, filterGroup.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        filterGroup.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(filterGroup)
                    }
                }
            }
        }

    }

}