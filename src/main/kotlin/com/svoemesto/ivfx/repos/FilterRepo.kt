package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Filter
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterRepo : CrudRepository<Filter, Long> {
    fun findByProjectIdAndOrderGreaterThanOrderByOrder(projectId: Long, order: Int) : Iterable<Filter>
    fun findByProjectIdAndOrderLessThanOrderByOrderDesc(projectId: Long, order: Int): Iterable<Filter>

    @Query(value = "SELECT * FROM tbl_filters WHERE project_id = ? ORDER BY order_filter DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(projectId:Long) : Iterable<Filter>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters WHERE id = ?", nativeQuery = true)
    fun delete(filterId:Long)
    fun findByProjectId(projectId: Long): Iterable<Filter>

}