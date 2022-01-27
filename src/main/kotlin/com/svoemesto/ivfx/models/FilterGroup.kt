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
class FilterGroup: Comparable<FilterGroup> {

    override fun compareTo(other: FilterGroup): Int {
        return this.order - other.order
    }

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_id")
    lateinit var filter: Filter

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "order_filter_group", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "is_and", columnDefinition = "boolean default true")
    var isAnd: Boolean = true

    @OneToMany(mappedBy = "filterGroup", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var filterConditions: MutableSet<FilterCondition> = mutableSetOf()

}