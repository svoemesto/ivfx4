package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.FilterGroup
import org.springframework.stereotype.Controller

@Controller
class FilterGroupController {

    companion object {

        fun save(filterGroup: FilterGroup) {
            Main.filterGroupRepo.save(filterGroup)
        }

        fun saveAll(filterGroups: Iterable<FilterGroup>) {
            filterGroups.forEach { save(it) }
        }

        fun delete(filterGroup: FilterGroup) {

            PropertyController.deleteAll(filterGroup::class.java.simpleName, filterGroup.id)
            PropertyCdfController.deleteAll(filterGroup::class.java.simpleName, filterGroup.id)

            filterGroup.filterConditions.clear()
            filterGroup.filterFilters.clear()


            Main.filterGroupRepo.delete(filterGroup)
        }

        fun deleteAll(projectId: Long) {
            Main.filterGroupRepo.deleteAll(projectId)
        }

    }

}