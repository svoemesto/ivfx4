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

//        fun getListPersons(project: Project): MutableList<Person> {
//            val result = Main.personRepo.findByProjectId(project.id).toMutableList()
//            result.forEach { it.project = project }
//            result.sort()
//            return result
//        }

        fun isPropertyPresent(person: Person, key: String) : Boolean {
            return Main.propertyRepo.findByParentClassAndParentIdAndKey(person::class.simpleName!!, person.id, key).any()
        }

        fun getSetPersons(project: Project): MutableSet<Person> {
            return Main.personRepo.findByProjectId(project.id).map { it.project = project;it }.toMutableSet()
        }

        fun getListPersonsExt(projectExt: ProjectExt): MutableList<PersonExt> {
            val result = projectExt.project.persons.map {PersonExt(it, projectExt)}.toMutableList()
            result.sort()
            return result
        }

        fun save(person: Person) {
            Main.personRepo.save(person)
        }

        fun saveAll(persons: Iterable<Person>) {
            persons.forEach { save(it) }
        }

        fun getUndefinded(project: Project): Person {
            var person = project.persons.firstOrNull { it.personType == PersonType.UNDEFINDED }
//            var person = Main.personRepo.findByProjectIdAndPersonType(project.id, PersonType.UNDEFINDED).firstOrNull()
            if (person == null) {
                person = create(project,"UNDEFINDED", PersonType.UNDEFINDED)
            }
            return person
        }

        fun getNonperson(project: Project): Person {
            var person = project.persons.firstOrNull { it.personType == PersonType.NONPERSON }
//            var person = Main.personRepo.findByProjectIdAndPersonType(project.id, PersonType.NONPERSON).firstOrNull()
            if (person == null) {
                person = create(project,"NONPERSON", PersonType.NONPERSON)
            }
            return person
        }

        fun getExtras(project: Project): Person {
            var person = project.persons.firstOrNull { it.personType == PersonType.EXTRAS }
//            var person = Main.personRepo.findByProjectIdAndPersonType(project.id, PersonType.EXTRAS).firstOrNull()
            if (person == null) {
                person = create(project,"EXTRAS", PersonType.EXTRAS)
            }
            return person
        }

        fun getUndefindedExt(projectExt: ProjectExt): PersonExt {
            return PersonExt(getUndefinded(projectExt.project), projectExt)
        }

        fun getNonpersonExt(projectExt: ProjectExt): PersonExt {
            return PersonExt(getNonperson(projectExt.project), projectExt)
        }

        fun getExtrasExt(projectExt: ProjectExt): PersonExt {
            return PersonExt(getExtras(projectExt.project), projectExt)
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
            project.persons.add(entity)

            return entity
        }

        // удаление файла
        fun delete(person: Person) {
            PropertyController.deleteAll(person::class.java.simpleName, person.id)
            PropertyCdfController.deleteAll(person::class.java.simpleName, person.id)
            Main.personRepo.delete(person)
        }

        fun deleteAll(project: Project) {
            project.persons.forEach { delete(it) }
        }

        fun getPersonByProjectIdAndNameInRecognizer(project: Project,
                                                    personRecognizedName: String,
                                                    fileIdToCreate: Long = 0,
                                                    frameNumberToCreate: Int = 0,
                                                    faceNumberInFrameToCreate: Int = 0): Person {
            val person = project.persons.firstOrNull { it.nameInRecognizer == personRecognizedName }
//            val person = Main.personRepo.findByProjectIdAndNameInRecognizer(project.id, personRecognizedName).firstOrNull()
            if (person != null) {
                return person
            } else {
                return create(project,"", PersonType.PERSON, personRecognizedName, fileIdToCreate, frameNumberToCreate, faceNumberInFrameToCreate)
            }
//            return person ?: create(project,"", PersonType.PERSON, personRecognizedName, fileIdToCreate, frameNumberToCreate, faceNumberInFrameToCreate)
        }

        fun getById(personId: Long): Person {
            return Main.personRepo.findById(personId).get()
        }

    }
}