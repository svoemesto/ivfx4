package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.enums.ShotTypeSize
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.models.TagNode
import org.springframework.stereotype.Controller

@Controller
class TagNodeController {
    companion object {

        fun create(tag: Tag,
                   parentId: Long,
                   parentClass: String,
                   name: String = "",
                   shotTypeSize: ShotTypeSize = ShotTypeSize.NONE,
                   proba: Double = 0.0): TagNode {
            val entity = TagNode()
            entity.tag = tag
            entity.parentId = parentId
            entity.parentClass = parentClass
            entity.typeSize = shotTypeSize
            entity.proba = proba
            val lastEntityForTag = Main.tagNodeRepo.getEntityWithGreaterOrderForTag(tag.id, parentClass).firstOrNull()
            val lastEntityForParent = Main.tagNodeRepo.getEntityWithGreaterOrderForTag(parentId, parentClass).firstOrNull()
            entity.orderForTag = if (lastEntityForTag != null) lastEntityForTag.orderForTag + 1 else 1
            entity.orderForParent = if (lastEntityForParent != null) lastEntityForParent.orderForParent + 1 else 1
            entity.name = if (name != "") name else "Tag node #${entity.orderForTag}/${entity.orderForParent}"
            save(entity)

            return entity
        }

        fun getListTagsNodesForTag(tag: Tag, parentClass: String = ""): MutableList<TagNode> {
            val result = if (parentClass == "")
                Main.tagNodeRepo.findByTagIdAndOrderForTagGreaterThanOrderByOrderForTag(tag.id, 0).toMutableList()
            else
                Main.tagNodeRepo.findByTagIdAndParentClassAndOrderForTagGreaterThanOrderByOrderForTag(tag.id, parentClass,0).toMutableList()
            result.forEach { tagNode ->
                tagNode.tag = tag
            }
            return result
        }

        fun getListTagsNodesForParent(parentId: Long, parentClass: String): MutableList<TagNode> {
            return Main.tagNodeRepo.findByParentIdAndParentClassAndOrderForParentGreaterThanOrderByOrderForParent(
                parentId,
                parentClass,
                0
            ).toMutableList()
        }

        fun save(tagNode: TagNode) {
            Main.tagNodeRepo.save(tagNode)
        }

        fun saveAll(tagsNodes: Iterable<TagNode>) {
            tagsNodes.forEach { save(it) }
        }

        fun delete(tagNode: TagNode) {
            reOrderForTag(ReorderTypes.MOVE_TO_LAST, tagNode)
            reOrderForParent(ReorderTypes.MOVE_TO_LAST, tagNode)

            PropertyController.deleteAll(tagNode::class.java.simpleName, tagNode.id)
            PropertyCdfController.deleteAll(tagNode::class.java.simpleName, tagNode.id)

            Main.tagNodeRepo.delete(tagNode)
        }

        fun deleteAll(tag: Tag) {
            getListTagsNodesForTag(tag).forEach { delete(it) }
        }

        fun deleteAll(parentClass: String, parentId: Long) {
            getListTagsNodesForParent(parentId, parentClass).forEach { delete(it) }
        }

        fun reOrderForTag(reorderType: ReorderTypes, tagNode: TagNode) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.tagNodeRepo.findByTagIdAndParentClassAndOrderForTagGreaterThanOrderByOrderForTag(tagNode.tag.id, tagNode.parentClass, tagNode.orderForTag).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.orderForTag -= 1
                        tagNode.orderForTag += 1
                        save(tagNode)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.tagNodeRepo.findByTagIdAndParentClassAndOrderForTagLessThanOrderByOrderForTagDesc(tagNode.tag.id, tagNode.parentClass, tagNode.orderForTag).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.orderForTag += 1
                        tagNode.orderForTag -= 1
                        save(tagNode)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.tagNodeRepo.findByTagIdAndParentClassAndOrderForTagLessThanOrderByOrderForTagDesc(tagNode.tag.id, tagNode.parentClass, tagNode.orderForTag)
                    previousEntities.forEach{it.orderForTag++}
                    saveAll(previousEntities)
                    tagNode.orderForTag = 1
                    save(tagNode)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.tagNodeRepo.findByTagIdAndParentClassAndOrderForTagGreaterThanOrderByOrderForTag(tagNode.tag.id, tagNode.parentClass, tagNode.orderForTag).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.orderForTag--}
                        saveAll(nextEntities)
                        tagNode.orderForTag = (nextEntities.lastOrNull()?.orderForTag ?: 0) + 1
                        save(tagNode)
                    }
                }
            }
        }

        fun reOrderForParent(reorderType: ReorderTypes, tagNode: TagNode) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.tagNodeRepo.findByParentIdAndParentClassAndOrderForParentGreaterThanOrderByOrderForParent(tagNode.parentId, tagNode.parentClass, tagNode.orderForParent).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.orderForParent -= 1
                        tagNode.orderForParent += 1
                        save(tagNode)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.tagNodeRepo.findByParentIdAndParentClassAndOrderForParentLessThanOrderByOrderForParentDesc(tagNode.parentId, tagNode.parentClass, tagNode.orderForParent).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.orderForParent += 1
                        tagNode.orderForParent -= 1
                        save(tagNode)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.tagNodeRepo.findByParentIdAndParentClassAndOrderForParentLessThanOrderByOrderForParentDesc(tagNode.parentId, tagNode.parentClass, tagNode.orderForParent)
                    previousEntities.forEach{it.orderForParent++}
                    saveAll(previousEntities)
                    tagNode.orderForParent = 1
                    save(tagNode)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.tagNodeRepo.findByParentIdAndParentClassAndOrderForParentGreaterThanOrderByOrderForParent(tagNode.parentId, tagNode.parentClass, tagNode.orderForParent).toList()
                    if (nextEntities.isNotEmpty()) {
                        nextEntities.forEach{it.orderForParent--}
                        saveAll(nextEntities)
                        tagNode.orderForParent = (nextEntities.lastOrNull()?.orderForParent ?: 0) + 1
                        save(tagNode)
                    }
                }
            }
        }

    }
}