package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.FaceTrack
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
interface FaceTrackRepo : CrudRepository<FaceTrack, Long> {
    fun findByShotId(shotId: Long): Iterable<FaceTrack>

    @Query(
        value =
            "select distinct ft.* from tbl_faces_tracks ft " +
                "inner join tbl_shots as s on ft.shot_id = s.id where s.file_id = ?1",
        nativeQuery = true,
    )
    fun findByFileId(fileId: Long): Iterable<FaceTrack>

    /**
     * Треки живут внутри сцен, поэтому чистим по сценам файла: сцена может
     * быть пересоздана шагом разделения, и тогда трек перестаёт существовать.
     * Порядок важен — сперва снимаем ссылку с лиц, иначе останется висячий
     * внешний ключ.
     */
    @Modifying
    @Query(
        value =
            "UPDATE tbl_faces SET track_id = NULL WHERE track_id IN " +
                "(SELECT ft.id FROM tbl_faces_tracks ft inner join tbl_shots as s on ft.shot_id = s.id " +
                "where s.file_id = ?1)",
        nativeQuery = true,
    )
    fun unlinkTracks(fileId: Long)

    @Modifying
    @Query(
        value =
            "DELETE FROM tbl_faces_tracks WHERE shot_id IN " +
                "(SELECT id FROM tbl_shots WHERE file_id = ?1)",
        nativeQuery = true,
    )
    fun deleteAll(fileId: Long)

    /**
     * Переносит треки с одного плана на другой. Нужен при объединении
     * планов: треки привязаны к плану внешним ключом, и удаление плана
     * вместе с ними либо падает, либо теряет работу. Перенос сохраняет и
     * границы треков — они заданы кадрами, а кадры при склейке не двигаются.
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE tbl_faces_tracks SET shot_id = ?2 WHERE shot_id = ?1", nativeQuery = true)
    fun moveToShot(
        fromShotId: Long,
        toShotId: Long,
    )
}
