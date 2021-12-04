package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
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
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.File as IOFile

@Controller
//@Scope("prototype")
@Transactional
class ProjectController() {

    fun getCdfFolder(project: Project, folder: Folders, createIfNotExist: Boolean = false): String {
        if (!isPropertyCdfPresent(project, folder.propertyCdfKey)) {
            Main.propertyCdfController.getOrCreate(project::class.java.simpleName, project.id, folder.propertyCdfKey)
        }
        val propertyValue = getPropertyCdfValue(project, folder.propertyCdfKey)
        val fld = if (propertyValue == "") project.folder + IOFile.separator + folder.folderName else propertyValue
        try {
            if (createIfNotExist && !IOFile(fld).exists()) {
                if (!isPropertyPresent(project, folder.propertyCdfKey))
                IOFile(fld).mkdir()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fld
    }

    fun getListProjects(): List<Project> {
        val result = Main.projectRepo.findByOrderGreaterThanOrderByOrder(0).toList()
        result.forEach { project ->
            project.cdfs = mutableListOf()
            project.cdfs.add(Main.projectCdfController.getProjectCdf(project))
            project.files = Main.fileController.getListFiles(project)
        }
        return result
    }

    fun getProperties(project: Project) : List<Property> {
        return Main.propertyRepo.findByParentClassAndParentId(project::class.simpleName!!, project.id).toList()
    }

    fun getPropertyValue(project: Project, key: String) : String {
        val property = Main.propertyRepo.findByParentClassAndParentIdAndKey(project::class.simpleName!!, project.id, key).firstOrNull()
        return property?.value ?: ""
    }

    fun isPropertyPresent(project: Project, key: String) : Boolean {
        return Main.propertyRepo.findByParentClassAndParentIdAndKey(project::class.simpleName!!, project.id, key).any()
    }

    fun getPropertyCdfValue(project: Project, key: String) : String {
        val property = Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(project::class.simpleName!!, project.id, Main.ccid, key).firstOrNull()
        return property?.value ?: ""
    }

    fun isPropertyCdfPresent(project: Project, key: String) : Boolean {
        return Main.propertyCdfRepo.findByParentClassAndParentIdAndComputerIdAndKey(project::class.simpleName!!, project.id, Main.ccid, key).any()
    }


    fun save(project: Project) {
        Main.projectRepo.save(project)
    }

    fun saveAll(projects: Iterable<Project>) {
        projects.forEach { save(it) }
    }

    fun create(): Project {
        val entity = Project()
        val lastEntity = Main.projectRepo.getEntityWithGreaterOrder().firstOrNull()
        entity.order = if (lastEntity != null) lastEntity.order + 1 else 1
        entity.name = "New order ${entity.order}"
        entity.cdfs = mutableListOf()
        entity.cdfs.add(Main.projectCdfController.create(entity))
        save(entity)
        Folders.values().forEach {
            Main.propertyCdfController.editOrCreate(entity::class.java.simpleName, entity.id, it.propertyCdfKey)
        }
        return entity
    }

    // удаление проекта
    fun delete(project: Project) {
        reOrder(ReorderTypes.MOVE_TO_LAST, project)
        // удаляем все свойства проекта
        Main.propertyRepo.deleteAll(project::class.java.simpleName, project.id)
        Main.propertyCdfRepo.deleteAll(project::class.java.simpleName, project.id)
        Main.projectCdfController.deleteAll(project)
        Main.fileController.deleteAll(project)
        Main.projectRepo.delete(project.id)
    }

    fun reOrder(reorderType: ReorderTypes, project: Project) {

        when (reorderType) {
            ReorderTypes.MOVE_DOWN -> {
                val nextEntity = Main.projectRepo.findByOrderGreaterThanOrderByOrder(project.order).firstOrNull()
                if (nextEntity != null) {
                    nextEntity.order -= 1
                    project.order += 1
                    save(project)
                    save(nextEntity)
                }
            }
            ReorderTypes.MOVE_UP -> {
                val previousEntity = Main.projectRepo.findByOrderLessThanOrderByOrderDesc(project.order).firstOrNull()
                if (previousEntity != null) {
                    previousEntity.order += 1
                    project.order -= 1
                    save(project)
                    save(previousEntity)
                }
            }
            ReorderTypes.MOVE_TO_FIRST -> {
                val previousEntities = Main.projectRepo.findByOrderLessThanOrderByOrderDesc(project.order)
                previousEntities.forEach{it.order++}
                saveAll(previousEntities)
                project.order = 1
                save(project)
            }
            ReorderTypes.MOVE_TO_LAST -> {
                val nextEntities = Main.projectRepo.findByOrderGreaterThanOrderByOrder(project.order)
                if (nextEntities.count()>0) {
                    nextEntities.forEach{it.order--}
                    saveAll(nextEntities)
                    project.order = (nextEntities.lastOrNull()?.order ?: 0) + 1
                    save(project)
                }
            }
        }
    }

}
