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
import javax.validation.constraints.NotNull


@Component
@Entity
@Table(name = "tbl_events")
@Transactional
class Event: Comparable<Event> {

    override fun compareTo(other: Event): Int {
        return this.firstFrameNumber - other.firstFrameNumber
    }

    @NotNull(message = "ID Event не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "parent_id", nullable = false, columnDefinition = "bigint default 0")
    var parentId: Long = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "first_frame_number", columnDefinition = "int default 0")
    var firstFrameNumber: Int = 0

    @Column(name = "last_frame_number", columnDefinition = "int default 0")
    var lastFrameNumber: Int = 0

}