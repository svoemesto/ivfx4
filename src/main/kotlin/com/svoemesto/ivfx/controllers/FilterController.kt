package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Filter
import org.springframework.stereotype.Controller

@Controller
class FilterController {
    companion object {

        fun save(filter: Filter) {
            Main.filterRepo.save(filter)
        }

        fun saveAll(filters: Iterable<Filter>) {
            filters.forEach { save(it) }
        }

        fun delete(filter: Filter) {

            PropertyController.deleteAll(filter::class.java.simpleName, filter.id)
            PropertyCdfController.deleteAll(filter::class.java.simpleName, filter.id)

            filter.filterGroups.clear()

            Main.filterRepo.delete(filter)
        }

        fun deleteAll(projectId: Long) {
            Main.filterRepo.deleteAll(projectId)
        }

    }
}