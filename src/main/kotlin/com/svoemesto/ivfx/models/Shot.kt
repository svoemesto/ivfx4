package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.enums.ShotTypeSize
import org.springframework.stereotype.Component
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
class Shot {

    @NotNull(message = "ID плана не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "shot_type_size")
    var typeSize: ShotTypeSize = ShotTypeSize.NONE

    @Column(name = "shot_type_person")
    var typePerson: ShotTypePerson = ShotTypePerson.NONE

    @Column(name = "first_frame_number", nullable = false)
    var firstFrameNumber: Int = 0

    @Column(name = "last_frame_number", nullable = false)
    var lastFrameNumber: Int = 0

    @Column(name = "nearest_i_frame", nullable = false)
    var nearestIFrame: Int = 0


}