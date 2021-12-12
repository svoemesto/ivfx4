package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypeSize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_tags_nodes")
@Transactional
class TagNode {

    @NotNull(message = "ID TagNode не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "tag_node_type_size", columnDefinition = "int default 0")
    var typeSize: ShotTypeSize = ShotTypeSize.NONE

    @Column(name = "tag_node_proba")
    var proba: Double = 0.0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    lateinit var tag: Tag

    @Column(name = "order_tag_node_for_tag", nullable = false, columnDefinition = "int default 0")
    var orderForTag: Int = 0

    @Column(name = "order_tag_node_for_parent", nullable = false, columnDefinition = "int default 0")
    var orderForParent: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "parent_id", columnDefinition = "int default 0")
    var parentId: Long = 0

    @Column(name = "parent_class", columnDefinition = "varchar(255) default ''")
    var parentClass: String = ""

}