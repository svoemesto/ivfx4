package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.TagNode
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface TagNodeRepo : CrudRepository<TagNode, Long> {
//    fun findByTagIdAndOrderGreaterThanOrderByOrder(tagId: Long, order: Int) : Iterable<TagNode>
    fun findByTagIdAndOrderForTagGreaterThanOrderByOrderForTag(tagId: Long, orderForTag: Int) : Iterable<TagNode>
    fun findByTagIdAndParentClassAndOrderForTagGreaterThanOrderByOrderForTag(tagId: Long, parentClass: String, orderForTag: Int) : Iterable<TagNode>
    fun findByTagIdAndParentClassAndOrderForTagLessThanOrderByOrderForTagDesc(tagId: Long, parentClass: String, orderForTag: Int): Iterable<TagNode>

    fun findByParentIdAndParentClassAndOrderForParentGreaterThanOrderByOrderForParent(parentId: Long, parentClass: String, orderForParent: Int) : Iterable<TagNode>
    fun findByParentIdAndParentClassAndOrderForParentLessThanOrderByOrderForParentDesc(parentId: Long, parentClass: String, orderForParent: Int) : Iterable<TagNode>

    @Query(value = "SELECT * FROM tbl_tags_nodes WHERE tag_id = ?1 AND parent_class = ?2 ORDER BY order_tag_node_for_tag DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrderForTag(tagId:Long, parentClass: String) : Iterable<TagNode>

    @Query(value = "SELECT * FROM tbl_tags_nodes WHERE parent_id = ?1 AND parent_class = ?2 ORDER BY order_tag_node_for_parent DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrderForParent(parentId: Long, parentClass: String) : Iterable<TagNode>

    fun findByTagId(tagId: Long): Iterable<TagNode>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags_nodes WHERE tag_id = ?", nativeQuery = true)
    fun deleteAll(tagId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_tags_nodes WHERE id = ?", nativeQuery = true)
    fun delete(tagNodeId:Long)

}