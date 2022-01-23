package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Frame
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.FrameExt
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class FrameController() {

    companion object {

//        fun getProperties(frame: Frame) : List<Property> {
//            return Main.propertyRepo.findByParentClassAndParentId(frame::class.simpleName!!, frame.id).toList()
//        }

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

        fun getListFramesExt(fileExt: FileExt): MutableList<FrameExt> {
            val listFrames = getListFrames(fileExt.file)
            val result: MutableList<FrameExt> = mutableListOf()
            listFrames.forEach { frame ->
                result.add(FrameExt(frame, fileExt))
            }
            return result
        }

        fun createFrames(fileExt: FileExt) {
            deleteAll(fileExt.file)

            val sql = "insert into tbl_frames (frame_number, file_id, sim_score_next_1, sim_score_next_2, sim_score_next_3, sim_score_prev_1, sim_score_prev_2, sim_score_prev_3, diff_next_1, diff_next_2, diff_prev_1, diff_prev_2) SELECT distinct (a1 + a2*10 + a3*100 + a4*1000 + a5*10000 + a6*100000)+1 as frameNumber, ${fileExt.file.id} as file_id, 0,0,0,0,0,0,0,0,0,0 " +
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
                    "limit ${fileExt.framesCount}"
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
            Main.frameRepo.delete(frame)
        }

        fun deleteAll(file: File) {
            Main.frameRepo.deleteAll(file.id)
        }
        fun create(file: File, frameNumber: Int): Frame {
            val entity = Frame()
            entity.file = file
            entity.frameNumber = frameNumber
            save(entity)
            return entity
        }

        fun getFrameExt(fileId: Long, frameNumber: Int, project: Project): FrameExt {
            val fileExt = FileController.getFileExt(fileId, project)
            val frame = getOrCreate(fileExt.file, frameNumber)
            return FrameExt(frame, fileExt)
        }

        fun getFrameExt(fileExt: FileExt, frameNumber: Int): FrameExt {
            val frame = getOrCreate(fileExt.file, frameNumber)
            return FrameExt(frame, fileExt)
        }

    }


}