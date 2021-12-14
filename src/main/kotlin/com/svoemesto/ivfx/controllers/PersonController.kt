package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.modelsext.FaceExt
import com.svoemesto.ivfx.modelsext.PersonExt
import com.svoemesto.ivfx.modelsext.ProjectExt
import org.springframework.stereotype.Controller
import java.util.*

@Controller
//@Scope("prototype")
class PersonController() {

    companion object {

        fun getListPersons(project: Project): MutableList<Person> {
            val result = Main.personRepo.findByProjectId(project.id).toMutableList()
            result.sort()
            return result
        }

        fun getListPersonsExt(projectExt: ProjectExt): MutableList<PersonExt> {

            val listPersonsExt: MutableList<PersonExt> = mutableListOf()
            getListPersons(projectExt.project).forEach { listPersonsExt.add(PersonExt(it, projectExt)) }

            return listPersonsExt
        }

        fun save(person: Person) {
            Main.personRepo.save(person)
        }

        fun saveAll(persons: Iterable<Person>) {
            persons.forEach { save(it) }
        }

        fun create(project: Project, name: String = "",
                   nameInRecognizer: String = "",
                   fileIdForPreview: Long = 0L,
                   frameNumberForPreview: Int = 0,
                   faceNumberForPreview: Int = 0): Person {
            val entity = Person()
            entity.project = project
            entity.name = if (name != "") name else "Person #" + Main.personRepo.findByProjectId(project.id).toMutableList().size
            entity.nameInRecognizer = if (nameInRecognizer != "") nameInRecognizer else UUID.randomUUID().toString()
            entity.fileIdForPreview = fileIdForPreview
            entity.frameNumberForPreview = frameNumberForPreview
            entity.faceNumberForPreview = faceNumberForPreview
            save(entity)

            return entity
        }

        // удаление файла
        fun delete(person: Person) {
            PropertyController.deleteAll(person::class.java.simpleName, person.id)
            PropertyCdfController.deleteAll(person::class.java.simpleName, person.id)
            Main.personRepo.delete(person)
        }

        fun deleteAll(project: Project) {
            getListPersons(project).forEach { delete(it) }
        }

    }
}