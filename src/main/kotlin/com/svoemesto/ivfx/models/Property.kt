package com.svoemesto.ivfx.models

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_properties")
@Transactional
class Property {

    @NotNull(message = "ID свойства не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "order_property", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "parent_class", columnDefinition = "varchar(255) default ''")
    var parentClass: String = ""

    @Column(name = "parent_id", nullable = false, columnDefinition = "bigint default 0")
    var parentId: Long = 0

    @Column(name = "property_key", columnDefinition = "varchar(255) default ''")
    var key: String = ""

    @Lob
    @Column(name = "property_value")
    var value: String = ""

}