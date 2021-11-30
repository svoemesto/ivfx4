package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.ReorderTypes
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.PropertyRepo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class PropertyController(val repo: PropertyRepo) {

    fun getListProperties(parentClass: String, parentId: Long): List<Property> {
        return repo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(parentClass, parentId, 0).toList()
    }

    fun getOrCreate(parentClass: String?, parentId: Long, key: String): String {
        var entity = repo.findByParentClassAndParentIdAndKey(parentClass!!, parentId, key).firstOrNull()
        if (entity == null) {
            entity = Property()
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = repo.getEntityWithGreaterOrder(parentClass, parentId).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = ""
            repo.save(entity)
        }
        return entity.value
    }

    fun editOrCreate(parentClass: String?, parentId: Long, key: String = "", value: String = ""): Property {

        var entity = repo.findByParentClassAndParentIdAndKey(parentClass!!, parentId, key).firstOrNull()
        if (entity != null) {
            entity.value = value
        } else {
            entity = Property()
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = repo.getEntityWithGreaterOrder(parentClass, parentId).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = value
        }
        repo.save(entity)
        return entity

    }

    fun delete(property: Property) {
        reOrder(ReorderTypes.MOVE_TO_LAST, property)
        repo.delete(property.id)
    }

    fun reOrder(reorderType: ReorderTypes, property: Property) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = repo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(property.parentClass, property.parentId, property.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    property.order += 1
                    repo.save(property)
                    repo.save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = repo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(property.parentClass, property.parentId, property.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    property.order -= 1
                    repo.save(property)
                    repo.save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = repo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(property.parentClass, property.parentId, property.order)
                previousEntities.forEach{it.order++}
                repo.saveAll(previousEntities)
                property.order = 1
                repo.save(property)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = repo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(property.parentClass, property.parentId, property.order)
                nextEntities.forEach{it.order--}
                repo.saveAll(nextEntities)
                property.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                repo.save(property)
            }
        }
    }

}