package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.TagType
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Tag
import javafx.fxml.FXML
import org.springframework.stereotype.Controller

@Controller
class TagController {
    companion object {

        fun create(project: Project, name: String = "", tagType: TagType = TagType.DESCRIPTION): Tag {
            val entity = Tag()
            entity.project = project
            val lastEntity = Main.tagRepo.getEntityWithGreaterOrder(project.id).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.tagType = tagType
            entity.name = if (name != "") name else "Tag #${entity.order}: ${tagType.name}"
            save(entity)

            return entity
        }



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