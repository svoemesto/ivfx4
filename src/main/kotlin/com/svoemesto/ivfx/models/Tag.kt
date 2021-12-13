package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypeSize
import com.svoemesto.ivfx.enums.TagType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

/**
 * parentClass/parentId - идентификатор родительского объекта
 * childClass/childId - идентификатор дочернего объекта
 * TagType может принимать следующие значения:
 * Типы  корневых тэгов (child = 0):
 * - NONE. Не определено. parent = 0, child = 0 Таких быть не должно.
 * - PERSON. Персонаж. parent = project. child = 0
 * - SCENE. Сцена. parent = file. child = 0
 * - EVENT. Событие  parent = file. child = 0
 * - DESCRIPTION_FOR_PERSON. Описательный тег персонажа. parent = project. child = 0
 * > Описание профессии или принадлежности персонажа: шлюха, король, слуга Ланистеров, дотракиец и т.п.
 * - DESCRIPTION_FOR_SHOT. Описательный тег плана. parent = project. child = 0
 * > Название локации или описание происходящего в кадре: убийство, секс, обнажонка и т.п.
 * - DESCRIPTION_FOR_SCENE. Описательный тег сцены. parent = project. child = 0
 * > Описание сцены. Ключевая сцена, второстепенная сцена, "ранее в сериале", титры, заставка и т.п.
 * - DESCRIPTION_FOR_EVENT. Описательный тег события. parent = project. child = 0
 * > Тип события: монолог, диалог, ключевой диалог и т.п.
 * Типы дочерних тэгов (child != 0):
 * - SHOT_PERSON. Персонаж плана. parent = shot, child = person (tag с типом PERSON)
 * - SCENE_SHOT. План сцены. parent = сцена (tag c типом SCENE), child - shot
 * - EVENT_SHOT. План события. parent = событие (tag c типом EVENT), child - shot
 * - PERSON_DESCRIPTION. Описание персонажа. parent = person (tag с типом PERSON), child - описание (tag с типом DESCRIPTION_FOR_PERSON)
 * - SHOT_DESCRIPTION. Описание плана. parent = shot, child - описание (tag с типом DESCRIPTION_FOR_SHOT)
 * - SCENE_DESCRIPTION. Описание сцены. parent = сцена (tag c типом SCENE), child - описание (tag с типом DESCRIPTION_FOR_SCENE)
 * - EVENT_DESCRIPTION. Описание события. parent = событие (tag c типом EVENT), child - описание (tag с типом DESCRIPTION_FOR_EVENT)
 */

@Component
@Entity
@Table(name = "tbl_tags")
@Transactional
class Tag {

    @NotNull(message = "ID тэга не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "order_tag", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "tag_type", columnDefinition = "int default 0")
    var tagType: TagType = TagType.NONE

    @Column(name = "parent_id", columnDefinition = "int default 0")
    var parentId: Long = 0

    @Column(name = "parent_class", columnDefinition = "varchar(255) default ''")
    var parentClass: String = ""

    @Column(name = "child_id", columnDefinition = "int default 0")
    var childId: Long = 0

    @Column(name = "child_class", columnDefinition = "varchar(255) default ''")
    var childClass: String = ""

    @Column(name = "size_type", columnDefinition = "int default 0")
    var sizeType: ShotTypeSize = ShotTypeSize.NONE

    @Column(name = "tag_proba")
    var proba: Double = 0.0

}