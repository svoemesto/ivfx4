package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Tag
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface TagRepo : CrudRepository<Tag, Long> {
    fun findByParentClassAndParentIdAndOrderGreaterThanOrderByOrder(parentClass: String, parentId: Long, order: Int) : Iterable<Tag>
    fun findByParentClassAndParentIdAndOrderLessThanOrderByOrderDesc(parentClass: String, parentId: Long, order: Int): Iterable<Tag>

    @Query(value = "SELECT * FROM tbl_tags WHERE parent_class = ?1 AND parent_id = ?2 ORDER BY order_tag DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(parentClass: String, parentId:Long) : Iterable<Tag>

//    @Transactional
//    @Modifying
//    @Query(value = "DELETE FROM tbl_tags WHERE project_id = ?", nativeQuery = true)
//    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags WHERE id = ?", nativeQuery = true)
    fun delete(tagId:Long)

}