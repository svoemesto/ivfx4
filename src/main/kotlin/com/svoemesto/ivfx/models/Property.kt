package com.svoemesto.ivfx.models

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
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

class Property {

    @NotNull(message = "ID свойства не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @Column(name = "order_property", nullable = false)
    var order: Int = 0

    @Column(name = "parent_class")
    var parentClass: String = ""

    @Column(name = "parent_id", nullable = false)
    var parentId: Long = 0

    @Column(name = "property_key")
    var key: String = ""

    @Lob
    @Column(name = "property_value")
    var value: String = ""

}