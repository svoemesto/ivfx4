package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.modelsext.FileExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import com.svoemesto.ivfx.modelsext.ShotExt
import org.springframework.stereotype.Controller

// @Scope("prototype")
@Controller
class ShotController {
    companion object {
        fun getProperties(shot: Shot): List<Property> =
            Main.propertyRepo.findByParentClassAndParentId(shot::class.simpleName!!, shot.id).toList()

        fun getSetShots(file: File): MutableSet<Shot> {
            val result = Main.shotRepo.findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(file.id, 0).toMutableSet()
            result.forEach { shot ->
                shot.file = file
            }
            return result
        }

        fun getPropertyValue(
            shot: Shot,
            key: String,
        ): String {
            val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).firstOrNull()
            return property?.value ?: ""
        }

        fun isPropertyPresent(
            shot: Shot,
            key: String,
        ): Boolean = Main.propertyRepo.findByParentClassAndParentIdAndKey(shot::class.simpleName!!, shot.id, key).any()

        fun save(shot: Shot) {
            Main.shotRepo.save(shot)
        }

        fun saveAll(shots: Iterable<Shot>) {
            Main.shotRepo.saveAll(shots)
        }

        /**
         * Переносит всё привязанное к плану from на план to. Нужна при
         * объединении планов: план исчезает, а работа, сделанная по нему,
         * должна сохраниться на выжившем.
         *
         * Без этого объединение либо падает по внешнему ключу на треках, либо
         * — если ключа не было бы — молча оставляет сирот. Обойти можно было
         * сначала прогнав пересборку треков, но тогда теряется работа: треки
         * пришлось бы строить заново.
         *
         * Границы переносимых треков не трогаются: они заданы кадрами, а
         * кадры при объединении планов не меняются.
         */
        fun moveAll(
            from: Shot,
            to: Shot,
        ) {
            val cls = from::class.java.simpleName
            Main.faceTrackRepo.moveToShot(from.id, to.id)
            Main.propertyRepo.moveToParent(cls, from.id, to.id)
            Main.propertyCdfRepo.moveToParent(cls, from.id, to.id)
            Main.shotTmpCdfRepo.moveToShot(from.id, to.id)
            Main.shotTmp2CdfRepo.moveToShot(from.id, to.id)
        }

        fun delete(shot: Shot) {
            PropertyController.deleteAll(shot::class.java.simpleName, shot.id)
            PropertyCdfController.deleteAll(shot::class.java.simpleName, shot.id)
            Main.shotRepo.delete(shot)
        }

        /**
         * Удаление плана после того, как [moveAll] перенёс его содержимое
         * на другой план. Свойства не удаляются — они уже живут на
         * выжившем плане, и удаление уничтожило бы перенесённое.
         */
        fun deleteAfterMove(shot: Shot) {
            Main.shotRepo.delete(shot)
        }

        /**
         * Удаление всех планов файла вместе со всем, что от них зависит.
         *
         * Треки сносятся здесь же, и это не потеря данных: планы удаляются и
         * создаются заново, поэтому идентификаторы меняются и любой трек
         * после пересборки всё равно ссылался бы в пустоту. Не удалить их
         * означало упереться во внешний ключ tbl_faces_tracks → tbl_shots и
         * упасть, не сделав ничего.
         *
         * Лица при этом не трогаются: они привязаны к кадрам, а не к планам,
         * и переживают пересборку планов без потерь.
         */
        fun deleteAll(file: File) {
            getSetShots(file).forEach { shot ->
                PropertyController.deleteAll(shot::class.java.simpleName, shot.id)
                PropertyCdfController.deleteAll(shot::class.java.simpleName, shot.id)
            }
            // Лица держат ссылку на свой трек, поэтому трек нельзя удалить,
            // пока эта ссылка жива: внешний ключ tbl_faces.track_id →
            // tbl_faces_tracks не даст. Ссылку обнуляем — сам трек после
            // пересборки планов anyway не сохранил бы смысла, а лицо
            // остаётся на месте вместе с эмбеддингом и распознанной персоной.
            Main.faceTrackRepo.unlinkTracks(file.id)
            Main.faceTrackRepo.deleteAll(file.id)
            Main.shotRepo.deleteAll(file.id)
        }

        fun getOrCreate(
            file: File,
            firstFrameNumber: Int,
            lastFrameNumber: Int,
            nearestIFrame: Int = 0,
        ): Shot {
            var entity =
                Main.shotRepo
                    .findByFileIdAndFirstFrameNumberAndLastFrameNumber(
                        file.id,
                        firstFrameNumber,
                        lastFrameNumber,
                    ).firstOrNull()
            if (entity == null) {
                entity = Shot()
                entity.file = file
                entity.firstFrameNumber = firstFrameNumber
                entity.lastFrameNumber = lastFrameNumber
                entity.nearestIFrame = nearestIFrame
                save(entity)
            } else {
                entity.file = file
            }
            return entity
        }

        fun convertSetShotsToListShotsExt(shots: Set<Shot>): MutableList<ShotExt> {
            val projectsExt: MutableSet<ProjectExt> = mutableSetOf()
            val filesExt: MutableSet<FileExt> = mutableSetOf()
            val result =
                shots
                    .map { shot ->
                        val projectExt = projectsExt.firstOrNull { it.project.id == shot.file.project.id } ?: ProjectExt(shot.file.project)
                        val fileExt = filesExt.firstOrNull { it.file.id == shot.file.id } ?: FileExt(shot.file, projectExt)
                        projectsExt.add(projectExt)
                        filesExt.add(fileExt)
                        ShotExt(
                            shot,
                            fileExt,
                            FrameController.getFrameExt(fileExt, shot.firstFrameNumber),
                            FrameController.getFrameExt(fileExt, shot.lastFrameNumber),
                        )
                    }.toMutableList()
            result.sort()
            return result
        }

        fun convertSetShotsIdsToListShotsExt(
            shotsIds: Set<Long>,
            projectExt: ProjectExt,
        ): MutableList<ShotExt> {
//            val projectsExt: MutableSet<ProjectExt> = mutableSetOf()
            val filesExt: MutableSet<FileExt> = mutableSetOf()
            Main.shotTmp2CdfRepo.deleteAll(Main.ccid)
//            shotsIds.forEach { Main.shotTmp2CdfRepo.addByShotId(Main.ccid, it) }
            Main.shotTmp2CdfRepo.addByShotIds(Main.ccid, shotsIds)
            val shotTmp2Cdfs = Main.shotTmp2CdfRepo.findByComputerId(Main.ccid)

            val tmp = Main.shotRepo.findByIds(shotsIds)

            val shots = tmp.associateBy { it.id }.toMutableMap()

            val result =
                shotTmp2Cdfs
                    .map { shotTmp2Cdf ->
//                val shot = Main.shotRepo.findById(shotTmp2Cdf.shotId).get()
                        val shot = shots[shotTmp2Cdf.shotId]!!
//                val projectExt = projectsExt.firstOrNull{it.project.id == shotTmp2Cdf.projectId}?: ProjectExt(ProjectController.getProject(shotTmp2Cdf.projectId))
                        val fileExt =
                            filesExt.firstOrNull { it.file.id == shotTmp2Cdf.fileId }
                                ?: FileExt(FileController.getFile(shotTmp2Cdf.fileId, projectExt.project), projectExt)
//                projectsExt.add(projectExt)
                        filesExt.add(fileExt)
                        shot.file = fileExt.file
//                shot.file.project = projectExt.project
                        ShotExt(shot, fileExt) // , FrameController.getFrameExt(fileExt, shot.firstFrameNumber), FrameController.getFrameExt(fileExt, shot.lastFrameNumber))
                    }.toMutableList()
            result.sort()
            return result
        }
    }
}
