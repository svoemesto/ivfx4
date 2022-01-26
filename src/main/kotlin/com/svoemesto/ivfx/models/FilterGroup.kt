package com.svoemesto.ivfx.models

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
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Component
@Entity
@Table(name = "tbl_filters_groups")
@Transactional
class FilterGroup {

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tbl_filters_groups_conditions",
        joinColumns = [JoinColumn(name = "filter_group_id")],
        inverseJoinColumns = [JoinColumn(name = "filter_condition_id")]
    )
    var filterConditions: MutableSet<FilterCondition> = mutableSetOf()

    @ManyToMany(mappedBy = "filterGroups", fetch = FetchType.EAGER)
    var filterFilters: MutableSet<Filter> = mutableSetOf()

}