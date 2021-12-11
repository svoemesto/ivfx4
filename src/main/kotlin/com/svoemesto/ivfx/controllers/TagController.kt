package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Tag
import org.springframework.stereotype.Controller

@Controller
class TagController {
    companion object {

        fun getListTags(project: Project): MutableList<Tag> {
            val result = Main.tagRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(project.id,0).toMutableList()
            result.forEach { tag ->
                tag.project = project
                tag.childs = TagChildController.getListTagsChilds(tag)
            }
            return result
        }

        fun save(tag: Tag) {
            Main.tagRepo.save(tag)
        }

        fun saveAll(tags: Iterable<Tag>) {
            tags.forEach { save(it) }
        }

        fun delete(tag: Tag) {
            reOrder(ReorderTypes.MOVE_TO_LAST, tag)
            TagChildController.deleteAll(tag)

            PropertyController.deleteAll(tag::class.java.simpleName, tag.id)
            PropertyCdfController.deleteAll(tag::class.java.simpleName, tag.id)

            Main.tagRepo.delete(tag)
        }

        fun deleteAll(project: Project) {
            getListTags(project).forEach { tag ->
                delete(tag)
            }
        }

        fun reOrder(reorderType: ReorderTypes, tag: Tag) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.tagRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(tag.project.id, tag.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        tag.order += 1
                        save(tag)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.tagRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(tag.project.id, tag.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        tag.order -= 1
                        save(tag)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.tagRepo.findByProjectIdAndOrderLessThanOrderByOrderDesc(tag.project.id, tag.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    tag.order = 1
                    save(tag)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.tagRepo.findByProjectIdAndOrderGreaterThanOrderByOrder(tag.project.id, tag.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        tag.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(tag)
                    }
                }
            }
        }

    }
}