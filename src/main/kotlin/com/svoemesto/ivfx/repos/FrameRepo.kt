package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Frame
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FrameRepo : CrudRepository<Frame, Long> {
    fun findByFileId(fileId: Long): Iterable<Frame>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_frames WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_frames WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

    fun findByFileIdAndFrameNumberGreaterThanOrderByFrameNumber(fileId: Long, frameNumber: Int): Iterable<Frame>
    fun findByFileIdAndFrameNumber(fileId: Long, frameNumber: Int): Iterable<Frame>

    @Query(value = "insert into tbl_frames (frame_number, file_id) SELECT distinct (a1 + a2*10 + a3*100 + a4*1000 + a5*10000 + a6*100000)+1 as frameNumber, ?1 as file_id " +
            "FROM (SELECT 0 a1 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1 cross JOIN " +
            "(SELECT 0 a2 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2 cross JOIN " +
            "(SELECT 0 a3 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3 cross JOIN " +
            "(SELECT 0 a4 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t4 cross JOIN " +
            "(SELECT 0 a5 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t5 cross JOIN " +
            "(SELECT 0 a6 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 " +
            "UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t6 " +
            "order by frameNumber " +
            "limit ?2", nativeQuery = true)
    fun createFrames(fileId: Long, countFrames: Int)

}