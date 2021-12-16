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
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_faces")
@Transactional
class Face {

    @NotNull(message = "ID face не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    lateinit var person: Person

    @Column(name = "face_number_in_frame", nullable = false, columnDefinition = "int default 0")
    var faceNumberInFrame: Int = 0

    @Column(name = "frame_number", nullable = false, columnDefinition = "int default 0")
    var frameNumber: Int = 0

    @Column(name = "person_recognized_name", nullable = false, columnDefinition = "varchar(255) default ''")
    var personRecognizedName: String = ""

    @Column(name = "recognize_probability")
    var recognizeProbability: Double = 0.0

    @Column(name = "start_x", nullable = false, columnDefinition = "int default 0")
    var startX: Int = 0

    @Column(name = "start_y", nullable = false, columnDefinition = "int default 0")
    var startY: Int = 0

    @Column(name = "end_x", nullable = false, columnDefinition = "int default 0")
    var endX: Int = 0

    @Column(name = "end_y", nullable = false, columnDefinition = "int default 0")
    var endY: Int = 0

    @Column(name = "is_confirmed", columnDefinition = "boolean default false")
    var isConfirmed: Boolean = false

    @Lob
    @Column(name = "vector")
    var vectorText: String = "0.0"

    var vector: DoubleArray
        get() {
            val textVector: Array<String> = vectorText.split("\\|".toRegex()).toTypedArray()
            val result = DoubleArray(textVector.size)
            for (i in textVector.indices) {
                result[i] = textVector[i].toDouble()
            }
            return result
        }
        set(value) {
            vectorText = if (vector.isEmpty()) "" else value.joinToString(separator = "|", prefix = "", postfix = "")
            }
}