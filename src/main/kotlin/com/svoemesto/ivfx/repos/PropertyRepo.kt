package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Property
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface PropertyRepo : CrudRepository<Property, Long> {

    fun findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder
                (parentClass: String, parentId: Long, order: Int) : Iterable<Property>

    fun findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc
                (parentClass: String, parentId: Long, order: Int): Iterable<Property>

    fun findByParentClassAndParentIdAndKey
                (parentClass: String, parentId: Long, key: String): Iterable<Property>

    fun findByParentClassAndParentId(parentClass: String, id: Long): Iterable<Property>

    @Query(value = "SELECT * FROM tbl_properties WHERE" +
            " parent_class = ?1 AND parent_id = ?2 ORDER BY order_property DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(parentClass: String, parentId:Long) : Iterable<Property>

    @Query(value = "select distinct property_key from tbl_properties where parent_class = ? order by  order_property", nativeQuery = true)
    fun getKeys(parentClass: String) : Iterable<String>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_properties WHERE parent_class = ?1 AND parent_id = ?2", nativeQuery = true)
    fun deleteAll(parentClass: String, parentId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_properties WHERE id = ?", nativeQuery = true)
    fun delete(propertyId: Long)

}