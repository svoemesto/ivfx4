package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.utils.VectorBinary
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
class Face: Comparable<Face> {

    override fun compareTo(other: Face): Int {
        if (this.file.order != other.file.order) return this.file.order - other.file.order
        if (this.frameNumber != other.frameNumber) return this.frameNumber - other.frameNumber
        return this.faceNumberInFrame - other.faceNumberInFrame
    }

    @NotNull(message = "ID face не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    /**
     * Кадр, в котором найдено лицо.
     *
     * Раньше связь была выводимой: кадр искался по паре «серия + номер
     * кадра». Этого хватало, пока серия у лица никто не переписывал.
     * Авария 2026-10-08 переписала её у 45 136 лиц, и восстановление
     * держалось на одном файле на диске: указания на то, какому файлу
     * принадлежит кадр, в самой базе не осталось.
     *
     * Теперь ссылка записана и от серии не зависит.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_id")
    var frame: Frame? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    lateinit var person: Person

    /** Трек, в который входит это лицо: один и тот же человек в пределах
     *  одной сцены. Заполняется шагом отслеживания и необязателен: лицо может
     *  остаться вне трека, если оно ни с кем не сошлось. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    var track: FaceTrack? = null

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

    @Column(name = "is_example", columnDefinition = "boolean default false")
    var isExample: Boolean = false

    @Column(name = "is_manual", columnDefinition = "boolean default false")
    var isManual: Boolean = false

    /**
     * Вектор лица: 512 float32 little-endian подряд, ровно 2048 байт.
     *
     * Текстовая колонка `vector` **удалена 2026-10-09** (задача #268). Она
     * занимала 1 116 МБ против 227 МБ здесь и была источником самого
     * дорогого разбора в проекте: 512 `toDouble` на лицо регуляркой.
     *
     * Значение `null` означает «вектора нет» — так и должно быть у лица,
     * которому эмбеддинг не достался. Подставлять вместо него нули нельзя:
     * нулевой вектор неотличим от настоящего и тихо портит сходство.
     */
    @Column(name = "vector_bin")
    var vectorBinary: ByteArray? = null

    /** Есть ли вектор у лица. */
    fun hasVector(): Boolean = vectorBinary?.size == VectorBinary.BYTES

    /**
     * Вектор числами, либо `null`, если его нет.
     *
     * `null`, а не пустой массив: «нет данных» и «512 нулей» — разные вещи,
     * и путать их нельзя.
     */
    fun vectorFloats(): FloatArray? {
        val binary = vectorBinary ?: return null
        if (binary.size != VectorBinary.BYTES) return null
        return VectorBinary.toFloats(binary)
    }

    /** Вектор числами `Double`; пустой массив, если вектора нет. */
    var vector: DoubleArray
        get() {
            val floats = vectorFloats() ?: return DoubleArray(0)
            val widened = DoubleArray(floats.size)
            for (i in floats.indices) {
                widened[i] = floats[i].toDouble()
            }
            return widened
        }
        set(value) {
            vectorBinary = packBinary(value)
        }

    /**
     * Двоичное представление вектора, либо `null`.
     *
     * Только для полного вектора: неполный остаётся `null`, и это видно, а не
     * выглядит как настоящие нули.
     */
    private fun packBinary(value: DoubleArray): ByteArray? {
        if (value.size != VectorBinary.COMPONENTS) return null
        val asText = value.joinToString("|")
        val probe = ByteArray(VectorBinary.BYTES)
        if (VectorBinary.encode(asText, probe, 0) != VectorBinary.COMPONENTS) return null
        val packed = ByteArray(VectorBinary.BYTES)
        VectorBinary.encode(asText, packed, 0)
        return packed
    }
}
