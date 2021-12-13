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
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_events_shots")
@Transactional
class EventShot {
    @NotNull(message = "ID EventShot не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    lateinit var event: Event

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shot_id")
    lateinit var shot: Shot

}