package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.PropertyCdf
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import org.springframework.stereotype.Controller

@Controller
class PropertyCdfController() {

    fun getListProperties(parentClass: String, parentId: Long): List<PropertyCdf> {
        return Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(parentClass, parentId, Main.ccid, 0).toList()
    }

    fun getOrCreate(parentClass: String?, parentId: Long, key: String): String {
        var entity = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(parentClass!!, parentId, Main.ccid, key).firstOrNull()
        if (entity == null) {
            entity = PropertyCdf()
            entity.computerId = Main.ccid
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = Main.propertyCdfRepo.getEntityWithGreaterOrder(parentClass, parentId, Main.ccid).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = ""
            Main.propertyCdfRepo.save(entity)
        }
        return entity.value
    }

    fun editOrCreate(parentClass: String?, parentId: Long, key: String = "", value: String = ""): PropertyCdf {

        var entity = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(parentClass!!, parentId, Main.ccid, key).firstOrNull()
        if (entity != null) {
            entity.value = value
        } else {
            entity = PropertyCdf()
            entity.computerId = Main.ccid
            entity.parentClass = parentClass
            entity.parentId = parentId
            val lastEntity = Main.propertyCdfRepo.getEntityWithGreaterOrder(parentClass, parentId, Main.ccid).firstOrNull()
            entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
            entity.key = if (key == "") "Key # ${entity.order}" else key
            entity.value = value
        }
        save(entity)
        return entity

    }

    fun delete(propertyCdf: PropertyCdf) {
        reOrder(ReorderTypes.MOVE_TO_LAST, propertyCdf)
        Main.propertyCdfRepo.delete(propertyCdf.id)
    }

    fun deleteAll(parentClass: String, parentId: Long) {
        Main.propertyCdfRepo.deleteAll(parentClass, parentId)
    }

    fun save(propertyCdf: PropertyCdf) {
        Main.propertyCdfRepo.save(propertyCdf)
    }

    fun saveAll(propertiesCdf: Iterable<PropertyCdf>) {
        Main.propertyCdfRepo.saveAll(propertiesCdf)
    }

    fun reOrder(reorderType: ReorderTypes, propertyCdf: PropertyCdf) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    propertyCdf.order += 1
                    save(propertyCdf)
                    save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndOrderLessThanOrderByOrderDesc(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    propertyCdf.order -= 1
                    save(propertyCdf)
                    save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndOrderLessThanOrderByOrderDesc(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order)
                previousEntities.forEach{it.order++}
                saveAll(previousEntities)
                propertyCdf.order = 1
                save(propertyCdf)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder(propertyCdf.parentClass, propertyCdf.parentId, Main.ccid, propertyCdf.order)
                nextEntities.forEach{it.order--}
                saveAll(nextEntities)
                propertyCdf.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                save(propertyCdf)
            }
        }
    }

    fun getKeys(parentClass: String, computerId: Int): Iterable<String> {
        return Main.propertyCdfRepo.getKeys(parentClass,computerId).toList()
    }

}