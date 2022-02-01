package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.PropertyRepo
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class PropertyController() {

    companion object {

        fun getMapKeyValuesByParentClass(parentClass: String): MutableMap<String, MutableList<String>> {
            val mapPropVals: MutableMap<String, MutableList<String>> = mutableMapOf()
            val propsMap = Main.propertyRepo.findByParentClass(parentClass).groupBy { it.key }.toSortedMap()
            propsMap.forEach { propMap->
                val vals = propMap.value.map { it.value }.toMutableSet()
                vals.add("")
                mapPropVals[propMap.key] = vals.toMutableList()
                mapPropVals[propMap.key]?.sort()
            }
            return mapPropVals
        }

        fun getListProperties(parentClass: String, parentId: Long): List<Property> {
            return Main.propertyRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(parentClass, parentId, 0).toList()
        }

        fun getOrCreate(parentClass: String?, parentId: Long, key: String): String {
            var entity = Main.propertyRepo.findByParentClassAndParentIdAndKey(parentClass!!, parentId, key).firstOrNull()
            if (entity == null) {
                entity = Property()
                entity.parentClass = parentClass
                entity.parentId = parentId
                val lastEntity = Main.propertyRepo.getEntityWithGreaterOrder(parentClass, parentId).firstOrNull()
                entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
                entity.key = if (key == "") "Key # ${entity.order}" else key
                entity.value = ""
                save(entity)
            }
            return entity.value
        }

        fun editOrCreate(parentClass: String?, parentId: Long, key: String = "", value: String = ""): Property {

            var entity = Main.propertyRepo.findByParentClassAndParentIdAndKey(parentClass!!, parentId, key).firstOrNull()
            if (entity != null) {
                entity.value = value
            } else {
                entity = Property()
                entity.parentClass = parentClass
                entity.parentId = parentId
                val lastEntity = Main.propertyRepo.getEntityWithGreaterOrder(parentClass, parentId).firstOrNull()
                entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
                entity.key = if (key == "") "Key # ${entity.order}" else key
                entity.value = value
            }
            save(entity)
            return entity

        }

        fun delete(property: Property) {
            reOrder(ReorderTypes.MOVE_TO_LAST, property)
            Main.propertyRepo.delete(property.id)
        }

        fun deleteAll(parentClass: String, parentId: Long) {
            Main.propertyRepo.deleteAll(parentClass, parentId)
        }

        fun save(property: Property) {
            Main.propertyRepo.save(property)
        }

        fun saveAll(properties: Iterable<Property>) {
            Main.propertyRepo.saveAll(properties)
        }

        fun reOrder(reorderType: ReorderTypes, property: Property) {

            when (reorderType) {
                ReorderTypes.MOVE_DOWN -> {
                    val nextEntity = Main.propertyRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(property.parentClass, property.parentId, property.order).firstOrNull()
                    if (nextEntity != null) {
                        nextEntity.order -= 1
                        property.order += 1
                        save(property)
                        save(nextEntity)
                    }
                }
                ReorderTypes.MOVE_UP -> {
                    val previousEntity = Main.propertyRepo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(property.parentClass, property.parentId, property.order).firstOrNull()
                    if (previousEntity != null) {
                        previousEntity.order += 1
                        property.order -= 1
                        save(property)
                        save(previousEntity)
                    }
                }
                ReorderTypes.MOVE_TO_FIRST -> {
                    val previousEntities = Main.propertyRepo.findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(property.parentClass, property.parentId, property.order)
                    previousEntities.forEach{it.order++}
                    saveAll(previousEntities)
                    property.order = 1
                    save(property)
                }
                ReorderTypes.MOVE_TO_LAST -> {
                    val nextEntities = Main.propertyRepo.findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(property.parentClass, property.parentId, property.order)
                    nextEntities.forEach{it.order--}
                    saveAll(nextEntities)
                    property.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    save(property)
                }
            }
        }

        fun getKeys(parentClass: String): List<String> {
            return Main.propertyRepo.getKeys(parentClass).toList()
        }


    }



}