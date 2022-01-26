package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.ShotTypeSize
import com.svoemesto.ivfx.enums.TagType
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Tag
import org.springframework.stereotype.Controller

@Controller
class TagController {
    companion object {

        fun create(parentClass: String,
                   parentId: Long,
                   name: String = "",
                   tagType: TagType = TagType.NONE,
                   childClass: String = "",
                   childId: Long = 0,
                   sizeType: ShotTypeSize = ShotTypeSize.NONE,
                   proba: Double = 0.0): Tag {
            val entity = Tag()
            entity.parentClass = parentClass
            entity.parentId = parentId
            entity.childClass = childClass
            entity.childId = childId
            entity.tagType = tagType
            entity.sizeType = sizeType
            entity.proba = proba
            val lastEntity = Main.tagRepo.getEntityWithGreaterOrder(parentClass,parentId,tagType).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.tagType = tagType
            entity.name = if (name != "") name else "Tag $tagType #${entity.order} for $parentClass #$parentId"
            save(entity)

            return entity
        }

        fun getListAllTags(parentClass: String, parentId: Long): MutableList<Tag> {
            return Main.tagRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(parentClass, parentId,0).toMutableList()
        }

        fun getListScenes(file: File): MutableList<Tag> {
            return Main.tagRepo.getTagsByType(file::class.java.simpleName, file.id, TagType.SCENE).toMutableList()
        }

        fun save(tag: Tag) {
            Main.tagRepo.save(tag)
        }

        fun saveAll(tags: Iterable<Tag>) {
            tags.forEach { save(it) }
        }

        fun delete(tag: Tag) {

            getListAllTags(tag::class.java.simpleName, tag.id).forEach { delete(it) }

            reOrder(ReorderTypes.MOVE_TO_LAST, tag)

            PropertyController.deleteAll(tag::class.java.simpleName, tag.id)
            PropertyCdfController.deleteAll(tag::class.java.simpleName, tag.id)

            Main.tagRepo.delete(tag)
        }

        fun deleteAll(parentClass: String, parentId: Long) {
            getListAllTags(parentClass, parentId).forEach { delete(it) }
        }

        fun deleteAllScenes(file: File) {
            getListScenes(file).forEach { delete(it) }
        }

        fun reOrder(reorderType: ReorderTypes, tag: Tag) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.tagRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(tag.parentClass, tag.parentId, tag.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        tag.order += 1
                        save(tag)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.tagRepo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(tag.parentClass, tag.parentId, tag.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        tag.order -= 1
                        save(tag)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.tagRepo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(tag.parentClass, tag.parentId, tag.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    tag.order = 1
                    save(tag)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.tagRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(tag.parentClass, tag.parentId, tag.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        tag.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(tag)
                    }
                }
            }
        }

        fun getById(tagId: Long): Tag {
            return Main.tagRepo.findById(tagId).get()
        }

    }
}