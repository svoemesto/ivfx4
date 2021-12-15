package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.models.Person
import com.svoemesto.ivfx.models.Project
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

        fun getUndefinded(projectExt: ProjectExt): PersonExt {
            var person = Main.personRepo.findByProjectIdAndPersonType(projectExt.project.id, PersonType.UNDEFINDED).firstOrNull()
            if (person == null) {
                person = create(projectExt.project,"UNDEFINDED", PersonType.UNDEFINDED)
            }
            return PersonExt(person, projectExt)
        }

        fun getNonperson(projectExt: ProjectExt): PersonExt {
            var person = Main.personRepo.findByProjectIdAndPersonType(projectExt.project.id, PersonType.NONPERSON).firstOrNull()
            if (person == null) {
                person = create(projectExt.project,"NONPERSON", PersonType.NONPERSON)
            }
            return PersonExt(person, projectExt)
        }

        fun getExtras(projectExt: ProjectExt): PersonExt {
            var person = Main.personRepo.findByProjectIdAndPersonType(projectExt.project.id, PersonType.EXTRAS).firstOrNull()
            if (person == null) {
                person = create(projectExt.project,"EXTRAS", PersonType.EXTRAS)
            }
            return PersonExt(person, projectExt)
        }

        fun create(project: Project, name: String = "", personType: PersonType = PersonType.PERSON,
                   nameInRecognizer: String = "",
                   fileIdForPreview: Long = 0L,
                   frameNumberForPreview: Int = 0,
                   faceNumberForPreview: Int = 0): Person {
            val entity = Person()
            entity.project = project
            entity.personType = personType
            entity.name = if (name != "") name else "Person #" + Main.personRepo.findByProjectId(project.id).toMutableList().size
            entity.fileIdForPreview = fileIdForPreview
            entity.frameNumberForPreview = frameNumberForPreview
            entity.faceNumberForPreview = faceNumberForPreview
            entity.uuid = UUID.randomUUID().toString()
            entity.nameInRecognizer = if (nameInRecognizer != "") nameInRecognizer else entity.uuid
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

        fun getPersonExtIdByProjectExtIdAndNameInRecognizer(projectExt: ProjectExt,
                                                            personRecognizedName: String,
                                                            createIfNotFound: Boolean = false,
                                                            fileIdToCreate: Long = 0,
                                                            frameNumberToCreate: Int = 0,
                                                            faceNumberInFrameToCreate: Int = 0): Long {
            val person = Main.personRepo.findByProjectIdAndNameInRecognizer(projectExt.project.id, personRecognizedName).firstOrNull()
            return person?.id
                ?: if (createIfNotFound) create(projectExt.project,"", PersonType.PERSON, personRecognizedName, fileIdToCreate, frameNumberToCreate, faceNumberInFrameToCreate).id else 0
        }

    }
}