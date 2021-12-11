package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.models.TagChild
import org.springframework.stereotype.Controller

@Controller
class TagChildController {
    companion object {

        fun getListTagsChilds(tag: Tag, childClass: String = ""): MutableList<TagChild> {
            val result = if (childClass == "")
                Main.tagChildRepo.findByTagIdAndChildClassAndOrderGreaterThanOrderByOrder(tag.id, childClass,0).toMutableList()
            else
                Main.tagChildRepo.findByTagIdAndOrderGreaterThanOrderByOrder(tag.id,0).toMutableList()
            result.forEach { tagChild ->
                tagChild.tag = tag
            }
            return result
        }

        fun save(tagChild: TagChild) {
            Main.tagChildRepo.save(tagChild)
        }

        fun saveAll(tagChilds: Iterable<TagChild>) {
            tagChilds.forEach { save(it) }
        }

        fun delete(tagChild: TagChild) {
            reOrder(ReorderTypes.MOVE_TO_LAST, tagChild)

            PropertyController.deleteAll(tagChild::class.java.simpleName, tagChild.id)
            PropertyCdfController.deleteAll(tagChild::class.java.simpleName, tagChild.id)

            Main.tagChildRepo.delete(tagChild)
        }

        fun deleteAll(tag: Tag) {
            getListTagsChilds(tag).forEach { delete(it) }
        }

        fun reOrder(reorderType: ReorderTypes, tagChild: TagChild) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.tagChildRepo.findByTagIdAndChildClassAndOrderGreaterThanOrderByOrder(tagChild.tag.id, tagChild.childClass, tagChild.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        tagChild.order += 1
                        save(tagChild)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.tagChildRepo.findByTagIdAndChildClassAndOrderLessThanOrderByOrderDesc(tagChild.tag.id, tagChild.childClass, tagChild.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        tagChild.order -= 1
                        save(tagChild)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.tagChildRepo.findByTagIdAndChildClassAndOrderLessThanOrderByOrderDesc(tagChild.tag.id, tagChild.childClass, tagChild.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    tagChild.order = 1
                    save(tagChild)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.tagChildRepo.findByTagIdAndChildClassAndOrderGreaterThanOrderByOrder(tagChild.tag.id, tagChild.childClass, tagChild.order).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.order--}
                        saveAll(nextEntities)
                        tagChild.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                        save(tagChild)
                    }
                }
            }
        }

    }
}