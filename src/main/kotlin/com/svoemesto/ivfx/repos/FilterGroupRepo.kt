package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterGroupRepo : CrudRepository<FilterGroup, Long> {

    fun findByFilterIdAndOrderGreaterThanOrderByOrder(projectId: Long, order: Int) : Iterable<FilterGroup>
    fun findByFilterIdAndOrderLessThanOrderByOrderDesc(projectId: Long, order: Int): Iterable<FilterGroup>

    @Query(value = "SELECT * FROM tbl_filters_groups WHERE filter_id = ? ORDER BY order_filter_group DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(filterId:Long) : Iterable<FilterGroup>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_groups WHERE filter_id = ?", nativeQuery = true)
    fun deleteAll(filterId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_groups WHERE id = ?", nativeQuery = true)
    fun delete(filterGroupId:Long)
    fun findByFilterId(filterId: Long): Iterable<FilterGroup>

}