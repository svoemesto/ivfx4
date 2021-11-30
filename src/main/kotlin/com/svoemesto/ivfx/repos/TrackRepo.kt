package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Track
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface TrackRepo : CrudRepository<Track, Long> {

    fun findByFileIdAndOrderGreaterThanOrderByOrder(fileId: Long, order: Int) : Iterable<Track>
    fun findByFileIdAndOrderLessThanOrderByOrderDesc(fileId: Long, order: Int): Iterable<Track>

    @Query(value = "SELECT * FROM tbl_files_tracks WHERE file_id = ? ORDER BY order_file_track DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(fileId:Long) : Iterable<Track>

    fun findByFileId(fileId: Long): Iterable<Track>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files_tracks WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files_tracks WHERE id = ?", nativeQuery = true)
    fun delete(trackId:Long)

}