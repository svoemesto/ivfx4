package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.ReorderTypes
import com.svoemesto.ivfx.models.PropertyCdf
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import org.springframework.stereotype.Controller

@Controller
class PropertyCdfController(val repo: PropertyCdfRepo) {

    fun getListProperties(parentClass: String, parentId: Long): List<PropertyCdf> {
        return repo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(parentClass, parentId, Main.ccid, 0).toList()
    }

    fun getOrCreate(parentClass: String?, parentId: Long, key: String): String {
        var entity = repo.findByParentClassAndParentIdAndComputerIdAndKey(parentClass!!, parentId, Main.ccid, key).firstOrNull()
        if (entity == null) {
            entity = PropertyCdf()
            entity.computerId = Main.ccid
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = repo.getEntityWithGreaterOrder(parentClass, parentId, Main.ccid).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = ""
            repo.save(entity)
        }
        return entity.value
    }

    fun editOrCreate(parentClass: String?, parentId: Long, key: String = "", value: String = ""): PropertyCdf {

        var entity = repo.findByParentClassAndParentIdAndComputerIdAndKey(parentClass!!, parentId, Main.ccid, key).firstOrNull()
        if (entity != null) {
            entity.value = value
        } else {
            entity = PropertyCdf()
            entity.computerId = Main.ccid
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = repo.getEntityWithGreaterOrder(parentClass, parentId, Main.ccid).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = value
        }
        repo.save(entity)
        return entity

    }

    fun delete(propertyCdf: PropertyCdf) {
        reOrder(ReorderTypes.MOVE_TO_LAST, propertyCdf)
        repo.delete(propertyCdf.id)
    }

    fun reOrder(reorderType: ReorderTypes, propertyCdf: PropertyCdf) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = repo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    propertyCdf.order += 1
                    repo.save(propertyCdf)
                    repo.save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = repo.findByParentClassAndParentIdAndComputerIdAndOrderLessThanOrderByOrderDesc(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    propertyCdf.order -= 1
                    repo.save(propertyCdf)
                    repo.save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = repo.findByParentClassAndParentIdAndComputerIdAndOrderLessThanOrderByOrderDesc(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order)
                previousEntities.forEach{it.order++}
                repo.saveAll(previousEntities)
                propertyCdf.order = 1
                repo.save(propertyCdf)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = repo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order)
                nextEntities.forEach{it.order--}
                repo.saveAll(nextEntities)
                propertyCdf.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                repo.save(propertyCdf)
            }
        }
    }
}