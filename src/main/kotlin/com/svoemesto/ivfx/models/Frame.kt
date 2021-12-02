package com.svoemesto.ivfx.models

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
@Table(name = "tbl_frames")
class Frame {

    @NotNull(message = "ID фрейма не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "frame_number", nullable = false, columnDefinition = "int default 0")
    var frameNumber: Int = 0

    @Column(name = "is_iframe", columnDefinition = "boolean default false")
    var isIFrame: Boolean = false

    @Column(name = "is_find", columnDefinition = "boolean default false")
    var isFind: Boolean = false

    @Column(name = "is_manual_add", columnDefinition = "boolean default false")
    var isManualAdd: Boolean = false

    @Column(name = "is_manual_cancel", columnDefinition = "boolean default false")
    var isManualCancel: Boolean = false

    @Column(name = "is_final_find", columnDefinition = "boolean default false")
    var isFinalFind: Boolean = false

    @Column(name = "sim_score_next_1", columnDefinition = "double default 0")
    var simScoreNext1: Double = 0.0

    @Column(name = "sim_score_next_2", columnDefinition = "double default 0")
    var simScoreNext2: Double = 0.0

    @Column(name = "sim_score_next_3", columnDefinition = "double default 0")
    var simScoreNext3: Double = 0.0

    @Column(name = "sim_score_prev_1", columnDefinition = "double default 0")
    var simScorePrev1: Double = 0.0

    @Column(name = "sim_score_prev_2", columnDefinition = "double default 0")
    var simScorePrev2: Double = 0.0

    @Column(name = "sim_score_prev_3", columnDefinition = "double default 0")
    var simScorePrev3: Double = 0.0

    @Column(name = "diff_next_1", columnDefinition = "double default 0")
    var diffNext1: Double = 0.0

    @Column(name = "diff_next_2", columnDefinition = "double default 0")
    var diffNext2: Double = 0.0

    @Column(name = "diff_prev_1", columnDefinition = "double default 0")
    var diffPrev1: Double = 0.0

    @Column(name = "diff_prev_2", columnDefinition = "double default 0")
    var diffPrev2: Double = 0.0

}