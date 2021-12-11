package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Tag
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface TagRepo : CrudRepository<Tag, Long> {
    fun findByProjectIdAndOrderGreaterThanOrderByOrder(projectId: Long, order: Int) : Iterable<Tag>
    fun findByProjectIdAndOrderLessThanOrderByOrderDesc(projectId: Long, order: Int): Iterable<Tag>

    @Query(value = "SELECT * FROM tbl_tags WHERE project_id = ? ORDER BY order_tag DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(projectId:Long) : Iterable<Tag>

    fun findByProjectId(projectId: Long): Iterable<Tag>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags WHERE id = ?", nativeQuery = true)
    fun delete(tagId:Long)

}