package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.TagType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
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
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_tags")
class Tag {

    @NotNull(message = "ID тэга не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "order_tag", nullable = false)
    var order: Int = 0

    @Column(name = "name")
    var name: String = ""

    @Column(name = "tag_type")
    var tagType: TagType = TagType.DESCRIPTION

}