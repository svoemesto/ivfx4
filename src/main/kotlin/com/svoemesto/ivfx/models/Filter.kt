package com.svoemesto.ivfx.models

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.Table

@Component
@Entity
@Table(name = "tbl_filters")
@Transactional
class Filter {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "is_and", columnDefinition = "boolean default true")
    var isAnd: Boolean = true

    @ManyToMany
    @JoinTable(
        name = "tbl_filters_filters_groups",
        joinColumns = [JoinColumn(name = "filter_id")],
        inverseJoinColumns = [JoinColumn(name = "filter_group_id")]
    )
    var filterGroups: MutableSet<FilterGroup> = mutableSetOf()

}