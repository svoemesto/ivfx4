package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Tag
import com.svoemesto.ivfx.models.TagChild
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface TagChildRepo : CrudRepository<TagChild, Long> {
    fun findByTagIdAndOrderGreaterThanOrderByOrder(tagId: Long, order: Int) : Iterable<TagChild>
    fun findByTagIdAndChildClassAndOrderGreaterThanOrderByOrder(tagId: Long, childClass: String, order: Int) : Iterable<TagChild>
    fun findByTagIdAndChildClassAndOrderLessThanOrderByOrderDesc(tagId: Long, childClass: String, order: Int): Iterable<TagChild>

    @Query(value = "SELECT * FROM tbl_tags_childs WHERE tag_id = ?1 AND child_id = ?2 AND child_class = ?3 ORDER BY order_tag_child DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(tagId:Long, childId: Long, childClass: String) : Iterable<TagChild>

    fun findByTagId(tagId: Long): Iterable<TagChild>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags_childs WHERE tag_id = ?", nativeQuery = true)
    fun deleteAll(tagId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags_childs WHERE id = ?", nativeQuery = true)
    fun delete(tagChildId:Long)

}