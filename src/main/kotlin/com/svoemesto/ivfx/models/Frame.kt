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
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "frame_number", nullable = false)
    var frameNumber: Int = 0

    @Column(name = "is_iframe")
    var isIFrame: Boolean = false

    @Column(name = "is_find")
    var isFind: Boolean = false

    @Column(name = "is_manual_add")
    var isManualAdd: Boolean = false

    @Column(name = "is_manual_cancel")
    var isManualCancel: Boolean = false

    @Column(name = "is_final_find")
    var isFinalFind: Boolean = false

    @Column(name = "sim_score_next_1")
    var simScoreNext1: Double = 0.0

    @Column(name = "sim_score_next_2")
    var simScoreNext2: Double = 0.0

    @Column(name = "sim_score_next_3")
    var simScoreNext3: Double = 0.0

    @Column(name = "sim_score_prev_1")
    var simScorePrev1: Double = 0.0

    @Column(name = "sim_score_prev_2")
    var simScorePrev2: Double = 0.0

    @Column(name = "sim_score_prev_3")
    var simScorePrev3: Double = 0.0

    @Column(name = "diff_next_1")
    var diffNext1: Double = 0.0

    @Column(name = "diff_next_2")
    var diffNext2: Double = 0.0

    @Column(name = "diff_prev_1")
    var diffPrev1: Double = 0.0

    @Column(name = "diff_prev_2")
    var diffPrev2: Double = 0.0

}