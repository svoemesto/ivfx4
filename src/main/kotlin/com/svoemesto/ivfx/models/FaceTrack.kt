package com.svoemesto.ivfx.models

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * Трек — это один и тот же человек в пределах одной сцены.
 *
 * Смысл в том, что DF просматривает не все кадры, а лишь часть: между
 * соседними проверенными кадрами проходит до 0,83 секунды, и человек за это
 * время может уйти из кадра и вернуться. Поэтому по одним и тем же лицам
 * судить о присутствии человека нельзя — а по треку можно: если он был в
 * начале сцены и в конце, значит был, скорее всего, и в промежутке между
 * ними, даже если ни одного проверенного кадра там нет.
 *
 * Треки нужны в двух местах:
 *  - распознавание принимает решение один раз на человека, а не на каждое
 *    лицо: на этой серии 8218 лиц-персонажей сворачиваются примерно в 1665
 *    треков, то есть решение принимается в 4,9 раза реже;
 *  - по трекам отбираются кадры для адресной перепроверки: пропущенные
 *    кадры, лежащие внутри окна присутствия человека.
 *
 * Таблица создаётся сама: в настройках включено ddl-auto=update.
 */
@Entity
@Table(name = "tbl_faces_tracks")
@Component
@Transactional
class FaceTrack: Comparable<FaceTrack> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    /** Сцена, внутри которой трек существует. Между сценами не склеиваем:
     *  это разные куски, и человек в них может быть другим. */
    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var shot: Shot

    /** Кто в этом треке. Заполняется при разметке или при распознавании. */
    @ManyToOne(fetch = FetchType.LAZY)
    var person: Person? = null

    var firstFrameNumber: Int = 0
    var lastFrameNumber: Int = 0

    /** Сколько лиц в треке. Держится отдельно от связи, чтобы не тянуть
     *  коллекцию ради одного числа. */
    var faceCount: Int = 0

    /**
     * Показывать ли этот трек при работе с лицами. Трек можно оставить в
     * базе и скрыть: полезно, когда объединение получилось неудачным и
     * лиц из него надо разбирать по отдельности.
     */
    var useTrack: Boolean = true

    override fun compareTo(other: FaceTrack): Int {
        if (shot.id != other.shot.id) return shot.id.compareTo(other.shot.id)
        return firstFrameNumber.compareTo(other.firstFrameNumber)
    }

    override fun toString(): String {
        return "FaceTrack[id=$id, shot=${shot.id}, frames=$firstFrameNumber..$lastFrameNumber, лиц=$faceCount]"
    }
}