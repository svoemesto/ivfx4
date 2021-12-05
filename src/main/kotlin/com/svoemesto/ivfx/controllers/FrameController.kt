package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.getH2Connection
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.repos.FileCdfRepo
import com.svoemesto.ivfx.repos.FileRepo
import com.svoemesto.ivfx.repos.FrameRepo
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import com.svoemesto.ivfx.repos.ProjectRepo
import com.svoemesto.ivfx.repos.PropertyCdfRepo
import com.svoemesto.ivfx.repos.PropertyRepo
import com.svoemesto.ivfx.repos.ShotRepo
import com.svoemesto.ivfx.repos.TrackRepo
import org.springframework.stereotype.Controller
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class FrameController() {

    class FrameExt(val frame: Frame, val pathToSmall: String, val pathToMedium: String, val pathToFull: String)

    fun getProperties(frame: Frame) : List<Property> {
        return Main.propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
    }

    fun getOrCreate(file: File, frameNumber: Int): Frame {
        var entity = Main.frameRepo.findByFileIdAndFrameNumber(file.id, frameNumber).firstOrNull()
        if (entity == null) {
            entity = Frame()
            entity.file = file
            entity.frameNumber = frameNumber
            save(entity)
        } else {
            entity.file = file
        }
        return entity
    }

    fun getListFrames(file: File): MutableList<Frame> {
        val result = Main.frameRepo.findByFileIdAndFrameNumberGreaterThanOrderByFrameNumber(file.id,0).toMutableList()
        result.forEach { it.file = file }
        return result
    }

    fun getListFramesExt(file: File): MutableList<FrameExt> {
        val listFrames = getListFrames(file)
        val pathToFramesSmall = file.folderFramesSmall + IOFile.separator
        val pathToFramesMedium = file.folderFramesMedium + IOFile.separator
        val pathToFramesFull = file.folderFramesFull + IOFile.separator
        var result: MutableList<FrameExt> = mutableListOf()
        listFrames.forEach { frame ->
            val fileName = "${file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
            result.add(FrameExt(frame, pathToFramesSmall + fileName, pathToFramesMedium + fileName, pathToFramesFull + fileName ))
        }
        return result
    }

    fun createFrames(file: File) {
        deleteAll(file)
//        Main.frameRepo.createFrames(file.id, Main.fileController.getFramesCount(file))

        var sql = "insert into tbl_frames (frame_number, file_id, sim_score_next_1, sim_score_next_2, sim_score_next_3, sim_score_prev_1, sim_score_prev_2, sim_score_prev_3, diff_next_1, diff_next_2, diff_prev_1, diff_prev_2) SELECT distinct (a1 + a2*10 + a3*100 + a4*1000 + a5*10000 + a6*100000)+1 as frameNumber, ${file.id} as file_id, 0,0,0,0,0,0,0,0,0,0 " +
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
                "limit ${file.framesCount}"
        val st = Main.connection.createStatement()
        st.execute(sql)

    }

    fun getPropertyValue(frame: Frame, key: String) : String {
        val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).firstOrNull()
        return property?.value ?: ""
    }

    fun isPropertyPresent(frame: Frame, key: String) : Boolean {
        return Main.propertyRepo.findByParentClassAndParentIdAndKey(frame::class.simpleName!!, frame.id, key).any()
    }

    fun save(frame: Frame) {
        Main.frameRepo.save(frame)
    }

    fun delete(frame: Frame) {
//        Main.propertyController.deleteAll(frame::class.java.simpleName, frame.id)
//        Main.propertyCdfController.deleteAll(frame::class.java.simpleName, frame.id)
        Main.frameRepo.delete(frame)
    }

    fun deleteAll(file: File) {
//        getListFrames(file).forEach { frame ->
//            Main.propertyController.deleteAll(frame::class.java.simpleName, frame.id)
//            Main.propertyCdfController.deleteAll(frame::class.java.simpleName, frame.id)
//        }
        Main.frameRepo.deleteAll(file.id)
    }
    fun create(file: File, frameNumber: Int): Frame {
        val entity = Frame()
        entity.file = file
        entity.frameNumber = frameNumber
        save(entity)
        return entity
    }

    fun getFileNameFrameSmall(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_SMALL)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameMedium(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_MEDIUM)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }

    fun getFileNameFrameFull(frame: Frame): String {
        return "${Main.fileController.getCdfFolder(frame.file, Folders.FRAMES_FULL)}${IOFile.separator}${frame.file.shortName}_frame_${String.format("%06d", frame.frameNumber)}.jpg"
    }
}