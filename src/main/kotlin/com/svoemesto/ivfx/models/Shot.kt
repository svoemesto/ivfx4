package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypePerson
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
@Table(name = "tbl_shots")
@Transactional
class Shot {

    @NotNull(message = "ID плана не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "shot_type_size", columnDefinition = "varchar(255) default 'NONE'")
    var typeSize: ShotTypeSize = ShotTypeSize.NONE

    @Column(name = "shot_type_person", columnDefinition = "varchar(255) default 'NONE'")
    var typePerson: ShotTypePerson = ShotTypePerson.NONE

    @Column(name = "first_frame_number", columnDefinition = "int default 0")
    var firstFrameNumber: Int = 0

    @Column(name = "last_frame_number", columnDefinition = "int default 0")
    var lastFrameNumber: Int = 0

    @Column(name = "nearest_i_frame", columnDefinition = "int default 0")
    var nearestIFrame: Int = 0

    @Column(name = "is_body_scene", columnDefinition = "boolean default false")
    var isBodyScene: Boolean = false

    @Column(name = "is_start_scene", columnDefinition = "boolean default false")
    var isStartScene: Boolean = false

    @Column(name = "is_end_scene", columnDefinition = "boolean default false")
    var isEndScene: Boolean = false

    @Column(name = "is_body_event", columnDefinition = "boolean default false")
    var isBodyEvent: Boolean = false

    @Column(name = "is_start_event", columnDefinition = "boolean default false")
    var isStartEvent: Boolean = false

    @Column(name = "is_end_event", columnDefinition = "boolean default false")
    var isEndEvent: Boolean = false
}