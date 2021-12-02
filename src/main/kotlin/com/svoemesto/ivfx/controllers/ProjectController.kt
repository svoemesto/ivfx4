package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Project
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
import java.io.IOException
import java.io.File as IOFile

@Controller
//@Scope("prototype")
class ProjectController(val projectRepo: ProjectRepo,
                        val propertyRepo: PropertyRepo,
                        val propertyCdfRepo: PropertyCdfRepo,
                        val projectCdfRepo: ProjectCdfRepo,
                        val fileRepo: FileRepo,
                        val fileCdfRepo: FileCdfRepo,
                        val frameRepo: FrameRepo,
                        val trackRepo: TrackRepo,
                        val shotRepo: ShotRepo) {

    fun getCdfFolder(project: Project, folder: Folders, createIfNotExist: Boolean = false): String {
        val propertyValue = getPropertyValue(project, folder.propertyCdfKey)
        val fld = if (propertyValue == "") project.folder + IOFile.separator + folder.folderName else propertyValue
        try {
            if (createIfNotExist && !IOFile(fld).exists()) IOFile(fld).mkdir()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fld
    }

    fun getListProjects(): List<Project> {
        val list = projectRepo.findByOrderGreaterThanOrderByOrder(0).toList()
        list.forEach { ProjectCdfController(projectCdfRepo).getProjectCdf(it) }
        return projectRepo.findByOrderGreaterThanOrderByOrder(0).toList()
    }

//    fun getProperties(project: Project) : List<Property> {
//        return propertyRepo.findByParentClassAndParentId(project::class.simpleName!!, project.id).toList()
//    }

    fun getPropertyValue(project: Project, key: String) : String {
        val property = propertyRepo.findByParentClassAndParentIdAndKey(project::class.simpleName!!, project.id, key).firstOrNull()
        return if (property != null) property.value else ""
    }

    fun isPropertyPresent(project: Project, key: String) : Boolean {
        return propertyRepo.findByParentClassAndParentIdAndKey(project::class.simpleName!!, project.id, key).any()
    }

    fun create(): Project {
        val entity = Project()
        val lastEntity = projectRepo.getEntityWithGreaterOrder().firstOrNull()
        entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
        entity.name = "New order ${entity.order}"
        projectRepo.save(entity)
        return entity
    }

    // удаление проекта
    fun delete(project: Project) {
        reOrder(ReorderTypes.MOVE_TO_LAST, project)
        // удаляем все свойства проекта
        propertyRepo.deleteAll(project::class.java.simpleName, project.id)
        propertyCdfRepo.deleteAll(project::class.java.simpleName, project.id)

        project.files.forEach { file ->
            propertyRepo.deleteAll(file::class.java.simpleName, file.id)
            propertyCdfRepo.deleteAll(file::class.java.simpleName, file.id)
            fileCdfRepo.deleteAll(file.id)
            file.frames.forEach{ frame ->
                propertyRepo.deleteAll(frame::class.java.simpleName, frame.id)
                propertyCdfRepo.deleteAll(frame::class.java.simpleName, frame.id)
            }
            frameRepo.deleteAll(file.id)
            file.tracks.forEach{ track ->
                propertyRepo.deleteAll(track::class.java.simpleName, track.id)
                propertyCdfRepo.deleteAll(track::class.java.simpleName, track.id)
            }
            trackRepo.deleteAll(file.id)
        }
        fileRepo.deleteAll(project.id)
        projectCdfRepo.deleteAll(project.id)
        projectRepo.delete(project.id)
    }

    fun reOrder(reorderType: ReorderTypes, project: Project) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = projectRepo.findByOrderGreaterThanOrderByOrder(project.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    project.order += 1
                    projectRepo.save(project)
                    projectRepo.save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = projectRepo.findByOrderLessThanOrderByOrderDesc(project.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    project.order -= 1
                    projectRepo.save(project)
                    projectRepo.save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = projectRepo.findByOrderLessThanOrderByOrderDesc(project.order)
                previousEntities.forEach{it.order++}
                projectRepo.saveAll(previousEntities)
                project.order = 1
                projectRepo.save(project)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = projectRepo.findByOrderGreaterThanOrderByOrder(project.order)
                if (nextEntities.count()>0) {
                    nextEntities.forEach{it.order--}
                    projectRepo.saveAll(nextEntities)
                    project.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    projectRepo.save(project)
                }
            }
        }
    }

}
