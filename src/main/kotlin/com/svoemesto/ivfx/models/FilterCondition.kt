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
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Component
@Entity
@Table(name = "tbl_filters_conditions")
@Transactional
class FilterCondition {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "object_id", nullable = false, columnDefinition = "int default 0")
    var objectId: Long = 0

    @Column(name = "object_class", columnDefinition = "varchar(255) default ''")
    var objectClass: String = ""

    @Column(name = "is_included", columnDefinition = "boolean default true")
    var isIncluded: Boolean = true

    @Column(name = "subject_class", columnDefinition = "varchar(255) default ''")
    var subjectClass: String = ""

    @ManyToMany(mappedBy = "filterConditions")
    var filterGroups: MutableSet<FilterGroup> = mutableSetOf()

}