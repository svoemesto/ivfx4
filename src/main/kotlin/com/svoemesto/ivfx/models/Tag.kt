package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypeSize
import com.svoemesto.ivfx.enums.TagType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_tags")
@Transactional
class Tag {

    @NotNull(message = "ID тэга не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "order_tag", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "tag_type", columnDefinition = "int default 0")
    var tagType: TagType = TagType.DESCRIPTION

    @Column(name = "parent_id", columnDefinition = "int default 0")
    var parentId: Long = 0

    @Column(name = "parent_class", columnDefinition = "varchar(255) default ''")
    var parentClass: String = ""

    @Column(name = "size_type", columnDefinition = "int default 0")
    var sizeType: ShotTypeSize = ShotTypeSize.NONE

    @Column(name = "tag_proba")
    var proba: Double = 0.0

}