package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.PropertyCdf
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface PropertyCdfRepo : CrudRepository<PropertyCdf, Long>  {

    fun findByParentClassAndParentIdAndComputerIdAndOrderGreaterThanOrderByOrder
                (parentClass: String, parentId: Long, computerId: Int, order: Int) : Iterable<PropertyCdf>

    fun findByParentClassAndParentIdAndComputerIdAndOrderLessThanOrderByOrderDesc
                (parentClass: String, parentId: Long, computerId: Int, order: Int): Iterable<PropertyCdf>

    fun findByParentClassAndParentIdAndComputerIdAndKey
                (parentClass: String, parentId: Long, computerId: Int, key: String): Iterable<PropertyCdf>

    @Query(value = "SELECT * FROM tbl_properties_cdf WHERE" +
            " parent_class = ?1 AND parent_id = ?2 AND computer_id = ?3 ORDER BY order_property DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(parentClass: String, parentId:Long, computerId: Int) : Iterable<PropertyCdf>

    @Query(value = "select distinct property_key from tbl_properties_cdf where parent_class = ?1 AND computer_id = ?2 order by  order_property", nativeQuery = true)
    fun getKeys(parentClass: String, computerId: Int) : Iterable<String>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_properties_cdf WHERE parent_class = ?1 AND parent_id = ?2", nativeQuery = true)
    fun deleteAll(parentClass: String, parentId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_properties_cdf WHERE id = ?", nativeQuery = true)
    fun delete(propertyCdfId: Long)

}