package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.Folders
import com.svoemesto.ivfx.enums.ReorderTypes
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.Property
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.util.logging.Logger
import java.io.File as IOFile

@Controller
//@Scope("prototype")
@Transactional
class ProjectController() {

    companion object {
        val LOG: Logger = Logger.getLogger(ProjectController::class.java.name)

        fun getCdfFolder(project: Project, folder: Folders, createIfNotExist: Boolean = false): String {
            LOG.info("fun getCdfFolder started")
            if (!isPropertyCdfPresent(project, folder.propertyCdfKey)) {
                PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, folder.propertyCdfKey)
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
            LOG.info("fun getListProjects started")
            val result = Main.projectRepo.findByOrderGreaterThanOrderByOrder(0).toList()
            result.forEach { project ->
                project.cdfs = mutableSetOf()
                project.cdfs.add(ProjectCdfController.getProjectCdf(project))
                project.files = FileController.getSetFiles(project)
                project.persons = PersonController.getSetPersons(project)
            }
            return result
        }

        fun getFolderPersons(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.PERSONS.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.PERSONS.folderName else value
        }

        fun getFolderLossless(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.LOSSLESS.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.LOSSLESS.folderName else value
        }

        fun getFolderPreview(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.PREVIEW.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.PREVIEW.folderName else value
        }

        fun getFolderFavorites(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.FAVORITES.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.FAVORITES.folderName else value
        }

        fun getFolderShots(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.SHOTS.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.SHOTS.folderName else value
        }

        fun getFolderFramesSmall(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.FRAMES_SMALL.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.FRAMES_SMALL.folderName else value
        }

        fun getFolderFramesMedium(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.FRAMES_MEDIUM.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.FRAMES_MEDIUM.folderName else value
        }

        fun getFolderFramesFull(project: Project): String{
            val value = PropertyCdfController.getOrCreate(project::class.java.simpleName, project.id, Folders.FRAMES_FULL.propertyCdfKey)
            return if (value == "") project.folder + IOFile.separator + Folders.FRAMES_FULL.folderName else value
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
            entity.cdfs = mutableSetOf()
            entity.cdfs.add(ProjectCdfController.create(entity))
            save(entity)
            Folders.values().filter{it.forProject}.forEach {
                PropertyCdfController.editOrCreate(entity::class.java.simpleName, entity.id, it.propertyCdfKey)
            }
            return entity
        }

        // удаление проекта
        fun delete(project: Project) {
            reOrder(ReorderTypes.MOVE_TO_LAST, project)
            // удаляем все свойства проекта
            Main.propertyRepo.deleteAll(project::class.java.simpleName, project.id)
            Main.propertyCdfRepo.deleteAll(project::class.java.simpleName, project.id)
            ProjectCdfController.deleteAll(project)
            FileController.deleteAll(project)
            TagController.deleteAll(project::class.java.simpleName, project.id)
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

        fun getProject(projectId: Long): Project {
            val project = Main.projectRepo.findById(projectId).get()
            project.cdfs = mutableSetOf()
            project.cdfs.add(ProjectCdfController.getProjectCdf(project))
            project.files = FileController.getSetFiles(project)
            project.persons = PersonController.getSetPersons(project)
            return project
        }

        fun getProjectExt(projectId: Long): ProjectExt {
            val project = getProject(projectId)
            return ProjectExt(project)
        }

    }

}
