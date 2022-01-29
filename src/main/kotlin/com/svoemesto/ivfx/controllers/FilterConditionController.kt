package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import org.springframework.stereotype.Controller

@Controller
class FilterConditionController {

    companion object {

        fun getSetFilterConditions(filterGroup: FilterGroup): MutableSet<FilterCondition> {
            return Main.filterConditionRepo.findByFilterGroupId(filterGroup.id).map {
                it.filterGroup = filterGroup
                it
            }.toMutableSet()
        }

        fun getList(filterGroupExt: FilterGroupExt): MutableList<FilterConditionExt> {

            val filterConditions = Main.filterConditionRepo.findByFilterGroupId(filterGroupExt.filterGroup.id).toMutableList()
            val result = filterConditions.map { filterCondition ->
                filterCondition.filterGroup = filterGroupExt.filterGroup
                FilterConditionExt(filterCondition)
            }.toMutableList()
            result.sort()
            return result

        }

        fun create(filterGroup: FilterGroup, name: String, objectId: Long, objectName: String, objectClass: String, subjectClass: String, isIncluded: Boolean): FilterCondition {

            val entity = FilterCondition()
            entity.filterGroup = filterGroup
            val lastEntity = Main.filterConditionRepo.getEntityWithGreaterOrder(filterGroup.id).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.name = name
            entity.objectId = objectId
            entity.objectName = objectName
            entity.objectClass = objectClass
            entity.subjectClass = subjectClass
            entity.isIncluded = isIncluded
            save(entity)

            return entity

        }

        fun save(filterCondition: FilterCondition) {
            Main.filterConditionRepo.save(filterCondition)
        }

        fun saveAll(filterConditions: Iterable<FilterCondition>) {
            filterConditions.forEach { save(it) }
        }

        fun delete(filterCondition: FilterCondition) {

            reOrder(ReorderTypes.MOVE_TO_LAST, filterCondition)
            PropertyController.deleteAll(filterCondition::class.java.simpleName, filterCondition.id)
            PropertyCdfController.deleteAll(filterCondition::class.java.simpleName, filterCondition.id)

            Main.filterConditionRepo.delete(filterCondition)

        }

        fun deleteAll(filterGroup: FilterGroup) {
            filterGroup.filterConditions.forEach { delete(it) }
        }

        fun reOrder(reorderType: ReorderTypes, filterCondition: FilterCondition) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.filterConditionRepo.findByFilterGroupIdAndOrderGreaterThanOrderByOrder(filterCondition.filterGroup.id, filterCondition.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        filterCondition.order += 1
                        save(filterCondition)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.filterConditionRepo.findByFilterGroupIdAndOrderLessThanOrderByOrderDesc(filterCondition.filterGroup.id, filterCondition.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        filterCondition.order -= 1
                        save(filterCondition)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.filterConditionRepo.findByFilterGroupIdAndOrderLessThanOrderByOrderDesc(filterCondition.filterGroup.id, filterCondition.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    filterCondition.order = 1
                    save(filterCondition)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.filterConditionRepo.findByFilterGroupIdAndOrderGreaterThanOrderByOrder(filterCondition.filterGroup.id, filterCondition.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        filterCondition.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(filterCondition)
                    }
                }
            }
        }

    }

}