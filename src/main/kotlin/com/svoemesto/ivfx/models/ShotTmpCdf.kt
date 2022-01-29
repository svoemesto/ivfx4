package com.svoemesto.ivfx.models

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Component
@Entity
@Table(name = "tbl_shots_tmp_cdf")
@Transactional
class ShotTmpCdf {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "computer_id", columnDefinition = "int default 0")
    var computerId: Int = 0

    @Column(name = "shot_id", nullable = false, columnDefinition = "bigint default 0")
    var shotId: Long = 0

}